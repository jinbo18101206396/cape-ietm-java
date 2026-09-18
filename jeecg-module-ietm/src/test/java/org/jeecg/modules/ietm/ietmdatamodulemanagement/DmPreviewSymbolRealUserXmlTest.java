package org.jeecg.modules.ietm.ietmdatamodulemanagement;

import org.jeecg.modules.ietm.ietmdatamodulemanagement.util.DmXsltTransformer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * L2: 真实用户XML验证测试
 *
 * 测试目标：使用用户提供的真实XML验证symbol尺寸渲染
 * 测试日期：2026-09-18
 * 测试背景：用户报告预览DM内容弹出框中图片大小不一样
 */
@DisplayName("L2: 真实用户XML - Symbol尺寸渲染验证")
public class DmPreviewSymbolRealUserXmlTest {

    /**
     * TC-L2-01: 用户真实XML - 三个symbol不同尺寸
     *
     * 预期结果：
     * - Symbol 1: width="1920" height="1080" (横向大图)
     * - Symbol 2: width="1440" height="1920" (竖向大图)
     * - Symbol 3: width="1920" height="1080" (横向大图)
     */
    @Test
    @DisplayName("TC-L2-01: 用户提供的真实XML - 三个symbol不同尺寸")
    public void testRealUserXml_ThreeSymbolsDifferentSizes() throws Exception {
        // 用户提供的真实XML（完整的dmodule结构）
        String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<dmodule xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://www.s1000d.org/S1000D_4-0/xml_schema_flat/descript.xsd\">\n" +
            "  <identAndStatusSection>\n" +
            "    <dmAddress>\n" +
            "      <dmIdent>\n" +
            "        <dmCode modelIdentCode=\"TEST\" systemDiffCode=\"A\" systemCode=\"00\" subSystemCode=\"0\" subSubSystemCode=\"0\" assyCode=\"00\" disassyCode=\"00\" disassyCodeVariant=\"A\" infoCode=\"040\" infoCodeVariant=\"A\" itemLocationCode=\"A\"/>\n" +
            "      </dmIdent>\n" +
            "      <dmAddressItems>\n" +
            "        <issueDate year=\"2026\" month=\"09\" day=\"18\"/>\n" +
            "        <language languageIsoCode=\"zh\" countryIsoCode=\"CN\"/>\n" +
            "      </dmAddressItems>\n" +
            "    </dmAddress>\n" +
            "    <dmStatus>\n" +
            "      <security securityClassification=\"01\"/>\n" +
            "      <responsiblePartnerCompany enterpriseCode=\"TEST\"/>\n" +
            "      <originator enterpriseCode=\"TEST\"/>\n" +
            "    </dmStatus>\n" +
            "  </identAndStatusSection>\n" +
            "  <content>\n" +
            "    <description>\n" +
            "      <levelledPara>\n" +
            "        <title>测试标题</title>\n" +
            "        <figure>\n" +
            "          <title>图1 - 横向大图</title>\n" +
            "          <graphic infoEntityIdent=\"ICN-001\">\n" +
            "            <symbol reproductionWidth=\"1920\" reproductionHeight=\"1080\" reproductionScale=\"100\" infoEntityIdent=\"ICN-001\"/>\n" +
            "          </graphic>\n" +
            "        </figure>\n" +
            "        <figure>\n" +
            "          <title>图2 - 竖向大图</title>\n" +
            "          <graphic infoEntityIdent=\"ICN-002\">\n" +
            "            <symbol reproductionWidth=\"1440\" reproductionHeight=\"1920\" reproductionScale=\"100\" infoEntityIdent=\"ICN-002\"/>\n" +
            "          </graphic>\n" +
            "        </figure>\n" +
            "        <figure>\n" +
            "          <title>图3 - 横向大图</title>\n" +
            "          <graphic infoEntityIdent=\"ICN-003\">\n" +
            "            <symbol reproductionWidth=\"1920\" reproductionHeight=\"1080\" reproductionScale=\"100\" infoEntityIdent=\"ICN-003\"/>\n" +
            "          </graphic>\n" +
            "        </figure>\n" +
            "      </levelledPara>\n" +
            "    </description>\n" +
            "  </content>\n" +
            "</dmodule>";

        // 执行XSLT转换
        String html = DmXsltTransformer.transform(xmlContent, "S1000D40", "descript");

        // 验证转换成功
        assertNotNull(html, "HTML输出不应为null");
        assertFalse(html.trim().isEmpty(), "HTML输出不应为空");

        // 验证Symbol 1: 1920x1080
        assertTrue(html.contains("ICN-001"), "应包含ICN-001图形引用");
        assertTrue(html.matches("(?s).*ICN-001.*width=\"1920\".*"),
            "Symbol 1应设置width=\"1920\"");
        assertTrue(html.matches("(?s).*ICN-001.*height=\"1080\".*"),
            "Symbol 1应设置height=\"1080\"");

        // 验证Symbol 2: 1440x1920
        assertTrue(html.contains("ICN-002"), "应包含ICN-002图形引用");
        assertTrue(html.matches("(?s).*ICN-002.*width=\"1440\".*"),
            "Symbol 2应设置width=\"1440\"");
        assertTrue(html.matches("(?s).*ICN-002.*height=\"1920\".*"),
            "Symbol 2应设置height=\"1920\"");

        // 验证Symbol 3: 1920x1080
        assertTrue(html.contains("ICN-003"), "应包含ICN-003图形引用");
        assertTrue(html.matches("(?s).*ICN-003.*width=\"1920\".*"),
            "Symbol 3应设置width=\"1920\"");
        assertTrue(html.matches("(?s).*ICN-003.*height=\"1080\".*"),
            "Symbol 3应设置height=\"1080\"");

        // 验证三个图片尺寸应该不同（Symbol 1和3相同，Symbol 2不同）
        int count1920x1080 = countOccurrences(html, "width=\"1920\".*?height=\"1080\"");
        int count1440x1920 = countOccurrences(html, "width=\"1440\".*?height=\"1920\"");

        assertEquals(2, count1920x1080, "应有2个1920x1080的图片（Symbol 1和3）");
        assertEquals(1, count1440x1920, "应有1个1440x1920的图片（Symbol 2）");

        System.out.println("✅ TC-L2-01通过: 用户真实XML验证成功");
        System.out.println("  - Symbol 1: 1920x1080 ✓");
        System.out.println("  - Symbol 2: 1440x1920 ✓");
        System.out.println("  - Symbol 3: 1920x1080 ✓");
    }

    /**
     * TC-L2-02: 真实XML - 极端尺寸组合
     */
    @Test
    @DisplayName("TC-L2-02: 真实XML - 极端尺寸组合（超大+超小）")
    public void testRealUserXml_ExtremeSizes() throws Exception {
        String xmlContent = buildDmodule(
            "<figure><title>超大图</title><graphic infoEntityIdent=\"ICN-LARGE\">" +
            "<symbol reproductionWidth=\"3840\" reproductionHeight=\"2160\" infoEntityIdent=\"ICN-LARGE\"/>" +
            "</graphic></figure>" +
            "<figure><title>超小图</title><graphic infoEntityIdent=\"ICN-SMALL\">" +
            "<symbol reproductionWidth=\"64\" reproductionHeight=\"64\" infoEntityIdent=\"ICN-SMALL\"/>" +
            "</graphic></figure>"
        );

        String html = DmXsltTransformer.transform(xmlContent, "S1000D40", "descript");

        assertNotNull(html);
        assertTrue(html.contains("width=\"3840\""), "应支持超大尺寸3840");
        assertTrue(html.contains("height=\"2160\""), "应支持超大尺寸2160");
        assertTrue(html.contains("width=\"64\""), "应支持超小尺寸64");
        assertTrue(html.contains("height=\"64\""), "应支持超小尺寸64");

        System.out.println("✅ TC-L2-02通过: 极端尺寸验证成功");
    }

    /**
     * TC-L2-03: 真实XML - 混合属性模式
     */
    @Test
    @DisplayName("TC-L2-03: 真实XML - Width/Height/Scale混合使用")
    public void testRealUserXml_MixedAttributes() throws Exception {
        String xmlContent = buildDmodule(
            "<figure><title>仅Width</title><graphic infoEntityIdent=\"ICN-W\">" +
            "<symbol reproductionWidth=\"800\" infoEntityIdent=\"ICN-W\"/>" +
            "</graphic></figure>" +
            "<figure><title>仅Height</title><graphic infoEntityIdent=\"ICN-H\">" +
            "<symbol reproductionHeight=\"600\" infoEntityIdent=\"ICN-H\"/>" +
            "</graphic></figure>" +
            "<figure><title>仅Scale</title><graphic infoEntityIdent=\"ICN-S\">" +
            "<symbol reproductionScale=\"50\" infoEntityIdent=\"ICN-S\"/>" +
            "</graphic></figure>" +
            "<figure><title>Width+Height</title><graphic infoEntityIdent=\"ICN-WH\">" +
            "<symbol reproductionWidth=\"1024\" reproductionHeight=\"768\" infoEntityIdent=\"ICN-WH\"/>" +
            "</graphic></figure>"
        );

        String html = DmXsltTransformer.transform(xmlContent, "S1000D40", "descript");

        assertNotNull(html);
        assertTrue(html.contains("width=\"800\""), "仅Width应生效");
        assertTrue(html.contains("height=\"600\""), "仅Height应生效");
        assertTrue(html.contains("width=\"50%\""), "仅Scale应转换为百分比");
        assertTrue(html.contains("width=\"1024\"") && html.contains("height=\"768\""),
            "Width+Height应同时生效");

        System.out.println("✅ TC-L2-03通过: 混合属性验证成功");
    }

    /**
     * TC-L2-04: 真实XML - 无尺寸属性（默认行为）
     */
    @Test
    @DisplayName("TC-L2-04: 真实XML - 无尺寸属性默认处理")
    public void testRealUserXml_NoSizeAttributes() throws Exception {
        String xmlContent = buildDmodule(
            "<figure><title>无尺寸</title><graphic infoEntityIdent=\"ICN-NONE\">" +
            "<symbol infoEntityIdent=\"ICN-NONE\"/>" +
            "</graphic></figure>"
        );

        String html = DmXsltTransformer.transform(xmlContent, "S1000D40", "descript");

        assertNotNull(html);
        assertTrue(html.contains("ICN-NONE"), "应包含图形引用");
        // 无尺寸属性时，img标签不应有width/height属性，或者为auto
        assertFalse(html.matches("(?s).*ICN-NONE.*width=\"[0-9]+\".*"),
            "无尺寸属性时不应设置数值width");

        System.out.println("✅ TC-L2-04通过: 无尺寸属性验证成功");
    }

    /**
     * TC-L2-05: 真实XML - 多个symbol在同一figure中
     */
    @Test
    @DisplayName("TC-L2-05: 真实XML - 同一figure多个symbol")
    public void testRealUserXml_MultipleSymbolsInOneFigure() throws Exception {
        String xmlContent = buildDmodule(
            "<figure><title>多图对比</title>" +
            "<graphic infoEntityIdent=\"ICN-A\">" +
            "<symbol reproductionWidth=\"400\" reproductionHeight=\"300\" infoEntityIdent=\"ICN-A\"/>" +
            "</graphic>" +
            "<graphic infoEntityIdent=\"ICN-B\">" +
            "<symbol reproductionWidth=\"600\" reproductionHeight=\"400\" infoEntityIdent=\"ICN-B\"/>" +
            "</graphic>" +
            "<graphic infoEntityIdent=\"ICN-C\">" +
            "<symbol reproductionWidth=\"800\" reproductionHeight=\"600\" infoEntityIdent=\"ICN-C\"/>" +
            "</graphic>" +
            "</figure>"
        );

        String html = DmXsltTransformer.transform(xmlContent, "S1000D40", "descript");

        assertNotNull(html);
        assertTrue(html.contains("width=\"400\"") && html.contains("height=\"300\""),
            "Symbol A应设置400x300");
        assertTrue(html.contains("width=\"600\"") && html.contains("height=\"400\""),
            "Symbol B应设置600x400");
        assertTrue(html.contains("width=\"800\"") && html.contains("height=\"600\""),
            "Symbol C应设置800x600");

        System.out.println("✅ TC-L2-05通过: 同一figure多symbol验证成功");
    }

    // ==================== 辅助方法 ====================

    /**
     * 构建完整的dmodule XML结构
     */
    private String buildDmodule(String content) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<dmodule xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
            "xsi:noNamespaceSchemaLocation=\"http://www.s1000d.org/S1000D_4-0/xml_schema_flat/descript.xsd\">\n" +
            "  <identAndStatusSection>\n" +
            "    <dmAddress>\n" +
            "      <dmIdent>\n" +
            "        <dmCode modelIdentCode=\"TEST\" systemDiffCode=\"A\" systemCode=\"00\" " +
            "subSystemCode=\"0\" subSubSystemCode=\"0\" assyCode=\"00\" disassyCode=\"00\" " +
            "disassyCodeVariant=\"A\" infoCode=\"040\" infoCodeVariant=\"A\" itemLocationCode=\"A\"/>\n" +
            "      </dmIdent>\n" +
            "      <dmAddressItems>\n" +
            "        <issueDate year=\"2026\" month=\"09\" day=\"18\"/>\n" +
            "        <language languageIsoCode=\"zh\" countryIsoCode=\"CN\"/>\n" +
            "      </dmAddressItems>\n" +
            "    </dmAddress>\n" +
            "    <dmStatus>\n" +
            "      <security securityClassification=\"01\"/>\n" +
            "      <responsiblePartnerCompany enterpriseCode=\"TEST\"/>\n" +
            "      <originator enterpriseCode=\"TEST\"/>\n" +
            "    </dmStatus>\n" +
            "  </identAndStatusSection>\n" +
            "  <content>\n" +
            "    <description>\n" +
            "      <levelledPara>\n" +
            "        <title>测试</title>\n" +
            "        " + content + "\n" +
            "      </levelledPara>\n" +
            "    </description>\n" +
            "  </content>\n" +
            "</dmodule>";
    }

    /**
     * 计算正则表达式匹配次数
     */
    private int countOccurrences(String text, String regex) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }
}
