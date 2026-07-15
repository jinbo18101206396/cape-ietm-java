package org.jeecg.modules.system.controller;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.oConvertUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;
import utils.DESUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

/**
 * 视频预览控制器 - 终极解决方案
 * 解决：1.主机中的软件中止一个已建立的连接 2.Redis序列化器污染导致登录反序列化失败
 * 核心：直接操作Redis原生连接，不修改全局RedisTemplate任何配置
 */
@Slf4j
@RestController
@RequestMapping("/sys/video")
public class VideoController {
    @Value(value = "${jeecg.path.upload}")
    private String uploadpath;
    //视频缓存大小
    private static final int BUFFER_SIZE = 1024 * 1024;
    // 解密后视频Redis缓存前缀
    private static final String REDIS_DECRYPT_PREFIX = "video:decrypt:";
    // Redis缓存过期时间：24小时
    private static final Long REDIS_EXPIRE = 24 * 60 * 60L;
    // 分片跳过最大尝试次数
    private static final int MAX_SKIP_RETRY = 3;

    // 直接注入全局RedisTemplate，全程不修改其序列化器
    @Resource
    private RedisTemplate<String, byte[]> redisTemplate;

    // 单独声明序列化器，仅用于视频接口的Redis原生连接操作
    private final StringRedisSerializer stringSerializer = new StringRedisSerializer();
    private final JdkSerializationRedisSerializer jdkSerializer = new JdkSerializationRedisSerializer();

    @GetMapping(value = "/viewVideo/**")
    public void viewVideo(HttpServletRequest request, HttpServletResponse response) {
        String imgPath = null;
        String redisKey = null;
        OutputStream out = null;
        InputStream in = null;
        String isEncryption = oConvertUtils.isEmpty(request.getParameter("isEncryption"))
                ? "false" : request.getParameter("isEncryption");
        byte[] videoBytes = null;

        try {
            // 1. 提取并处理视频路径
            imgPath = extractPathFromPattern(request);
            if (oConvertUtils.isEmpty(imgPath) || "null".equals(imgPath)) {
                log.warn("视频路径为空，请求参数：{}", request.getQueryString());
                return;
            }
            // 路径防注入：过滤../ 避免目录遍历
            imgPath = imgPath.replace("..", "").replace("../", "");
            if (imgPath.endsWith(",")) {
                imgPath = imgPath.substring(0, imgPath.length() - 1);
            }

            // 2. 定义Redis缓存Key和原始加密文件路径
            redisKey = REDIS_DECRYPT_PREFIX + imgPath.replace(File.separator, "_");
            String orgFilePath = uploadpath + File.separator + imgPath;

            // 3. 处理加密/非加密视频，获取二进制数组
            if ("true".equals(isEncryption)) { // 加密文件：解密+Redis缓存【核心改造：原生Redis连接操作】
                videoBytes = getRedisValue(redisKey); // 自定义方法获取，不使用redisTemplate.opsForValue()
                if (videoBytes == null || videoBytes.length == 0) {
                    // 直接解密为字节数组，无临时文件
                    videoBytes = DESUtils.decodeBase64Bytes(orgFilePath);
                    // 解密失败校验
                    if (videoBytes == null || videoBytes.length == 0) {
                        setResponseStatus(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        log.error("文件解密失败，路径：{}", orgFilePath);
                        return;
                    }
                    // 解密成功：存入Redis并设置过期时间【原生连接操作】
                    setRedisValue(redisKey, videoBytes, REDIS_EXPIRE);
                    log.info("视频解密并缓存至Redis，key：{}", redisKey);
                }
            } else { // 非加密文件：直接读取本地文件为字节数组
                File orgFile = new File(orgFilePath);
                if (!orgFile.exists() || !orgFile.isFile()) {
                    setResponseStatus(response, HttpServletResponse.SC_NOT_FOUND);
                    log.error("非加密文件不存在/非文件，路径：{}", orgFilePath);
                    return;
                }
                // 本地文件转二进制数组
                videoBytes = Files.readAllBytes(orgFile.toPath());
            }

            // 4. 二进制数组有效性校验
            if (videoBytes == null || videoBytes.length == 0) {
                setResponseStatus(response, HttpServletResponse.SC_NOT_FOUND);
                log.error("视频二进制数据为空，路径：{}", imgPath);
                return;
            }

            long fileLength = videoBytes.length; // 视频总长度
            String rangeHeader = request.getHeader("Range");
            // 5. 设置视频播放通用响应头
            response.setContentType("video/mp4");
            response.setHeader("Accept-Ranges", "bytes"); // 支持分片
            response.setHeader("Cache-Control", "no-cache"); // 防止分片缓存异常
            response.setHeader("Pragma", "no-cache"); // 兼容低版本浏览器
            response.setDateHeader("Expires", 0); // 禁止缓存

            // 获取响应输出流并判空
            out = response.getOutputStream();
            if (out == null) {
                log.warn("获取响应输出流失败，视频路径：{}", imgPath);
                setResponseStatus(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }
            in = new ByteArrayInputStream(videoBytes);

            // 6. 处理【无Range请求】：整段视频流式输出
            if (rangeHeader == null) {
                response.setHeader("Content-Length", String.valueOf(fileLength));
                byte[] buffer = new byte[BUFFER_SIZE];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    writeWithExceptionHandle(out, buffer, 0, len);
                }
                flushWithExceptionHandle(out);
                return;
            }

            // 7. 处理【Range分片请求】：视频拖拽/断点续传（核心）
            String[] ranges = rangeHeader.substring("bytes=".length()).split("-");
            long start = 0;
            // 解析起始位置，做异常容错
            try {
                start = Long.parseLong(ranges[0]);
            } catch (NumberFormatException e) {
                start = 0;
                log.warn("Range起始位置解析失败，使用默认值0，Range：{}", rangeHeader);
            }
            // 解析结束位置，默认到文件末尾
            long end = fileLength - 1;
            if (ranges.length > 1 && !oConvertUtils.isEmpty(ranges[1])) {
                try {
                    end = Long.parseLong(ranges[1]);
                } catch (NumberFormatException e) {
                    end = fileLength - 1;
                    log.warn("Range结束位置解析失败，使用默认值{}，Range：{}", end, rangeHeader);
                }
            }

            // 分片范围合法性校验
            if (start > end || start >= fileLength || end >= fileLength) {
                setResponseStatus(response, HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                response.setHeader("Content-Range", "bytes */" + fileLength);
                log.warn("分片范围不合法，start：{}，end：{}，文件长度：{}", start, end, fileLength);
                return;
            }

            long contentLength = end - start + 1;
            // 设置分片响应头，符合HTTP Range协议
            setResponseStatus(response, HttpServletResponse.SC_PARTIAL_CONTENT);
            response.setHeader("Content-Length", String.valueOf(contentLength));
            response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);

            // 精准跳过起始字节：优化skip逻辑，增加最大重试次数
            long skipBytes = start;
            int retryCount = 0;
            while (skipBytes > 0 && retryCount < MAX_SKIP_RETRY) {
                long actualSkip = in.skip(skipBytes);
                if (actualSkip <= 0) {
                    retryCount++;
                    continue;
                }
                skipBytes -= actualSkip;
                retryCount = 0; // 重置重试次数
            }
            // 跳过失败则返回416
            if (skipBytes > 0) {
                setResponseStatus(response, HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                response.setHeader("Content-Range", "bytes */" + fileLength);
                log.error("分片字节跳过失败，剩余未跳过：{}，start：{}", skipBytes, start);
                return;
            }

            // 8. 二进制数组分片流式传输
            byte[] buffer = new byte[BUFFER_SIZE];
            long remaining = contentLength;
            while (remaining > 0) {
                int readLen = (int) Math.min(buffer.length, remaining);
                int actualRead = in.read(buffer, 0, readLen);
                if (actualRead == -1) {
                    log.warn("视频流读取提前结束，剩余未传输：{}", remaining);
                    break;
                }
                // 安全写入：解决连接中断写报错
                writeWithExceptionHandle(out, buffer, 0, actualRead);
                remaining -= actualRead;
            }
            flushWithExceptionHandle(out);

        } catch (Exception e) {
            // 非连接中断的核心异常，记ERROR并返回500
            log.error("视频预览核心异常，路径：{}，异常信息：{}", imgPath, e.getMessage(), e);
            setResponseStatus(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            // 解密失败删除Redis脏缓存【原生连接操作】
            if (redisKey != null && "true".equals(isEncryption)) {
                deleteRedisKey(redisKey);
                log.info("清理Redis脏缓存，key：{}", redisKey);
            }
        } finally {
            // 仅关闭流，无任何Redis序列化器还原操作（根本没修改过）
            closeStream(in);
            closeStream(out);
        }
    }

    // ===================== 核心：Redis原生连接操作方法，全程不修改全局序列化器 =====================
    /**
     * 自定义Redis获取值：直接用原生连接，手动指定序列化器
     */
    private byte[] getRedisValue(String redisKey) {
        try (RedisConnection connection = redisTemplate.getConnectionFactory().getConnection()) {
            // 字符串序列化key，JDK序列化value
            byte[] keyBytes = stringSerializer.serialize(redisKey);
            if (keyBytes == null) {
                return null;
            }
            byte[] valueBytes = connection.get(keyBytes);
            if (valueBytes == null || valueBytes.length == 0) {
                return null;
            }
            // JDK反序列化为字节数组
            return (byte[]) jdkSerializer.deserialize(valueBytes);
        } catch (Exception e) {
            log.error("Redis获取值失败，key：{}，异常：{}", redisKey, e.getMessage());
            return null;
        }
    }

    /**
     * 自定义Redis设置值：直接用原生连接，手动指定序列化器+设置过期时间
     */
    private void setRedisValue(String redisKey, byte[] value, long expireSeconds) {
        try (RedisConnection connection = redisTemplate.getConnectionFactory().getConnection()) {
            byte[] keyBytes = stringSerializer.serialize(redisKey);
            byte[] valueBytes = jdkSerializer.serialize(value);
            if (keyBytes == null || valueBytes == null) {
                log.warn("Redis序列化key/value失败，key：{}", redisKey);
                return;
            }
            // 设置值+过期时间
            connection.set(keyBytes, valueBytes);
            connection.expire(keyBytes, expireSeconds);
        } catch (Exception e) {
            log.error("Redis设置值失败，key：{}，异常：{}", redisKey, e.getMessage());
        }
    }

    /**
     * 自定义Redis删除key：直接用原生连接
     */
    private void deleteRedisKey(String redisKey) {
        try (RedisConnection connection = redisTemplate.getConnectionFactory().getConnection()) {
            byte[] keyBytes = stringSerializer.serialize(redisKey);
            if (keyBytes != null) {
                connection.del(keyBytes);
            }
        } catch (Exception e) {
            log.error("Redis删除key失败，key：{}，异常：{}", redisKey, e.getMessage());
        }
    }

    // ===================== 原有工具方法，保留并优化 =====================
    /**
     * 安全设置响应状态码：避免响应已提交抛出异常
     */
    private void setResponseStatus(HttpServletResponse response, int status) {
        if (!response.isCommitted()) {
            response.setStatus(status);
        }
    }

    /**
     * 安全写入字节数组 - 捕获连接中断类异常，仅记WARN不抛出
     */
    private void writeWithExceptionHandle(OutputStream out, byte[] buffer, int off, int len) {
        try {
            out.write(buffer, off, len);
        } catch (IOException e) {
            String errMsg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
            if (errMsg.contains("软件中止了一个已建立的连接") || errMsg.contains("connection reset")
                    || errMsg.contains("broken pipe") || errMsg.contains("socket closed")) {
                //log.warn("客户端主动断开连接，写入数据失败：{}", e.getMessage());
            } else {
                log.error("流写入异常：{}", e.getMessage(), e);
            }
        }
    }

    /**
     * 安全刷新缓冲区 - 捕获连接中断类异常
     */
    private void flushWithExceptionHandle(OutputStream out) {
        try {
            if (out != null) {
                out.flush();
            }
        } catch (IOException e) {
            String errMsg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
            if (errMsg.contains("软件中止了一个已建立的连接") || errMsg.contains("connection reset")
                    || errMsg.contains("broken pipe") || errMsg.contains("socket closed")) {
                //log.warn("客户端主动断开连接，刷新缓冲区失败：{}", e.getMessage());
            } else {
                log.error("流刷新异常：{}", e.getMessage(), e);
            }
        }
    }

    /**
     * 通用流关闭方法 - 无异常关闭，确保资源释放
     */
    private void closeStream(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                //log.warn("流关闭异常：{}", e.getMessage());
            }
        }
    }

    /**
     * 提取URL中匹配后的路径参数，兼容中文/特殊字符
     */
    private static String extractPathFromPattern(final HttpServletRequest request) {
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String bestMatchPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (oConvertUtils.isEmpty(path) || oConvertUtils.isEmpty(bestMatchPattern)) {
            return null;
        }
        return new AntPathMatcher().extractPathWithinPattern(bestMatchPattern, path);
    }
}
