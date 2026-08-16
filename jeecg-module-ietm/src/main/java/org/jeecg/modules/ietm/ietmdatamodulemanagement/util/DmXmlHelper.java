package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDataModule;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.DmValidateItemVO;
import org.springframework.core.io.ClassPathResource;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * DM XML 解析/校验/渲染工具类（需求 §11/§17.5/§18）
 */
@Slf4j
public class DmXmlHelper {

    private static final String RES_BASE = "ietm/";

    /**
     * 路径片段安全校验：拒绝含 .. / \ 的值，防止 classpath 内路径遍历。
     * 合法值形如 "S1000D4.0" / "descript.xsd"，均不含这些字符，不受影响。
     */
    private static boolean isUnsafePathSegment(String s) {
        return s != null && (s.contains("..") || s.contains("/") || s.contains("\\"));
    }

    // ── 模板加载与填充（需求 §11） ───────────────────────────────────────────────

    /**
     * 加载并填充空模板（§11.2）。
     * 从 classpath 读取真实模板文件（自带命名空间 dc/rdf/xlink/xsi 及正确层级结构），
     * 再用 dm 字段填充 dmCode/issueInfo/issueDate/techName/infoName/security/language 节点。
     * 模板文件保留原始格式，前端会再统一格式化。
     */
    public static String loadTemplate(String standard, String templateFile, IetmDataModule dm) {
        // 路径遍历防护：非法片段回退最小骨架（复用既有「模板缺失」降级路径）
        if (isUnsafePathSegment(standard) || isUnsafePathSegment(templateFile)) {
            log.warn("unsafe template path segment: standard={}, templateFile={}", standard, templateFile);
            return minimalSkeleton(standard);
        }
        String path = RES_BASE + standard.replace(".", "") + "/template/" + templateFile;
        String raw;
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            raw = readAll(in);
        } catch (Exception e) {
            // 模板缺失不抛异常：返回最小 dmodule 骨架，保证编辑器有可编辑内容而非空白（§11.2）
            log.warn("template not found: {}, fallback to minimal skeleton", path);
            return minimalSkeleton(standard);
        }
        return fillTemplate(raw, dm);
    }

    /**
     * 用 dom4j 将 IetmDataModule 元数据填入模板 XML（§11.2 字段清单）。
     * 仅设置已存在节点的属性/文本，不新增节点、不动根元素命名空间。
     * dmCode 各段按 S1000D SNS 定长位提取（与导出XML generateS1000DXml 同一套规则）。
     */
    private static String fillTemplate(String xml, IetmDataModule dm) {
        if (dm == null) return xml;
        try {
            org.dom4j.io.SAXReader reader = new org.dom4j.io.SAXReader();
            reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            reader.setFeature("http://xml.org/sax/features/external-general-entities", false);
            reader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            org.dom4j.Document doc = reader.read(
                    new java.io.ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            org.dom4j.Element root = doc.getRootElement();
            fillDmCode(root, dm);
            fillBrexDmRef(root);
            fillIssueInfo(root, dm);
            fillIssueDate(root, dm);
            fillTitleAndStatus(root, dm);
            return doc.asXML();
        } catch (Exception e) {
            log.warn("fillTemplate failed, return raw template xml", e);
            return xml;
        }
    }

    /**
     * 同步数据库字段到 XML 内部的 dmIdent 元素（复制DM/编辑属性后调用）
     * 确保 XML 内部的 <dmCode><language><issueInfo> 与数据库字段一致
     *
     * @param xmlContent 原始XML内容
     * @param dm 数据库中的DM实体（包含最新的字段值）
     * @return 同步后的XML内容
     */
    public static String syncDmIdentToXml(String xmlContent, IetmDataModule dm) {
        if (xmlContent == null || xmlContent.trim().isEmpty()) {
            log.warn("syncDmIdentToXml: XML内容为空，跳过同步");
            return xmlContent;
        }
        if (dm == null) {
            log.warn("syncDmIdentToXml: DM实体为空，跳过同步");
            return xmlContent;
        }

        try {
            org.dom4j.io.SAXReader reader = new org.dom4j.io.SAXReader();
            reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            reader.setFeature("http://xml.org/sax/features/external-general-entities", false);
            reader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            org.dom4j.Document doc = reader.read(
                    new java.io.ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));
            org.dom4j.Element root = doc.getRootElement();

            // 同步 dmCode 的所有属性（SNS拆解 + infoCode + itemLocationCode等）
            fillDmCode(root, dm);

            // 同步版本号（issueNumber + inWork）
            fillIssueInfo(root, dm);

            // 同步发行日期
            fillIssueDate(root, dm);

            // 同步语言/国家码（language元素）
            fillTitleAndStatus(root, dm);

            String result = doc.asXML();
            log.info("syncDmIdentToXml成功: ID={}, DMC={}", dm.getId(), dm.getDmcCode());
            return result;
        } catch (Exception e) {
            log.error("syncDmIdentToXml失败，返回原XML: {}", e.getMessage(), e);
            return xmlContent;
        }
    }

    // ── XSD 校验（§17.5 CONFIRMED） ─────────────────────────────────────────────

    /**
     * JAXP Validator + XSD 校验。等价 legacy SAXParserFactory+dom4j SAXValidator（§17.5）。
     * 行号来自 SAXParseException.getLineNumber()。
     */
    public static List<DmValidateItemVO> validateAgainstXsd(String content, String standard, String xsd)
            throws Exception {
        final List<DmValidateItemVO> errors = new ArrayList<>();
        // 路径遍历防护：standard/xsd 来自请求体，拒绝含 .. / \ 的值（正常文件名不受影响）
        if (isUnsafePathSegment(standard) || isUnsafePathSegment(xsd)) {
            errors.add(new DmValidateItemVO(0, "非法的Schema路径: " + xsd));
            return errors;
        }
        String xsdPath = RES_BASE + standard.replace(".", "") + "/schema/" + xsd;
        ClassPathResource xsdRes = new ClassPathResource(xsdPath);
        if (!xsdRes.exists()) {
            errors.add(new DmValidateItemVO(0, "找不到Schema文件: " + xsd));
            return errors;
        }
        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        // XXE 防护基线：安全处理特性（新旧解析器均支持，JAXP 1.3+）
        try {
            sf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (org.xml.sax.SAXException ignore) {
            // 极旧解析器可能不支持，忽略（下方 Validator 层特性仍生效）
        }
        // SchemaFactory 禁外部 DTD/Schema 访问（XXE 防护，与 renderHtml 对齐）。
        // 这两个属性是 JAXP 1.5(JDK 7u40) 才引入；classpath 若存在旧版独立 Xerces
        // (xercesImpl.jar) 会抛 SAXNotRecognizedException。此处优雅降级：跳过属性设置，
        // 不中断校验（Schema 来自可信 classpath 资源，XXE 主防线在 Validator 层特性）。
        try {
            sf.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            sf.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        } catch (org.xml.sax.SAXException ex) {
            log.warn("SchemaFactory 不支持 ACCESS_EXTERNAL_* 属性(疑似旧版 Xerces)，已跳过: {}",
                    ex.getMessage());
        }
        try (InputStream xsdIn = xsdRes.getInputStream()) {
            // 传入 systemId 使 XSD 内 xs:include/xs:import 相对路径可解析
            StreamSource xsdSource = new StreamSource(xsdIn, xsdRes.getURL().toExternalForm());
            javax.xml.validation.Schema schemaObj = sf.newSchema(xsdSource);
            Validator validator = schemaObj.newValidator();
            // 禁外部 DTD/实体加载（§17.5：XXE 防护，与 renderHtml 三项对齐）。
            // 旧版独立 Xerces(2.9.1) 的 Validator 不认识这些 parser 特性，会抛
            // SAXNotRecognizedException；逐项 try/catch 降级，不中断校验。
            setValidatorFeatureQuietly(validator,
                    "http://apache.org/xml/features/nonvalidating/load-external-dtd");
            setValidatorFeatureQuietly(validator,
                    "http://xml.org/sax/features/external-general-entities");
            setValidatorFeatureQuietly(validator,
                    "http://xml.org/sax/features/external-parameter-entities");
            validator.setErrorHandler(new ErrorHandler() {
                public void warning(SAXParseException e) { /* 忽略 warning */ }
                public void error(SAXParseException e) { errors.add(toItem(e)); }
                public void fatalError(SAXParseException e) { errors.add(toItem(e)); }
            });
            try (InputStream in = new ByteArrayInputStream(
                    content.getBytes(StandardCharsets.UTF_8))) {
                validator.validate(new StreamSource(in));
            }
        }
        return errors;
    }

    private static DmValidateItemVO toItem(SAXParseException e) {
        // 无 locator 的错误用 0 作哨兵（前端显示为"-"，不参与定位）；真实行号(>0)原样保留
        int line = e.getLineNumber() > 0 ? e.getLineNumber() : 0;
        return new DmValidateItemVO(line, e.getMessage());
    }

    /**
     * 为 Validator 设置 XXE 防护特性；旧版解析器不识别该特性时静默降级。
     * classpath 存在旧版独立 Xerces(2.9.1) 时，setFeature 会抛
     * SAXNotRecognizedException——此处捕获后仅记 warn，避免中断整个校验流程。
     */
    private static void setValidatorFeatureQuietly(Validator validator, String feature) {
        try {
            validator.setFeature(feature, false);
        } catch (org.xml.sax.SAXException ex) {
            log.warn("Validator 不支持特性 {}(疑似旧版 Xerces)，已跳过", feature);
        }
    }

    // ── HTML 预览（§18，二期完整XSLT渲染） ──────────────────────────────────────────

    /**
     * XML → HTML。二期使用 XSLT 转换，支持图形、表格、层级段落等完整元素。
     *
     * 转换流程：
     * 1. 检测DM类型（description/procedure/fault/ipd）
     * 2. 两趟XSLT转换（预处理 + 主样式表）
     * 3. ICN图元路径替换
     *
     * @param content DM XML内容
     * @param contextPath 应用上下文路径（用于构建ICN图片URL）
     * @return HTML字符串
     */
    public static String renderHtml(String content, String contextPath) {
        long startTime = System.currentTimeMillis();

        try {
            log.debug("=== DM预览开始 === XML长度: {} 字节", content != null ? content.length() : 0);

            // 检测标准（默认S1000D40，可从XML命名空间扩展）
            long t1 = System.currentTimeMillis();
            String standard = detectStandard(content);
            String dmType = detectDmType(content);
            log.debug("① 检测完成: standard={}, dmType={}, 耗时: {}ms",
                    standard, dmType, System.currentTimeMillis() - t1);

            // XSLT转换
            long t2 = System.currentTimeMillis();
            String html = DmXsltTransformer.transform(content, standard, dmType);
            log.debug("② XSLT转换完成: HTML长度={} 字节, 耗时: {}ms",
                    html != null ? html.length() : 0, System.currentTimeMillis() - t2);

            // 增强预览HTML - 添加CSS样式
            long t3 = System.currentTimeMillis();
            html = DmXsltTransformer.enhancePreviewHtml(html);
            log.debug("③ CSS增强完成: 耗时: {}ms", System.currentTimeMillis() - t3);

            // ICN图元路径替换
            long t4 = System.currentTimeMillis();
            html = replaceIcnPaths(html, contextPath);
            log.debug("④ ICN路径替换完成: 耗时: {}ms", System.currentTimeMillis() - t4);

            // 修复旧阅读器遗留的全局函数调用（§18需求：兼容旧系统XSLT生成的HTML）
            long t5 = System.currentTimeMillis();
            html = fixLegacyFunctionCalls(html);
            log.debug("⑤ 旧函数调用修复完成: 耗时: {}ms", System.currentTimeMillis() - t5);

            // 包装到预览容器
            String result = "<div class=\"dm-preview\">" + html + "</div>";

            long totalTime = System.currentTimeMillis() - startTime;
            log.info("=== DM预览完成 === 总耗时: {}ms, 最终HTML: {} 字节",
                    totalTime, result.length());

            return result;

        } catch (Exception e) {
            log.error("renderHtml error", e);
            return "<div style='color:red'>预览渲染失败: " + escape(e.getMessage()) + "</div>";
        }
    }

    /**
     * 兼容旧接口（无contextPath参数）
     */
    public static String renderHtml(String content) {
        return renderHtml(content, "/ietm");
    }

    /**
     * 检测S1000D标准版本
     *
     * 从XML命名空间中提取，例如：
     * - http://www.s1000d.org/S1000D_4-0 → S1000D40
     * - http://www.s1000d.org/S1000D_4-1 → S1000D41
     */
    private static String detectStandard(String xmlContent) {
        // 简化实现：从命名空间提取版本号
        if (xmlContent.contains("S1000D_4-1") || xmlContent.contains("S1000D_4.1")) {
            return "S1000D41";
        }
        if (xmlContent.contains("S1000D_4-2") || xmlContent.contains("S1000D_4.2")) {
            return "S1000D42";
        }
        // 默认4.0
        return "S1000D40";
    }

    /**
     * 检测DM类型
     *
     * 从 <content> 子元素判断：
     * - <description> → descript
     * - <procedure> → proced
     * - <faultIsolation> → fault
     * - <illustratedPartsCatalog> → ipd
     */
    private static String detectDmType(String xmlContent) {
        if (xmlContent.contains("<description")) return "descript";
        if (xmlContent.contains("<procedure")) return "proced";
        if (xmlContent.contains("<faultIsolation")) return "fault";
        if (xmlContent.contains("<illustratedPartsCatalog")) return "ipd";

        // 默认描述类
        return "descript";
    }

    /**
     * 替换ICN图元路径 - 使用base64内联方式
     *
     * 修复问题：iframe srcdoc + sandbox 无法加载外部图片URL
     * 解决方案：将ICN图片文件内容转为base64嵌入到HTML中
     * 例如: <img src="ShowSmallImage" boardno="ICN-xxx" />
     *    → <img src="data:image/svg+xml;base64,..." boardno="ICN-xxx" />
     */
    private static String replaceIcnPaths(String html, String contextPath) {
        // 匹配包含boardno属性的img标签
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "<img([^>]*?)boardno=\"([^\"]+)\"([^>]*?)>",
            java.util.regex.Pattern.CASE_INSENSITIVE
        );
        java.util.regex.Matcher matcher = pattern.matcher(html);

        StringBuffer result = new StringBuffer();
        int count = 0;
        int successCount = 0;

        while (matcher.find()) {
            String beforeBoardno = matcher.group(1);
            String icnCode = matcher.group(2);
            String afterBoardno = matcher.group(3);

            count++;

            // 尝试加载ICN图片并转为base64
            String base64Data = loadIcnAsBase64(icnCode);

            if (base64Data != null) {
                log.debug("ICN替换成功: boardno={}, base64长度={}", icnCode, base64Data.length());
                successCount++;
            } else {
                log.warn("ICN加载失败，使用占位图: boardno={}", icnCode);
                // 使用SVG占位图
                base64Data = "data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMDAiIGhlaWdodD0iMjAwIj48cmVjdCB3aWR0aD0iMzAwIiBoZWlnaHQ9IjIwMCIgZmlsbD0iI2YwZjBmMCIgc3Ryb2tlPSIjY2NjIiBzdHJva2Utd2lkdGg9IjIiLz48dGV4dCB4PSIxNTAiIHk9IjEwMCIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZm9udC1zaXplPSIxNCIgZmlsbD0iIzY2NiI+SUNO5Yqg6L295aSx6LSlPC90ZXh0Pjwvc3ZnPg==";
            }

            // 移除现有的src属性（如果存在）
            String beforeCleaned = beforeBoardno.replaceAll("\\s*src\\s*=\\s*\"[^\"]*\"", "");
            String afterCleaned = afterBoardno.replaceAll("\\s*src\\s*=\\s*\"[^\"]*\"", "");

            // 重新构建img标签，使用base64 data URI
            String replacement = "<img src=\"" + base64Data + "\"" +
                                beforeCleaned +
                                " boardno=\"" + icnCode + "\"" +
                                afterCleaned + ">";

            matcher.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        log.info("ICN路径替换完成: 总数={}, 成功={}, 失败={}", count, successCount, count - successCount);

        return result.toString();
    }

    /**
     * 加载ICN图片文件并转为base64 data URI
     */
    private static String loadIcnAsBase64(String icnCode) {
        try {
            // 获取ICN Service和Attachment Service
            org.jeecg.modules.ietm.icnmanage.service.IIetmIcnManageService icnService =
                org.jeecg.common.util.SpringContextUtils.getBean(
                    org.jeecg.modules.ietm.icnmanage.service.IIetmIcnManageService.class);

            org.jeecg.modules.ietm.ietmattachment.service.IIetmAttachmentService attachmentService =
                org.jeecg.common.util.SpringContextUtils.getBean(
                    org.jeecg.modules.ietm.ietmattachment.service.IIetmAttachmentService.class);

            // 1. 查询ICN实体
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<org.jeecg.modules.ietm.icnmanage.entity.IetmIcnManage> qw =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            qw.eq("icn", icnCode);
            org.jeecg.modules.ietm.icnmanage.entity.IetmIcnManage icn = icnService.getOne(qw);

            if (icn == null) {
                log.warn("ICN不存在: icnCode={}", icnCode);
                return null;
            }

            // 2. 直接查询附件（使用pid关联，不调用loadAttachment避免business_id错误）
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<org.jeecg.modules.ietm.ietmattachment.entity.IetmAttachment> attachmentQw =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            attachmentQw.eq("pid", icn.getId());
            attachmentQw.eq("file_type", "实体文件");
            attachmentQw.orderByAsc("create_time");

            java.util.List<org.jeecg.modules.ietm.ietmattachment.entity.IetmAttachment> attachments =
                attachmentService.list(attachmentQw);

            if (attachments == null || attachments.isEmpty()) {
                log.warn("ICN附件缺失: icnCode={}, icnId={}", icnCode, icn.getId());
                return null;
            }

            // 3. 获取第一个附件的文件信息
            org.jeecg.modules.ietm.ietmattachment.entity.IetmAttachment attachment = attachments.get(0);
            String fileKey = attachment.getFileKey();
            String fileName = attachment.getFileName();

            if (fileKey == null || fileKey.isEmpty()) {
                log.warn("ICN文件Key为空: icnCode={}", icnCode);
                return null;
            }

            // 4. 从配置获取ICN存储路径（与IetmIcnManageServiceImpl一致）
            String fileStorageLocation = org.jeecg.common.util.SpringContextUtils
                .getApplicationContext()
                .getEnvironment()
                .getProperty("accessFile.icnLocation", "D:\\workspace\\IETM\\file\\icn");

            // 5. 提取文件名（去掉fileKey中的路径部分）
            String extractedFileName = fileKey;
            if (fileKey.contains(java.io.File.separator)) {
                extractedFileName = fileKey.substring(fileKey.lastIndexOf(java.io.File.separator) + 1);
            } else if (fileKey.contains("/")) {
                extractedFileName = fileKey.substring(fileKey.lastIndexOf("/") + 1);
            }

            // 6. 构建完整文件路径
            java.io.File file = new java.io.File(fileStorageLocation + java.io.File.separator + extractedFileName);

            if (!file.exists()) {
                log.warn("ICN文件不存在: icnCode={}, fileKey={}, extractedFileName={}, path={}",
                         icnCode, fileKey, extractedFileName, file.getAbsolutePath());
                return null;
            }

            // 7. 读取文件内容
            byte[] fileContent = java.nio.file.Files.readAllBytes(file.toPath());
            String mimeType = getMimeTypeForIcn(extractFileExt(fileName));

            // 检测文件是否已经是base64文本（旧系统可能已经编码过）
            String base64;

            // 如果文件全是ASCII可打印字符且包含典型base64字符，可能已经是base64
            if (fileContent.length > 100) {
                String preview = new String(fileContent, 0, Math.min(100, fileContent.length), java.nio.charset.StandardCharsets.US_ASCII);
                // base64文本特征：只包含A-Za-z0-9+/=和空白符
                if (preview.matches("[A-Za-z0-9+/=\\s]+")) {
                    // 尝试解码验证
                    try {
                        String fileAsText = new String(fileContent, java.nio.charset.StandardCharsets.UTF_8).trim();
                        byte[] decoded = java.util.Base64.getDecoder().decode(fileAsText.replaceAll("\\s", ""));
                        // 检查解码后的数据是否是有效图片（JPEG: FF D8, PNG: 89 50 4E 47）
                        if (decoded.length > 4 &&
                            ((decoded[0] == (byte)0xFF && decoded[1] == (byte)0xD8) ||  // JPEG
                             (decoded[0] == (byte)0x89 && decoded[1] == (byte)0x50))) { // PNG
                            base64 = fileAsText.replaceAll("\\s", "");
                        } else {
                            base64 = java.util.Base64.getEncoder().encodeToString(fileContent);
                        }
                    } catch (Exception e) {
                        // 解码失败，说明不是base64，正常编码
                        base64 = java.util.Base64.getEncoder().encodeToString(fileContent);
                    }
                } else {
                    // 不是base64文本，正常编码
                    base64 = java.util.Base64.getEncoder().encodeToString(fileContent);
                }
            } else {
                base64 = java.util.Base64.getEncoder().encodeToString(fileContent);
            }

            return "data:" + mimeType + ";base64," + base64;

        } catch (Exception e) {
            log.error("加载ICN图片失败: icnCode={}", icnCode, e);
            return null;
        }
    }

    private static String extractFileExt(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    private static String getMimeTypeForIcn(String extension) {
        switch (extension) {
            case "svg": return "image/svg+xml";
            case "png": return "image/png";
            case "jpg":
            case "jpeg": return "image/jpeg";
            case "gif": return "image/gif";
            case "bmp": return "image/bmp";
            case "webp": return "image/webp";
            case "cgm": return "image/cgm";
            default: return "application/octet-stream";
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * 修复旧IETM阅读器遗留的全局函数调用
     *
     * 旧系统XSLT生成的HTML中包含以下遗留调用：
     * 1. window.external.ShowDmRef(dmc, fragment) - dmRef链接点击
     * 2. window.parent.addShowContentPanel(dmc) - 另一种dmRef调用方式
     * 3. window.parent.showPicture(icnIdent) - 图形/多媒体点击
     *
     * 新系统前端已在DmPreviewModal.vue中注入对应函数：
     * - showDmRefInfo(dmc, fragment) - 显示dmRef详情弹框
     * - showMultimediaInfo(icnIdent) - 显示ICN预览弹框
     *
     * 同时移除 display:none 样式，强制显示所有元素（对标旧系统行为）
     *
     * @param html 原始HTML
     * @return 修复后的HTML
     */
    private static String fixLegacyFunctionCalls(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }

        // 1. 替换dmRef链接调用（两种模式）
        html = html.replaceAll("window\\.external\\.ShowDmRef", "showDmRefInfo");
        html = html.replaceAll("window\\.parent\\.addShowContentPanel", "showDmRefInfo");

        // 2. 替换图形/多媒体调用
        html = html.replaceAll("window\\.parent\\.showPicture", "showMultimediaInfo");

        // 3. 移除display:none - 仅处理内联内容元素，保留容器元素的UI控制
        // 问题：全局替换会破坏XSLT模板的UI控制逻辑（警告面板、故障隔离流程等）
        // 修复：只替换内联元素（span/emphasis/strong/em）的display:none
        // 容器元素（div等）的display:none保留，用于JavaScript动态控制
        // 词边界修复：em分支加\b避免误匹配<embed>等标签
        html = html.replaceAll(
            "<(span|emphasis|strong|em\\b)([^>]*?)style=\"([^\"]*?)display:\\s*none\\s*;?([^\"]*?)\"",
            "<$1$2style=\"$3display:;$4\""
        );

        return html;
    }

    /**
     * 修复锚点链接路径（§18需求）
     *
     * 问题：XSLT生成的锚点链接 href="#section-1" 在iframe中无法正常跳转
     * 原因：iframe使用blob:// URL，锚点需要完整的 blob://...#section-1 格式
     * 解决：在前端通过JavaScript动态修复（后端保持原样），或添加base标签
     *
     * 当前方案：保持 href="#xxx" 不变，由前端iframe的onload事件处理
     * 备用方案：如果前端无法处理，可以在这里注入 <base href="#" /> 标签
     */
    private static String fixAnchorLinks(String html) {
        // 暂时不做处理，保持原有 href="#xxx" 格式
        // 前端DmPreviewModal.vue会在iframe.onload中通过JS修复
        // 如果需要后端处理，可以用正则替换：
        // html = html.replaceAll("href=\"#", "href=\"javascript:void(0);\" data-anchor=\"#");
        return html;
    }

    // ── 模板填充辅助（§11.2） ────────────────────────────────────────────────────

    /**
     * dmCode 各段：【方案A】SNS 含 equipname 作首段(=modelIdentCode)，按 - 拆分对标老系统 getDmrefByDmc。
     * modelIdentCode=SNS[0]、systemDiffCode=SNS[1]、systemCode=SNS[2]、subSystem+subSub=SNS[3]、
     * assy=SNS[4]、disassy+variant=SNS[5]。缺失段由 DmcUtils.decomposeSns 兜底。
     */
    private static void fillDmCode(org.dom4j.Element root, IetmDataModule dm) {
        org.dom4j.Element dmCode = findFirst(root, "dmCode");
        if (dmCode == null) return;
        java.util.Map<String, String> seg = DmcUtils.decomposeSns(dm.getSns());
        // modelIdentCode=SNS[0]；SNS 缺首段时回退 resolveModelIdentCode（schema→AA）
        String modelIdentCode = seg.get("modelIdentCode");
        if (modelIdentCode == null || modelIdentCode.trim().isEmpty()) {
            modelIdentCode = DmcUtils.resolveModelIdentCode(dm.getSchema(), null);
        }
        // subSubSystemCode/disassyCodeVariant XSD 要求非空，decomposeSns 缺省为空串时兜底为老逻辑默认值
        String subSub = seg.get("subSubSystemCode");
        if (subSub == null || subSub.isEmpty()) subSub = "0";
        String variant = seg.get("disassyCodeVariant");
        if (variant == null || variant.isEmpty()) variant = "A";
        setAttr(dmCode, "modelIdentCode",     modelIdentCode);
        setAttr(dmCode, "systemDiffCode",     seg.get("systemDiffCode"));
        setAttr(dmCode, "systemCode",         seg.get("systemCode"));
        setAttr(dmCode, "subSystemCode",      seg.get("subSystemCode"));
        setAttr(dmCode, "subSubSystemCode",   subSub);
        setAttr(dmCode, "assyCode",           seg.get("assyCode"));
        setAttr(dmCode, "disassyCode",        seg.get("disassyCode"));
        setAttr(dmCode, "disassyCodeVariant", variant);
        setAttr(dmCode, "infoCode",           safeStr(dm.getInfoCode()));
        setAttr(dmCode, "infoCodeVariant",    safeStr(dm.getInfoCodeVariant(), "A"));
        setAttr(dmCode, "itemLocationCode",   safeStr(dm.getIetmLocationCode(), "A"));
    }

    /**
     * 填充 brexDmRef 内的 dmCode（§11.2：默认业务规则DM，固定值 DEFAULT-A-00-00-00-00A-022A-D）。
     * 模板 brexDmRef/dmCode 各属性为空 → 一校验即报 11×cvc-pattern-valid；此处补填固定合法值。
     * 注意 fillDmCode 用 findFirst 只填了主 dmCode（dmIdent 内），brexDmRef 的第二个 dmCode 从未被填。
     * 因是固定值，直接硬编码拆解结果（各段均已核对满足 descript.xsd 各属性 pattern），
     * 不复用「按 DMC 字符串切片」的通用逻辑。
     */
    private static void fillBrexDmRef(org.dom4j.Element root) {
        org.dom4j.Element brexDmRef = findFirst(root, "brexDmRef");
        if (brexDmRef == null) return;
        org.dom4j.Element dmCode = findFirst(brexDmRef, "dmCode");
        if (dmCode == null) return;
        setAttr(dmCode, "modelIdentCode",     "DEFAULT");
        setAttr(dmCode, "systemDiffCode",     "A");
        setAttr(dmCode, "systemCode",         "00");
        setAttr(dmCode, "subSystemCode",      "0");
        setAttr(dmCode, "subSubSystemCode",   "0");
        setAttr(dmCode, "assyCode",           "00");
        setAttr(dmCode, "disassyCode",        "00");
        setAttr(dmCode, "disassyCodeVariant", "A");
        setAttr(dmCode, "infoCode",           "022");
        setAttr(dmCode, "infoCodeVariant",    "A");
        setAttr(dmCode, "itemLocationCode",   "D");
    }

    /** issueInfo：issueNumber=发行编号、inWork=在编版本号 */
    private static void fillIssueInfo(org.dom4j.Element root, IetmDataModule dm) {
        org.dom4j.Element issueInfo = findFirst(root, "issueInfo");
        if (issueInfo == null) return;
        setAttr(issueInfo, "issueNumber", safeStr(dm.getIssueNo(), "001"));
        setAttr(issueInfo, "inWork",      safeStr(dm.getInWork(), "00"));
    }

    /** issueDate：year/month/day（month/day 补前导0） */
    private static void fillIssueDate(org.dom4j.Element root, IetmDataModule dm) {
        org.dom4j.Element issueDate = findFirst(root, "issueDate");
        if (issueDate == null) return;
        java.util.Date d = dm.getIssueDate() != null ? dm.getIssueDate() : new java.util.Date();
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(d);
        setAttr(issueDate, "year",  String.valueOf(cal.get(java.util.Calendar.YEAR)));
        setAttr(issueDate, "month", String.format("%02d", cal.get(java.util.Calendar.MONTH) + 1));
        setAttr(issueDate, "day",   String.format("%02d", cal.get(java.util.Calendar.DAY_OF_MONTH)));
    }

    /** techName/infoName 文本 + security 密级 + language 语言/国家码 */
    private static void fillTitleAndStatus(org.dom4j.Element root, IetmDataModule dm) {
        org.dom4j.Element techNameEl = findFirst(root, "techName");
        if (techNameEl != null && org.springframework.util.StringUtils.hasText(dm.getTechName()))
            techNameEl.setText(dm.getTechName());
        org.dom4j.Element infoNameEl = findFirst(root, "infoName");
        if (infoNameEl != null && org.springframework.util.StringUtils.hasText(dm.getInfoName()))
            infoNameEl.setText(dm.getInfoName());
        org.dom4j.Element security = findFirst(root, "security");
        if (security != null && org.springframework.util.StringUtils.hasText(dm.getSecurity()))
            setAttr(security, "securityClassification", "0" + dm.getSecurity());
        // 填充 dmStatus 的 issueType 属性（S1000D 标准）
        org.dom4j.Element dmStatus = findFirst(root, "dmStatus");
        if (dmStatus != null && org.springframework.util.StringUtils.hasText(dm.getIssueType())) {
            setAttr(dmStatus, "issueType", dm.getIssueType());
        }
        org.dom4j.Element language = findFirst(root, "language");
        if (language != null) {
            // S1000D descript.xsd 约定：languageIsoCode 为小写 [a-z]{2,3}、countryIsoCode 为大写 [A-Z]{2,3}。
            // 库中可能存大写(如 "ZH")，此处规范化大小写，避免生成的模板 XML 一校验即报 cvc-pattern-valid。
            String lang = safeStr(dm.getLanguageIsoCode(), "zh").toLowerCase();
            String country = safeStr(dm.getCountryIsoCode(), "CN").toUpperCase();
            setAttr(language, "languageIsoCode", lang);
            setAttr(language, "countryIsoCode",  country);
        }
    }

    /** 深度优先找第一个匹配名称的元素 */
    @SuppressWarnings("unchecked")
    private static org.dom4j.Element findFirst(org.dom4j.Element el, String name) {
        if (name.equals(el.getName())) return el;
        for (org.dom4j.Element child : (List<org.dom4j.Element>) el.elements()) {
            org.dom4j.Element found = findFirst(child, name);
            if (found != null) return found;
        }
        return null;
    }

    /** 设置属性（覆盖模板原有空值）；val 为空则跳过，保留模板占位 */
    private static void setAttr(org.dom4j.Element el, String attr, String val) {
        if (val != null && !val.isEmpty()) el.addAttribute(attr, val);
    }

    private static String safeStr(String s)              { return s != null ? s : ""; }
    private static String safeStr(String s, String def)  { return (s != null && !s.isEmpty()) ? s : def; }

    /**
     * 模板缺失时的最小可编辑骨架，按标准生成正确根标签。
     * GJB6600 根为 &lt;数据模块&gt;；其余默认 &lt;dmodule&gt;（§11.2）。
     */
    private static String minimalSkeleton(String standard) {
        boolean isGjb = "GJB6600".equals(standard);
        String root = isGjb ? "数据模块" : "dmodule";
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<" + root + ">\n"
                + "</" + root + ">";
    }

    private static String readAll(InputStream in) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096]; int n;
        while ((n = in.read(buf)) != -1) bos.write(buf, 0, n);
        return new String(bos.toByteArray(), StandardCharsets.UTF_8);
    }

    // ── 引用关系提取（§6 引用关系计算逻辑） ───────────────────────────────────────

    /**
     * 从 DM XML 内容中提取所有外部引用关系。
     * <p>
     * 支持元素类型：
     * <ul>
     *   <li>dmRef     → DM引用（//dmRef/dmRefIdent/dmCode 属性拼 DMC）</li>
     *   <li>graphic   → 图形引用（//graphic/@infoEntityIdent）</li>
     *   <li>multimedia→ 多媒体引用（//multimedia/@infoEntityIdent）</li>
     * </ul>
     * internalRef（内部锚点）不建立跨DM关系，忽略。
     *
     * @param xmlContent DM的 dm_content 字段值
     * @return 提取到的引用列表；若 xmlContent 为空则返回空列表
     * @throws Exception XML 解析失败时抛出，由调用方决定是否继续
     */
    @SuppressWarnings("unchecked")
    public static List<org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.DmRefExtractItemVO>
            extractReferencesFromXml(String xmlContent) throws Exception {

        List<org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.DmRefExtractItemVO> result = new ArrayList<>();
        if (xmlContent == null || xmlContent.trim().isEmpty()) {
            return result;
        }

        org.dom4j.io.SAXReader reader = new org.dom4j.io.SAXReader();
        // 禁用外部实体（XXE防护，与已有代码保持一致）
        reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        reader.setFeature("http://xml.org/sax/features/external-general-entities", false);
        reader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

        org.dom4j.Document doc;
        try {
            doc = reader.read(new java.io.StringReader(xmlContent));
        } catch (Exception e) {
            log.warn("extractReferencesFromXml: XML解析失败，内容长度={}，错误：{}", xmlContent.length(), e.getMessage());
            throw e;
        }
        org.dom4j.Element root = doc.getRootElement();

        // 1. 提取 dmRef 引用（selectNodes 返回 List<Node>，用 cast 转 Element）
        for (Object node : root.selectNodes("//dmRef/dmRefIdent/dmCode")) {
            if (!(node instanceof org.dom4j.Element)) continue;
            org.dom4j.Element dmCode = (org.dom4j.Element) node;
            String targetDmc = buildDmcFromElement(dmCode);
            if (targetDmc != null && !targetDmc.isEmpty()) {
                org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.DmRefExtractItemVO item =
                        new org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.DmRefExtractItemVO();
                item.setRefType("dmRef");
                item.setTargetDmc(targetDmc);
                item.setRefPosition(dmCode.getUniquePath());
                result.add(item);
            }
        }

        int dmRefCount = result.size();

        // 2. 提取 graphic 引用（infoEntityIdent 属性即 ICN 编码）
        for (Object node : root.selectNodes("//graphic[@infoEntityIdent]")) {
            if (!(node instanceof org.dom4j.Element)) continue;
            org.dom4j.Element graphic = (org.dom4j.Element) node;
            String icn = graphic.attributeValue("infoEntityIdent");
            if (icn != null && !icn.trim().isEmpty()) {
                org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.DmRefExtractItemVO item =
                        new org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.DmRefExtractItemVO();
                item.setRefType("graphic");
                item.setTargetDmc(icn.trim());
                item.setRefPosition(graphic.getUniquePath());
                result.add(item);
            }
        }

        int graphicCount = result.size() - dmRefCount;

        // 3. 提取 multimedia 引用
        for (Object node : root.selectNodes("//multimedia[@infoEntityIdent]")) {
            if (!(node instanceof org.dom4j.Element)) continue;
            org.dom4j.Element multimedia = (org.dom4j.Element) node;
            String imf = multimedia.attributeValue("infoEntityIdent");
            if (imf != null && !imf.trim().isEmpty()) {
                org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.DmRefExtractItemVO item =
                        new org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.DmRefExtractItemVO();
                item.setRefType("multimedia");
                item.setTargetDmc(imf.trim());
                item.setRefPosition(multimedia.getUniquePath());
                result.add(item);
            }
        }

        int multimediaCount = result.size() - dmRefCount - graphicCount;

        log.debug("extractReferencesFromXml: 共提取 {} 条引用（dmRef={}, graphic={}, multimedia={}）",
                result.size(), dmRefCount, graphicCount, multimediaCount);
        return result;
    }

    /**
     * 从 &lt;dmCode&gt; 元素的属性拼装完整 DMC 字符串（S1000D / GJB 均适用）。
     * 返回格式与 DmcUtils.generateDmc 保持一致（11段，下划线分隔版本/语言段不在此处拼）。
     */
    private static String buildDmcFromElement(org.dom4j.Element dmCode) {
        String model   = safeStr(dmCode.attributeValue("modelIdentCode"));
        String sysDiff = safeStr(dmCode.attributeValue("systemDiffCode"));
        String sys     = safeStr(dmCode.attributeValue("systemCode"));
        String subSys  = safeStr(dmCode.attributeValue("subSystemCode"));
        String subSub  = safeStr(dmCode.attributeValue("subSubSystemCode"));
        String assy    = safeStr(dmCode.attributeValue("assyCode"));
        String dis     = safeStr(dmCode.attributeValue("disassyCode"));
        String disVar  = safeStr(dmCode.attributeValue("disassyCodeVariant"));
        String info    = safeStr(dmCode.attributeValue("infoCode"));
        String infoVar = safeStr(dmCode.attributeValue("infoCodeVariant"));
        String loc     = safeStr(dmCode.attributeValue("itemLocationCode"));

        // 任意必要段为空则无法定位目标 DM，返回 null 让调用方跳过
        if (model.isEmpty() || sys.isEmpty() || info.isEmpty()) {
            log.debug("buildDmcFromElement: dmCode 属性不完整，跳过: {}", dmCode.asXML());
            return null;
        }

        // 拼装 SNS（与 DmcUtils.composeSns 相同逻辑）
        String sns = model + "-" + sysDiff + "-" + sys + "-" + subSys + subSub + "-" + assy + "-" + dis + disVar;
        return "DMC-" + sns + "-" + info + infoVar + "-" + loc;
    }

}
