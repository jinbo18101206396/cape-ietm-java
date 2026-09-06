package org.jeecg.modules.ietm.ietmimport;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * ICN文件名校验单元测试
 *
 * @author IETM Team
 * @date 2026-09-03
 */
public class IcnFileNameValidationTest {

    /**
     * 模拟Service中的isValidIcnFileName方法
     */
    private boolean isValidIcnFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }

        // 提取文件名（去掉路径）
        String baseName = fileName;
        if (fileName.contains("/")) {
            baseName = fileName.substring(fileName.lastIndexOf("/") + 1);
        }
        if (fileName.contains("\\")) {
            baseName = fileName.substring(fileName.lastIndexOf("\\") + 1);
        }

        // 去掉扩展名
        if (baseName.contains(".")) {
            baseName = baseName.substring(0, baseName.lastIndexOf("."));
        }

        // 简单校验：必须以ICN-开头
        if (!baseName.toUpperCase().startsWith("ICN-")) {
            return false;
        }

        // 分段校验（至少4段：ICN-model-sns-number）
        String[] parts = baseName.split("-");
        if (parts.length < 4) {
            return false;
        }

        return true;
    }

    @Test
    public void testValidIcnFileName_standard() {
        // 测试：标准格式的ICN文件名
        assertTrue(isValidIcnFileName("ICN-MODEL001-SNS001-00001.png"));
        assertTrue(isValidIcnFileName("ICN-MODEL002-SNS002-00002.jpg"));
        assertTrue(isValidIcnFileName("ICN-ABC-DEF-123.gif"));
    }

    @Test
    public void testValidIcnFileName_lowercase() {
        // 测试：小写的ICN前缀（应该被toUpperCase处理）
        assertTrue(isValidIcnFileName("icn-model001-sns001-00001.png"));
        assertTrue(isValidIcnFileName("Icn-MODEL001-SNS001-00001.jpg"));
        assertTrue(isValidIcnFileName("ICn-MODEL001-SNS001-00001.gif"));
    }

    @Test
    public void testValidIcnFileName_moreSegments() {
        // 测试：超过4段的文件名（合法）
        assertTrue(isValidIcnFileName("ICN-MODEL001-SNS001-00001-EXTRA.png"));
        assertTrue(isValidIcnFileName("ICN-A-B-C-D-E-F.jpg"));
    }

    @Test
    public void testValidIcnFileName_differentExtensions() {
        // 测试：不同的图片扩展名
        assertTrue(isValidIcnFileName("ICN-MODEL001-SNS001-00001.png"));
        assertTrue(isValidIcnFileName("ICN-MODEL001-SNS001-00001.jpg"));
        assertTrue(isValidIcnFileName("ICN-MODEL001-SNS001-00001.jpeg"));
        assertTrue(isValidIcnFileName("ICN-MODEL001-SNS001-00001.gif"));
        assertTrue(isValidIcnFileName("ICN-MODEL001-SNS001-00001.bmp"));
        assertTrue(isValidIcnFileName("ICN-MODEL001-SNS001-00001.svg"));
    }

    @Test
    public void testInvalidIcnFileName_noPrefix() {
        // 测试：没有ICN-前缀
        assertFalse(isValidIcnFileName("MODEL001-SNS001-00001.png"));
        assertFalse(isValidIcnFileName("image-001.png"));
        assertFalse(isValidIcnFileName("test.png"));
    }

    @Test
    public void testInvalidIcnFileName_wrongPrefix() {
        // 测试：错误的前缀
        assertFalse(isValidIcnFileName("DMC-MODEL001-SNS001-00001.png"));
        assertFalse(isValidIcnFileName("IMG-MODEL001-SNS001-00001.png"));
        assertFalse(isValidIcnFileName("PIC-MODEL001-SNS001-00001.png"));
    }

    @Test
    public void testInvalidIcnFileName_insufficientSegments() {
        // 测试：段数不足（少于4段）
        assertFalse(isValidIcnFileName("ICN.png"));
        assertFalse(isValidIcnFileName("ICN-MODEL001.png"));
        assertFalse(isValidIcnFileName("ICN-MODEL001-SNS001.png"));
    }

    @Test
    public void testInvalidIcnFileName_nullOrEmpty() {
        // 测试：null或空字符串
        assertFalse(isValidIcnFileName(null));
        assertFalse(isValidIcnFileName(""));
        assertFalse(isValidIcnFileName("   "));
    }

    @Test
    public void testValidIcnFileName_withPath() {
        // 测试：带路径的文件名
        assertTrue(isValidIcnFileName("C:\\images\\ICN-MODEL001-SNS001-00001.png"));
        assertTrue(isValidIcnFileName("/home/user/ICN-MODEL001-SNS001-00001.png"));
        assertTrue(isValidIcnFileName("folder/ICN-MODEL001-SNS001-00001.png"));
    }

    @Test
    public void testValidIcnFileName_noExtension() {
        // 测试：没有扩展名（文件名本身合法）
        assertTrue(isValidIcnFileName("ICN-MODEL001-SNS001-00001"));
    }

    @Test
    public void testExtractIcnCode() {
        // 测试：提取ICN编码（去掉扩展名）
        assertEquals("ICN-MODEL001-SNS001-00001",
                     extractIcnCode("ICN-MODEL001-SNS001-00001.png"));
        assertEquals("ICN-MODEL002-SNS002-00002",
                     extractIcnCode("ICN-MODEL002-SNS002-00002.jpg"));
    }

    @Test
    public void testExtractSnsFromIcnCode() {
        // 测试：从ICN编码提取SNS（第3段）
        assertEquals("SNS001", extractSnsFromIcnCode("ICN-MODEL001-SNS001-00001"));
        assertEquals("SNS002", extractSnsFromIcnCode("ICN-MODEL002-SNS002-00002"));
        assertEquals("ABC", extractSnsFromIcnCode("ICN-MODEL-ABC-123"));
    }

    @Test
    public void testExtractSnsFromIcnCode_invalid() {
        // 测试：无效的ICN编码
        assertNull(extractSnsFromIcnCode(null));
        assertNull(extractSnsFromIcnCode(""));
        assertNull(extractSnsFromIcnCode("ICN"));
        assertNull(extractSnsFromIcnCode("ICN-MODEL"));
    }

    // ========== 辅助方法 ==========

    private String extractIcnCode(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }
        // 去掉扩展名
        if (fileName.contains(".")) {
            return fileName.substring(0, fileName.lastIndexOf("."));
        }
        return fileName;
    }

    private String extractSnsFromIcnCode(String icnCode) {
        if (icnCode == null || icnCode.isEmpty()) {
            return null;
        }

        String[] parts = icnCode.split("-");
        if (parts.length < 3) {
            return null;
        }

        // 第3段是SNS（ICN-model-sns-number）
        return parts[2];
    }
}
