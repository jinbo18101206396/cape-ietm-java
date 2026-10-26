package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

import org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.util.*;

/**
 * Figure XML转换工具
 *
 * 功能:
 * - xmlToFigure: XML → 对象
 * - figureToXml: 对象 → XML
 * - toCnFigureXml: 英文标签 → 中文标签
 * - toEnFigureXml: 中文标签 → 英文标签
 * - escapeXml: XSS防护
 */
public class FigureConverter {

    /**
     * XML → 对象转换
     *
     * @param figureXml Figure XML字符串
     * @return FigureResponseVO对象
     * @throws Exception 解析失败
     */
    public static FigureResponseVO xmlToFigure(String figureXml) throws Exception {
        if (figureXml == null || figureXml.trim().isEmpty()) {
            throw new IllegalArgumentException("XML内容不能为空");
        }

        figureXml = figureXml.replace("xlink:", "xlink_");

        // 安全的DocumentBuilder配置 - 禁用XXE攻击
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        try {
            // 禁用外部DTD
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

            // 禁用外部通用实体
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);

            // 禁用外部参数实体
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            // 禁用加载外部DTD
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            // 禁用实体引用创建
            factory.setFeature("http://apache.org/xml/features/dom/create-entity-ref-nodes", false);

            // 禁用XInclude
            factory.setXIncludeAware(false);

            // 禁用实体扩展
            factory.setExpandEntityReferences(false);

        } catch (ParserConfigurationException e) {
            throw new RuntimeException("XML解析器配置失败: " + e.getMessage(), e);
        }

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(figureXml.getBytes("UTF-8")));

        Element figureElement = doc.getDocumentElement();
        FigureResponseVO result = new FigureResponseVO();

        result.setTitle(getElementText(figureElement, "title"));
        result.setId(figureElement.getAttribute("id"));

        // 提取图形
        List<GraphicItemVO> graphics = new ArrayList<>();
        NodeList graphicNodes = figureElement.getElementsByTagName("graphic");
        for (int i = 0; i < graphicNodes.getLength(); i++) {
            Element graphicElement = (Element) graphicNodes.item(i);
            GraphicItemVO graphic = new GraphicItemVO();

            // 优先从<graphic>标签的infoEntityIdent属性读取（新格式）
            String icn = graphicElement.getAttribute("infoEntityIdent");

            // 如果<graphic>标签没有infoEntityIdent属性，尝试从<image>子元素读取（旧格式兼容）
            if (icn == null || icn.isEmpty()) {
                NodeList imageNodes = graphicElement.getElementsByTagName("image");
                if (imageNodes.getLength() > 0) {
                    Element imageElement = (Element) imageNodes.item(0);
                    icn = imageElement.getAttribute("infoEntityIdent");
                }
            }

            graphic.setIcn(icn);
            graphic.setFilename("");

            // 提取热点
            List<HotspotItemVO> hotspots = new ArrayList<>();
            NodeList hotspotNodes = graphicElement.getElementsByTagName("hotspot");
            for (int j = 0; j < hotspotNodes.getLength(); j++) {
                Element hsElement = (Element) hotspotNodes.item(j);
                HotspotItemVO hs = new HotspotItemVO();
                hs.setApsid(hsElement.getAttribute("applicationStructureIdent"));
                hs.setName(hsElement.getAttribute("applicationStructureName"));
                hs.setId(hsElement.getAttribute("id"));
                hotspots.add(hs);
            }
            graphic.setHotspots(hotspots);
            graphics.add(graphic);
        }
        result.setGraphics(graphics);

        // 提取图例
        List<LegendItemVO> legends = new ArrayList<>();
        NodeList itemNodes = figureElement.getElementsByTagName("definitionListItem");
        for (int i = 0; i < itemNodes.getLength(); i++) {
            Element itemElement = (Element) itemNodes.item(i);
            LegendItemVO legend = new LegendItemVO();
            legend.setNo(getElementText(itemElement, "listItemTerm"));
            legend.setName(getElementText(itemElement, "para"));
            legends.add(legend);
        }
        result.setLegends(legends);

        return result;
    }

    /**
     * 对象 → XML转换
     *
     * @param figure Figure对象
     * @return Figure XML字符串
     */
    public static String figureToXml(FigureResponseVO figure) {
        StringBuilder xml = new StringBuilder();

        xml.append("<figure");
        if (figure.getId() != null && !figure.getId().isEmpty()) {
            xml.append(" id=\"").append(escapeXml(figure.getId())).append("\"");
        }
        xml.append(">\n");

        xml.append("<title>").append(escapeXml(figure.getTitle())).append("</title>\n");

        if (figure.getGraphics() != null) {
            for (GraphicItemVO graphic : figure.getGraphics()) {
                // S1000D规范: <graphic>标签应该是自闭合的，带有xlink属性和infoEntityIdent
                xml.append("<graphic")
                   .append(" xlink:type=\"simple\"")
                   .append(" xlink:show=\"replace\"")
                   .append(" xlink:actuate=\"onRequest\"")
                   .append(" infoEntityIdent=\"").append(escapeXml(graphic.getIcn())).append("\"");

                // 如果有热点，则不自闭合
                if (graphic.getHotspots() != null && !graphic.getHotspots().isEmpty()) {
                    xml.append(">\n");
                    for (HotspotItemVO hs : graphic.getHotspots()) {
                        xml.append("<hotspot")
                           .append(" applicationStructureIdent=\"").append(escapeXml(hs.getApsid())).append("\"")
                           .append(" applicationStructureName=\"").append(escapeXml(hs.getName())).append("\"");
                        if (hs.getId() != null && !hs.getId().isEmpty()) {
                            xml.append(" id=\"").append(escapeXml(hs.getId())).append("\"");
                        }
                        xml.append("/>\n");
                    }
                    xml.append("</graphic>\n");
                } else {
                    // 没有热点，使用自闭合标签
                    xml.append("/>\n");
                }
            }
        }

        if (figure.getLegends() != null && !figure.getLegends().isEmpty()) {
            xml.append("<legend>\n<definitionList>\n");
            for (LegendItemVO legend : figure.getLegends()) {
                xml.append("<definitionListItem>\n");
                xml.append("<listItemTerm>").append(escapeXml(legend.getNo())).append("</listItemTerm>\n");
                xml.append("<listItemDefinition>\n");
                xml.append("<para>").append(escapeXml(legend.getName())).append("</para>\n");
                xml.append("</listItemDefinition>\n");
                xml.append("</definitionListItem>\n");
            }
            xml.append("</definitionList>\n</legend>\n");
        }

        xml.append("</figure>");
        return xml.toString();
    }

    /**
     * XSS防护 - XML特殊字符转义（优化性能）
     */
    public static String escapeXml(String str) {
        if (str == null) return "";

        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&apos;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 英文标签 → 中文标签
     */
    public static String toCnFigureXml(String enXml) {
        if (enXml == null) return "";
        String cnXml = enXml
            .replace("<figure", "<插图")
            .replace("</figure>", "</插图>")
            .replace("<title", "<标题")
            .replace("</title>", "</标题>")
            .replace("<graphic", "<图形")
            .replace("</graphic>", "</图形>")
            .replace("<image", "<图像")
            .replace("<hotspot", "<热点")
            .replace("</hotspot>", "</热点>")
            .replace("<legend", "<图例")
            .replace("</legend>", "</图例>")
            .replace("<definitionList", "<定义列表")
            .replace("</definitionList>", "</定义列表>")
            .replace("<definitionListItem", "<定义项")
            .replace("</definitionListItem>", "</定义项>")
            .replace("<listItemTerm", "<列表项标号")
            .replace("</listItemTerm>", "</列表项标号>")
            .replace("<listItemDefinition", "<列表项定义")
            .replace("</listItemDefinition>", "</列表项定义>")
            .replace("<para", "<段落")
            .replace("</para>", "</段落>")
            .replace("infoEntityIdent", "信息实体标识")
            .replace("applicationStructureIdent", "应用结构标识")
            .replace("applicationStructureName", "应用结构名称");
        return cnXml;
    }

    /**
     * 中文标签 → 英文标签
     */
    public static String toEnFigureXml(String cnXml) {
        if (cnXml == null) return "";
        String enXml = cnXml
            .replace("<插图", "<figure")
            .replace("</插图>", "</figure>")
            .replace("<标题", "<title")
            .replace("</标题>", "</title>")
            .replace("<图形", "<graphic")
            .replace("</图形>", "</graphic>")
            .replace("<图像", "<image")
            .replace("<热点", "<hotspot")
            .replace("</热点>", "</hotspot>")
            .replace("<图例", "<legend")
            .replace("</图例>", "</legend>")
            .replace("<定义列表", "<definitionList")
            .replace("</定义列表>", "</definitionList>")
            .replace("<定义项", "<definitionListItem")
            .replace("</定义项>", "</definitionListItem>")
            .replace("<列表项标号", "<listItemTerm")
            .replace("</列表项标号>", "</listItemTerm>")
            .replace("<列表项定义", "<listItemDefinition")
            .replace("</列表项定义>", "</listItemDefinition>")
            .replace("<段落", "<para")
            .replace("</段落>", "</para>")
            .replace("信息实体标识", "infoEntityIdent")
            .replace("应用结构标识", "applicationStructureIdent")
            .replace("应用结构名称", "applicationStructureName");
        return enXml;
    }

    /**
     * 辅助方法 - 获取元素文本内容
     */
    private static String getElementText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent().trim();
        }
        return "";
    }
}
