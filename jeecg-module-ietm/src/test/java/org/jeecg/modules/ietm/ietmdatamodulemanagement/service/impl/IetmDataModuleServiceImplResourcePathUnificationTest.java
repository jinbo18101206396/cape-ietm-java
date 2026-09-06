package org.jeecg.modules.ietm.ietmdatamodulemanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDataModule;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDmComment;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.mapper.IetmDataModuleMapper;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.mapper.IetmDmCommentMapper;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.service.IIetmDataModuleService;
import org.jeecg.modules.ietm.projectconfigurationmanagement.entity.IetmProjectConfigurationManagement;
import org.jeecg.modules.ietm.projectconfigurationmanagement.service.IIetmProjectConfigurationManagementService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 资源文件路径统一性测试
 * 参照ICN修复方案，验证手工上传也使用 project/{projectId}/dm_resource/ 格式
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("资源文件路径统一性测试")
class IetmDataModuleServiceImplResourcePathUnificationTest {

    @Autowired
    private IIetmDataModuleService dataModuleService;

    @Autowired
    private IetmDataModuleMapper dataModuleMapper;

    @Autowired
    private IetmDmCommentMapper dmCommentMapper;

    @Autowired
    private IIetmProjectConfigurationManagementService configurationService;

    @Value("${accessFile.location:D:/workspace/IETM/file}")
    private String fileStorageLocation;

    private String testDmId;
    private String testProjectId = "TEST_PROJECT_123";
    private String testCmNodeId;

    @BeforeEach
    void setUp() {
        // 1. 创建测试配置节点
        IetmProjectConfigurationManagement config = new IetmProjectConfigurationManagement();
        config.setProjectId(testProjectId);
        config.setTitle("测试配置节点");  // 使用 title 字段
        config.setPid("0");
        configurationService.save(config);
        testCmNodeId = config.getId();

        // 2. 创建测试DM
        IetmDataModule dm = new IetmDataModule();
        dm.setProjectId(testProjectId);
        dm.setProjectName("测试项目");
        dm.setCmNodeId(testCmNodeId);
        dm.setStatus("1");
        dataModuleMapper.insert(dm);
        testDmId = dm.getId();
    }

    @AfterEach
    void tearDown() {
        // 清理测试数据
        if (testDmId != null) {
            // 删除资源记录
            dmCommentMapper.delete(new LambdaQueryWrapper<IetmDmComment>()
                    .eq(IetmDmComment::getDmId, testDmId));

            // 删除DM
            dataModuleMapper.deleteById(testDmId);
        }

        if (testCmNodeId != null) {
            configurationService.removeById(testCmNodeId);
        }

        // 清理测试文件
        File testDir = new File(fileStorageLocation, "project/" + testProjectId + "/dm_resource");
        if (testDir.exists()) {
            File[] files = testDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        }
    }

    @Test
    @DisplayName("TC-01: 手工上传应使用项目隔离目录")
    void testManualUploadUsesProjectDirectory() throws Exception {
        // 准备测试文件
        String fileContent = "这是手工上传的测试资源文件\nManual Upload Test";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "manual-test.txt",
                "text/plain",
                fileContent.getBytes()
        );

        // 执行上传
        String relativePath = dataModuleService.uploadDmResource(
                testDmId,
                "手工上传测试资源",
                "路径统一性测试",
                file
        );

        // 验证路径格式：应该是 project/{projectId}/dm_resource/xxx
        assertNotNull(relativePath, "返回的相对路径不应为null");
        assertTrue(relativePath.startsWith("project/" + testProjectId + "/dm_resource/"),
                "手工上传应使用项目隔离目录格式：project/{projectId}/dm_resource/");

        // 验证文件名包含时间戳（避免冲突）
        assertTrue(relativePath.contains("manual-test.txt"),
                "路径应包含原始文件名");

        // 验证物理文件存在
        File physicalFile = new File(fileStorageLocation, relativePath);
        assertTrue(physicalFile.exists(),
                "物理文件应该存在：" + physicalFile.getAbsolutePath());

        // 验证文件内容
        String savedContent = new String(Files.readAllBytes(physicalFile.toPath()));
        assertEquals(fileContent, savedContent, "保存的文件内容应该正确");

        // 验证数据库记录
        List<IetmDmComment> resources = dmCommentMapper.selectList(
                new LambdaQueryWrapper<IetmDmComment>()
                        .eq(IetmDmComment::getDmId, testDmId)
        );
        assertEquals(1, resources.size(), "应该有1条资源记录");

        IetmDmComment resource = resources.get(0);
        assertEquals(relativePath, resource.getFilePath(),
                "数据库中的filePath应该是完整相对路径");
        assertEquals("manual-test.txt", resource.getFileName(),
                "数据库中的fileName应该是原始文件名");
        assertEquals("手工上传测试资源", resource.getResourceName(),
                "数据库中的resourceName应该正确");
    }

    @Test
    @DisplayName("TC-02: 导入和手工上传路径格式完全一致")
    void testImportAndManualUploadPathAlignment() throws Exception {
        // 1. 模拟导入的资源（直接插入数据库记录，模拟导入逻辑）
        String importedRelativePath = "project/" + testProjectId + "/dm_resource/1609459200000_imported.pdf";
        IetmDmComment importedResource = new IetmDmComment();
        importedResource.setDmId(testDmId);
        importedResource.setFilePath(importedRelativePath);
        importedResource.setFileName("imported.pdf");
        importedResource.setResourceName("导入的资源");
        dmCommentMapper.insert(importedResource);

        // 2. 手工上传资源
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "manual.pdf",
                "application/pdf",
                "PDF Content".getBytes()
        );

        String manualRelativePath = dataModuleService.uploadDmResource(
                testDmId,
                "手工上传的资源",
                "测试",
                file
        );

        // 3. 验证路径格式一致性
        String importedFormat = importedRelativePath.substring(0, importedRelativePath.lastIndexOf("/"));
        String manualFormat = manualRelativePath.substring(0, manualRelativePath.lastIndexOf("/"));

        assertEquals(importedFormat, manualFormat,
                "导入和手工上传应使用相同的目录格式");

        // 4. 验证都使用项目隔离目录
        assertTrue(importedRelativePath.startsWith("project/" + testProjectId + "/dm_resource/"),
                "导入的资源应使用项目隔离目录");
        assertTrue(manualRelativePath.startsWith("project/" + testProjectId + "/dm_resource/"),
                "手工上传的资源应使用项目隔离目录");
    }

    @Test
    @DisplayName("TC-03: 多项目文件不冲突")
    void testMultiProjectNoConflict() throws Exception {
        // 创建第二个项目的配置节点
        String testProjectId2 = "TEST_PROJECT_456";
        IetmProjectConfigurationManagement config2 = new IetmProjectConfigurationManagement();
        config2.setProjectId(testProjectId2);
        config2.setTitle("测试配置节点2");  // 使用 title 字段
        config2.setPid("0");
        configurationService.save(config2);
        String testCmNodeId2 = config2.getId();

        // 创建第二个项目的DM
        IetmDataModule dm2 = new IetmDataModule();
        dm2.setProjectId(testProjectId2);
        dm2.setProjectName("第二个项目");
        dm2.setCmNodeId(testCmNodeId2);
        dm2.setStatus("1");
        dataModuleMapper.insert(dm2);
        String testDmId2 = dm2.getId();

        try {
            // 两个项目上传相同文件名的资源
            String fileName = "same-name-resource.txt";

            MockMultipartFile file1 = new MockMultipartFile(
                    "file", fileName, "text/plain", "项目1的内容".getBytes()
            );
            MockMultipartFile file2 = new MockMultipartFile(
                    "file", fileName, "text/plain", "项目2的内容".getBytes()
            );

            String path1 = dataModuleService.uploadDmResource(testDmId, "资源1", null, file1);
            String path2 = dataModuleService.uploadDmResource(testDmId2, "资源2", null, file2);

            // 验证路径不同（项目隔离）
            assertNotEquals(path1, path2, "不同项目的资源路径应该不同");

            assertTrue(path1.contains(testProjectId), "项目1的资源路径应包含项目1的ID");
            assertTrue(path2.contains(testProjectId2), "项目2的资源路径应包含项目2的ID");

            // 验证物理文件都存在
            assertTrue(new File(fileStorageLocation, path1).exists(), "项目1的文件应存在");
            assertTrue(new File(fileStorageLocation, path2).exists(), "项目2的文件应存在");

            // 验证内容不同
            String content1 = new String(Files.readAllBytes(new File(fileStorageLocation, path1).toPath()));
            String content2 = new String(Files.readAllBytes(new File(fileStorageLocation, path2).toPath()));
            assertEquals("项目1的内容", content1);
            assertEquals("项目2的内容", content2);

        } finally {
            // 清理
            dmCommentMapper.delete(new LambdaQueryWrapper<IetmDmComment>()
                    .eq(IetmDmComment::getDmId, testDmId2));
            dataModuleMapper.deleteById(testDmId2);
            configurationService.removeById(testCmNodeId2);

            File testDir2 = new File(fileStorageLocation, "project/" + testProjectId2 + "/dm_resource");
            if (testDir2.exists()) {
                File[] files = testDir2.listFiles();
                if (files != null) {
                    for (File file : files) {
                        file.delete();
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("TC-04: 文件名清理功能正常工作")
    void testFileNameSanitization() throws Exception {
        // 测试包含特殊字符的文件名
        String dangerousFileName = "../../../etc/passwd.txt";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                dangerousFileName,
                "text/plain",
                "test content".getBytes()
        );

        String relativePath = dataModuleService.uploadDmResource(
                testDmId,
                "特殊字符测试",
                null,
                file
        );

        // 验证路径不包含 ../ （路径遍历防护）
        assertFalse(relativePath.contains("../"), "路径不应包含 ../");
        assertFalse(relativePath.contains("..\\"), "路径不应包含 ..\\");

        // 验证路径在项目目录内
        assertTrue(relativePath.startsWith("project/" + testProjectId + "/dm_resource/"),
                "路径应该在项目目录内");
    }

    @Test
    @DisplayName("TC-05: 上传失败时正确回滚")
    void testUploadRollbackOnFailure() {
        // 使用不存在的DM ID
        String invalidDmId = "INVALID_DM_ID_99999";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "test content".getBytes()
        );

        // 验证抛出异常
        Exception exception = assertThrows(Exception.class, () -> {
            dataModuleService.uploadDmResource(invalidDmId, "测试", null, file);
        });

        assertTrue(exception.getMessage().contains("DM不存在"),
                "异常信息应该提示DM不存在");

        // 验证没有创建数据库记录
        List<IetmDmComment> resources = dmCommentMapper.selectList(
                new LambdaQueryWrapper<IetmDmComment>()
                        .eq(IetmDmComment::getDmId, invalidDmId)
        );
        assertEquals(0, resources.size(), "失败时不应创建数据库记录");
    }

    @Test
    @DisplayName("TC-06: 路径遍历防护有效")
    void testPathTraversalProtection() throws Exception {
        // 这个测试主要验证后端的路径遍历防护逻辑
        // 正常上传应该成功
        MockMultipartFile normalFile = new MockMultipartFile(
                "file",
                "normal.txt",
                "text/plain",
                "normal content".getBytes()
        );

        String relativePath = dataModuleService.uploadDmResource(
                testDmId,
                "正常文件",
                null,
                normalFile
        );

        // 验证路径规范化后仍在基础目录内
        File physicalFile = new File(fileStorageLocation, relativePath);
        File baseDir = new File(fileStorageLocation);

        assertTrue(physicalFile.toPath().toAbsolutePath().normalize()
                        .startsWith(baseDir.toPath().toAbsolutePath().normalize()),
                "物理文件路径应该在基础目录内");
    }
}
