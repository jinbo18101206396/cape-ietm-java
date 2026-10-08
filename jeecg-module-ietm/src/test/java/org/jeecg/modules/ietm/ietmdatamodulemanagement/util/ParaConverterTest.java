package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ParaConverter单元测试
 * 覆盖para2html和html2para双向转换，以及安全防护
 *
 * @author IETM Team
 * @date 2026-09-23
 */
@DisplayName("ParaConverter转换工具测试")
public class ParaConverterTest {

    // ==================== para2html测试 ====================

    @Test
    @DisplayName("para2html - 基础元素转换（para→p）")
    public void testPara2html_BasicPara() {
        String xml = "<para>测试内容</para>";
        String html = ParaConverter.para2html(xml);

        assertTrue(html.contains("<p>"), "应包含<p>标签");
        assertTrue(html.contains("测试内容"), "应包含原始文本");
        assertTrue(html.contains("</p>"), "应包含</p>标签");
        assertFalse(html.contains("<para>"), "不应包含<para>标签");
    }

    @Test
    @DisplayName("para2html - emphasis转换（emphasis→strong）")
    public void testPara2html_Emphasis() {
        String xml = "<para>普通文本<emphasis>强调文本</emphasis>结束</para>";
        String html = ParaConverter.para2html(xml);

        assertTrue(html.contains("<strong>"), "应包含<strong>标签");
        assertTrue(html.contains("强调文本"), "应包含强调文本");
        assertTrue(html.contains("</strong>"), "应包含</strong>标签");
        assertFalse(html.contains("<emphasis>"), "不应包含<emphasis>标签");
    }

    @Test
    @DisplayName("para2html - superScript转换（superScript→sup）")
    public void testPara2html_SuperScript() {
        String xml = "<para>X<superScript>2</superScript></para>";
        String html = ParaConverter.para2html(xml);

        assertTrue(html.contains("<sup>"), "应包含<sup>标签");
        assertTrue(html.contains("</sup>"), "应包含</sup>标签");
        assertFalse(html.contains("<superScript>"), "不应包含<superScript>标签");
    }

    @Test
    @DisplayName("para2html - subScript转换（subScript→sub）")
    public void testPara2html_SubScript() {
        String xml = "<para>H<subScript>2</subScript>O</para>";
        String html = ParaConverter.para2html(xml);

        assertTrue(html.contains("<sub>"), "应包含<sub>标签");
        assertTrue(html.contains("</sub>"), "应包含</sub>标签");
        assertFalse(html.contains("<subScript>"), "不应包含<subScript>标签");
    }

    @Test
    @DisplayName("para2html - randomList转换（randomList→ul）")
    public void testPara2html_RandomList() {
        String xml = "<para><randomList><listItem>项目1</listItem><listItem>项目2</listItem></randomList></para>";
        String html = ParaConverter.para2html(xml);

        assertTrue(html.contains("<ul>"), "应包含<ul>标签");
        assertTrue(html.contains("<li>"), "应包含<li>标签");
        assertTrue(html.contains("</ul>"), "应包含</ul>标签");
        assertFalse(html.contains("<randomList>"), "不应包含<randomList>标签");
    }

    @Test
    @DisplayName("para2html - sequentialList转换（sequentialList→ol）")
    public void testPara2html_SequentialList() {
        String xml = "<para><sequentialList><listItem>步骤1</listItem><listItem>步骤2</listItem></sequentialList></para>";
        String html = ParaConverter.para2html(xml);

        assertTrue(html.contains("<ol>"), "应包含<ol>标签");
        assertTrue(html.contains("<li>"), "应包含<li>标签");
        assertTrue(html.contains("</ol>"), "应包含</ol>标签");
        assertFalse(html.contains("<sequentialList>"), "不应包含<sequentialList>标签");
    }

    @Test
    @DisplayName("para2html - definitionList转换（definitionList→table）")
    public void testPara2html_DefinitionList() {
        String xml = "<para><definitionList>" +
                "<definitionListItem>" +
                "<listItemTerm>术语1</listItemTerm>" +
                "<listItemDefinition>定义1</listItemDefinition>" +
                "</definitionListItem>" +
                "</definitionList></para>";
        String html = ParaConverter.para2html(xml);

        assertTrue(html.contains("<table deflist=\"1\">"), "应包含<table deflist=\"1\">标签");
        assertTrue(html.contains("<tr>"), "应包含<tr>标签");
        assertTrue(html.contains("<th>"), "应包含<th>标签");
        assertTrue(html.contains("<td>"), "应包含<td>标签");
        assertTrue(html.contains("术语1"), "应包含术语文本");
        assertTrue(html.contains("定义1"), "应包含定义文本");
    }

    @Test
    @DisplayName("para2html - 复杂嵌套转换")
    public void testPara2html_ComplexNested() {
        String xml = "<para>外层<emphasis>强调<superScript>上标</superScript></emphasis>结束</para>";
        String html = ParaConverter.para2html(xml);

        assertTrue(html.contains("<strong>"), "应包含<strong>标签");
        assertTrue(html.contains("<sup>"), "应包含<sup>标签");
        assertTrue(html.contains("外层"), "应包含外层文本");
        assertTrue(html.contains("上标"), "应包含上标文本");
    }

    @Test
    @DisplayName("para2html - 空内容处理")
    public void testPara2html_EmptyContent() {
        String xml = "";
        String html = ParaConverter.para2html(xml);

        assertEquals("", html, "空输入应返回空字符串");
    }

    @Test
    @DisplayName("para2html - null输入处理")
    public void testPara2html_NullInput() {
        String html = ParaConverter.para2html(null);

        assertEquals("", html, "null输入应返回空字符串");
    }

    // ==================== html2para测试 ====================

    @Test
    @DisplayName("html2para - 基础元素转换（p→para）")
    public void testHtml2para_BasicPara() {
        String html = "<p>测试内容</p>";
        String xml = ParaConverter.html2para(html);

        assertTrue(xml.contains("<para>"), "应包含<para>标签");
        assertTrue(xml.contains("测试内容"), "应包含原始文本");
        assertTrue(xml.contains("</para>"), "应包含</para>标签");
        assertFalse(xml.contains("<p>"), "不应包含<p>标签");
    }

    @Test
    @DisplayName("html2para - strong转换（strong→emphasis）")
    public void testHtml2para_Strong() {
        String html = "<p>普通文本<strong>强调文本</strong>结束</p>";
        String xml = ParaConverter.html2para(html);

        assertTrue(xml.contains("<emphasis>"), "应包含<emphasis>标签");
        assertTrue(xml.contains("强调文本"), "应包含强调文本");
        assertTrue(xml.contains("</emphasis>"), "应包含</emphasis>标签");
        assertFalse(xml.contains("<strong>"), "不应包含<strong>标签");
    }

    @Test
    @DisplayName("html2para - sup转换（sup→superScript）")
    public void testHtml2para_Sup() {
        String html = "<p>X<sup>2</sup></p>";
        String xml = ParaConverter.html2para(html);

        assertTrue(xml.contains("<superScript"), "应包含<superScript标签");
        assertTrue(xml.contains("</superScript>"), "应包含</superScript>标签");
        assertFalse(xml.contains("<sup>"), "不应包含<sup>标签");
    }

    @Test
    @DisplayName("html2para - sub转换（sub→subScript）")
    public void testHtml2para_Sub() {
        String html = "<p>H<sub>2</sub>O</p>";
        String xml = ParaConverter.html2para(html);

        assertTrue(xml.contains("<subScript"), "应包含<subScript标签");
        assertTrue(xml.contains("</subScript>"), "应包含</subScript>标签");
        assertFalse(xml.contains("<sub>"), "不应包含<sub>标签");
    }

    @Test
    @DisplayName("html2para - ul转换（ul→randomList）")
    public void testHtml2para_Ul() {
        String html = "<ul><li>项目1</li><li>项目2</li></ul>";
        String xml = ParaConverter.html2para(html);

        assertTrue(xml.contains("<randomList>"), "应包含<randomList>标签");
        assertTrue(xml.contains("<listItem>"), "应包含<listItem>标签");
        assertTrue(xml.contains("</randomList>"), "应包含</randomList>标签");
        assertFalse(xml.contains("<ul>"), "不应包含<ul>标签");
    }

    @Test
    @DisplayName("html2para - ol转换（ol→sequentialList）")
    public void testHtml2para_Ol() {
        String html = "<ol><li>步骤1</li><li>步骤2</li></ol>";
        String xml = ParaConverter.html2para(html);

        assertTrue(xml.contains("<sequentialList>"), "应包含<sequentialList>标签");
        assertTrue(xml.contains("<listItem>"), "应包含<listItem>标签");
        assertTrue(xml.contains("</sequentialList>"), "应包含</sequentialList>标签");
        assertFalse(xml.contains("<ol>"), "不应包含<ol>标签");
    }

    @Test
    @DisplayName("html2para - table deflist转换（table→definitionList）")
    public void testHtml2para_TableDeflist() {
        String html = "<table deflist=\"1\">" +
                "<tr><th>术语1</th><td>定义1</td></tr>" +
                "</table>";
        String xml = ParaConverter.html2para(html);

        assertTrue(xml.contains("<definitionList>"), "应包含<definitionList>标签");
        assertTrue(xml.contains("<definitionListItem>"), "应包含<definitionListItem>标签");
        assertTrue(xml.contains("<listItemTerm>"), "应包含<listItemTerm>标签");
        assertTrue(xml.contains("<listItemDefinition>"), "应包含<listItemDefinition>标签");
        assertFalse(xml.contains("<table"), "不应包含<table标签");
    }

    @Test
    @DisplayName("html2para - 清理HTML冗余标签")
    public void testHtml2para_CleanupRedundant() {
        String html = "<p>&nbsp;<br/>测试<br>内容</p>";
        String xml = ParaConverter.html2para(html);

        assertFalse(xml.contains("&nbsp;"), "不应包含&nbsp;");
        assertFalse(xml.contains("<br"), "不应包含<br标签");
        assertTrue(xml.contains("测试"), "应包含文本内容");
        assertTrue(xml.contains("内容"), "应包含文本内容");
    }

    @Test
    @DisplayName("html2para - 空内容处理")
    public void testHtml2para_EmptyContent() {
        String html = "";
        String xml = ParaConverter.html2para(html);

        assertEquals("<para></para>", xml, "空输入应返回<para></para>");
    }

    @Test
    @DisplayName("html2para - null输入处理")
    public void testHtml2para_NullInput() {
        String xml = ParaConverter.html2para(null);

        assertEquals("<para></para>", xml, "null输入应返回<para></para>");
    }

    // ==================== 双向转换测试 ====================

    @Test
    @DisplayName("双向转换 - para2html→html2para一致性")
    public void testBidirectional_Consistency() {
        String originalXml = "<para>测试<emphasis>加粗</emphasis>内容</para>";

        // XML→HTML
        String html = ParaConverter.para2html(originalXml);

        // HTML→XML
        String resultXml = ParaConverter.html2para(html);

        // 验证关键内容
        assertTrue(resultXml.contains("<para>"), "应包含<para>标签");
        assertTrue(resultXml.contains("<emphasis>"), "应包含<emphasis>标签");
        assertTrue(resultXml.contains("测试"), "应包含原始文本");
        assertTrue(resultXml.contains("加粗"), "应包含原始文本");
    }

    @Test
    @DisplayName("双向转换 - 复杂结构一致性")
    public void testBidirectional_ComplexStructure() {
        String originalXml = "<para><randomList>" +
                "<listItem>项目1<emphasis>强调</emphasis></listItem>" +
                "<listItem>项目2</listItem>" +
                "</randomList></para>";

        // XML→HTML
        String html = ParaConverter.para2html(originalXml);

        // HTML→XML
        String resultXml = ParaConverter.html2para(html);

        // 验证关键结构
        assertTrue(resultXml.contains("<randomList>"), "应包含<randomList>标签");
        assertTrue(resultXml.contains("<listItem>"), "应包含<listItem>标签");
        assertTrue(resultXml.contains("<emphasis>"), "应包含<emphasis>标签");
    }

    // ==================== 异常处理测试 ====================

    @Test
    @DisplayName("异常处理 - 格式错误的XML")
    public void testException_MalformedXml() {
        String malformedXml = "<para>未闭合标签<emphasis>测试";

        assertThrows(RuntimeException.class, () -> {
            ParaConverter.para2html(malformedXml);
        }, "格式错误的XML应抛出异常");
    }

    @Test
    @DisplayName("异常处理 - 无效的标签")
    public void testException_InvalidTags() {
        String invalidXml = "<para><invalidTag>测试</invalidTag></para>";

        // 应不抛出异常，未识别标签应被忽略
        assertDoesNotThrow(() -> {
            ParaConverter.para2html(invalidXml);
        }, "未识别的标签应被忽略，不应抛出异常");
    }

    // ==================== 边界条件测试 ====================

    @Test
    @DisplayName("边界条件 - 大文本内容")
    public void testBoundary_LargeText() {
        StringBuilder sb = new StringBuilder("<para>");
        for (int i = 0; i < 10000; i++) {
            sb.append("测试内容").append(i);
        }
        sb.append("</para>");

        String xml = sb.toString();
        String html = ParaConverter.para2html(xml);

        assertNotNull(html, "大文本应正常转换");
        assertTrue(html.contains("<p>"), "应包含<p>标签");
    }

    @Test
    @DisplayName("边界条件 - 深度嵌套")
    public void testBoundary_DeepNesting() {
        String xml = "<para>" +
                "<emphasis>" +
                "<emphasis>" +
                "<emphasis>深度嵌套</emphasis>" +
                "</emphasis>" +
                "</emphasis>" +
                "</para>";

        String html = ParaConverter.para2html(xml);

        assertNotNull(html, "深度嵌套应正常转换");
        assertTrue(html.contains("深度嵌套"), "应包含文本内容");
    }

    @Test
    @DisplayName("边界条件 - 特殊字符")
    public void testBoundary_SpecialCharacters() {
        String xml = "<para>&lt;测试&gt;&amp;特殊字符&quot;</para>";

        assertDoesNotThrow(() -> {
            ParaConverter.para2html(xml);
        }, "特殊字符应正常处理");
    }

    @Test
    @DisplayName("边界条件 - 空白字符")
    public void testBoundary_Whitespace() {
        String xml = "<para>   多个   空格   </para>";
        String html = ParaConverter.para2html(xml);

        assertNotNull(html, "空白字符应正常处理");
        assertTrue(html.contains("多个"), "应保留文本内容");
    }

    @Test
    @DisplayName("边界条件 - 中文内容")
    public void testBoundary_ChineseContent() {
        String xml = "<para>这是中文内容<emphasis>测试</emphasis></para>";
        String html = ParaConverter.para2html(xml);

        assertTrue(html.contains("这是中文内容"), "应正确处理中文");
        assertTrue(html.contains("测试"), "应正确处理中文");
    }

    @Test
    @DisplayName("边界条件 - 混合语言")
    public void testBoundary_MixedLanguage() {
        String xml = "<para>English中文日本語한국어</para>";
        String html = ParaConverter.para2html(xml);

        assertTrue(html.contains("English"), "应包含英文");
        assertTrue(html.contains("中文"), "应包含中文");
        assertTrue(html.contains("日本語"), "应包含日文");
        assertTrue(html.contains("한국어"), "应包含韩文");
    }

    // ==================== 性能测试 ====================

    @Test
    @DisplayName("性能测试 - para2html转换时间")
    public void testPerformance_Para2html() {
        String xml = "<para>性能测试内容<emphasis>加粗</emphasis></para>";

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            ParaConverter.para2html(xml);
        }
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        assertTrue(duration < 5000, "1000次转换应在5秒内完成，实际耗时: " + duration + "ms");
    }

    @Test
    @DisplayName("性能测试 - html2para转换时间")
    public void testPerformance_Html2para() {
        String html = "<p>性能测试内容<strong>加粗</strong></p>";

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            ParaConverter.html2para(html);
        }
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        assertTrue(duration < 5000, "1000次转换应在5秒内完成，实际耗时: " + duration + "ms");
    }
}
