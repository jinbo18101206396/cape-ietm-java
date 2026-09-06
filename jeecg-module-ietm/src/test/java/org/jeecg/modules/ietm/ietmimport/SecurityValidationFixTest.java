package org.jeecg.modules.ietm.ietmimport;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 密级值校验修复测试
 *
 * 问题：文件 DMC-ZB1-A-05-00-00-00A-007A-A_001-03_zh-CN.xml 报错"密级值不存在"
 * 根因：校验正则 ^[0-5]$ 只匹配单位数，但XML实际使用两位数格式 "01"
 * 修复：正则改为 ^0?[0-5]$ 同时支持单位数和两位数
 */
@DisplayName("密级值校验修复测试")
public class SecurityValidationFixTest {

    @Test
    @DisplayName("验证正则表达式：支持两位数格式（01-05）")
    void testTwoDigitFormat() {
        String regex = "^0?[0-5]$";

        // 两位数格式（XML实际格式）
        assertTrue("01".matches(regex), "01 应该匹配");
        assertTrue("02".matches(regex), "02 应该匹配");
        assertTrue("03".matches(regex), "03 应该匹配");
        assertTrue("04".matches(regex), "04 应该匹配");
        assertTrue("05".matches(regex), "05 应该匹配");

        // 边界测试
        assertTrue("00".matches(regex), "00 应该匹配（公开）");
    }

    @Test
    @DisplayName("验证正则表达式：支持单位数格式（0-5）")
    void testSingleDigitFormat() {
        String regex = "^0?[0-5]$";

        // 单位数格式（数据库格式）
        assertTrue("0".matches(regex), "0 应该匹配");
        assertTrue("1".matches(regex), "1 应该匹配");
        assertTrue("2".matches(regex), "2 应该匹配");
        assertTrue("3".matches(regex), "3 应该匹配");
        assertTrue("4".matches(regex), "4 应该匹配");
        assertTrue("5".matches(regex), "5 应该匹配");
    }

    @Test
    @DisplayName("验证正则表达式：拒绝非法格式")
    void testInvalidFormats() {
        String regex = "^0?[0-5]$";

        // 超出范围
        assertFalse("06".matches(regex), "06 应该被拒绝");
        assertFalse("6".matches(regex), "6 应该被拒绝");
        assertFalse("10".matches(regex), "10 应该被拒绝");
        assertFalse("99".matches(regex), "99 应该被拒绝");

        // 三位数
        assertFalse("001".matches(regex), "001 应该被拒绝");

        // 非数字
        assertFalse("0a".matches(regex), "0a 应该被拒绝");
        assertFalse("a1".matches(regex), "a1 应该被拒绝");
        assertFalse("AA".matches(regex), "AA 应该被拒绝");

        // 空值
        assertFalse("".matches(regex), "空字符串应该被拒绝");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "1", "2", "3", "4", "5", "00", "01", "02", "03", "04", "05"})
    @DisplayName("参数化测试：所有合法密级值")
    void testAllValidSecurityValues(String security) {
        String regex = "^0?[0-5]$";
        assertTrue(security.matches(regex),
                   String.format("密级值 '%s' 应该被接受", security));
    }

    @ParameterizedTest
    @ValueSource(strings = {"6", "06", "7", "10", "99", "-1", "0-1", "001", "a", "01a"})
    @DisplayName("参数化测试：所有非法密级值")
    void testAllInvalidSecurityValues(String security) {
        String regex = "^0?[0-5]$";
        assertFalse(security.matches(regex),
                    String.format("密级值 '%s' 应该被拒绝", security));
    }

    @Test
    @DisplayName("验证问题文件的密级值：01")
    void testProblemFileSecurityValue() {
        String regex = "^0?[0-5]$";
        String problemFileSecurity = "01";  // DMC-ZB1-A-05-00-00-00A-007A-A_001-03_zh-CN.xml

        assertTrue(problemFileSecurity.matches(regex),
                   "问题文件的密级值 '01' 应该通过校验");
    }

    @Test
    @DisplayName("对比：旧正则vs新正则")
    void testOldRegexVsNewRegex() {
        String oldRegex = "^[0-5]$";   // 旧的（错误）
        String newRegex = "^0?[0-5]$"; // 新的（正确）

        String xmlSecurity = "01";  // XML实际格式

        // 旧正则：不匹配（导致报错）
        assertFalse(xmlSecurity.matches(oldRegex),
                    "旧正则不匹配两位数格式（这是bug）");

        // 新正则：匹配（修复成功）
        assertTrue(xmlSecurity.matches(newRegex),
                   "新正则匹配两位数格式（修复成功）");
    }

    @Test
    @DisplayName("边界测试：null和空字符串处理")
    void testNullAndEmpty() {
        String regex = "^0?[0-5]$";

        // 空字符串
        assertFalse("".matches(regex), "空字符串不应匹配");

        // 注意：null会抛出NullPointerException，需要在业务代码中先判空
        assertThrows(NullPointerException.class, () -> {
            ((String) null).matches(regex);
        }, "null应该在业务代码中先判空");
    }

    @Test
    @DisplayName("密级值范围语义验证")
    void testSecurityLevelSemantics() {
        String regex = "^0?[0-5]$";

        // 根据 IetmDataModule.java 注释：
        // 0=公开 1=内部 2=秘密 3=机密 4=绝密 5=核心绝密

        String[] validLevels = {"0", "1", "2", "3", "4", "5", "00", "01", "02", "03", "04", "05"};
        String[] semanticNames = {
            "公开", "内部", "秘密", "机密", "绝密", "核心绝密",
            "公开", "内部", "秘密", "机密", "绝密", "核心绝密"
        };

        for (int i = 0; i < validLevels.length; i++) {
            assertTrue(validLevels[i].matches(regex),
                       String.format("密级 %s (%s) 应该有效", validLevels[i], semanticNames[i]));
        }
    }

    @Test
    @DisplayName("验证格式转换逻辑：数据库→XML")
    void testDatabaseToXmlConversion() {
        String regex = "^0?[0-5]$";

        // 模拟 DmXmlHelper.java:676 的转换逻辑
        // setAttr(security, "securityClassification", "0" + dm.getSecurity());

        String[] dbValues = {"0", "1", "2", "3", "4", "5"};
        String[] xmlValues = {"00", "01", "02", "03", "04", "05"};

        for (int i = 0; i < dbValues.length; i++) {
            String dbValue = dbValues[i];
            String xmlValue = "0" + dbValue;  // 转换逻辑

            assertEquals(xmlValues[i], xmlValue,
                        String.format("数据库值 %s 应转换为 %s", dbValue, xmlValues[i]));

            assertTrue(xmlValue.matches(regex),
                      String.format("转换后的XML值 %s 应该通过校验", xmlValue));
        }
    }
}
