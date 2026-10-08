package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

/**
 * Para设计器转换工具类
 * 实现XML↔HTML双向转换，覆盖9类元素
 *
 * @author IETM Team
 * @date 2026-09-23
 */
public class ParaConverter {

    private static final Logger log = LoggerFactory.getLogger(ParaConverter.class);

    /**
     * para2html转换（XML → HTML）
     * @param xml para XML字符串
     * @return HTML字符串
     */
    public static String para2html(String xml) {
        if (xml == null || xml.trim().isEmpty()) {
            return "";
        }

        try {
            // 使用JDOM2解析XML
            SAXBuilder builder = new SAXBuilder();
            // XXE防护
            builder.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            builder.setFeature("http://xml.org/sax/features/external-general-entities", false);
            builder.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            Document doc = builder.build(new StringReader(xml));
            Element root = doc.getRootElement();

            // 转换根元素
            String html = convertElementToHtml(root);

            return html;

        } catch (JDOMException | IOException e) {
            log.error("para2html转换失败", e);
            throw new RuntimeException("para2html转换失败: " + e.getMessage(), e);
        }
    }

    /**
     * html2para转换（HTML → XML）
     * @param html HTML字符串
     * @return para XML字符串
     */
    public static String html2para(String html) {
        if (html == null || html.trim().isEmpty()) {
            return "<para></para>";
        }

        try {
            // 基础清理
            String para = html.trim()
                .replaceAll("&nbsp;", "")
                .replaceAll("<br>|</br>|<br/>", "")
                .replaceAll("\n", "")
                .replaceAll("<p></p>", "")
                .replaceAll("<a name=.*?></a>", "")
                .replaceAll("<h\\d.*?>", "<p>")
                .replaceAll("</h\\d>", "</p>")
                .replaceAll("<h.*?/>", "")
                .replaceAll("</p><symbol", "<symbol");

            // 列表转换
            para = para.replaceAll("<ul.*?>", "<ul>")
                .replaceAll("<ol.*?>", "<ol>")
                .replaceAll("<li.*?>", "<li>")
                .replaceAll("<span.*?>", "")
                .replaceAll("</span>", "")
                .replaceAll("<p.*?>", "<p>")
                .replaceAll("</p><ul", "<ul")
                .replaceAll("</p><ol", "<ol");

            // 基础元素逆转换
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

            // table转definitionList
            para = convertDeflistTableToXml(para);

            // table转captionGroup
            para = convertCaptionTableToXml(para);

            // <a>标签还原为internalRef/dmRef
            para = convertAnchorsToXml(para);

            // <img>标签还原为symbol
            para = convertImagesToXml(para);

            return para;

        } catch (Exception e) {
            log.error("html2para转换失败", e);
            throw new RuntimeException("html2para转换失败: " + e.getMessage(), e);
        }
    }

    /**
     * 递归转换Element到HTML
     */
    private static String convertElementToHtml(Element element) {
        String tagName = element.getName();
        StringBuilder html = new StringBuilder();

        // 根据标签名转换
        switch (tagName) {
            case "para":
                html.append("<p");
                appendAttributes(html, element);
                html.append(">");
                appendChildren(html, element);
                html.append("</p>");
                break;

            case "emphasis":
                html.append("<strong>");
                appendChildren(html, element);
                html.append("</strong>");
                break;

            case "superScript":
                html.append("<sup>");
                appendChildren(html, element);
                html.append("</sup>");
                break;

            case "subScript":
                html.append("<sub>");
                appendChildren(html, element);
                html.append("</sub>");
                break;

            case "randomList":
                html.append("<ul>");
                appendChildren(html, element);
                html.append("</ul>");
                break;

            case "sequentialList":
                html.append("<ol>");
                appendChildren(html, element);
                html.append("</ol>");
                break;

            case "listItem":
                html.append("<li>");
                appendChildren(html, element);
                html.append("</li>");
                break;

            case "definitionList":
                html.append(convertDefinitionListToTable(element));
                break;

            case "captionGroup":
                html.append(convertCaptionGroupToTable(element));
                break;

            case "internalRef":
                html.append(convertInternalRefToAnchor(element));
                break;

            case "dmRef":
                html.append(convertDmRefToAnchor(element));
                break;

            case "symbol":
                html.append(convertSymbolToImg(element));
                break;

            default:
                // 未识别标签，递归处理子元素
                appendChildren(html, element);
                break;
        }

        return html.toString();
    }

    /**
     * 追加子元素
     */
    private static void appendChildren(StringBuilder html, Element element) {
        List<org.jdom2.Content> content = element.getContent();
        for (org.jdom2.Content obj : content) {
            if (obj instanceof Element) {
                html.append(convertElementToHtml((Element) obj));
            } else if (obj instanceof org.jdom2.Text) {
                html.append(((org.jdom2.Text) obj).getText());
            }
        }
    }

    /**
     * 追加属性
     */
    private static void appendAttributes(StringBuilder html, Element element) {
        List<org.jdom2.Attribute> attrs = element.getAttributes();
        for (org.jdom2.Attribute attr : attrs) {
            html.append(" ").append(attr.getName()).append("=\"")
                .append(attr.getValue().replace("\"", "&quot;")).append("\"");
        }
    }

    /**
     * definitionList转table
     */
    private static String convertDefinitionListToTable(Element element) {
        StringBuilder html = new StringBuilder("<table deflist=\"1\">");

        List<Element> items = element.getChildren("definitionListItem");
        for (Element item : items) {
            html.append("<tr>");

            Element term = item.getChild("listItemTerm");
            if (term != null) {
                html.append("<th>");
                appendChildren(html, term);
                html.append("</th>");
            }

            Element def = item.getChild("listItemDefinition");
            if (def != null) {
                html.append("<td>");
                appendChildren(html, def);
                html.append("</td>");
            }

            html.append("</tr>");
        }

        html.append("</table>");
        return html.toString();
    }

    /**
     * captionGroup转table
     */
    private static String convertCaptionGroupToTable(Element element) {
        StringBuilder html = new StringBuilder("<table caption=\"1\">");

        List<Element> rows = element.getChildren("captionRow");
        for (Element row : rows) {
            html.append("<tr>");

            List<Element> entries = row.getChildren("captionEntry");
            for (Element entry : entries) {
                html.append("<td");

                // 处理跨列
                String namest = entry.getAttributeValue("namest");
                String nameend = entry.getAttributeValue("nameend");
                if (namest != null && nameend != null && !namest.equals(nameend)) {
                    // 简化：假设每列宽度相等
                    html.append(" colspan=\"2\"");
                }

                // 处理跨行
                String morerows = entry.getAttributeValue("morerows");
                if (morerows != null && !morerows.equals("0")) {
                    int rowspan = Integer.parseInt(morerows) + 1;
                    html.append(" rowspan=\"").append(rowspan).append("\"");
                }

                html.append(">");

                // 处理captionLine
                List<Element> lines = entry.getChildren("captionLine");
                for (Element line : lines) {
                    html.append(line.getText()).append("<br/>");
                }

                html.append("</td>");
            }

            html.append("</tr>");
        }

        html.append("</table>");
        return html.toString();
    }

    /**
     * internalRef转<a>标签
     */
    private static String convertInternalRefToAnchor(Element element) {
        String refId = element.getAttributeValue("internalRefId");
        String refType = element.getAttributeValue("internalRefTargetType");

        // 将XML转为转义字符串
        XMLOutputter outputter = new XMLOutputter(Format.getCompactFormat());
        String xmlStr = outputter.outputString(element)
            .replace("\"", "`")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("&", "&amp;");

        return String.format("<a href=\"javascript:void(0);\" xml=\"%s\">【%s(%s)】</a>",
            xmlStr, refType != null ? refType : "", refId != null ? refId : "");
    }

    /**
     * dmRef转<a>标签
     */
    private static String convertDmRefToAnchor(Element element) {
        // 简化实现：返回占位符，实际由前端调用后端接口获取DMC
        XMLOutputter outputter = new XMLOutputter(Format.getCompactFormat());
        String xmlStr = outputter.outputString(element)
            .replace("\"", "`")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("&", "&amp;");

        return String.format("<a href=\"javascript:void(0);\" xml=\"%s\">【DM引用】</a>", xmlStr);
    }

    /**
     * symbol转<img>标签
     */
    private static String convertSymbolToImg(Element element) {
        String icn = element.getAttributeValue("infoEntityIdent");
        String symbolId = element.getAttributeValue("symbolid");

        // 简化实现：返回占位符，实际由前端调用后端接口获取图片
        XMLOutputter outputter = new XMLOutputter(Format.getCompactFormat());
        String xmlStr = outputter.outputString(element)
            .replace("\"", "`")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("&", "&amp;");

        String imgSrc = String.format("/jeecg-boot/ietm/icn/tmpICN/%s", symbolId != null ? symbolId : "");
        return String.format("<img src=\"%s\" xml=\"%s\">", imgSrc, xmlStr);
    }

    /**
     * table deflist转definitionList
     */
    private static String convertDeflistTableToXml(String html) {
        // 简化实现：使用正则替换
        return html.replaceAll("<table deflist=\"1\">", "<definitionList>")
            .replaceAll("</table>", "</definitionList>")
            .replaceAll("<tbody>", "")
            .replaceAll("</tbody>", "")
            .replaceAll("<tr.*?>", "<definitionListItem>")
            .replaceAll("</tr>", "</definitionListItem>")
            .replaceAll("<th.*?>", "<listItemTerm>")
            .replaceAll("</th>", "</listItemTerm>")
            .replaceAll("</para></td>", "</td>")
            .replaceAll("<td.*?>", "<listItemDefinition><para>")
            .replaceAll("</td>", "</para></listItemDefinition>");
    }

    /**
     * table caption转captionGroup
     */
    private static String convertCaptionTableToXml(String html) {
        // 简化实现：使用正则替换
        return html.replaceAll("<table caption=\"1\">", "<captionGroup>")
            .replaceAll("</table>", "</captionGroup>")
            .replaceAll("<tr.*?>", "<captionRow>")
            .replaceAll("</tr>", "</captionRow>")
            .replaceAll("<td.*?>", "<captionEntry><captionLine>")
            .replaceAll("</td>", "</captionLine></captionEntry>");
    }

    /**
     * <a>标签还原为internalRef/dmRef
     */
    private static String convertAnchorsToXml(String html) {
        // 匹配<a>标签中的xml属性
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("<a href=.*?xml=\"([^\"]+)\".*?</a>");
        java.util.regex.Matcher matcher = pattern.matcher(html);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String xmlAttr = matcher.group(1);
            // 反转义
            String xml = xmlAttr.replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("`", "\"");
            matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(xml));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * <img>标签还原为symbol
     */
    private static String convertImagesToXml(String html) {
        // 匹配<img>标签中的xml属性
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("<img.*?xml=\"([^\"]+)\".*?>");
        java.util.regex.Matcher matcher = pattern.matcher(html);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String xmlAttr = matcher.group(1);
            // 反转义
            String xml = xmlAttr.replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("`", "\"");
            matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(xml));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
