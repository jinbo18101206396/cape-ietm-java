package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.*;

/**
 * LevelledPara XML 处理工具类
 * 提供 XML 解析、验证、格式化、模板生成等功能
 */
@Slf4j
public class LevelledParaXmlUtil {

    private static final List<String> CONTENT_TYPES = Arrays.asList(
        "para", "warning", "caution", "note", "figure", "multimedia", "table"
    );

    private static final int MAX_DEPTH = 10;

    /**
     * 解析 levelledPara XML 为树形结构
     */
    public static Map<String, Object> parseXmlToTree(String xmlString) throws Exception {
        if (xmlString == null || xmlString.trim().isEmpty()) {
            throw new IllegalArgumentException("XML内容不能为空");
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xmlString.getBytes("UTF-8")));

        Element root = doc.getDocumentElement();
        if (!"levelledPara".equals(root.getNodeName())) {
            throw new IllegalArgumentException("根节点必须是 levelledPara");
        }

        List<Map<String, Object>> treeNodes = new ArrayList<>();
        parseChildren(root, treeNodes, "");

        Map<String, Object> result = new HashMap<>();
        result.put("nodes", treeNodes);
        result.put("count", countNodes(treeNodes));

        return result;
    }

    /**
     * 递归解析子节点
     */
    private static void parseChildren(Element parent, List<Map<String, Object>> targetList, String parentId) {
        NodeList children = parent.getChildNodes();
        int index = 1;

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);

            // 跳过非元素节点
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element element = (Element) child;
            String tagName = element.getTagName();

            // 只处理7种内容类型
            if (!CONTENT_TYPES.contains(tagName)) {
                continue;
            }

            // 生成层级ID
            String nodeId = parentId.isEmpty() ? String.valueOf(index) : parentId + "." + index;

            // 提取标题
            String title = extractTitle(element, tagName);

            // 创建树节点
            Map<String, Object> treeNode = new HashMap<>();
            treeNode.put("id", nodeId);
            treeNode.put("type", tagName);
            treeNode.put("title", title);
            treeNode.put("content", nodeToString(element));

            // 递归解析子节点
            List<Map<String, Object>> childNodes = new ArrayList<>();
            parseChildren(element, childNodes, nodeId);

            if (!childNodes.isEmpty()) {
                treeNode.put("children", childNodes);
            }

            targetList.add(treeNode);
            index++;
        }
    }

    /**
     * 提取节点标题
     */
    private static String extractTitle(Element element, String type) {
        switch (type) {
            case "para":
            case "warning":
            case "caution":
            case "note":
                String text = element.getTextContent();
                if (text != null && text.trim().length() > 0) {
                    text = text.trim();
                    return text.length() > 30 ? text.substring(0, 30) + "..." : text;
                }
                return type;

            case "figure":
            case "table":
                NodeList titleNodes = element.getElementsByTagName("title");
                if (titleNodes.getLength() > 0) {
                    return titleNodes.item(0).getTextContent().trim();
                }
                return type;

            case "multimedia":
                return "Multimedia";

            default:
                return type;
        }
    }

    /**
     * 将 Element 转换为 XML 字符串
     */
    private static String nodeToString(Element element) {
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(element), new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            log.error("节点转字符串失败", e);
            return "";
        }
    }

    /**
     * 统计节点数量
     */
    private static int countNodes(List<Map<String, Object>> nodes) {
        int count = nodes.size();
        for (Map<String, Object> node : nodes) {
            if (node.containsKey("children")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> children = (List<Map<String, Object>>) node.get("children");
                count += countNodes(children);
            }
        }
        return count;
    }

    /**
     * 验证 levelledPara XML
     */
    public static List<String> validateXml(String xmlString) {
        List<String> errors = new ArrayList<>();

        if (xmlString == null || xmlString.trim().isEmpty()) {
            errors.add("XML内容不能为空");
            return errors;
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xmlString.getBytes("UTF-8")));

            Element root = doc.getDocumentElement();
            if (!"levelledPara".equals(root.getNodeName())) {
                errors.add("根节点必须是 levelledPara");
                return errors;
            }

            // 验证结构
            validateElement(root, 1, errors, "");

        } catch (Exception e) {
            errors.add("XML解析失败: " + e.getMessage());
        }

        return errors;
    }

    /**
     * 递归验证元素
     */
    private static void validateElement(Element element, int depth, List<String> errors, String path) {
        // 检查深度限制
        if (depth > MAX_DEPTH) {
            errors.add("节点深度超过限制（最大" + MAX_DEPTH + "层）: " + path);
            return;
        }

        NodeList children = element.getChildNodes();
        int index = 1;

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);

            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element childElement = (Element) child;
            String tagName = childElement.getTagName();

            // 验证节点类型
            if (!CONTENT_TYPES.contains(tagName)) {
                // 跳过非内容类型节点（如 title, warningAndCautionPara 等）
                continue;
            }

            String currentPath = path.isEmpty() ? String.valueOf(index) : path + "." + index;

            // 递归验证子节点
            validateElement(childElement, depth + 1, errors, currentPath);
            index++;
        }
    }

    /**
     * 获取节点模板
     */
    public static String getNodeTemplate(String type) {
        switch (type) {
            case "para":
                return "<para>段落内容</para>";
            case "warning":
                return "<warning><warningAndCautionPara>警告内容</warningAndCautionPara></warning>";
            case "caution":
                return "<caution><warningAndCautionPara>注意内容</warningAndCautionPara></caution>";
            case "note":
                return "<note><notePara>提示内容</notePara></note>";
            case "figure":
                return "<figure><title>图形标题</title><graphic/></figure>";
            case "multimedia":
                return "<multimedia><multimediaObject><imageObject><imageData icnRef=\"\"/></imageObject></multimediaObject></multimedia>";
            case "table":
                return "<table><title>表格标题</title><tgroup cols=\"2\"><tbody><row><entry/><entry/></row></tbody></tgroup></table>";
            default:
                return null;
        }
    }

    /**
     * 格式化 XML
     */
    public static String formatXml(String xmlString, int indent) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xmlString.getBytes("UTF-8")));

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", String.valueOf(indent));
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));

        return writer.toString();
    }
}
