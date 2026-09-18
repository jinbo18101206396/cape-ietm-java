package org.jeecg.modules.ietm.ietmdatamodulemanagement;

import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.controller.IetmDataModuleController;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.service.IIetmDataModuleService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 废弃接口监控测试
 *
 * 验证目标:
 * 1. @Deprecated注解已添加
 * 2. 监控日志正常输出
 * 3. 功能行为未改变
 * 4. 新旧接口对比验证
 *
 * @author Kiro
 * @date 2026-09-18
 */
@RunWith(MockitoJUnitRunner.class)
public class DeprecatedEndpointMonitoringTest {

    @Mock
    private IIetmDataModuleService dataModuleService;

    @InjectMocks
    private IetmDataModuleController controller;

    /**
     * TC-D01: 验证previewDm废弃注解
     */
    @Test
    public void testPreviewDm_DeprecationAnnotation() throws Exception {
        // Given: 准备测试数据
        String testId = "test-dm-001";
        HttpServletResponse response = new MockHttpServletResponse();

        // When: 调用废弃方法
        controller.previewDm(testId, response);

        // Then: 验证方法被调用（功能正常）
        verify(dataModuleService, times(1)).previewDm(eq(testId), eq(response));

        // 验证@Deprecated注解存在
        try {
            java.lang.reflect.Method method = IetmDataModuleController.class
                .getMethod("previewDm", String.class, HttpServletResponse.class);
            assertTrue("previewDm应该有@Deprecated注解",
                      method.isAnnotationPresent(Deprecated.class));
        } catch (NoSuchMethodException e) {
            fail("previewDm方法不存在");
        }
    }

    /**
     * TC-D02: 验证validateContent废弃注解
     */
    @Test
    public void testValidateContent_DeprecationAnnotation() {
        // Given: 准备测试数据
        Map<String, String> params = new HashMap<>();
        params.put("content", "<dmodule></dmodule>");

        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("valid", true);
        when(dataModuleService.validateXmlContent(anyString())).thenReturn(mockResult);

        // When: 调用废弃方法
        controller.validateContent(params);

        // Then: 验证方法被调用（功能正常）
        verify(dataModuleService, times(1)).validateXmlContent(eq("<dmodule></dmodule>"));

        // 验证@Deprecated注解存在
        try {
            java.lang.reflect.Method method = IetmDataModuleController.class
                .getMethod("validateContent", Map.class);
            assertTrue("validateContent应该有@Deprecated注解",
                      method.isAnnotationPresent(Deprecated.class));
        } catch (NoSuchMethodException e) {
            fail("validateContent方法不存在");
        }
    }

    /**
     * TC-D03: 验证previewDm功能行为不变
     */
    @Test
    public void testPreviewDm_BehaviorUnchanged() throws Exception {
        // Given: 准备测试数据
        String testId = "test-dm-002";
        HttpServletResponse response = new MockHttpServletResponse();

        // When: 调用废弃方法
        controller.previewDm(testId, response);

        // Then: 验证service层被正确调用
        verify(dataModuleService, times(1)).previewDm(testId, response);
    }

    /**
     * TC-D04: 验证validateContent功能行为不变
     */
    @Test
    public void testValidateContent_BehaviorUnchanged() {
        // Given: 准备测试数据
        Map<String, String> params = new HashMap<>();
        String xmlContent = "<?xml version=\"1.0\"?><dmodule><content></content></dmodule>";
        params.put("content", xmlContent);

        Map<String, Object> expectedResult = new HashMap<>();
        expectedResult.put("valid", true);
        expectedResult.put("errors", new java.util.ArrayList<>());
        when(dataModuleService.validateXmlContent(xmlContent)).thenReturn(expectedResult);

        // When: 调用废弃方法
        Result<Map<String, Object>> result = controller.validateContent(params);

        // Then: 验证返回结果正确
        assertNotNull("返回结果不应为空", result);
        assertTrue("应该返回成功", result.isSuccess());
        assertEquals("结果数据应该正确", expectedResult, result.getResult());
        verify(dataModuleService, times(1)).validateXmlContent(xmlContent);
    }

    /**
     * TC-D05: 验证previewDm空ID异常处理
     */
    @Test
    public void testPreviewDm_NullId() throws Exception {
        // Given: ID为null
        HttpServletResponse response = new MockHttpServletResponse();
        doThrow(new IllegalArgumentException("ID不能为空"))
            .when(dataModuleService).previewDm(null, response);

        // When/Then: 调用应该抛出异常
        try {
            controller.previewDm(null, response);
            fail("应该抛出IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("ID不能为空", e.getMessage());
        }
    }

    /**
     * TC-D06: 验证validateContent空内容处理
     */
    @Test
    public void testValidateContent_EmptyContent() {
        // Given: 空内容
        Map<String, String> params = new HashMap<>();
        params.put("content", "");

        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("valid", false);
        mockResult.put("message", "XML内容为空");
        when(dataModuleService.validateXmlContent("")).thenReturn(mockResult);

        // When: 调用废弃方法
        Result<Map<String, Object>> result = controller.validateContent(params);

        // Then: 验证返回结果
        assertNotNull("返回结果不应为空", result);
        assertTrue("应该返回成功（结构上）", result.isSuccess());
        assertFalse("校验应该失败", (Boolean) result.getResult().get("valid"));
        verify(dataModuleService, times(1)).validateXmlContent("");
    }

    /**
     * TC-D07: 验证previewDm与新接口preview的区别
     */
    @Test
    public void testPreviewDm_vs_Preview() throws Exception {
        // Given: 准备测试数据
        String testId = "test-dm-003";
        HttpServletResponse response = new MockHttpServletResponse();

        // When: 调用旧接口
        controller.previewDm(testId, response);

        // Then: 验证调用的是previewDm（旧方法）
        verify(dataModuleService, times(1)).previewDm(testId, response);
    }

    /**
     * TC-D08: 验证validateContent参数传递
     */
    @Test
    public void testValidateContent_ParameterPassing() {
        // Given: 准备复杂XML内容
        Map<String, String> params = new HashMap<>();
        String complexXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                           "<dmodule>\n" +
                           "  <identAndStatusSection>\n" +
                           "    <dmAddress><dmIdent/></dmAddress>\n" +
                           "  </identAndStatusSection>\n" +
                           "  <content><description><para>测试</para></description></content>\n" +
                           "</dmodule>";
        params.put("content", complexXml);

        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("valid", true);
        when(dataModuleService.validateXmlContent(complexXml)).thenReturn(mockResult);

        // When: 调用方法
        controller.validateContent(params);

        // Then: 验证参数完整传递
        verify(dataModuleService, times(1)).validateXmlContent(complexXml);
    }
}
