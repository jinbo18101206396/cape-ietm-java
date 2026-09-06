package org.jeecg.modules.ietm.common.util;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * FileNameUtils 单元测试
 *
 * @author IETM Team
 * @date 2026-09-02
 */
public class FileNameUtilsTest {

    @Test
    public void testSanitize_normalFileName() {
        assertEquals("test.txt", FileNameUtils.sanitize("test.txt"));
        assertEquals("文档.pdf", FileNameUtils.sanitize("文档.pdf"));
        assertEquals("金波.jpg", FileNameUtils.sanitize("金波.jpg"));
    }

    @Test
    public void testSanitize_withUnsafeChars() {
        // Windows不允许的字符应该被替换为下划线
        assertEquals("test_file.txt", FileNameUtils.sanitize("test:file.txt"));
        assertEquals("doc_page.pdf", FileNameUtils.sanitize("doc/page.pdf"));
        assertEquals("image_001.jpg", FileNameUtils.sanitize("image*001.jpg"));
        assertEquals("file_name.txt", FileNameUtils.sanitize("file\\name.txt"));
        assertEquals("data_set_.csv", FileNameUtils.sanitize("data<set>.csv")); // < 和 > 都被替换
        assertEquals("report_2024.xlsx", FileNameUtils.sanitize("report|2024.xlsx"));
    }

    @Test
    public void testSanitize_nullOrEmpty() {
        assertEquals("unnamed", FileNameUtils.sanitize(null));
        assertEquals("unnamed", FileNameUtils.sanitize(""));
        assertEquals("unnamed", FileNameUtils.sanitize("."));
        assertEquals("unnamed", FileNameUtils.sanitize(".."));
    }

    @Test
    public void testSanitize_longFileName() {
        // 构建一个超长文件名
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 250; i++) {
            sb.append("a");
        }
        String longName = sb.toString() + ".txt";

        String result = FileNameUtils.sanitize(longName);
        assertTrue(result.length() <= 200);
        assertTrue(result.endsWith(".txt")); // 应该保留扩展名
    }

    @Test
    public void testSanitize_withMaxLength() {
        String fileName = "very_long_file_name.txt";
        String result = FileNameUtils.sanitize(fileName, 10);
        assertTrue(result.length() <= 10);
    }

    @Test
    public void testExtractOriginalName_withIdSuffix() {
        // 标准格式：原始名_ID1_ID2.ext
        assertEquals("金波.jpg", FileNameUtils.extractOriginalName("金波_1786887767219_1788313777683.jpg"));
        assertEquals("文档.pdf", FileNameUtils.extractOriginalName("文档_123456_789012.pdf"));
        assertEquals("image.png", FileNameUtils.extractOriginalName("image_111_222.png"));
    }

    @Test
    public void testExtractOriginalName_withoutSuffix() {
        // 没有下划线，直接返回原值
        assertEquals("test.txt", FileNameUtils.extractOriginalName("test.txt"));
        assertEquals("文档.pdf", FileNameUtils.extractOriginalName("文档.pdf"));
    }

    @Test
    public void testExtractOriginalName_withoutExtension() {
        // 没有扩展名
        assertEquals("readme", FileNameUtils.extractOriginalName("readme_123_456"));
        assertEquals("file", FileNameUtils.extractOriginalName("file"));
    }

    @Test
    public void testExtractOriginalName_nullOrEmpty() {
        assertNull(FileNameUtils.extractOriginalName(null));
        assertEquals("", FileNameUtils.extractOriginalName(""));
    }

    @Test
    public void testExtractOriginalName_multipleUnderscores() {
        // 只提取第一个下划线之前的部分
        assertEquals("my.txt", FileNameUtils.extractOriginalName("my_file_123_456.txt"));
    }

    @Test
    public void testWithPrefix_normal() {
        assertEquals("DMC-ABC-123_image.jpg", FileNameUtils.withPrefix("DMC-ABC-123", "image.jpg"));
        assertEquals("ICN-001_图片.png", FileNameUtils.withPrefix("ICN-001", "图片.png"));
    }

    @Test
    public void testWithPrefix_nullPrefix() {
        assertEquals("file.txt", FileNameUtils.withPrefix(null, "file.txt"));
        assertEquals("doc.pdf", FileNameUtils.withPrefix("", "doc.pdf"));
    }

    @Test
    public void testWithPrefix_unsafeChars() {
        // 前缀和文件名都应该被清理
        String result = FileNameUtils.withPrefix("DMC:ABC", "file/name.txt");
        assertEquals("DMC_ABC_file_name.txt", result);
    }

    @Test
    public void testGetExtension() {
        assertEquals(".txt", FileNameUtils.getExtension("file.txt"));
        assertEquals(".pdf", FileNameUtils.getExtension("document.pdf"));
        assertEquals(".jpg", FileNameUtils.getExtension("image.jpg"));
        assertEquals("", FileNameUtils.getExtension("readme"));
        assertEquals("", FileNameUtils.getExtension(null));
        assertEquals("", FileNameUtils.getExtension(""));
    }

    @Test
    public void testGetExtension_multipleDotsFileName() {
        // 应该返回最后一个点后的部分
        assertEquals(".gz", FileNameUtils.getExtension("archive.tar.gz"));
        assertEquals(".txt", FileNameUtils.getExtension("file.backup.txt"));
    }

    @Test
    public void testGetBaseName() {
        assertEquals("file", FileNameUtils.getBaseName("file.txt"));
        assertEquals("document", FileNameUtils.getBaseName("document.pdf"));
        assertEquals("readme", FileNameUtils.getBaseName("readme"));
        assertEquals("", FileNameUtils.getBaseName(null));
        assertEquals("", FileNameUtils.getBaseName(""));
    }

    @Test
    public void testGetBaseName_multipleDotsFileName() {
        assertEquals("archive.tar", FileNameUtils.getBaseName("archive.tar.gz"));
        assertEquals("file.backup", FileNameUtils.getBaseName("file.backup.txt"));
    }

    @Test
    public void testIsSafe_safeFileNames() {
        assertTrue(FileNameUtils.isSafe("test.txt"));
        assertTrue(FileNameUtils.isSafe("file-name_123.pdf"));
        assertTrue(FileNameUtils.isSafe("文档.doc"));
        assertTrue(FileNameUtils.isSafe("image_金波.jpg"));
    }

    @Test
    public void testIsSafe_unsafeFileNames() {
        assertFalse(FileNameUtils.isSafe("test:file.txt"));
        assertFalse(FileNameUtils.isSafe("doc/page.pdf"));
        assertFalse(FileNameUtils.isSafe("image*001.jpg"));
        assertFalse(FileNameUtils.isSafe("file\\name.txt"));
        assertFalse(FileNameUtils.isSafe("data<set>.csv"));
        assertFalse(FileNameUtils.isSafe("report|2024.xlsx"));
        assertFalse(FileNameUtils.isSafe("file?name.txt"));
        assertFalse(FileNameUtils.isSafe("doc\"quote.txt"));
    }

    @Test
    public void testIsSafe_nullOrEmpty() {
        assertFalse(FileNameUtils.isSafe(null));
        assertFalse(FileNameUtils.isSafe(""));
    }

    @Test
    public void testRealWorldScenario_ddnExport() {
        // 模拟DDN导出场景
        String dmcCode = "DMC-ZB1-30101-00000-00-00A-000A-A";
        String uploadedFileName = "金波_1786887767219_1788313777683.jpg";

        // 1. 提取原始文件名
        String originalName = FileNameUtils.extractOriginalName(uploadedFileName);
        assertEquals("金波.jpg", originalName);

        // 2. 构建带前缀的安全文件名
        String exportName = FileNameUtils.withPrefix(dmcCode, originalName);
        assertEquals("DMC-ZB1-30101-00000-00-00A-000A-A_金波.jpg", exportName);

        // 3. 验证安全性
        assertTrue(FileNameUtils.isSafe(exportName));
    }

    @Test
    public void testRealWorldScenario_icnExport() {
        // 模拟ICN导出场景
        String icnCode = "ICN-ZB1-30101-00000-001";
        String fileName = "液压系统示意图.png";

        String exportName = FileNameUtils.withPrefix(icnCode, fileName);
        assertEquals("ICN-ZB1-30101-00000-001_液压系统示意图.png", exportName);
    }
}
