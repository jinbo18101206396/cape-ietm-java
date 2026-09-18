package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * 诊断测试：模拟完整的预览流程，检查HTML内容是否被误删
 */
public class DmXmlHelperContentDiagnosticTest {

    @Test
    public void testFullPreviewPipeline_WithSymbols() throws Exception {
        // 模拟包含symbol的真实DM XML
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
            "        <para>这是第一段内容。</para>\n" +
            "        <para>这是第二段内容。</para>\n" +
            "      </levelledPara>\n" +
            "    </description>\n" +
            "  </content>\n" +
            "</dmodule>";

        System.out.println("=== 完整预览流程诊断 ===\n");

        // 调用完整渲染流程
        String html = DmXmlHelper.renderHtml(dmXml, "/jeecg-boot");

        System.out.println("1. HTML总长度: " + html.length() + " 字符");
        System.out.println("2. 是否包含外层容器: " + html.contains("dm-preview"));
        System.out.println("3. 是否包含CSS: " + html.contains("<style>"));
        System.out.println("4. 是否包含'测试标题': " + html.contains("测试标题"));
        System.out.println("5. 是否包含'第一段内容': " + html.contains("第一段内容"));
        System.out.println("6. 是否包含'第二段内容': " + html.contains("第二段内容"));
        System.out.println("7. 是否包含lessonPath: " + html.contains("lessonPath"));
        System.out.println("8. 是否包含lessonCode: " + html.contains("lessonCode"));
        System.out.println("9. 是否包含<script>标签: " + html.contains("<script"));

        // 打印HTML前500字符和后500字符
        System.out.println("\n=== HTML开头 (前500字符) ===");
        System.out.println(html.substring(0, Math.min(500, html.length())));

        if (html.length() > 500) {
            System.out.println("\n=== HTML结尾 (后500字符) ===");
            System.out.println(html.substring(Math.max(0, html.length() - 500)));
        }

        // 统计关键标签数量
        int divCount = countOccurrences(html, "<div");
        int pCount = countOccurrences(html, "<p>");
        int titleCount = countOccurrences(html, "<title>");

        System.out.println("\n=== 标签统计 ===");
        System.out.println("div标签数: " + divCount);
        System.out.println("p标签数: " + pCount);
        System.out.println("title标签数: " + titleCount);

        // 断言：关键内容必须存在
        assertTrue("HTML不应为空", html.length() > 0);
        assertTrue("必须包含dm-preview容器", html.contains("dm-preview"));
        assertTrue("必须包含测试标题", html.contains("测试标题"));
        assertTrue("必须包含第一段内容", html.contains("第一段内容"));
        assertTrue("必须包含第二段内容", html.contains("第二段内容"));
        assertFalse("不应包含lessonPath", html.contains("lessonPath"));
        assertFalse("不应包含lessonCode", html.contains("lessonCode"));
    }

    @Test
    public void testEmptyDmContent() throws Exception {
        // 测试空内容DM
        String dmXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<dmodule xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
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
            "  </content>\n" +
            "</dmodule>";

        System.out.println("\n=== 空内容DM测试 ===\n");

        String html = DmXmlHelper.renderHtml(dmXml, "/jeecg-boot");

        System.out.println("1. HTML总长度: " + html.length() + " 字符");
        System.out.println("2. 是否包含dm-preview: " + html.contains("dm-preview"));

        assertTrue("即使内容为空，也应生成HTML框架", html.length() > 0);
        assertTrue("应该包含dm-preview容器", html.contains("dm-preview"));
    }

    @Test
    public void testMultipleParagraphs() throws Exception {
        // 测试多段落内容
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
            "      <levelledPara><title>第一章</title><para>内容A</para></levelledPara>\n" +
            "      <levelledPara><title>第二章</title><para>内容B</para></levelledPara>\n" +
            "      <levelledPara><title>第三章</title><para>内容C</para></levelledPara>\n" +
            "    </description>\n" +
            "  </content>\n" +
            "</dmodule>";

        System.out.println("\n=== 多段落内容测试 ===\n");

        String html = DmXmlHelper.renderHtml(dmXml, "/jeecg-boot");

        System.out.println("是否包含'第一章': " + html.contains("第一章"));
        System.out.println("是否包含'第二章': " + html.contains("第二章"));
        System.out.println("是否包含'第三章': " + html.contains("第三章"));
        System.out.println("是否包含'内容A': " + html.contains("内容A"));
        System.out.println("是否包含'内容B': " + html.contains("内容B"));
        System.out.println("是否包含'内容C': " + html.contains("内容C"));

        assertTrue("所有章节标题都应该存在",
            html.contains("第一章") && html.contains("第二章") && html.contains("第三章"));
        assertTrue("所有内容都应该存在",
            html.contains("内容A") && html.contains("内容B") && html.contains("内容C"));
    }

    private int countOccurrences(String text, String pattern) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(pattern, index)) != -1) {
            count++;
            index += pattern.length();
        }
        return count;
    }
}
