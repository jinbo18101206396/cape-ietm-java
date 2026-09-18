package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

import org.junit.Test;
import java.lang.reflect.Method;
import static org.junit.Assert.*;

/**
 * 测试贪婪匹配问题：多个script标签时是否会误删中间内容
 */
public class DmXmlHelperGreedyTest {

    private String callFixLegacyFunctionCalls(String html) throws Exception {
        Method method = DmXmlHelper.class.getDeclaredMethod("fixLegacyFunctionCalls", String.class);
        method.setAccessible(true);
        return (String) method.invoke(null, html);
    }

    @Test
    public void testMultipleScriptsWithContentBetween() throws Exception {
        // 模拟真实HTML：两个遗留脚本之间有重要内容
        String html = "<div class=\"content\">" +
                      "<p>重要段落1</p>" +
                      "<script>control.src = lessonPath + \"test1\";</script>" +
                      "<h2>重要标题</h2>" +
                      "<p>重要段落2</p>" +
                      "<table><tr><td>重要表格</td></tr></table>" +
                      "<script>control.src = lessonCode + \"test2\";</script>" +
                      "<p>重要段落3</p>" +
                      "</div>";

        System.out.println("=== INPUT ===");
        System.out.println(html);

        String result = callFixLegacyFunctionCalls(html);

        System.out.println("\n=== OUTPUT ===");
        System.out.println(result);

        // 验证：中间的重要内容应该保留
        assertTrue("段落1应该保留", result.contains("重要段落1"));
        assertTrue("标题应该保留", result.contains("重要标题"));
        assertTrue("段落2应该保留", result.contains("重要段落2"));
        assertTrue("表格应该保留", result.contains("重要表格"));
        assertTrue("段落3应该保留", result.contains("重要段落3"));

        // 验证：遗留脚本应该被移除
        assertFalse("lessonPath脚本应该被移除", result.contains("lessonPath"));
        assertFalse("lessonCode脚本应该被移除", result.contains("lessonCode"));
    }
}
