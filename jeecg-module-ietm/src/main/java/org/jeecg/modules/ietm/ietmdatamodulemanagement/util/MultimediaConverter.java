package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

import org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.util.*;

/**
 * Multimedia XML转换工具
 *
 * 功能:
 * - xmlToMultimedia: XML → 对象
 * - multimediaToXml: 对象 → XML
 * - toCnMultimediaXml: 英文标签 → 中文标签
 * - toEnMultimediaXml: 中文标签 → 英文标签
 * - detectMultimediaType: 自动检测multimediaType
 * - escapeXml: XSS防护
 */
public class MultimediaConverter {

    // 视频格式常量
    private static final List<String> VIDEO_EXT_ARR =
        Arrays.asList(".avi", ".wmv", ".mp4", ".mov", ".rm", ".mpg", ".webm");

    // 音频格式常量
    private static final List<String> AUDIO_EXT_ARR =
        Arrays.asList(".mp3");

    // 3D模型格式常量
    private static final List<String> D3_EXT_ARR =
        Arrays.asList(".wrl", ".smg", ".swf");

    /**
     * XML → 对象转换
     *
     * @param multimediaXml Multimedia XML字符串
     * @return MultimediaResponseVO对象
     * @throws Exception 解析失败
     */
    public static MultimediaResponseVO xmlToMultimedia(String multimediaXml) throws Exception {
        if (multimediaXml == null || multimediaXml.trim().isEmpty()) {
            throw new IllegalArgumentException("XML内容不能为空");
        }

        // 关键修复：在解析前检查并注入缺失的xlink命名空间声明
        // 旧数据可能没有xmlns:xlink声明，导致"Attribute xlink:type prefix is unbound"错误
        if (multimediaXml.contains("xlink:") && !multimediaXml.contains("xmlns:xlink")) {
            // 在<multimedia标签后注入命名空间声明
            multimediaXml = multimediaXml.replaceFirst(
                "<multimedia([\\s>])",
                "<multimedia xmlns:xlink=\"http://www.w3.org/1999/xlink\"$1"
            );
        }

        multimediaXml = multimediaXml.replace("xlink:", "xlink_");

        // 安全的DocumentBuilder配置 - 禁用XXE攻击
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        try {
            // 禁用外部DTD
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://apache.org/xml/features/dom/create-entity-ref-nodes", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("XML解析器配置失败: " + e.getMessage(), e);
        }

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(multimediaXml.getBytes("UTF-8")));

        Element multimediaElement = doc.getDocumentElement();
        MultimediaResponseVO result = new MultimediaResponseVO();

        result.setTitle(getElementText(multimediaElement, "title"));
        result.setId(multimediaElement.getAttribute("id"));

        // 提取multimediaObject
        List<MultimediaObjectItemVO> multimediaObjects = new ArrayList<>();
        NodeList objectNodes = multimediaElement.getElementsByTagName("multimediaObject");
        for (int i = 0; i < objectNodes.getLength(); i++) {
            Element objectElement = (Element) objectNodes.item(i);
            MultimediaObjectItemVO obj = new MultimediaObjectItemVO();

            String icn = objectElement.getAttribute("infoEntityIdent");
            obj.setIcn(icn);
            obj.setFilename("");
            obj.setMultimediaType(objectElement.getAttribute("multimediaType"));

            // 提取热点
            List<HotspotItemVO> hotspots = new ArrayList<>();
            NodeList hotspotNodes = objectElement.getElementsByTagName("hotspot");
            for (int j = 0; j < hotspotNodes.getLength(); j++) {
                Element hsElement = (Element) hotspotNodes.item(j);
                HotspotItemVO hs = new HotspotItemVO();
                hs.setApsid(hsElement.getAttribute("applicationStructureIdent"));
                hs.setName(hsElement.getAttribute("applicationStructureName"));
                hs.setId(hsElement.getAttribute("id"));
                hotspots.add(hs);
            }
            obj.setHotspots(hotspots);

            multimediaObjects.add(obj);
        }
        result.setMultimediaObjects(multimediaObjects);

        return result;
    }

    /**
     * 对象 → XML转换
     *
     * @param multimedia MultimediaResponseVO对象
     * @return XML字符串
     */
    public static String multimediaToXml(MultimediaResponseVO multimedia) {
        if (multimedia == null) {
            throw new IllegalArgumentException("multimedia对象不能为空");
        }

        StringBuilder xml = new StringBuilder();
        xml.append("<multimedia");

        // 声明xlink命名空间
        xml.append(" xmlns:xlink=\"http://www.w3.org/1999/xlink\"");

        if (multimedia.getId() != null && !multimedia.getId().trim().isEmpty()) {
            xml.append(" id=\"").append(escapeXml(multimedia.getId())).append("\"");
        }

        xml.append(">\n");
        xml.append("<title>").append(escapeXml(multimedia.getTitle())).append("</title>\n");

        // 生成multimediaObject元素
        if (multimedia.getMultimediaObjects() != null && !multimedia.getMultimediaObjects().isEmpty()) {
            for (MultimediaObjectItemVO obj : multimedia.getMultimediaObjects()) {
                xml.append("<multimediaObject");
                xml.append(" xlink:type=\"simple\"");
                xml.append(" xlink:show=\"replace\"");
                xml.append(" xlink:actuate=\"onRequest\"");
                xml.append(" infoEntityIdent=\"").append(escapeXml(obj.getIcn())).append("\"");

                // 添加multimediaType属性
                String multimediaType = obj.getMultimediaType();
                if (multimediaType == null || multimediaType.isEmpty()) {
                    multimediaType = detectMultimediaType(obj.getFilename());
                }
                if (multimediaType != null && !multimediaType.isEmpty()) {
                    xml.append(" multimediaType=\"").append(multimediaType).append("\"");
                }

                // 判断是否有hotspot子元素
                if (obj.getHotspots() != null && !obj.getHotspots().isEmpty()) {
                    xml.append(">\n");

                    for (HotspotItemVO hs : obj.getHotspots()) {
                        xml.append("<hotspot");
                        xml.append(" applicationStructureIdent=\"").append(escapeXml(hs.getApsid())).append("\"");
                        xml.append(" applicationStructureName=\"").append(escapeXml(hs.getName())).append("\"");
                        if (hs.getId() != null && !hs.getId().isEmpty()) {
                            xml.append(" id=\"").append(escapeXml(hs.getId())).append("\"");
                        }
                        xml.append("/>\n");
                    }

                    xml.append("</multimediaObject>\n");
                } else {
                    xml.append("/>\n");
                }
            }
        }

        xml.append("</multimedia>");
        return xml.toString();
    }

    /**
     * 自动检测multimediaType
     *
     * @param filename 文件名
     * @return multimediaType值（"video" | "3D" | ""）
     */
    public static String detectMultimediaType(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }

        String ext = filename.substring(filename.lastIndexOf('.')).toLowerCase();

        if (VIDEO_EXT_ARR.contains(ext)) {
            return "video";
        }

        if (AUDIO_EXT_ARR.contains(ext)) {
            return "video";  // 音频也标记为video
        }

        if (D3_EXT_ARR.contains(ext)) {
            return "3D";
        }

        return "";
    }

    /**
     * 英文标签 → 中文标签
     */
    public static String toCnMultimediaXml(String enXml) {
        if (enXml == null) return null;

        Map<String, String> cnTagMap = new HashMap<>();
        cnTagMap.put("<multimedia", "<多媒体");
        cnTagMap.put("</multimedia>", "</多媒体>");
        cnTagMap.put("<title", "<标题");
        cnTagMap.put("</title>", "</标题>");
        cnTagMap.put("<multimediaObject", "<多媒体对象");
        cnTagMap.put("</multimediaObject>", "</多媒体对象>");
        cnTagMap.put("<hotspot", "<热点");
        cnTagMap.put("</hotspot>", "</热点>");
        cnTagMap.put("infoEntityIdent", "信息实体标识");
        cnTagMap.put("multimediaType", "多媒体类型");
        cnTagMap.put("applicationStructureIdent", "应用结构标识");
        cnTagMap.put("applicationStructureName", "应用结构名称");

        String result = enXml;
        for (Map.Entry<String, String> entry : cnTagMap.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    /**
     * 中文标签 → 英文标签
     */
    public static String toEnMultimediaXml(String cnXml) {
        if (cnXml == null) return null;

        Map<String, String> enTagMap = new HashMap<>();
        enTagMap.put("<多媒体", "<multimedia");
        enTagMap.put("</多媒体>", "</multimedia>");
        enTagMap.put("<标题", "<title");
        enTagMap.put("</标题>", "</title>");
        enTagMap.put("<多媒体对象", "<multimediaObject");
        enTagMap.put("</多媒体对象>", "</multimediaObject>");
        enTagMap.put("<热点", "<hotspot");
        enTagMap.put("</热点>", "</hotspot>");
        enTagMap.put("信息实体标识", "infoEntityIdent");
        enTagMap.put("多媒体类型", "multimediaType");
        enTagMap.put("应用结构标识", "applicationStructureIdent");
        enTagMap.put("应用结构名称", "applicationStructureName");

        String result = cnXml;
        for (Map.Entry<String, String> entry : enTagMap.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    /**
     * XSS防护 - XML特殊字符转义
     */
    private static String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }

    /**
     * 获取元素文本内容
     */
    private static String getElementText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent();
        }
        return "";
    }
}
