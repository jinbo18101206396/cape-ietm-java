package org.jeecg.modules.ietm.ietmdatamodulemanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.ws.commons.schema.*;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmTranslate;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.mapper.IetmTranslateMapper;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.service.IIetmDmSchemaService;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import javax.xml.namespace.QName;
import java.io.InputStream;
import java.util.*;

/**
 * XSD → 前端约束对象实现（需求 §12/§25.6）
 * 按 §12.2 CONFIRMED 输出结构重新实现（字节码不可逐行核实，见04说明）
 */
@Slf4j
@Service
public class IetmDmSchemaServiceImpl implements IIetmDmSchemaService {

    @Resource
    private IetmTranslateMapper translateMapper;

    /** maxOccurs 无限时用 Long.MAX_VALUE 字符串（需求 §12.2 CONFIRMED，不用 "unbounded"） */
    private static final String UNBOUNDED = String.valueOf(Long.MAX_VALUE);
    private static final String SCHEMA_BASE = "ietm/";

    // ── schema2Designer ───────────────────────────────────────────────────────

    /**
     * 全局属性/类型索引（localName → 声明）。XSD 中属性多以 ref 引用全局声明，
     * 且类型多为具名 simpleType，需要按名反查才能拿到 pattern/enum 约束。
     */
    private Map<String, XmlSchemaAttribute>      globalAttrs      = new HashMap<>();
    private Map<String, XmlSchemaType>           globalTypes      = new HashMap<>();
    private Map<String, XmlSchemaAttributeGroup> globalAttrGroups = new HashMap<>();
    /** 全局元素模型组（xs:group name=...）索引：内容模型常用 <xs:group ref> 引用，需按名展开 */
    private Map<String, XmlSchemaGroup>          globalGroups     = new HashMap<>();

    /** 遍历 schema 集合内所有元素 */
    private void parseSchema(XmlSchemaCollection col, Map<String, Object> result) {
        // 先建全局索引（含 import 进来的 xlink/rdf 等）
        globalAttrs = new HashMap<>();
        globalTypes = new HashMap<>();
        globalAttrGroups = new HashMap<>();
        globalGroups = new HashMap<>();
        for (XmlSchema xs : col.getXmlSchemas()) {
            for (Map.Entry<QName, XmlSchemaAttribute> e : xs.getAttributes().entrySet())
                globalAttrs.putIfAbsent(e.getKey().getLocalPart(), e.getValue());
            for (Map.Entry<QName, XmlSchemaType> e : xs.getSchemaTypes().entrySet())
                globalTypes.putIfAbsent(e.getKey().getLocalPart(), e.getValue());
            for (Map.Entry<QName, XmlSchemaAttributeGroup> e : xs.getAttributeGroups().entrySet())
                globalAttrGroups.putIfAbsent(e.getKey().getLocalPart(), e.getValue());
            for (Map.Entry<QName, XmlSchemaGroup> e : xs.getGroups().entrySet())
                globalGroups.putIfAbsent(e.getKey().getLocalPart(), e.getValue());
        }
        for (XmlSchema xs : col.getXmlSchemas()) {
            for (Map.Entry<QName, XmlSchemaElement> e : xs.getElements().entrySet()) {
                parseElement(e.getValue(), result);
            }
            for (Map.Entry<QName, XmlSchemaType> e : xs.getSchemaTypes().entrySet()) {
                if (e.getValue() instanceof XmlSchemaComplexType) {
                    parseComplexType(e.getKey().getLocalPart(),
                            (XmlSchemaComplexType) e.getValue(), result);
                }
            }
        }
    }

    private void parseElement(XmlSchemaElement elem, Map<String, Object> result) {
        String name = elem.getName();
        if (name == null) return;
        // 元素类型可能是内联 complexType，也可能是具名类型（type="xxxElemType"）
        XmlSchemaType type = elem.getSchemaType();
        if (type == null && elem.getSchemaTypeName() != null) {
            type = globalTypes.get(elem.getSchemaTypeName().getLocalPart());
        }
        if (type instanceof XmlSchemaComplexType) {
            parseComplexTypeAs(name, (XmlSchemaComplexType) type, result);
        } else if (!result.containsKey(name)) {
            // 简单/无类型叶子：仅当无同名富定义时写空节点，避免 dc:title 等空声明覆盖 S1000D 富元素
            result.put(name, newNode());
        }
    }

    /** 具名 complexType：key 用类型名（保持既有行为） */
    private void parseComplexType(String name, XmlSchemaComplexType ct, Map<String, Object> result) {
        parseComplexTypeAs(name, ct, result);
    }

    /** children/attrs 皆空视为空节点（占位/简单类型），允许被同名富定义覆盖 */
    @SuppressWarnings("unchecked")
    private boolean isEmptyNode(Map<String, Object> node) {
        if (node == null) return true;
        List<String> ch = (List<String>) node.get("children");
        Map<String, Object> at = (Map<String, Object>) node.get("attrs");
        return (ch == null || ch.isEmpty()) && (at == null || at.isEmpty());
    }

    /** 把 complexType ct 的约束写入 result[key] */
    @SuppressWarnings("unchecked")
    private void parseComplexTypeAs(String key, XmlSchemaComplexType ct, Map<String, Object> result) {
        if (key == null) return;
        // 已有富定义则保留；已有空占位（如先解析到的 dc:title）或不存在时才解析写入 → 富定义优先，与解析顺序无关
        Map<String, Object> existing = (Map<String, Object>) result.get(key);
        if (existing != null && !isEmptyNode(existing)) return;
        Map<String, Object> node = newNode();
        List<String> children = (List<String>) node.get("children");
        Map<String, Object> setelem = (Map<String, Object>) node.get("setelem");
        Map<String, Object> attrs   = (Map<String, Object>) node.get("attrs");
        Map<String, Object> setattr = (Map<String, Object>) node.get("setattr");
        parseParticle(ct.getParticle(), children, setelem, ct.isMixed(), false);
        parseAttrItems(ct.getAttributes(), attrs, setattr);
        result.put(key, node);
    }

    /** 处理属性列表：直接属性 + attributeGroup ref（可嵌套） */
    private void parseAttrItems(List<XmlSchemaAttributeOrGroupRef> items,
                                Map<String, Object> attrs, Map<String, Object> setattr) {
        if (items == null) return;
        for (XmlSchemaAttributeOrGroupRef o : items) {
            if (o instanceof XmlSchemaAttribute) {
                parseAttribute((XmlSchemaAttribute) o, attrs, setattr);
            } else if (o instanceof XmlSchemaAttributeGroupRef) {
                XmlSchemaAttributeGroupRef gr = (XmlSchemaAttributeGroupRef) o;
                QName q = gr.getRef() != null ? gr.getRef().getTargetQName() : null;
                XmlSchemaAttributeGroup g = q != null ? globalAttrGroups.get(q.getLocalPart()) : null;
                if (g != null) parseAttrGroup(g, attrs, setattr, new HashSet<>());
            }
        }
    }

    /** 展开 attributeGroup 内的属性与嵌套 group（防环） */
    private void parseAttrGroup(XmlSchemaAttributeGroup g, Map<String, Object> attrs,
                                Map<String, Object> setattr, Set<String> seen) {
        if (g == null || g.getName() == null || !seen.add(String.valueOf(g.getName()))) return;
        for (XmlSchemaAttributeGroupMember m : g.getAttributes()) {
            if (m instanceof XmlSchemaAttribute) {
                parseAttribute((XmlSchemaAttribute) m, attrs, setattr);
            } else if (m instanceof XmlSchemaAttributeGroupRef) {
                XmlSchemaAttributeGroupRef gr = (XmlSchemaAttributeGroupRef) m;
                QName q = gr.getRef() != null ? gr.getRef().getTargetQName() : null;
                XmlSchemaAttributeGroup ng = q != null ? globalAttrGroups.get(q.getLocalPart()) : null;
                if (ng != null) parseAttrGroup(ng, attrs, setattr, seen);
            } else if (m instanceof XmlSchemaAttributeGroup) {
                parseAttrGroup((XmlSchemaAttributeGroup) m, attrs, setattr, seen);
            }
        }
    }

    private Map<String, Object> newNode() {
        Map<String, Object> n = new LinkedHashMap<>();
        n.put("children", new ArrayList<String>());
        n.put("setelem", new LinkedHashMap<String, Object>());
        n.put("attrs",   new LinkedHashMap<String, Object>());
        n.put("setattr", new LinkedHashMap<String, Object>());
        return n;
    }

    @Override
    public Map<String, Object> schema2Designer(String standard, String xsd) {
        Map<String, Object> result = new LinkedHashMap<>();
        String path = SCHEMA_BASE + standard.replace(".", "") + "/schema/" + xsd;
        try {
            // 必须用带 systemId 的方式读取，否则 xs:include 相对路径无法解析 → schema 为空
            java.net.URL xsdUrl = getClass().getClassLoader().getResource(path);
            if (xsdUrl == null) { log.warn("XSD not found: {}", path); return result; }
            XmlSchemaCollection col = new XmlSchemaCollection();
            try (java.io.InputStream in = xsdUrl.openStream()) {
                org.xml.sax.InputSource is = new org.xml.sax.InputSource(in);
                is.setSystemId(xsdUrl.toString());  // 关键：让 xs:include 能相对此 URL 解析
                col.read(is);
                parseSchema(col, result);
            }
        } catch (Exception e) {
            log.error("schema2Designer error, standard={}, xsd={}", standard, xsd, e);
        }
        return result;
    }

    // ── 粒子与属性解析 ─────────────────────────────────────────────────────────

    private void parseParticle(XmlSchemaParticle p, List<String> children,
                               Map<String, Object> setelem, boolean mixed, boolean inChoice) {
        parseParticle(p, children, setelem, mixed, inChoice, new HashSet<>());
    }

    @SuppressWarnings("unchecked")
    private void parseParticle(XmlSchemaParticle p, List<String> children,
                               Map<String, Object> setelem, boolean mixed, boolean inChoice,
                               Set<String> seenGroups) {
        if (p == null) return;
        if (p instanceof XmlSchemaGroupRef) {
            // <xs:group ref="xxx"> 引用全局模型组：按名展开其内容粒子（防环）
            XmlSchemaGroupRef gr = (XmlSchemaGroupRef) p;
            QName q = gr.getRefName();
            String gname = q != null ? q.getLocalPart() : null;
            if (gname == null || !seenGroups.add(gname)) return;
            XmlSchemaGroup g = globalGroups.get(gname);
            if (g != null) parseParticle(g.getParticle(), children, setelem, mixed, inChoice, seenGroups);
            return;
        }
        if (p instanceof XmlSchemaElement) {
            XmlSchemaElement ce = (XmlSchemaElement) p;
            QName refQ = ce.getRef() != null ? ce.getRef().getTargetQName() : null;
            String cname = ce.getName() != null ? ce.getName()
                    : (refQ != null ? refQ.getLocalPart() : null);
            if (cname == null) return;
            if (!children.contains(cname)) children.add(cname);
            Map<String, Object> occ = new LinkedHashMap<>();
            occ.put("minocc", String.valueOf(ce.getMinOccurs()));
            long max = ce.getMaxOccurs();
            occ.put("maxocc", max == Long.MAX_VALUE ? UNBOUNDED : String.valueOf(max));
            occ.put("ifchoice", inChoice ? "true" : "false");
            if (mixed) occ.put("mixed", "true");
            if (ce.getSchemaType() != null && !(ce.getSchemaType() instanceof XmlSchemaComplexType)) {
                occ.put("typnam", "string");
            } else if (ce.getSchemaTypeName() != null
                    && "string".equalsIgnoreCase(ce.getSchemaTypeName().getLocalPart())) {
                occ.put("typnam", "string");
            }
            setelem.put(cname, occ);
        } else if (p instanceof XmlSchemaSequence) {
            for (XmlSchemaSequenceMember m : ((XmlSchemaSequence) p).getItems())
                parseParticle(asP(m), children, setelem, mixed, inChoice, seenGroups);
        } else if (p instanceof XmlSchemaChoice) {
            for (XmlSchemaChoiceMember m : ((XmlSchemaChoice) p).getItems())
                parseParticle(asP(m), children, setelem, mixed, true, seenGroups);
        } else if (p instanceof XmlSchemaAll) {
            for (XmlSchemaAllMember m : ((XmlSchemaAll) p).getItems())
                parseParticle(asP(m), children, setelem, mixed, inChoice, seenGroups);
        }
    }

    private XmlSchemaParticle asP(Object o) { return (XmlSchemaParticle) o; }

    private void parseAttribute(XmlSchemaAttribute attr,
                                Map<String, Object> attrs, Map<String, Object> setattr) {
        // 属性名：内联 name 优先，否则取 ref 目标名（XSD 中多为 ref）
        String aname = attr.getName();
        XmlSchemaAttribute globalDecl = null;
        if (aname == null && attr.getRef() != null && attr.getRef().getTargetQName() != null) {
            aname = attr.getRef().getTargetQName().getLocalPart();
            globalDecl = globalAttrs.get(aname);   // ref 场景：约束在全局声明上
        }
        if (aname == null) return;

        List<String> constraints = new ArrayList<>();
        if (attr.getUse() == XmlSchemaUse.REQUIRED) constraints.add("use:REQUIRED");

        // 类型解析：本地 attr 优先，否则用全局声明；类型可能内联，也可能具名（type="xxxAttType"）
        XmlSchemaAttribute typeSrc = globalDecl != null ? globalDecl : attr;
        XmlSchemaSimpleType st = typeSrc.getSchemaType();
        if (st == null && typeSrc.getSchemaTypeName() != null) {
            XmlSchemaType t = globalTypes.get(typeSrc.getSchemaTypeName().getLocalPart());
            if (t instanceof XmlSchemaSimpleType) st = (XmlSchemaSimpleType) t;
        }

        List<String> enums = new ArrayList<>();
        if (st != null && st.getContent() instanceof XmlSchemaSimpleTypeRestriction) {
            XmlSchemaSimpleTypeRestriction r = (XmlSchemaSimpleTypeRestriction) st.getContent();
            for (XmlSchemaFacet f : r.getFacets()) {
                if (f instanceof XmlSchemaEnumerationFacet)
                    enums.add(String.valueOf(((XmlSchemaEnumerationFacet) f).getValue()));
                else if (f instanceof XmlSchemaPatternFacet)
                    constraints.add("pttn:" + ((XmlSchemaPatternFacet) f).getValue());
                else if (f instanceof XmlSchemaMaxLengthFacet)
                    constraints.add("maxi:" + ((XmlSchemaMaxLengthFacet) f).getValue());
            }
        }
        attrs.put(aname, enums.isEmpty() ? null : enums);
        if (!constraints.isEmpty()) setattr.put(aname, constraints);
    }

    // ── 中文化与映射 ───────────────────────────────────────────────────────────

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> schema2DesignerCn(Map<String, Object> enSchema,
                                                 Map<String, String> en2cnElem) {
        if (enSchema == null) return new LinkedHashMap<>();
        Map<String, Object> cn = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : enSchema.entrySet()) {
            String cnKey = en2cnElem.getOrDefault(e.getKey(), e.getKey());
            Map<String, Object> node = (Map<String, Object>) e.getValue();
            Map<String, Object> cnNode = new LinkedHashMap<>();
            List<String> children = (List<String>) node.get("children");
            List<String> cnChildren = new ArrayList<>();
            for (String c : children) cnChildren.add(en2cnElem.getOrDefault(c, c));
            cnNode.put("children", cnChildren);
            Map<String, Object> setelem = (Map<String, Object>) node.get("setelem");
            Map<String, Object> cnSetelem = new LinkedHashMap<>();
            for (Map.Entry<String, Object> se : setelem.entrySet())
                cnSetelem.put(en2cnElem.getOrDefault(se.getKey(), se.getKey()), se.getValue());
            cnNode.put("setelem", cnSetelem);
            cnNode.put("attrs",   node.get("attrs"));   // 属性 key 保持英文（§12.3）
            cnNode.put("setattr", node.get("setattr"));
            cn.put(cnKey, cnNode);
        }
        return cn;
    }

    @Override
    public Map<String, String> loadEn2CnElem(String standard) {
        Map<String, String> map = new HashMap<>();
        for (IetmTranslate t : queryTranslate(standard)) map.put(t.getEnName(), t.getCnName());
        return map;
    }

    @Override
    public Map<String, String> loadCn2EnElem(String standard) {
        Map<String, String> map = new HashMap<>();
        for (IetmTranslate t : queryTranslate(standard)) map.put(t.getCnName(), t.getEnName());
        return map;
    }

    private List<IetmTranslate> queryTranslate(String standard) {
        LambdaQueryWrapper<IetmTranslate> w = new LambdaQueryWrapper<>();
        w.eq(IetmTranslate::getTransType, "elem");
        w.and(q -> q.isNull(IetmTranslate::getIetmStandard)
                .or().eq(IetmTranslate::getIetmStandard, standard));
        return translateMapper.selectList(w);
    }
}
