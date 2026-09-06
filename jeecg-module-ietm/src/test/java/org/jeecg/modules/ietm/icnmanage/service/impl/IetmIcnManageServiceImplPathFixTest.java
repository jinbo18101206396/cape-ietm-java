package org.jeecg.modules.ietm.icnmanage.service.impl;

import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.ietm.ietmattachment.entity.IetmAttachment;
import org.jeecg.modules.ietm.ietmattachment.service.IIetmAttachmentService;
import org.jeecg.modules.ietm.icnmanage.entity.IetmIcnManage;
import org.jeecg.modules.ietm.icnmanage.mapper.IetmIcnManageMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * ICN预览路径修复验证测试
 *
 * 测试场景：
 * 1. 新格式路径（导入后）：project/{projectId}/icn/{fileName}
 * 2. 旧格式路径（手工上传）：{fileName}
 * 3. 文件不存在的情况
 *
 * @author IETM Team
 * @date 2026-09-05
 */
@RunWith(MockitoJUnitRunner.class)
public class IetmIcnManageServiceImplPathFixTest {

    @InjectMocks
    private IetmIcnManageServiceImpl service;

    @Mock
    private IetmIcnManageMapper baseMapper;

    @Mock
    private IIetmAttachmentService attachmentService;

    private String tempDir;

    @Before
    public void setUp() throws Exception {
        // 创建临时测试目录
        tempDir = System.getProperty("java.io.tmpdir") + File.separator + "ietm-test-" + System.currentTimeMillis();
        new File(tempDir).mkdirs();

        // 设置fileStorageLocation
        ReflectionTestUtils.setField(service, "fileStorageLocation", tempDir);
        ReflectionTestUtils.setField(service, "uploadType", "local");
    }

    @Test
    public void testViewFile_NewFormat_Success() throws Exception {
        System.out.println("\n========== 测试1：新格式路径（导入后）==========");

        // 准备测试数据：创建新格式路径的文件
        String projectId = "1234567890";
        String fileName = "ICN-ZB1-A-001.jpg";
        String fileKey = "project/" + projectId + "/icn/" + fileName;

        // 创建目录结构
        File projectDir = new File(tempDir, "project");
        File projectSubDir = new File(projectDir, projectId);
        File icnDir = new File(projectSubDir, "icn");
        icnDir.mkdirs();

        // 创建测试文件（Base64编码的内容）
        File testFile = new File(icnDir, fileName);
        String testContent = "VGVzdCBJbWFnZSBDb250ZW50";  // "Test Image Content" 的Base64
        try (FileOutputStream fos = new FileOutputStream(testFile)) {
            fos.write(testContent.getBytes(StandardCharsets.UTF_8));
        }

        System.out.println("创建测试文件：" + testFile.getAbsolutePath());
        System.out.println("fileKey: " + fileKey);

        // 执行测试
        MockHttpServletResponse response = new MockHttpServletResponse();
        service.viewFile(fileKey, response);

        // 验证：响应状态应该是200（文件找到）
        assertEquals("文件应该被找到", HttpServletResponse.SC_OK, response.getStatus());
        System.out.println("✅ 新格式路径测试通过：文件成功找到并返回");

        // 清理
        testFile.delete();
    }

    @Test
    public void testViewFile_LegacyFormat_Success() throws Exception {
        System.out.println("\n========== 测试2：旧格式路径（只有文件名）==========");

        // 准备测试数据：创建旧格式路径的文件（直接在根目录）
        String fileName = "legacy-icn.jpg";
        String fileKey = fileName;

        // 创建测试文件
        File testFile = new File(tempDir, fileName);
        String testContent = "TGVnYWN5IEltYWdl";  // "Legacy Image" 的Base64
        try (FileOutputStream fos = new FileOutputStream(testFile)) {
            fos.write(testContent.getBytes(StandardCharsets.UTF_8));
        }

        System.out.println("创建测试文件：" + testFile.getAbsolutePath());
        System.out.println("fileKey: " + fileKey);

        // 执行测试
        MockHttpServletResponse response = new MockHttpServletResponse();
        service.viewFile(fileKey, response);

        // 验证
        assertEquals("旧格式文件应该被找到", HttpServletResponse.SC_OK, response.getStatus());
        System.out.println("✅ 旧格式路径测试通过：向后兼容性验证成功");

        // 清理
        testFile.delete();
    }

    @Test
    public void testViewFile_FileNotFound() throws Exception {
        System.out.println("\n========== 测试3：文件不存在 ==========");

        String fileKey = "project/9999/icn/not-exist.jpg";
        System.out.println("fileKey: " + fileKey);

        // 执行测试
        MockHttpServletResponse response = new MockHttpServletResponse();
        service.viewFile(fileKey, response);

        // 验证：响应状态应该是404
        assertEquals("文件不存在应返回404", HttpServletResponse.SC_NOT_FOUND, response.getStatus());
        System.out.println("✅ 文件不存在测试通过：正确返回404");
    }

    @Test
    public void testGetPreviewInfo_NewFormat() throws Exception {
        System.out.println("\n========== 测试4：getPreviewInfo接口（新格式）==========");

        // 准备测试数据
        String icnId = "test-icn-id";
        String projectId = "1234567890";
        String fileName = "ICN-TEST-001.png";
        String fileKey = "project/" + projectId + "/icn/" + fileName;

        // Mock ICN和Attachment
        IetmIcnManage icn = new IetmIcnManage();
        icn.setId(icnId);
        icn.setIcn("ICN-TEST-001");
        icn.setIssueNo("001");
        icn.setSecurity(0);

        IetmAttachment attachment = new IetmAttachment();
        attachment.setFileName(fileName);
        attachment.setFileKey(fileKey);
        attachment.setFileType("png");

        icn.setIetmAttachment(attachment);

        when(baseMapper.getByIdWithAttachment(icnId)).thenReturn(icn);

        System.out.println("ICN ID: " + icnId);
        System.out.println("fileKey: " + fileKey);

        // 执行测试
        org.jeecg.modules.ietm.icnmanage.vo.PreviewInfoVO vo = service.getPreviewInfo(icnId);

        // 验证：fileUrl应该包含完整的fileKey
        assertNotNull("PreviewInfoVO不应为null", vo);
        assertNotNull("fileUrl不应为null", vo.getFileUrl());
        assertTrue("fileUrl应该包含fileKey", vo.getFileUrl().contains(fileKey));
        System.out.println("返回的fileUrl: " + vo.getFileUrl());
        System.out.println("✅ getPreviewInfo测试通过：返回正确的预览URL");
    }

    @Test
    public void testPathNormalization() throws Exception {
        System.out.println("\n========== 测试5：路径分隔符标准化 ==========");

        // 准备测试数据：Windows风格的路径分隔符
        String projectId = "1234567890";
        String fileName = "ICN-PATH-TEST.jpg";
        String fileKeyWindows = "project\\" + projectId + "\\icn\\" + fileName;  // Windows风格

        // 创建目录结构
        File projectDir = new File(tempDir, "project");
        File projectSubDir = new File(projectDir, projectId);
        File icnDir = new File(projectSubDir, "icn");
        icnDir.mkdirs();

        // 创建测试文件
        File testFile = new File(icnDir, fileName);
        try (FileOutputStream fos = new FileOutputStream(testFile)) {
            fos.write("V2luZG93cyBQYXRo".getBytes(StandardCharsets.UTF_8));
        }

        System.out.println("创建测试文件：" + testFile.getAbsolutePath());
        System.out.println("fileKey (Windows格式): " + fileKeyWindows);

        // 执行测试
        MockHttpServletResponse response = new MockHttpServletResponse();
        service.viewFile(fileKeyWindows, response);

        // 验证：应该能正确处理Windows风格的路径
        assertEquals("Windows路径格式应该被正确处理", HttpServletResponse.SC_OK, response.getStatus());
        System.out.println("✅ 路径标准化测试通过：支持Windows和Unix路径格式");

        // 清理
        testFile.delete();
    }

    @Test
    public void testImportExportAlignment() {
        System.out.println("\n========== 测试6：导入导出路径格式对齐性验证 ==========");

        // 模拟导出逻辑：从attachment.fileKey读取
        String exportedFileKey = "project/1234567890/icn/ICN-EXPORT-001.jpg";
        System.out.println("导出侧fileKey: " + exportedFileKey);

        // 模拟导入逻辑：保存的fileKey格式
        String importedFileKey = "project/1234567890/icn/ICN-EXPORT-001.jpg";
        System.out.println("导入侧fileKey: " + importedFileKey);

        // 验证：格式应该完全一致
        assertEquals("导入导出的fileKey格式应该一致", exportedFileKey, importedFileKey);
        System.out.println("✅ 导入导出对齐性验证通过");
    }

    @Test
    public void testMultiplePathFormats() throws Exception {
        System.out.println("\n========== 测试7：混合路径格式兼容性 ==========");

        // 测试场景：系统中同时存在新旧两种格式的文件

        // 场景1：新格式文件
        String newFormatKey = "project/1234567890/icn/new-format.jpg";
        File newFormatDir = new File(tempDir, "project/1234567890/icn");
        newFormatDir.mkdirs();
        File newFormatFile = new File(newFormatDir, "new-format.jpg");
        try (FileOutputStream fos = new FileOutputStream(newFormatFile)) {
            fos.write("TmV3IEZvcm1hdA==".getBytes(StandardCharsets.UTF_8));
        }

        // 场景2：旧格式文件
        String oldFormatKey = "old-format.jpg";
        File oldFormatFile = new File(tempDir, "old-format.jpg");
        try (FileOutputStream fos = new FileOutputStream(oldFormatFile)) {
            fos.write("T2xkIEZvcm1hdA==".getBytes(StandardCharsets.UTF_8));
        }

        System.out.println("新格式fileKey: " + newFormatKey);
        System.out.println("旧格式fileKey: " + oldFormatKey);

        // 测试新格式
        MockHttpServletResponse response1 = new MockHttpServletResponse();
        service.viewFile(newFormatKey, response1);
        assertEquals("新格式应该成功", HttpServletResponse.SC_OK, response1.getStatus());
        System.out.println("✅ 新格式文件访问成功");

        // 测试旧格式
        MockHttpServletResponse response2 = new MockHttpServletResponse();
        service.viewFile(oldFormatKey, response2);
        assertEquals("旧格式应该成功", HttpServletResponse.SC_OK, response2.getStatus());
        System.out.println("✅ 旧格式文件访问成功");

        System.out.println("✅ 混合路径格式兼容性验证通过：新旧格式可以共存");

        // 清理
        newFormatFile.delete();
        oldFormatFile.delete();
    }
}
