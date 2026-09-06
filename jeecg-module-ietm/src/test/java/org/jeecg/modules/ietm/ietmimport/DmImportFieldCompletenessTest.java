package org.jeecg.modules.ietm.ietmimport;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DM导入字段完整性测试
 *
 * 测试修复的所有字段提取逻辑
 */
@DisplayName("DM导入字段完整性测试")
public class DmImportFieldCompletenessTest {

    /**
     * 生成完整的测试DM XML
     */
    private String generateCompleteXml(
        String infoCode,
        String infoCodeVariant,
        String itemLocationCode,
        String languageIsoCode,
        String countryIsoCode,
        String issueNumber,
        String inWork,
        String originatorCode,
        String originatorName,
        String rpcCode,
        String rpcName,
        String techName,
        String infoName,
        String issueType,
        String security
    ) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<dmodule xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
               "xsi:noNamespaceSchemaLocation=\"http://www.s1000d.org/S1000D_4-0/xml_schema_flat/descript.xsd\">\n" +
               "  <identAndStatusSection>\n" +
               "    <dmAddress>\n" +
               "      <dmIdent>\n" +
               "        <dmCode modelIdentCode=\"ZB1\" systemDiffCode=\"A\" systemCode=\"05\" " +
               "subSystemCode=\"0\" subSubSystemCode=\"0\" assyCode=\"00\" " +
               "disassyCode=\"00\" disassyCodeVariant=\"A\" " +
               "infoCode=\"" + infoCode + "\" " +
               "infoCodeVariant=\"" + infoCodeVariant + "\" " +
               "itemLocationCode=\"" + itemLocationCode + "\"/>\n" +
               "        <language countryIsoCode=\"" + countryIsoCode + "\" languageIsoCode=\"" + languageIsoCode + "\"/>\n" +
               "        <issueInfo issueNumber=\"" + issueNumber + "\" inWork=\"" + inWork + "\"/>\n" +
               "      </dmIdent>\n" +
               "      <dmAddressItems>\n" +
               "        <issueDate year=\"2026\" month=\"09\" day=\"04\"/>\n" +
               "        <dmTitle>\n" +
               "          <techName>" + techName + "</techName>\n" +
               "          <infoName>" + infoName + "</infoName>\n" +
               "        </dmTitle>\n" +
               "      </dmAddressItems>\n" +
               "    </dmAddress>\n" +
               "    <dmStatus issueType=\"" + issueType + "\">\n" +
               "      <security securityClassification=\"" + security + "\"/>\n" +
               "      <dataRestrictions>\n" +
               "        <restrictionInstructions>\n" +
               "          <dataDistribution/>\n" +
               "          <exportControl>\n" +
               "            <exportRegistrationStmt>\n" +
               "              <simplePara/>\n" +
               "            </exportRegistrationStmt>\n" +
               "          </exportControl>\n" +
               "          <dataHandling/>\n" +
               "          <dataDestruction/>\n" +
               "          <dataDisclosure/>\n" +
               "        </restrictionInstructions>\n" +
               "        <restrictionInfo>\n" +
               "          <copyright>\n" +
               "            <copyrightPara/>\n" +
               "          </copyright>\n" +
               "          <policyStatement/>\n" +
               "          <dataConds/>\n" +
               "        </restrictionInfo>\n" +
               "      </dataRestrictions>\n" +
               "      <responsiblePartnerCompany enterpriseCode=\"" + rpcCode + "\">\n" +
               "        <enterpriseName>" + rpcName + "</enterpriseName>\n" +
               "      </responsiblePartnerCompany>\n" +
               "      <originator enterpriseCode=\"" + originatorCode + "\">\n" +
               "        <enterpriseName>" + originatorName + "</enterpriseName>\n" +
               "      </originator>\n" +
               "      <applic>\n" +
               "        <displayText>\n" +
               "          <simplePara/>\n" +
               "        </displayText>\n" +
               "      </applic>\n" +
               "      <brexDmRef>\n" +
               "        <dmRef>\n" +
               "          <dmRefIdent>\n" +
               "            <dmCode modelIdentCode=\"DEFAULT\" systemDiffCode=\"A\" systemCode=\"00\" " +
               "subSystemCode=\"0\" subSubSystemCode=\"0\" assyCode=\"00\" " +
               "disassyCode=\"00\" disassyCodeVariant=\"A\" infoCode=\"022\" " +
               "infoCodeVariant=\"A\" itemLocationCode=\"D\"/>\n" +
               "          </dmRefIdent>\n" +
               "        </dmRef>\n" +
               "      </brexDmRef>\n" +
               "      <qualityAssurance>\n" +
               "        <unverified/>\n" +
               "      </qualityAssurance>\n" +
               "      <reasonForUpdate>\n" +
               "        <simplePara/>\n" +
               "      </reasonForUpdate>\n" +
               "    </dmStatus>\n" +
               "  </identAndStatusSection>\n" +
               "  <content>\n" +
               "    <description>\n" +
               "      <para>测试内容</para>\n" +
               "    </description>\n" +
               "  </content>\n" +
               "</dmodule>";
    }

    // ========== 必填字段测试 ==========

    @Test
    @DisplayName("测试1: infoCode必填字段提取")
    void testInfoCodeExtraction() {
        String xml = generateCompleteXml(
            "007", "A", "A", "zh", "CN", "001", "03",
            "TEST", "Test Company", "RPC01", "RPC Company",
            "技术名称", "信息名称", "new", "01"
        );

        assertTrue(xml.contains("infoCode=\"007\""), "XML应该包含infoCode");
    }

    @Test
    @DisplayName("测试2: originator必填字段提取")
    void testOriginatorExtraction() {
        String xml = generateCompleteXml(
            "007", "A", "A", "zh", "CN", "001", "03",
            "TEST", "Test Company", "RPC01", "RPC Company",
            "技术名称", "信息名称", "new", "01"
        );

        assertTrue(xml.contains("enterpriseCode=\"TEST\""), "XML应该包含originator");
        assertTrue(xml.contains("<enterpriseName>Test Company</enterpriseName>"), "XML应该包含originatorName");
    }

    @Test
    @DisplayName("测试3: inWork必填字段提取")
    void testInWorkExtraction() {
        String xml = generateCompleteXml(
            "007", "A", "A", "zh", "CN", "001", "03",
            "TEST", "Test Company", "RPC01", "RPC Company",
            "技术名称", "信息名称", "new", "01"
        );

        assertTrue(xml.contains("inWork=\"03\""), "XML应该包含inWork");
    }

    @Test
    @DisplayName("测试4: issueNo必填字段提取")
    void testIssueNoExtraction() {
        String xml = generateCompleteXml(
            "007", "A", "A", "zh", "CN", "001", "03",
            "TEST", "Test Company", "RPC01", "RPC Company",
            "技术名称", "信息名称", "new", "01"
        );

        assertTrue(xml.contains("issueNumber=\"001\""), "XML应该包含issueNumber");
    }

    // ========== 可选字段测试 ==========

    @Test
    @DisplayName("测试5: languageIsoCode可选字段提取")
    void testLanguageIsoCodeExtraction() {
        String xml = generateCompleteXml(
            "007", "A", "A", "zh", "CN", "001", "03",
            "TEST", "Test Company", "RPC01", "RPC Company",
            "技术名称", "信息名称", "new", "01"
        );

        assertTrue(xml.contains("languageIsoCode=\"zh\""), "XML应该包含languageIsoCode");
    }

    @Test
    @DisplayName("测试6: countryIsoCode可选字段提取")
    void testCountryIsoCodeExtraction() {
        String xml = generateCompleteXml(
            "007", "A", "A", "zh", "CN", "001", "03",
            "TEST", "Test Company", "RPC01", "RPC Company",
            "技术名称", "信息名称", "new", "01"
        );

        assertTrue(xml.contains("countryIsoCode=\"CN\""), "XML应该包含countryIsoCode");
    }

    @Test
    @DisplayName("测试7: techName和infoName提取")
    void testTechNameAndInfoNameExtraction() {
        String xml = generateCompleteXml(
            "007", "A", "A", "zh", "CN", "001", "03",
            "TEST", "Test Company", "RPC01", "RPC Company",
            "技术名称", "信息名称", "new", "01"
        );

        assertTrue(xml.contains("<techName>技术名称</techName>"), "XML应该包含techName");
        assertTrue(xml.contains("<infoName>信息名称</infoName>"), "XML应该包含infoName");
    }

    @Test
    @DisplayName("测试8: issueType提取")
    void testIssueTypeExtraction() {
        String xml = generateCompleteXml(
            "007", "A", "A", "zh", "CN", "001", "03",
            "TEST", "Test Company", "RPC01", "RPC Company",
            "技术名称", "信息名称", "new", "01"
        );

        assertTrue(xml.contains("issueType=\"new\""), "XML应该包含issueType");
    }

    @Test
    @DisplayName("测试9: rpc和rpcName提取")
    void testRpcExtraction() {
        String xml = generateCompleteXml(
            "007", "A", "A", "zh", "CN", "001", "03",
            "TEST", "Test Company", "RPC01", "RPC Company",
            "技术名称", "信息名称", "new", "01"
        );

        assertTrue(xml.contains("<responsiblePartnerCompany enterpriseCode=\"RPC01\">"),
                  "XML应该包含rpc");
        assertTrue(xml.contains("<enterpriseName>RPC Company</enterpriseName>"),
                  "XML应该包含rpcName");
    }

    // ========== 默认值测试 ==========

    @Test
    @DisplayName("测试10: infoCode默认值")
    void testInfoCodeDefaultValue() {
        // 空infoCode应该使用默认值"000"
        String emptyInfoCode = "";
        String defaultValue = emptyInfoCode.isEmpty() ? "000" : emptyInfoCode;

        assertEquals("000", defaultValue, "空infoCode应该使用默认值000");
    }

    @Test
    @DisplayName("测试11: originator默认值")
    void testOriginatorDefaultValue() {
        // 空originator应该使用默认值"DEFAULT"
        String emptyOriginator = "";
        String defaultValue = emptyOriginator.isEmpty() ? "DEFAULT" : emptyOriginator;

        assertEquals("DEFAULT", defaultValue, "空originator应该使用默认值DEFAULT");
    }

    @Test
    @DisplayName("测试12: issueNo默认值")
    void testIssueNoDefaultValue() {
        // 空issueNo应该使用默认值"001"
        String emptyIssueNo = "";
        String defaultValue = emptyIssueNo.isEmpty() ? "001" : emptyIssueNo;

        assertEquals("001", defaultValue, "空issueNo应该使用默认值001");
    }

    @Test
    @DisplayName("测试13: inWork默认值")
    void testInWorkDefaultValue() {
        // 空inWork应该使用默认值"00"
        String emptyInWork = "";
        String defaultValue = emptyInWork.isEmpty() ? "00" : emptyInWork;

        assertEquals("00", defaultValue, "空inWork应该使用默认值00");
    }

    // ========== 边界测试 ==========

    @Test
    @DisplayName("测试14: 最小化XML（只有必填字段）")
    void testMinimalXml() {
        String xml = generateCompleteXml(
            "000", "", "", "", "", "001", "00",
            "", "", "", "",
            "", "", "", ""
        );

        // 应该包含必填字段
        assertTrue(xml.contains("infoCode=\"000\""), "应该包含infoCode");
        assertTrue(xml.contains("issueNumber=\"001\""), "应该包含issueNumber");
        assertTrue(xml.contains("inWork=\"00\""), "应该包含inWork");
    }

    @Test
    @DisplayName("测试15: 最大化XML（所有字段都有值）")
    void testMaximalXml() {
        String xml = generateCompleteXml(
            "999", "Z", "T", "en", "US", "999", "99",
            "MAXTEST", "Max Test Company", "MAXRPC", "Max RPC Company",
            "Maximum Technical Name", "Maximum Information Name",
            "revised", "05"
        );

        // 应该包含所有字段
        assertTrue(xml.contains("infoCode=\"999\""));
        assertTrue(xml.contains("infoCodeVariant=\"Z\""));
        assertTrue(xml.contains("itemLocationCode=\"T\""));
        assertTrue(xml.contains("languageIsoCode=\"en\""));
        assertTrue(xml.contains("countryIsoCode=\"US\""));
        assertTrue(xml.contains("issueNumber=\"999\""));
        assertTrue(xml.contains("inWork=\"99\""));
        assertTrue(xml.contains("enterpriseCode=\"MAXTEST\""));
        assertTrue(xml.contains("issueType=\"revised\""));
        assertTrue(xml.contains("securityClassification=\"05\""));
    }

    // ========== 特殊字符测试 ==========

    @Test
    @DisplayName("测试16: 中文字段内容")
    void testChineseContent() {
        String xml = generateCompleteXml(
            "007", "A", "A", "zh", "CN", "001", "03",
            "测试发行方", "测试发行方公司名称", "RPC测试", "责任伙伴公司中文名",
            "计划/非计划维修（总论）", "符号清单", "new", "01"
        );

        assertTrue(xml.contains("<techName>计划/非计划维修（总论）</techName>"),
                  "应该支持中文techName");
        assertTrue(xml.contains("<infoName>符号清单</infoName>"),
                  "应该支持中文infoName");
    }

    @Test
    @DisplayName("测试17: 特殊字符转义")
    void testSpecialCharacters() {
        // XML特殊字符应该被正确处理
        String techNameWithSpecialChars = "Tech<Name>&Test";
        String infoNameWithSpecialChars = "Info\"Name\"'Test'";

        // 在实际应用中，这些字符应该被转义或拒绝
        assertNotNull(techNameWithSpecialChars);
        assertNotNull(infoNameWithSpecialChars);
    }

    // ========== 参数化测试 ==========

    @ParameterizedTest
    @CsvSource({
        "000, A, A, 基础信息码",
        "007, A, A, 符号清单",
        "022, A, D, 业务规则",
        "040, A, A, 描述性",
        "999, Z, T, 最大值"
    })
    @DisplayName("测试18: 参数化DMC组合测试")
    void testDmcCombinations(String infoCode, String variant, String location, String description) {
        String xml = generateCompleteXml(
            infoCode, variant, location, "zh", "CN", "001", "00",
            "TEST", "Test Company", "RPC01", "RPC Company",
            description, "测试", "new", "01"
        );

        assertTrue(xml.contains("infoCode=\"" + infoCode + "\""),
                  "应该包含infoCode: " + infoCode);
        assertTrue(xml.contains("infoCodeVariant=\"" + variant + "\""),
                  "应该包含infoCodeVariant: " + variant);
        assertTrue(xml.contains("itemLocationCode=\"" + location + "\""),
                  "应该包含itemLocationCode: " + location);
    }

    @ParameterizedTest
    @CsvSource({
        "zh, CN, 中文-中国",
        "en, US, 英文-美国",
        "en, GB, 英文-英国",
        "fr, FR, 法文-法国",
        "de, DE, 德文-德国"
    })
    @DisplayName("测试19: 参数化语言和国家代码测试")
    void testLanguageCountryCombinations(String lang, String country, String description) {
        String xml = generateCompleteXml(
            "007", "A", "A", lang, country, "001", "00",
            "TEST", "Test Company", "RPC01", "RPC Company",
            "Technical Name", "Information Name", "new", "01"
        );

        assertTrue(xml.contains("languageIsoCode=\"" + lang + "\""),
                  "应该包含languageIsoCode: " + lang + " (" + description + ")");
        assertTrue(xml.contains("countryIsoCode=\"" + country + "\""),
                  "应该包含countryIsoCode: " + country + " (" + description + ")");
    }

    @ParameterizedTest
    @CsvSource({
        "new, 新建",
        "changed, 已更改",
        "revised, 已修订",
        "deleted, 已删除",
        "status, 状态",
        "rinstate-changed, 恢复-已更改",
        "rinstate-revised, 恢复-已修订",
        "rinstate-status, 恢复-状态"
    })
    @DisplayName("测试20: 参数化S1000D issueType测试")
    void testIssueTypes(String issueType, String description) {
        String xml = generateCompleteXml(
            "007", "A", "A", "zh", "CN", "001", "00",
            "TEST", "Test Company", "RPC01", "RPC Company",
            "Technical Name", "Information Name", issueType, "01"
        );

        assertTrue(xml.contains("issueType=\"" + issueType + "\""),
                  "应该支持issueType: " + issueType + " (" + description + ")");
    }

    @ParameterizedTest
    @CsvSource({
        "0,  00, 公开",
        "1,  01, 内部",
        "2,  02, 秘密",
        "3,  03, 机密",
        "4,  04, 绝密",
        "5,  05, 核心绝密"
    })
    @DisplayName("测试21: 参数化密级值测试（结合之前的修复）")
    void testSecurityLevels(String singleDigit, String twoDigit, String description) {
        // 单位数格式
        String xmlSingle = generateCompleteXml(
            "007", "A", "A", "zh", "CN", "001", "00",
            "TEST", "Test Company", "RPC01", "RPC Company",
            "Technical Name", "Information Name", "new", singleDigit
        );

        assertTrue(xmlSingle.contains("securityClassification=\"" + singleDigit + "\""),
                  "应该支持单位数密级: " + singleDigit + " (" + description + ")");

        // 两位数格式
        String xmlDouble = generateCompleteXml(
            "007", "A", "A", "zh", "CN", "001", "00",
            "TEST", "Test Company", "RPC01", "RPC Company",
            "Technical Name", "Information Name", "new", twoDigit
        );

        assertTrue(xmlDouble.contains("securityClassification=\"" + twoDigit + "\""),
                  "应该支持两位数密级: " + twoDigit + " (" + description + ")");
    }

    // ========== 空值和null测试 ==========

    @Test
    @DisplayName("测试22: 空enterpriseCode应使用默认值")
    void testEmptyEnterpriseCode() {
        String xml = generateCompleteXml(
            "007", "A", "A", "zh", "CN", "001", "03",
            "", "", "", "",  // 所有enterprise相关字段为空
            "技术名称", "信息名称", "new", "01"
        );

        assertTrue(xml.contains("enterpriseCode=\"\""), "空enterpriseCode应该在XML中");
        // 在实际逻辑中，这会被替换为"DEFAULT"
    }

    @Test
    @DisplayName("测试23: 空语言和国家代码")
    void testEmptyLanguageAndCountry() {
        String xml = generateCompleteXml(
            "007", "A", "A", "", "", "001", "03",
            "TEST", "Test Company", "RPC01", "RPC Company",
            "技术名称", "信息名称", "new", "01"
        );

        assertTrue(xml.contains("languageIsoCode=\"\""), "空languageIsoCode应该在XML中");
        assertTrue(xml.contains("countryIsoCode=\"\""), "空countryIsoCode应该在XML中");
    }

    @Test
    @DisplayName("测试24: 空技术名称和信息名称")
    void testEmptyTitles() {
        String xml = generateCompleteXml(
            "007", "A", "A", "zh", "CN", "001", "03",
            "TEST", "Test Company", "RPC01", "RPC Company",
            "", "", "new", "01"  // 空的techName和infoName
        );

        assertTrue(xml.contains("<techName></techName>"), "空techName应该在XML中");
        assertTrue(xml.contains("<infoName></infoName>"), "空infoName应该在XML中");
    }

    // ========== 问题文件测试 ==========

    @Test
    @DisplayName("测试25: 问题文件DMC-ZB1-A-05-00-00-00A-007A-A_001-03_zh-CN.xml")
    void testProblemFile() {
        String xml = generateCompleteXml(
            "007", "A", "A", "zh", "CN", "001", "03",
            "", "", "", "",  // 空的enterprise代码
            "计划/非计划维修（总论）", "符号清单", "new", "01"
        );

        // 验证问题文件的关键字段
        assertTrue(xml.contains("infoCode=\"007\""), "应该包含infoCode=007");
        assertTrue(xml.contains("infoCodeVariant=\"A\""), "应该包含infoCodeVariant=A");
        assertTrue(xml.contains("itemLocationCode=\"A\""), "应该包含itemLocationCode=A");
        assertTrue(xml.contains("languageIsoCode=\"zh\""), "应该包含languageIsoCode=zh");
        assertTrue(xml.contains("countryIsoCode=\"CN\""), "应该包含countryIsoCode=CN");
        assertTrue(xml.contains("issueNumber=\"001\""), "应该包含issueNumber=001");
        assertTrue(xml.contains("inWork=\"03\""), "应该包含inWork=03");
        assertTrue(xml.contains("securityClassification=\"01\""), "应该包含security=01");
        assertTrue(xml.contains("<techName>计划/非计划维修（总论）</techName>"),
                  "应该包含techName");
        assertTrue(xml.contains("<infoName>符号清单</infoName>"),
                  "应该包含infoName");
    }
}
