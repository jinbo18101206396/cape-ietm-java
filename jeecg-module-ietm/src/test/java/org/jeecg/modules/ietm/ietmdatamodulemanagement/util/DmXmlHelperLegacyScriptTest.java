package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

import org.junit.Test;
import java.lang.reflect.Method;
import static org.junit.Assert.*;

/**
 * 测试 DmXmlHelper.fixLegacyFunctionCalls() 对遗留脚本的清理
 *
 * 背景：旧IETM系统的XSLT模板（symbols.xsl/multimedia.xsl/fig_tabFigure.xsl）
 * 生成的HTML包含引用未定义变量（lessonPath/lessonCode）的<script>标签
 *
 * 问题：新系统浏览器控制台报错 "Uncaught ReferenceError: lessonCode is not defined"
 *
 * 修复：在fixLegacyFunctionCalls()中添加正则表达式，移除这些遗留脚本标签
 */
public class DmXmlHelperLegacyScriptTest {

    /**
     * 使用反射调用私有方法 fixLegacyFunctionCalls
     */
    private String callFixLegacyFunctionCalls(String html) throws Exception {
        Method method = DmXmlHelper.class.getDeclaredMethod("fixLegacyFunctionCalls", String.class);
        method.setAccessible(true);
        return (String) method.invoke(null, html);
    }

    @Test
    public void testRemoveLegacyScript_symbolsXsl() throws Exception {
        // symbols.xsl 生成的脚本（第48行）
        String html = "<img class=\"figureLinkGraphic\" id=\"ICN-001\" boardno=\"ICN-001\"/>" +
                      "<script>\n" +
                      "    var borderno=\"ICN-001\";\n" +
                      "    var ext = borderno.substring(borderno.length-4,borderno.length).toUpperCase();\n" +
                      "    var control=document.getElementById(\"ICN-001\");\n" +
                      "    control.src = lessonPath+\"Manual/\"+lessonCode+\"/\"+\"ICN-001\";\n" +
                      "</script>";

        String result = callFixLegacyFunctionCalls(html);

        // 验证：脚本标签应该被完全移除
        assertFalse("应该移除包含lessonCode的script标签", result.contains("lessonCode"));
        assertFalse("应该移除包含lessonPath的script标签", result.contains("lessonPath"));
        assertFalse("script标签应该被完全移除", result.contains("<script>"));
        assertTrue("img标签应该保留", result.contains("<img"));
        assertTrue("boardno属性应该保留", result.contains("boardno=\"ICN-001\""));
    }

    @Test
    public void testRemoveLegacyScript_multimediaXsl_audio() throws Exception {
        // multimedia.xsl 生成的脚本（第81行）
        String html = "<img id=\"AUDIO-001\" class=\"multimedia\"/>" +
                      "<script>\n" +
                      "    document.getElementById(\"AUDIO-001\").src = lessonPath+\"images/audio.gif\";\n" +
                      "</script>";

        String result = callFixLegacyFunctionCalls(html);

        assertFalse("应该移除包含lessonPath的audio脚本", result.contains("lessonPath"));
        assertFalse("应该移除audio.gif引用", result.contains("audio.gif"));
        assertTrue("img标签应该保留", result.contains("id=\"AUDIO-001\""));
    }

    @Test
    public void testRemoveLegacyScript_multimediaXsl_video() throws Exception {
        // multimedia.xsl 生成的脚本（第111行）
        String html = "<img id=\"VIDEO-001\" class=\"multimedia\"/>" +
                      "<script>\n" +
                      "    document.getElementById(\"VIDEO-001\").src = lessonPath+\"images/video.gif\";\n" +
                      "</script>";

        String result = callFixLegacyFunctionCalls(html);

        assertFalse("应该移除包含lessonPath的video脚本", result.contains("lessonPath"));
        assertFalse("应该移除video.gif引用", result.contains("video.gif"));
    }

    @Test
    public void testRemoveLegacyScript_figTabFigureXsl_cgm() throws Exception {
        // fig_tabFigure.xsl 生成的脚本（第78行）
        String html = "<img id=\"FIG-001\" class=\"figure\"/>" +
                      "<script>\n" +
                      "    control.src = lessonPath+\"image/cgm.gif\";\n" +
                      "</script>";

        String result = callFixLegacyFunctionCalls(html);

        assertFalse("应该移除包含lessonPath的cgm脚本", result.contains("lessonPath"));
        assertFalse("应该移除cgm.gif引用", result.contains("cgm.gif"));
    }

    @Test
    public void testKeepNormalScripts() throws Exception {
        // 正常的脚本（不包含lessonPath/lessonCode）应该保留
        String html = "<img id=\"IMG-001\"/>" +
                      "<script>\n" +
                      "    document.getElementById(\"IMG-001\").onclick = function() { alert('clicked'); };\n" +
                      "</script>";

        String result = callFixLegacyFunctionCalls(html);

        // 验证：正常脚本应该完整保留
        assertTrue("正常脚本应该保留", result.contains("<script>"));
        assertTrue("正常脚本内容应该保留", result.contains("alert('clicked')"));
    }

    @Test
    public void testMultipleLegacyScripts() throws Exception {
        // 多个遗留脚本同时存在
        String html = "<img id=\"ICN-001\"/>" +
                      "<script>control.src = lessonPath+\"Manual/\"+lessonCode+\"/\"+\"ICN-001\";</script>" +
                      "<img id=\"AUDIO-001\"/>" +
                      "<script>document.getElementById(\"AUDIO-001\").src = lessonPath+\"images/audio.gif\";</script>" +
                      "<div>正常内容</div>" +
                      "<script>console.log('normal script');</script>";

        String result = callFixLegacyFunctionCalls(html);

        // 验证：遗留脚本全部移除，正常内容保留
        assertFalse("所有lessonCode引用应该被移除", result.contains("lessonCode"));
        assertFalse("所有lessonPath引用应该被移除", result.contains("lessonPath"));
        assertTrue("正常div内容应该保留", result.contains("<div>正常内容</div>"));
        assertTrue("正常脚本应该保留", result.contains("console.log('normal script')"));
    }

    @Test
    public void testEmptyAndNullInput() throws Exception {
        // 边界条件测试
        assertNull("null输入应返回null", callFixLegacyFunctionCalls(null));
        assertEquals("空字符串应返回空字符串", "", callFixLegacyFunctionCalls(""));
    }

    @Test
    public void testScriptWithWhitespace() throws Exception {
        // 脚本标签包含各种空白字符
        String html = "<script  type=\"text/javascript\"  >\n" +
                      "    var x = lessonPath + \"test\";\n" +
                      "    var y = lessonCode;\n" +
                      "</script>";

        String result = callFixLegacyFunctionCalls(html);

        assertFalse("应该移除包含空白字符的遗留脚本", result.contains("lessonPath"));
        assertFalse("应该移除包含空白字符的遗留脚本", result.contains("lessonCode"));
    }

    @Test
    public void testDmRefAndMultimediaCallsStillWork() throws Exception {
        // 验证其他功能（dmRef/multimedia调用替换）仍然正常工作
        String html = "<a onclick=\"window.external.ShowDmRef('DMC-001')\">Link1</a>" +
                      "<a onclick=\"window.parent.showPicture('ICN-001')\">Link2</a>" +
                      "<script>control.src = lessonPath + lessonCode;</script>";

        String result = callFixLegacyFunctionCalls(html);

        // 验证：dmRef/multimedia调用被正确替换
        assertTrue("ShowDmRef应该被替换为showDmRefInfo", result.contains("showDmRefInfo('DMC-001')"));
        assertTrue("showPicture应该被替换为showMultimediaInfo", result.contains("showMultimediaInfo('ICN-001')"));
        // 遗留脚本被移除
        assertFalse("遗留脚本应该被移除", result.contains("lessonPath"));
        assertFalse("遗留脚本应该被移除", result.contains("lessonCode"));
    }
}
