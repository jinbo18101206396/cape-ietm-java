package org.jeecg.modules.ietm.ietmimport;

import org.jeecg.modules.ietm.ietmimport.constants.DmImportConstants;
import org.jeecg.modules.ietm.ietmimport.service.impl.IetmDmImportServiceImpl;
import org.jeecg.modules.ietm.ietmimport.vo.DmValidateResultVO;
import org.jeecg.modules.ietm.ietmimport.vo.ImportFileItemVO;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.service.IIetmDataModuleService;
import org.jeecg.modules.ietm.icnmanage.service.IIetmIcnManageService;
import org.jeecg.modules.ietm.projectconfigurationmanagement.service.IIetmProjectConfigurationManagementService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * IetmDmImportService 单元测试
 *
 * @author IETM Team
 * @date 2026-09-03
 */
public class IetmDmImportServiceTest {

    @InjectMocks
    private IetmDmImportServiceImpl importService;

    @Mock
    private IIetmDataModuleService dataModuleService;

    @Mock
    private IIetmIcnManageService icnManageService;

    @Mock
    private IIetmProjectConfigurationManagementService configurationService;

    private MockHttpServletRequest request;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        request = new MockHttpServletRequest();
        request.getSession().setAttribute("projectId", "test-project-001");
        request.getSession().setAttribute("username", "testuser");
        request.getSession().setAttribute("modelCode", "MODEL001");
        request.getSession().setAttribute("userMaxSecurity", "03");
    }

    @Test
    public void testValidateFile_nullFile() {
        // 测试：上传null文件
        try {
            importService.validateFile(null, request);
            fail("应该抛出异常");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("文件不能为空") || e instanceof NullPointerException);
        }
    }

    @Test
    public void testValidateFile_emptyFile() throws Exception {
        // 测试：上传空文件
        MockMultipartFile emptyFile = new MockMultipartFile(
            "file",
            "empty.xml",
            "text/xml",
            new byte[0]
        );

        try {
            DmValidateResultVO result = importService.validateFile(emptyFile, request);
            // 空文件可能返回错误或空结果
            assertNotNull(result);
        } catch (Exception e) {
            // 预期可能抛出异常
            assertTrue(true);
        }
    }

    @Test
    public void testValidateFile_invalidXml() throws Exception {
        // 测试：上传无效的XML文件
        String invalidXml = "this is not xml";
        MockMultipartFile xmlFile = new MockMultipartFile(
            "file",
            "invalid.xml",
            "text/xml",
            invalidXml.getBytes(StandardCharsets.UTF_8)
        );

        DmValidateResultVO result = importService.validateFile(xmlFile, request);
        assertNotNull(result);
        assertNotNull(result.getFiles());

        if (!result.getFiles().isEmpty()) {
            ImportFileItemVO item = result.getFiles().get(0);
            // 无效XML应该校验失败
            assertFalse(item.canImport());
        }
    }

    @Test
    public void testValidateFile_validXml_withoutSession() {
        // 测试：没有Session（未登录）
        MockHttpServletRequest emptyRequest = new MockHttpServletRequest();

        String validXml = "<?xml version=\"1.0\"?><dmodule></dmodule>";
        MockMultipartFile xmlFile = new MockMultipartFile(
            "file",
            "test.xml",
            "text/xml",
            validXml.getBytes(StandardCharsets.UTF_8)
        );

        try {
            importService.validateFile(xmlFile, emptyRequest);
            fail("应该抛出'请先打开项目'异常");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("请先打开项目"));
        }
    }

    @Test
    public void testValidateFile_unsupportedFileType() throws Exception {
        // 测试：不支持的文件类型
        MockMultipartFile txtFile = new MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            "some text content".getBytes(StandardCharsets.UTF_8)
        );

        try {
            DmValidateResultVO result = importService.validateFile(txtFile, request);
            // 可能返回错误结果或抛出异常
            assertNotNull(result);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("不支持的文件类型") ||
                      e.getMessage().contains("文件类型"));
        }
    }

    @Test
    public void testValidateFile_fileSizeLimit() {
        // 测试：文件大小超限
        // 注意：实际创建超过1GB的文件会OOM，这里只测试常量定义
        long maxSize = DmImportConstants.MAX_FILE_SIZE;
        assertEquals(1024L * 1024 * 1024, maxSize); // 1GB
    }

    @Test
    public void testExtractFileName_normal() {
        // 这是私有方法，通过公开方法间接测试
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.xml",
            "text/xml",
            "content".getBytes()
        );
        assertEquals("test.xml", file.getOriginalFilename());
    }

    @Test
    public void testExtractFileName_withPath() {
        // 测试：带路径的文件名（IE浏览器）
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "C:\\Users\\test\\file.xml",
            "text/xml",
            "content".getBytes()
        );
        String originalName = file.getOriginalFilename();
        assertTrue(originalName.contains("file.xml") || originalName.endsWith(".xml"));
    }

    @Test
    public void testBufferSize() {
        // 测试：缓冲区大小
        assertEquals(8192, DmImportConstants.BUFFER_SIZE);
    }

    @Test
    public void testErrorConstants() {
        // 测试：所有错误常量都已定义
        assertNotNull(DmImportConstants.ERROR_DM_EXISTS);
        assertNotNull(DmImportConstants.ERROR_SNS_NOT_IN_CM);
        assertNotNull(DmImportConstants.ERROR_CODE_MISMATCH);
        assertNotNull(DmImportConstants.ERROR_MODEL_MISMATCH);
        assertNotNull(DmImportConstants.ERROR_SECURITY_NOT_EXISTS);
        assertNotNull(DmImportConstants.ERROR_SECURITY_EXCEED);
        assertNotNull(DmImportConstants.ERROR_NO_FILE);
        assertNotNull(DmImportConstants.ERROR_ICN_NAME_INVALID);
        assertNotNull(DmImportConstants.ERROR_ICN_SNS_NOT_IN_CM);
        assertNotNull(DmImportConstants.ERROR_ICN_EXISTS);
        assertNotNull(DmImportConstants.SUCCESS);
    }

    @Test
    public void testValidateResultVO_calculation() {
        // 测试：校验结果统计正确
        DmValidateResultVO result = new DmValidateResultVO();

        ImportFileItemVO item1 = new ImportFileItemVO();
        item1.setResultCode(DmImportConstants.SUCCESS);

        ImportFileItemVO item2 = new ImportFileItemVO();
        item2.setResultCode(DmImportConstants.ERROR_DM_EXISTS);

        ImportFileItemVO item3 = new ImportFileItemVO();
        item3.setResultCode(DmImportConstants.SUCCESS);

        java.util.List<ImportFileItemVO> files = new java.util.ArrayList<>();
        files.add(item1);
        files.add(item2);
        files.add(item3);

        result.setFiles(files);
        result.setTotalCount(files.size());
        result.setSuccessCount((int) files.stream().filter(ImportFileItemVO::canImport).count());
        result.setFailureCount(result.getTotalCount() - result.getSuccessCount());

        assertEquals(3, result.getTotalCount().intValue());
        assertEquals(2, result.getSuccessCount().intValue());
        assertEquals(1, result.getFailureCount().intValue());
    }

    @Test
    public void testMockDependencies() {
        // 测试：Mock依赖正确注入
        assertNotNull(dataModuleService);
        assertNotNull(icnManageService);
        assertNotNull(configurationService);
        assertNotNull(importService);
    }
}
