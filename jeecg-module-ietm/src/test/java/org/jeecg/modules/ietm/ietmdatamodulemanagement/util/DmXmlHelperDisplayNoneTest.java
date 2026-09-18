package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;

/**
 * DmXmlHelper display:none 处理深度测试
 *
 * 测试目标：验证 fixLegacyFunctionCalls() 中的 display:none 选择性移除逻辑
 *
 * 关键验证点：
 * 1. 内联元素（span/emphasis/strong/em）- display:none 应被移除
 * 2. 容器元素（div/section/table等）- display:none 应保留
 * 3. 混合样式处理（display:none + 其他CSS）
 * 4. 嵌套元素处理
 * 5. S1000D 条件内容（applicability）
 * 6. 边界场景（空值、特殊字符、超长HTML）
 *
 * @author claude
 * @date 2026-09-18
 */
class DmXmlHelperDisplayNoneTest {

    /**
     * 使用反射调用私有方法 fixLegacyFunctionCalls
     */
    private String callFixLegacyFunctionCalls(String html) throws Exception {
        Method method = DmXmlHelper.class.getDeclaredMethod("fixLegacyFunctionCalls", String.class);
        method.setAccessible(true);
        return (String) method.invoke(null, html);
    }

    // ========== 第一部分：内联元素测试（应删除display:none）==========

    /**
     * TC-01: span元素 - 完全删除display:none
     */
    @Test
    void testDisplayNone_Span_Removed() throws Exception {
        String input = "<p>前文<span style=\"display:none\">隐藏内容</span>后文</p>";
        String result = callFixLegacyFunctionCalls(input);

        // 验证display:none被移除（替换为display:;）
        assertTrue(result.contains("display:;"),
            "span的display:none应被移除");
        assertFalse(result.contains("display:none"),
            "不应包含display:none");
        assertTrue(result.contains("隐藏内容"),
            "内容应保留");
    }

    /**
     * TC-02: emphasis元素 - 完全删除display:none
     */
    @Test
    void testDisplayNone_Emphasis_Removed() throws Exception {
        String input = "<para>正常<emphasis style=\"display:none\">隐藏</emphasis>文本</para>";
        String result = callFixLegacyFunctionCalls(input);

        assertTrue(result.contains("display:;"));
        assertFalse(result.contains("display:none"));
    }

    /**
     * TC-03: strong元素 - 完全删除display:none
     */
    @Test
    void testDisplayNone_Strong_Removed() throws Exception {
        String input = "<p>重要<strong style=\"display:none\">警告</strong>信息</p>";
        String result = callFixLegacyFunctionCalls(input);

        assertTrue(result.contains("display:;"));
        assertFalse(result.contains("display:none"));
    }

    /**
     * TC-04: em元素 - 完全删除display:none（词边界测试）
     */
    @Test
    void testDisplayNone_Em_Removed() throws Exception {
        String input = "<p>斜体<em style=\"display:none\">文本</em>测试</p>";
        String result = callFixLegacyFunctionCalls(input);

        assertTrue(result.contains("display:;"));
        assertFalse(result.contains("display:none"));
    }

    /**
     * TC-05: 词边界验证 - em不应误匹配embed标签
     */
    @Test
    void testDisplayNone_EmbedNotMatched() throws Exception {
        String input = "<embed style=\"display:none\" src=\"video.mp4\"/>";
        String result = callFixLegacyFunctionCalls(input);

        // embed标签不应被匹配（因为正则有\b词边界）
        assertTrue(result.contains("display:none"),
            "embed标签的display:none应保留（不是内联文本元素）");
    }

    // ========== 第二部分：容器元素测试（应保留display:none）==========

    /**
     * TC-06: div元素 - 保留display:none（用于UI控制）
     */
    @Test
    void testDisplayNone_Div_Preserved() throws Exception {
        String input = "<div style=\"display:none\" class=\"warning-panel\">警告面板</div>";
        String result = callFixLegacyFunctionCalls(input);

        // div的display:none应保留（容器元素，用于JavaScript动态控制）
        assertTrue(result.contains("display:none"),
            "div的display:none应保留");
        assertTrue(result.contains("warning-panel"));
    }

    /**
     * TC-07: section元素 - 保留display:none
     */
    @Test
    void testDisplayNone_Section_Preserved() throws Exception {
        String input = "<section style=\"display:none\"><title>章节</title><para>内容</para></section>";
        String result = callFixLegacyFunctionCalls(input);

        assertTrue(result.contains("display:none"),
            "section的display:none应保留");
    }

    /**
     * TC-08: table元素 - 保留display:none
     */
    @Test
    void testDisplayNone_Table_Preserved() throws Exception {
        String input = "<table style=\"display:none\"><tr><td>数据</td></tr></table>";
        String result = callFixLegacyFunctionCalls(input);

        assertTrue(result.contains("display:none"),
            "table的display:none应保留");
    }

    // ========== 第三部分：混合样式测试 ==========

    /**
     * TC-09: 混合样式 - 仅移除display:none，保留其他CSS
     */
    @Test
    void testDisplayNone_MixedStyles_Span() throws Exception {
        String input = "<span style=\"color:red; display:none; font-size:14px\">文本</span>";
        String result = callFixLegacyFunctionCalls(input);

        // 验证其他样式保留
        assertTrue(result.contains("color:red"), "color样式应保留");
        assertTrue(result.contains("font-size:14px"), "font-size样式应保留");
        assertTrue(result.contains("display:;"), "display:none应被替换");
        assertFalse(result.contains("display:none"));
    }

    /**
     * TC-10: 混合样式 - display:none后无分号
     */
    @Test
    void testDisplayNone_NoSemicolon() throws Exception {
        String input = "<span style=\"color:red; display:none\">文本</span>";
        String result = callFixLegacyFunctionCalls(input);

        assertTrue(result.contains("color:red"));
        assertTrue(result.contains("display:;"));
        assertFalse(result.contains("display:none"));
    }

    /**
     * TC-11: 混合样式 - display:none在开头
     */
    @Test
    void testDisplayNone_AtBeginning() throws Exception {
        String input = "<span style=\"display:none; color:blue\">文本</span>";
        String result = callFixLegacyFunctionCalls(input);

        assertTrue(result.contains("color:blue"));
        assertTrue(result.contains("display:;"));
    }

    /**
     * TC-12: 混合样式 - display: none（有空格）
     */
    @Test
    void testDisplayNone_WithSpaces() throws Exception {
        String input = "<span style=\"display: none; color:green\">文本</span>";
        String result = callFixLegacyFunctionCalls(input);

        assertTrue(result.contains("color:green"));
        // 正则 display:\\s*none 应匹配 display: none
        assertTrue(result.contains("display:;") || result.contains("display: ;"));
    }

    // ========== 第四部分：嵌套场景测试 ==========

    /**
     * TC-13: 嵌套内联元素 - 内部和外部都应处理
     */
    @Test
    void testDisplayNone_NestedInline() throws Exception {
        String input =
            "<span style=\"display:none\">" +
            "外层<emphasis style=\"display:none\">内层</emphasis>文本" +
            "</span>";

        String result = callFixLegacyFunctionCalls(input);

        // 两个display:none都应被移除
        assertFalse(result.contains("display:none"),
            "所有内联元素的display:none都应被移除");
        int displayColonCount = result.split("display:;", -1).length - 1;
        assertEquals(2, displayColonCount,
            "应有2个display:;（两个内联元素）");
    }

    /**
     * TC-14: 容器包含内联元素 - 区分处理
     */
    @Test
    void testDisplayNone_ContainerWithInline() throws Exception {
        String input =
            "<div style=\"display:none\">" +
            "<span style=\"display:none\">隐藏文本</span>" +
            "</div>";

        String result = callFixLegacyFunctionCalls(input);

        // div的display:none保留，span的display:none移除
        assertTrue(result.contains("<div style=\"display:none\">"),
            "div的display:none应保留");
        assertTrue(result.contains("<span style=\"display:;\">") ||
                   result.contains("span") && result.contains("display:;"),
            "span的display:none应被移除");
    }

    /**
     * TC-15: 多层嵌套 - 复杂结构
     */
    @Test
    void testDisplayNone_DeepNesting() throws Exception {
        String input =
            "<section style=\"display:none\">" +
            "  <title>章节</title>" +
            "  <para>正常文本<span style=\"display:none\">隐藏</span>后续</para>" +
            "</section>";

        String result = callFixLegacyFunctionCalls(input);

        // section保留，span移除
        assertTrue(result.contains("<section style=\"display:none\">"));
        assertTrue(result.contains("display:;"));
        // section的display:none + span替换后的display:; = 应该有1个display:none和1个display:;
        assertTrue(result.split("display:none", -1).length - 1 == 1);
    }

    // ========== 第五部分：S1000D条件内容测试 ==========

    /**
     * TC-16: S1000D applicability条件 - 保留结构
     */
    @Test
    void testDisplayNone_S1000D_Applicability() throws Exception {
        String input =
            "<para>适用于A320</para>" +
            "<para style=\"display:none\" applicRefId=\"A330\">适用于A330</para>";

        String result = callFixLegacyFunctionCalls(input);

        // para是容器元素，display:none应保留
        assertTrue(result.contains("display:none"),
            "S1000D条件内容的display:none应保留（para是容器）");
        assertTrue(result.contains("applicRefId=\"A330\""));
    }

    /**
     * TC-17: 条件内容中的内联元素
     */
    @Test
    void testDisplayNone_S1000D_InlineInCondition() throws Exception {
        String input =
            "<para>" +
            "正常文本" +
            "<span style=\"display:none\" applicRefId=\"CONFIG-B\">配置B专用</span>" +
            "后续文本" +
            "</para>";

        String result = callFixLegacyFunctionCalls(input);

        // span是内联元素，display:none应被移除
        assertTrue(result.contains("display:;"));
        assertTrue(result.contains("applicRefId=\"CONFIG-B\""));
    }

    // ========== 第六部分：边界场景测试 ==========

    /**
     * TC-18: 空字符串
     */
    @Test
    void testDisplayNone_EmptyString() throws Exception {
        String result = callFixLegacyFunctionCalls("");
        assertEquals("", result);
    }

    /**
     * TC-19: null值
     */
    @Test
    void testDisplayNone_Null() throws Exception {
        String result = callFixLegacyFunctionCalls(null);
        assertNull(result);
    }

    /**
     * TC-20: 无display:none的HTML
     */
    @Test
    void testDisplayNone_NoDisplayNone() throws Exception {
        String input = "<p>正常段落<span style=\"color:red\">红色文本</span></p>";
        String result = callFixLegacyFunctionCalls(input);

        assertEquals(input, result,
            "无display:none的HTML应保持不变");
    }

    /**
     * TC-21: 大文档性能测试（1000个元素）
     */
    @Test
    void testDisplayNone_LargeDocument_Performance() throws Exception {
        StringBuilder input = new StringBuilder("<document>");

        // 生成1000个元素（500个span + 500个div）
        for (int i = 0; i < 500; i++) {
            input.append("<span style=\"display:none\">文本").append(i).append("</span>");
            input.append("<div style=\"display:none\">容器").append(i).append("</div>");
        }
        input.append("</document>");

        long startTime = System.currentTimeMillis();
        String result = callFixLegacyFunctionCalls(input.toString());
        long duration = System.currentTimeMillis() - startTime;

        // 性能验证：1000个元素处理应在500ms内完成
        assertTrue(duration < 500,
            "大文档处理应在500ms内完成，实际耗时: " + duration + "ms");

        // 功能验证：span的display:none被移除，div的display:none保留
        assertTrue(result.contains("display:;"));
        assertTrue(result.contains("display:none"));
    }

    /**
     * TC-22: 特殊字符转义
     */
    @Test
    void testDisplayNone_SpecialCharacters() throws Exception {
        String input = "<span style=\"display:none\">特殊字符：&lt;&gt;&amp;&quot;</span>";
        String result = callFixLegacyFunctionCalls(input);

        assertTrue(result.contains("display:;"));
        assertTrue(result.contains("&lt;&gt;&amp;&quot;"),
            "特殊字符应保持转义");
    }

    /**
     * TC-23: 中文内容
     */
    @Test
    void testDisplayNone_ChineseContent() throws Exception {
        String input = "<span style=\"display:none\">这是中文测试内容，包含标点符号：，。！？</span>";
        String result = callFixLegacyFunctionCalls(input);

        assertTrue(result.contains("display:;"));
        assertTrue(result.contains("这是中文测试内容"));
    }

    /**
     * TC-24: 多个相同元素
     */
    @Test
    void testDisplayNone_MultipleSpans() throws Exception {
        String input =
            "<p>" +
            "<span style=\"display:none\">A</span>" +
            "<span style=\"display:none\">B</span>" +
            "<span style=\"display:none\">C</span>" +
            "</p>";

        String result = callFixLegacyFunctionCalls(input);

        // 所有3个span的display:none都应被移除
        assertFalse(result.contains("display:none"));
        int count = result.split("display:;", -1).length - 1;
        assertEquals(3, count, "应有3个display:;");
    }

    /**
     * TC-25: 属性顺序变化
     */
    @Test
    void testDisplayNone_AttributeOrder() throws Exception {
        // style在前
        String input1 = "<span style=\"display:none\" class=\"highlight\">文本</span>";
        String result1 = callFixLegacyFunctionCalls(input1);
        assertTrue(result1.contains("display:;"));

        // style在后
        String input2 = "<span class=\"highlight\" style=\"display:none\">文本</span>";
        String result2 = callFixLegacyFunctionCalls(input2);
        assertTrue(result2.contains("display:;"));
    }

    // ========== 第七部分：回归测试（确保不影响其他功能）==========

    /**
     * TC-26: dmRef链接替换不受影响
     */
    @Test
    void testDisplayNone_DmRefReplacement() throws Exception {
        String input =
            "<a href=\"javascript:window.external.ShowDmRef('DMC-001')\">" +
            "<span style=\"display:none\">隐藏</span>" +
            "链接文本" +
            "</a>";

        String result = callFixLegacyFunctionCalls(input);

        // 验证dmRef替换正常
        assertTrue(result.contains("showDmRefInfo"),
            "dmRef链接应被替换");
        // 验证display:none处理正常
        assertTrue(result.contains("display:;"));
    }

    /**
     * TC-27: 多媒体调用替换不受影响
     */
    @Test
    void testDisplayNone_MultimediaReplacement() throws Exception {
        String input =
            "<div onclick=\"window.parent.showPicture('ICN-001')\">" +
            "<span style=\"display:none\">图标</span>" +
            "</div>";

        String result = callFixLegacyFunctionCalls(input);

        // 验证多媒体替换正常
        assertTrue(result.contains("showMultimediaInfo"));
        // 验证display:none处理正常
        assertTrue(result.contains("display:;"));
        // div的display:none不应被移除（如果有的话）
    }

    /**
     * TC-28: 遗留脚本清理不受影响
     */
    @Test
    void testDisplayNone_LegacyScriptRemoval() throws Exception {
        String input =
            "<script>var lessonPath = '/path';</script>" +
            "<span style=\"display:none\">文本</span>";

        String result = callFixLegacyFunctionCalls(input);

        // 验证脚本被清理
        assertFalse(result.contains("lessonPath"),
            "遗留脚本应被清理");
        // 验证display:none处理正常
        assertTrue(result.contains("display:;"));
    }
}
