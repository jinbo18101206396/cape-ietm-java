package org.jeecg.modules.ietm.ietmdatamodulemanagement;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.util.DmXmlHelper;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.util.DmXsltTransformer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DM预览图形尺寸测试 - 验证S1000D 4.0标准合规性
 *
 * 修复问题：BUG-PREVIEW-GRAPHIC-SIZE
 * 问题描述：DM预览弹窗中图片大小不一致
 * 根因：fig_tabFigure.xsl未处理reproductionWidth/Height/Scale属性
 *
 * 🔧 不使用@SpringBootTest，避免CacheConfig冲突
 *
 * @author claude
 * @date 2026-09-18
 */
@Slf4j
@DisplayName("DM预览 - 图形尺寸符合S1000D标准测试")
public class DmPreviewGraphicSizeTest {

    @BeforeEach
    void setUp() {
        log.info("========== 测试开始：清除XSLT缓存 ==========");
        DmXsltTransformer.clearCache();
    }

    @AfterEach
    void tearDown() {
        log.info("========== 测试完成 ==========\n");
    }

    /**
     * TC-01: 指定宽高的图形应精确显示
     */
    @Test
    @DisplayName("TC-01: reproductionWidth + reproductionHeight → 精确尺寸")
    void testGraphicWithWidthAndHeight() throws Exception {
        String xml = buildTestDm(
            "<graphic infoEntityIdent=\"ICN-TEST-001\" " +
            "reproductionWidth=\"300px\" reproductionHeight=\"200px\"/>"
        );

        String html = DmXmlHelper.renderHtml(xml, "/jeecg-boot");

        log.info("生成的HTML片段:\n{}", extractImgTag(html));

        // 验证：img标签应包含width和height属性
        assertTrue(html.contains("width=\"300px\""), "应包含width=\"300px\"");
        assertTrue(html.contains("height=\"200px\""), "应包含height=\"200px\"");
        assertFalse(html.contains("style=\"max-width: 800px"),
            "指定了尺寸时不应添加默认style");
    }

    /**
     * TC-02: 仅指定宽度，高度自动
     */
    @Test
    @DisplayName("TC-02: reproductionWidth only → 宽度固定，高度auto")
    void testGraphicWithWidthOnly() throws Exception {
        String xml = buildTestDm(
            "<graphic infoEntityIdent=\"ICN-TEST-002\" reproductionWidth=\"400px\"/>"
        );

        String html = DmXmlHelper.renderHtml(xml, "/jeecg-boot");

        log.info("生成的HTML片段:\n{}", extractImgTag(html));

        assertTrue(html.contains("width=\"400px\""), "应包含width=\"400px\"");
        assertFalse(html.contains("height=\""), "不应包含height属性");
    }

    /**
     * TC-03: 仅指定高度，宽度自动
     */
    @Test
    @DisplayName("TC-03: reproductionHeight only → 高度固定，宽度auto")
    void testGraphicWithHeightOnly() throws Exception {
        String xml = buildTestDm(
            "<graphic infoEntityIdent=\"ICN-TEST-003\" reproductionHeight=\"150px\"/>"
        );

        String html = DmXmlHelper.renderHtml(xml, "/jeecg-boot");

        log.info("生成的HTML片段:\n{}", extractImgTag(html));

        assertTrue(html.contains("height=\"150px\""), "应包含height=\"150px\"");
        // 注意：<img>标签还有其他width属性（如boardno等），只检查没有reproductionWidth即可
        assertFalse(html.matches(".*width=\"\\d+px\".*"), "不应有像素宽度属性");
    }

    /**
     * TC-04: 指定缩放比例
     */
    @Test
    @DisplayName("TC-04: reproductionScale → 百分比缩放")
    void testGraphicWithScale() throws Exception {
        String xml = buildTestDm(
            "<graphic infoEntityIdent=\"ICN-TEST-004\" reproductionScale=\"50\"/>"
        );

        String html = DmXmlHelper.renderHtml(xml, "/jeecg-boot");

        log.info("生成的HTML片段:\n{}", extractImgTag(html));

        // reproductionScale应转为width百分比
        assertTrue(html.contains("width=\"50%\""), "应包含width=\"50%\"");
    }

    /**
     * TC-05: 无尺寸属性，使用默认样式
     */
    @Test
    @DisplayName("TC-05: 无尺寸属性 → 默认max-width:800px")
    void testGraphicWithoutSize() throws Exception {
        String xml = buildTestDm(
            "<graphic infoEntityIdent=\"ICN-TEST-005\"/>"
        );

        String html = DmXmlHelper.renderHtml(xml, "/jeecg-boot");

        log.info("生成的HTML片段:\n{}", extractImgTag(html));

        // 无尺寸属性时，应添加默认style
        assertTrue(html.contains("style=\"max-width: 800px; height: auto;\""),
            "应包含默认style=\"max-width: 800px; height: auto;\"");
        // 检查没有像素尺寸的width/height属性
        assertFalse(html.matches(".*width=\"\\d+px\".*"), "不应有像素宽度属性");
        assertFalse(html.matches(".*height=\"\\d+px\".*"), "不应有像素高度属性");
    }

    /**
     * TC-06: 多图一致性 - 所有图都设置reproductionScale="100"
     */
    @Test
    @DisplayName("TC-06: 多图一致性 → 所有图片尺寸统一")
    void testMultipleGraphicsConsistency() throws Exception {
        String xml = buildTestDmWithMultipleGraphics();

        String html = DmXmlHelper.renderHtml(xml, "/jeecg-boot");

        log.info("生成的HTML片段（前500字符）:\n{}",
            html.length() > 500 ? html.substring(0, 500) : html);

        // 验证：所有图形都应用了相同的尺寸
        int count = countOccurrences(html, "width=\"100%\"");
        assertTrue(count >= 3, "应至少有3个图形都设置了width=\"100%\"，实际: " + count);
    }

    /**
     * TC-07: reproductionWidth优先级高于reproductionScale
     */
    @Test
    @DisplayName("TC-07: Width优先级 > Scale")
    void testWidthPriorityOverScale() throws Exception {
        String xml = buildTestDm(
            "<graphic infoEntityIdent=\"ICN-TEST-007\" " +
            "reproductionWidth=\"350px\" reproductionScale=\"50\"/>"
        );

        String html = DmXmlHelper.renderHtml(xml, "/jeecg-boot");

        log.info("生成的HTML片段:\n{}", extractImgTag(html));

        // 验证：应使用Width而非Scale
        assertTrue(html.contains("width=\"350px\""), "应使用reproductionWidth");
        assertFalse(html.contains("width=\"50%\""), "不应使用reproductionScale");
    }

    /**
     * TC-08: 单位支持 - mm, cm, px, in, pt
     */
    @Test
    @DisplayName("TC-08: 多种单位支持")
    void testDifferentUnits() throws Exception {
        // 测试mm单位
        String xmlMm = buildTestDm(
            "<graphic infoEntityIdent=\"ICN-TEST-008\" reproductionWidth=\"150mm\"/>"
        );
        String htmlMm = DmXmlHelper.renderHtml(xmlMm, "/jeecg-boot");
        assertTrue(htmlMm.contains("width=\"150mm\""), "应支持mm单位");

        // 测试cm单位
        String xmlCm = buildTestDm(
            "<graphic infoEntityIdent=\"ICN-TEST-009\" reproductionWidth=\"15cm\"/>"
        );
        String htmlCm = DmXmlHelper.renderHtml(xmlCm, "/jeecg-boot");
        assertTrue(htmlCm.contains("width=\"15cm\""), "应支持cm单位");

        log.info("✅ 单位测试通过：mm, cm");
    }

    // ==================== 辅助方法 ====================

    /**
     * 构建测试用DM XML
     *
     * 🔧 注意：不使用xmlns默认命名空间，对齐真实数据库数据
     * 真实DM数据只使用xmlns:xsi，XSLT才能正确匹配元素
     */
    private String buildTestDm(String graphicElement) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<dmodule>\n" +
            "  <identAndStatusSection>\n" +
            "    <dmAddress>\n" +
            "      <dmIdent><dmCode modelIdentCode=\"TEST\" systemDiffCode=\"A\" " +
            "systemCode=\"00\" subSystemCode=\"0\" subSubSystemCode=\"0\" " +
            "assyCode=\"00\" disassyCode=\"00\" disassyCodeVariant=\"A\" " +
            "infoCode=\"040\" infoCodeVariant=\"A\" itemLocationCode=\"A\"/></dmIdent>\n" +
            "    </dmAddress>\n" +
            "  </identAndStatusSection>\n" +
            "  <content>\n" +
            "    <description>\n" +
            "      <levelledPara><title>测试图形尺寸</title>\n" +
            "        <para>图形尺寸测试</para>\n" +
            "        <figure id=\"fig-001\"><title>测试图</title>\n" +
            "          " + graphicElement + "\n" +
            "        </figure>\n" +
            "      </levelledPara>\n" +
            "    </description>\n" +
            "  </content>\n" +
            "</dmodule>";
    }

    /**
     * 构建包含多个图形的测试DM
     */
    private String buildTestDmWithMultipleGraphics() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<dmodule>\n" +
            "  <identAndStatusSection>\n" +
            "    <dmAddress>\n" +
            "      <dmIdent><dmCode modelIdentCode=\"TEST\" systemDiffCode=\"A\" " +
            "systemCode=\"00\" subSystemCode=\"0\" subSubSystemCode=\"0\" " +
            "assyCode=\"00\" disassyCode=\"00\" disassyCodeVariant=\"A\" " +
            "infoCode=\"040\" infoCodeVariant=\"A\" itemLocationCode=\"A\"/></dmIdent>\n" +
            "    </dmAddress>\n" +
            "  </identAndStatusSection>\n" +
            "  <content>\n" +
            "    <description>\n" +
            "      <levelledPara><title>多图测试</title>\n" +
            "        <figure id=\"fig-001\"><title>图1</title>\n" +
            "          <graphic infoEntityIdent=\"ICN-001\" reproductionScale=\"100\"/>\n" +
            "        </figure>\n" +
            "        <figure id=\"fig-002\"><title>图2</title>\n" +
            "          <graphic infoEntityIdent=\"ICN-002\" reproductionScale=\"100\"/>\n" +
            "        </figure>\n" +
            "        <figure id=\"fig-003\"><title>图3</title>\n" +
            "          <graphic infoEntityIdent=\"ICN-003\" reproductionScale=\"100\"/>\n" +
            "        </figure>\n" +
            "      </levelledPara>\n" +
            "    </description>\n" +
            "  </content>\n" +
            "</dmodule>";
    }

    /**
     * 提取HTML中的<img>标签（用于日志输出）
     */
    private String extractImgTag(String html) {
        int start = html.indexOf("<img");
        if (start == -1) return "(未找到img标签)";
        int end = html.indexOf(">", start);
        if (end == -1) return "(img标签不完整)";
        return html.substring(start, end + 1);
    }

    /**
     * 统计子串出现次数
     */
    private int countOccurrences(String str, String sub) {
        int count = 0;
        int idx = 0;
        while ((idx = str.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }
}
