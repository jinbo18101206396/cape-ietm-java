package org.jeecg.modules.ietm.ietmimport;

import org.jeecg.modules.ietm.ietmimport.service.impl.IetmDmImportServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;

/**
 * P1-1修复验证：英文短文件名提取算法测试
 *
 * 问题：之前算法将英文短文件名（如"file"、"manual"）误认为元数据代码段，导致提取失败
 * 修复：改用精确的元数据格式匹配（版本号格式、语言代码格式），其他一律认为是文件名
 *
 * @date 2026-09-06
 */
@RunWith(MockitoJUnitRunner.class)
public class ExtractOriginalFileNameFixTest {

    @InjectMocks
    private IetmDmImportServiceImpl dmImportService;

    /**
     * TC-01: 中文文件名（原有功能不受影响）
     */
    @Test
    public void testExtractOriginalFileName_ChineseName() {
        String input = "DMC-ZB1-A-05-00-00-00A-007A-A_001-03_zh-CN_金波.jpg";
        String expected = "金波.jpg";
        String actual = invokeExtractOriginalFileName(input);
        assertEquals(expected, actual);
    }

    /**
     * TC-02: 英文短文件名（修复场景）
     */
    @Test
    public void testExtractOriginalFileName_EnglishShortName() {
        String input = "DMC-XYZ_001-01_en-US_manual_1234567890123.pdf";
        String expected = "manual.pdf";
        String actual = invokeExtractOriginalFileName(input);
        assertEquals(expected, actual);
    }

    /**
     * TC-03: 英文超短文件名（4字符）
     */
    @Test
    public void testExtractOriginalFileName_EnglishVeryShortName() {
        String input = "DMC-TEST_file_1786887767219_1788313777683.jpg";
        String expected = "file.jpg";
        String actual = invokeExtractOriginalFileName(input);
        assertEquals(expected, actual);
    }

    /**
     * TC-04: 英文长文件名（原有功能不受影响）
     */
    @Test
    public void testExtractOriginalFileName_EnglishLongName() {
        String input = "DMC-ABC-123_UserManual_1234567890123.pdf";
        String expected = "UserManual.pdf";
        String actual = invokeExtractOriginalFileName(input);
        assertEquals(expected, actual);
    }

    /**
     * TC-05: 中文长文件名（原有功能不受影响）
     */
    @Test
    public void testExtractOriginalFileName_ChineseLongName() {
        String input = "DMC-XXX-YYY_001-03_zh-CN_测试文件.pdf";
        String expected = "测试文件.pdf";
        String actual = invokeExtractOriginalFileName(input);
        assertEquals(expected, actual);
    }

    /**
     * TC-06: 中文短文件名（2字符）
     */
    @Test
    public void testExtractOriginalFileName_ChineseShortName() {
        String input = "DMC-A_报告_1234567890123456.xlsx";
        String expected = "报告.xlsx";
        String actual = invokeExtractOriginalFileName(input);
        assertEquals(expected, actual);
    }

    /**
     * TC-07: 只有DMC前缀和文件名（无版本号、语言代码）
     */
    @Test
    public void testExtractOriginalFileName_SimpleFormat() {
        String input = "DMC-ABC-123_文档.docx";
        String expected = "文档.docx";
        String actual = invokeExtractOriginalFileName(input);
        assertEquals(expected, actual);
    }

    /**
     * TC-08: 包含多个雪花ID（取最后一个非ID段）
     */
    @Test
    public void testExtractOriginalFileName_MultipleSnowflakeIds() {
        String input = "DMC-XYZ_1234567890123_data_9876543210987.csv";
        String expected = "data.csv";
        String actual = invokeExtractOriginalFileName(input);
        assertEquals(expected, actual);
    }

    /**
     * TC-09: 文件名本身包含连字符（不应误认为是语言代码）
     */
    @Test
    public void testExtractOriginalFileName_HyphenInFileName() {
        String input = "DMC-ABC_user-guide.pdf";
        String expected = "user-guide.pdf";
        String actual = invokeExtractOriginalFileName(input);
        assertEquals(expected, actual);
    }

    /**
     * TC-10: 文件名本身包含数字（不应误认为是版本号）
     */
    @Test
    public void testExtractOriginalFileName_NumbersInFileName() {
        String input = "DMC-ABC_report2024.docx";
        String expected = "report2024.docx";
        String actual = invokeExtractOriginalFileName(input);
        assertEquals(expected, actual);
    }

    /**
     * TC-11: 边界情况 - 文件名恰好是8字符英文
     */
    @Test
    public void testExtractOriginalFileName_ExactlyEightChars() {
        String input = "DMC-ABC_document.pdf";
        String expected = "document.pdf";
        String actual = invokeExtractOriginalFileName(input);
        assertEquals(expected, actual);
    }

    /**
     * TC-12: 边界情况 - 文件名恰好匹配语言代码格式但更长
     */
    @Test
    public void testExtractOriginalFileName_LanguageCodeLike() {
        String input = "DMC-ABC_en-US-v2.pdf";
        String expected = "en-US-v2.pdf";
        String actual = invokeExtractOriginalFileName(input);
        assertEquals(expected, actual);
    }

    /**
     * TC-13: 实际DDN导出格式（完整）
     */
    @Test
    public void testExtractOriginalFileName_RealDdnFormat() {
        String input = "DMC-ZB1-A-05-00-00-00A-007A-A_001-03_zh-CN_用户手册.pdf";
        String expected = "用户手册.pdf";
        String actual = invokeExtractOriginalFileName(input);
        assertEquals(expected, actual);
    }

    /**
     * TC-14: 特殊字符文件名（已被sanitize处理）
     *
     * 注意：文件名 "report_2024_v1.2_.pdf" 包含多个下划线，算法从后向前查找，
     * 会先遇到 "v1.2_"，这是预期行为。如果需要保留完整文件名，
     * 应在导出时避免在原始文件名中使用下划线，或使用双下划线分隔。
     */
    @Test
    public void testExtractOriginalFileName_SanitizedSpecialChars() {
        String input = "DMC-ABC_report-2024-v1.2.pdf";
        String expected = "report-2024-v1.2.pdf";
        String actual = invokeExtractOriginalFileName(input);
        assertEquals(expected, actual);
    }

    /**
     * TC-15: 无扩展名的文件
     */
    @Test
    public void testExtractOriginalFileName_NoExtension() {
        String input = "DMC-ABC_README";
        String expected = "README";
        String actual = invokeExtractOriginalFileName(input);
        assertEquals(expected, actual);
    }

    // ============== 辅助方法 ==============

    /**
     * 通过反射调用私有方法 extractOriginalFileName
     */
    private String invokeExtractOriginalFileName(String fileName) {
        return ReflectionTestUtils.invokeMethod(dmImportService, "extractOriginalFileName", fileName);
    }
}
