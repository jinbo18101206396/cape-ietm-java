package org.jeecg.modules.ietm.ietmdatamodulemanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDataModule;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDmType;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.mapper.IetmDataModuleMapper;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.mapper.IetmDmTypeMapper;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.service.IIetmDmContentService;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.service.IIetmDmSchemaService;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.util.DmXmlHelper;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.util.DmcUtils;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.DmEditorLoadVO;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.DmValidateItemVO;
import org.jeecg.modules.ietm.projectmanagement.entity.IetmProject;
import org.jeecg.modules.ietm.projectmanagement.mapper.IetmProjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import javax.annotation.Resource;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import java.io.StringReader;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.DmRefBuildItemVO;
import java.util.Calendar;

/**
 * DM 编辑器内容服务实现（加载/保存/校验/预览，需求 §9/§15/§17/§18）
 */
@Slf4j
@Service
public class IetmDmContentServiceImpl implements IIetmDmContentService {

    @Resource private IetmDataModuleMapper dataModuleMapper;
    @Resource private IetmDmTypeMapper     dmTypeMapper;
    @Resource private IIetmDmSchemaService schemaService;
    @Resource private IetmProjectMapper    projectMapper;
    @Resource private org.jeecg.modules.ietm.icnmanage.mapper.IetmIcnReferenceMapper icnReferenceMapper;
    @Resource private org.jeecg.modules.ietm.icnmanage.mapper.IetmIcnManageMapper icnManageMapper;

    private static final String DEFAULT_STANDARD = "S1000D4.0";
    private static final String DEFAULT_XSD      = "descript.xsd";

    // ── 加载（§9） ─────────────────────────────────────────────────────────────

    @Override
    public DmEditorLoadVO loadEditorData(String id, String historyId) {
        DmEditorLoadVO vo = new DmEditorLoadVO();

        // 如果提供了historyId，优先从历史版本加载；否则从主表当前版本加载
        String loadId = (historyId != null && !historyId.trim().isEmpty()) ? historyId : id;
        IetmDataModule dm = dataModuleMapper.selectById(loadId);

        if (dm == null) {
            vo.setFlag("failure");
            vo.setMessage("DM不存在");
            return vo;
        }

        String standard = resolveStandard(dm);
        IetmDmType type = resolveDmType(dm, standard);
        String xsd = type != null ? type.getXsdFile() : DEFAULT_XSD;

        // 正文：库中为空则回填空模板（§9/§11）。
        // 模板文件名按需求§11.2 由 XSD 名派生（descript.xsd→descript.xml）；
        // 优先用 dm_type 命中的 template_file，未命中（如 dm_type 数据不一致）则回退 XSD 派生名。
        String xml = dm.getDmContent();
        if (!StringUtils.hasText(xml)) {
            String templateFile = (type != null && StringUtils.hasText(type.getTemplateFile()))
                    ? type.getTemplateFile()
                    : xsd.replace(".xsd", ".xml");
            xml = DmXmlHelper.loadTemplate(standard, templateFile, dm);
        }
        vo.setXml(xml);
        vo.setXsdSchema(xsd);
        vo.setIetmStandard(standard);

        // Schema 约束对象 + 中文化 + 中英映射（§12）
        Map<String, Object> schema = schemaService.schema2Designer(standard, xsd);
        Map<String, String> en2cn  = schemaService.loadEn2CnElem(standard);
        Map<String, String> cn2en  = schemaService.loadCn2EnElem(standard);
        vo.setSchema(schema);
        vo.setCnSchema(schemaService.schema2DesignerCn(schema, en2cn));
        vo.setEn2cnElem(en2cn);
        vo.setCn2enElem(cn2en);

        vo.setDesignerSett(defaultDesignerSett());
        vo.setVersion(dm.getVersion());   // 随加载返回，保存时原样回传（避免前端二次 queryById 竞态）
        vo.setCheckoutUser(dm.getCheckoutUser()); // 🔧 修复：返回签出用户，用于流程提交前校验（对齐旧系统）
        vo.setFlag("success");
        return vo;
    }

    /**
     * 兼容旧代码：无historyId参数的重载方法
     * @deprecated 新代码应使用 loadEditorData(String id, String historyId)
     */
    @Deprecated
    public DmEditorLoadVO loadEditorData(String id) {
        return loadEditorData(id, null);
    }

    /** 从项目表取 ietmStandard；项目未配置时回退到 DEFAULT_STANDARD（§25.4） */
    private String resolveStandard(IetmDataModule dm) {
        if (StringUtils.hasText(dm.getProjectId())) {
            IetmProject project = projectMapper.selectById(dm.getProjectId());
            if (project != null && StringUtils.hasText(project.getIetmStandard())) {
                return project.getIetmStandard();
            }
        }
        log.warn("DM[{}] project not found or ietmStandard unset, fallback to {}", dm.getId(), DEFAULT_STANDARD);
        return DEFAULT_STANDARD;
    }

    private IetmDmType resolveDmType(IetmDataModule dm, String standard) {
        if (!StringUtils.hasText(dm.getDmType())) return null;
        LambdaQueryWrapper<IetmDmType> w = new LambdaQueryWrapper<>();
        w.eq(IetmDmType::getTypeCode,     dm.getDmType())
         .eq(IetmDmType::getIetmStandard, standard)
         .eq(IetmDmType::getStatus,       "1").last("FETCH FIRST 1 ROWS ONLY");
        return dmTypeMapper.selectOne(w);
    }

    private Map<String, Object> defaultDesignerSett() {
        Map<String, Object> common = new LinkedHashMap<>();
        common.put("autosavetime", "10");              // 分钟，"0"禁用（§21.4）
        common.put("validdmBeforesave", "false");
        common.put("toRefsAndDoctypeBeforesave", "false");
        common.put("popupPreview", "false");
        Map<String, Object> sett = new LinkedHashMap<>();
        sett.put("common", common);
        return sett;
    }

    // ── 保存（§15） ────────────────────────────────────────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveContent(String id, String content, Integer clientVersion, String username) throws Exception {
        IetmDataModule dm = dataModuleMapper.selectById(id);
        if (dm == null) return "DM不存在";

        // 签出锁校验（§3.1）：必须本人已签出才能保存
        String lockUser = dm.getCheckoutUser();
        if (!StringUtils.hasText(lockUser)) return "该DM未签出，不能保存。请先签出。";
        if (!lockUser.equals(username))     return "该DM由【" + lockUser + "】签出，您不能保存。";

        // 版本号必填：为空则乐观锁失效，会静默覆盖他人修改，故直接拒绝（§15）
        if (clientVersion == null) return "缺少版本号，无法保存，请重新加载。";

        // ✅ 同步版本号：数据库 → XML（在保存前修正XML中的issueInfo标签）
        content = syncVersionToXml(content, dm.getIssueNo(), dm.getInWork());

        // 仅更新 dm_content + 乐观锁，不走 updateDm（其逻辑会在 checkout_user 非空时抛异常）
        IetmDataModule update = new IetmDataModule();
        update.setId(id);
        update.setDmContent(content);
        update.setUpdateBy(username);
        update.setUpdateTime(new Date());
        update.setVersion(clientVersion);  // @Version 自动 +1

        int rows = dataModuleMapper.updateById(update);
        if (rows == 0) return "保存失败：数据已被他人修改(版本冲突)，请重新加载。";

        // ✅ 自动同步 ICN 引用关系到 ietm_icn_reference 表
        // 修复P1-2：ICN引用同步失败不应阻塞主流程，用户可通过"计算引用"按钮手动补偿
        try {
            syncIcnReferences(id, content, username);
            log.info("saveContent ICN引用同步成功 dmId={}", id);
        } catch (Exception e) {
            log.error("saveContent ICN引用同步失败（不影响DM保存） dmId={} error={}", id, e.getMessage(), e);
            // 不抛出异常，ICN引用失败不影响DM内容保存
        }

        return null; // 成功
    }

    /**
     * 同步版本号：确保XML中的issueInfo与数据库字段一致
     * 原则：以数据库为准，自动修正XML
     *
     * @param xmlContent XML内容
     * @param dbIssueNo 数据库中的issueNo
     * @param dbInWork 数据库中的inWork
     * @return 修正后的XML内容
     */
    private String syncVersionToXml(String xmlContent, String dbIssueNo, String dbInWork) {
        if (!StringUtils.hasText(xmlContent) || !StringUtils.hasText(dbIssueNo)) {
            return xmlContent;
        }

        // 默认inWork为00
        if (!StringUtils.hasText(dbInWork)) {
            dbInWork = "00";
        }

        try {
            // 正则匹配<issueInfo>标签（支持属性顺序任意）
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "<issueInfo[^>]*/>",
                java.util.regex.Pattern.CASE_INSENSITIVE
            );

            java.util.regex.Matcher matcher = pattern.matcher(xmlContent);

            if (matcher.find()) {
                // 提取当前XML中的版本号
                String issueInfoTag = matcher.group();
                java.util.regex.Pattern issueNumberPattern = java.util.regex.Pattern.compile(
                    "issueNumber\\s*=\\s*[\"']([^\"']+)[\"']",
                    java.util.regex.Pattern.CASE_INSENSITIVE
                );
                java.util.regex.Pattern inWorkPattern = java.util.regex.Pattern.compile(
                    "inWork\\s*=\\s*[\"']([^\"']+)[\"']",
                    java.util.regex.Pattern.CASE_INSENSITIVE
                );

                java.util.regex.Matcher issueNumberMatcher = issueNumberPattern.matcher(issueInfoTag);
                java.util.regex.Matcher inWorkMatcher = inWorkPattern.matcher(issueInfoTag);

                String xmlIssueNumber = issueNumberMatcher.find() ? issueNumberMatcher.group(1) : "";
                String xmlInWork = inWorkMatcher.find() ? inWorkMatcher.group(1) : "";

                // 如果不一致，替换整个标签
                if (!xmlIssueNumber.equals(dbIssueNo) || !xmlInWork.equals(dbInWork)) {
                    String newTag = String.format(
                        "<issueInfo issueNumber=\"%s\" inWork=\"%s\"/>",
                        escapeXmlAttr(dbIssueNo),
                        escapeXmlAttr(dbInWork)
                    );

                    xmlContent = pattern.matcher(xmlContent).replaceAll(
                        java.util.regex.Matcher.quoteReplacement(newTag)
                    );

                    log.info("[版本号同步] 已将XML版本号从 {}-{} 修正为 {}-{}",
                        xmlIssueNumber, xmlInWork, dbIssueNo, dbInWork);
                }
            }
        } catch (Exception e) {
            log.error("[版本号同步失败] 错误: {}", e.getMessage(), e);
            // 同步失败不影响保存，返回原XML
        }

        return xmlContent;
    }

    // ── 校验（§17.5 CONFIRMED） ────────────────────────────────────────────────

    @Override
    public Map<String, Object> validateById(String id) {
        Map<String, Object> ret = new HashMap<>();
        IetmDataModule dm = dataModuleMapper.selectById(id);
        if (dm == null) {
            ret.put("flag", "error");
            List<DmValidateItemVO> errs = new ArrayList<>();
            errs.add(new DmValidateItemVO(0, "DM不存在，ID: " + id));
            ret.put("errors", errs);
            return ret;
        }
        String content = dm.getDmContent();
        if (!StringUtils.hasText(content)) {
            ret.put("flag", "0");
            return ret;
        }
        String standard = resolveStandard(dm);
        IetmDmType type = resolveDmType(dm, standard);
        String schema = type != null ? type.getXsdFile() : DEFAULT_XSD;
        List<DmValidateItemVO> errors = validateXsd(content, standard, schema, id);
        if (errors.isEmpty()) {
            ret.put("flag", "1");
        } else {
            ret.put("flag", "error");
            ret.put("errors", errors);
        }
        return ret;
    }

    @Override
    public List<DmValidateItemVO> validateXsd(String content, String standard,
                                              String schema, String dmId) {
        List<DmValidateItemVO> errors = new ArrayList<>();
        if (!StringUtils.hasText(content)) return errors; // 空内容，controller 侧处理

        if (!StringUtils.hasText(standard)) standard = DEFAULT_STANDARD;
        if (!StringUtils.hasText(schema)) {
            IetmDataModule dm = dmId != null ? dataModuleMapper.selectById(dmId) : null;
            IetmDmType t = dm != null ? resolveDmType(dm, standard) : null;
            schema = t != null ? t.getXsdFile() : DEFAULT_XSD;
        }
        try {
            errors = DmXmlHelper.validateAgainstXsd(content, standard, schema);
        } catch (Exception e) {
            log.error("validateXsd error", e);
            errors.add(new DmValidateItemVO(0, "校验异常：" + e.getMessage()));
        }
        // 过滤 ICN 实体引用导致的误报（§17.5 CONFIRMED）
        errors.removeIf(it -> it.getInfo() != null
                && (it.getInfo().contains("UndeclaredEntity")
                    || it.getInfo().contains("infoEntityIdent")));
        return errors;
    }

    // ── 预览（§18） ────────────────────────────────────────────────────────────

    @Override
    public String renderHtml(String content, String dmId) {
        if (!StringUtils.hasText(content)) return null;
        return DmXmlHelper.renderHtml(content);
    }

    // ── 引用片段列表（§14.5.2③） ───────────────────────────────────────────────

    @Override
    public Map<String, Object> getRef(String id) {
        Map<String, Object> ret = new HashMap<>();
        IetmDataModule dm = dataModuleMapper.selectById(id);
        if (dm == null) {
            ret.put("flag", "failure");
            ret.put("message", "DM不存在");
            return ret;
        }
        String content = dm.getDmContent();
        if (!StringUtils.hasText(content)) {
            ret.put("flag", "success");
            ret.put("refs", Collections.emptyList());
            return ret;
        }
        List<String> refs = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            // 禁用外部实体，防止 XXE。不用 accessExternalDTD/Schema 属性——
            // 项目内旧版 Xerces(autopoi 2.9.1) 不支持 JAXP1.5，setAttribute 会抛异常。
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(content)));
            collectRefs(doc.getDocumentElement(), refs);
        } catch (Exception e) {
            log.error("getRef XML解析失败, dmId={}", id, e);
            ret.put("flag", "failure");
            ret.put("message", "XML解析失败：" + e.getMessage());
            return ret;
        }
        ret.put("flag", "success");
        ret.put("refs", refs);
        return ret;
    }

    /**
     * 递归收集含 id 属性的元素，格式：elementName%%%[前缀]idValue。
     * <p>对标老系统 IetmEditorPlatform-src.js:674-687：引用片段目标仅按 id 收集
     * （referredFragment 语义须匹配元素 id）。graphic/multimediaObject 的 infoEntityIdent
     * 是 ICN 实体标识，非引用片段目标，不在此收集。特殊元素类型前缀：
     * graphic→g_  multimediaObject→m_  dmRef→d_。
     */
    private void collectRefs(org.w3c.dom.Element elem, List<String> refs) {
        String idAttr = elem.getAttribute("id");
        if (idAttr != null && !idAttr.isEmpty()) {
            String name = elem.getTagName();
            String idPrefix;
            switch (name) {
                case "graphic":          idPrefix = "g_"; break;
                case "multimediaObject": idPrefix = "m_"; break;
                case "dmRef":            idPrefix = "d_"; break;
                default:                 idPrefix = "";   break;
            }
            refs.add(name + "%%%" + idPrefix + idAttr);
        }
        NodeList children = elem.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                collectRefs((org.w3c.dom.Element) child, refs);
            }
        }
    }

    // ── 批量生成 dmRef XML（§14.5.4，dmCode 从目标DM XML权威提取） ─────────────────

    @Override
    public Map<String, Object> buildDmRef(List<DmRefBuildItemVO> items) {
        Map<String, Object> ret = new HashMap<>();
        if (items == null || items.isEmpty()) {
            ret.put("flag", "failure");
            ret.put("message", "参数不能为空");
            return ret;
        }
        StringBuilder sb = new StringBuilder();
        for (DmRefBuildItemVO item : items) {
            IetmDataModule dm = dataModuleMapper.selectById(item.getDmId());
            if (dm == null) {
                log.warn("buildDmRef：DM不存在，id={}", item.getDmId());
                continue;
            }
            // 1. 优先从 dm_content 提取 dmCode（权威）
            String dmCodeXml = extractDmCodeFromContent(dm.getDmContent());
            // 2. dm_content 为空时回退：从实体字段构建 dmCode（与编辑器加载模板时用的 fillDmCode 同逻辑）
            if (dmCodeXml == null) {
                dmCodeXml = buildDmCodeXmlFromEntity(dm);
                if (dmCodeXml == null) {
                    log.warn("buildDmRef：DM[{}] 无法生成dmCode（sns/infoCode为空或不符合格式），跳过", item.getDmId());
                    continue;
                }
                log.info("buildDmRef：DM[{}] dm_content为空，已从实体字段构建dmCode", item.getDmId());
            }
            String single = buildSingleDmRef(dm, dmCodeXml, item);
            if (single != null) {
                if (sb.length() > 0) sb.append("\n");
                sb.append(single);
            }
        }
        ret.put("flag", "success");
        ret.put("xml", sb.toString());
        return ret;
    }

    /**
     * 从 dm_content 里找到第一个 {@code <dmCode>} 元素，
     * 把它的全部属性重新序列化为自闭合标签字符串。
     */
    private String extractDmCodeFromContent(String content) {
        if (!StringUtils.hasText(content)) return null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(content)));
            org.w3c.dom.Element dmCodeElem = findFirstByName(doc.getDocumentElement(), "dmCode");
            if (dmCodeElem == null) return null;
            StringBuilder tag = new StringBuilder("<dmCode");
            org.w3c.dom.NamedNodeMap attrs = dmCodeElem.getAttributes();
            for (int i = 0; i < attrs.getLength(); i++) {
                org.w3c.dom.Attr a = (org.w3c.dom.Attr) attrs.item(i);
                if (a.getName().startsWith("xmlns")) continue; // 跳过命名空间声明
                tag.append(" ").append(a.getName()).append("=\"")
                   .append(escapeXmlAttr(a.getValue())).append("\"");
            }
            tag.append("/>");
            return tag.toString();
        } catch (Exception e) {
            log.error("extractDmCodeFromContent 解析失败", e);
            return null;
        }
    }

    /**
     * 从 DM 实体字段构建 {@code <dmCode>} 标签字符串（复用编辑器模板回填逻辑）。
     * 当 dm_content 为空时的兜底方案，与 DmXmlHelper.fillDmCode 采用相同字段+转换规则。
     */
    private String buildDmCodeXmlFromEntity(IetmDataModule dm) {
        if (!StringUtils.hasText(dm.getSns())) return null;
        try {
            // SNS 按 S1000D 定长位分解 modelIdentCode / systemDiffCode / systemCode / subSystemCode / subSubSystemCode / assyCode / disassyCode / disassyCodeVariant
            java.util.Map<String, String> seg = DmcUtils.decomposeSns(dm.getSns());
            String modelIdentCode = seg.get("modelIdentCode");
            if (modelIdentCode == null || modelIdentCode.trim().isEmpty()) {
                // 缺首段时回退 resolveModelIdentCode（从 schema→AA 映射），兜底 "UNKNOWN"
                modelIdentCode = DmcUtils.resolveModelIdentCode(dm.getSchema(), "UNKNOWN");
            }
            // subSubSystemCode/disassyCodeVariant XSD 要求非空，空时兜底默认值
            String subSub = seg.get("subSubSystemCode");
            if (subSub == null || subSub.isEmpty()) subSub = "0";
            String variant = seg.get("disassyCodeVariant");
            if (variant == null || variant.isEmpty()) variant = "A";

            // 组装属性（与 DmXmlHelper.fillDmCode 一致）
            StringBuilder tag = new StringBuilder("<dmCode");
            tag.append(" modelIdentCode=\"").append(escapeXmlAttr(modelIdentCode)).append("\"");
            if (seg.get("systemDiffCode") != null)
                tag.append(" systemDiffCode=\"").append(escapeXmlAttr(seg.get("systemDiffCode"))).append("\"");
            if (seg.get("systemCode") != null)
                tag.append(" systemCode=\"").append(escapeXmlAttr(seg.get("systemCode"))).append("\"");
            if (seg.get("subSystemCode") != null)
                tag.append(" subSystemCode=\"").append(escapeXmlAttr(seg.get("subSystemCode"))).append("\"");
            tag.append(" subSubSystemCode=\"").append(escapeXmlAttr(subSub)).append("\"");
            if (seg.get("assyCode") != null)
                tag.append(" assyCode=\"").append(escapeXmlAttr(seg.get("assyCode"))).append("\"");
            if (seg.get("disassyCode") != null)
                tag.append(" disassyCode=\"").append(escapeXmlAttr(seg.get("disassyCode"))).append("\"");
            tag.append(" disassyCodeVariant=\"").append(escapeXmlAttr(variant)).append("\"");
            // infoCode/infoCodeVariant/itemLocationCode 直接取字段（默认值 "A"）
            String infoCode = dm.getInfoCode();
            if (!StringUtils.hasText(infoCode)) return null;  // infoCode 必需
            tag.append(" infoCode=\"").append(escapeXmlAttr(infoCode)).append("\"");
            String infoCodeVariant = StringUtils.hasText(dm.getInfoCodeVariant()) ? dm.getInfoCodeVariant() : "A";
            tag.append(" infoCodeVariant=\"").append(escapeXmlAttr(infoCodeVariant)).append("\"");
            String itemLocationCode = StringUtils.hasText(dm.getIetmLocationCode()) ? dm.getIetmLocationCode() : "A";
            tag.append(" itemLocationCode=\"").append(escapeXmlAttr(itemLocationCode)).append("\"");
            tag.append("/>");
            return tag.toString();
        } catch (Exception e) {
            log.error("buildDmCodeXmlFromEntity 构建失败，dm.id={}", dm.getId(), e);
            return null;
        }
    }

    /** 递归查找第一个指定 localName 的元素（忽略命名空间前缀） */
    private org.w3c.dom.Element findFirstByName(org.w3c.dom.Element root, String localName) {
        String name = root.getLocalName() != null ? root.getLocalName() : root.getTagName();
        if (localName.equals(name)) {
            return root;
        }
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                org.w3c.dom.Element found = findFirstByName((org.w3c.dom.Element) child, localName);
                if (found != null) return found;
            }
        }
        return null;
    }

    /** XML属性值转义（针对 " < > &） */
    private String escapeXmlAttr(String val) {
        if (val == null) return "";
        return val.replace("&", "&amp;").replace("\"", "&quot;")
                  .replace("<", "&lt;").replace(">", "&gt;");
    }

    /** 根据DM实体+已提取的dmCodeXml+入参，组装完整的 {@code <dmRef>} 字符串 */
    private String buildSingleDmRef(IetmDataModule dm, String dmCodeXml, DmRefBuildItemVO item) {
        boolean includeVersion = Boolean.TRUE.equals(item.getIncludeVersion());
        String fragment = item.getReferredFragment();

        // referredFragment 属性（修复旧系统BUG：应取 item.referredFragment 而非空局部变量）
        String fragAttr = StringUtils.hasText(fragment)
                ? " referredFragment=\"" + escapeXmlAttr(fragment) + "\"" : "";

        StringBuilder xml = new StringBuilder();
        xml.append("<dmRef xlink:type=\"simple\" xlink:show=\"replace\" xlink:actuate=\"onRequest\"")
           .append(fragAttr).append(">\n");
        xml.append("  <dmRefIdent>\n");
        xml.append("    ").append(dmCodeXml).append("\n");

        // issueInfo节点
        appendIssueInfo(xml, dm, includeVersion);

        // language节点
        appendLanguage(xml, dm);

        xml.append("  </dmRefIdent>\n");

        // dmRefAddressItems节点
        appendDmRefAddressItems(xml, dm, includeVersion);

        xml.append("</dmRef>");
        return xml.toString();
    }

    /** 追加issueInfo节点（仅含版本时生成） */
    private void appendIssueInfo(StringBuilder xml, IetmDataModule dm, boolean includeVersion) {
        if (includeVersion && StringUtils.hasText(dm.getIssueNo())) {
            xml.append("    <issueInfo issueNumber=\"")
               .append(escapeXmlAttr(dm.getIssueNo())).append("\" inWork=\"")
               .append(escapeXmlAttr(dm.getInWork())).append("\"/>\n");
        }
    }

    /** 追加language节点（languageIsoCode非空时生成） */
    private void appendLanguage(StringBuilder xml, IetmDataModule dm) {
        if (StringUtils.hasText(dm.getLanguageIsoCode())) {
            xml.append("    <language languageIsoCode=\"")
               .append(escapeXmlAttr(dm.getLanguageIsoCode())).append("\" countryIsoCode=\"")
               .append(escapeXmlAttr(dm.getCountryIsoCode())).append("\"/>\n");
        }
    }

    /** 追加dmRefAddressItems节点（包含dmTitle和issueDate） */
    private void appendDmRefAddressItems(StringBuilder xml, IetmDataModule dm, boolean includeVersion) {
        boolean hasTech  = StringUtils.hasText(dm.getTechName());
        boolean hasInfo  = StringUtils.hasText(dm.getInfoName());
        boolean hasDate  = includeVersion && dm.getIssueDate() != null;
        boolean hasTitle = hasTech || hasInfo;

        if (hasTitle || hasDate) {
            xml.append("  <dmRefAddressItems>\n");

            // dmTitle节点
            if (hasTitle) {
                xml.append("    <dmTitle>");
                if (hasTech) xml.append("<techName>").append(escapeXmlAttr(dm.getTechName())).append("</techName>");
                if (hasInfo) xml.append("<infoName>").append(escapeXmlAttr(dm.getInfoName())).append("</infoName>");
                xml.append("</dmTitle>\n");
            }

            // issueDate节点
            if (hasDate) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(dm.getIssueDate());
                xml.append("    <issueDate year=\"").append(cal.get(Calendar.YEAR))
                   .append("\" month=\"").append(String.format("%02d", cal.get(Calendar.MONTH) + 1))
                   .append("\" day=\"").append(String.format("%02d", cal.get(Calendar.DAY_OF_MONTH)))
                   .append("\"/>\n");
            }

            xml.append("  </dmRefAddressItems>\n");
        }
    }

    // ── ICN引用自动同步（§需求2：DM引用ICN时自动创建ietm_icn_reference记录） ─────

    /**
     * 同步 DM 中的 ICN 引用关系到 ietm_icn_reference 表
     * <p>
     * 从 XML 中提取所有 graphic/@infoEntityIdent、multimedia/@infoEntityIdent 和 symbol/@infoEntityIdent，
     * 查询对应的 ICN 记录，批量创建 ietm_icn_reference 记录（referenceType=ICN_TO_DM）。
     * <p>
     * 特性：
     * <ul>
     *   <li>幂等性：已存在的引用不重复插入</li>
     *   <li>只增不删：XML 中删除 ICN 引用后，旧记录保持不变</li>
     *   <li>事务性：同步失败会导致整个保存操作回滚</li>
     * </ul>
     *
     * @param dmId DM主键
     * @param xmlContent DM的XML内容
     * @param username 当前用户名
     * @throws Exception 同步失败时抛出，触发事务回滚
     */
    /**
     * 同步ICN引用关系（DM保存场景）
     * <p>委托给 IetmIcnReferenceHelper 执行统一逻辑</p>
     */
    private void syncIcnReferences(String dmId, String xmlContent, String username) throws Exception {
        org.jeecg.modules.ietm.ietmdatamodulemanagement.util.IetmIcnReferenceHelper.syncIcnReferences(
                dmId,
                xmlContent,
                username,
                org.jeecg.modules.ietm.ietmdatamodulemanagement.constants.IetmDataModuleConstants.ICN_REF_REMARK_SAVE,
                icnManageMapper,
                icnReferenceMapper
        );
    }
}
