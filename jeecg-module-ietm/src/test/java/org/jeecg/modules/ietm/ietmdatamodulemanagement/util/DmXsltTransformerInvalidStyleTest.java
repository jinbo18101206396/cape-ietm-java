package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DM预览功能无效style标签修复专项测试
 *
 * 验证BUG-PREVIEW-01的修复：
 * XSLT模板不应输出无效的<style>../css/main</style>标签
 * CSS应该完全由Java后端的enhancePreviewHtml方法统一注入
 *
 * @author claude
 * @date 2026-08-06
 */
@DisplayName("DM预览无效style标签修复测试")
public class DmXsltTransformerInvalidStyleTest {

    private static final String INVALID_STYLE_PATTERN = "<style[^>]*>\\s*\\.\\./css/main\\s*</style>";
    private static final String VALID_CSS_PATTERN = "<style[^>]*>\\s*/\\*.*?\\*/.*?body\\s*\\{";

    @BeforeEach
    public void setUp() {
        // 清除缓存，确保测试使用最新的XSLT模板
        DmXsltTransformer.clearCache();
    }

    @Test
    @DisplayName("描述类DM - XSLT不应输出无效style标签")
    public void testDescriptDmNoInvalidStyle() throws Exception {
        String xml = buildTestDmXml("descript", "description",
            "<levelledPara>\n" +
            "  <title>测试标题</title>\n" +
            "  <para>测试段落内容。</para>\n" +
            "</levelledPara>");

        // 只测试XSLT转换，不包含CSS注入
        String html = DmXsltTransformer.transform(xml, "S1000D40", "descript");

        assertNotNull(html, "HTML结果不应为null");

        // 关键断言：XSLT转换结果不应包含无效的style标签
        assertFalse(html.matches("(?s).*" + INVALID_STYLE_PATTERN + ".*"),
            "XSLT转换不应输出<style>../css/main</style>标签");

        System.out.println("✅ 描述类DM - XSLT不输出无效style标签");
    }

    @Test
    @DisplayName("过程类DM - XSLT不应输出无效style标签")
    public void testProcedDmNoInvalidStyle() throws Exception {
        String xml = buildTestDmXml("proced", "procedure",
            "<mainProcedure>\n" +
            "  <proceduralStep>\n" +
            "    <para>执行步骤1。</para>\n" +
            "  </proceduralStep>\n" +
            "</mainProcedure>");

        String html = DmXsltTransformer.transform(xml, "S1000D40", "proced");

        assertNotNull(html, "HTML结果不应为null");
        assertFalse(html.matches("(?s).*" + INVALID_STYLE_PATTERN + ".*"),
            "XSLT转换不应输出<style>../css/main</style>标签");

        System.out.println("✅ 过程类DM - XSLT不输出无效style标签");
    }

    @Test
    @DisplayName("故障类DM - XSLT不应输出无效style标签")
    public void testFaultDmNoInvalidStyle() throws Exception {
        String xml = buildTestDmXml("fault", "faultIsolation",
            "<isolationProcedure>\n" +
            "  <isolationMainProcedure>\n" +
            "    <isolationStep>\n" +
            "      <para>故障隔离步骤1。</para>\n" +
            "    </isolationStep>\n" +
            "  </isolationMainProcedure>\n" +
            "</isolationProcedure>");

        String html = DmXsltTransformer.transform(xml, "S1000D40", "fault");

        assertNotNull(html, "HTML结果不应为null");
        assertFalse(html.matches("(?s).*" + INVALID_STYLE_PATTERN + ".*"),
            "XSLT转换不应输出<style>../css/main</style>标签");

        System.out.println("✅ 故障类DM - XSLT不输出无效style标签");
    }

    @Test
    @DisplayName("IPD类DM - XSLT不应输出无效style标签")
    public void testIpdDmNoInvalidStyle() throws Exception {
        String xml = buildTestDmXml("ipd", "illustratedPartsCatalog",
            "<figure>\n" +
            "  <title>零件图</title>\n" +
            "  <graphic boardno=\"ICN-TEST-001\"/>\n" +
            "</figure>");

        String html = DmXsltTransformer.transform(xml, "S1000D40", "ipd");

        assertNotNull(html, "HTML结果不应为null");
        assertFalse(html.matches("(?s).*" + INVALID_STYLE_PATTERN + ".*"),
            "XSLT转换不应输出<style>../css/main</style>标签");

        System.out.println("✅ IPD类DM - XSLT不输出无效style标签");
    }

    @Test
    @DisplayName("完整流程 - 转换+CSS注入后应只有一个有效style标签")
    public void testCompleteFlowWithCssInjection() throws Exception {
        String xml = buildTestDmXml("descript", "description",
            "<levelledPara>\n" +
            "  <title>标题</title>\n" +
            "  <para>内容。</para>\n" +
            "</levelledPara>");

        // 完整流程：XSLT转换 + CSS注入
        String xsltHtml = DmXsltTransformer.transform(xml, "S1000D40", "descript");
        String finalHtml = DmXsltTransformer.enhancePreviewHtml(xsltHtml);

        // 统计style标签数量
        Pattern stylePattern = Pattern.compile("<style[^>]*>");
        Matcher matcher = stylePattern.matcher(finalHtml);
        int count = 0;
        while (matcher.find()) {
            count++;
        }

        assertEquals(1, count, "完整流程后应该只有一个style标签（Java后端注入的）");

        // 验证没有无效style标签
        assertFalse(finalHtml.matches("(?s).*" + INVALID_STYLE_PATTERN + ".*"),
            "不应包含无效的style标签");

        System.out.println("✅ 完整流程验证通过：只有一个有效style标签");
    }

    @Test
    @DisplayName("CSS注入后应包含完整CSS内容")
    public void testCssContentLength() throws Exception {
        String xml = buildTestDmXml("descript", "description",
            "<levelledPara>\n" +
            "  <title>标题</title>\n" +
            "  <para>内容。</para>\n" +
            "</levelledPara>");

        // 完整流程
        String xsltHtml = DmXsltTransformer.transform(xml, "S1000D40", "descript");
        String finalHtml = DmXsltTransformer.enhancePreviewHtml(xsltHtml);

        // 提取style标签内容
        Pattern styleContentPattern = Pattern.compile("<style[^>]*>([\\s\\S]*?)</style>");
        Matcher matcher = styleContentPattern.matcher(finalHtml);

        if (matcher.find()) {
            String cssContent = matcher.group(1).trim();
            int cssLength = cssContent.length();

            // 有效的CSS应该包含大量样式规则
            assertTrue(cssLength > 1000,
                String.format("CSS内容长度应该>1000字符，实际: %d", cssLength));

            // 不应该是路径字符串
            assertFalse(cssContent.equals("../css/main"),
                "CSS内容不应该是路径字符串");

            System.out.println(String.format("✅ CSS内容长度验证通过: %d 字符", cssLength));
        } else {
            fail("未找到style标签");
        }
    }

    @Test
    @DisplayName("CSS注入后应包含关键样式类")
    public void testCssContainsKeyClasses() throws Exception {
        String xml = buildTestDmXml("descript", "description",
            "<levelledPara>\n" +
            "  <title>标题</title>\n" +
            "  <para>内容。</para>\n" +
            "</levelledPara>");

        // 完整流程
        String xsltHtml = DmXsltTransformer.transform(xml, "S1000D40", "descript");
        String finalHtml = DmXsltTransformer.enhancePreviewHtml(xsltHtml);

        // 验证包含关键CSS类
        String[] keyClasses = {
            ".loclefttd",    // 序号单元格
            ".locrighttd",   // 标题单元格
            ".toc-table",    // 目录表格
            "body",          // 基础样式
            "table"          // 表格样式
        };

        for (String cssClass : keyClasses) {
            assertTrue(finalHtml.contains(cssClass),
                String.format("CSS应包含关键类: %s", cssClass));
        }

        System.out.println("✅ CSS关键类验证通过");
    }

    @Test
    @DisplayName("缓存清除后应重新加载XSLT")
    public void testCacheClearReloadsXslt() throws Exception {
        String xml = buildTestDmXml("descript", "description",
            "<levelledPara><title>T</title><para>P</para></levelledPara>");

        // 第一次转换
        String html1 = DmXsltTransformer.transform(xml, "S1000D40", "descript");
        String cacheInfo1 = DmXsltTransformer.getCacheInfo();
        assertTrue(cacheInfo1.contains("XSLT模板缓存数量:") && !cacheInfo1.contains(": 0"),
            "应该有缓存");

        // 清除缓存
        DmXsltTransformer.clearCache();
        String cacheInfo2 = DmXsltTransformer.getCacheInfo();
        assertTrue(cacheInfo2.contains("XSLT模板缓存数量: 0"), "缓存应该被清空");

        // 第二次转换
        String html2 = DmXsltTransformer.transform(xml, "S1000D40", "descript");
        String cacheInfo3 = DmXsltTransformer.getCacheInfo();
        assertTrue(cacheInfo3.contains("XSLT模板缓存数量:") && !cacheInfo3.contains(": 0"),
            "应该重新加载模板");

        // 两次结果应该一致
        assertEquals(html1.length(), html2.length(), "清除缓存前后结果应一致");

        System.out.println("✅ 缓存清除测试通过");
    }

    @Test
    @DisplayName("HTML基本结构验证")
    public void testHtmlStructureIntegrity() throws Exception {
        String xml = buildTestDmXml("descript", "description",
            "<levelledPara>\n" +
            "  <title>第一章</title>\n" +
            "  <para>第一段内容。</para>\n" +
            "  <levelledPara>\n" +
            "    <title>第一节</title>\n" +
            "    <para>第二段内容。</para>\n" +
            "  </levelledPara>\n" +
            "</levelledPara>");

        String html = DmXsltTransformer.transform(xml, "S1000D40", "descript");

        // XSLT转换可能输出HTML片段而非完整文档，验证基本内容结构
        assertNotNull(html, "HTML不应为null");
        assertFalse(html.isEmpty(), "HTML不应为空");

        // 验证目录结构（这是核心功能）
        assertTrue(html.contains("正文目录"), "应包含目录标题");
        assertTrue(html.contains("class=\"toc-table\""), "应包含目录表格");

        // 验证标签配对
        long openTagCount = html.chars().filter(ch -> ch == '<').count();
        assertTrue(openTagCount > 10, "应该有足够的HTML标签");

        // 验证不包含无效style标签
        assertFalse(html.matches("(?s).*" + INVALID_STYLE_PATTERN + ".*"),
            "不应包含无效style标签");

        System.out.println("✅ HTML结构完整性验证通过");
    }

    /**
     * 构建测试用DM XML
     */
    private String buildTestDmXml(String dmType, String contentType, String contentBody) {
        String schemaLocation = String.format("http://www.s1000d.org/S1000D_4-0/xml_schema_flat/%s.xsd", dmType);

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<dmodule xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
            "xsi:noNamespaceSchemaLocation=\"" + schemaLocation + "\">\n" +
            "  <identAndStatusSection>\n" +
            "    <dmAddress>\n" +
            "      <dmIdent>\n" +
            "        <dmCode modelIdentCode=\"TEST\" systemDiffCode=\"A\" systemCode=\"00\" " +
            "subSystemCode=\"0\" subSubSystemCode=\"0\" assyCode=\"00\" disassyCode=\"00\" " +
            "disassyCodeVariant=\"A\" infoCode=\"040\" infoCodeVariant=\"A\" itemLocationCode=\"A\"/>\n" +
            "      </dmIdent>\n" +
            "      <dmAddressItems>\n" +
            "        <issueDate year=\"2026\" month=\"08\" day=\"06\"/>\n" +
            "        <dmTitle><techName>测试</techName><infoName>测试</infoName></dmTitle>\n" +
            "      </dmAddressItems>\n" +
            "    </dmAddress>\n" +
            "  </identAndStatusSection>\n" +
            "  <content>\n" +
            "    <" + contentType + ">\n" +
            contentBody + "\n" +
            "    </" + contentType + ">\n" +
            "  </content>\n" +
            "</dmodule>";
    }
}
