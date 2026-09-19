package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.ietm.common.util.MimeTypeDetector;
import org.jeecg.modules.ietm.ietmattachment.entity.IetmAttachment;
import org.jeecg.modules.ietm.ietmattachment.mapper.IetmAttachmentMapper;
import org.jeecg.modules.ietm.icnmanage.entity.IetmIcnManage;
import org.jeecg.modules.ietm.icnmanage.mapper.IetmIcnManageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ICN批量加载工具类
 *
 * 设计原则：复用DdnPackageBuilder的批量加载模式
 * 用于DM预览场景的性能优化，避免N+1查询问题
 *
 * 性能优化：
 * - 批量查询ICN实体（1次数据库查询）
 * - 批量查询附件（1次数据库查询）
 * - 并行读取文件（parallelStream）
 *
 * 复用组件：
 * - MyBatis Plus批量查询（DdnPackageBuilder模式）
 * - MimeTypeDetector魔数检测（统一工具类）
 * - SVG占位图机制（优雅降级）
 *
 * @author jeecg-boot
 * @since 2026-09-18
 */
@Component
@Slf4j
public class IcnBatchLoader {

    @Autowired
    private IetmIcnManageMapper icnManageMapper;

    @Autowired
    private IetmAttachmentMapper attachmentMapper;

    /**
     * 批量加载ICN的Base64数据
     *
     * 调用路径：DmXmlHelper.replaceIcnPaths() → loadIcnBatch()
     * 使用场景：DM预览时将ICN路径替换为Base64内联数据
     *
     * @param icnCodes ICN编码集合（如：ICN-ZB1-A-02-60101-30101-00001-A-001-01）
     * @return Map<icnCode, base64Data>，格式：data:image/png;base64,iVBORw0KG...
     */
    public Map<String, String> loadIcnBatch(Set<String> icnCodes) {
        if (icnCodes == null || icnCodes.isEmpty()) {
            log.debug("ICN编码集合为空，跳过批量加载");
            return Collections.emptyMap();
        }

        long startTime = System.currentTimeMillis();
        log.debug("🔍 开始批量加载{}个ICN", icnCodes.size());

        try {
            // 1. 批量查询ICN实体（复用DdnPackageBuilder模式）
            List<IetmIcnManage> icns = icnManageMapper.selectList(
                    new LambdaQueryWrapper<IetmIcnManage>()
                            .in(IetmIcnManage::getIcn, icnCodes));

            if (icns.isEmpty()) {
                log.warn("未找到任何ICN实体，请求的编码: {}", icnCodes);
                return Collections.emptyMap();
            }

            log.debug("📦 查询到{}个ICN实体", icns.size());

            // 2. 提取ICN ID集合
            Set<String> icnIds = icns.stream()
                    .map(IetmIcnManage::getId)
                    .collect(Collectors.toSet());

            // 3. 批量查询附件（复用DdnPackageBuilder模式）
            Map<String, List<IetmAttachment>> attachmentMap = batchLoadAttachments(icnIds);
            log.debug("📎 查询到{}个ICN的附件信息", attachmentMap.size());

            // 4. 并行读取文件并转Base64
            String fileStorageLocation = System.getProperty("user.dir") + "/upload/files/";
            log.debug("📂 文件存储路径: {}", fileStorageLocation);

            Map<String, String> result = icns.parallelStream()
                    .collect(Collectors.toMap(
                            IetmIcnManage::getIcn,
                            icn -> loadIcnAsBase64(icn, attachmentMap, fileStorageLocation),
                            (v1, v2) -> {
                                log.warn("发现重复的ICN编码，保留第一个: {}", v1);
                                return v1;
                            }
                    ));

            long duration = System.currentTimeMillis() - startTime;
            int successCount = (int) result.values().stream()
                    .filter(v -> !v.contains("占位图"))
                    .count();

            log.info("✅ ICN批量加载完成: 总数={}, 成功={}, 失败={}, 耗时={}ms",
                    icnCodes.size(), successCount, icnCodes.size() - successCount, duration);

            return result;

        } catch (Exception e) {
            log.error("❌ ICN批量加载失败", e);
            return Collections.emptyMap();
        }
    }

    /**
     * 批量加载附件（完全复用DdnPackageBuilder的实现）
     *
     * @param icnIds ICN实体ID集合
     * @return Map<icnId, List<IetmAttachment>>
     */
    private Map<String, List<IetmAttachment>> batchLoadAttachments(Set<String> icnIds) {
        if (icnIds == null || icnIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<IetmAttachment> attachments = attachmentMapper.selectList(
                new LambdaQueryWrapper<IetmAttachment>()
                        .in(IetmAttachment::getPid, icnIds));

        return attachments.stream()
                .collect(Collectors.groupingBy(IetmAttachment::getPid));
    }

    /**
     * 加载单个ICN为Base64
     *
     * @param icn ICN实体
     * @param attachmentMap 附件映射表
     * @param storageLocation 文件存储根路径
     * @return Base64数据URI（如：data:image/png;base64,iVBORw0KG...）
     */
    private String loadIcnAsBase64(IetmIcnManage icn,
                                    Map<String, List<IetmAttachment>> attachmentMap,
                                    String storageLocation) {
        String icnCode = icn.getIcn();

        try {
            // 1. 获取附件
            List<IetmAttachment> attachments = attachmentMap.get(icn.getId());
            if (attachments == null || attachments.isEmpty()) {
                log.warn("ICN附件缺失: {}", icnCode);
                return getPlaceholderBase64("附件缺失");
            }

            IetmAttachment attachment = attachments.get(0);
            String fileKey = attachment.getFileKey();

            if (fileKey == null || fileKey.trim().isEmpty()) {
                log.warn("ICN附件FileKey为空: {}", icnCode);
                return getPlaceholderBase64("文件Key缺失");
            }

            // 2. 读取文件
            File file = new File(storageLocation, fileKey);
            if (!file.exists()) {
                log.warn("ICN文件不存在: icn={}, fileKey={}, path={}",
                        icnCode, fileKey, file.getAbsolutePath());
                return getPlaceholderBase64("文件不存在");
            }

            // 3. 读取文件内容
            byte[] fileContent = Files.readAllBytes(file.toPath());

            // 4. 检测MIME类型（复用现有工具类）
            String mimeType = MimeTypeDetector.detect(fileContent);

            // 5. 转Base64
            String base64 = Base64.getEncoder().encodeToString(fileContent);

            log.debug("✓ 加载ICN成功: icn={}, mime={}, size={}KB",
                    icnCode, mimeType, fileContent.length / 1024);

            return "data:" + mimeType + ";base64," + base64;

        } catch (Exception e) {
            log.error("加载ICN失败: icn={}", icnCode, e);
            return getPlaceholderBase64("加载失败");
        }
    }

    /**
     * 获取占位图Base64（复用Controller的SVG逻辑）
     *
     * 当ICN不存在、附件缺失、文件读取失败时使用占位图
     * 优雅降级，避免显示破损图标，提升用户体验
     *
     * @param message 占位图显示的提示文本
     * @return SVG占位图的Base64编码
     */
    private String getPlaceholderBase64(String message) {
        // SVG占位图：300x200灰色矩形 + 提示文字
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

        String base64 = Base64.getEncoder().encodeToString(svg.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return "data:image/svg+xml;base64," + base64;
    }

    /**
     * XML转义（用于SVG文本）
     *
     * @param text 原始文本
     * @return 转义后的文本
     */
    private String escapeXml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
