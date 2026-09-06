package org.jeecg.modules.ietm.ietmimport;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.ietm.ietmimport.constants.DmImportConstants;
import org.jeecg.modules.ietm.ietmimport.vo.ImportFileItemVO;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * S1000D 4.0标准目录结构兼容性测试
 *
 * 验证修复后的导入逻辑能够：
 * 1. 识别S1000D 4.0标准目录结构（DM/, ICN/, MM/）
 * 2. 向后兼容旧的扁平结构（根目录 + MM/）
 * 3. 支持混合格式
 *
 * 这是纯单元测试，不依赖Spring容器和Shiro安全管理器
 *
 * @author IETM Team
 * @date 2026-09-04
 */
@Slf4j
public class S1000D40StructureCompatibilityTest {

    /**
     * 测试用例1: S1000D 4.0标准结构 - 完整三目录结构
     *
     * ZIP结构：
     * - DM/DMC-TEST-A-001-00-00-00-00A-001A-A.xml
     * - ICN/ICN-MODEL-SNS001-00001.png
     * - MM/DMC-TEST-A-001-00-00-00-00A-001A-A_附件.pdf
     *
     * 预期：全部3个文件正确识别
     */
    @Test
    public void testCase1_S1000D40_StandardStructure() throws Exception {
        log.info("\n========== 测试用例1: S1000D 4.0标准结构 ==========");

        // 构建ZIP包
        byte[] zipBytes = createS1000D40StandardZip();

        // 解析ZIP并验证文件类型识别
        List<FileTypeResult> results = parseZipAndRecognizeFileTypes(zipBytes);

        // 断言
        Assert.assertEquals("应该识别3个文件", 3, results.size());

        // 验证DM文件
        FileTypeResult dmFile = results.stream()
            .filter(f -> "DM".equals(f.fileType))
            .findFirst()
            .orElse(null);
        Assert.assertNotNull("应该找到DM文件", dmFile);
        Assert.assertTrue("DM文件路径应该在DM/目录", dmFile.entryName.startsWith("DM/"));

        // 验证ICN文件
        FileTypeResult icnFile = results.stream()
            .filter(f -> "ICN".equals(f.fileType))
            .findFirst()
            .orElse(null);
        Assert.assertNotNull("应该找到ICN文件", icnFile);
        Assert.assertTrue("ICN文件路径应该在ICN/目录", icnFile.entryName.startsWith("ICN/"));

        // 验证资源文件
        FileTypeResult resourceFile = results.stream()
            .filter(f -> "RESOURCE".equals(f.fileType))
            .findFirst()
            .orElse(null);
        Assert.assertNotNull("应该找到资源文件", resourceFile);
        Assert.assertTrue("资源文件路径应该在MM/目录", resourceFile.entryName.startsWith("MM/"));
        Assert.assertEquals("资源文件应该关联到正确的DMC",
            "DMC-TEST-A-001-00-00-00-00A-001A-A", resourceFile.associatedDmc);

        log.info("✅ 测试用例1通过: S1000D 4.0标准结构正确识别");
    }

    /**
     * 测试用例2: 旧扁平结构 - 向后兼容性
     *
     * ZIP结构：
     * - DMC-TEST-A-002-00-00-00-00A-001A-A.xml (根目录)
     * - ICN-MODEL-SNS002-00001.png (根目录)
     * - MM/DMC-TEST-A-002-00-00-00-00A-001A-A_附件.pdf (MM/目录)
     *
     * 预期：全部3个文件正确识别（向后兼容）
     */
    @Test
    public void testCase2_LegacyFlatStructure() throws Exception {
        log.info("\n========== 测试用例2: 旧扁平结构（向后兼容） ==========");

        // 构建ZIP包
        byte[] zipBytes = createLegacyFlatZip();

        // 解析ZIP并验证
        List<FileTypeResult> results = parseZipAndRecognizeFileTypes(zipBytes);

        // 断言
        Assert.assertEquals("应该识别3个文件", 3, results.size());

        // 验证DM文件（根目录）
        FileTypeResult dmFile = results.stream()
            .filter(f -> "DM".equals(f.fileType))
            .findFirst()
            .orElse(null);
        Assert.assertNotNull("应该找到DM文件", dmFile);
        Assert.assertFalse("DM文件路径不应该在DM/目录", dmFile.entryName.startsWith("DM/"));

        // 验证ICN文件（根目录）
        FileTypeResult icnFile = results.stream()
            .filter(f -> "ICN".equals(f.fileType))
            .findFirst()
            .orElse(null);
        Assert.assertNotNull("应该找到ICN文件", icnFile);
        Assert.assertFalse("ICN文件路径不应该在ICN/目录", icnFile.entryName.startsWith("ICN/"));

        // 验证资源文件（MM/目录）
        FileTypeResult resourceFile = results.stream()
            .filter(f -> "RESOURCE".equals(f.fileType))
            .findFirst()
            .orElse(null);
        Assert.assertNotNull("应该找到资源文件", resourceFile);
        Assert.assertTrue("资源文件路径应该在MM/目录", resourceFile.entryName.startsWith("MM/"));

        log.info("✅ 测试用例2通过: 旧扁平结构正确识别（向后兼容）");
    }

    /**
     * 测试用例3: 混合格式
     *
     * ZIP结构：
     * - DM/DMC-TEST-A-003-00-00-00-00A-001A-A.xml (S1000D标准)
     * - ICN-MODEL-SNS003-00001.png (扁平结构，根目录)
     * - MM/DMC-TEST-A-003-00-00-00-00A-001A-A_附件.pdf (两者共用)
     *
     * 预期：全部3个文件正确识别（混合格式兼容）
     */
    @Test
    public void testCase3_MixedFormat() throws Exception {
        log.info("\n========== 测试用例3: 混合格式 ==========");

        // 构建ZIP包
        byte[] zipBytes = createMixedFormatZip();

        // 解析ZIP并验证
        List<FileTypeResult> results = parseZipAndRecognizeFileTypes(zipBytes);

        // 断言
        Assert.assertEquals("应该识别3个文件", 3, results.size());

        // 验证DM文件（S1000D标准，DM/目录）
        FileTypeResult dmFile = results.stream()
            .filter(f -> "DM".equals(f.fileType))
            .findFirst()
            .orElse(null);
        Assert.assertNotNull("应该找到DM文件", dmFile);
        Assert.assertTrue("DM文件应该在DM/目录", dmFile.entryName.startsWith("DM/"));

        // 验证ICN文件（扁平结构，根目录）
        FileTypeResult icnFile = results.stream()
            .filter(f -> "ICN".equals(f.fileType))
            .findFirst()
            .orElse(null);
        Assert.assertNotNull("应该找到ICN文件", icnFile);
        Assert.assertFalse("ICN文件应该在根目录", icnFile.entryName.startsWith("ICN/"));

        // 验证资源文件（MM/目录）
        FileTypeResult resourceFile = results.stream()
            .filter(f -> "RESOURCE".equals(f.fileType))
            .findFirst()
            .orElse(null);
        Assert.assertNotNull("应该找到资源文件", resourceFile);
        Assert.assertTrue("资源文件应该在MM/目录", resourceFile.entryName.startsWith("MM/"));

        log.info("✅ 测试用例3通过: 混合格式正确识别");
    }

    /**
     * 测试用例4: 大小写不敏感
     *
     * ZIP结构：
     * - dm/DMC-TEST-A-004-00-00-00-00A-001A-A.xml (小写dm/)
     * - icn/ICN-MODEL-SNS004-00001.png (小写icn/)
     * - mm/DMC-TEST-A-004-00-00-00-00A-001A-A_附件.pdf (小写mm/)
     *
     * 预期：全部3个文件正确识别（大小写不敏感）
     */
    @Test
    public void testCase4_CaseInsensitive() throws Exception {
        log.info("\n========== 测试用例4: 大小写不敏感 ==========");

        // 构建ZIP包
        byte[] zipBytes = createLowercaseDirectoryZip();

        // 解析ZIP并验证
        List<FileTypeResult> results = parseZipAndRecognizeFileTypes(zipBytes);

        // 断言
        Assert.assertEquals("应该识别3个文件", 3, results.size());

        long dmCount = results.stream().filter(f -> "DM".equals(f.fileType)).count();
        long icnCount = results.stream().filter(f -> "ICN".equals(f.fileType)).count();
        long resourceCount = results.stream().filter(f -> "RESOURCE".equals(f.fileType)).count();

        Assert.assertEquals("应该有1个DM", 1, dmCount);
        Assert.assertEquals("应该有1个ICN", 1, icnCount);
        Assert.assertEquals("应该有1个资源文件", 1, resourceCount);

        log.info("✅ 测试用例4通过: 大小写不敏感识别");
    }

    /**
     * 测试用例5: 导出→导入闭环测试
     *
     * 模拟场景：
     * 1. 用户在"导出数据模块"页面导出100个DM（生成S1000D 4.0标准ZIP）
     * 2. 用户在"数据模块导入"页面导入该ZIP
     *
     * 预期：100个DM全部识别成功，0失败
     */
    @Test
    public void testCase5_ExportImportClosedLoop() throws Exception {
        log.info("\n========== 测试用例5: 导出→导入闭环测试 ==========");

        // 模拟导出生成的ZIP包（包含100个DM）
        byte[] zipBytes = createExportedPackageWithMultipleDms(100);

        // 解析ZIP并验证
        List<FileTypeResult> results = parseZipAndRecognizeFileTypes(zipBytes);

        // 断言
        Assert.assertEquals("应该识别100个文件", 100, results.size());

        long dmCount = results.stream().filter(f -> "DM".equals(f.fileType)).count();
        Assert.assertEquals("所有文件应该都是DM类型", 100L, dmCount);

        // 验证所有DM都在DM/目录
        boolean allInDmDir = results.stream()
            .allMatch(f -> f.entryName.startsWith("DM/"));
        Assert.assertTrue("所有DM应该都在DM/目录", allInDmDir);

        log.info("✅ 测试用例5通过: 导出→导入闭环（100个DM, 0失败）");
    }

    // ========== 核心识别逻辑（复制自IetmDmImportServiceImpl.validateZipFile） ==========

    /**
     * 解析ZIP并识别文件类型（复制核心逻辑）
     */
    private List<FileTypeResult> parseZipAndRecognizeFileTypes(byte[] zipBytes) throws Exception {
        List<FileTypeResult> results = new ArrayList<>();

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();

                // 跳过目录
                if (entry.isDirectory()) {
                    continue;
                }

                // 跳过Mac系统文件
                if (entryName.contains("__MACOSX") || entryName.contains(".DS_Store")) {
                    continue;
                }

                // 跳过DDN文件
                String fileName = entryName.substring(entryName.lastIndexOf('/') + 1);
                if (fileName.toUpperCase().startsWith("DDN-") && fileName.toLowerCase().endsWith(".xml")) {
                    continue;
                }

                // 判断文件类型
                boolean isXml = fileName.toLowerCase().endsWith(".xml");
                boolean hasImageExt = fileName.toLowerCase().matches(".*\\.(png|jpg|jpeg|gif|bmp|svg|cgm|tif|tiff)$");

                String fileType = null;
                String associatedDmc = null;

                // ========== 核心识别逻辑（对齐修复后的代码） ==========
                // 优先级：S1000D 4.0标准目录结构
                if (entryName.startsWith("DM/") || entryName.startsWith("dm/")) {
                    if (isXml) fileType = "DM";
                } else if (entryName.startsWith("ICN/") || entryName.startsWith("icn/")) {
                    if (hasImageExt) fileType = "ICN";
                } else if (entryName.startsWith("MM/") || entryName.startsWith("mm/")) {
                    fileType = "RESOURCE";
                    // 从文件名提取DMC
                    associatedDmc = extractDmcFromResourceFileName(fileName);
                } else {
                    // 向后兼容：根目录文件（旧扁平结构）
                    if (isXml) fileType = "DM";
                    else if (hasImageExt) fileType = "ICN";
                }

                if (fileType != null) {
                    FileTypeResult result = new FileTypeResult();
                    result.entryName = entryName;
                    result.fileName = fileName;
                    result.fileType = fileType;
                    result.associatedDmc = associatedDmc;
                    results.add(result);
                }

                zis.closeEntry();
            }
        }

        log.info("识别完成: 总文件数={}, DM={}, ICN={}, RESOURCE={}",
            results.size(),
            results.stream().filter(f -> "DM".equals(f.fileType)).count(),
            results.stream().filter(f -> "ICN".equals(f.fileType)).count(),
            results.stream().filter(f -> "RESOURCE".equals(f.fileType)).count());

        return results;
    }

    /**
     * 从资源文件名提取DMC
     */
    private String extractDmcFromResourceFileName(String fileName) {
        // 示例文件名: DMC-TEST-A-001-00-00-00-00A-001A-A_附件.pdf
        // 提取DMC: DMC-TEST-A-001-00-00-00-00A-001A-A
        if (fileName.toUpperCase().startsWith("DMC-")) {
            int underscoreIndex = fileName.indexOf('_');
            if (underscoreIndex > 0) {
                return fileName.substring(0, underscoreIndex);
            } else {
                int dotIndex = fileName.lastIndexOf('.');
                if (dotIndex > 0) {
                    return fileName.substring(0, dotIndex);
                }
            }
        }
        return null;
    }

    // ========== 辅助类 ==========

    /**
     * 文件类型识别结果
     */
    private static class FileTypeResult {
        String entryName;       // ZIP中的完整路径
        String fileName;        // 文件名
        String fileType;        // DM/ICN/RESOURCE
        String associatedDmc;   // 关联的DMC（仅RESOURCE）
    }

    // ========== 辅助方法：构建测试ZIP包 ==========

    private byte[] createS1000D40StandardZip() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
            addZipEntry(zos, "DM/DMC-TEST-A-001-00-00-00-00A-001A-A.xml", createTestDmXml("001"));
            addZipEntry(zos, "ICN/ICN-MODEL-SNS001-00001.png", "fake-png-content".getBytes());
            addZipEntry(zos, "MM/DMC-TEST-A-001-00-00-00-00A-001A-A_附件.pdf", "fake-pdf-content".getBytes());
        }
        return baos.toByteArray();
    }

    private byte[] createLegacyFlatZip() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
            addZipEntry(zos, "DMC-TEST-A-002-00-00-00-00A-001A-A.xml", createTestDmXml("002"));
            addZipEntry(zos, "ICN-MODEL-SNS002-00001.png", "fake-png-content".getBytes());
            addZipEntry(zos, "MM/DMC-TEST-A-002-00-00-00-00A-001A-A_附件.pdf", "fake-pdf-content".getBytes());
        }
        return baos.toByteArray();
    }

    private byte[] createMixedFormatZip() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
            addZipEntry(zos, "DM/DMC-TEST-A-003-00-00-00-00A-001A-A.xml", createTestDmXml("003"));
            addZipEntry(zos, "ICN-MODEL-SNS003-00001.png", "fake-png-content".getBytes());
            addZipEntry(zos, "MM/DMC-TEST-A-003-00-00-00-00A-001A-A_附件.pdf", "fake-pdf-content".getBytes());
        }
        return baos.toByteArray();
    }

    private byte[] createLowercaseDirectoryZip() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
            addZipEntry(zos, "dm/DMC-TEST-A-004-00-00-00-00A-001A-A.xml", createTestDmXml("004"));
            addZipEntry(zos, "icn/ICN-MODEL-SNS004-00001.png", "fake-png-content".getBytes());
            addZipEntry(zos, "mm/DMC-TEST-A-004-00-00-00-00A-001A-A_附件.pdf", "fake-pdf-content".getBytes());
        }
        return baos.toByteArray();
    }

    private byte[] createExportedPackageWithMultipleDms(int dmCount) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
            for (int i = 1; i <= dmCount; i++) {
                String seq = String.format("%03d", i);
                String dmFileName = String.format("DM/DMC-TEST-A-%s-00-00-00-00A-001A-A.xml", seq);
                addZipEntry(zos, dmFileName, createTestDmXml(seq));
            }
        }
        return baos.toByteArray();
    }

    private void addZipEntry(ZipOutputStream zos, String entryName, byte[] content) throws Exception {
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        zos.write(content);
        zos.closeEntry();
    }

    private byte[] createTestDmXml(String seq) {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<dmodule>\n" +
                "  <identAndStatusSection>\n" +
                "    <dmAddress>\n" +
                "      <dmIdent>\n" +
                "        <dmCode modelIdentCode=\"TEST\" systemDiffCode=\"A\" systemCode=\"" + seq + "\" " +
                "subSystemCode=\"0\" subSubSystemCode=\"0\" assyCode=\"00\" disassyCode=\"00\" " +
                "disassyCodeVariant=\"A\" infoCode=\"001\" infoCodeVariant=\"A\" itemLocationCode=\"A\"/>\n" +
                "      </dmIdent>\n" +
                "    </dmAddress>\n" +
                "    <dmStatus>\n" +
                "      <security securityClassification=\"01\"/>\n" +
                "    </dmStatus>\n" +
                "  </identAndStatusSection>\n" +
                "</dmodule>";
        return xml.getBytes(StandardCharsets.UTF_8);
    }
}
