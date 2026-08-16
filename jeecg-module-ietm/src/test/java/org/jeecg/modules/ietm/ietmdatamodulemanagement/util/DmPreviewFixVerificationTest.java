package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 修复后验证测试 - 验证状况模块和目录序号的CSS修复
 *
 * @author claude
 * @date 2026-08-07
 */
@DisplayName("修复后验证测试")
public class DmPreviewFixVerificationTest {

    @Test
    @DisplayName("验证状况模块显示修复")
    public void testIdentAndStatusSectionVisible() throws Exception {
        String xml = buildTestDm();

        // 完整流程
        String xsltHtml = DmXsltTransformer.transform(xml, "S1000D40", "descript");
        String finalHtml = DmXsltTransformer.enhancePreviewHtml(xsltHtml);

        System.out.println("\n========================================");
        System.out.println("状况模块显示修复验证");
        System.out.println("========================================\n");

        // 检查是否包含修复的CSS
        boolean hasFixCss = finalHtml.contains(".hidesection#idstatus");
        System.out.println("包含修复CSS: " + (hasFixCss ? "✅ 是" : "❌ 否"));

        boolean hasDisplayBlock = finalHtml.contains("display: block !important");
        System.out.println("包含display: block !important: " + (hasDisplayBlock ? "✅ 是" : "❌ 否"));

        // 检查状况模块HTML
        boolean hasIdStatus = finalHtml.contains("id=\"idstatus\"");
        System.out.println("包含idstatus div: " + (hasIdStatus ? "✅ 是" : "❌ 否"));

        // 检查关键字段
        boolean hasDmc = finalHtml.contains("数据模块代码");
        boolean hasTitle = finalHtml.contains("测试技术名称");
        boolean hasSecurity = finalHtml.contains("安全");

        System.out.println("\n字段检查:");
        System.out.println("  数据模块代码: " + (hasDmc ? "✅" : "❌"));
        System.out.println("  标题: " + (hasTitle ? "✅" : "❌"));
        System.out.println("  安全密级: " + (hasSecurity ? "✅" : "❌"));

        // 断言
        assertTrue(hasFixCss, "应该包含修复CSS");
        assertTrue(hasDisplayBlock, "应该包含display: block !important");
        assertTrue(hasIdStatus, "应该包含idstatus div");
        assertTrue(hasDmc && hasTitle && hasSecurity, "应该包含所有关键字段");

        System.out.println("\n结论: ✅ 状况模块显示修复成功！");
        System.out.println("========================================\n");
    }

    @Test
    @DisplayName("验证目录序号显示增强")
    public void testTocNumbersEnhanced() throws Exception {
        String xml = buildTestDm();

        String xsltHtml = DmXsltTransformer.transform(xml, "S1000D40", "descript");
        String finalHtml = DmXsltTransformer.enhancePreviewHtml(xsltHtml);

        System.out.println("\n========================================");
        System.out.println("目录序号显示增强验证");
        System.out.println("========================================\n");

        // 检查增强CSS
        boolean hasLocLeftTdCss = finalHtml.contains(".loclefttd");
        System.out.println("包含.loclefttd CSS: " + (hasLocLeftTdCss ? "✅ 是" : "❌ 否"));

        boolean hasColorImportant = finalHtml.contains("color: #1890ff !important");
        System.out.println("包含color: #1890ff !important: " + (hasColorImportant ? "✅ 是" : "❌ 否"));

        boolean hasFontBold = finalHtml.contains("font-weight: bold !important");
        System.out.println("包含font-weight: bold !important: " + (hasFontBold ? "✅ 是" : "❌ 否"));

        // 检查目录HTML
        boolean hasTocTable = finalHtml.contains("class=\"toc-table\"");
        boolean hasLocLeftTd = finalHtml.contains("class=\"loclefttd\"");

        System.out.println("\n目录结构:");
        System.out.println("  toc-table: " + (hasTocTable ? "✅" : "❌"));
        System.out.println("  loclefttd: " + (hasLocLeftTd ? "✅" : "❌"));

        // 断言
        assertTrue(hasLocLeftTdCss, "应该包含.loclefttd CSS");
        assertTrue(hasColorImportant, "应该包含color: #1890ff !important");
        assertTrue(hasFontBold, "应该包含font-weight: bold !important");
        assertTrue(hasTocTable && hasLocLeftTd, "应该包含目录结构");

        System.out.println("\n结论: ✅ 目录序号显示增强成功！");
        System.out.println("========================================\n");
    }

    @Test
    @DisplayName("验证CSS总长度增加")
    public void testCssLengthIncreased() throws Exception {
        String xml = buildTestDm();

        String xsltHtml = DmXsltTransformer.transform(xml, "S1000D40", "descript");
        String finalHtml = DmXsltTransformer.enhancePreviewHtml(xsltHtml);

        System.out.println("\n========================================");
        System.out.println("CSS长度验证");
        System.out.println("========================================\n");

        // 提取CSS内容
        int styleStart = finalHtml.indexOf("<style>");
        int styleEnd = finalHtml.indexOf("</style>");

        if (styleStart != -1 && styleEnd > styleStart) {
            String cssContent = finalHtml.substring(styleStart + 7, styleEnd);
            int cssLength = cssContent.length();

            System.out.println("CSS总长度: " + cssLength + " 字符");

            // 修复后的CSS应该比原来的14265字符更长
            // 新增约60行CSS，大约1500字符
            assertTrue(cssLength > 15000,
                String.format("CSS长度应该>15000字符（新增了修复CSS），实际: %d", cssLength));

            System.out.println("✅ CSS长度增加，修复CSS已注入");
        } else {
            fail("未找到style标签");
        }

        System.out.println("========================================\n");
    }

    private String buildTestDm() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<dmodule xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
            "xsi:noNamespaceSchemaLocation=\"http://www.s1000d.org/S1000D_4-0/xml_schema_flat/descript.xsd\">\n" +
            "  <identAndStatusSection>\n" +
            "    <dmAddress>\n" +
            "      <dmIdent>\n" +
            "        <dmCode modelIdentCode=\"TEST\" systemDiffCode=\"A\" systemCode=\"00\" " +
            "subSystemCode=\"0\" subSubSystemCode=\"0\" assyCode=\"00\" disassyCode=\"00\" " +
            "disassyCodeVariant=\"A\" infoCode=\"040\" infoCodeVariant=\"A\" itemLocationCode=\"A\"/>\n" +
            "        <language languageIsoCode=\"zh\" countryIsoCode=\"CN\"/>\n" +
            "        <issueInfo issueNumber=\"001\" inWork=\"00\"/>\n" +
            "      </dmIdent>\n" +
            "      <dmAddressItems>\n" +
            "        <issueDate year=\"2026\" month=\"08\" day=\"07\"/>\n" +
            "        <dmTitle>\n" +
            "          <techName>测试技术名称</techName>\n" +
            "          <infoName>测试信息名称</infoName>\n" +
            "        </dmTitle>\n" +
            "      </dmAddressItems>\n" +
            "    </dmAddress>\n" +
            "    <dmStatus>\n" +
            "      <security securityClassification=\"01\"/>\n" +
            "      <responsiblePartnerCompany enterpriseCode=\"TEST001\">\n" +
            "        <enterpriseName>测试公司</enterpriseName>\n" +
            "      </responsiblePartnerCompany>\n" +
            "    </dmStatus>\n" +
            "  </identAndStatusSection>\n" +
            "  <content>\n" +
            "    <description>\n" +
            "      <levelledPara>\n" +
            "        <title>第一章</title>\n" +
            "        <para>内容。</para>\n" +
            "        <levelledPara>\n" +
            "          <title>第一节</title>\n" +
            "          <para>详细内容。</para>\n" +
            "        </levelledPara>\n" +
            "      </levelledPara>\n" +
            "    </description>\n" +
            "  </content>\n" +
            "</dmodule>";
    }
}
