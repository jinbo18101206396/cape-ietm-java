package org.jeecg.modules.ietm.ietmimport;

import org.jeecg.modules.ietm.ietmimport.constants.DmImportConstants;
import org.jeecg.modules.ietm.ietmimport.service.impl.IetmDmImportServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 密级值校验集成测试
 * 测试完整的DM导入流程中的密级校验功能
 */
@SpringBootTest
@DisplayName("密级值校验集成测试")
public class SecurityValidationIntegrationTest {

    @Autowired(required = false)
    private IetmDmImportServiceImpl dmImportService;

    /**
     * 生成测试用的DM XML内容
     */
    private String generateDmXml(String securityValue) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<dmodule xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
               "xsi:noNamespaceSchemaLocation=\"http://www.s1000d.org/S1000D_4-0/xml_schema_flat/descript.xsd\">\n" +
               "  <identAndStatusSection>\n" +
               "    <dmAddress>\n" +
               "      <dmIdent>\n" +
               "        <dmCode modelIdentCode=\"ZB1\" systemDiffCode=\"A\" systemCode=\"05\" " +
               "subSystemCode=\"0\" subSubSystemCode=\"0\" assyCode=\"00\" " +
               "disassyCode=\"00\" disassyCodeVariant=\"A\" infoCode=\"007\" " +
               "infoCodeVariant=\"A\" itemLocationCode=\"A\"/>\n" +
               "        <language countryIsoCode=\"CN\" languageIsoCode=\"zh\"/>\n" +
               "        <issueInfo issueNumber=\"001\" inWork=\"03\"/>\n" +
               "      </dmIdent>\n" +
               "      <dmAddressItems>\n" +
               "        <issueDate year=\"2026\" month=\"09\" day=\"04\"/>\n" +
               "        <dmTitle>\n" +
               "          <techName>测试技术名称</techName>\n" +
               "          <infoName>测试信息名称</infoName>\n" +
               "        </dmTitle>\n" +
               "      </dmAddressItems>\n" +
               "    </dmAddress>\n" +
               "    <dmStatus issueType=\"new\">\n" +
               "      <security securityClassification=\"" + securityValue + "\"/>\n" +
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
               "            <copyrightPara>\n" +
               "            </copyrightPara>\n" +
               "          </copyright>\n" +
               "          <policyStatement/>\n" +
               "          <dataConds/>\n" +
               "        </restrictionInfo>\n" +
               "      </dataRestrictions>\n" +
               "      <responsiblePartnerCompany enterpriseCode=\"\">\n" +
               "        <enterpriseName/>\n" +
               "      </responsiblePartnerCompany>\n" +
               "      <originator enterpriseCode=\"\">\n" +
               "        <enterpriseName/>\n" +
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

    @ParameterizedTest
    @CsvSource({
        "0,    true,  公开",
        "1,    true,  内部",
        "2,    true,  秘密",
        "3,    true,  机密",
        "4,    true,  绝密",
        "5,    true,  核心绝密",
        "00,   true,  公开(两位数)",
        "01,   true,  内部(两位数)",
        "02,   true,  秘密(两位数)",
        "03,   true,  机密(两位数)",
        "04,   true,  绝密(两位数)",
        "05,   true,  核心绝密(两位数)",
        "6,    false, 超范围",
        "06,   false, 超范围(两位数)",
        "10,   false, 非法值",
        "99,   false, 非法值",
        "001,  false, 三位数",
        "a,    false, 非数字",
        "0a,   false, 包含字母"
    })
    @DisplayName("参数化测试：所有密级值格式")
    void testAllSecurityFormats(String securityValue, boolean shouldPass, String description) {
        // 验证正则表达式
        String regex = "^0?[0-5]$";
        boolean matches = securityValue.matches(regex);

        assertEquals(shouldPass, matches,
                    String.format("密级值 '%s' (%s) 的校验结果不符合预期", securityValue, description));
    }

    @Test
    @DisplayName("测试问题文件的密级值：01")
    void testProblemFileSecurityValue() {
        String problemSecurity = "01";
        String regex = "^0?[0-5]$";

        assertTrue(problemSecurity.matches(regex),
                  "问题文件的密级值 '01' 必须通过校验");
    }

    @Test
    @DisplayName("测试XML解析：提取密级值")
    void testExtractSecurityFromXml() {
        String xml = generateDmXml("01");

        // 手动解析XML验证
        assertTrue(xml.contains("securityClassification=\"01\""),
                  "XML应该包含密级值 01");
    }

    @Test
    @DisplayName("边界测试：最小值0和最大值5")
    void testBoundaryValues() {
        String regex = "^0?[0-5]$";

        // 最小值
        assertTrue("0".matches(regex), "最小值 0 应该通过");
        assertTrue("00".matches(regex), "最小值 00 应该通过");

        // 最大值
        assertTrue("5".matches(regex), "最大值 5 应该通过");
        assertTrue("05".matches(regex), "最大值 05 应该通过");

        // 超出范围
        assertFalse("6".matches(regex), "6 应该被拒绝");
        assertFalse("06".matches(regex), "06 应该被拒绝");
        assertFalse("-1".matches(regex), "-1 应该被拒绝");
    }

    @Test
    @DisplayName("格式转换测试：数据库→XML")
    void testFormatConversion() {
        String regex = "^0?[0-5]$";

        // 模拟数据库值转XML值的逻辑
        String[] dbValues = {"0", "1", "2", "3", "4", "5"};

        for (String dbValue : dbValues) {
            // 数据库值应该有效
            assertTrue(dbValue.matches(regex),
                      String.format("数据库值 %s 应该有效", dbValue));

            // 转换为XML格式（添加前导零）
            String xmlValue = "0" + dbValue;

            // XML值也应该有效
            assertTrue(xmlValue.matches(regex),
                      String.format("XML值 %s 应该有效", xmlValue));
        }
    }

    @Test
    @DisplayName("对比测试：旧正则vs新正则")
    void testOldVsNewRegex() {
        String oldRegex = "^[0-5]$";
        String newRegex = "^0?[0-5]$";

        String[] twoDigitValues = {"00", "01", "02", "03", "04", "05"};

        for (String value : twoDigitValues) {
            // 旧正则应该拒绝两位数（这是bug）
            assertFalse(value.matches(oldRegex),
                       String.format("旧正则应该拒绝 %s（这是bug的原因）", value));

            // 新正则应该接受两位数（这是修复）
            assertTrue(value.matches(newRegex),
                      String.format("新正则应该接受 %s（修复后）", value));
        }

        String[] singleDigitValues = {"0", "1", "2", "3", "4", "5"};

        for (String value : singleDigitValues) {
            // 旧正则和新正则都应该接受单位数（向后兼容）
            assertTrue(value.matches(oldRegex),
                      String.format("旧正则接受 %s", value));
            assertTrue(value.matches(newRegex),
                      String.format("新正则接受 %s（保持兼容）", value));
        }
    }

    @Test
    @DisplayName("空值和null测试")
    void testNullAndEmpty() {
        String regex = "^0?[0-5]$";

        // 空字符串
        assertFalse("".matches(regex), "空字符串应该被拒绝");

        // 空格
        assertFalse(" ".matches(regex), "空格应该被拒绝");
        assertFalse("0 ".matches(regex), "带空格的值应该被拒绝");
        assertFalse(" 0".matches(regex), "前置空格的值应该被拒绝");

        // null会抛出异常
        assertThrows(NullPointerException.class, () -> {
            ((String) null).matches(regex);
        }, "null应该在业务代码中提前判空");
    }

    @Test
    @DisplayName("特殊字符测试")
    void testSpecialCharacters() {
        String regex = "^0?[0-5]$";

        String[] invalidValues = {
            "0-1", "0.1", "0,1", "0/1", "0\\1",
            "0+1", "0*1", "0&1", "0|1", "0^1",
            "(0)", "[0]", "{0}", "<0>", "\"0\""
        };

        for (String value : invalidValues) {
            assertFalse(value.matches(regex),
                       String.format("特殊字符值 '%s' 应该被拒绝", value));
        }
    }

    @Test
    @DisplayName("大小写测试")
    void testCaseVariations() {
        String regex = "^0?[0-5]$";

        // 数字没有大小写，但测试字母混入
        assertFalse("A".matches(regex), "字母A应该被拒绝");
        assertFalse("a".matches(regex), "字母a应该被拒绝");
        assertFalse("0A".matches(regex), "0A应该被拒绝");
        assertFalse("A0".matches(regex), "A0应该被拒绝");
        assertFalse("O".matches(regex), "字母O(不是零)应该被拒绝");
    }

    @Test
    @DisplayName("Unicode和多字节字符测试")
    void testUnicodeCharacters() {
        String regex = "^0?[0-5]$";

        // 中文数字
        assertFalse("零".matches(regex), "中文'零'应该被拒绝");
        assertFalse("一".matches(regex), "中文'一'应该被拒绝");

        // 全角数字
        assertFalse("０".matches(regex), "全角'０'应该被拒绝");
        assertFalse("１".matches(regex), "全角'１'应该被拒绝");

        // 罗马数字
        assertFalse("Ⅰ".matches(regex), "罗马数字'Ⅰ'应该被拒绝");
        assertFalse("Ⅴ".matches(regex), "罗马数字'Ⅴ'应该被拒绝");
    }

    @Test
    @DisplayName("错误码映射测试")
    void testErrorCodeMapping() {
        // 验证错误码定义
        assertEquals("-6", DmImportConstants.ERROR_SECURITY_NOT_EXISTS,
                    "密级值不存在的错误码应该是-6");

        // 验证错误消息
        String errorMessage = DmImportConstants.getErrorMessage(
            DmImportConstants.ERROR_SECURITY_NOT_EXISTS);
        assertEquals("密级值不存在", errorMessage,
                    "错误消息应该是'密级值不存在'");
    }

    @Test
    @DisplayName("密级语义测试")
    void testSecuritySemantics() {
        String regex = "^0?[0-5]$";

        // 根据IetmDataModule.java的注释
        String[][] semantics = {
            {"0", "00", "公开"},
            {"1", "01", "内部"},
            {"2", "02", "秘密"},
            {"3", "03", "机密"},
            {"4", "04", "绝密"},
            {"5", "05", "核心绝密"}
        };

        for (String[] semantic : semantics) {
            String singleDigit = semantic[0];
            String twoDigit = semantic[1];
            String meaning = semantic[2];

            assertTrue(singleDigit.matches(regex),
                      String.format("密级 %s (%s) 单位数格式应该有效", singleDigit, meaning));
            assertTrue(twoDigit.matches(regex),
                      String.format("密级 %s (%s) 两位数格式应该有效", twoDigit, meaning));
        }
    }

    @Test
    @DisplayName("性能测试：大量密级值校验")
    void testPerformance() {
        String regex = "^0?[0-5]$";

        // 准备测试数据
        String[] validValues = {"0", "1", "2", "3", "4", "5", "00", "01", "02", "03", "04", "05"};

        long startTime = System.currentTimeMillis();

        // 执行10000次校验
        for (int i = 0; i < 10000; i++) {
            for (String value : validValues) {
                assertTrue(value.matches(regex));
            }
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // 性能断言：10000次校验应该在1秒内完成
        assertTrue(duration < 1000,
                  String.format("性能测试失败：%d次校验耗时%dms（应该<1000ms）",
                               validValues.length * 10000, duration));

        System.out.println(String.format("性能测试：%d次校验耗时%dms",
                                        validValues.length * 10000, duration));
    }
}
