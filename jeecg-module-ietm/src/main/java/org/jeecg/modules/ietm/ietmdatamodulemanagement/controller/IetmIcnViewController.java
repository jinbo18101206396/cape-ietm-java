package org.jeecg.modules.ietm.ietmdatamodulemanagement.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.ietm.common.util.MimeTypeDetector;
import org.jeecg.modules.ietm.icnmanage.entity.IetmIcnManage;
import org.jeecg.modules.ietm.icnmanage.service.IIetmIcnManageService;
import org.jeecg.modules.ietm.icnmanage.vo.MultimediaInfoVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * ICN资源查看Controller - DM预览专用
 *
 * 设计目的：
 * - 为DM预览功能提供统一的ICN资源访问接口
 * - 路径简洁（/ietm/icn），符合RESTful设计
 * - 不影响现有的ICN管理模块（/icnmanage/ietmIcnManage）
 *
 * 使用场景：
 * - DmXmlHelper/DmXsltTransformer中的多媒体标签替换
 * - DM预览弹窗中的图片/视频/音频显示
 *
 * 技术特性：
 * - 支持HTTP Range请求（视频拖动）
 * - MIME类型自动检测
 * - 优雅降级（SVG占位图）
 *
 * @author IETM Team
 * @date 2026-09-18
 * @version 1.0
 */
@Api(tags = "ICN资源查看-DM预览专用")
@RestController
@RequestMapping("/ietm/icn")
@Slf4j
public class IetmIcnViewController {

    @Autowired
    private IIetmIcnManageService icnManageService;

    @Autowired
    private org.jeecg.modules.ietm.icnmanage.service.IIetmIcnFormulaService icnFormulaService;

    /**
     * 获取ICN多媒体元数据
     *
     * 返回MIME类型、文件大小、是否视频/音频等信息，
     * 用于前端判断如何渲染播放器（video/audio/img标签）
     *
     * 使用场景：DmPreviewModal.vue中点击多媒体图标时先调用此接口
     *
     * @param icnCode ICN完整编码（如：ICN-ZB1-A-02-60101-30101-00001-A-001-01）
     * @return 多媒体元数据（mimeType, fileSize, isVideo, isAudio, isImage等）
     */
    @ApiOperation(value = "获取ICN多媒体元数据", notes = "返回ICN的MIME类型、文件大小等信息，用于前端判断渲染方式")
    @GetMapping("/metadata/{icnCode}")
    public Result<MultimediaInfoVO> getMetadata(@PathVariable("icnCode") String icnCode) {
        try {
            // 1. 根据ICN编码查询实体
            // 🔧 2026-10-10 修复：改用.list()而非.getOne()，避免多条记录时抛异常
            QueryWrapper<IetmIcnManage> qw = new QueryWrapper<>();
            qw.eq("icn", icnCode);
            qw.orderByDesc("create_time");
            qw.last("LIMIT 1");
            List<IetmIcnManage> icnList = icnManageService.list(qw);

            if (icnList == null || icnList.isEmpty()) {
                log.warn("ICN不存在: {}", icnCode);
                return Result.error("ICN不存在: " + icnCode);
            }

            IetmIcnManage icn = icnList.get(0);

            // 2. 加载附件信息
            if (!icnManageService.loadAttachment(icn)) {  // 🔧 修复P3：检查加载结果
                log.warn("ICN附件缺失: {}", icnCode);
                return Result.error("ICN附件缺失");
            }

            if (icn.getIetmAttachment().getFileKey() == null) {
                log.warn("ICN附件缺失: {}", icnCode);
                return Result.error("ICN附件缺失");
            }

            // 3. 读取文件内容
            String fileKey = icn.getIetmAttachment().getFileKey();
            String fileName = icn.getIetmAttachment().getFileName();
            String fileStorageLocation = System.getProperty("user.dir") + "/upload/files/";
            java.io.File file = new java.io.File(fileStorageLocation, fileKey);

            if (!file.exists()) {
                log.warn("ICN文件不存在: fileKey={}, path={}", fileKey, file.getAbsolutePath());
                return Result.error("文件不存在");
            }

            byte[] fileContent = java.nio.file.Files.readAllBytes(file.toPath());

            // 4. 检测MIME类型（使用魔数检测，支持18种格式）
            String mimeType = MimeTypeDetector.detect(fileContent);

            // 5. 构建返回对象
            MultimediaInfoVO info = new MultimediaInfoVO();
            info.setIcnCode(icnCode);
            info.setMimeType(mimeType); // 自动设置isVideo/isAudio/isImage
            info.setFileSize((long) fileContent.length);
            info.setFileName(fileName);

            log.debug("获取ICN元数据成功: icnCode={}, mimeType={}, size={}",
                     icnCode, mimeType, fileContent.length);

            return Result.OK(info);

        } catch (Exception e) {
            log.error("获取ICN元数据失败: icnCode={}", icnCode, e);
            return Result.error("获取元数据失败: " + e.getMessage());
        }
    }

    /**
     * 查看ICN图片/视频/音频 - DM预览专用接口
     *
     * 使用场景：
     * - DM预览弹窗中的<img>/<video>/<audio>标签的src属性
     * - 调用路径：DmPreviewModal.vue → /ietm/icn/view/{icnCode}
     *
     * 设计特性：
     * - 用户体验优先：返回占位图避免显示破损图标
     * - 错误友好：捕获所有异常
     * - 基础缓存：Cache-Control 1天
     * - Range请求支持：视频拖动播放必需（HTTP 206 Partial Content）
     *
     * 技术实现：
     * 1. 根据ICN编码查询实体和附件
     * 2. 读取文件内容
     * 3. MIME魔数检测（18种格式）
     * 4. 检测Range请求头，按需返回文件片段或完整文件
     * 5. 失败时返回SVG占位图
     *
     * @param icnCode ICN完整编码
     * @param request HTTP请求（用于读取Range头）
     * @param response HTTP响应
     */
    @ApiOperation(value = "查看ICN图片/视频/音频",
                  notes = "用于DM预览中显示图形/视频/音频，根据ICN编码返回文件流，支持Range请求（视频拖动）。支持公式ICN（从ietm_icn_formula表加载）")
    @GetMapping(value = "/view/{icnCode}")
    public void viewIcn(@PathVariable("icnCode") String icnCode,
                        HttpServletRequest request,
                        HttpServletResponse response) {
        try {
            // 🔧 2026-10-15 修复P0：公式预览加载失败
            // 根因：公式ICN存储在独立的ietm_icn_formula表（绕过DM8聚集索引限制），
            //      但预览接口只查询普通的ietm_icn_manage表，导致"ICN不存在"
            // 修复：优先从ietm_icn_formula表查询，如果是公式ICN则从filecontent字段
            //      提取base64图片返回；否则走原有的附件流程

            // 1. 优先查询公式ICN（独立表）
            org.jeecg.modules.ietm.icnmanage.entity.IetmIcnFormula formulaEntity =
                icnFormulaService.getByIcnWithContent(icnCode);

            if (formulaEntity != null && formulaEntity.getFilecontent() != null && formulaEntity.getFilecontent().length > 0) {
                // 公式ICN：从HTML中提取base64图片
                String formulaHtml = new String(formulaEntity.getFilecontent(), java.nio.charset.StandardCharsets.UTF_8);

                // 提取base64图片数据（格式：<img src="data:image/png;base64,iVBORw0KG..." .../>）
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("src=\"(data:image/png;base64,[^\"]+)\"");
                java.util.regex.Matcher matcher = pattern.matcher(formulaHtml);

                if (matcher.find()) {
                    String base64Data = matcher.group(1);
                    // 去掉data URL前缀，只保留base64编码
                    String base64Image = base64Data.substring(base64Data.indexOf(',') + 1);
                    byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Image);

                    // 返回PNG图片
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType("image/png");
                    response.setContentLength(imageBytes.length);
                    response.setHeader("Content-Disposition", "inline; filename=\"formula.png\"");
                    response.setHeader("Cache-Control", "public, max-age=86400");

                    response.getOutputStream().write(imageBytes);
                    response.getOutputStream().flush();

                    log.info("公式ICN预览成功: icnCode={}, 图片大小={}KB", icnCode, imageBytes.length / 1024);
                    return;
                } else {
                    log.warn("公式ICN无法提取base64图片: icnCode={}", icnCode);
                    sendPlaceholderImage(response, "公式图片格式错误");
                    return;
                }
            }

            // 2. 查询普通ICN（图符/视频/音频）
            QueryWrapper<IetmIcnManage> qw = new QueryWrapper<>();
            qw.eq("icn", icnCode);
            qw.orderByDesc("create_time");
            qw.orderByDesc("id");  // 🔧 修复P1：添加secondary sort，确定排序顺序
            qw.last("LIMIT 1");  // 🔧 修复P1：仅取1条记录
            List<IetmIcnManage> icnList = icnManageService.list(qw);

            if (icnList == null || icnList.isEmpty()) {
                log.warn("ICN不存在: {}", icnCode);
                sendPlaceholderImage(response, "ICN不存在");
                return;
            }

            IetmIcnManage icn = icnList.get(0);

            // 3. 加载附件信息
            if (!icnManageService.loadAttachment(icn)) {  // 🔧 修复P3：检查加载结果
                log.warn("ICN附件缺失: {}", icnCode);
                sendPlaceholderImage(response, "图片文件缺失");
                return;
            }

            if (icn.getIetmAttachment().getFileKey() == null) {
                log.warn("ICN附件缺失: {}", icnCode);
                sendPlaceholderImage(response, "图片文件缺失");
                return;
            }

            // 4. 读取文件内容
            String fileKey = icn.getIetmAttachment().getFileKey();
            String fileName = icn.getIetmAttachment().getFileName();
            String fileStorageLocation = System.getProperty("user.dir") + "/upload/files/";
            java.io.File file = new java.io.File(fileStorageLocation, fileKey);

            if (!file.exists()) {
                log.warn("ICN文件不存在: fileKey={}, path={}", fileKey, file.getAbsolutePath());
                sendPlaceholderImage(response, "文件不存在");
                return;
            }

            byte[] fileContent = java.nio.file.Files.readAllBytes(file.toPath());

            // 5. MIME类型检测（使用魔数，支持18种格式）
            String mimeType = MimeTypeDetector.detect(fileContent);
            log.debug("ICN: {}, 文件名: {}, 检测到MIME类型: {}, 文件大小: {} bytes",
                     icnCode, fileName, mimeType, fileContent.length);

            // 6. 检测Range请求头
            String rangeHeader = request.getHeader("Range");

            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                // 处理Range请求（视频拖动场景）
                handleRangeRequest(fileContent, mimeType, fileName, rangeHeader, response);
            } else {
                // 处理普通请求（图片场景）
                handleFullRequest(fileContent, mimeType, fileName, response);
            }

        } catch (Exception e) {
            log.error("查看ICN图片失败: icnCode={}", icnCode, e);
            try {
                sendPlaceholderImage(response, "加载失败");
            } catch (Exception ex) {
                log.error("发送占位图失败", ex);
            }
        }
    }

    /**
     * 处理普通请求（完整文件）
     *
     * 适用场景：图片、小文件
     * 返回状态码：200 OK
     *
     * @param fileContent 文件内容
     * @param mimeType MIME类型
     * @param fileName 文件名
     * @param response HTTP响应
     */
    private void handleFullRequest(byte[] fileContent, String mimeType, String fileName,
                                    HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(mimeType);
        response.setContentLength(fileContent.length);
        response.setHeader("Content-Disposition", "inline; filename=\"" + fileName + "\"");
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("Cache-Control", "public, max-age=86400");

        response.getOutputStream().write(fileContent);
        response.getOutputStream().flush();

        log.debug("完整文件请求: {} size={}KB", fileName, fileContent.length / 1024);
    }

    /**
     * 处理Range请求（视频拖动场景）
     *
     * Range格式示例：
     * - bytes=0-1023      : 请求前1024字节
     * - bytes=1024-       : 请求从1024到文件末尾
     * - bytes=-1024       : 请求最后1024字节
     *
     * 适用场景：
     * - 视频拖动进度条（浏览器自动发起Range请求）
     * - 音频快进/快退
     * - 大文件断点续传
     *
     * 返回状态码：206 Partial Content
     *
     * @param fileContent 文件内容
     * @param mimeType MIME类型
     * @param fileName 文件名
     * @param rangeHeader Range请求头（如：bytes=0-1023）
     * @param response HTTP响应
     */
    private void handleRangeRequest(byte[] fileContent, String mimeType, String fileName,
                                     String rangeHeader, HttpServletResponse response) throws Exception {
        long fileLength = fileContent.length;

        // 解析Range头（格式：bytes=start-end）
        String rangeValue = rangeHeader.replace("bytes=", "");
        String[] ranges = rangeValue.split("-");

        long start = 0;
        long end = fileLength - 1;

        try {
            if (ranges[0].isEmpty()) {
                // bytes=-1024：最后N字节
                start = fileLength - Long.parseLong(ranges[1]);
            } else {
                start = Long.parseLong(ranges[0]);
                if (ranges.length > 1 && !ranges[1].isEmpty()) {
                    end = Long.parseLong(ranges[1]);
                }
            }

            // 边界校验
            if (start < 0) start = 0;
            if (end >= fileLength) end = fileLength - 1;
            if (start > end) {
                response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                response.setHeader("Content-Range", "bytes */" + fileLength);
                log.warn("无效的Range范围: start={}, end={}, fileLength={}", start, end, fileLength);
                return;
            }

        } catch (NumberFormatException e) {
            log.warn("无效的Range头格式: {}", rangeHeader);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        long contentLength = end - start + 1;

        // 设置206 Partial Content响应
        response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        response.setContentType(mimeType);
        response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
        response.setHeader("Content-Length", String.valueOf(contentLength));
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("Cache-Control", "public, max-age=86400");

        // 写入指定范围的数据
        response.getOutputStream().write(fileContent, (int) start, (int) contentLength);
        response.getOutputStream().flush();

        log.debug("Range请求: {} bytes={}-{}/{} ({}KB)", fileName, start, end, fileLength, contentLength / 1024);
    }

    /**
     * 发送占位图（当ICN不存在或加载失败时）
     *
     * 使用SVG格式，避免显示浏览器的破损图标，提升用户体验
     *
     * @param response HTTP响应
     * @param message 错误消息
     */
    private void sendPlaceholderImage(HttpServletResponse response, String message) throws Exception {
        response.setContentType("image/svg+xml");
        response.setCharacterEncoding("UTF-8");

        // 生成一个简单的SVG占位图
        String svg = String.format(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"300\" height=\"200\" viewBox=\"0 0 300 200\">" +
            "  <rect width=\"300\" height=\"200\" fill=\"#f0f0f0\" stroke=\"#ccc\" stroke-width=\"2\"/>" +
            "  <text x=\"150\" y=\"100\" text-anchor=\"middle\" font-family=\"Arial\" font-size=\"14\" fill=\"#666\">" +
            "    %s" +
            "  </text>" +
            "</svg>",
            escapeXml(message)
        );

        response.getOutputStream().write(svg.getBytes("UTF-8"));
        response.getOutputStream().flush();
    }

    /**
     * XML转义（用于SVG文本）
     *
     * @param text 原始文本
     * @return 转义后的文本
     */
    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }

    /**
     * 公式保存端点（对标旧系统的 platform/ietm/csdb/ietmicn/IetmIcnController/operation/save）
     *
     * 方案B：使用独立的ietm_icn_formula表（绕过DM8聚集索引限制）
     * - ietm_icn_manage表有CLUSTER索引，无法添加CLOB字段
     * - ietm_icn_formula表无CLUSTER索引，可以使用CLOB存储公式HTML
     * - 对现有ICN功能（图符管理）零影响
     *
     * 接收参数格式：{data: JSON.stringify({icn, sns, rpc, originator, uniqueid, variantcode, securityclassification, issueno, secretLevel, cmnodeid, filename, filecontent})}
     */
    @PostMapping("/save-formula")
    @ApiOperation(value = "保存公式ICN", notes = "将UEditor中的公式保存为ICN，filecontent存储完整的<img>标签HTML到独立表ietm_icn_formula")
    public Result<?> saveFormula(@RequestBody java.util.Map<String, String> params) {
        try {
            String dataJson = params.get("data");
            if (dataJson == null || dataJson.isEmpty()) {
                return Result.error("参数data不能为空");
            }

            // 解析JSON为FormulaIcnVO
            org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.FormulaIcnVO vo =
                com.alibaba.fastjson.JSON.parseObject(dataJson,
                    org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.FormulaIcnVO.class);

            // 🔍 2026-10-14 调试日志：检查filecontent是HTML还是十六进制
            String filecontent = vo.getFilecontent();
            log.info("[saveFormula] 接收到的filecontent前100字符: {}",
                filecontent != null && filecontent.length() > 100 ? filecontent.substring(0, 100) : filecontent);
            log.info("[saveFormula] filecontent是否以<img开头: {}", filecontent != null && filecontent.startsWith("<img"));

            // 关键：filecontent存储完整的<img>标签HTML（包含base64图片和LaTeX源码）
            byte[] filecontentBytes = filecontent.getBytes(java.nio.charset.StandardCharsets.UTF_8);

            // 创建公式ICN实体（使用独立表）
            org.jeecg.modules.ietm.icnmanage.entity.IetmIcnFormula entity =
                new org.jeecg.modules.ietm.icnmanage.entity.IetmIcnFormula();
            entity.setIcn(vo.getIcn());
            entity.setSns(vo.getSns());
            entity.setRpc(vo.getRpc());
            entity.setOriginator(vo.getOriginator());
            entity.setUniqueId(vo.getUniqueid());
            entity.setVariantCode(vo.getVariantcode());
            entity.setSecurityClassification(vo.getSecurityclassification());
            entity.setIssueNo(vo.getIssueno());
            entity.setSecurity(Integer.parseInt(vo.getSecretLevel()));
            entity.setCmNodeId(vo.getCmnodeid());
            entity.setFilecontent(filecontentBytes);  // 存储完整的HTML到CLOB
            entity.setIcnType("formula");  // 标识为公式类型
            entity.setVersion("1");
            entity.setIspublished("0");
            entity.setIsdeleted("0");
            entity.setCreateTime(new java.util.Date());

            // 🔧 修复P0: 从LoginUser获取当前用户名
            try {
                org.jeecg.common.system.vo.LoginUser loginUser =
                    (org.jeecg.common.system.vo.LoginUser) org.apache.shiro.SecurityUtils.getSubject().getPrincipal();
                if (loginUser != null) {
                    entity.setCreateBy(loginUser.getUsername());
                } else {
                    log.warn("[saveFormula] LoginUser为null，使用默认值");
                    entity.setCreateBy("system");
                }
            } catch (Exception e) {
                log.error("[saveFormula] 获取用户名失败，使用默认值", e);
                entity.setCreateBy("system");
            }

            // 保存到独立表ietm_icn_formula
            icnFormulaService.save(entity);

            log.info("[saveFormula] 公式保存成功（独立表）- ICN: {}, UniqueId: {}", vo.getIcn(), vo.getUniqueid());
            return Result.OK("公式保存成功");
        } catch (Exception e) {
            log.error("[saveFormula] 公式保存失败", e);
            return Result.error("公式保存失败：" + e.getMessage());
        }
    }

    /**
     * 公式加载端点（对标旧系统的 platform/ietm/csdb/ietmicn/IetmIcnController/operation/getIcnContent）
     *
     * 方案B：优先从独立的ietm_icn_formula表查询
     * - 如果是公式ICN，返回 {formula: "<img class=\"kfformula\"..."}
     * - 否则返回普通附件信息（从ietm_icn_manage + ietm_attachment查询）
     */
    @PostMapping("/getIcnContent")
    @ApiOperation(value = "获取ICN内容", notes = "如果是公式ICN，从独立表ietm_icn_formula返回完整HTML；否则返回附件信息")
    public Result<?> getIcnContent(@RequestParam String icn) {
        try {
            java.util.Map<String, Object> result = new java.util.HashMap<>();

            // 1. 优先从独立的公式表查询
            org.jeecg.modules.ietm.icnmanage.entity.IetmIcnFormula formulaEntity =
                icnFormulaService.getByIcnWithContent(icn);

            if (formulaEntity != null && formulaEntity.getFilecontent() != null && formulaEntity.getFilecontent().length > 0) {
                // 🔧 2026-10-14 修复P0: byte[]直接放入Map会被JSON序列化成十六进制字符串
                // 必须先转成String再放入result，否则前端收到的是hex而不是HTML
                String formulaHtml = new String(formulaEntity.getFilecontent(), java.nio.charset.StandardCharsets.UTF_8);
                result.put("formula", formulaHtml);  // 关键：必须是String类型，不能是byte[]
                log.info("[getIcnContent] 公式加载成功（独立表）- ICN: {}, HTML长度: {}", icn, formulaHtml.length());
                return Result.OK(result);
            }

            // 2. 查询普通ICN（图符）
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<IetmIcnManage> qw =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            qw.eq("icn", icn);
            qw.orderByDesc("create_time");
            qw.last("LIMIT 1");
            java.util.List<IetmIcnManage> icnList = icnManageService.list(qw);

            if (icnList == null || icnList.isEmpty()) {
                log.warn("[getIcnContent] ICN不存在 - ICN: {}", icn);
                return Result.error("ICN不存在");
            }

            IetmIcnManage entity = icnList.get(0);

            // 3. 加载附件信息（普通图符）
            if (icnManageService.loadAttachment(entity)) {
                result.put("dto", entity);
                log.info("[getIcnContent] 附件加载成功 - ICN: {}", icn);
                return Result.OK(result);
            }

            log.warn("[getIcnContent] 未找到ICN内容 - ICN: {}", icn);
            return Result.error("未找到ICN内容");
        } catch (Exception e) {
            log.error("[getIcnContent] 获取ICN内容失败", e);
            return Result.error("获取ICN内容失败：" + e.getMessage());
        }
    }
}
