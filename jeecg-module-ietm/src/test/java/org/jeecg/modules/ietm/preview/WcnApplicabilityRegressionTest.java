package org.jeecg.modules.ietm.preview;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Warning/Caution适用性修复 - 回归测试
 *
 * 测试目标：
 * 1. 验证带applicRefId的warning显示适用性
 * 2. 验证带applicRefId的caution显示适用性
 * 3. 验证不带applicRefId的warning/caution不显示适用性
 * 4. 验证note的适用性未受影响
 * 5. 验证向后兼容性
 */
@SpringBootTest
@DisplayName("状况模块适用性回归测试")
public class WcnApplicabilityRegressionTest {

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

        System.out.println("TC-01 输出HTML:\n" + html);

        // 断言：应包含适用性信息
        assertTrue(html.contains("applicAnnotation"), "应包含适用性CSS类");
        assertTrue(html.contains("仅适用于A型飞机"), "应显示适用性文本");
        assertTrue(html.contains("warningBGImage"), "应包含警告样式");
        assertTrue(html.contains("警"), "应包含警告标题");
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

        System.out.println("TC-02 输出HTML:\n" + html);

        // 断言：不应包含适用性信息
        assertFalse(html.contains("applicAnnotation"), "不应包含适用性CSS类");
        assertFalse(html.contains("适用性"), "不应显示适用性文本");
        assertTrue(html.contains("warningBGImage"), "应包含警告样式");
        assertTrue(html.contains("这是一个不带适用性的警告"), "应显示警告内容");
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

        System.out.println("TC-03 输出HTML:\n" + html);

        // 断言：应包含适用性信息
        assertTrue(html.contains("applicAnnotation"), "应包含适用性CSS类");
        assertTrue(html.contains("仅适用于沙漠型设备"), "应显示适用性文本");
        assertTrue(html.contains("cautionBGImage"), "应包含注意样式");
        assertTrue(html.contains("注"), "应包含注意标题");
    }

    @Test
    @DisplayName("TC-04: 不带适用性的Caution显示正确")
    public void testCautionWithoutApplicability() throws Exception {
        String xml = buildTestXml(
            "<caution>" +
            "  <warningAndCautionPara>这是一个不带适用性的注意</warningAndCautionPara>" +
            "</caution>",
            ""
        );

        String html = transformToHtml(xml);

        System.out.println("TC-04 输出HTML:\n" + html);

        // 断言：不应包含适用性信息
        assertFalse(html.contains("applicAnnotation"), "不应包含适用性CSS类");
        assertFalse(html.contains("适用性"), "不应显示适用性文本");
        assertTrue(html.contains("cautionBGImage"), "应包含注意样式");
    }

    @Test
    @DisplayName("TC-05: Note的适用性未受影响")
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

        System.out.println("TC-05 输出HTML:\n" + html);

        // 断言：Note应保持原有行为
        assertTrue(html.contains("applicAnnotation"), "应包含适用性CSS类");
        assertTrue(html.contains("仅适用于v2.0+"), "应显示适用性文本");
        assertTrue(html.contains("noteBlock"), "应包含注释样式");
        assertTrue(html.contains("[注]"), "应包含注释标题");
    }

    @Test
    @DisplayName("TC-06: 混合场景 - 多种状况同时存在")
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

        System.out.println("TC-06 输出HTML:\n" + html);

        // 断言：检查所有元素
        assertTrue(html.contains("warningBGImage"), "应包含警告");
        assertTrue(html.contains("cautionBGImage"), "应包含注意");
        assertTrue(html.contains("noteBlock"), "应包含注释");

        // 统计适用性出现次数（应该是3次：Warning1 + Caution1 + Note1）
        int applicCount = countOccurrences(html, "applicAnnotation");
        assertEquals(3, applicCount, "应有3个适用性标注");

        assertTrue(html.contains("适用于配置A"), "应包含配置A");
        assertTrue(html.contains("适用于配置B"), "应包含配置B");
    }

    @Test
    @DisplayName("TC-07: 适用性文本格式检查")
    public void testApplicabilityTextFormat() throws Exception {
        String xml = buildTestXml(
            "<warning applicRefId=\"app-format-001\">" +
            "  <warningAndCautionPara>格式测试</warningAndCautionPara>" +
            "</warning>",
            "<applic id=\"app-format-001\">" +
            "  <displayText><simplePara>测试适用性</simplePara></displayText>" +
            "</applic>"
        );

        String html = transformToHtml(xml);

        System.out.println("TC-07 输出HTML:\n" + html);

        // 断言：检查"适用性:"前缀
        assertTrue(html.contains("适用性"), "应包含'适用性'前缀");

        // 提取适用性文本
        int start = html.indexOf("applicAnnotation");
        if (start != -1) {
            String applicSection = html.substring(start, Math.min(start + 200, html.length()));
            System.out.println("适用性区域: " + applicSection);

            // 验证格式：应该是 "适用性: 测试适用性" 或类似格式
            assertTrue(applicSection.contains("测试适用性"), "应包含适用性内容");
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 构建测试用的完整XML
     */
    private String buildTestXml(String content, String applic) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
               "<dmodule xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
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
        String xslPath = "classpath:ietm/S1000D40/xsl/base.xsl";

        // 创建转换器
        TransformerFactory factory = TransformerFactory.newInstance();
        Source xslSource = new StreamSource(
            getClass().getClassLoader().getResourceAsStream("ietm/S1000D40/xsl/base.xsl")
        );
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
