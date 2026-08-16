package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * DmXmlHelper 预览功能修复测试
 *
 * 验证 fixLegacyFunctionCalls() 方法是否正确处理旧系统遗留的函数调用
 *
 * @author Claude
 * @date 2026-08-14
 */
public class DmXmlHelperPreviewTest {

    @Test
    @DisplayName("TC-01: 替换 window.external.ShowDmRef")
    public void testFixLegacyFunctionCalls_windowExternalShowDmRef() {
        // 准备测试数据（模拟XSLT生成的HTML）
        String html = "<a onclick=\"window.external.ShowDmRef('DMC-SAMPLE-001', 'section1')\">查看引用DM</a>";

        // 调用私有方法（通过反射）
        String result = invokeFixLegacyFunctionCalls(html);

        // 验证结果
        assertNotNull(result, "返回值不应为null");
        assertTrue(result.contains("showDmRefInfo"), "应包含 showDmRefInfo");
        assertFalse(result.contains("window.external.ShowDmRef"), "不应包含 window.external.ShowDmRef");
        assertEquals("<a onclick=\"showDmRefInfo('DMC-SAMPLE-001', 'section1')\">查看引用DM</a>", result);
    }

    @Test
    @DisplayName("TC-02: 替换 window.parent.addShowContentPanel")
    public void testFixLegacyFunctionCalls_addShowContentPanel() {
        String html = "<a onclick=\"window.parent.addShowContentPanel('DMC-SAMPLE-002')\">打开内容面板</a>";

        String result = invokeFixLegacyFunctionCalls(html);

        assertTrue(result.contains("showDmRefInfo"), "应包含 showDmRefInfo");
        assertFalse(result.contains("window.parent.addShowContentPanel"), "不应包含 addShowContentPanel");
        assertEquals("<a onclick=\"showDmRefInfo('DMC-SAMPLE-002')\">打开内容面板</a>", result);
    }

    @Test
    @DisplayName("TC-03: 替换 window.parent.showPicture")
    public void testFixLegacyFunctionCalls_windowParentShowPicture() {
        String html = "<img onclick=\"window.parent.showPicture('ICN-SAMPLE-001')\" src=\"placeholder.gif\"/>";

        String result = invokeFixLegacyFunctionCalls(html);

        assertTrue(result.contains("showMultimediaInfo"), "应包含 showMultimediaInfo");
        assertFalse(result.contains("window.parent.showPicture"), "不应包含 window.parent.showPicture");
        assertEquals("<img onclick=\"showMultimediaInfo('ICN-SAMPLE-001')\" src=\"placeholder.gif\"/>", result);
    }

    @Test
    @DisplayName("TC-04: display:none处理 - 容器元素保留")
    public void testFixLegacyFunctionCalls_displayNoneWithSemicolon() {
        String html = "<div style=\"width:100%; display: none; color:red;\">隐藏元素</div>";

        String result = invokeFixLegacyFunctionCalls(html);

        // 修复后：div的display:none应该被保留（用于UI控制）
        assertTrue(result.contains("display: none"), "div的display:none应该保留");
        assertFalse(result.contains("display:;"), "div不应该被替换成display:;");
        assertTrue(result.contains("width:100%"), "其他样式应保留");
        assertTrue(result.contains("color:red"), "其他样式应保留");
    }

    @Test
    @DisplayName("TC-05: 移除 display:none (无分号)")
    public void testFixLegacyFunctionCalls_displayNoneWithoutSemicolon() {
        String html = "<span style=\"display:none\">隐藏文本</span>";

        String result = invokeFixLegacyFunctionCalls(html);

        assertTrue(result.contains("display:;"), "应包含 display:;");
        assertFalse(result.contains("display:none"), "不应包含 display:none");
    }

    @Test
    @DisplayName("TC-06: 混合场景 - 同时包含多种遗留调用")
    public void testFixLegacyFunctionCalls_mixedScenario() {
        String html =
            "<div style=\"display: none;\">" +
            "  <a onclick=\"window.external.ShowDmRef('DMC-001', '')\">DM引用</a>" +
            "  <img onclick=\"window.parent.showPicture('ICN-001')\"/>" +
            "  <a onclick=\"window.parent.addShowContentPanel('DMC-002')\">面板</a>" +
            "</div>";

        String result = invokeFixLegacyFunctionCalls(html);

        // 验证所有函数替换都生效
        assertTrue(result.contains("showDmRefInfo('DMC-001', '')"), "dmRef应被替换");
        assertTrue(result.contains("showMultimediaInfo('ICN-001')"), "图形应被替换");
        assertTrue(result.contains("showDmRefInfo('DMC-002')"), "面板应被替换");

        // div的display:none应该保留(容器元素不处理)
        assertTrue(result.contains("display: none"), "div的display:none应该保留");
        assertFalse(result.contains("display:;"), "div不应该被替换成display:;");

        assertFalse(result.contains("window.external"), "不应包含 window.external");
        assertFalse(result.contains("window.parent"), "不应包含 window.parent");
    }

    @Test
    @DisplayName("TC-07: 空HTML处理")
    public void testFixLegacyFunctionCalls_emptyHtml() {
        String result1 = invokeFixLegacyFunctionCalls(null);
        String result2 = invokeFixLegacyFunctionCalls("");

        assertNull(result1, "null应返回null");
        assertEquals("", result2, "空字符串应返回空字符串");
    }

    @Test
    @DisplayName("TC-08: 不含遗留调用的HTML应保持不变")
    public void testFixLegacyFunctionCalls_noLegacyCalls() {
        String html = "<div><p>正常内容</p><img src=\"test.png\"/></div>";

        String result = invokeFixLegacyFunctionCalls(html);

        assertEquals(html, result, "不含遗留调用的HTML应保持不变");
    }

    @Test
    @DisplayName("TC-09: 多个相同函数调用")
    public void testFixLegacyFunctionCalls_multipleIdenticalCalls() {
        String html =
            "<a onclick=\"window.external.ShowDmRef('DMC-001', 'sec1')\">Link1</a>" +
            "<a onclick=\"window.external.ShowDmRef('DMC-002', 'sec2')\">Link2</a>" +
            "<a onclick=\"window.external.ShowDmRef('DMC-003', 'sec3')\">Link3</a>";

        String result = invokeFixLegacyFunctionCalls(html);

        // 验证所有出现都被替换
        assertEquals(3, countOccurrences(result, "showDmRefInfo"), "应替换3处");
        assertEquals(0, countOccurrences(result, "window.external.ShowDmRef"), "不应包含原调用");
    }

    @Test
    @DisplayName("TC-10: display:none 多处出现 - 选择性替换")
    public void testFixLegacyFunctionCalls_multipleDisplayNone() {
        String html =
            "<div style=\"display: none;\">Div1</div>" +
            "<span style=\"color:blue; display:none; font-size:12px;\">Span1</span>" +
            "<p style=\"display:  none  ;\">Para1</p>";

        String result = invokeFixLegacyFunctionCalls(html);

        // 验证选择性替换：只有span的display:none被移除
        assertTrue(result.contains("color:blue"), "span的其他样式应保留");
        assertTrue(result.contains("font-size:12px"), "span的其他样式应保留");
        assertTrue(result.contains("display:;"), "span的display:none应被替换成display:;");

        // div和p的display:none应该保留(容器元素不处理)
        assertEquals(2, countOccurrences(result, "display: none") + countOccurrences(result, "display:  none"),
            "div和p的display:none应该保留");
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

    /**
     * 统计子字符串出现次数
     */
    private int countOccurrences(String str, String substr) {
        if (str == null || substr == null || substr.isEmpty()) {
            return 0;
        }
        int count = 0;
        int index = 0;
        while ((index = str.indexOf(substr, index)) != -1) {
            count++;
            index += substr.length();
        }
        return count;
    }
}
