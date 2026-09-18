package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

import org.junit.Test;
import java.lang.reflect.Method;
import static org.junit.Assert.*;

/**
 * 测试P1-1修复：嵌套script标签递归清理
 */
public class DmXmlHelperNestedScriptTest {

    private String callFixLegacyFunctionCalls(String html) throws Exception {
        Method method = DmXmlHelper.class.getDeclaredMethod("fixLegacyFunctionCalls", String.class);
        method.setAccessible(true);
        return (String) method.invoke(null, html);
    }

    @Test
    public void testNestedScript_SingleLevel() throws Exception {
        // 单层嵌套
        String html = "<div>" +
                      "<script><script>var x = lessonPath;</script></script>" +
                      "<p>内容</p>" +
                      "</div>";

        String result = callFixLegacyFunctionCalls(html);

        System.out.println("=== INPUT ===");
        System.out.println(html);
        System.out.println("\n=== OUTPUT ===");
        System.out.println(result);

        // 验证：不应残留任何script标签
        assertFalse("不应残留<script>开始标签", result.contains("<script>"));
        assertFalse("不应残留</script>闭合标签", result.contains("</script>"));
        assertFalse("不应残留lessonPath", result.contains("lessonPath"));
        assertTrue("正常内容应该保留", result.contains("<p>内容</p>"));
    }

    @Test
    public void testNestedScript_DoubleLevel() throws Exception {
        // 双层嵌套
        String html = "<script><script><script>lessonCode</script></script></script>";

        String result = callFixLegacyFunctionCalls(html);

        System.out.println("=== INPUT ===");
        System.out.println(html);
        System.out.println("\n=== OUTPUT ===");
        System.out.println(result);

        assertFalse("不应残留script标签", result.contains("<script>"));
        assertFalse("不应残留script闭合标签", result.contains("</script>"));
        assertEquals("应完全清除为空字符串", "", result);
    }

    @Test
    public void testNestedScript_WithContentBetween() throws Exception {
        // 嵌套script之间有正常内容
        String html = "<div>" +
                      "<p>段落1</p>" +
                      "<script>outer1<script>inner lessonPath</script>outer2</script>" +
                      "<h2>标题</h2>" +
                      "<script>var x = lessonCode;</script>" +
                      "<p>段落2</p>" +
                      "</div>";

        String result = callFixLegacyFunctionCalls(html);

        System.out.println("=== INPUT ===");
        System.out.println(html);
        System.out.println("\n=== OUTPUT ===");
        System.out.println(result);

        // 验证：正常内容保留，所有script清除
        assertTrue("段落1应该保留", result.contains("<p>段落1</p>"));
        assertTrue("标题应该保留", result.contains("<h2>标题</h2>"));
        assertTrue("段落2应该保留", result.contains("<p>段落2</p>"));
        assertFalse("不应残留script标签", result.contains("<script"));
        assertFalse("不应残留lessonPath", result.contains("lessonPath"));
        assertFalse("不应残留lessonCode", result.contains("lessonCode"));
    }

    @Test
    public void testRecursionLimit() throws Exception {
        // 验证递归有上限（防止无限循环）
        // 构造10层嵌套
        StringBuilder html = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            html.append("<script>");
        }
        html.append("lessonPath");
        for (int i = 0; i < 10; i++) {
            html.append("</script>");
        }

        String result = callFixLegacyFunctionCalls(html.toString());

        System.out.println("=== INPUT (10层嵌套) ===");
        System.out.println(html.toString());
        System.out.println("\n=== OUTPUT ===");
        System.out.println(result);

        // 验证：最终应该完全清除（10次迭代足够）
        assertEquals("10层嵌套应该在10次迭代内清除完毕", "", result);
    }

    @Test
    public void testMixedNormalAndLegacyScripts() throws Exception {
        // 正常脚本 + 嵌套遗留脚本混合
        String html = "<div>" +
                      "<script>console.log('normal script 1');</script>" +
                      "<script><script>var x = lessonPath;</script></script>" +
                      "<script>console.log('normal script 2');</script>" +
                      "<script>var y = lessonCode;</script>" +
                      "</div>";

        String result = callFixLegacyFunctionCalls(html);

        System.out.println("=== INPUT ===");
        System.out.println(html);
        System.out.println("\n=== OUTPUT ===");
        System.out.println(result);

        // 验证：正常脚本保留，遗留脚本清除
        assertTrue("正常脚本1应该保留", result.contains("console.log('normal script 1')"));
        assertTrue("正常脚本2应该保留", result.contains("console.log('normal script 2')"));
        assertFalse("遗留脚本应该清除", result.contains("lessonPath"));
        assertFalse("遗留脚本应该清除", result.contains("lessonCode"));
    }

    @Test
    public void testPerformance_NoInfiniteLoop() throws Exception {
        // 性能测试：确保不会陷入无限循环
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("<script>lessonPath</script>");
        }
        String html = sb.toString();

        long start = System.nanoTime();
        String result = callFixLegacyFunctionCalls(html);
        long elapsed = System.nanoTime() - start;

        System.out.println("=== 性能测试：100个遗留脚本 ===");
        System.out.println("耗时: " + (elapsed / 1_000_000.0) + " ms");

        assertTrue("应在100ms内完成", elapsed < 100_000_000); // 100ms
        assertEquals("所有遗留脚本应该清除", "", result);
    }
}
