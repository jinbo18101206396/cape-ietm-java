package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * fixLegacyFunctionCalls() 修复验证测试
 *
 * BUG修复: display:none全局替换改为选择性替换
 * - 只处理内联元素(span/emphasis/strong/em)的display:none
 * - 保留容器元素(div等)的display:none，用于UI控制
 *
 * 使用反射直接测试fixLegacyFunctionCalls方法，避免XSLT转换问题
 */
class DmXmlHelperFixLegacyTest {

    @Test
    @DisplayName("修复前：内联元素的display:none应该被移除")
    void testInlineElementDisplayNoneRemoved() {
        String input = "<span style=\"display:none\">Legacy hidden content</span>";
        String result = invokeFixLegacyFunctionCalls(input);

        // 内联元素的display:none应该被移除
        assertFalse(result.contains("<span style=\"display:none\""),
            "span元素的display:none应该被移除");
        assertTrue(result.contains("display:;"),
            "span元素应该包含display:;（已移除none）");
    }

    @Test
    @DisplayName("修复后：容器元素的display:none应该被保留")
    void testContainerElementDisplayNonePreserved() {
        String input = "<div id=\"wcnDiv\" style=\"display:none\">Warning panel</div>";
        String result = invokeFixLegacyFunctionCalls(input);

        // 容器元素的display:none应该被保留
        assertTrue(result.contains("display:none") || result.contains("display: none"),
            "div元素的display:none应该被保留（用于UI控制）");
    }

    @Test
    @DisplayName("修复后：警告面板的display:none应该被保留（base.xsl场景）")
    void testWarningPanelPreserved() {
        // 模拟base.xsl生成的警告面板HTML
        String input = "<div id=\"wcnDiv\" class=\"dmview\" style=\"display: none\">" +
                       "<h3>Warning and Caution</h3>" +
                       "<div class=\"warning\">Important warning</div>" +
                       "</div>";
        String result = invokeFixLegacyFunctionCalls(input);

        assertTrue(result.contains("display") && result.contains("none"),
            "警告面板的display:none应该被保留");
    }

    @Test
    @DisplayName("修复后：故障隔离步骤的display:none应该被保留（fault.xsl场景）")
    void testFaultStepsPreserved() {
        // 模拟fault.xsl生成的故障步骤HTML
        String input = "<div class=\"faultStep\" style=\"display:none;text-align: center;\">" +
                       "<p>Step 2: Check voltage</p>" +
                       "</div>" +
                       "<div class=\"faultStep\" style=\"display:none;\">" +
                       "<p>Step 3: Replace component</p>" +
                       "</div>";
        String result = invokeFixLegacyFunctionCalls(input);

        // 故障步骤的display:none应该被保留（用于逐步引导）
        int noneCount = countOccurrences(result, "display:none") + countOccurrences(result, "display: none");
        assertTrue(noneCount >= 2,
            "两个故障步骤的display:none都应该保留（实际: " + noneCount + "）");
    }

    @Test
    @DisplayName("修复后：emphasis元素的display:none应该被移除")
    void testEmphasisDisplayNoneRemoved() {
        String input = "<emphasis emphasisType=\"em01\" style=\"display:none\">Hidden emphasis</emphasis>";
        String result = invokeFixLegacyFunctionCalls(input);

        assertFalse(result.contains("display:none"),
            "emphasis元素的display:none应该被移除");
        assertTrue(result.contains("display:;"),
            "emphasis元素应该包含display:;");
    }

    @Test
    @DisplayName("修复后：strong/em元素的display:none应该被移除")
    void testStrongEmDisplayNoneRemoved() {
        String input = "<strong style=\"display:none\">Strong</strong>" +
                       "<em style=\"display:none\">Emphasis</em>";
        String result = invokeFixLegacyFunctionCalls(input);

        // strong和em的display:none应该被移除
        assertFalse(result.contains("display:none"),
            "strong和em元素的display:none应该被移除");
        assertTrue(result.contains("display:;"),
            "应该包含display:;（替换后的值）");
    }

    @Test
    @DisplayName("混合场景：内联元素移除，容器元素保留")
    void testMixedScenario() {
        String input = "<span style=\"display:none\">Hidden span</span>" +
                       "<div id=\"panel\" style=\"display:none\">UI panel</div>" +
                       "<emphasis style=\"display:none\">Hidden emphasis</emphasis>";
        String result = invokeFixLegacyFunctionCalls(input);

        // span和emphasis的display:none应该被移除
        assertTrue(result.contains("display:;"),
            "span和emphasis的display:none应该被替换成display:;");

        // div的display:none应该被保留
        assertTrue(result.contains("display:none") || result.contains("display: none"),
            "div的display:none应该被保留");

        // 验证div的none被保留，但span/emphasis的被移除
        String divPart = result.substring(result.indexOf("<div"));
        assertTrue(divPart.contains("display:none") || divPart.contains("display: none"),
            "div部分应该包含display:none");
    }

    @Test
    @DisplayName("回归测试：旧函数调用仍然正常替换")
    void testLegacyFunctionsStillReplaced() {
        String input = "<a onclick=\"window.external.ShowDmRef('DMC-001', 'para1')\">Link</a>" +
                       "<img onclick=\"window.parent.showPicture('ICN-001')\"/>";
        String result = invokeFixLegacyFunctionCalls(input);

        assertFalse(result.contains("window.external.ShowDmRef"),
            "ShowDmRef应该被替换");
        assertFalse(result.contains("window.parent.showPicture"),
            "showPicture应该被替换");
        assertTrue(result.contains("showDmRefInfo"),
            "应该包含showDmRefInfo");
        assertTrue(result.contains("showMultimediaInfo"),
            "应该包含showMultimediaInfo");
    }

    @Test
    @DisplayName("边界测试：display:none带多个空格")
    void testDisplayNoneWithSpaces() {
        String input = "<span style=\"display:   none  \">Spaces</span>";
        String result = invokeFixLegacyFunctionCalls(input);

        assertFalse(result.contains("display:   none"),
            "带空格的display:none应该被处理");
        assertTrue(result.contains("display:;"),
            "应该被替换成display:;");
    }

    @Test
    @DisplayName("边界测试：display:none without semicolon")
    void testDisplayNoneWithoutSemicolon() {
        String input = "<span style=\"color:red;display:none\">No semicolon</span>";
        String result = invokeFixLegacyFunctionCalls(input);

        assertFalse(result.contains("display:none\""),
            "无分号的display:none应该被处理");
        assertTrue(result.contains("display:;"),
            "应该被替换成display:;");
    }

    @Test
    @DisplayName("词边界测试：<embed>不应被em分支误匹配")
    void testEmbedNotMatchedByEmBranch() {
        // <embed>以em开头，但不是em元素，其display:none应该保留（当作容器元素处理）
        String input = "<embed style=\"display:none\" src=\"movie.swf\"/>";
        String result = invokeFixLegacyFunctionCalls(input);

        // embed是容器/替换元素，display:none应该保留（不应被em分支误匹配）
        assertTrue(result.contains("display:none"),
            "embed的display:none应该保留（不应被em分支误匹配）");
        assertFalse(result.contains("display:;"),
            "embed不应该被替换成display:;");
    }

    @Test
    @DisplayName("词边界测试：<embed>与真实<em>混合场景")
    void testEmbedAndEmMixed() {
        String input = "<embed style=\"display:none\" src=\"a.swf\"/>" +
                       "<em style=\"display:none\">真实em元素</em>";
        String result = invokeFixLegacyFunctionCalls(input);

        // em元素应该被处理
        assertTrue(result.contains("display:;"),
            "真实em元素的display:none应该被移除");

        // embed应该保留display:none（提取embed标签验证）
        int embedStart = result.indexOf("<embed");
        int embedEnd = result.indexOf("/>", embedStart) + 2;
        String embedTag = result.substring(embedStart, embedEnd);
        assertTrue(embedTag.contains("display:none"),
            "embed标签应该保留display:none，实际: " + embedTag);
    }

    // ========== 辅助方法 ==========

    /**
     * 通过反射调用私有方法 fixLegacyFunctionCalls
     */
    private String invokeFixLegacyFunctionCalls(String html) {
        try {
            java.lang.reflect.Method method = DmXmlHelper.class.getDeclaredMethod("fixLegacyFunctionCalls", String.class);
            method.setAccessible(true);
            return (String) method.invoke(null, html);
        } catch (Exception e) {
            fail("反射调用失败: " + e.getMessage());
            return null;
        }
    }

    private int countOccurrences(String str, String substring) {
        int count = 0;
        int index = 0;
        while ((index = str.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
    }
}
