package org.jeecg.modules.ietm.ietmdatamodulemanagement;

import org.jeecg.modules.ietm.ietmdatamodulemanagement.util.DmXsltTransformer;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 🔬 DM预览Symbol尺寸测试
 *
 * 测试symbol元素的尺寸属性处理（reproductionWidth/Height/Scale）
 * 符合S1000D 4.0 § 3.9.5.2.16标准
 *
 * @author Kiro
 * @date 2026-09-18
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DmPreviewSymbolSizeTest {

    /**
     * TC-S01: 用户实际XML - 1920x1080横向图片
     */
    @Test
    @Order(1)
    @DisplayName("TC-S01: 1920x1080横向symbol")
    public void testUserXml_Horizontal() throws Exception {
        String xml = buildXml(
            "<symbol infoEntityIdent=\"ICN-ZB1-A-020000-60101-30101-00017-A-001-01\" " +
            "reproductionWidth=\"1920\" reproductionHeight=\"1080\" reproductionScale=\"100\"/>"
        );

        String html = DmXsltTransformer.transform(xml, "S1000D40", "descript");

        // 应包含width和height属性
        assertTrue(html.contains("width=\"1920\""), "应包含width=\"1920\"");
        assertTrue(html.contains("height=\"1080\""), "应包含height=\"1080\"");

        // 验证img标签存在
        assertTrue(html.contains("<img"), "应生成img标签");
        assertTrue(html.contains("class=\"figureLinkGraphic\""), "应包含figureLinkGraphic类");
    }

    /**
     * TC-S02: 用户实际XML - 1440x1920竖向图片
     */
    @Test
    @Order(2)
    @DisplayName("TC-S02: 1440x1920竖向symbol")
    public void testUserXml_Vertical() throws Exception {
        String xml = buildXml(
            "<symbol infoEntityIdent=\"ICN-ZB1-A-020000-60101-30101-00016-A-001-01\" " +
            "reproductionWidth=\"1440\" reproductionHeight=\"1920\" reproductionScale=\"100\"/>"
        );

        String html = DmXsltTransformer.transform(xml, "S1000D40", "descript");

        // 应包含width和height属性
        assertTrue(html.contains("width=\"1440\""), "应包含width=\"1440\"");
        assertTrue(html.contains("height=\"1920\""), "应包含height=\"1920\"");
    }

    /**
     * TC-S03: 仅指定宽度
     */
    @Test
    @Order(3)
    @DisplayName("TC-S03: 仅宽度symbol")
    public void testWidthOnly() throws Exception {
        String xml = buildXml(
            "<symbol infoEntityIdent=\"ICN-TEST-001\" reproductionWidth=\"600px\"/>"
        );

        String html = DmXsltTransformer.transform(xml, "S1000D40", "descript");

        assertTrue(html.contains("width=\"600px\""), "应包含width=\"600px\"");
        assertFalse(html.matches(".*height=\"\\d+.*"), "不应有height属性（数字）");
    }

    /**
     * TC-S04: 仅指定高度
     */
    @Test
    @Order(4)
    @DisplayName("TC-S04: 仅高度symbol")
    public void testHeightOnly() throws Exception {
        String xml = buildXml(
            "<symbol infoEntityIdent=\"ICN-TEST-002\" reproductionHeight=\"400px\"/>"
        );

        String html = DmXsltTransformer.transform(xml, "S1000D40", "descript");

        assertTrue(html.contains("height=\"400px\""), "应包含height=\"400px\"");
        assertFalse(html.matches(".*width=\"\\d+px\".*"), "不应有width像素属性");
    }

    /**
     * TC-S05: 仅指定缩放比例
     */
    @Test
    @Order(5)
    @DisplayName("TC-S05: 仅缩放比例symbol")
    public void testScaleOnly() throws Exception {
        String xml = buildXml(
            "<symbol infoEntityIdent=\"ICN-TEST-003\" reproductionScale=\"75\"/>"
        );

        String html = DmXsltTransformer.transform(xml, "S1000D40", "descript");

        assertTrue(html.contains("width=\"75%\""), "应包含width=\"75%\"");
    }

    /**
     * TC-S06: 无尺寸属性（使用默认）
     */
    @Test
    @Order(6)
    @DisplayName("TC-S06: 无尺寸属性symbol")
    public void testNoSize() throws Exception {
        String xml = buildXml(
            "<symbol infoEntityIdent=\"ICN-TEST-004\"/>"
        );

        String html = DmXsltTransformer.transform(xml, "S1000D40", "descript");

        // 应有默认样式
        assertTrue(html.contains("max-width: 800px") || html.contains("style=\"max-width: 800px"),
                   "应包含默认max-width: 800px样式");
    }

    /**
     * TC-S07: 优先级测试 - Width/Height优先于Scale
     */
    @Test
    @Order(7)
    @DisplayName("TC-S07: Width/Height优先于Scale")
    public void testPriority() throws Exception {
        String xml = buildXml(
            "<symbol infoEntityIdent=\"ICN-TEST-005\" " +
            "reproductionWidth=\"500px\" reproductionHeight=\"300px\" reproductionScale=\"50\"/>"
        );

        String html = DmXsltTransformer.transform(xml, "S1000D40", "descript");

        // 应优先使用Width/Height
        assertTrue(html.contains("width=\"500px\""), "应包含width=\"500px\"");
        assertTrue(html.contains("height=\"300px\""), "应包含height=\"300px\"");
        assertFalse(html.contains("50%"), "不应包含scale百分比");
    }

    /**
     * TC-S08: 多个symbol一致性测试
     */
    @Test
    @Order(8)
    @DisplayName("TC-S08: 多symbol一致性")
    public void testMultipleSymbols() throws Exception {
        String xml = buildXml(
            "<symbol infoEntityIdent=\"ICN-1\" reproductionWidth=\"100px\"/>" +
            "<symbol infoEntityIdent=\"ICN-2\" reproductionWidth=\"200px\"/>" +
            "<symbol infoEntityIdent=\"ICN-3\" reproductionWidth=\"300px\"/>"
        );

        String html = DmXsltTransformer.transform(xml, "S1000D40", "descript");

        // 验证所有尺寸都正确
        int count100 = countOccurrences(html, "width=\"100px\"");
        int count200 = countOccurrences(html, "width=\"200px\"");
        int count300 = countOccurrences(html, "width=\"300px\"");

        assertEquals(1, count100, "应包含1个100px");
        assertEquals(1, count200, "应包含1个200px");
        assertEquals(1, count300, "应包含1个300px");
    }

    /**
     * TC-S09: 用户完整XML回归测试
     */
    @Test
    @Order(9)
    @DisplayName("TC-S09: 用户完整XML回归")
    public void testUserCompleteXml() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<dmodule xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
            "  <content>\n" +
            "    <description>\n" +
            "      <para>\n" +
            "        <symbol infoEntityIdent=\"ICN-ZB1-A-020000-60101-30101-00017-A-001-01\" " +
            "reproductionWidth=\"1920\" reproductionHeight=\"1080\" reproductionScale=\"100\"/>\n" +
            "        <symbol infoEntityIdent=\"ICN-ZB1-A-020000-60101-30101-00016-A-001-01\" " +
            "reproductionWidth=\"1440\" reproductionHeight=\"1920\" reproductionScale=\"100\"/>\n" +
            "      </para>\n" +
            "    </description>\n" +
            "  </content>\n" +
            "</dmodule>";

        String html = DmXsltTransformer.transform(xml, "S1000D40", "descript");

        // 验证两个图片都有正确尺寸
        assertTrue(html.contains("width=\"1920\""), "应包含第一个图片width=\"1920\"");
        assertTrue(html.contains("height=\"1080\""), "应包含第一个图片height=\"1080\"");
        assertTrue(html.contains("width=\"1440\""), "应包含第二个图片width=\"1440\"");
        assertTrue(html.contains("height=\"1920\""), "应包含第二个图片height=\"1920\"");

        // 验证img标签数量
        int imgCount = countOccurrences(html, "<img");
        assertTrue(imgCount >= 2, "应至少生成2个img标签，实际: " + imgCount);
    }

    /**
     * TC-S10: 单位支持测试（px, cm, in等）
     */
    @Test
    @Order(10)
    @DisplayName("TC-S10: 多单位支持")
    public void testMultipleUnits() throws Exception {
        String xml = buildXml(
            "<symbol infoEntityIdent=\"ICN-1\" reproductionWidth=\"10cm\" reproductionHeight=\"150px\"/>"
        );

        String html = DmXsltTransformer.transform(xml, "S1000D40", "descript");

        assertTrue(html.contains("width=\"10cm\""), "应支持cm单位");
        assertTrue(html.contains("height=\"150px\""), "应支持px单位");
    }

    // ==================== 工具方法 ====================

    /**
     * 构建测试XML
     */
    private String buildXml(String content) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<dmodule xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
               "  <content>\n" +
               "    <description>\n" +
               "      <para>" + content + "</para>\n" +
               "    </description>\n" +
               "  </content>\n" +
               "</dmodule>";
    }

    /**
     * 统计字符串出现次数
     */
    private int countOccurrences(String text, String pattern) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(pattern, index)) != -1) {
            count++;
            index += pattern.length();
        }
        return count;
    }
}
