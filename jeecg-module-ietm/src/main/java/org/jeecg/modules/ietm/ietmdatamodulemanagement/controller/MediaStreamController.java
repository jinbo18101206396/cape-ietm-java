package org.jeecg.modules.ietm.ietmdatamodulemanagement.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.modules.ietm.common.IetmConstants;
import org.jeecg.modules.ietm.icnmanage.entity.IetmIcnManage;
import org.jeecg.modules.ietm.icnmanage.service.IIetmIcnManageService;
import org.jeecg.modules.ietm.ietmattachment.entity.IetmAttachment;
import org.jeecg.modules.ietm.ietmattachment.service.IIetmAttachmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 多媒体流式传输Controller
 *
 * 功能：提供视频/音频的流式播放API，支持：
 * 1. HTTP缓存（ETag + Cache-Control）
 * 2. 安全路径校验（防止路径遍历）
 * 3. MIME类型检测（基于文件头）
 * 4. 断点续传（HTTP Range，未来扩展）
 *
 * S1000D 4.0符合性：
 * - 支持multimediaType="video"
 * - 支持multimediaType="audio"
 * - 支持infoEntityIdent引用机制
 *
 * @author claude
 * @date 2026-09-18
 */
@Api(tags = "多媒体流式传输")
@RestController
@RequestMapping("/ietm/attachment")
@Slf4j
public class MediaStreamController {

    @Autowired
    private IIetmIcnManageService icnService;

    @Autowired
    private IIetmAttachmentService attachmentService;

    /**
     * ICN文件存储位置（从配置读取）
     */
    @Value("${accessFile.icnLocation:D:\\workspace\\IETM\\file\\icn}")
    private String icnFileStorageLocation;

    /**
     * 流式传输多媒体文件
     *
     * @param imfCode ICN/IMF编码（如：ICN-001、IMF-VIDEO-001）
     * @param ifNoneMatch 客户端ETag（用于304缓存检查）
     * @return 文件流或304 Not Modified
     */
    @ApiOperation(value = "流式传输多媒体文件", notes = "支持视频/音频的流式播放，自动缓存")
    @GetMapping("/stream/{imfCode}")
    @RequiresPermissions("ietm:datamodule:preview")
    public ResponseEntity<Resource> streamMedia(
            @PathVariable String imfCode,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {

        try {
            log.info("流式传输请求: imfCode={}, ifNoneMatch={}", imfCode, ifNoneMatch);

            // 1. 查询ICN实体（使用与loadIcnAsBase64相同的逻辑）
            QueryWrapper<IetmIcnManage> icnQw = new QueryWrapper<>();
            icnQw.eq("icn", imfCode);
            icnQw.orderByDesc("create_time");

            List<IetmIcnManage> icnList = icnService.list(icnQw);

            if (icnList == null || icnList.isEmpty()) {
                log.warn("ICN不存在: imfCode={}", imfCode);
                return ResponseEntity.notFound().build();
            }

            // 2. 查询附件（使用pid关联）
            IetmIcnManage icn = null;
            List<IetmAttachment> attachments = null;

            for (IetmIcnManage icnRecord : icnList) {
                QueryWrapper<IetmAttachment> attachmentQw = new QueryWrapper<>();
                attachmentQw.eq("pid", icnRecord.getId());
                attachmentQw.eq("file_type", IetmConstants.FILE_TYPE_ENTITY);
                attachmentQw.orderByAsc("create_time");

                List<IetmAttachment> foundAttachments = attachmentService.list(attachmentQw);

                if (foundAttachments != null && !foundAttachments.isEmpty()) {
                    icn = icnRecord;
                    attachments = foundAttachments;
                    break;
                }
            }

            if (icn == null || attachments == null || attachments.isEmpty()) {
                log.warn("ICN没有关联附件: imfCode={}", imfCode);
                return ResponseEntity.notFound().build();
            }

            // 3. 获取第一个附件的文件信息
            IetmAttachment attachment = attachments.get(0);
            String fileKey = attachment.getFileKey();

            if (fileKey == null || fileKey.isEmpty()) {
                log.warn("附件fileKey为空: imfCode={}", imfCode);
                return ResponseEntity.notFound().build();
            }

            // 4. 提取文件名（与loadIcnAsBase64一致）
            String extractedFileName = fileKey;
            if (fileKey.contains(File.separator)) {
                extractedFileName = fileKey.substring(fileKey.lastIndexOf(File.separator) + 1);
            } else if (fileKey.contains("/")) {
                extractedFileName = fileKey.substring(fileKey.lastIndexOf("/") + 1);
            }

            // 5. 构建完整文件路径
            Path filePath = Paths.get(icnFileStorageLocation, extractedFileName).normalize();

            log.info("多媒体文件路径: imfCode={}, fileKey={}, extractedFileName={}, fullPath={}",
                    imfCode, fileKey, extractedFileName, filePath.toAbsolutePath());

            // 6. 安全校验：防止路径遍历攻击
            Path allowedDir = Paths.get(icnFileStorageLocation).normalize();
            if (!filePath.startsWith(allowedDir)) {
                log.error("安全威胁：路径遍历尝试: imfCode={}, filePath={}, allowedDir={}",
                        imfCode, filePath, allowedDir);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // 7. 检查文件是否存在
            if (!Files.exists(filePath)) {
                log.warn("文件不存在: imfCode={}, path={}", imfCode, filePath.toAbsolutePath());
                return ResponseEntity.notFound().build();
            }

            // 8. 检测MIME类型（基于文件头）
            String mimeType = detectMimeTypeSafely(filePath);
            if (mimeType == null || "application/octet-stream".equals(mimeType)) {
                log.warn("未知或不支持的MIME类型: imfCode={}, mimeType={}", imfCode, mimeType);
                // 降级为下载
                mimeType = "application/octet-stream";
            }

            // 9. 生成ETag（基于文件修改时间和大小）
            String etag = generateETag(filePath);

            // 10. 304缓存检查
            if (etag.equals(ifNoneMatch)) {
                log.debug("304 Not Modified: imfCode={}, etag={}", imfCode, etag);
                return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                        .eTag(etag)
                        .build();
            }

            // 11. 流式返回文件
            Resource resource = new FileSystemResource(filePath.toFile());

            log.info("流式传输成功: imfCode={}, mimeType={}, size={}, etag={}",
                    imfCode, mimeType, Files.size(filePath), etag);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(mimeType))
                    .contentLength(Files.size(filePath))
                    .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                    .eTag(etag)
                    .body(resource);

        } catch (Exception e) {
            log.error("流式传输失败: imfCode={}", imfCode, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 安全检测MIME类型（基于文件头Magic Number）
     */
    private String detectMimeTypeSafely(Path filePath) {
        try {
            // 方法1: 使用Java NIO的文件类型探测（基于文件头）
            String mimeType = Files.probeContentType(filePath);

            if (mimeType != null && isAllowedMimeType(mimeType)) {
                return mimeType;
            }

            // 方法2: 读取文件头手动判断（备用）
            byte[] header = new byte[12];
            try (java.io.InputStream is = Files.newInputStream(filePath)) {
                int bytesRead = is.read(header);
                if (bytesRead < 4) {
                    return "application/octet-stream";
                }
            }

            // MP4: 0x00 0x00 0x00 [size] 0x66 0x74 0x79 0x70
            if (header[4] == 0x66 && header[5] == 0x74 && header[6] == 0x79 && header[7] == 0x70) {
                return "video/mp4";
            }

            // MP3: 0xFF 0xFB 或 ID3标签 0x49 0x44 0x33
            if ((header[0] == (byte) 0xFF && header[1] == (byte) 0xFB) ||
                    (header[0] == 0x49 && header[1] == 0x44 && header[2] == 0x33)) {
                return "audio/mpeg";
            }

            // WAV: RIFF 0x52 0x49 0x46 0x46
            if (header[0] == 0x52 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x46) {
                return "audio/wav";
            }

            // WebM: 0x1A 0x45 0xDF 0xA3
            if (header[0] == 0x1A && header[1] == 0x45 && header[2] == (byte) 0xDF && header[3] == (byte) 0xA3) {
                return "video/webm";
            }

            // AVI: RIFF...AVI
            if (header[0] == 0x52 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x46 &&
                    header[8] == 0x41 && header[9] == 0x56 && header[10] == 0x49) {
                return "video/x-msvideo";
            }

            log.warn("未识别的文件头: {}", bytesToHex(header));
            return "application/octet-stream";

        } catch (IOException e) {
            log.error("MIME类型检测失败: {}", filePath, e);
            return "application/octet-stream";
        }
    }

    /**
     * 白名单校验（S1000D允许的多媒体类型）
     */
    private boolean isAllowedMimeType(String mimeType) {
        String[] allowed = {
                "video/mp4", "video/webm", "video/ogg", "video/x-msvideo",
                "audio/mpeg", "audio/wav", "audio/ogg", "audio/mp3"
        };

        for (String type : allowed) {
            if (type.equalsIgnoreCase(mimeType)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 生成ETag（基于文件修改时间和大小）
     */
    private String generateETag(Path filePath) throws IOException {
        long lastModified = Files.getLastModifiedTime(filePath).toMillis();
        long fileSize = Files.size(filePath);
        return String.format("\"%d-%d\"", lastModified, fileSize);
    }

    /**
     * 字节数组转16进制字符串（用于调试）
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(bytes.length, 12); i++) {
            sb.append(String.format("%02X ", bytes[i]));
        }
        return sb.toString().trim();
    }
}
