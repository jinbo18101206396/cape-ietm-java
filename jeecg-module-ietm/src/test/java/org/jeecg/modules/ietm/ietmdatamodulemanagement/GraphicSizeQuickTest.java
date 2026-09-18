package org.jeecg.modules.ietm.ietmdatamodulemanagement;

import org.jeecg.modules.ietm.ietmdatamodulemanagement.util.DmXmlHelper;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.util.DmXsltTransformer;

/**
 * 快速测试 - 检查XSLT转换输出
 */
public class GraphicSizeQuickTest {

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

        System.out.println("========== 测试XML ==========");
        System.out.println(xml.substring(0, Math.min(500, xml.length())));
        System.out.println("...\n");

        String html = DmXmlHelper.renderHtml(xml, "/jeecg-boot");

        System.out.println("========== 生成的HTML（前2000字符）==========");
        System.out.println(html.substring(0, Math.min(2000, html.length())));
        System.out.println("...\n");

        System.out.println("========== 查找<img标签 ==========");
        int imgStart = html.indexOf("<img");
        if (imgStart == -1) {
            System.out.println("❌ 未找到<img>标签！");
        } else {
            int imgEnd = html.indexOf(">", imgStart);
            String imgTag = html.substring(imgStart, imgEnd + 1);
            System.out.println("✅ 找到<img>标签：");
            System.out.println(imgTag);

            // 检查width和height属性
            if (imgTag.contains("width=\"300px\"")) {
                System.out.println("✅ 包含 width=\"300px\"");
            } else {
                System.out.println("❌ 不包含 width=\"300px\"");
            }

            if (imgTag.contains("height=\"200px\"")) {
                System.out.println("✅ 包含 height=\"200px\"");
            } else {
                System.out.println("❌ 不包含 height=\"200px\"");
            }
        }
    }
}
