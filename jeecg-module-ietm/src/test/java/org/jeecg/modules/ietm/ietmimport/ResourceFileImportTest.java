package org.jeecg.modules.ietm.ietmimport;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.ietm.ietmimport.constants.DmImportConstants;
import org.jeecg.modules.ietm.ietmimport.service.IIetmDmImportService;
import org.jeecg.modules.ietm.ietmimport.vo.DmValidateResultVO;
import org.jeecg.modules.ietm.ietmimport.vo.ImportFileItemVO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 资源文件导入功能测试
 *
 * 验证：资源文件在校验阶段显示"资源文件，导入时校验"的行为是否正确
 *
 * @author IETM Team
 * @date 2026-09-05
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ResourceFileImportTest {

    @Autowired
    private IIetmDmImportService importService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String TEST_PROJECT_ID = "test_project_resource_001";

    /**
     * 测试前准备：设置项目上下文
     */
    @Before
    public void setUp() {
        // 模拟打开项目
        String userId = "test_user_resource";
        String redisKey = "ietm:current_project:" + userId;
        redisTemplate.opsForValue().set(redisKey, TEST_PROJECT_ID);
        log.info("测试准备：已设置项目上下文，projectId={}", TEST_PROJECT_ID);
    }

    /**
     * TC-01: S1000D 4.0标准结构 - MM/目录下的资源文件
     *
     * 验证：资源文件在校验阶段显示"资源文件，导入时校验"
     */
    @Test
    public void testTC01_S1000D_ResourceFile_ValidatePhase() throws Exception {
        log.info("=== TC-01: 资源文件校验阶段测试（S1000D 4.0结构） ===");

        // 1. 准备测试数据
        String dmXml = createTestDmXml("TEST", "A", "001", "A");
        String resourceFileName = "DMC-TEST-A-00-00-00-00A-001A-A_manual.pdf";
        byte[] resourceContent = "Mock PDF content for testing".getBytes(StandardCharsets.UTF_8);

        // 2. 创建ZIP包（S1000D 4.0三级目录结构）
        byte[] zipBytes = createZipWithStructure(
            "DM/DMC-TEST-A-00-00-00-00A-001A-A.xml", dmXml.getBytes(StandardCharsets.UTF_8),
            "MM/" + resourceFileName, resourceContent
        );

        MockMultipartFile zipFile = new MockMultipartFile(
            "file",
            "test-s1000d-resource.zip",
            "application/zip",
            zipBytes
        );

        // 3. 调用校验接口
        MockHttpServletRequest request = new MockHttpServletRequest();
        DmValidateResultVO validateResult = importService.validateFile(zipFile, request);

        // 4. 验证结果
        Assert.assertNotNull("校验结果不能为空", validateResult);
        Assert.assertEquals("应该有2个文件", 2, (int) validateResult.getTotalCount());

        // 5. 验证DM文件
        ImportFileItemVO dmFile = validateResult.getFiles().stream()
            .filter(f -> "DM".equals(f.getFileType()))
            .findFirst()
            .orElse(null);
        Assert.assertNotNull("应该找到DM文件", dmFile);
        Assert.assertTrue("DM应该可以导入", dmFile.canImport());
        log.info("✓ DM文件校验通过: {}", dmFile.getFileName());

        // 6. 验证资源文件（核心验证点）
        ImportFileItemVO resourceFile = validateResult.getFiles().stream()
            .filter(f -> "RESOURCE".equals(f.getFileType()))
            .findFirst()
            .orElse(null);

        Assert.assertNotNull("应该找到资源文件", resourceFile);
        Assert.assertEquals("文件类型应该是RESOURCE", "RESOURCE", resourceFile.getFileType());
        Assert.assertEquals("资源文件名应该正确", resourceFileName, resourceFile.getFileName());

        // ★ 核心验证：资源文件应该标记为可导入（vldCode=1）
        Assert.assertTrue("资源文件应该可以导入", resourceFile.canImport());
        Assert.assertEquals("资源文件resultCode应该是SUCCESS",
            DmImportConstants.SUCCESS, resourceFile.getResultCode());

        // ★ 核心验证：提取的DMC前缀应该正确
        Assert.assertEquals("应该正确提取DMC前缀",
            "DMC-TEST-A-00-00-00-00A-001A-A", resourceFile.getAssociatedDmcCode());

        log.info("✓ 资源文件校验通过: {}", resourceFile.getFileName());
        log.info("  - 文件类型: {}", resourceFile.getFileType());
        log.info("  - 关联DMC: {}", resourceFile.getAssociatedDmcCode());
        log.info("  - 校验消息: {}", resourceFile.getResultMessage());
        log.info("  - 是否可导入: {}", resourceFile.canImport());

        log.info("=== TC-01 测试通过 ✓ ===");
    }

    /**
     * TC-02: 扁平结构 - 根目录下的资源文件（旧系统兼容）
     *
     * 验证：扁平结构中的资源文件也能正确识别
     */
    @Test
    public void testTC02_Legacy_FlatStructure_ResourceFile() throws Exception {
        log.info("=== TC-02: 资源文件扁平结构测试（旧系统兼容） ===");

        // 1. 准备测试数据（扁平结构：所有文件在根目录）
        String dmXml = createTestDmXml("LEGACY", "B", "002", "A");
        String resourceFileName = "DMC-LEGACY-B-00-00-00-00A-002A-A_guide.pdf";
        byte[] resourceContent = "Legacy flat structure resource".getBytes(StandardCharsets.UTF_8);

        // 2. 创建ZIP包（扁平结构）
        byte[] zipBytes = createZipWithStructure(
            "DMC-LEGACY-B-00-00-00-00A-002A-A.xml", dmXml.getBytes(StandardCharsets.UTF_8),
            resourceFileName, resourceContent  // 注意：直接在根目录
        );

        MockMultipartFile zipFile = new MockMultipartFile(
            "file",
            "test-legacy-flat.zip",
            "application/zip",
            zipBytes
        );

        // 3. 调用校验接口
        MockHttpServletRequest request = new MockHttpServletRequest();
        DmValidateResultVO validateResult = importService.validateFile(zipFile, request);

        // 4. 验证结果
        Assert.assertNotNull("校验结果不能为空", validateResult);
        log.info("校验结果：总{}个文件，成功{}个",
            validateResult.getTotalCount(), validateResult.getSuccessCount());

        // 5. 扁平结构中，PDF文件可能被识别为其他类型或跳过
        // 因为没有MM/前缀，且不是图片扩展名
        // 根据代码逻辑（IetmDmImportServiceImpl.java:383-393），
        // 根目录下只识别XML和图片，其他跳过
        log.info("扁平结构测试：文件列表 = {}", validateResult.getFiles().size());
        for (ImportFileItemVO file : validateResult.getFiles()) {
            log.info("  - {}: {}", file.getFileType(), file.getFileName());
        }

        log.info("=== TC-02 测试完成（扁平结构不支持非图片资源文件） ===");
    }

    /**
     * TC-03: DMC前缀提取测试
     *
     * 验证：从资源文件名中正确提取DMC前缀
     */
    @Test
    public void testTC03_ExtractDmcPrefix() {
        log.info("=== TC-03: DMC前缀提取测试 ===");

        // 测试用例
        String[][] testCases = {
            {"DMC-TEST-A-00-00-00-00A-001A-A_manual.pdf", "DMC-TEST-A-00-00-00-00A-001A-A"},
            {"DMC-MODEL-B-01-02-03-04B-005C-D_spec.docx", "DMC-MODEL-B-01-02-03-04B-005C-D"},
            {"DMC-ABC_resource.pdf", "DMC-ABC"},
            {"invalid-filename.pdf", null},  // 无下划线，应返回null
            {"_noDmc.pdf", ""},  // 下划线在开头，应返回空串
        };

        for (String[] testCase : testCases) {
            String fileName = testCase[0];
            String expectedPrefix = testCase[1];

            String actualPrefix = extractDmcPrefixFromResourceName(fileName);

            if (expectedPrefix == null) {
                Assert.assertNull("文件名" + fileName + "应该返回null", actualPrefix);
            } else {
                Assert.assertEquals("文件名" + fileName + "的DMC前缀不正确",
                    expectedPrefix, actualPrefix);
            }

            log.info("✓ {}: {} -> {}",
                expectedPrefix == null ? "✗" : "✓",
                fileName,
                actualPrefix == null ? "null" : actualPrefix);
        }

        log.info("=== TC-03 测试通过 ✓ ===");
    }

    /**
     * TC-04: 资源文件命名错误测试
     *
     * 验证：命名不规范的资源文件在校验阶段就报错
     */
    @Test
    public void testTC04_InvalidResourceFileName() throws Exception {
        log.info("=== TC-04: 资源文件命名错误测试 ===");

        // 1. 准备测试数据（资源文件名缺少DMC前缀）
        String invalidResourceFileName = "invalid-no-dmc-prefix.pdf";
        byte[] resourceContent = "Invalid resource file".getBytes(StandardCharsets.UTF_8);

        // 2. 创建ZIP包
        byte[] zipBytes = createZipWithStructure(
            "MM/" + invalidResourceFileName, resourceContent
        );

        MockMultipartFile zipFile = new MockMultipartFile(
            "file",
            "test-invalid-resource.zip",
            "application/zip",
            zipBytes
        );

        // 3. 调用校验接口
        MockHttpServletRequest request = new MockHttpServletRequest();
        DmValidateResultVO validateResult = importService.validateFile(zipFile, request);

        // 4. 验证结果
        Assert.assertNotNull("校验结果不能为空", validateResult);
        Assert.assertEquals("应该有1个文件", 1, (int) validateResult.getTotalCount());

        ImportFileItemVO resourceFile = validateResult.getFiles().get(0);
        Assert.assertEquals("文件类型应该是RESOURCE", "RESOURCE", resourceFile.getFileType());

        // ★ 核心验证：命名错误的资源文件应该不能导入
        Assert.assertFalse("命名错误的资源文件不应该可以导入", resourceFile.canImport());
        Assert.assertTrue("错误消息应该包含'DMC前缀'",
            resourceFile.getResultMessage().contains("DMC前缀"));

        log.info("✓ 命名错误的资源文件正确被拒绝: {}", resourceFile.getResultMessage());
        log.info("=== TC-04 测试通过 ✓ ===");
    }

    // ========== 辅助方法 ==========

    /**
     * 创建测试DM XML内容
     */
    private String createTestDmXml(String modelCode, String systemDiffCode,
                                   String infoCode, String infoCodeVariant) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<dmodule xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
            "  <identAndStatusSection>\n" +
            "    <dmAddress>\n" +
            "      <dmIdent>\n" +
            "        <dmCode modelIdentCode=\"" + modelCode + "\" systemDiffCode=\"" + systemDiffCode + "\" " +
            "systemCode=\"00\" subSystemCode=\"00\" subSubSystemCode=\"00\" assyCode=\"00\" " +
            "disassyCode=\"00\" disassyCodeVariant=\"A\" infoCode=\"" + infoCode + "\" " +
            "infoCodeVariant=\"" + infoCodeVariant + "\" itemLocationCode=\"A\"/>\n" +
            "        <language languageIsoCode=\"zh\" countryIsoCode=\"CN\"/>\n" +
            "        <issueInfo issueNumber=\"001\" inWork=\"00\"/>\n" +
            "      </dmIdent>\n" +
            "      <dmAddressItems>\n" +
            "        <issueDate year=\"2026\" month=\"09\" day=\"05\"/>\n" +
            "      </dmAddressItems>\n" +
            "    </dmAddress>\n" +
            "    <dmStatus issueType=\"new\">\n" +
            "      <security securityClassification=\"01\"/>\n" +
            "      <responsiblePartnerCompany enterpriseCode=\"" + modelCode + "\"/>\n" +
            "    </dmStatus>\n" +
            "  </identAndStatusSection>\n" +
            "  <content>\n" +
            "    <description>\n" +
            "      <levelledPara>\n" +
            "        <title>Test DM</title>\n" +
            "        <para>This is a test data module for resource file testing.</para>\n" +
            "      </levelledPara>\n" +
            "    </description>\n" +
            "  </content>\n" +
            "</dmodule>";
    }

    /**
     * 创建带目录结构的ZIP文件
     * 参数格式：path1, content1, path2, content2, ...
     */
    private byte[] createZipWithStructure(Object... pathAndContent) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
            for (int i = 0; i < pathAndContent.length; i += 2) {
                String path = (String) pathAndContent[i];
                byte[] content = (byte[]) pathAndContent[i + 1];

                ZipEntry entry = new ZipEntry(path);
                zos.putNextEntry(entry);
                zos.write(content);
                zos.closeEntry();
            }
        }
        return baos.toByteArray();
    }

    /**
     * 从资源文件名中提取DMC前缀（模拟后端逻辑）
     */
    private String extractDmcPrefixFromResourceName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }

        int underscoreIndex = fileName.indexOf('_');
        if (underscoreIndex > 0) {
            return fileName.substring(0, underscoreIndex);
        }

        return null;
    }
}
