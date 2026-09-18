package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

import org.junit.Test;
import java.lang.reflect.Method;

/**
 * 调试测试：查看 fixLegacyFunctionCalls 的实际输出
 */
public class DmXmlHelperLegacyScriptDebugTest {

    private String callFixLegacyFunctionCalls(String html) throws Exception {
        Method method = DmXmlHelper.class.getDeclaredMethod("fixLegacyFunctionCalls", String.class);
        method.setAccessible(true);
        return (String) method.invoke(null, html);
    }

    @Test
    public void debugOutput() throws Exception {
        String html = "<script>\n" +
                      "    var borderno=\"ICN-001\";\n" +
                      "    var control=document.getElementById(\"ICN-001\");\n" +
                      "    control.src = lessonPath+\"Manual/\"+lessonCode+\"/\"+\"ICN-001\";\n" +
                      "</script>";

        System.out.println("=== INPUT ===");
        System.out.println(html);
        System.out.println("\n=== OUTPUT ===");
        String result = callFixLegacyFunctionCalls(html);
        System.out.println(result);
        System.out.println("\n=== CONTAINS lessonCode? ===");
        System.out.println(result.contains("lessonCode"));
    }
}
