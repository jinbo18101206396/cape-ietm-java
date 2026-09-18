package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

import org.junit.Test;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDataModule;

/**
 * 测试真实DM预览的完整输出
 */
public class DmXmlHelperRealPreviewTest {

    @Test
    public void testRealDmPreview() throws Exception {
        // 构建一个简单的DM XML
        String dmXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<dmodule xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
            "xsi:noNamespaceSchemaLocation=\"http://www.s1000d.org/S1000D_4-0/xml_schema_flat/descript.xsd\">\n" +
            "  <identAndStatusSection>\n" +
            "    <dmAddress>\n" +
            "      <dmIdent>\n" +
            "        <dmCode modelIdentCode=\"TEST\" systemDiffCode=\"A\" systemCode=\"00\" " +
            "subSystemCode=\"0\" subSubSystemCode=\"0\" assyCode=\"00\" " +
            "disassyCode=\"00\" disassyCodeVariant=\"A\" infoCode=\"001\" " +
            "infoCodeVariant=\"A\" itemLocationCode=\"A\"/>\n" +
            "      </dmIdent>\n" +
            "    </dmAddress>\n" +
            "  </identAndStatusSection>\n" +
            "  <content>\n" +
            "    <description>\n" +
            "      <levelledPara>\n" +
            "        <title>测试标题</title>\n" +
            "        <para>这是测试段落内容。</para>\n" +
            "      </levelledPara>\n" +
            "    </description>\n" +
            "  </content>\n" +
            "</dmodule>";

        System.out.println("=== 输入 DM XML ===");
        System.out.println(dmXml);

        // 调用 renderHtml
        String html = DmXmlHelper.renderHtml(dmXml, "/jeecg-boot");

        System.out.println("\n=== 输出 HTML ===");
        System.out.println(html);

        System.out.println("\n=== HTML 长度 ===");
        System.out.println(html.length() + " 字符");

        System.out.println("\n=== 是否包含关键内容 ===");
        System.out.println("包含 '测试标题': " + html.contains("测试标题"));
        System.out.println("包含 '测试段落': " + html.contains("测试段落"));
        System.out.println("包含 'dm-preview': " + html.contains("dm-preview"));
    }
}
