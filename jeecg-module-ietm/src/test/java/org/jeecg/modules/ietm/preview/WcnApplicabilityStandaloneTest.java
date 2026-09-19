package org.jeecg.modules.ietm.preview;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Warning/Caution适用性修复 - 独立XSLT回归测试
 *
 * 不依赖SpringBoot容器，直接测试XSLT转换
 */
@DisplayName("状况模块适用性回归测试（独立）")
public class WcnApplicabilityStandaloneTest {

    @Test
    @DisplayName("TC-01: 带适用性的Warning显示正确")
    public void testWarningWithApplicability() throws Exception {
        String xml = buildTestXml(
            "<warning applicRefId=\"app-001\">" +
            "  <warningAndCautionPara>这是一个带适用性的警告</warningAndCautionPara>" +
            "</warning>",
            "<applic id=\"app-001\">" +
            "  <displayText><simplePara>仅适用于A型飞机</simplePara></displayText>" +
            "</applic>"
        );

        String html = transformToHtml(xml);

        System.out.println("================================================================================");
        System.out.println("TC-01: 带适用性的Warning");
        System.out.println("================================================================================");
        System.out.println(html);
        System.out.println("================================================================================");

        // 断言：应包含适用性信息
        assertTrue(html.contains("applicAnnotation"), "❌ 应包含适用性CSS类");
        assertTrue(html.contains("仅适用于A型飞机"), "❌ 应显示适用性文本");
        assertTrue(html.contains("warningBGImage"), "❌ 应包含警告样式");

        System.out.println("✅ TC-01 通过");
    }

    @Test
    @DisplayName("TC-02: 不带适用性的Warning显示正确")
    public void testWarningWithoutApplicability() throws Exception {
        String xml = buildTestXml(
            "<warning>" +
            "  <warningAndCautionPara>这是一个不带适用性的警告</warningAndCautionPara>" +
            "</warning>",
            ""
        );

        String html = transformToHtml(xml);

        System.out.println("================================================================================");
        System.out.println("TC-02: 不带适用性的Warning");
        System.out.println("================================================================================");
        System.out.println(html);
        System.out.println("================================================================================");

        // 断言：不应包含适用性信息
        assertFalse(html.contains("applicAnnotation"), "❌ 不应包含适用性CSS类");
        assertFalse(html.contains("适用性"), "❌ 不应显示适用性文本");
        assertTrue(html.contains("warningBGImage"), "❌ 应包含警告样式");

        System.out.println("✅ TC-02 通过（向后兼容）");
    }

    @Test
    @DisplayName("TC-03: 带适用性的Caution显示正确")
    public void testCautionWithApplicability() throws Exception {
        String xml = buildTestXml(
            "<caution applicRefId=\"app-002\">" +
            "  <warningAndCautionPara>这是一个带适用性的注意</warningAndCautionPara>" +
            "</caution>",
            "<applic id=\"app-002\">" +
            "  <displayText><simplePara>仅适用于沙漠型设备</simplePara></displayText>" +
            "</applic>"
        );

        String html = transformToHtml(xml);

        System.out.println("================================================================================");
        System.out.println("TC-03: 带适用性的Caution");
        System.out.println("================================================================================");
        System.out.println(html);
        System.out.println("================================================================================");

        // 断言：应包含适用性信息
        assertTrue(html.contains("applicAnnotation"), "❌ 应包含适用性CSS类");
        assertTrue(html.contains("仅适用于沙漠型设备"), "❌ 应显示适用性文本");
        assertTrue(html.contains("cautionBGImage"), "❌ 应包含注意样式");

        System.out.println("✅ TC-03 通过");
    }

    @Test
    @DisplayName("TC-04: Note的适用性未受影响")
    public void testNoteApplicabilityUnchanged() throws Exception {
        String xml = buildTestXml(
            "<note applicRefId=\"app-003\">" +
            "  <notePara>这是一个带适用性的注释</notePara>" +
            "</note>",
            "<applic id=\"app-003\">" +
            "  <displayText><simplePara>仅适用于v2.0+</simplePara></displayText>" +
            "</applic>"
        );

        String html = transformToHtml(xml);

        System.out.println("================================================================================");
        System.out.println("TC-04: Note的适用性");
        System.out.println("================================================================================");
        System.out.println(html);
        System.out.println("================================================================================");

        // 断言：Note应保持原有行为
        assertTrue(html.contains("applicAnnotation"), "❌ 应包含适用性CSS类");
        assertTrue(html.contains("仅适用于v2.0+"), "❌ 应显示适用性文本");
        assertTrue(html.contains("noteBlock"), "❌ 应包含注释样式");

        System.out.println("✅ TC-04 通过（Note功能保持不变）");
    }

    @Test
    @DisplayName("TC-05: 混合场景 - 三种状况同时存在")
    public void testMixedScenario() throws Exception {
        String xml = buildTestXml(
            "<warning applicRefId=\"app-mix-001\">" +
            "  <warningAndCautionPara>带适用性的警告</warningAndCautionPara>" +
            "</warning>" +
            "<warning>" +
            "  <warningAndCautionPara>不带适用性的警告</warningAndCautionPara>" +
            "</warning>" +
            "<caution applicRefId=\"app-mix-002\">" +
            "  <warningAndCautionPara>带适用性的注意</warningAndCautionPara>" +
            "</caution>" +
            "<note applicRefId=\"app-mix-001\">" +
            "  <notePara>带适用性的注释</notePara>" +
            "</note>",
            "<applic id=\"app-mix-001\">" +
            "  <displayText><simplePara>适用于配置A</simplePara></displayText>" +
            "</applic>" +
            "<applic id=\"app-mix-002\">" +
            "  <displayText><simplePara>适用于配置B</simplePara></displayText>" +
            "</applic>"
        );

        String html = transformToHtml(xml);

        System.out.println("================================================================================");
        System.out.println("TC-05: 混合场景");
        System.out.println("================================================================================");
        System.out.println(html);
        System.out.println("================================================================================");

        // 断言：检查所有元素
        assertTrue(html.contains("warningBGImage"), "❌ 应包含警告");
        assertTrue(html.contains("cautionBGImage"), "❌ 应包含注意");
        assertTrue(html.contains("noteBlock"), "❌ 应包含注释");

        // 统计适用性出现次数（应该是3次：Warning1 + Caution1 + Note1）
        int applicCount = countOccurrences(html, "applicAnnotation");
        assertEquals(3, applicCount, "❌ 应有3个适用性标注");

        assertTrue(html.contains("适用于配置A"), "❌ 应包含配置A");
        assertTrue(html.contains("适用于配置B"), "❌ 应包含配置B");

        System.out.println("✅ TC-05 通过");
        System.out.println("   - 适用性标注数量：" + applicCount);
    }

    // ==================== 辅助方法 ====================

    /**
     * 构建测试用的完整XML
     */
    private String buildTestXml(String content, String applic) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
               "<dmodule>" +
               "  <identAndStatusSection>" +
               "    <dmAddress>" +
               "      <dmIdent>" +
               "        <dmCode modelIdentCode=\"TEST\" systemDiffCode=\"A\" systemCode=\"00\" " +
               "                subSystemCode=\"0\" subSubSystemCode=\"0\" assyCode=\"00\" " +
               "                disassyCode=\"00\" disassyCodeVariant=\"A\" infoCode=\"000\" " +
               "                infoCodeVariant=\"A\" itemLocationCode=\"A\"/>" +
               "      </dmIdent>" +
               "    </dmAddress>" +
               "    <dmAddressItems>" +
               "      <issueDate year=\"2026\" month=\"09\" day=\"18\"/>" +
               "      <language languageIsoCode=\"zh\" countryIsoCode=\"CN\"/>" +
               "    </dmAddressItems>" +
               "  </identAndStatusSection>" +
               "  <content>" +
               (applic.isEmpty() ? "" : "    <applicReposit>" + applic + "</applicReposit>") +
               "    <description>" +
               "      <levelledPara>" +
               "        <title>测试内容</title>" +
               "        " + content +
               "      </levelledPara>" +
               "    </description>" +
               "  </content>" +
               "</dmodule>";
    }

    /**
     * 使用XSLT转换XML为HTML
     */
    private String transformToHtml(String xml) throws Exception {
        // 加载XSLT文件
        InputStream xslStream = getClass().getClassLoader()
            .getResourceAsStream("ietm/S1000D40/xsl/base.xsl");

        if (xslStream == null) {
            throw new RuntimeException("无法加载XSLT文件: ietm/S1000D40/xsl/base.xsl");
        }

        // 创建转换器
        TransformerFactory factory = TransformerFactory.newInstance();
        Source xslSource = new StreamSource(xslStream);
        Transformer transformer = factory.newTransformer(xslSource);

        // 设置参数
        transformer.setParameter("Publication", "TEST");
        transformer.setParameter("cssDir", "");

        // 执行转换
        Source xmlSource = new StreamSource(new StringReader(xml));
        StringWriter writer = new StringWriter();
        Result result = new StreamResult(writer);

        transformer.transform(xmlSource, result);

        return writer.toString();
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
