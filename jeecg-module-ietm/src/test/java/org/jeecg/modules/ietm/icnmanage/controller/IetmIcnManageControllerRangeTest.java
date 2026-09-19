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
 * IetmIcnManageController Range请求测试
 *
 * 测试范围：
 * 1. 普通请求（200 OK）
 * 2. Range请求基础场景（206 Partial Content）
 * 3. Range请求边界场景
 * 4. Range请求异常场景
 * 5. 视频拖动场景模拟
 *
 * @author jeecg-boot
 * @since 2026-09-18
 */
@ExtendWith(MockitoExtension.class)
class IetmIcnManageControllerRangeTest {

    @Mock
    private IIetmIcnManageService ietmIcnManageService;

    @InjectMocks
    private IetmIcnManageController controller;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @TempDir
    Path tempDir;

    private String testIcnCode = "ICN-TEST-VIDEO-001";
    private IetmIcnManage mockIcn;
    private byte[] testVideoContent;

    @BeforeEach
    void setUp() throws Exception {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        // 创建测试ICN实体
        mockIcn = new IetmIcnManage();
        mockIcn.setId("2001");
        mockIcn.setIcn(testIcnCode);

        // 创建测试附件
        IetmAttachment attachment = new IetmAttachment();
        attachment.setFileKey("test-video.mp4");
        attachment.setFileName("test-video.mp4");
        mockIcn.setIetmAttachment(attachment);

        // 创建测试视频文件（模拟100KB视频）
        testVideoContent = new byte[100 * 1024]; // 100KB
        for (int i = 0; i < testVideoContent.length; i++) {
            testVideoContent[i] = (byte) (i % 256);
        }

        // 创建临时文件
        Path videoFile = tempDir.resolve("test-video.mp4");
        Files.write(videoFile, testVideoContent);

        // Mock文件存储路径
        System.setProperty("user.dir", tempDir.getParent().toString());
    }

    @Test
    void testViewIcn_NormalRequest_200OK() throws Exception {
        // Given
        when(ietmIcnManageService.getOne(any())).thenReturn(mockIcn);
        doNothing().when(ietmIcnManageService).loadAttachment(any());

        // 创建测试文件
        Path uploadDir = tempDir.resolve("upload").resolve("files");
        Files.createDirectories(uploadDir);
        Path videoFile = uploadDir.resolve("test-video.mp4");
        Files.write(videoFile, testVideoContent);

        System.setProperty("user.dir", tempDir.toString());

        // When
        controller.viewIcn(testIcnCode, request, response);

        // Then
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        // 注意：测试文件是随机字节，MIME检测器会返回 application/octet-stream
        assertEquals("application/octet-stream", response.getContentType());
        assertEquals(testVideoContent.length, response.getContentLength());
        assertEquals("bytes", response.getHeader("Accept-Ranges"));
        assertNotNull(response.getHeader("Cache-Control"));

        // 验证返回了完整文件
        byte[] responseContent = response.getContentAsByteArray();
        assertEquals(testVideoContent.length, responseContent.length);
    }

    @Test
    void testViewIcn_RangeRequest_First1KB() throws Exception {
        // Given - 请求前1KB：bytes=0-1023
        request.addHeader("Range", "bytes=0-1023");
        when(ietmIcnManageService.getOne(any())).thenReturn(mockIcn);
        doNothing().when(ietmIcnManageService).loadAttachment(any());

        Path uploadDir = tempDir.resolve("upload").resolve("files");
        Files.createDirectories(uploadDir);
        Path videoFile = uploadDir.resolve("test-video.mp4");
        Files.write(videoFile, testVideoContent);
        System.setProperty("user.dir", tempDir.toString());

        // When
        controller.viewIcn(testIcnCode, request, response);

        // Then
        assertEquals(HttpServletResponse.SC_PARTIAL_CONTENT, response.getStatus());
        // 注意：测试文件是随机字节，MIME检测器会返回 application/octet-stream
        assertEquals("application/octet-stream", response.getContentType());
        assertEquals("bytes 0-1023/" + testVideoContent.length, response.getHeader("Content-Range"));
        assertEquals("1024", response.getHeader("Content-Length"));

        // 验证返回了正确的片段
        byte[] responseContent = response.getContentAsByteArray();
        assertEquals(1024, responseContent.length);
        for (int i = 0; i < 1024; i++) {
            assertEquals(testVideoContent[i], responseContent[i]);
        }
    }

    @Test
    void testViewIcn_RangeRequest_Middle() throws Exception {
        // Given - 请求中间部分：bytes=50000-51023
        request.addHeader("Range", "bytes=50000-51023");
        when(ietmIcnManageService.getOne(any())).thenReturn(mockIcn);
        doNothing().when(ietmIcnManageService).loadAttachment(any());

        Path uploadDir = tempDir.resolve("upload").resolve("files");
        Files.createDirectories(uploadDir);
        Path videoFile = uploadDir.resolve("test-video.mp4");
        Files.write(videoFile, testVideoContent);
        System.setProperty("user.dir", tempDir.toString());

        // When
        controller.viewIcn(testIcnCode, request, response);

        // Then
        assertEquals(HttpServletResponse.SC_PARTIAL_CONTENT, response.getStatus());
        assertEquals("bytes 50000-51023/" + testVideoContent.length, response.getHeader("Content-Range"));
        assertEquals("1024", response.getHeader("Content-Length"));

        // 验证返回了正确的片段
        byte[] responseContent = response.getContentAsByteArray();
        assertEquals(1024, responseContent.length);
        for (int i = 0; i < 1024; i++) {
            assertEquals(testVideoContent[50000 + i], responseContent[i]);
        }
    }

    @Test
    void testViewIcn_RangeRequest_FromStartToEnd() throws Exception {
        // Given - 请求从某位置到末尾：bytes=100000-
        request.addHeader("Range", "bytes=100000-");
        when(ietmIcnManageService.getOne(any())).thenReturn(mockIcn);
        doNothing().when(ietmIcnManageService).loadAttachment(any());

        Path uploadDir = tempDir.resolve("upload").resolve("files");
        Files.createDirectories(uploadDir);
        Path videoFile = uploadDir.resolve("test-video.mp4");
        Files.write(videoFile, testVideoContent);
        System.setProperty("user.dir", tempDir.toString());

        // When
        controller.viewIcn(testIcnCode, request, response);

        // Then
        int expectedLength = testVideoContent.length - 100000;
        assertEquals(HttpServletResponse.SC_PARTIAL_CONTENT, response.getStatus());
        assertEquals("bytes 100000-" + (testVideoContent.length - 1) + "/" + testVideoContent.length,
                response.getHeader("Content-Range"));
        assertEquals(String.valueOf(expectedLength), response.getHeader("Content-Length"));

        // 验证返回了正确的片段
        byte[] responseContent = response.getContentAsByteArray();
        assertEquals(expectedLength, responseContent.length);
    }

    @Test
    void testViewIcn_RangeRequest_LastNBytes() throws Exception {
        // Given - 请求最后1KB：bytes=-1024
        request.addHeader("Range", "bytes=-1024");
        when(ietmIcnManageService.getOne(any())).thenReturn(mockIcn);
        doNothing().when(ietmIcnManageService).loadAttachment(any());

        Path uploadDir = tempDir.resolve("upload").resolve("files");
        Files.createDirectories(uploadDir);
        Path videoFile = uploadDir.resolve("test-video.mp4");
        Files.write(videoFile, testVideoContent);
        System.setProperty("user.dir", tempDir.toString());

        // When
        controller.viewIcn(testIcnCode, request, response);

        // Then
        int start = testVideoContent.length - 1024;
        int end = testVideoContent.length - 1;
        assertEquals(HttpServletResponse.SC_PARTIAL_CONTENT, response.getStatus());
        assertEquals("bytes " + start + "-" + end + "/" + testVideoContent.length,
                response.getHeader("Content-Range"));
        assertEquals("1024", response.getHeader("Content-Length"));

        // 验证返回了最后1KB
        byte[] responseContent = response.getContentAsByteArray();
        assertEquals(1024, responseContent.length);
        for (int i = 0; i < 1024; i++) {
            assertEquals(testVideoContent[start + i], responseContent[i]);
        }
    }

    @Test
    void testViewIcn_RangeRequest_InvalidRange_StartGreaterThanEnd() throws Exception {
        // Given - 无效Range：start > end
        request.addHeader("Range", "bytes=5000-1000");
        when(ietmIcnManageService.getOne(any())).thenReturn(mockIcn);
        doNothing().when(ietmIcnManageService).loadAttachment(any());

        Path uploadDir = tempDir.resolve("upload").resolve("files");
        Files.createDirectories(uploadDir);
        Path videoFile = uploadDir.resolve("test-video.mp4");
        Files.write(videoFile, testVideoContent);
        System.setProperty("user.dir", tempDir.toString());

        // When
        controller.viewIcn(testIcnCode, request, response);

        // Then
        assertEquals(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE, response.getStatus());
        assertEquals("bytes */" + testVideoContent.length, response.getHeader("Content-Range"));
    }

    @Test
    void testViewIcn_RangeRequest_InvalidFormat() throws Exception {
        // Given - 无效Range格式
        request.addHeader("Range", "bytes=abc-def");
        when(ietmIcnManageService.getOne(any())).thenReturn(mockIcn);
        doNothing().when(ietmIcnManageService).loadAttachment(any());

        Path uploadDir = tempDir.resolve("upload").resolve("files");
        Files.createDirectories(uploadDir);
        Path videoFile = uploadDir.resolve("test-video.mp4");
        Files.write(videoFile, testVideoContent);
        System.setProperty("user.dir", tempDir.toString());

        // When
        controller.viewIcn(testIcnCode, request, response);

        // Then
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
    }

    @Test
    void testViewIcn_RangeRequest_BeyondFileSize() throws Exception {
        // Given - Range超出文件大小：bytes=0-999999
        request.addHeader("Range", "bytes=0-999999");
        when(ietmIcnManageService.getOne(any())).thenReturn(mockIcn);
        doNothing().when(ietmIcnManageService).loadAttachment(any());

        Path uploadDir = tempDir.resolve("upload").resolve("files");
        Files.createDirectories(uploadDir);
        Path videoFile = uploadDir.resolve("test-video.mp4");
        Files.write(videoFile, testVideoContent);
        System.setProperty("user.dir", tempDir.toString());

        // When
        controller.viewIcn(testIcnCode, request, response);

        // Then - 应该自动校正到文件末尾
        assertEquals(HttpServletResponse.SC_PARTIAL_CONTENT, response.getStatus());
        assertEquals("bytes 0-" + (testVideoContent.length - 1) + "/" + testVideoContent.length,
                response.getHeader("Content-Range"));
        assertEquals(String.valueOf(testVideoContent.length), response.getHeader("Content-Length"));
    }

    @Test
    void testViewIcn_VideoSeeking_SimulateUserDrag() throws Exception {
        // Given - 模拟用户拖动视频进度条到50%位置
        int halfPoint = testVideoContent.length / 2;
        request.addHeader("Range", "bytes=" + halfPoint + "-");
        when(ietmIcnManageService.getOne(any())).thenReturn(mockIcn);
        doNothing().when(ietmIcnManageService).loadAttachment(any());

        Path uploadDir = tempDir.resolve("upload").resolve("files");
        Files.createDirectories(uploadDir);
        Path videoFile = uploadDir.resolve("test-video.mp4");
        Files.write(videoFile, testVideoContent);
        System.setProperty("user.dir", tempDir.toString());

        // When
        controller.viewIcn(testIcnCode, request, response);

        // Then
        assertEquals(HttpServletResponse.SC_PARTIAL_CONTENT, response.getStatus());
        // 注意：测试文件是随机字节，MIME检测器会返回 application/octet-stream
        assertEquals("application/octet-stream", response.getContentType());

        // 验证仅返回后半部分
        byte[] responseContent = response.getContentAsByteArray();
        assertEquals(testVideoContent.length - halfPoint, responseContent.length);

        // 验证内容正确
        for (int i = 0; i < responseContent.length; i++) {
            assertEquals(testVideoContent[halfPoint + i], responseContent[i]);
        }
    }
}
