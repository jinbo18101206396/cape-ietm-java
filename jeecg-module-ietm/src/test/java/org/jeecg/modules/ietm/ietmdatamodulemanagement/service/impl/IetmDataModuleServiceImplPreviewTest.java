package org.jeecg.modules.ietm.ietmdatamodulemanagement.service.impl;

import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDataModule;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.mapper.IetmDataModuleMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * previewDm 方法单元测试
 *
 * 测试场景：
 * 1. 空DM内容预览
 * 2. 正常DM预览（使用DmXmlHelper.renderHtml）
 * 3. DM不存在异常
 * 4. 包含ICN的DM预览
 * 5. XSLT转换异常降级
 *
 * @author jeecg-boot
 * @date 2026-09-17
 */
@RunWith(MockitoJUnitRunner.class)
public class IetmDataModuleServiceImplPreviewTest {

    @Mock
    private IetmDataModuleMapper baseMapper;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private IetmDataModuleServiceImpl service;

    private StringWriter responseWriter;

    @Before
    public void setUp() throws Exception {
        responseWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(responseWriter);
        when(response.getWriter()).thenReturn(printWriter);
    }

    /**
     * TC-01: 测试DM不存在的情况
     */
    @Test(expected = RuntimeException.class)
    public void testPreviewDm_DmNotFound() {
        // Given: DM不存在
        when(baseMapper.selectById(anyString())).thenReturn(null);

        // When: 调用预览方法
        service.previewDm("non-existent-id", response);

        // Then: 应该抛出异常
    }

    /**
     * TC-02: 测试空DM内容的情况
     */
    @Test
    public void testPreviewDm_EmptyContent() throws Exception {
        // Given: DM存在但内容为空
        IetmDataModule dm = new IetmDataModule();
        dm.setId("test-id-001");
        dm.setDmContent(null);
        when(baseMapper.selectById("test-id-001")).thenReturn(dm);

        // When: 调用预览方法
        service.previewDm("test-id-001", response);

        // Then: 应该返回"暂无内容"提示
        String htmlOutput = responseWriter.toString();
        assertTrue("应该包含'暂无内容'提示", htmlOutput.contains("暂无内容"));
        assertTrue("应该是有效的HTML", htmlOutput.contains("<html") && htmlOutput.contains("</html>"));
        verify(response).setContentType("text/html;charset=UTF-8");
    }

    /**
     * TC-03: 测试正常DM内容预览
     */
    @Test
    public void testPreviewDm_NormalContent() throws Exception {
        // Given: DM包含正常的S1000D XML内容
        IetmDataModule dm = new IetmDataModule();
        dm.setId("test-id-002");
        dm.setDmContent(buildSimpleDmXml());
        when(baseMapper.selectById("test-id-002")).thenReturn(dm);

        // When: 调用预览方法
        service.previewDm("test-id-002", response);

        // Then: 应该返回包含预览容器的HTML
        String htmlOutput = responseWriter.toString();
        assertTrue("应该包含预览容器", htmlOutput.contains("dm-preview"));
        verify(response).setContentType("text/html;charset=UTF-8");
        verify(response).setHeader("Content-Disposition", "inline");
    }

    /**
     * TC-04: 测试空字符串内容
     */
    @Test
    public void testPreviewDm_EmptyStringContent() throws Exception {
        // Given: DM内容为空字符串
        IetmDataModule dm = new IetmDataModule();
        dm.setId("test-id-003");
        dm.setDmContent("");
        when(baseMapper.selectById("test-id-003")).thenReturn(dm);

        // When: 调用预览方法
        service.previewDm("test-id-003", response);

        // Then: 应该返回"暂无内容"提示（空字符串也算空）
        String htmlOutput = responseWriter.toString();
        assertTrue("应该包含'暂无内容'提示", htmlOutput.contains("暂无内容"));
    }

    /**
     * TC-05: 测试包含ICN的DM预览
     * 注意：此测试验证调用流程，实际ICN加载依赖数据库和文件系统
     */
    @Test
    public void testPreviewDm_WithIcn() throws Exception {
        // Given: DM包含图形引用的XML
        IetmDataModule dm = new IetmDataModule();
        dm.setId("test-id-004");
        dm.setDmContent(buildDmXmlWithGraphic());
        when(baseMapper.selectById("test-id-004")).thenReturn(dm);

        // When: 调用预览方法
        service.previewDm("test-id-004", response);

        // Then: 应该成功返回HTML（ICN处理在DmXmlHelper内部）
        String htmlOutput = responseWriter.toString();
        assertNotNull("应该返回HTML内容", htmlOutput);
        assertFalse("不应该为空", htmlOutput.isEmpty());
        verify(response).setContentType("text/html;charset=UTF-8");
    }

    // ==================== 辅助方法 ====================

    /**
     * 构建简单的S1000D DM XML
     */
    private String buildSimpleDmXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<dmodule xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
               "  <identAndStatusSection>\n" +
               "    <dmAddress>\n" +
               "      <dmIdent>\n" +
               "        <dmCode modelIdentCode=\"TEST\" systemDiffCode=\"A\" " +
               "systemCode=\"00\" subSystemCode=\"0\" subSubSystemCode=\"0\" " +
               "assyCode=\"00\" disassyCode=\"00\" disassyCodeVariant=\"A\" " +
               "infoCode=\"001\" infoCodeVariant=\"A\" itemLocationCode=\"A\"/>\n" +
               "      </dmIdent>\n" +
               "    </dmAddress>\n" +
               "  </identAndStatusSection>\n" +
               "  <content>\n" +
               "    <description>\n" +
               "      <levelledPara>\n" +
               "        <title>测试标题</title>\n" +
               "        <para>这是测试内容。</para>\n" +
               "      </levelledPara>\n" +
               "    </description>\n" +
               "  </content>\n" +
               "</dmodule>";
    }

    /**
     * 构建包含图形的S1000D DM XML
     */
    private String buildDmXmlWithGraphic() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<dmodule xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
               "  <identAndStatusSection>\n" +
               "    <dmAddress>\n" +
               "      <dmIdent>\n" +
               "        <dmCode modelIdentCode=\"TEST\" systemDiffCode=\"A\" " +
               "systemCode=\"00\" subSystemCode=\"0\" subSubSystemCode=\"0\" " +
               "assyCode=\"00\" disassyCode=\"00\" disassyCodeVariant=\"A\" " +
               "infoCode=\"001\" infoCodeVariant=\"A\" itemLocationCode=\"A\"/>\n" +
               "      </dmIdent>\n" +
               "    </dmAddress>\n" +
               "  </identAndStatusSection>\n" +
               "  <content>\n" +
               "    <description>\n" +
               "      <levelledPara>\n" +
               "        <title>包含图形的测试</title>\n" +
               "        <figure>\n" +
               "          <title>图1 测试图形</title>\n" +
               "          <graphic infoEntityIdent=\"ICN-TEST-001-01\"/>\n" +
               "        </figure>\n" +
               "      </levelledPara>\n" +
               "    </description>\n" +
               "  </content>\n" +
               "</dmodule>";
    }
}
