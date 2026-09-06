package org.jeecg.modules.ietm.ietmimport;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 资源文件导入逻辑单元测试（独立测试，不依赖Spring容器）
 *
 * 验证核心逻辑：
 * 1. DMC前缀提取算法
 * 2. 文件类型识别逻辑
 * 3. S1000D 4.0目录结构支持
 *
 * @author IETM Team
 * @date 2026-09-05
 */
@Slf4j
public class ResourceFileImportLogicTest {

    /**
     * TC-01: DMC前缀提取算法测试
     *
     * 验证：从资源文件名中正确提取DMC前缀
     */
    @Test
    public void testExtractDmcPrefix() {
        log.info("=== TC-01: DMC前缀提取算法测试 ===");

        // 测试用例：[文件名, 预期DMC前缀]
        String[][] testCases = {
            // 标准格式
            {"DMC-TEST-A-00-00-00-00A-001A-A_manual.pdf", "DMC-TEST-A-00-00-00-00A-001A-A"},
            {"DMC-MODEL-B-01-02-03-04B-005C-D_spec.docx", "DMC-MODEL-B-01-02-03-04B-005C-D"},
            {"DMC-ABC_resource.pdf", "DMC-ABC"},

            // 多个下划线
            {"DMC-TEST_file_v2_final.pdf", "DMC-TEST"},

            // 边界情况
            {"invalid-filename.pdf", null},  // 无下划线，应返回null
            {"_noDmc.pdf", ""},  // 下划线在开头，应返回空串
            {"", null},  // 空字符串
            {null, null}  // null输入
        };

        int passCount = 0;
        int failCount = 0;

        for (String[] testCase : testCases) {
            String fileName = testCase[0];
            String expectedPrefix = testCase[1];

            String actualPrefix = extractDmcPrefixFromResourceName(fileName);

            boolean pass = (expectedPrefix == null && actualPrefix == null) ||
                          (expectedPrefix != null && expectedPrefix.equals(actualPrefix));

            if (pass) {
                passCount++;
                log.info("✓ PASS: {} -> {}", fileName, actualPrefix);
            } else {
                failCount++;
                log.error("✗ FAIL: {} -> expected={}, actual={}", fileName, expectedPrefix, actualPrefix);
            }

            // Assert验证
            if (expectedPrefix == null) {
                Assert.assertNull("文件名" + fileName + "应该返回null", actualPrefix);
            } else {
                Assert.assertEquals("文件名" + fileName + "的DMC前缀不正确",
                    expectedPrefix, actualPrefix);
            }
        }

        log.info("=== TC-01 完成: {}/{} 通过 ===", passCount, passCount + failCount);
    }

    /**
     * TC-02: 文件类型识别逻辑测试（S1000D 4.0）
     *
     * 验证：根据ZIP路径正确识别文件类型
     */
    @Test
    public void testFileTypeRecognition() {
        log.info("=== TC-02: 文件类型识别逻辑测试 ===");

        // 测试用例：[ZIP内路径, 预期文件类型]
        String[][] testCases = {
            // S1000D 4.0标准目录结构
            {"DM/DMC-TEST-A-00-00-00-00A-001A-A.xml", "DM"},
            {"dm/DMC-TEST-A-00-00-00-00A-001A-A.xml", "DM"},  // 小写
            {"ICN/ICN-MODEL-SNS001-00001.png", "ICN"},
            {"icn/ICN-MODEL-SNS001-00001.jpg", "ICN"},  // 小写
            {"MM/DMC-TEST-A-00-00-00-00A-001A-A_manual.pdf", "RESOURCE"},
            {"mm/DMC-TEST-A-00-00-00-00A-001A-A_spec.docx", "RESOURCE"},  // 小写

            // 扁平结构（旧系统兼容）
            {"DMC-TEST-B-00-00-00-00A-001A-A.xml", "DM"},
            {"ICN-MODEL-SNS002-00001.png", "ICN"},

            // 应跳过的文件
            {"DM/readme.txt", "SKIP"},  // DM/目录下非XML文件
            {"ICN/document.pdf", "SKIP"},  // ICN/目录下非图片文件
            {"DDN-TEST-001.xml", "SKIP"},  // DDN元数据文件
            {"readme.txt", "SKIP"}  // 根目录下非XML非图片文件
        };

        int passCount = 0;
        int failCount = 0;

        for (String[] testCase : testCases) {
            String zipPath = testCase[0];
            String expectedType = testCase[1];

            String actualType = recognizeFileType(zipPath);

            boolean pass = expectedType.equals(actualType);

            if (pass) {
                passCount++;
                log.info("✓ PASS: {} -> {}", zipPath, actualType);
            } else {
                failCount++;
                log.error("✗ FAIL: {} -> expected={}, actual={}", zipPath, expectedType, actualType);
            }

            Assert.assertEquals("路径" + zipPath + "的文件类型识别错误",
                expectedType, actualType);
        }

        log.info("=== TC-02 完成: {}/{} 通过 ===", passCount, passCount + failCount);
    }

    /**
     * TC-03: 资源文件命名规范验证
     *
     * 验证：资源文件命名是否符合 {DMC}_{原文件名} 格式
     */
    @Test
    public void testResourceFileNamingConvention() {
        log.info("=== TC-03: 资源文件命名规范验证 ===");

        String[][] testCases = {
            // 合法命名
            {"DMC-TEST-A-00-00-00-00A-001A-A_manual.pdf", "true"},
            {"DMC-MODEL-B-01-02-03-04B-005C-D_spec_v2.docx", "true"},

            // 非法命名
            {"invalid-no-underscore.pdf", "false"},
            {"_no-dmc-prefix.pdf", "false"},  // 下划线在开头，DMC为空
            {"only-filename.pdf", "false"}
        };

        int passCount = 0;

        for (String[] testCase : testCases) {
            String fileName = testCase[0];
            boolean expectedValid = Boolean.parseBoolean(testCase[1]);

            boolean actualValid = isValidResourceFileName(fileName);

            Assert.assertEquals("文件名" + fileName + "的有效性判断错误",
                expectedValid, actualValid);

            passCount++;
            log.info("✓ PASS: {} -> valid={}", fileName, actualValid);
        }

        log.info("=== TC-03 完成: {}/{} 通过 ===", passCount, testCases.length);
    }

    /**
     * TC-04: ZIP包结构测试数据生成
     *
     * 验证：能够生成符合S1000D 4.0标准的ZIP包
     */
    @Test
    public void testCreateS1000DZipStructure() throws Exception {
        log.info("=== TC-04: ZIP包结构测试数据生成 ===");

        // 创建S1000D 4.0标准结构的ZIP包
        byte[] zipBytes = createTestZipPackage();

        Assert.assertNotNull("ZIP包不能为空", zipBytes);
        Assert.assertTrue("ZIP包大小应该大于0", zipBytes.length > 0);

        log.info("✓ 成功生成ZIP包，大小: {} bytes", zipBytes.length);
        log.info("  - 包含: DM/DMC-TEST-A-00-00-00-00A-001A-A.xml");
        log.info("  - 包含: ICN/ICN-TEST-SNS001-00001.png");
        log.info("  - 包含: MM/DMC-TEST-A-00-00-00-00A-001A-A_manual.pdf");

        log.info("=== TC-04 完成 ✓ ===");
    }

    // ========== 辅助方法（模拟后端逻辑） ==========

    /**
     * 从资源文件名中提取DMC前缀
     * 对应后端: IetmDmImportServiceImpl.extractDmcPrefixFromResourceName()
     */
    private String extractDmcPrefixFromResourceName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }

        int underscoreIndex = fileName.indexOf('_');
        if (underscoreIndex > 0) {
            return fileName.substring(0, underscoreIndex);
        } else if (underscoreIndex == 0) {
            // 下划线在开头，返回空串（而非null）
            return "";
        }

        return null;
    }

    /**
     * 识别文件类型
     * 对应后端: IetmDmImportServiceImpl.validateZipFile() 中的文件类型判断逻辑
     */
    private String recognizeFileType(String zipPath) {
        // 提取文件名
        String fileName = zipPath.substring(zipPath.lastIndexOf('/') + 1);

        // 过滤DDN元数据文件
        if (fileName.toUpperCase().startsWith("DDN-")) {
            return "SKIP";
        }

        // 判断扩展名
        boolean isXml = zipPath.toLowerCase().endsWith(".xml");
        boolean hasImageExt = zipPath.matches(".*\\.(png|jpg|jpeg|gif|bmp|svg|tif|tiff|cgm)$");

        // S1000D 4.0标准目录结构
        if (zipPath.startsWith("DM/") || zipPath.startsWith("dm/")) {
            return isXml ? "DM" : "SKIP";
        } else if (zipPath.startsWith("ICN/") || zipPath.startsWith("icn/")) {
            return hasImageExt ? "ICN" : "SKIP";
        } else if (zipPath.startsWith("MM/") || zipPath.startsWith("mm/")) {
            return "RESOURCE";
        }

        // 扁平结构（根目录）
        if (isXml) {
            return "DM";
        } else if (hasImageExt) {
            return "ICN";
        }

        return "SKIP";
    }

    /**
     * 验证资源文件命名是否合法
     */
    private boolean isValidResourceFileName(String fileName) {
        String dmcPrefix = extractDmcPrefixFromResourceName(fileName);
        // DMC前缀不能为null或空串
        return dmcPrefix != null && !dmcPrefix.isEmpty();
    }

    /**
     * 创建测试ZIP包
     */
    private byte[] createTestZipPackage() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
            // DM文件
            ZipEntry dmEntry = new ZipEntry("DM/DMC-TEST-A-00-00-00-00A-001A-A.xml");
            zos.putNextEntry(dmEntry);
            zos.write("<dmodule>Test DM</dmodule>".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // ICN文件
            ZipEntry icnEntry = new ZipEntry("ICN/ICN-TEST-SNS001-00001.png");
            zos.putNextEntry(icnEntry);
            zos.write("PNG_IMAGE_CONTENT".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // 资源文件
            ZipEntry resourceEntry = new ZipEntry("MM/DMC-TEST-A-00-00-00-00A-001A-A_manual.pdf");
            zos.putNextEntry(resourceEntry);
            zos.write("PDF_RESOURCE_CONTENT".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return baos.toByteArray();
    }
}
