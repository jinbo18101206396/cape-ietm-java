package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Para转换工具类（para2html / html2para）
 * 基于需求文档§8/§9实现
 *
 * @author jeecgboot
 * @date 2026-09-22
 */
@Slf4j
@Component
public class ParaConverter {

    /**
     * 获取安全的SAXBuilder实例（XXE防护）
     * §7.1 P0-8安全修复
     *
     * @return 配置了XXE防护的SAXBuilder
     */
    private SAXBuilder getSafeBuilder() {
        SAXBuilder builder = new SAXBuilder();

        try {
            // 禁用DTD
            builder.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

            // 禁用外部实体
            builder.setFeature("http://xml.org/sax/features/external-general-entities", false);
            builder.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            // 禁用外部DTD
            builder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            // 设置为非验证模式
            builder.setValidation(false);

        } catch (Exception e) {
            log.error("配置SAXBuilder安全特性失败: {}", e.getMessage(), e);
        }

        return builder;
    }

    /**
     * para2html转换（XML → HTML）
     * §8 实现
     *
     * @param paraXml 完整的para XML字符串（包含<para>标签）
     * @return HTML字符串
     */
    public String para2html(String paraXml) {
        if (paraXml == null || paraXml.trim().isEmpty()) {
            return "";
        }

        try {
            String html = paraXml.trim();

            // §8.2.1 definitionList转table
            html = convertDefinitionList(html);

            // §8.3 captionGroup转table
            html = convertCaptionGroup(html);

            // §8.2.2 基础元素转换
            html = html.replaceAll("<para", "<p")
                .replaceAll("</para>", "</p>")
                .replaceAll("<superScript>", "<sup>")
                .replaceAll("</superScript>", "</sup>")
                .replaceAll("<subScript>", "<sub>")
                .replaceAll("</subScript>", "</sub>")
                .replaceAll("<randomList", "<ul")
                .replaceAll("</randomList>", "</ul>")
                .replaceAll("<sequentialList", "<ol")
                .replaceAll("</sequentialList>", "</ol>")
                .replaceAll("<listItem", "<li")
                .replaceAll("</listItem>", "</li>")
                .replaceAll("<warningAndCautionPara>", "<p>")
                .replaceAll("</warningAndCautionPara>", "</p>")
                .replaceAll("<notePara>", "<p>")
                .replaceAll("</notePara>", "</p>")
                .replaceAll("<emphasis>", "<strong>")
                .replaceAll("</emphasis>", "</strong>");

            // §8.2.3 internalRef转<a>标签
            html = convertInternalRef(html);

            // §8.2.4 dmRef转<a>标签（后端简化：保留原XML，前端再处理）
            html = convertDmRef(html);

            // §8.2.5 symbol转<img>标签（后端简化：保留原XML，前端再处理）
            html = convertSymbol(html);

            return html;

        } catch (Exception e) {
            log.error("para2html转换失败: {}", e.getMessage(), e);
            return paraXml;
        }
    }

    /**
     * html2para转换（HTML → XML）
     * §9 实现
     *
     * @param html UEditor的HTML内容
     * @return 完整的para XML字符串（包含<para>标签）
     */
    public String html2para(String html) {
        if (html == null || html.trim().isEmpty()) {
            return "<para></para>";
        }

        try {
            // §9.2.1 清理UEditor标签
            String para = html.trim()
                .replaceAll("&nbsp;", "")
                .replaceAll("<br>", "")
                .replaceAll("</br>", "")
                .replaceAll("<br/>", "")
                .replaceAll("\n", "")
                .replaceAll("<p></p>", "")
                .replaceAll("<a name=.*?></a>", "")
                .replaceAll("<h\\d.*?>", "<p>")
                .replaceAll("</h\\d>", "</p>")
                .replaceAll("<h.*?/>", "")
                .replaceAll("</p><symbol", "<symbol");

            para = para.replaceAll("<ul.*?>", "<ul>")
                .replaceAll("<ol.*?>", "<ol>")
                .replaceAll("<li.*?>", "<li>")
                .replaceAll("<span.*?>", "")
                .replaceAll("</span>", "")
                .replaceAll("<p.*?>", "<p>")
                .replaceAll("</p><ul", "<ul")
                .replaceAll("</p><ol", "<ol");

            // §9.2.2 基础元素逆转换
            para = para.replaceAll("<sup", "<superScript")
                .replaceAll("</sup>", "</superScript>")
                .replaceAll("<sub", "<subScript")
                .replaceAll("</sub>", "</subScript>")
                .replaceAll("</p><ul>", "<ul>")
                .replaceAll("</p><ol>", "<ol>")
                .replaceAll("</ul></p>", "</ul>")
                .replaceAll("</ol></p>", "</ol>")
                .replaceAll("</ul><p>", "</ul>")
                .replaceAll("</ol><p>", "</ol>")
                .replaceAll("<li><p>", "<li>")
                .replaceAll("</p></li>", "</li>")
                .replaceAll("<ul>", "<randomList>")
                .replaceAll("</ul>", "</randomList>")
                .replaceAll("<ol>", "<sequentialList>")
                .replaceAll("</ol>", "</sequentialList>")
                .replaceAll("<li>", "<listItem><para>")
                .replaceAll("</li>", "</para></listItem>")
                .replaceAll("</table><p>", "</table>")
                .replaceAll("</p><p><table>", "<table>")
                .replaceAll("</p><table", "<table")
                .replaceAll("</table></p>", "</table>")
                .replaceAll("<p>", "<para>")
                .replaceAll("</p>", "</para>")
                .replaceAll("<strong>", "<emphasis>")
                .replaceAll("</strong>", "</emphasis>");

            // §9.2.3 table转definitionList
            para = convertTableToDefinitionList(para);

            // §9.2.4 table转captionGroup
            para = convertTableToCaptionGroup(para);

            // §9.2.6 <img>标签还原为symbol
            para = convertImgToSymbol(para);

            // §9.2.8 <a>标签还原为XML
            para = convertAnchorToXml(para);

            // §9.2.9 添加para标签
            if (!para.startsWith("<para")) {
                para = "<para>" + para;
            }
            if (!para.endsWith("</para>")) {
                para += "</para>";
            }

            return para;

        } catch (Exception e) {
            log.error("html2para转换失败: {}", e.getMessage(), e);
            return "<para></para>";
        }
    }

    // ========== 私有辅助方法 ==========

    /**
     * §8.2.1 definitionList转table
     */
    private String convertDefinitionList(String html) {
        return html.replaceAll(
            "<definitionList>",
            "<table deflist=\"1\">"
        )
        .replaceAll("</definitionList>", "</table>")
        .replaceAll("<definitionListItem>", "<tr>")
        .replaceAll("</definitionListItem>", "</tr>")
        .replaceAll("<listItemTerm/>", "<th></th>")
        .replaceAll("<listItemTerm>", "<th>")
        .replaceAll("</listItemTerm>", "</th>")
        .replaceAll("<listItemDefinition/>", "<td></td>")
        .replaceAll("<listItemDefinition>", "<td>")
        .replaceAll("</listItemDefinition>", "</td>");
    }

    /**
     * §8.3 captionGroup转table（使用安全的SAXBuilder）
     * 完整实现需要解析XML DOM
     */
    private String convertCaptionGroup(String html) {
        try {
            SAXBuilder builder = getSafeBuilder();  // P0-8: 使用安全的Builder
            Document doc = builder.build(new StringReader(html));
            Element root = doc.getRootElement();

            List<Element> captionGroups = root.getChildren("captionGroup");
            for (Element cg : captionGroups) {
                // 解析colspec
                List<Element> colspecs = cg.getChildren("colspec");

                // 转换captionRow
                Element captionBody = cg.getChild("captionBody");
                if (captionBody != null) {
                    List<Element> rows = captionBody.getChildren("captionRow");
                    // ... 转换逻辑（简化）
                }
            }

            XMLOutputter outputter = new XMLOutputter(Format.getCompactFormat());
            return outputter.outputString(doc);

        } catch (JDOMException | IOException e) {
            log.warn("captionGroup转换失败，返回原始内容: {}", e.getMessage());
            return html;
        }
    }

    /**
     * §8.2.3 internalRef转<a>标签
     */
    private String convertInternalRef(String html) {
        // 使用正则表达式匹配internalRef标签
        Pattern pattern = Pattern.compile(
            "<internalRef[^>]*internalRefId=\"([^\"]+)\"[^>]*internalRefTargetType=\"([^\"]+)\"[^>]*>(.*?)</internalRef>",
            Pattern.DOTALL
        );

        Matcher matcher = pattern.matcher(html);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String refid = matcher.group(1);
            String reftype = matcher.group(2);
            String content = matcher.group(3);
            String original = matcher.group(0);

            // 生成<a>标签，保留原始XML
            String anchor = String.format(
                "<a href=\"#%s\" reftype=\"%s\" refid=\"%s\" xml=\"%s\">【%s(%s)】</a>",
                refid, reftype, refid,
                escapeXml(original), reftype, refid
            );

            matcher.appendReplacement(sb, Matcher.quoteReplacement(anchor));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * §8.2.4 dmRef转<a>标签
     */
    private String convertDmRef(String html) {
        Pattern pattern = Pattern.compile(
            "<dmRef[^>]*>(.*?)</dmRef>",
            Pattern.DOTALL
        );

        Matcher matcher = pattern.matcher(html);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String original = matcher.group(0);

            // 生成<a>标签，保留原始XML供前端异步处理
            String anchor = String.format(
                "<a href=\"#dmref\" class=\"dmref\" xml=\"%s\">【DM引用】</a>",
                escapeXml(original)
            );

            matcher.appendReplacement(sb, Matcher.quoteReplacement(anchor));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * §8.2.5 symbol转<img>标签
     */
    private String convertSymbol(String html) {
        Pattern pattern = Pattern.compile(
            "<symbol[^>]*infoEntityIdent=\"([^\"]+)\"[^>]*>",
            Pattern.DOTALL
        );

        Matcher matcher = pattern.matcher(html);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String icn = matcher.group(1);
            String original = matcher.group(0);

            // 生成<img>标签，保留原始XML供前端异步加载
            String img = String.format(
                "<img src=\"\" icn=\"%s\" xml=\"%s\" alt=\"%s\" />",
                icn, escapeXml(original), icn
            );

            matcher.appendReplacement(sb, Matcher.quoteReplacement(img));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * §9.2.3 table转definitionList
     */
    private String convertTableToDefinitionList(String para) {
        return para.replaceAll(
            "<table deflist=\"1\">",
            "<definitionList>"
        )
        .replaceAll("<table deflist='1'>", "<definitionList>")
        .replaceAll("<tr>", "<definitionListItem>")
        .replaceAll("</tr>", "</definitionListItem>")
        .replaceAll("<th></th>", "<listItemTerm/>")
        .replaceAll("<th>", "<listItemTerm>")
        .replaceAll("</th>", "</listItemTerm>")
        .replaceAll("<td></td>", "<listItemDefinition/>")
        .replaceAll("<td>", "<listItemDefinition>")
        .replaceAll("</td>", "</listItemDefinition>");
    }

    /**
     * §9.2.4 table转captionGroup（简化版本）
     */
    private String convertTableToCaptionGroup(String para) {
        // 简化实现：仅处理标记为captionGroup的table
        return para.replaceAll(
            "<table captionGroup=\"1\">",
            "<captionGroup>"
        )
        .replaceAll("</table>", "</captionGroup>");
    }

    /**
     * §9.2.6 <img>标签还原为symbol
     */
    private String convertImgToSymbol(String para) {
        Pattern pattern = Pattern.compile(
            "<img[^>]*xml=\"([^\"]+)\"[^>]*/?>",
            Pattern.DOTALL
        );

        Matcher matcher = pattern.matcher(para);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String xml = unescapeXml(matcher.group(1));
            matcher.appendReplacement(sb, Matcher.quoteReplacement(xml));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * §9.2.8 <a>标签还原为XML
     */
    private String convertAnchorToXml(String para) {
        Pattern pattern = Pattern.compile(
            "<a[^>]*xml=\"([^\"]+)\"[^>]*>.*?</a>",
            Pattern.DOTALL
        );

        Matcher matcher = pattern.matcher(para);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String xml = unescapeXml(matcher.group(1));
            matcher.appendReplacement(sb, Matcher.quoteReplacement(xml));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * XML转义（用于嵌入HTML属性）
     */
    private String escapeXml(String xml) {
        if (xml == null) {
            return "";
        }
        return xml.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&apos;");
    }

    /**
     * XML反转义（从HTML属性还原）
     */
    private String unescapeXml(String xml) {
        if (xml == null) {
            return "";
        }
        return xml.replace("&quot;", "\"")
                  .replace("&apos;", "'")
                  .replace("&lt;", "<")
                  .replace("&gt;", ">")
                  .replace("&amp;", "&");
    }
}
