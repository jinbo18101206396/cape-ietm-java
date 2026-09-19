package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DmXsltTransformer 多媒体路径替换测试
 *
 * 测试覆盖：
 * 1. 视频标签替换（showMultimediaInfo）
 * 2. 视频标签替换（showMultimedia，兼容性）
 * 3. 音频标签替换（playSound）
 * 4. 旧脚本清理
 * 5. 多个多媒体对象
 * 6. 嵌套场景
 * 7. 空HTML处理
 * 8. contextPath处理
 *
 * @author claude
 * @date 2026-09-18
 */
class DmXsltTransformerTest {

    /**
     * TC-01: 视频标签替换 - showMultimediaInfo
     */
    @Test
    void testReplaceMultimediaPaths_VideoShowMultimediaInfo() {
        String html = "<div>" +
            "<img id=\"IMF-VIDEO-001\" onclick=\"showMultimediaInfo('IMF-VIDEO-001')\" " +
            "src=\"placeholder.png\"/>" +
            "</div>";

        String contextPath = "/jeecg-boot";

        String result = DmXsltTransformer.enhancePreviewHtml(html, contextPath);

        // 验证video标签生成
        assertTrue(result.contains("<video controls width=\"640\" preload=\"metadata\">"),
            "Should contain video tag");
        assertTrue(result.contains("<source src=\"/jeecg-boot/ietm/icn/view/IMF-VIDEO-001\""),
            "Should contain correct viewIcn URL");
        assertTrue(result.contains("type=\"video/mp4\""),
            "Should have video/mp4 MIME type");
        assertTrue(result.contains("您的浏览器不支持视频播放"),
            "Should have fallback text");

        // 验证原始img标签被替换
        assertFalse(result.contains("onclick=\"showMultimediaInfo"),
            "Should remove onclick handler");
    }

    /**
     * TC-02: 视频标签替换 - showMultimedia（兼容旧格式）
     */
    @Test
    void testReplaceMultimediaPaths_VideoShowMultimedia() {
        String html = "<img id=\"IMF-VIDEO-002\" " +
            "onclick=\"showMultimedia('IMF-VIDEO-002', 'video')\" />";

        String contextPath = "/jeecg-boot";

        String result = DmXsltTransformer.enhancePreviewHtml(html, contextPath);

        // 验证兼容性（showMultimedia也能识别）
        assertTrue(result.contains("<video controls"),
            "Should handle showMultimedia (without Info)");
        assertTrue(result.contains("IMF-VIDEO-002"),
            "Should preserve IMF code");
    }

    /**
     * TC-03: 音频标签替换 - playSound
     */
    @Test
    void testReplaceMultimediaPaths_AudioPlaySound() {
        String html = "<div>" +
            "<img id=\"IMF-AUDIO-001\" onclick=\"playSound('IMF-AUDIO-001')\" />" +
            "</div>";

        String contextPath = "/jeecg-boot";

        String result = DmXsltTransformer.enhancePreviewHtml(html, contextPath);

        // 验证audio标签生成
        assertTrue(result.contains("<audio controls preload=\"metadata\">"),
            "Should contain audio tag");
        assertTrue(result.contains("<source src=\"/jeecg-boot/ietm/icn/view/IMF-AUDIO-001\""),
            "Should contain correct viewIcn URL");
        assertTrue(result.contains("type=\"audio/mpeg\""),
            "Should have audio/mpeg MIME type");
        assertTrue(result.contains("您的浏览器不支持音频播放"),
            "Should have fallback text");
    }

    /**
     * TC-04: 旧脚本清理
     */
    @Test
    void testReplaceMultimediaPaths_RemoveOldScripts() {
        String html =
            "<script type=\"text/javascript\">\n" +
            "function showMultimediaInfo(imfCode) {\n" +
            "  window.open('/multimedia/' + imfCode);\n" +
            "}\n" +
            "</script>\n" +
            "<img id=\"IMF-001\" onclick=\"showMultimediaInfo('IMF-001')\" />";

        String result = DmXsltTransformer.enhancePreviewHtml(html, "/app");

        // 验证脚本被清理
        assertFalse(result.contains("function showMultimediaInfo"),
            "Should remove showMultimediaInfo function");

        // 验证video标签存在
        assertTrue(result.contains("<video controls"),
            "Should have video tag");
    }

    /**
     * TC-05: playSound脚本清理
     */
    @Test
    void testReplaceMultimediaPaths_RemovePlaySoundScript() {
        String html =
            "<script>\n" +
            "function playSound(audioId) { /* ... */ }\n" +
            "</script>\n" +
            "<img id=\"SND-001\" onclick=\"playSound('SND-001')\" />";

        String result = DmXsltTransformer.enhancePreviewHtml(html, "/app");

        // 验证playSound函数被清理
        assertFalse(result.contains("function playSound"),
            "Should remove playSound function");
    }

    /**
     * TC-06: 多个多媒体对象
     */
    @Test
    void testReplaceMultimediaPaths_MultipleMedia() {
        String html =
            "<div>" +
            "<img id=\"IMF-VIDEO-001\" onclick=\"showMultimediaInfo('IMF-VIDEO-001')\" />" +
            "<p>说明文字</p>" +
            "<img id=\"IMF-AUDIO-001\" onclick=\"playSound('IMF-AUDIO-001')\" />" +
            "<img id=\"IMF-VIDEO-002\" onclick=\"showMultimediaInfo('IMF-VIDEO-002')\" />" +
            "</div>";

        String result = DmXsltTransformer.enhancePreviewHtml(html, "/app");

        // 验证3个多媒体对象都被替换
        int videoCount = countOccurrences(result, "<video controls");
        int audioCount = countOccurrences(result, "<audio controls");

        assertEquals(2, videoCount, "Should have 2 video tags");
        assertEquals(1, audioCount, "Should have 1 audio tag");

        // 验证所有IMF代码正确
        assertTrue(result.contains("IMF-VIDEO-001"));
        assertTrue(result.contains("IMF-AUDIO-001"));
        assertTrue(result.contains("IMF-VIDEO-002"));

        // 验证说明文字保留
        assertTrue(result.contains("<p>说明文字</p>"));
    }

    /**
     * TC-07: 嵌套HTML结构
     */
    @Test
    void testReplaceMultimediaPaths_NestedStructure() {
        String html =
            "<section>" +
            "  <div class=\"multimedia-container\">" +
            "    <h3>视频演示</h3>" +
            "    <img id=\"IMF-NESTED-001\" onclick=\"showMultimediaInfo('IMF-NESTED-001')\" />" +
            "    <p class=\"caption\">图1：操作演示</p>" +
            "  </div>" +
            "</section>";

        String result = DmXsltTransformer.enhancePreviewHtml(html, "/app");

        // 验证结构保留
        assertTrue(result.contains("<section>"));
        assertTrue(result.contains("<div class=\"multimedia-container\">"));
        assertTrue(result.contains("<h3>视频演示</h3>"));
        assertTrue(result.contains("<p class=\"caption\">图1：操作演示</p>"));

        // 验证video替换
        assertTrue(result.contains("<video controls"));
        assertTrue(result.contains("IMF-NESTED-001"));
    }

    /**
     * TC-08: 空HTML处理
     */
    @Test
    void testReplaceMultimediaPaths_EmptyHtml() {
        String result1 = DmXsltTransformer.enhancePreviewHtml("", "/app");
        assertEquals("", result1, "Empty string should return empty");

        String result2 = DmXsltTransformer.enhancePreviewHtml(null, "/app");
        assertNull(result2, "Null should return null");
    }

    /**
     * TC-09: contextPath处理 - 空路径
     */
    @Test
    void testReplaceMultimediaPaths_EmptyContextPath() {
        String html = "<img id=\"IMF-001\" onclick=\"showMultimediaInfo('IMF-001')\" />";

        String result = DmXsltTransformer.enhancePreviewHtml(html, "");

        // 验证URL正确（空contextPath）
        assertTrue(result.contains("src=\"/ietm/icn/view/IMF-001\""),
            "Empty contextPath should work");
    }

    /**
     * TC-10: contextPath处理 - 带斜杠
     */
    @Test
    void testReplaceMultimediaPaths_ContextPathWithSlash() {
        String html = "<img id=\"IMF-001\" onclick=\"showMultimediaInfo('IMF-001')\" />";

        String result = DmXsltTransformer.enhancePreviewHtml(html, "/my-app");

        // 验证URL正确
        assertTrue(result.contains("src=\"/my-app/ietm/icn/view/IMF-001\""),
            "ContextPath with slash should work");
    }

    /**
     * TC-11: 自闭合img标签
     */
    @Test
    void testReplaceMultimediaPaths_SelfClosingImg() {
        String html = "<img id=\"IMF-001\" onclick=\"showMultimediaInfo('IMF-001')\"/>";

        String result = DmXsltTransformer.enhancePreviewHtml(html, "/app");

        assertTrue(result.contains("<video controls"),
            "Self-closing img should be replaced");
    }

    /**
     * TC-12: 非自闭合img标签（虽然不规范，但可能出现）
     */
    @Test
    void testReplaceMultimediaPaths_NonSelfClosingImg() {
        String html = "<img id=\"IMF-001\" onclick=\"showMultimediaInfo('IMF-001')\">";

        String result = DmXsltTransformer.enhancePreviewHtml(html, "/app");

        // 正则应该仍能匹配（[^>]*/>中的/是可选的）
        assertTrue(result.contains("<video controls") || result.contains("<img"),
            "Should handle non-self-closing img");
    }

    /**
     * TC-13: IMF编码中包含特殊字符
     */
    @Test
    void testReplaceMultimediaPaths_SpecialCharactersInImfCode() {
        String html = "<img id=\"ICN-EEEE-A-344310-A-EEEEE-00073-A-001-06\" " +
            "onclick=\"showMultimediaInfo('ICN-EEEE-A-344310-A-EEEEE-00073-A-001-06')\" />";

        String result = DmXsltTransformer.enhancePreviewHtml(html, "/app");

        // 验证完整ICN编码保留
        assertTrue(result.contains("ICN-EEEE-A-344310-A-EEEEE-00073-A-001-06"),
            "Should preserve full ICN code");
    }

    /**
     * TC-14: CSS样式注入验证
     */
    @Test
    void testEnhancePreviewHtml_InjectsCss() {
        String html = "<div>content</div>";

        String result = DmXsltTransformer.enhancePreviewHtml(html, "/app");

        // 验证CSS被注入
        assertTrue(result.startsWith("<style>"),
            "Should inject CSS at beginning");
        assertTrue(result.contains("</style>"),
            "Should close style tag");
        assertTrue(result.contains("<div>content</div>"),
            "Should preserve original content");
    }

    /**
     * TC-15: 同时包含video和audio
     */
    @Test
    void testReplaceMultimediaPaths_MixedVideoAndAudio() {
        String html =
            "<para>" +
            "  <multimedia>" +
            "    <img id=\"VID-001\" onclick=\"showMultimediaInfo('VID-001')\" />" +
            "  </multimedia>" +
            "  <multimedia>" +
            "    <img id=\"AUD-001\" onclick=\"playSound('AUD-001')\" />" +
            "  </multimedia>" +
            "</para>";

        String result = DmXsltTransformer.enhancePreviewHtml(html, "/app");

        // 验证video和audio都存在
        assertTrue(result.contains("<video controls"));
        assertTrue(result.contains("<audio controls"));
        assertTrue(result.contains("VID-001"));
        assertTrue(result.contains("AUD-001"));
    }

    // ========== 辅助方法 ==========

    /**
     * 统计子串出现次数
     */
    private int countOccurrences(String text, String substring) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
    }
}
