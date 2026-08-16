package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 目录序号显示问题排查测试
 *
 * @author claude
 * @date 2026-08-07
 */
@DisplayName("目录序号显示排查")
public class DmPreviewTocNumberTest {

    @Test
    @DisplayName("验证目录序号在HTML中生成")
    public void testTocNumbersGenerated() throws Exception {
        String xml = buildTestDmWithToc();

        // XSLT转换
        String xsltHtml = DmXsltTransformer.transform(xml, "S1000D40", "descript");

        // 完整流程（含CSS注入）
        String finalHtml = DmXsltTransformer.enhancePreviewHtml(xsltHtml);

        System.out.println("========================================");
        System.out.println("目录序号测试 - HTML片段分析");
        System.out.println("========================================");

        // 检查是否包含目录表格
        assertTrue(finalHtml.contains("class=\"toc-table\""), "应包含目录表格");
        System.out.println("✅ 找到目录表格");

        // 检查是否包含序号单元格
        assertTrue(finalHtml.contains("class=\"loclefttd\""), "应包含序号单元格");
        System.out.println("✅ 找到序号单元格");

        // 检查是否包含标题单元格
        assertTrue(finalHtml.contains("class=\"locrighttd\""), "应包含标题单元格");
        System.out.println("✅ 找到标题单元格");

        // 检查CSS样式是否注入
        assertTrue(finalHtml.contains(".loclefttd"), "CSS应包含序号样式");
        System.out.println("✅ CSS包含序号样式");

        // 提取目录HTML片段
        int tocStart = finalHtml.indexOf("<table width=\"100%\" class=\"toc-table\">");
        int tocEnd = finalHtml.indexOf("</table>", tocStart) + 8;

        if (tocStart != -1 && tocEnd > tocStart) {
            String tocHtml = finalHtml.substring(tocStart, tocEnd);
            System.out.println("\n========== 目录HTML片段 ==========");
            System.out.println(tocHtml.substring(0, Math.min(1000, tocHtml.length())));
            System.out.println("=================================\n");

            // 检查序号单元格内容
            if (tocHtml.contains("<td class=\"loclefttd\">")) {
                int leftTdStart = tocHtml.indexOf("<td class=\"loclefttd\">");
                int leftTdEnd = tocHtml.indexOf("</td>", leftTdStart);
                String leftTdContent = tocHtml.substring(leftTdStart, leftTdEnd + 5);

                System.out.println("========== 序号单元格内容 ==========");
                System.out.println(leftTdContent);
                System.out.println("===================================\n");

                // 检查是否包含数字
                boolean hasNumber = leftTdContent.matches("(?s).*\\d+.*");
                if (hasNumber) {
                    System.out.println("✅ 序号单元格包含数字");
                } else {
                    System.out.println("❌ 序号单元格不包含数字");
                }

                // 检查span内容
                if (leftTdContent.contains("<span")) {
                    int spanStart = leftTdContent.indexOf("<span");
                    int spanContentStart = leftTdContent.indexOf(">", spanStart) + 1;
                    int spanEnd = leftTdContent.indexOf("</span>", spanStart);
                    if (spanEnd > spanContentStart) {
                        String spanContent = leftTdContent.substring(spanContentStart, spanEnd).trim();
                        System.out.println("Span内容: \"" + spanContent + "\"");

                        if (!spanContent.isEmpty() && spanContent.matches("\\d+")) {
                            System.out.println("✅ Span包含纯数字序号: " + spanContent);
                        } else if (spanContent.isEmpty()) {
                            System.out.println("⚠️  Span内容为空！");
                        } else {
                            System.out.println("⚠️  Span内容不是纯数字: " + spanContent);
                        }
                    }
                }
            }
        }

        System.out.println("========================================");
    }

    private String buildTestDmWithToc() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<dmodule xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
            "xsi:noNamespaceSchemaLocation=\"http://www.s1000d.org/S1000D_4-0/xml_schema_flat/descript.xsd\">\n" +
            "  <identAndStatusSection>\n" +
            "    <dmAddress>\n" +
            "      <dmIdent>\n" +
            "        <dmCode modelIdentCode=\"TEST\" systemDiffCode=\"A\" systemCode=\"00\" " +
            "subSystemCode=\"0\" subSubSystemCode=\"0\" assyCode=\"00\" disassyCode=\"00\" " +
            "disassyCodeVariant=\"A\" infoCode=\"040\" infoCodeVariant=\"A\" itemLocationCode=\"A\"/>\n" +
            "      </dmIdent>\n" +
            "      <dmAddressItems>\n" +
            "        <issueDate year=\"2026\" month=\"08\" day=\"07\"/>\n" +
            "        <dmTitle><techName>测试技术名称</techName><infoName>测试信息名称</infoName></dmTitle>\n" +
            "      </dmAddressItems>\n" +
            "    </dmAddress>\n" +
            "  </identAndStatusSection>\n" +
            "  <content>\n" +
            "    <description>\n" +
            "      <levelledPara>\n" +
            "        <title>第一章 概述</title>\n" +
            "        <para>这是第一章的内容。</para>\n" +
            "        <levelledPara>\n" +
            "          <title>第一节 简介</title>\n" +
            "          <para>这是第一节的内容。</para>\n" +
            "        </levelledPara>\n" +
            "        <levelledPara>\n" +
            "          <title>第二节 特性</title>\n" +
            "          <para>这是第二节的内容。</para>\n" +
            "        </levelledPara>\n" +
            "      </levelledPara>\n" +
            "      <levelledPara>\n" +
            "        <title>第二章 操作</title>\n" +
            "        <para>这是第二章的内容。</para>\n" +
            "      </levelledPara>\n" +
            "    </description>\n" +
            "  </content>\n" +
            "</dmodule>";
    }
}
