package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.lang.reflect.Method;

/**
 * DmXmlHelper.fixLegacyFunctionCalls 方法单元测试
 *
 * 测试目标: 验证修复后的 LEGACY_SCRIPT_PATTERN 正则表达式能够：
 * 1. 正确移除包含 lessonPath/lessonCode 的 script 标签
 * 2. 保留其他所有HTML结构（特别是 idstatus 和 dmview div）
 * 3. 处理各种边界情况
 *
 * 修复历史: 2026-09-18 修复了原正则 .*? 的贪婪匹配问题
 * Bug参考: docs/bugfix/2026-09-18-dm-preview-title-missing.md
 *
 * 注意: fixLegacyFunctionCalls 是 private 方法，使用反射调用测试
 *
 * @author IETM Team
 * @date 2026-09-18
 */
public class DmXmlHelperFixLegacyTest {

    private Method fixLegacyMethod;

    @Before
    public void setUp() throws Exception {
        // 使用反射获取 private 方法
        fixLegacyMethod = DmXmlHelper.class.getDeclaredMethod("fixLegacyFunctionCalls", String.class);
        fixLegacyMethod.setAccessible(true);
    }

    /**
     * 调用 private 方法的辅助方法
     */
    private String fixLegacy(String html) throws Exception {
        return (String) fixLegacyMethod.invoke(null, html);
    }

    /**
     * 测试1: 基础功能 - 移除包含 lessonPath 的单个 script 标签
     */
    @Test
    public void testRemoveSingleScriptWithLessonPath() throws Exception {
        String input = "<div>保留内容</div><script>var path = lessonPath;</script><div>保留内容2</div>";
        String result = fixLegacy(input);

        assertFalse("应移除包含lessonPath的script", result.contains("lessonPath"));
        assertTrue("应保留第一个div", result.contains("保留内容"));
        assertTrue("应保留第二个div", result.contains("保留内容2"));
    }

    /**
     * 测试2: 基础功能 - 移除包含 lessonCode 的单个 script 标签
     */
    @Test
    public void testRemoveSingleScriptWithLessonCode() throws Exception {
        String input = "<div>保留内容</div><script>var code = lessonCode;</script><div>保留内容2</div>";
        String result = fixLegacy(input);

        assertFalse("应移除包含lessonCode的script", result.contains("lessonCode"));
        assertTrue("应保留第一个div", result.contains("保留内容"));
        assertTrue("应保留第二个div", result.contains("保留内容2"));
    }

    /**
     * 测试3: 关键场景 - 保留 idstatus 和 dmview div（核心修复验证）
     *
     * 这是修复的核心测试：原始bug会删除中间的所有HTML结构
     */
    @Test
    public void testPreserveIdstatusAndDmviewDivs() throws Exception {
        String input = "<script>var a = 1;</script>" +
                      "<div id=\"idstatus\">重要标题内容</div>" +
                      "<div id=\"dmview\">正文内容</div>" +
                      "<script>var b = lessonPath;</script>";

        String result = fixLegacy(input);

        assertTrue("必须保留idstatus div", result.contains("id=\"idstatus\""));
        assertTrue("必须保留idstatus内容", result.contains("重要标题内容"));
        assertTrue("必须保留dmview div", result.contains("id=\"dmview\""));
        assertTrue("必须保留dmview内容", result.contains("正文内容"));
        assertFalse("应移除包含lessonPath的script", result.contains("lessonPath"));
        assertTrue("应保留不含目标关键字的script", result.contains("var a = 1;"));
    }

    /**
     * 测试4: 边界情况 - 没有目标关键字的 script 标签应保留
     */
    @Test
    public void testPreserveScriptWithoutTargetKeywords() throws Exception {
        String input = "<script>var x = 123; console.log('test');</script>";
        String result = fixLegacy(input);

        assertEquals("不包含目标关键字的script应完全保留", input, result);
    }

    /**
     * 测试5: 边界情况 - 多个目标 script 标签都应被移除
     */
    @Test
    public void testRemoveMultipleTargetScripts() throws Exception {
        String input = "<script>a = lessonPath;</script>" +
                      "<div>中间内容</div>" +
                      "<script>b = lessonCode;</script>";

        String result = fixLegacy(input);

        assertFalse("应移除第一个script", result.contains("lessonPath"));
        assertFalse("应移除第二个script", result.contains("lessonCode"));
        assertTrue("应保留中间div", result.contains("中间内容"));
    }

    /**
     * 测试6: 边界情况 - script 标签带属性
     */
    @Test
    public void testScriptWithAttributes() throws Exception {
        String input = "<script type=\"text/javascript\" charset=\"utf-8\">var p = lessonPath;</script>";
        String result = fixLegacy(input);

        assertFalse("应移除带属性的目标script", result.contains("lessonPath"));
    }

    /**
     * 测试7: 边界情况 - 关键字在字符串中
     */
    @Test
    public void testKeywordInString() throws Exception {
        String input = "<script>var msg = \"使用lessonPath变量\";</script>";
        String result = fixLegacy(input);

        assertFalse("字符串中包含关键字也应被移除", result.contains("lessonPath"));
    }

    /**
     * 测试8: 边界情况 - 跨行的 script 标签
     */
    @Test
    public void testMultilineScript() throws Exception {
        String input = "<script>\n" +
                      "  var path = lessonPath;\n" +
                      "  console.log(path);\n" +
                      "</script>";

        String result = fixLegacy(input);

        assertFalse("应移除跨行的目标script", result.contains("lessonPath"));
    }

    /**
     * 测试9: 边界情况 - 空输入
     */
    @Test
    public void testEmptyInput() throws Exception {
        String result = fixLegacy("");
        assertEquals("空输入应返回空字符串", "", result);
    }

    /**
     * 测试10: 边界情况 - null 输入
     */
    @Test
    public void testNullInput() throws Exception {
        String result = fixLegacy(null);
        assertNull("null输入应返回null", result);
    }

    /**
     * 测试11: 性能测试 - 大HTML文档（模拟真实预览场景）
     */
    @Test
    public void testLargeHtmlPerformance() throws Exception {
        // 构建一个约250KB的HTML（接近实际预览HTML大小）
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");

        // 添加1000个正常div
        for (int i = 0; i < 1000; i++) {
            sb.append("<div class=\"content\">段落内容").append(i).append("</div>");
        }

        // 添加关键结构
        sb.append("<div id=\"idstatus\">重要标题</div>");
        sb.append("<div id=\"dmview\">正文内容</div>");

        // 添加目标script
        sb.append("<script>var legacy = lessonPath;</script>");

        sb.append("</body></html>");

        String input = sb.toString();

        // 执行测试
        long startTime = System.currentTimeMillis();
        String result = fixLegacy(input);
        long endTime = System.currentTimeMillis();

        // 验证结果
        assertTrue("必须保留idstatus", result.contains("id=\"idstatus\""));
        assertTrue("必须保留dmview", result.contains("id=\"dmview\""));
        assertFalse("必须移除lessonPath", result.contains("lessonPath"));

        // 性能断言：大HTML处理应在合理时间内完成（<100ms）
        long duration = endTime - startTime;
        assertTrue("处理250KB HTML应在100ms内完成，实际: " + duration + "ms",
                  duration < 100);
    }

    /**
     * 测试12: 真实场景 - 完整的DM预览HTML片段
     */
    @Test
    public void testRealDmPreviewHtml() throws Exception {
        String input = "<!DOCTYPE html>\n" +
                      "<html>\n" +
                      "<head>\n" +
                      "  <script>var init = true;</script>\n" +
                      "</head>\n" +
                      "<body>\n" +
                      "  <div id=\"idstatus\">\n" +
                      "    <div class=\"dm-title\">使用限制（总论）-符号清单1</div>\n" +
                      "    <div class=\"dm-code\">DMC-HAWJ7A-EAA-D00-00-00-00A-040A-A</div>\n" +
                      "  </div>\n" +
                      "  <div id=\"dmview\">\n" +
                      "    <div class=\"content\">DM正文内容</div>\n" +
                      "  </div>\n" +
                      "  <script>\n" +
                      "    // 旧系统遗留代码\n" +
                      "    var path = lessonPath;\n" +
                      "    var code = lessonCode;\n" +
                      "  </script>\n" +
                      "</body>\n" +
                      "</html>";

        String result = fixLegacy(input);

        // 验证关键内容都保留
        assertTrue("必须保留DOCTYPE", result.contains("<!DOCTYPE html>"));
        assertTrue("必须保留idstatus div", result.contains("id=\"idstatus\""));
        assertTrue("必须保留DM标题", result.contains("使用限制（总论）-符号清单1"));
        assertTrue("必须保留DMC编码", result.contains("DMC-HAWJ7A-EAA-D00-00-00-00A-040A-A"));
        assertTrue("必须保留dmview div", result.contains("id=\"dmview\""));
        assertTrue("必须保留正文内容", result.contains("DM正文内容"));
        assertTrue("必须保留普通script", result.contains("var init = true;"));

        // 验证遗留代码被移除
        assertFalse("必须移除lessonPath", result.contains("lessonPath"));
        assertFalse("必须移除lessonCode", result.contains("lessonCode"));
    }

    /**
     * 测试13: 边界情况 - script标签紧邻（无空白）
     */
    @Test
    public void testAdjacentScriptTags() throws Exception {
        String input = "<script>var a = 1;</script><script>var b = lessonPath;</script><script>var c = 3;</script>";
        String result = fixLegacy(input);

        assertTrue("应保留第一个script", result.contains("var a = 1;"));
        assertFalse("应移除第二个script", result.contains("lessonPath"));
        assertTrue("应保留第三个script", result.contains("var c = 3;"));
    }

    /**
     * 测试14: 边界情况 - 嵌套结构不受影响
     */
    @Test
    public void testNestedStructures() throws Exception {
        String input = "<div class=\"outer\">" +
                      "  <div class=\"inner\">" +
                      "    <script>var x = lessonPath;</script>" +
                      "    <div id=\"preserved\">重要内容</div>" +
                      "  </div>" +
                      "</div>";

        String result = fixLegacy(input);

        assertTrue("外层div应保留", result.contains("class=\"outer\""));
        assertTrue("内层div应保留", result.contains("class=\"inner\""));
        assertTrue("重要内容应保留", result.contains("id=\"preserved\""));
        assertTrue("重要内容文本应保留", result.contains("重要内容"));
        assertFalse("目标script应移除", result.contains("lessonPath"));
    }
}
