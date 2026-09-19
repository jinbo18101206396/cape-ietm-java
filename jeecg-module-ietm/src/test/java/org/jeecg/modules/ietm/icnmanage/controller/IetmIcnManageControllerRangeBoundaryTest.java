package org.jeecg.modules.ietm.icnmanage.controller;

import org.jeecg.modules.ietm.icnmanage.entity.IetmIcnManage;
import org.jeecg.modules.ietm.icnmanage.service.IIetmIcnManageService;
import org.jeecg.modules.ietm.ietmattachment.entity.IetmAttachment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Range请求边界和异常测试
 *
 * 测试范围：
 * 1. 极端边界值（0字节文件、超大文件）
 * 2. 异常Range格式
 * 3. 多Range请求（暂不支持）
 * 4. 特殊字符处理
 * 5. 并发Range请求
 *
 * @author jeecg-boot
 * @since 2026-09-18
 */
@ExtendWith(MockitoExtension.class)
class IetmIcnManageControllerRangeBoundaryTest {

    @Mock
    private IIetmIcnManageService ietmIcnManageService;

    @InjectMocks
    private IetmIcnManageController controller;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @TempDir
    Path tempDir;

    private String testIcnCode = "ICN-TEST-BOUNDARY-001";
    private IetmIcnManage mockIcn;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        mockIcn = new IetmIcnManage();
        mockIcn.setId("2001");
        mockIcn.setIcn(testIcnCode);

        IetmAttachment attachment = new IetmAttachment();
        attachment.setFileKey("test-boundary.dat");
        attachment.setFileName("test-boundary.dat");
        mockIcn.setIetmAttachment(attachment);
    }

    @Test
    void testViewIcn_EmptyFile() throws Exception {
        // Given - 0字节文件
        byte[] emptyContent = new byte[0];

        when(ietmIcnManageService.getOne(any())).thenReturn(mockIcn);
        doNothing().when(ietmIcnManageService).loadAttachment(any());

        Path uploadDir = tempDir.resolve("upload").resolve("files");
        Files.createDirectories(uploadDir);
        Path emptyFile = uploadDir.resolve("test-boundary.dat");
        Files.write(emptyFile, emptyContent);
        System.setProperty("user.dir", tempDir.toString());

        // When
        controller.viewIcn(testIcnCode, request, response);

        // Then
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertEquals(0, response.getContentLength());
        assertEquals(0, response.getContentAsByteArray().length);
    }

    @Test
    void testViewIcn_SingleByteFile() throws Exception {
        // Given - 1字节文件
        byte[] singleByteContent = new byte[]{0x42};

        when(ietmIcnManageService.getOne(any())).thenReturn(mockIcn);
        doNothing().when(ietmIcnManageService).loadAttachment(any());

        Path uploadDir = tempDir.resolve("upload").resolve("files");
        Files.createDirectories(uploadDir);
        Path singleByteFile = uploadDir.resolve("test-boundary.dat");
        Files.write(singleByteFile, singleByteContent);
        System.setProperty("user.dir", tempDir.toString());

        // When - Range请求整个文件
        request.addHeader("Range", "bytes=0-0");
        controller.viewIcn(testIcnCode, request, response);

        // Then
        assertEquals(HttpServletResponse.SC_PARTIAL_CONTENT, response.getStatus());
        assertEquals("bytes 0-0/1", response.getHeader("Content-Range"));
        assertEquals("1", response.getHeader("Content-Length"));
        assertArrayEquals(singleByteContent, response.getContentAsByteArray());
    }

    @Test
    void testViewIcn_RangeRequest_StartEqualsEnd() throws Exception {
        // Given - Range: bytes=100-100（单字节）
        byte[] testContent = new byte[1000];
        for (int i = 0; i < testContent.length; i++) {
            testContent[i] = (byte) (i % 256);
        }

        when(ietmIcnManageService.getOne(any())).thenReturn(mockIcn);
        doNothing().when(ietmIcnManageService).loadAttachment(any());

        Path uploadDir = tempDir.resolve("upload").resolve("files");
        Files.createDirectories(uploadDir);
        Path testFile = uploadDir.resolve("test-boundary.dat");
        Files.write(testFile, testContent);
        System.setProperty("user.dir", tempDir.toString());

        // When
        request.addHeader("Range", "bytes=100-100");
        controller.viewIcn(testIcnCode, request, response);

        // Then
        assertEquals(HttpServletResponse.SC_PARTIAL_CONTENT, response.getStatus());
        assertEquals("bytes 100-100/1000", response.getHeader("Content-Range"));
        assertEquals("1", response.getHeader("Content-Length"));
        assertEquals(1, response.getContentAsByteArray().length);
        assertEquals(testContent[100], response.getContentAsByteArray()[0]);
    }

    @Test
    void testViewIcn_RangeRequest_StartAtZero() throws Exception {
        // Given - Range: bytes=0-
        byte[] testContent = new byte[1000];

        when(ietmIcnManageService.getOne(any())).thenReturn(mockIcn);
        doNothing().when(ietmIcnManageService).loadAttachment(any());

        Path uploadDir = tempDir.resolve("upload").resolve("files");
        Files.createDirectories(uploadDir);
        Path testFile = uploadDir.resolve("test-boundary.dat");
        Files.write(testFile, testContent);
        System.setProperty("user.dir", tempDir.toString());

        // When
        request.addHeader("Range", "bytes=0-");
        controller.viewIcn(testIcnCode, request, response);

        // Then
        assertEquals(HttpServletResponse.SC_PARTIAL_CONTENT, response.getStatus());
        assertEquals("bytes 0-999/1000", response.getHeader("Content-Range"));
        assertEquals("1000", response.getHeader("Content-Length"));
    }

    @Test
    void testViewIcn_RangeRequest_EndAtLastByte() throws Exception {
        // Given - Range: bytes=-1（最后1字节）
        byte[] testContent = new byte[1000];
        for (int i = 0; i < testContent.length; i++) {
            testContent[i] = (byte) (i % 256);
        }

        when(ietmIcnManageService.getOne(any())).thenReturn(mockIcn);
        doNothing().when(ietmIcnManageService).loadAttachment(any());

        Path uploadDir = tempDir.resolve("upload").resolve("files");
        Files.createDirectories(uploadDir);
        Path testFile = uploadDir.resolve("test-boundary.dat");
        Files.write(testFile, testContent);
        System.setProperty("user.dir", tempDir.toString());

        // When
        request.addHeader("Range", "bytes=-1");
        controller.viewIcn(testIcnCode, request, response);

        // Then
        assertEquals(HttpServletResponse.SC_PARTIAL_CONTENT, response.getStatus());
        assertEquals("bytes 999-999/1000", response.getHeader("Content-Range"));
        assertEquals("1", response.getHeader("Content-Length"));
        assertEquals(testContent[999], response.getContentAsByteArray()[0]);
    }

    @Test
    void testViewIcn_RangeRequest_NegativeStart() throws Exception {
        // Given - Range: bytes=-1000（最后1000字节，但文件只有1000字节）
        byte[] testContent = new byte[1000];

        when(ietmIcnManageService.getOne(any())).thenReturn(mockIcn);
        doNothing().when(ietmIcnManageService).loadAttachment(any());

        Path uploadDir = tempDir.resolve("upload").resolve("files");
        Files.createDirectories(uploadDir);
        Path testFile = uploadDir.resolve("test-boundary.dat");
        Files.write(testFile, testContent);
        System.setProperty("user.dir", tempDir.toString());

        // When
        request.addHeader("Range", "bytes=-1000");
        controller.viewIcn(testIcnCode, request, response);

        // Then - 应该返回整个文件
        assertEquals(HttpServletResponse.SC_PARTIAL_CONTENT, response.getStatus());
        assertEquals("bytes 0-999/1000", response.getHeader("Content-Range"));
        assertEquals("1000", response.getHeader("Content-Length"));
    }

    @Test
    void testViewIcn_RangeRequest_NegativeStartExceedsFileSize() throws Exception {
        // Given - Range: bytes=-2000（请求最后2000字节，但文件只有1000字节）
        byte[] testContent = new byte[1000];

        when(ietmIcnManageService.getOne(any())).thenReturn(mockIcn);
        doNothing().when(ietmIcnManageService).loadAttachment(any());

        Path uploadDir = tempDir.resolve("upload").resolve("files");
        Files.createDirectories(uploadDir);
        Path testFile = uploadDir.resolve("test-boundary.dat");
        Files.write(testFile, testContent);
        System.setProperty("user.dir", tempDir.toString());

        // When
        request.addHeader("Range", "bytes=-2000");
        controller.viewIcn(testIcnCode, request, response);

        // Then - 应该从0开始返回整个文件
        assertEquals(HttpServletResponse.SC_PARTIAL_CONTENT, response.getStatus());
        assertEquals("bytes 0-999/1000", response.getHeader("Content-Range"));
    }

    @Test
    void testViewIcn_RangeRequest_StartBeyondFileSize() throws Exception {
        // Given - Range: bytes=2000-（start超出文件大小）
        byte[] testContent = new byte[1000];

        when(ietmIcnManageService.getOne(any())).thenReturn(mockIcn);
        doNothing().when(ietmIcnManageService).loadAttachment(any());

        Path uploadDir = tempDir.resolve("upload").resolve("files");
        Files.createDirectories(uploadDir);
        Path testFile = uploadDir.resolve("test-boundary.dat");
        Files.write(testFile, testContent);
        System.setProperty("user.dir", tempDir.toString());

        // When
        request.addHeader("Range", "bytes=2000-");
        controller.viewIcn(testIcnCode, request, response);

        // Then - 应该返回416 Range Not Satisfiable
        assertEquals(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE, response.getStatus());
        assertEquals("bytes */1000", response.getHeader("Content-Range"));
    }

    @Test
    void testViewIcn_RangeRequest_InvalidFormatWithSpaces() throws Exception {
        // Given - Range格式包含空格
        byte[] testContent = new byte[1000];

        when(ietmIcnManageService.getOne(any())).thenReturn(mockIcn);
        doNothing().when(ietmIcnManageService).loadAttachment(any());

        Path uploadDir = tempDir.resolve("upload").resolve("files");
        Files.createDirectories(uploadDir);
        Path testFile = uploadDir.resolve("test-boundary.dat");
        Files.write(testFile, testContent);
        System.setProperty("user.dir", tempDir.toString());

        // When
        request.addHeader("Range", "bytes= 0 - 100 ");
        controller.viewIcn(testIcnCode, request, response);

        // Then - 应该返回400 Bad Request
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
    }

    @Test
    void testViewIcn_RangeRequest_MultipleRanges() throws Exception {
        // Given - 多Range请求（当前实现不支持，会解析失败）
        byte[] testContent = new byte[1000];

        when(ietmIcnManageService.getOne(any())).thenReturn(mockIcn);
        doNothing().when(ietmIcnManageService).loadAttachment(any());

        Path uploadDir = tempDir.resolve("upload").resolve("files");
        Files.createDirectories(uploadDir);
        Path testFile = uploadDir.resolve("test-boundary.dat");
        Files.write(testFile, testContent);
        System.setProperty("user.dir", tempDir.toString());

        // When
        request.addHeader("Range", "bytes=0-99,200-299");
        controller.viewIcn(testIcnCode, request, response);

        // Then - 当前实现会因为逗号导致NumberFormatException，返回400
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
    }

    @Test
    void testViewIcn_RangeRequest_EmptyRange() throws Exception {
        // Given - Range头为空（会被当做普通请求处理）
        byte[] testContent = new byte[1000];

        when(ietmIcnManageService.getOne(any())).thenReturn(mockIcn);
        doNothing().when(ietmIcnManageService).loadAttachment(any());

        Path uploadDir = tempDir.resolve("upload").resolve("files");
        Files.createDirectories(uploadDir);
        Path testFile = uploadDir.resolve("test-boundary.dat");
        Files.write(testFile, testContent);
        System.setProperty("user.dir", tempDir.toString());

        // When
        request.addHeader("Range", "bytes=");
        controller.viewIcn(testIcnCode, request, response);

        // Then - "bytes="不会进入Range处理逻辑，作为普通请求处理
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }

    @Test
    void testViewIcn_RangeRequest_OnlyDash() throws Exception {
        // Given - Range只有一个破折号（会被当做普通请求处理）
        byte[] testContent = new byte[1000];

        when(ietmIcnManageService.getOne(any())).thenReturn(mockIcn);
        doNothing().when(ietmIcnManageService).loadAttachment(any());

        Path uploadDir = tempDir.resolve("upload").resolve("files");
        Files.createDirectories(uploadDir);
        Path testFile = uploadDir.resolve("test-boundary.dat");
        Files.write(testFile, testContent);
        System.setProperty("user.dir", tempDir.toString());

        // When
        request.addHeader("Range", "bytes=-");
        controller.viewIcn(testIcnCode, request, response);

        // Then - "bytes=-"不满足格式，可能作为普通请求或返回错误
        // 实际测试发现返回200（当做普通请求处理）
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }

    @Test
    void testViewIcn_RangeRequest_VeryLargeNumbers() throws Exception {
        // Given - Range包含超大数字
        byte[] testContent = new byte[1000];

        when(ietmIcnManageService.getOne(any())).thenReturn(mockIcn);
        doNothing().when(ietmIcnManageService).loadAttachment(any());

        Path uploadDir = tempDir.resolve("upload").resolve("files");
        Files.createDirectories(uploadDir);
        Path testFile = uploadDir.resolve("test-boundary.dat");
        Files.write(testFile, testContent);
        System.setProperty("user.dir", tempDir.toString());

        // When
        request.addHeader("Range", "bytes=0-999999999999999");
        controller.viewIcn(testIcnCode, request, response);

        // Then - 应该自动校正到文件末尾
        assertEquals(HttpServletResponse.SC_PARTIAL_CONTENT, response.getStatus());
        assertEquals("bytes 0-999/1000", response.getHeader("Content-Range"));
    }

    @Test
    void testViewIcn_WithoutRangeHeader() throws Exception {
        // Given - 没有Range头（普通请求）
        byte[] testContent = new byte[1000];

        when(ietmIcnManageService.getOne(any())).thenReturn(mockIcn);
        doNothing().when(ietmIcnManageService).loadAttachment(any());

        Path uploadDir = tempDir.resolve("upload").resolve("files");
        Files.createDirectories(uploadDir);
        Path testFile = uploadDir.resolve("test-boundary.dat");
        Files.write(testFile, testContent);
        System.setProperty("user.dir", tempDir.toString());

        // When
        controller.viewIcn(testIcnCode, request, response);

        // Then
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertEquals(1000, response.getContentLength());
        assertEquals("bytes", response.getHeader("Accept-Ranges"));
    }

    @Test
    void testViewIcn_RangeRequest_ZeroLengthRange() throws Exception {
        // Given - bytes=100-99（无效的零长度范围）
        byte[] testContent = new byte[1000];

        when(ietmIcnManageService.getOne(any())).thenReturn(mockIcn);
        doNothing().when(ietmIcnManageService).loadAttachment(any());

        Path uploadDir = tempDir.resolve("upload").resolve("files");
        Files.createDirectories(uploadDir);
        Path testFile = uploadDir.resolve("test-boundary.dat");
        Files.write(testFile, testContent);
        System.setProperty("user.dir", tempDir.toString());

        // When
        request.addHeader("Range", "bytes=100-99");
        controller.viewIcn(testIcnCode, request, response);

        // Then
        assertEquals(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE, response.getStatus());
    }
}
