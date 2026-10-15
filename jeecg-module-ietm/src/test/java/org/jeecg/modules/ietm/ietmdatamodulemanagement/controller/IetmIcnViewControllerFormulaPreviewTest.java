package org.jeecg.modules.ietm.ietmdatamodulemanagement.controller;

import org.jeecg.modules.ietm.icnmanage.entity.IetmIcnFormula;
import org.jeecg.modules.ietm.icnmanage.service.IIetmIcnFormulaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * IetmIcnViewController公式预览测试
 *
 * 测试场景：在DM预览弹窗中点击公式图片时，能否正确加载公式ICN
 *
 * 根因分析：
 * - 公式ICN存储在独立的ietm_icn_formula表（绕过DM8聚集索引限制）
 * - 旧代码的/ietm/icn/view/{icnCode}端点只查询ietm_icn_manage表
 * - 导致公式ICN返回"ICN不存在"的占位图
 *
 * 修复方案：
 * - viewIcn()方法优先从ietm_icn_formula表查询
 * - 如果是公式ICN，从filecontent字段提取base64图片返回
 * - 否则走原有的附件流程（图符/视频/音频）
 *
 * @author Claude
 * @date 2026-10-15
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureMockMvc
public class IetmIcnViewControllerFormulaPreviewTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IIetmIcnFormulaService icnFormulaService;

    /**
     * 测试1: 公式ICN预览 - 正常场景
     *
     * 验证：
     * 1. 能从ietm_icn_formula表查询到公式ICN
     * 2. 能正确提取base64图片数据
     * 3. 返回Content-Type为image/png
     * 4. 返回状态码为200
     */
    @Test
    public void testFormulaIcnPreview_Success() throws Exception {
        // 准备测试数据：模拟一个公式ICN
        String icnCode = "ICN-ZBBM02-A-A11000-C-JB301-00004-A-001-01";

        // 模拟公式HTML（包含LaTeX和base64图片）
        String base64Image = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";
        String formulaHtml = String.format(
            "<img class=\"kfformula\" src=\"data:image/png;base64,%s\" data-latex=\"x=\\frac{-b\\pm\\sqrt{b^2-4ac}}{2a}\" style=\"vertical-align:middle\" />",
            base64Image
        );

        IetmIcnFormula mockEntity = new IetmIcnFormula();
        mockEntity.setIcn(icnCode);
        mockEntity.setFilecontent(formulaHtml.getBytes(StandardCharsets.UTF_8));

        when(icnFormulaService.getByIcnWithContent(icnCode)).thenReturn(mockEntity);

        // 执行请求
        MvcResult result = mockMvc.perform(get("/ietm/icn/view/" + icnCode))
            .andExpect(status().isOk())
            .andReturn();

        // 验证响应
        MockHttpServletResponse response = result.getResponse();
        assertEquals("image/png", response.getContentType());

        byte[] content = response.getContentAsByteArray();
        assertTrue(content.length > 0, "图片内容不应为空");

        // 验证返回的是真实PNG图片（解码base64后应与原始数据一致）
        byte[] expectedBytes = Base64.getDecoder().decode(base64Image);
        assertArrayEquals(expectedBytes, content, "返回的图片数据应与base64解码后的数据一致");
    }

    /**
     * 测试2: 公式ICN预览 - base64提取失败
     *
     * 验证：
     * 1. filecontent不包含base64图片数据
     * 2. 返回占位图（SVG格式）
     * 3. 返回Content-Type为image/svg+xml
     */
    @Test
    public void testFormulaIcnPreview_Base64ExtractionFailed() throws Exception {
        String icnCode = "ICN-ZBBM02-A-A11000-C-JB301-00005-A-001-01";

        // 模拟损坏的公式HTML（没有src属性）
        String malformedHtml = "<img class=\"kfformula\" data-latex=\"x^2\" />";

        IetmIcnFormula mockEntity = new IetmIcnFormula();
        mockEntity.setIcn(icnCode);
        mockEntity.setFilecontent(malformedHtml.getBytes(StandardCharsets.UTF_8));

        when(icnFormulaService.getByIcnWithContent(icnCode)).thenReturn(mockEntity);

        // 执行请求
        MvcResult result = mockMvc.perform(get("/ietm/icn/view/" + icnCode))
            .andExpect(status().isOk())
            .andReturn();

        // 验证返回占位图
        MockHttpServletResponse response = result.getResponse();
        assertEquals("image/svg+xml", response.getContentType());

        String svgContent = response.getContentAsString(StandardCharsets.UTF_8);
        assertTrue(svgContent.contains("<svg"), "应返回SVG占位图");
        assertTrue(svgContent.contains("公式图片格式错误"), "占位图应包含错误提示");
    }

    /**
     * 测试3: 非公式ICN预览 - 降级到原有流程
     *
     * 验证：
     * 1. ietm_icn_formula表查询不到数据
     * 2. 降级到查询ietm_icn_manage表（原有流程）
     * 3. 返回"ICN不存在"占位图（因为MockBean没有配置ietm_icn_manage的mock数据）
     */
    @Test
    public void testNonFormulaIcnPreview_FallbackToOriginalFlow() throws Exception {
        String icnCode = "ICN-ZBBM02-A-A11000-C-JB301-99999-A-001-01";

        // ietm_icn_formula表查询不到数据
        when(icnFormulaService.getByIcnWithContent(icnCode)).thenReturn(null);

        // 执行请求（会降级到查询ietm_icn_manage表）
        MvcResult result = mockMvc.perform(get("/ietm/icn/view/" + icnCode))
            .andExpect(status().isOk())
            .andReturn();

        // 验证返回占位图（因为ietm_icn_manage也查不到）
        MockHttpServletResponse response = result.getResponse();
        assertEquals("image/svg+xml", response.getContentType());

        String svgContent = response.getContentAsString(StandardCharsets.UTF_8);
        assertTrue(svgContent.contains("<svg"), "应返回SVG占位图");
        assertTrue(svgContent.contains("ICN不存在"), "占位图应包含'ICN不存在'提示");
    }

    /**
     * 测试4: 空filecontent场景
     *
     * 验证：
     * 1. ietm_icn_formula表查询到记录，但filecontent为空
     * 2. 降级到查询ietm_icn_manage表
     */
    @Test
    public void testFormulaIcnPreview_EmptyFilecontent() throws Exception {
        String icnCode = "ICN-ZBBM02-A-A11000-C-JB301-00006-A-001-01";

        IetmIcnFormula mockEntity = new IetmIcnFormula();
        mockEntity.setIcn(icnCode);
        mockEntity.setFilecontent(new byte[0]);  // 空内容

        when(icnFormulaService.getByIcnWithContent(icnCode)).thenReturn(mockEntity);

        // 执行请求
        MvcResult result = mockMvc.perform(get("/ietm/icn/view/" + icnCode))
            .andExpect(status().isOk())
            .andReturn();

        // 验证返回占位图
        MockHttpServletResponse response = result.getResponse();
        assertEquals("image/svg+xml", response.getContentType());
    }

    /**
     * 测试5: 特殊字符ICN编码
     *
     * 验证URL路径参数编码正确性
     */
    @Test
    public void testFormulaIcnPreview_SpecialCharactersInIcn() throws Exception {
        // 注意：ICN编码中包含连字符，Spring MVC会正确处理
        String icnCode = "ICN-ZBBM02-A-A11000-C-JB301-00007-A-001-01";

        when(icnFormulaService.getByIcnWithContent(anyString())).thenReturn(null);

        MvcResult result = mockMvc.perform(get("/ietm/icn/view/" + icnCode))
            .andExpect(status().isOk())
            .andReturn();

        assertNotNull(result.getResponse());
    }
}
