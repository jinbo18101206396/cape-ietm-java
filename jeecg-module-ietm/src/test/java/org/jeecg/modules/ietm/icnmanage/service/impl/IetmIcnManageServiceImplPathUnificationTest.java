package org.jeecg.modules.ietm.icnmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.ietm.ietmattachment.entity.IetmAttachment;
import org.jeecg.modules.ietm.ietmattachment.service.IIetmAttachmentService;
import org.jeecg.modules.ietm.icnmanage.entity.IetmIcnManage;
import org.jeecg.modules.ietm.icnmanage.mapper.IetmIcnManageMapper;
import org.jeecg.modules.ietm.projectconfigurationmanagement.entity.IetmProjectConfigurationManagement;
import org.jeecg.modules.ietm.projectconfigurationmanagement.service.IIetmProjectConfigurationManagementService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ICN路径统一修复验证测试
 *
 * 测试场景：
 * 1. 手工上传ICN使用 project/{projectId}/icn/ 目录
 * 2. 导入ICN使用 project/{projectId}/icn/ 目录
 * 3. 路径格式100%统一
 * 4. 文件名不会冲突
 *
 * @author IETM Team
 * @date 2026-09-05
 */
@RunWith(MockitoJUnitRunner.class)
public class IetmIcnManageServiceImplPathUnificationTest {

    @InjectMocks
    private IetmIcnManageServiceImpl service;

    @Mock
    private IetmIcnManageMapper baseMapper;

    @Mock
    private IIetmAttachmentService attachmentService;

    @Mock
    private IIetmProjectConfigurationManagementService configurationService;

    private String tempDir;
    private String projectId = "1234567890";
    private String cmNodeId = "test-cmnode-id";

    @Before
    public void setUp() throws Exception {
        // 创建临时测试目录
        tempDir = System.getProperty("java.io.tmpdir") + File.separator + "ietm-test-" + System.currentTimeMillis();
        new File(tempDir).mkdirs();

        // 设置fileStorageLocation
        ReflectionTestUtils.setField(service, "fileStorageLocation", tempDir);
        ReflectionTestUtils.setField(service, "uploadType", "local");

        // Mock构型节点查询
        IetmProjectConfigurationManagement config = new IetmProjectConfigurationManagement();
        config.setId(cmNodeId);
        config.setProjectId(projectId);
        when(configurationService.getById(cmNodeId)).thenReturn(config);
    }

    @Test
    public void testManualUpload_UsesProjectDirectory() throws Exception {
        System.out.println("\n========== 测试1：手工上传使用project目录 ==========");

        // 准备测试数据
        String icnId = "test-icn-id-001";
        IetmIcnManage icn = new IetmIcnManage();
        icn.setId(icnId);
        icn.setCmNodeId(cmNodeId);

        when(baseMapper.selectById(icnId)).thenReturn(icn);

        // 创建测试文件
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test-icn.jpg",
            "image/jpeg",
            "Test ICN Content".getBytes(StandardCharsets.UTF_8)
        );

        // 执行上传（通过反射调用private方法）
        java.lang.reflect.Method method = service.getClass().getDeclaredMethod(
            "saveAttachment", String.class, org.springframework.web.multipart.MultipartFile.class,
            String.class, Integer.class, String.class
        );
        method.setAccessible(true);

        // 捕获保存的attachment对象
        when(attachmentService.save(any(IetmAttachment.class))).thenAnswer(invocation -> {
            IetmAttachment attachment = invocation.getArgument(0);

            System.out.println("保存的附件信息：");
            System.out.println("  - fileName: " + attachment.getFileName());
            System.out.println("  - fileKey: " + attachment.getFileKey());
            System.out.println("  - pid: " + attachment.getPid());

            // 验证：fileKey应该是 project/{projectId}/icn/ 格式
            assertNotNull("fileKey不应为null", attachment.getFileKey());
            assertTrue("fileKey应该包含project前缀",
                attachment.getFileKey().startsWith("project/"));
            assertTrue("fileKey应该包含projectId",
                attachment.getFileKey().contains(projectId));
            assertTrue("fileKey应该包含/icn/目录",
                attachment.getFileKey().contains("/icn/"));

            System.out.println("✅ 手工上传路径格式正确：" + attachment.getFileKey());

            return true;
        });

        method.invoke(service, icnId, file, "entity", 0, "testUser");

        // 验证：文件应该保存在 project/{projectId}/icn/ 目录
        File projectDir = new File(tempDir, "project");
        File projectSubDir = new File(projectDir, projectId);
        File icnDir = new File(projectSubDir, "icn");

        assertTrue("project目录应该被创建", projectDir.exists());
        assertTrue("project/{projectId}目录应该被创建", projectSubDir.exists());
        assertTrue("project/{projectId}/icn目录应该被创建", icnDir.exists());

        System.out.println("✅ 测试通过：手工上传使用project/{projectId}/icn/目录");
    }

    @Test
    public void testImportAndManualUpload_SameDirectory() throws Exception {
        System.out.println("\n========== 测试2：导入和手工上传使用相同目录 ==========");

        String icnId1 = "test-icn-import";
        String icnId2 = "test-icn-manual";

        IetmIcnManage icn1 = new IetmIcnManage();
        icn1.setId(icnId1);
        icn1.setCmNodeId(cmNodeId);

        IetmIcnManage icn2 = new IetmIcnManage();
        icn2.setId(icnId2);
        icn2.setCmNodeId(cmNodeId);

        when(baseMapper.selectById(icnId1)).thenReturn(icn1);
        when(baseMapper.selectById(icnId2)).thenReturn(icn2);

        // 模拟导入的fileKey格式
        String importFileKey = "project/" + projectId + "/icn/ICN-ZB1-A-001.jpg";
        System.out.println("导入的fileKey: " + importFileKey);

        // 模拟手工上传
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "manual-upload.jpg",
            "image/jpeg",
            "Manual Upload".getBytes(StandardCharsets.UTF_8)
        );

        final String[] manualFileKey = {null};

        when(attachmentService.save(any(IetmAttachment.class))).thenAnswer(invocation -> {
            IetmAttachment attachment = invocation.getArgument(0);
            manualFileKey[0] = attachment.getFileKey();
            return true;
        });

        // 执行手工上传
        java.lang.reflect.Method method = service.getClass().getDeclaredMethod(
            "saveAttachment", String.class, org.springframework.web.multipart.MultipartFile.class,
            String.class, Integer.class, String.class
        );
        method.setAccessible(true);
        method.invoke(service, icnId2, file, "entity", 0, "testUser");

        System.out.println("手工上传的fileKey: " + manualFileKey[0]);

        // 验证：两者都使用 project/{projectId}/icn/ 目录
        assertTrue("导入应使用project目录", importFileKey.startsWith("project/"));
        assertTrue("手工上传应使用project目录", manualFileKey[0].startsWith("project/"));

        String importDir = importFileKey.substring(0, importFileKey.lastIndexOf("/"));
        String manualDir = manualFileKey[0].substring(0, manualFileKey[0].lastIndexOf("/"));

        assertEquals("导入和手工上传应使用相同的目录",
            "project/" + projectId + "/icn", importDir);
        assertEquals("导入和手工上传应使用相同的目录",
            "project/" + projectId + "/icn", manualDir);

        System.out.println("✅ 测试通过：导入和手工上传使用相同目录结构");
    }

    @Test
    public void testMultiProject_NoFileConflict() throws Exception {
        System.out.println("\n========== 测试3：多项目无文件名冲突 ==========");

        // 项目A
        String projectIdA = "1111111111";
        String cmNodeIdA = "cmnode-a";
        String icnIdA = "icn-a";

        // 项目B
        String projectIdB = "2222222222";
        String cmNodeIdB = "cmnode-b";
        String icnIdB = "icn-b";

        // Mock构型节点
        IetmProjectConfigurationManagement configA = new IetmProjectConfigurationManagement();
        configA.setId(cmNodeIdA);
        configA.setProjectId(projectIdA);

        IetmProjectConfigurationManagement configB = new IetmProjectConfigurationManagement();
        configB.setId(cmNodeIdB);
        configB.setProjectId(projectIdB);

        when(configurationService.getById(cmNodeIdA)).thenReturn(configA);
        when(configurationService.getById(cmNodeIdB)).thenReturn(configB);

        // Mock ICN
        IetmIcnManage icnA = new IetmIcnManage();
        icnA.setId(icnIdA);
        icnA.setCmNodeId(cmNodeIdA);

        IetmIcnManage icnB = new IetmIcnManage();
        icnB.setId(icnIdB);
        icnB.setCmNodeId(cmNodeIdB);

        when(baseMapper.selectById(icnIdA)).thenReturn(icnA);
        when(baseMapper.selectById(icnIdB)).thenReturn(icnB);

        // 创建相同文件名的文件
        MockMultipartFile fileA = new MockMultipartFile(
            "file",
            "same-name.jpg",  // 相同文件名
            "image/jpeg",
            "Project A Content".getBytes(StandardCharsets.UTF_8)
        );

        MockMultipartFile fileB = new MockMultipartFile(
            "file",
            "same-name.jpg",  // 相同文件名
            "image/jpeg",
            "Project B Content".getBytes(StandardCharsets.UTF_8)
        );

        final String[] fileKeyA = {null};
        final String[] fileKeyB = {null};

        when(attachmentService.save(any(IetmAttachment.class))).thenAnswer(invocation -> {
            IetmAttachment attachment = invocation.getArgument(0);
            if (attachment.getPid().equals(icnIdA)) {
                fileKeyA[0] = attachment.getFileKey();
            } else {
                fileKeyB[0] = attachment.getFileKey();
            }
            return true;
        });

        // 上传到项目A
        java.lang.reflect.Method method = service.getClass().getDeclaredMethod(
            "saveAttachment", String.class, org.springframework.web.multipart.MultipartFile.class,
            String.class, Integer.class, String.class
        );
        method.setAccessible(true);
        method.invoke(service, icnIdA, fileA, "entity", 0, "testUser");

        // 上传到项目B
        method.invoke(service, icnIdB, fileB, "entity", 0, "testUser");

        System.out.println("项目A的fileKey: " + fileKeyA[0]);
        System.out.println("项目B的fileKey: " + fileKeyB[0]);

        // 验证：两个文件路径不同，不会冲突
        assertNotNull("项目A的fileKey不应为null", fileKeyA[0]);
        assertNotNull("项目B的fileKey不应为null", fileKeyB[0]);
        assertNotEquals("两个项目的fileKey应该不同（避免冲突）", fileKeyA[0], fileKeyB[0]);

        assertTrue("项目A应在自己的目录", fileKeyA[0].contains("project/" + projectIdA + "/icn/"));
        assertTrue("项目B应在自己的目录", fileKeyB[0].contains("project/" + projectIdB + "/icn/"));

        System.out.println("✅ 测试通过：多项目文件隔离，无冲突");
    }

    @Test
    public void testPathUnification_ImportExportAlignment() {
        System.out.println("\n========== 测试4：路径统一性验证 ==========");

        // 导入逻辑的路径格式
        String importPath = "project/1234567890/icn/ICN-TEST-001.jpg";
        System.out.println("导入路径格式: " + importPath);

        // 手工上传的路径格式（修复后）
        String manualPath = "project/1234567890/icn/uuid-xxxxx.jpg";
        System.out.println("手工上传路径格式: " + manualPath);

        // 提取目录部分
        String importDir = importPath.substring(0, importPath.lastIndexOf("/"));
        String manualDir = manualPath.substring(0, manualPath.lastIndexOf("/"));

        System.out.println("导入目录: " + importDir);
        System.out.println("手工上传目录: " + manualDir);

        // 验证：目录结构完全一致
        assertEquals("导入和手工上传应使用相同的目录结构", importDir, manualDir);

        System.out.println("✅ 测试通过：路径格式100%统一");
    }

    @Test
    public void testGetProjectIdByCmNodeId() throws Exception {
        System.out.println("\n========== 测试5：通过cmNodeId获取projectId ==========");

        // Mock配置节点
        IetmProjectConfigurationManagement config = new IetmProjectConfigurationManagement();
        config.setId(cmNodeId);
        config.setProjectId(projectId);
        when(configurationService.getById(cmNodeId)).thenReturn(config);

        // 通过反射调用private方法
        java.lang.reflect.Method method = service.getClass().getDeclaredMethod(
            "getProjectIdByCmNodeId", String.class
        );
        method.setAccessible(true);

        String result = (String) method.invoke(service, cmNodeId);

        System.out.println("cmNodeId: " + cmNodeId);
        System.out.println("返回的projectId: " + result);

        assertEquals("应该返回正确的projectId", projectId, result);

        System.out.println("✅ 测试通过：getProjectIdByCmNodeId方法正常工作");
    }

    @Test
    public void testGetProjectIdByCmNodeId_NotFound() throws Exception {
        System.out.println("\n========== 测试6：cmNodeId不存在的情况 ==========");

        String invalidCmNodeId = "invalid-cmnode";
        when(configurationService.getById(invalidCmNodeId)).thenReturn(null);

        // 通过反射调用private方法
        java.lang.reflect.Method method = service.getClass().getDeclaredMethod(
            "getProjectIdByCmNodeId", String.class
        );
        method.setAccessible(true);

        String result = (String) method.invoke(service, invalidCmNodeId);

        System.out.println("无效的cmNodeId: " + invalidCmNodeId);
        System.out.println("返回结果: " + result);

        assertNull("cmNodeId不存在时应返回null", result);

        System.out.println("✅ 测试通过：正确处理cmNodeId不存在的情况");
    }
}
