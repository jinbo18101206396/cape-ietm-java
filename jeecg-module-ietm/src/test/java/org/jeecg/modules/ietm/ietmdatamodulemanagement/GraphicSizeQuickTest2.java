package org.jeecg.modules.ietm.ietmdatamodulemanagement;

import org.jeecg.modules.ietm.ietmdatamodulemanagement.util.DmXmlHelper;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.util.DmXsltTransformer;

import java.io.PrintWriter;

/**
 * 快速测试 - 完整输出HTML到文件
 */
public class GraphicSizeQuickTest2 {

    public static void main(String[] args) throws Exception {
        // 清除缓存
        DmXsltTransformer.clearCache();

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<dmodule xmlns=\"http://www.s1000d.org/S1000D_4-0\">\n" +
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

        String html = DmXmlHelper.renderHtml(xml, "/jeecg-boot");

        // 输出到文件
        PrintWriter writer = new PrintWriter("D:/test-output.html", "UTF-8");
        writer.println(html);
        writer.close();

        System.out.println("========== HTML已输出到 D:/test-output.html ==========");
        System.out.println("HTML长度: " + html.length() + " 字符");

        // 搜索关键词
        System.out.println("\n========== 关键词搜索 ==========");
        System.out.println("包含 <figure ?: " + html.contains("<figure"));
        System.out.println("包含 <graphic ?: " + html.contains("<graphic"));
        System.out.println("包含 <img ?: " + html.contains("<img"));
        System.out.println("包含 图 字: " + html.contains("图"));
        System.out.println("包含 figureLinkGraphic: " + html.contains("figureLinkGraphic"));
    }
}
