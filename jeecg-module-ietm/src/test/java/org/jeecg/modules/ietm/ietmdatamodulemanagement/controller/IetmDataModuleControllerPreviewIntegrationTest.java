package org.jeecg.modules.ietm.ietmdatamodulemanagement.controller;

import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDataModule;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.service.IIetmDataModuleService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

/**
 * 预览接口集成测试：验证完整的Controller -> Service -> Helper链路
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class IetmDataModuleControllerPreviewIntegrationTest {

    @Autowired
    private IetmDataModuleController controller;

    @Autowired
    private IIetmDataModuleService service;

    @Test
    public void testPreviewEndpoint_WithRealData() {
        // 从数据库查找一个有内容的DM
        IetmDataModule dm = service.list().stream()
            .filter(d -> d.getDmContent() != null && !d.getDmContent().isEmpty())
            .findFirst()
            .orElse(null);

        if (dm == null) {
            System.out.println("⚠️ 数据库中没有有内容的DM，跳过测试");
            return;
        }

        System.out.println("=== 使用真实DM测试预览接口 ===");
        System.out.println("DM ID: " + dm.getId());
        System.out.println("DMC Code: " + dm.getDmcCode());
        System.out.println("DM Content长度: " + dm.getDmContent().length());

        // 调用Controller接口
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/jeecg-boot");

        org.jeecg.common.api.vo.Result<java.util.Map<String, Object>> result =
            controller.preview(dm.getId(), request);

        System.out.println("\n=== 接口响应 ===");
        System.out.println("Success: " + result.isSuccess());
        System.out.println("Code: " + result.getCode());

        assertTrue("接口应该返回成功", result.isSuccess());

        java.util.Map<String, Object> data = result.getResult();
        assertNotNull("Result不应为null", data);

        String flag = (String) data.get("flag");
        System.out.println("Flag: " + flag);

        if ("success".equals(flag)) {
            String html = (String) data.get("html");
            assertNotNull("HTML不应为null", html);

            System.out.println("\n=== HTML内容分析 ===");
            System.out.println("HTML总长度: " + html.length());
            System.out.println("包含dm-preview: " + html.contains("dm-preview"));
            System.out.println("包含<style>: " + html.contains("<style>"));
            System.out.println("包含lessonPath: " + html.contains("lessonPath"));
            System.out.println("包含lessonCode: " + html.contains("lessonCode"));

            // 打印HTML片段
            System.out.println("\n=== HTML开头(前500字符) ===");
            System.out.println(html.substring(0, Math.min(500, html.length())));

            // 验证关键修复
            assertFalse("不应包含lessonPath", html.contains("lessonPath"));
            assertFalse("不应包含lessonCode", html.contains("lessonCode"));
            assertTrue("应该包含dm-preview容器", html.contains("dm-preview"));

        } else {
            System.out.println("Flag不是success: " + flag);
            System.out.println("Message: " + data.get("message"));
        }
    }
}
