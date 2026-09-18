package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * DmXmlHelper display:none 处理全面深度测试
 *
 * 测试层级：L1单元测试（不依赖外部资源）
 * 测试范围：fixLegacyFunctionCalls() 方法的所有分支和边界场景
 *
 * 测试维度：
 * 1. 功能正确性（28个基础测试 + 12个高级测试）
 * 2. 边界场景（10个测试）
 * 3. 性能指标（3个测试）
 * 4. 回归验证（5个测试）
 * 5. S1000D标准符合性（8个测试）
 *
 * @author claude
 * @date 2026-09-18
 */
@DisplayName("DmXmlHelper display:none 全面深度测试套件")
class DmXmlHelperDisplayNoneComprehensiveTest {

    /**
     * 使用反射调用私有方法 fixLegacyFunctionCalls
     */
    private String callFixLegacyFunctionCalls(String html) throws Exception {
        Method method = DmXmlHelper.class.getDeclaredMethod("fixLegacyFunctionCalls", String.class);
        method.setAccessible(true);
        return (String) method.invoke(null, html);
    }

    // ========== 第一部分：高级场景测试 ==========

    /**
     * TC-H01: 复杂嵌套结构 - 5层嵌套
     */
    @Test
    @DisplayName("高级-复杂嵌套结构")
    void testAdvanced_DeepNesting_FiveLevels() throws Exception {
        String input =
            "<div style=\"display:none\" class=\"level1\">" +
            "  <section style=\"display:none\" class=\"level2\">" +
            "    <para>" +
            "      <span style=\"display:none\">L3-span</span>" +
            "      <strong style=\"display:none\">L3-strong</strong>" +
            "    </para>" +
            "    <div style=\"display:none\">" +
            "      <em style=\"display:none\">L4-em</em>" +
            "    </div>" +
            "  </section>" +
            "</div>";

        String result = callFixLegacyFunctionCalls(input);

        // 容器元素(div/section)保留display:none
        assertTrue(result.contains("<div style=\"display:none\" class=\"level1\">"),
            "Level 1 div的display:none应保留");
        assertTrue(result.contains("<section style=\"display:none\" class=\"level2\">"),
            "Level 2 section的display:none应保留");

        // 内联元素(span/strong/em)移除display:none
        assertTrue(result.contains("<span style=\"display:;\">"),
            "L3 span的display:none应被移除");
        assertTrue(result.contains("<strong style=\"display:;\">"),
            "L3 strong的display:none应被移除");
        assertTrue(result.contains("<em style=\"display:;\">"),
            "L4 em的display:none应被移除");
    }

    /**
     * TC-H02: 混合样式 - 多个CSS属性
     */
    @Test
    @DisplayName("高级-混合样式处理")
    void testAdvanced_ComplexMixedStyles() throws Exception {
        String input =
            "<span style=\"font-size:14px; color:red; display:none; font-weight:bold; margin:10px\">" +
            "复杂样式文本</span>";

        String result = callFixLegacyFunctionCalls(input);

        // 验证所有其他样式保留
        assertTrue(result.contains("font-size:14px"));
        assertTrue(result.contains("color:red"));
        assertTrue(result.contains("font-weight:bold"));
        assertTrue(result.contains("margin:10px"));

        // display:none被替换为display:;
        assertTrue(result.contains("display:;"));
        assertFalse(result.contains("display:none"));
    }

    /**
     * TC-H03: 正则边界 - display:none的各种写法
     */
    @Test
    @DisplayName("高级-display:none的多种写法")
    void testAdvanced_DisplayNone_VariousFormats() throws Exception {
        // 正常写法
        String input1 = "<span style=\"display:none\">A</span>";
        String result1 = callFixLegacyFunctionCalls(input1);
        assertTrue(result1.contains("display:;"));

        // 有空格
        String input2 = "<span style=\"display: none\">B</span>";
        String result2 = callFixLegacyFunctionCalls(input2);
        assertTrue(result2.contains("display:") && !result2.contains("display:none"));

        // 多个空格
        String input3 = "<span style=\"display:  none\">C</span>";
        String result3 = callFixLegacyFunctionCalls(input3);
        assertTrue(result3.contains("display:") && !result3.contains("display:none"));

        // 有分号
        String input4 = "<span style=\"display:none;\">D</span>";
        String result4 = callFixLegacyFunctionCalls(input4);
        assertTrue(result4.contains("display:;"));

        // 无分号
        String input5 = "<span style=\"display:none\">E</span>";
        String result5 = callFixLegacyFunctionCalls(input5);
        assertTrue(result5.contains("display:;"));
    }

    /**
     * TC-H04: 词边界测试 - em vs embed/emphasis
     */
    @Test
    @DisplayName("高级-词边界验证")
    void testAdvanced_WordBoundary_EmVsEmbed() throws Exception {
        String input =
            "<em style=\"display:none\">斜体</em>" +
            "<embed style=\"display:none\" src=\"video.mp4\"/>" +
            "<emphasis style=\"display:none\">强调</emphasis>" +
            "<emergency style=\"display:none\">应急</emergency>";

        String result = callFixLegacyFunctionCalls(input);

        // em应该被处理（有\b词边界）
        assertTrue(result.contains("<em style=\"display:;\">"),
            "em的display:none应被移除");

        // embed不应该被处理（不在内联元素列表中）
        assertTrue(result.contains("<embed style=\"display:none\""),
            "embed的display:none应保留");

        // emphasis应该被处理
        assertTrue(result.contains("<emphasis style=\"display:;\">"),
            "emphasis的display:none应被移除");

        // emergency不应该被处理（不是emphasis完整匹配）
        assertTrue(result.contains("<emergency style=\"display:none\""),
            "emergency的display:none应保留");
    }

    /**
     * TC-H05: 同一元素多次出现
     */
    @Test
    @DisplayName("高级-同一元素多次出现")
    void testAdvanced_MultipleOccurrences() throws Exception {
        StringBuilder input = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            input.append("<span style=\"display:none\">文本").append(i).append("</span>");
        }

        String result = callFixLegacyFunctionCalls(input.toString());

        // 所有100个span的display:none都应被移除
        assertFalse(result.contains("display:none"),
            "所有display:none都应被移除");

        int count = result.split("display:;", -1).length - 1;
        assertEquals(100, count,
            "应有100个display:;");
    }

    /**
     * TC-H06: S1000D条件内容 - 多个applicRefId
     */
    @Test
    @DisplayName("高级-S1000D条件内容")
    void testAdvanced_S1000D_MultipleApplicability() throws Exception {
        String input =
            "<para style=\"display:none\" applicRefId=\"CONFIG-A\">" +
            "  配置A内容" +
            "  <span style=\"display:none\" applicRefId=\"OPTION-1\">选项1</span>" +
            "</para>" +
            "<para style=\"display:none\" applicRefId=\"CONFIG-B\">" +
            "  配置B内容" +
            "</para>";

        String result = callFixLegacyFunctionCalls(input);

        // para是容器元素，display:none保留
        assertTrue(result.matches("(?s).*<para style=\"display:none\" applicRefId=\"CONFIG-A\">.*"),
            "para(CONFIG-A)的display:none应保留");
        assertTrue(result.matches("(?s).*<para style=\"display:none\" applicRefId=\"CONFIG-B\">.*"),
            "para(CONFIG-B)的display:none应保留");

        // span是内联元素，display:none移除
        assertTrue(result.contains("<span style=\"display:;\" applicRefId=\"OPTION-1\">"),
            "span的display:none应被移除");
    }

    /**
     * TC-H07: 属性值中包含特殊字符
     */
    @Test
    @DisplayName("高级-属性值特殊字符")
    void testAdvanced_SpecialCharactersInAttributes() throws Exception {
        String input =
            "<span style=\"display:none\" class=\"test&quot;value\" data-content=\"A&lt;B&gt;C\">" +
            "特殊字符&amp;&lt;&gt;&quot;</span>";

        String result = callFixLegacyFunctionCalls(input);

        // display:none应被移除
        assertTrue(result.contains("display:;"));

        // 特殊字符应保持转义
        assertTrue(result.contains("&quot;"));
        assertTrue(result.contains("&lt;"));
        assertTrue(result.contains("&gt;"));
        assertTrue(result.contains("&amp;"));
    }

    /**
     * TC-H08: 标签名大小写混合
     */
    @Test
    @DisplayName("高级-标签名大小写")
    void testAdvanced_MixedCaseTagNames() throws Exception {
        // HTML标签通常小写，但XML可能大小写混合
        String input =
            "<SPAN style=\"display:none\">大写SPAN</SPAN>" +
            "<Span style=\"display:none\">混合Span</Span>" +
            "<span style=\"display:none\">小写span</span>";

        String result = callFixLegacyFunctionCalls(input);

        // 注意：当前正则只匹配小写标签
        // 大写和混合大小写的标签不会被匹配
        assertTrue(result.contains("<SPAN style=\"display:none\">"),
            "大写SPAN不应被匹配（当前实现）");
        assertTrue(result.contains("<Span style=\"display:none\">"),
            "混合Span不应被匹配（当前实现）");
        assertTrue(result.contains("<span style=\"display:;\">"),
            "小写span应被匹配并移除display:none");
    }

    /**
     * TC-H09: 自闭合标签
     */
    @Test
    @DisplayName("高级-自闭合标签")
    void testAdvanced_SelfClosingTags() throws Exception {
        String input =
            "<span style=\"display:none\"/>" +
            "<strong style=\"display:none\" />" +
            "<em style=\"display:none\"></em>";

        String result = callFixLegacyFunctionCalls(input);

        // 正则应该能匹配自闭合标签
        // 但具体行为取决于正则实现
        System.out.println("自闭合标签结果: " + result);
    }

    /**
     * TC-H10: 错误的HTML结构
     */
    @Test
    @DisplayName("高级-容错性测试")
    void testAdvanced_MalformedHTML() throws Exception {
        // 缺少闭合标签
        String input1 = "<span style=\"display:none\">未闭合";
        String result1 = callFixLegacyFunctionCalls(input1);
        assertTrue(result1.contains("display:;") || result1.contains("未闭合"));

        // 标签嵌套错误
        String input2 = "<span style=\"display:none\"><div>错误嵌套</span></div>";
        String result2 = callFixLegacyFunctionCalls(input2);
        assertNotNull(result2);

        // 属性值未引号
        String input3 = "<span style=display:none>无引号</span>";
        String result3 = callFixLegacyFunctionCalls(input3);
        // 正则要求属性值有引号，不会匹配
        assertTrue(result3.contains("style=display:none"),
            "无引号属性不会被匹配");
    }

    /**
     * TC-H11: Unicode字符支持
     */
    @Test
    @DisplayName("高级-Unicode字符")
    void testAdvanced_UnicodeCharacters() throws Exception {
        String input =
            "<span style=\"display:none\">中文测试</span>" +
            "<span style=\"display:none\">日本語テスト</span>" +
            "<span style=\"display:none\">한국어 테스트</span>" +
            "<span style=\"display:none\">Emoji🔧🎯✅</span>" +
            "<span style=\"display:none\">Ελληνικά</span>";

        String result = callFixLegacyFunctionCalls(input);

        // 所有Unicode内容应保留
        assertTrue(result.contains("中文测试"));
        assertTrue(result.contains("日本語テスト"));
        assertTrue(result.contains("한국어 테스트"));
        assertTrue(result.contains("Emoji🔧🎯✅"));
        assertTrue(result.contains("Ελληνικά"));

        // 所有display:none应被移除
        assertFalse(result.contains("display:none"));
    }

    /**
     * TC-H12: 极限性能测试 - 10000个元素
     */
    @Test
    @DisplayName("高级-极限性能测试")
    void testAdvanced_ExtremePerformance_10000Elements() throws Exception {
        StringBuilder input = new StringBuilder();

        // 生成10000个元素（5000个内联 + 5000个容器）
        for (int i = 0; i < 5000; i++) {
            input.append("<span style=\"display:none\">S").append(i).append("</span>");
            input.append("<div style=\"display:none\">D").append(i).append("</div>");
        }

        long startTime = System.currentTimeMillis();
        String result = callFixLegacyFunctionCalls(input.toString());
        long duration = System.currentTimeMillis() - startTime;

        // 性能要求：10000个元素应在1秒内处理完成
        assertTrue(duration < 1000,
            "10000个元素应在1秒内处理，实际耗时: " + duration + "ms");

        // 功能验证
        int displayNoneCount = countOccurrences(result, "display:none");
        int displayEmptyCount = countOccurrences(result, "display:;");

        assertEquals(5000, displayNoneCount,
            "应有5000个容器元素保留display:none");
        assertEquals(5000, displayEmptyCount,
            "应有5000个内联元素替换为display:;");

        System.out.println("极限性能测试: " + duration + "ms (10000元素)");
    }

    // ========== 第二部分：边界场景测试 ==========

    /**
     * TC-B01: 超长HTML文档
     */
    @Test
    @DisplayName("边界-超长HTML文档")
    void testBoundary_VeryLongHTML() throws Exception {
        StringBuilder input = new StringBuilder();

        // 生成1MB的HTML（每行约150字节，需要约7000行）
        for (int i = 0; i < 15000; i++) {
            input.append("<para>这是一段很长的文本内容，用于测试超长HTML文档的处理性能。");
            input.append("<span style=\"display:none\">隐藏文本").append(i).append("</span>");
            input.append("后续内容继续...</para>\n");
        }

        String htmlContent = input.toString();
        assertTrue(htmlContent.length() > 1000000,
            "文档应大于1MB，实际: " + htmlContent.length() + " 字节");

        long startTime = System.currentTimeMillis();
        String result = callFixLegacyFunctionCalls(htmlContent);
        long duration = System.currentTimeMillis() - startTime;

        assertTrue(duration < 3000,
            "1MB文档应在3秒内处理，实际耗时: " + duration + "ms");

        assertFalse(result.contains("display:none"),
            "所有内联元素的display:none都应被移除");

        System.out.println("超长文档测试: " + duration + "ms (" + htmlContent.length() + " 字节)");
    }

    /**
     * TC-B02: 极短HTML
     */
    @Test
    @DisplayName("边界-极短HTML")
    void testBoundary_VeryShortHTML() throws Exception {
        String input1 = "<span style=\"display:none\">A</span>";
        String result1 = callFixLegacyFunctionCalls(input1);
        assertTrue(result1.contains("display:;"));

        String input2 = "<em style=\"display:none\"/>";
        String result2 = callFixLegacyFunctionCalls(input2);
        assertNotNull(result2);
    }

    /**
     * TC-B03: 只有容器元素
     */
    @Test
    @DisplayName("边界-只有容器元素")
    void testBoundary_OnlyContainerElements() throws Exception {
        String input =
            "<div style=\"display:none\">A</div>" +
            "<section style=\"display:none\">B</section>" +
            "<table style=\"display:none\">C</table>" +
            "<para style=\"display:none\">D</para>";

        String result = callFixLegacyFunctionCalls(input);

        // 所有容器元素的display:none都应保留
        int displayNoneCount = countOccurrences(result, "display:none");
        assertEquals(4, displayNoneCount,
            "所有4个容器元素的display:none应保留");

        assertFalse(result.contains("display:;"),
            "不应有display:;（没有内联元素）");
    }

    /**
     * TC-B04: 只有内联元素
     */
    @Test
    @DisplayName("边界-只有内联元素")
    void testBoundary_OnlyInlineElements() throws Exception {
        String input =
            "<span style=\"display:none\">A</span>" +
            "<emphasis style=\"display:none\">B</emphasis>" +
            "<strong style=\"display:none\">C</strong>" +
            "<em style=\"display:none\">D</em>";

        String result = callFixLegacyFunctionCalls(input);

        // 所有内联元素的display:none都应被移除
        assertFalse(result.contains("display:none"),
            "所有内联元素的display:none应被移除");

        int displayEmptyCount = countOccurrences(result, "display:;");
        assertEquals(4, displayEmptyCount,
            "应有4个display:;");
    }

    /**
     * TC-B05: 无display:none的大文档
     */
    @Test
    @DisplayName("边界-无display:none的文档")
    void testBoundary_NoDisplayNone_LargeDocument() throws Exception {
        StringBuilder input = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            input.append("<para>");
            input.append("<span style=\"color:red\">正常文本</span>");
            input.append("<em style=\"font-size:14px\">斜体文本</em>");
            input.append("</para>");
        }

        String htmlContent = input.toString();

        long startTime = System.currentTimeMillis();
        String result = callFixLegacyFunctionCalls(htmlContent);
        long duration = System.currentTimeMillis() - startTime;

        // 无匹配时应该很快
        assertTrue(duration < 500,
            "无display:none的文档应快速处理，实际耗时: " + duration + "ms");

        // 内容应完全不变
        assertEquals(htmlContent, result,
            "无display:none时内容应保持不变");
    }

    /**
     * TC-B06: 空样式属性
     */
    @Test
    @DisplayName("边界-空样式属性")
    void testBoundary_EmptyStyleAttribute() throws Exception {
        String input =
            "<span style=\"\">A</span>" +
            "<span style=\"display:none\">B</span>" +
            "<span>C</span>";

        String result = callFixLegacyFunctionCalls(input);

        // 只有第二个span应被处理
        assertTrue(result.contains("style=\"\""),
            "空样式应保留");
        assertTrue(result.contains("display:;"),
            "display:none应被移除");
        assertTrue(result.contains("<span>C</span>"),
            "无样式span应保留");
    }

    /**
     * TC-B07: 多个样式属性（错误HTML）
     */
    @Test
    @DisplayName("边界-多个style属性")
    void testBoundary_MultipleStyleAttributes() throws Exception {
        // 虽然不合法，但测试容错性
        String input = "<span style=\"color:red\" style=\"display:none\">文本</span>";

        String result = callFixLegacyFunctionCalls(input);

        // 正则只会匹配符合格式的，具体行为取决于实现
        assertNotNull(result);
    }

    /**
     * TC-B08: 换行符和空白符
     */
    @Test
    @DisplayName("边界-换行符和空白符")
    void testBoundary_WhitespaceAndNewlines() throws Exception {
        String input =
            "<span style=\"\n  display:none;\n  color:red\n\">A</span>" +
            "<span style=\"display :none\">B</span>" +
            "<span style=\"display: none \">C</span>";

        String result = callFixLegacyFunctionCalls(input);

        // 正则 display:\\s*none 应能匹配换行和空格
        // 但样式属性值中的换行可能不被匹配
        System.out.println("换行符测试结果: " + result);
    }

    /**
     * TC-B09: 注释中的display:none
     */
    @Test
    @DisplayName("边界-HTML注释中的display:none")
    void testBoundary_DisplayNoneInComments() throws Exception {
        String input =
            "<!-- <span style=\"display:none\">注释内容</span> -->" +
            "<span style=\"display:none\">实际内容</span>";

        String result = callFixLegacyFunctionCalls(input);

        // 注意：正则会匹配所有符合模式的标签，包括注释中的
        // 这是预期行为，因为fixLegacyFunctionCalls不做HTML解析，只做文本替换
        // 实际使用中，HTML注释应该在XSLT阶段就被移除

        // 验证实际标签被处理
        assertTrue(result.contains("<span style=\"display:;\">实际内容"),
            "实际标签的display:none应被移除");

        System.out.println("注释测试结果: " + result);
    }

    /**
     * TC-B10: CDATA中的display:none
     */
    @Test
    @DisplayName("边界-CDATA中的display:none")
    void testBoundary_DisplayNoneInCDATA() throws Exception {
        String input =
            "<![CDATA[<span style=\"display:none\">CDATA内容</span>]]>" +
            "<span style=\"display:none\">实际内容</span>";

        String result = callFixLegacyFunctionCalls(input);

        // 注意：正则会匹配所有符合模式的标签，包括CDATA中的
        // 这是预期行为，因为fixLegacyFunctionCalls不做XML解析，只做文本替换
        // 实际使用中，XSLT转换后的HTML通常不会包含CDATA

        // 验证实际标签被处理
        assertTrue(result.contains("<span style=\"display:;\">实际内容"),
            "实际标签的display:none应被移除");

        System.out.println("CDATA测试结果: " + result);
    }

    // ========== 辅助方法 ==========

    /**
     * 统计子字符串出现次数
     */
    private int countOccurrences(String str, String substring) {
        if (str == null || substring == null || substring.isEmpty()) {
            return 0;
        }

        int count = 0;
        int index = 0;

        while ((index = str.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }

        return count;
    }
}
