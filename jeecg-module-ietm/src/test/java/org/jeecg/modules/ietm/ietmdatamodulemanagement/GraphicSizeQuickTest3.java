package org.jeecg.modules.ietm.ietmdatamodulemanagement;

import org.jeecg.modules.ietm.ietmdatamodulemanagement.util.DmXmlHelper;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.util.DmXsltTransformer;

import java.io.PrintWriter;

/**
 * 快速测试 - 无命名空间的XML
 */
public class GraphicSizeQuickTest3 {

    public static void main(String[] args) throws Exception {
        // 清除缓存
        DmXsltTransformer.clearCache();

        // 🔧 关键：不使用xmlns命名空间
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<dmodule>\n" +
            "  <identAndStatusSection>\n" +
            "    <dmAddress>\n" +
            "      <dmIdent>\n" +
            "        <dmCode modelIdentCode=\"TEST\" systemDiffCode=\"A\" systemCode=\"00\" " +
            "subSystemCode=\"0\" subSubSystemCode=\"0\" assyCode=\"00\" " +
            "disassyCode=\"00\" disassyCodeVariant=\"A\" infoCode=\"040\" " +
            "infoCodeVariant=\"A\" itemLocationCode=\"A\"/>\n" +
            "        <language languageIsoCode=\"zh\" countryIsoCode=\"CN\"/>\n" +
            "        <issueInfo issueNumber=\"001\" inWork=\"00\"/>\n" +
            "      </dmIdent>\n" +
            "    </dmAddress>\n" +
            "    <dmAddressItems>\n" +
            "      <issueDate year=\"2026\" month=\"09\" day=\"18\"/>\n" +
            "      <dmTitle><techName>测试</techName><infoName>图形尺寸</infoName></dmTitle>\n" +
            "    </dmAddressItems>\n" +
            "  </identAndStatusSection>\n" +
            "  <content>\n" +
            "    <description>\n" +
            "      <levelledPara>\n" +
            "        <title>测试图形尺寸</title>\n" +
            "        <para>图形尺寸测试</para>\n" +
            "        <figure id=\"fig-001\">\n" +
            "          <title>测试图</title>\n" +
            "          <graphic infoEntityIdent=\"ICN-TEST-001\" reproductionWidth=\"300px\" reproductionHeight=\"200px\"/>\n" +
            "        </figure>\n" +
            "      </levelledPara>\n" +
            "    </description>\n" +
            "  </content>\n" +
            "</dmodule>";

        System.out.println("========== 测试XML（无命名空间）==========");
        System.out.println(xml.substring(0, Math.min(500, xml.length())));

        String html = DmXmlHelper.renderHtml(xml, "/jeecg-boot");

        // 输出到文件
        PrintWriter writer = new PrintWriter("D:/test-output-no-ns.html", "UTF-8");
        writer.println(html);
        writer.close();

        System.out.println("\n========== HTML已输出到 D:/test-output-no-ns.html ==========");
        System.out.println("HTML长度: " + html.length() + " 字符");

        // 搜索关键词
        System.out.println("\n========== 关键词搜索 ==========");
        System.out.println("包含 <img ?: " + html.contains("<img"));
        System.out.println("包含 addFigure: " + html.contains("addFigure"));
        System.out.println("包含 width=\"300px\": " + html.contains("width=\"300px\""));

        if (html.contains("<img")) {
            int imgStart = html.indexOf("<img");
            int imgEnd = html.indexOf(">", imgStart);
            String imgTag = html.substring(imgStart, Math.min(imgEnd + 1, imgStart + 500));
            System.out.println("\n✅ 找到<img>标签：");
            System.out.println(imgTag);
        } else {
            System.out.println("\n❌ 未找到<img>标签");
        }
    }
}
