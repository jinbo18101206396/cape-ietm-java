package org.jeecg.modules.ietm.ietmdatamodulemanagement;

import org.jeecg.modules.ietm.ietmdatamodulemanagement.util.DmXsltTransformer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 🔬 DM预览图形尺寸 - 全面深度测试套件
 *
 * 测试层级：
 * - L1: 边界值测试 (Boundary Tests)
 * - L2: 异常处理测试 (Exception Handling)
 * - L3: 组合场景测试 (Combination Scenarios)
 * - L4: 性能基准测试 (Performance Benchmarks)
 * - L5: 回归测试 (Regression Tests)
 *
 * 符合标准：S1000D 4.0 § 3.9.5.2.16
 *
 * @author Kiro
 * @date 2026-09-18
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DmPreviewGraphicSizeComprehensiveTest {

    // ==================== L1: 边界值测试 ====================

    /**
     * TC-B01: 最小宽度（1px）
     */
    @Test
    @Order(1)
    @DisplayName("L1-B01: 最小宽度1px")
    public void testMinimumWidth() throws Exception {
        String xml = buildXml("<graphic reproductionWidth=\"1px\" infoEntityIdent=\"ICN-TEST-001\"/>");
        String html = DmXsltTransformer.transform(xml, "S1000D40", "descript");

        assertTrue(html.contains("width=\"1px\""), "应包含width=\"1px\"");
        assertFalse(html.contains("width=\"0"), "宽度不应为0");
    }

    /**
     * TC-B02: 最大宽度（9999px）
     */
    @Test
    @Order(2)
    @DisplayName("L1-B02: 最大宽度9999px")
    public void testMaximumWidth() throws Exception {
        String xml = buildXml("<graphic reproductionWidth=\"9999px\" infoEntityIdent=\"ICN-TEST-002\"/>");
        String html = DmXsltTransformer.transform(xml, "S1000D40", "descript");

        assertTrue(html.contains("width=\"9999px\""), "应包含width=\"9999px\"");
    }

    /**
     * TC-B03: 最小缩放（1%）
     */
    @Test
    @Order(3)
    @DisplayName("L1-B03: 最小缩放1%")
    public void testMinimumScale() throws Exception {
        String xml = buildXml("<graphic reproductionScale=\"1\" infoEntityIdent=\"ICN-TEST-003\"/>");
        String html = DmXsltTransformer.transform(xml, "S1000D40", "descript");

        assertTrue(html.contains("width=\"1%\""), "应包含width=\"1%\"");
    }

    /**
     * TC-B04: 最大缩放（1000%）
     */
    @Test
    @Order(4)
    @DisplayName("L1-B04: 最大缩放1000%")
    public void testMaximumScale() throws Exception {
        String xml = buildXml("<graphic reproductionScale=\"1000\" infoEntityIdent=\"ICN-TEST-004\"/>");
        String html = DmXsltTransformer.transform(xml, "S1000D40", "descript");

        assertTrue(html.contains("width=\"1000%\""), "应包含width=\"1000%\"");
    }

    /**
     * TC-B05: 零宽度（边界异常）
     */
    @Test
    @Order(5)
    @DisplayName("L1-B05: 零宽度边界测试")
    public void testZeroWidth() throws Exception {
        String xml = buildXml("<graphic reproductionWidth=\"0px\" infoEntityIdent=\"ICN-TEST-005\"/>");
        String html = DmXsltTransformer.transform(xml, "S1000D40", "descript");

        // XSLT不做校验，直接传递
        assertTrue(html.contains("width=\"0px\""), "应包含width=\"0px\"（XSLT直接传递）");
    }

    /**
     * TC-B06: 负数值（边界异常）
     */
    @Test
    @Order(6)
    @DisplayName("L1-B06: 负数值边界测试")
    public void testNegativeValue() throws Exception {
        String xml = buildXml("<graphic reproductionWidth=\"-100px\" infoEntityIdent=\"ICN-TEST-006\"/>");
        String html = DmXsltTransformer.transform(xml, "S1000D40", "descript");

        // XSLT不做校验，直接传递
        assertTrue(html.contains("width=\"-100px\""), "应包含width=\"-100px\"（XSLT直接传递）");
    }

    // ==================== L2: 异常处理测试 ====================

    /**
     * TC-E01: 空属性值
     */
    @Test
    @Order(11)
    @DisplayName("L2-E01: 空属性值处理")
    public void testEmptyAttribute() throws Exception {
        String xml = buildXml("<graphic reproductionWidth=\"\" infoEntityIdent=\"ICN-TEST-011\"/>");
        String html = DmXsltTransformer.transform(xml, "S1000D40", "descript");

        // 空值在XSLT中test="@reproductionWidth"会返回true（属性存在），
        // 因此会生成width=""，这是XSLT的标准行为
        assertTrue(html.contains("<img"), "应生成img标签");
        assertTrue(html.contains("width=\"\""), "空属性值会生成width=\"\"（XSLT标准行为）");
    }

    /**
     * TC-E02: 无效单位
     */
    @Test
    @Order(12)
    @DisplayName("L2-E02: 无效单位处理")
    public void testInvalidUnit() throws Exception {
        String xml = buildXml("<graphic reproductionWidth=\"100xyz\" infoEntityIdent=\"ICN-TEST-012\"/>");
        String html = DmXsltTransformer.transform(xml, "S1000D40", "descript");

        // XSLT直接传递，由浏览器处理
        assertTrue(html.contains("width=\"100xyz\""), "应包含原始值（由浏览器处理）");
    }

    /**
     * TC-E03: 特殊字符
     */
    @Test
    @Order(13)
    @DisplayName("L2-E03: 特殊字符处理")
    public void testSpecialCharacters() throws Exception {
        // 测试XML转义字符在属性值中的处理
        String xml = buildXml("<graphic reproductionWidth=\"100&amp;200\" infoEntityIdent=\"ICN-TEST-013\"/>");
        String html = DmXsltTransformer.transform(xml, "S1000D40", "descript");

        // HTML输出中&会被再次转义为&amp;（HTML标准），这是正确的
        assertTrue(html.contains("<img"), "应生成img标签");
        assertTrue(html.contains("width=\"100&amp;200\""), "HTML输出中&应被转义为&amp;（HTML标准）");
    }

    /**
     * TC-E04: 缺少infoEntityIdent属性
     */
    @Test
    @Order(14)
    @DisplayName("L2-E04: 缺少必需属性")
    public void testMissingRequiredAttribute() throws Exception {
        String xml = buildXml("<graphic reproductionWidth=\"200px\"/>");
        String html = DmXsltTransformer.transform(xml, "S1000D40", "descript");

        // 应该能正常生成，但没有src
        assertTrue(html.contains("<img"), "应生成img标签");
        assertTrue(html.contains("width=\"200px\""), "应包含width属性");
    }

    /**
     * TC-E05: 多余空白字符
     */
    @Test
    @Order(15)
    @DisplayName("L2-E05: 属性值包含空白")
    public void testWhitespaceInAttribute() throws Exception {
        String xml = buildXml("<graphic reproductionWidth=\" 300px \" infoEntityIdent=\"ICN-TEST-015\"/>");
        String html = DmXsltTransformer.transform(xml, "S1000D40", "descript");

        // XSLT会保留空白，由浏览器处理
        assertTrue(html.contains("<img"), "应生成img标签");
    }

    // ==================== L3: 组合场景测试 ====================

    /**
     * TC-C01: 宽度+高度+缩放（优先级测试）
     */
    @Test
    @Order(21)
    @DisplayName("L3-C01: 三属性优先级")
    public void testThreeAttributesPriority() throws Exception {
        String xml = buildXml(
            "<graphic reproductionWidth=\"400px\" reproductionHeight=\"300px\" " +
            "reproductionScale=\"50\" infoEntityIdent=\"ICN-TEST-021\"/>"
        );
        String html = DmXsltTransformer.transform(xml, "S1000D40", "descript");

        // 宽高优先于缩放
        assertTrue(html.contains("width=\"400px\""), "应优先使用width");
        assertTrue(html.contains("height=\"300px\""), "应优先使用height");
        assertFalse(html.contains("50%"), "不应包含scale");
    }

    /**
     * TC-C02: 仅缩放（无宽高）
     */
    @Test
    @Order(22)
    @DisplayName("L3-C02: 仅缩放属性")
    public void testScaleOnly() throws Exception {
        String xml = buildXml("<graphic reproductionScale=\"75\" infoEntityIdent=\"ICN-TEST-022\"/>");
        String html = DmXsltTransformer.transform(xml, "S1000D40", "descript");

        assertTrue(html.contains("width=\"75%\""), "应包含width=\"75%\"");
        assertFalse(html.matches(".*height=\"\\d+.*"), "不应有height属性");
    }

    /**
     * TC-C03: 混合单位（px + cm）
     */
    @Test
    @Order(23)
    @DisplayName("L3-C03: 混合单位")
    public void testMixedUnits() throws Exception {
        String xml = buildXml(
            "<graphic reproductionWidth=\"10cm\" reproductionHeight=\"150px\" " +
            "infoEntityIdent=\"ICN-TEST-023\"/>"
        );
        String html = DmXsltTransformer.transform(xml, "S1000D40", "descript");

        assertTrue(html.contains("width=\"10cm\""), "应支持cm单位");
        assertTrue(html.contains("height=\"150px\""), "应支持px单位");
    }

    /**
     * TC-C04: 嵌套图形（多层级）
     */
    @Test
    @Order(24)
    @DisplayName("L3-C04: 嵌套结构")
    public void testNestedGraphics() throws Exception {
        String xml = buildXml(
            "<figure>" +
            "  <graphic reproductionWidth=\"100px\" infoEntityIdent=\"ICN-1\"/>" +
            "  <graphic reproductionWidth=\"200px\" infoEntityIdent=\"ICN-2\"/>" +
            "</figure>"
        );
        String html = DmXsltTransformer.transform(xml, "S1000D40", "descript");

        int count100 = countOccurrences(html, "width=\"100px\"");
        int count200 = countOccurrences(html, "width=\"200px\"");

        assertEquals(1, count100, "应包含1个100px");
        assertEquals(1, count200, "应包含1个200px");
    }

    /**
     * TC-C05: 10个连续图形（批量测试）
     */
    @Test
    @Order(25)
    @DisplayName("L3-C05: 批量图形处理")
    public void testBatchGraphics() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 10; i++) {
            sb.append("<graphic reproductionWidth=\"")
              .append(i * 50)
              .append("px\" infoEntityIdent=\"ICN-")
              .append(i)
              .append("\"/>");
        }

        String xml = buildXml(sb.toString());
        String html = DmXsltTransformer.transform(xml, "S1000D40", "descript");

        // 验证所有尺寸都正确生成
        for (int i = 1; i <= 10; i++) {
            int width = i * 50;
            assertTrue(html.contains("width=\"" + width + "px\""),
                      "应包含width=\"" + width + "px\"");
        }
    }

    // ==================== L4: 性能基准测试 ====================

    /**
     * TC-P01: 单次转换性能
     */
    @Test
    @Order(31)
    @DisplayName("L4-P01: 单次转换性能")
    public void testSingleTransformPerformance() throws Exception {
        String xml = buildXml("<graphic reproductionWidth=\"500px\" infoEntityIdent=\"ICN-PERF-001\"/>");

        long start = System.nanoTime();
        String html = DmXsltTransformer.transform(xml, "S1000D40", "descript");
        long duration = System.nanoTime() - start;

        assertTrue(html.contains("width=\"500px\""), "转换结果正确");
        assertTrue(duration < 100_000_000, // 100ms
                  "单次转换应在100ms内完成，实际: " + (duration / 1_000_000) + "ms");
    }

    /**
     * TC-P02: 100次转换性能（批量）
     */
    @Test
    @Order(32)
    @DisplayName("L4-P02: 批量转换性能")
    public void testBatchTransformPerformance() throws Exception {
        String xml = buildXml("<graphic reproductionWidth=\"500px\" infoEntityIdent=\"ICN-PERF-002\"/>");

        long start = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            DmXsltTransformer.transform(xml, "S1000D40", "descript");
        }
        long duration = System.nanoTime() - start;
        long avgDuration = duration / 100;

        assertTrue(avgDuration < 10_000_000, // 10ms平均
                  "平均转换应在10ms内，实际: " + (avgDuration / 1_000_000) + "ms");
    }

    /**
     * TC-P03: 大文档转换性能（50个图形）
     */
    @Test
    @Order(33)
    @DisplayName("L4-P03: 大文档转换性能")
    public void testLargeDocumentPerformance() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 50; i++) {
            sb.append("<graphic reproductionWidth=\"")
              .append(i * 10)
              .append("px\" infoEntityIdent=\"ICN-")
              .append(i)
              .append("\"/>");
        }

        String xml = buildXml(sb.toString());

        long start = System.nanoTime();
        String html = DmXsltTransformer.transform(xml, "S1000D40", "descript");
        long duration = System.nanoTime() - start;

        assertTrue(html.contains("<img"), "应生成图形");
        assertTrue(duration < 500_000_000, // 500ms
                  "50个图形转换应在500ms内，实际: " + (duration / 1_000_000) + "ms");
    }

    // ==================== L5: 回归测试 ====================

    /**
     * TC-R01: 向后兼容 - 无size属性的图形仍能显示
     */
    @Test
    @Order(41)
    @DisplayName("L5-R01: 向后兼容性")
    public void testBackwardCompatibility() throws Exception {
        String xml = buildXml("<graphic infoEntityIdent=\"ICN-OLD-001\"/>");
        String html = DmXsltTransformer.transform(xml, "S1000D40", "descript");

        assertTrue(html.contains("<img"), "应生成img标签");
        assertTrue(html.contains("max-width: 800px") || html.contains("style=\"max-width: 800px"),
                   "应有默认样式");
    }

    /**
     * TC-R02: symbol元素不受影响
     */
    @Test
    @Order(42)
    @DisplayName("L5-R02: symbol元素独立性")
    public void testSymbolIndependence() throws Exception {
        String xml = buildXml(
            "<symbol reproductionWidth=\"100px\" infoEntityIdent=\"ICN-SYM-001\"/>"
        );
        String html = DmXsltTransformer.transform(xml, "S1000D40", "descript");

        // symbol应该有自己的处理逻辑
        assertTrue(html.contains("<img"), "应生成img标签");
    }

    /**
     * TC-R03: 其他元素不受影响
     */
    @Test
    @Order(43)
    @DisplayName("L5-R03: 其他元素隔离性")
    public void testOtherElementsIsolation() throws Exception {
        String xml = buildXml(
            "<para>测试段落</para>" +
            "<graphic reproductionWidth=\"200px\" infoEntityIdent=\"ICN-TEST-043\"/>" +
            "<table><title>测试表格</title></table>"
        );
        String html = DmXsltTransformer.transform(xml, "S1000D40", "descript");

        assertTrue(html.contains("测试段落"), "段落应正常显示");
        assertTrue(html.contains("width=\"200px\""), "图形应有尺寸");
        assertTrue(html.contains("测试表格"), "表格应正常显示");
    }

    /**
     * TC-R04: 原有测试用例 - 标准宽高
     */
    @Test
    @Order(44)
    @DisplayName("L5-R04: 原测试TC-01回归")
    public void testOriginalTC01() throws Exception {
        String xml = buildXml(
            "<graphic reproductionWidth=\"600px\" reproductionHeight=\"400px\" " +
            "infoEntityIdent=\"ICN-12345-00001-001\"/>"
        );
        String html = DmXsltTransformer.transform(xml, "S1000D40", "descript");

        assertTrue(html.contains("width=\"600px\""), "应包含width=\"600px\"");
        assertTrue(html.contains("height=\"400px\""), "应包含height=\"400px\"");
    }

    /**
     * TC-R05: 原有测试用例 - 仅缩放
     */
    @Test
    @Order(45)
    @DisplayName("L5-R05: 原测试TC-04回归")
    public void testOriginalTC04() throws Exception {
        String xml = buildXml(
            "<graphic reproductionScale=\"80\" infoEntityIdent=\"ICN-12345-00001-004\"/>"
        );
        String html = DmXsltTransformer.transform(xml, "S1000D40", "descript");

        assertTrue(html.contains("width=\"80%\""), "应包含width=\"80%\"");
    }

    // ==================== 参数化测试 ====================

    /**
     * TC-PZ01: 多单位支持测试
     */
    @ParameterizedTest
    @CsvSource({
        "100px, width=\"100px\"",
        "10cm, width=\"10cm\"",
        "5in, width=\"5in\"",
        "50mm, width=\"50mm\"",
        "20pt, width=\"20pt\"",
        "15pc, width=\"15pc\""
    })
    @Order(51)
    @DisplayName("L6-PZ01: 多单位支持")
    public void testMultipleUnits(String widthValue, String expectedHtml) throws Exception {
        String xml = buildXml(
            "<graphic reproductionWidth=\"" + widthValue + "\" infoEntityIdent=\"ICN-UNIT\"/>"
        );
        String html = DmXsltTransformer.transform(xml, "S1000D40", "descript");

        assertTrue(html.contains(expectedHtml),
                  "应包含" + expectedHtml + "，实际: " + html);
    }

    /**
     * TC-PZ02: 缩放比例范围测试
     */
    @ParameterizedTest
    @ValueSource(ints = {10, 25, 50, 75, 100, 150, 200})
    @Order(52)
    @DisplayName("L6-PZ02: 常用缩放比例")
    public void testCommonScales(int scale) throws Exception {
        String xml = buildXml(
            "<graphic reproductionScale=\"" + scale + "\" infoEntityIdent=\"ICN-SCALE\"/>"
        );
        String html = DmXsltTransformer.transform(xml, "S1000D40", "descript");

        assertTrue(html.contains("width=\"" + scale + "%\""),
                  "应包含width=\"" + scale + "%\"");
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
               "      " + content + "\n" +
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
