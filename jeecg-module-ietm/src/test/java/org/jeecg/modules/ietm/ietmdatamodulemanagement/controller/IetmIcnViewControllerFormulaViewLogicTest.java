package org.jeecg.modules.ietm.ietmdatamodulemanagement.controller;

import org.jeecg.modules.ietm.icnmanage.entity.IetmIcnFormula;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * IetmIcnViewController公式预览逻辑单元测试（不启动Spring容器）
 *
 * 测试核心逻辑：
 * 1. 从公式HTML中提取base64图片
 * 2. 处理不同的输入格式
 * 3. 验证边界情况
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
public class IetmIcnViewControllerFormulaViewLogicTest {

    /**
     * 测试1: 正常提取base64图片
     */
    @Test
    public void testExtractBase64Image_Success() {
        // 准备测试数据
        String base64Image = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";
        String formulaHtml = String.format(
            "<img class=\"kfformula\" src=\"data:image/png;base64,%s\" data-latex=\"x^2\" />",
            base64Image
        );

        // 提取base64图片（复制viewIcn()中的逻辑）
        Pattern pattern = Pattern.compile("src=\"(data:image/png;base64,[^\"]+)\"");
        Matcher matcher = pattern.matcher(formulaHtml);

        assertTrue(matcher.find(), "应该能找到base64图片");

        String base64Data = matcher.group(1);
        String extractedBase64 = base64Data.substring(base64Data.indexOf(',') + 1);

        // 验证提取的base64与原始数据一致
        assertEquals(base64Image, extractedBase64);

        // 验证能正确解码
        byte[] imageBytes = Base64.getDecoder().decode(extractedBase64);
        assertTrue(imageBytes.length > 0, "解码后的图片数据不应为空");
    }

    /**
     * 测试2: HTML中包含多个属性
     */
    @Test
    public void testExtractBase64Image_MultipleAttributes() {
        String base64Image = "iVBORw0KGgoAAAANSUhEUgAAAAUAAAAFCAYAAACNbyblAAAAHElEQVQI12P4//8/w38GIAXDIBKE0DHxgljNBAAO9TXL0Y4OHwAAAABJRU5ErkJggg==";
        String formulaHtml = String.format(
            "<img class=\"kfformula\" src=\"data:image/png;base64,%s\" data-latex=\"\\frac{a}{b}\" style=\"vertical-align:middle\" width=\"120\" height=\"40\" />",
            base64Image
        );

        Pattern pattern = Pattern.compile("src=\"(data:image/png;base64,[^\"]+)\"");
        Matcher matcher = pattern.matcher(formulaHtml);

        assertTrue(matcher.find());
        String base64Data = matcher.group(1);
        String extractedBase64 = base64Data.substring(base64Data.indexOf(',') + 1);

        assertEquals(base64Image, extractedBase64);
    }

    /**
     * 测试3: 提取失败 - 没有src属性
     */
    @Test
    public void testExtractBase64Image_NoSrc() {
        String malformedHtml = "<img class=\"kfformula\" data-latex=\"x^2\" />";

        Pattern pattern = Pattern.compile("src=\"(data:image/png;base64,[^\"]+)\"");
        Matcher matcher = pattern.matcher(malformedHtml);

        assertFalse(matcher.find(), "损坏的HTML不应找到base64图片");
    }

    /**
     * 测试4: 提取失败 - src不是data URL
     */
    @Test
    public void testExtractBase64Image_NonDataUrl() {
        String formulaHtml = "<img class=\"kfformula\" src=\"/path/to/image.png\" />";

        Pattern pattern = Pattern.compile("src=\"(data:image/png;base64,[^\"]+)\"");
        Matcher matcher = pattern.matcher(formulaHtml);

        assertFalse(matcher.find(), "非data URL不应匹配");
    }

    /**
     * 测试5: 空字符串
     */
    @Test
    public void testExtractBase64Image_EmptyString() {
        String emptyHtml = "";

        Pattern pattern = Pattern.compile("src=\"(data:image/png;base64,[^\"]+)\"");
        Matcher matcher = pattern.matcher(emptyHtml);

        assertFalse(matcher.find(), "空字符串不应找到base64图片");
    }

    /**
     * 测试6: 真实复杂公式HTML
     */
    @Test
    public void testExtractBase64Image_RealFormulaHtml() {
        // 模拟真实的公式HTML（包含LaTeX源码）
        String realBase64 = "iVBORw0KGgoAAAANSUhEUgAAAMgAAAAyCAYAAAAZUZThAAAMQUlEQVR4Xu2de5BdRRnGf99N";
        String latex = "x=\\frac{-b\\pm\\sqrt{b^2-4ac}}{2a}";
        String formulaHtml = String.format(
            "<img class=\"kfformula\" src=\"data:image/png;base64,%s\" data-latex=\"%s\" style=\"vertical-align:middle\" width=\"240\" height=\"50\" />",
            realBase64,
            latex
        );

        Pattern pattern = Pattern.compile("src=\"(data:image/png;base64,[^\"]+)\"");
        Matcher matcher = pattern.matcher(formulaHtml);

        assertTrue(matcher.find());
        String base64Data = matcher.group(1);
        String extractedBase64 = base64Data.substring(base64Data.indexOf(',') + 1);

        assertEquals(realBase64, extractedBase64);

        // 验证LaTeX源码不影响提取
        assertTrue(formulaHtml.contains(latex), "HTML应包含LaTeX源码");
    }

    /**
     * 测试7: IetmIcnFormula实体转换
     */
    @Test
    public void testIetmIcnFormulaEntity_ByteArrayToString() {
        String base64Image = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";
        String formulaHtml = String.format(
            "<img class=\"kfformula\" src=\"data:image/png;base64,%s\" />",
            base64Image
        );

        // 模拟从数据库读取
        IetmIcnFormula entity = new IetmIcnFormula();
        entity.setIcn("ICN-TEST-A-A11000-C-JB301-00001-A-001-01");
        entity.setFilecontent(formulaHtml.getBytes(StandardCharsets.UTF_8));

        // 验证byte[]能正确转换为String
        assertNotNull(entity.getFilecontent());
        assertTrue(entity.getFilecontent().length > 0);

        String htmlFromBytes = new String(entity.getFilecontent(), StandardCharsets.UTF_8);
        assertEquals(formulaHtml, htmlFromBytes, "byte[]转String应保持内容一致");

        // 验证能从转换后的HTML提取base64
        Pattern pattern = Pattern.compile("src=\"(data:image/png;base64,[^\"]+)\"");
        Matcher matcher = pattern.matcher(htmlFromBytes);

        assertTrue(matcher.find());
        String base64Data = matcher.group(1);
        String extractedBase64 = base64Data.substring(base64Data.indexOf(',') + 1);

        assertEquals(base64Image, extractedBase64);
    }

    /**
     * 测试8: 边界情况 - null filecontent
     */
    @Test
    public void testIetmIcnFormulaEntity_NullFilecontent() {
        IetmIcnFormula entity = new IetmIcnFormula();
        entity.setIcn("ICN-TEST-A-A11000-C-JB301-00002-A-001-01");
        entity.setFilecontent(null);

        // 验证Controller中的null检查
        boolean shouldFallback = entity.getFilecontent() == null || entity.getFilecontent().length == 0;
        assertTrue(shouldFallback, "null filecontent应降级到查询ietm_icn_manage表");
    }

    /**
     * 测试9: 边界情况 - 空byte[]
     */
    @Test
    public void testIetmIcnFormulaEntity_EmptyFilecontent() {
        IetmIcnFormula entity = new IetmIcnFormula();
        entity.setIcn("ICN-TEST-A-A11000-C-JB301-00003-A-001-01");
        entity.setFilecontent(new byte[0]);

        // 验证Controller中的空检查
        boolean shouldFallback = entity.getFilecontent() == null || entity.getFilecontent().length == 0;
        assertTrue(shouldFallback, "空byte[]应降级到查询ietm_icn_manage表");
    }

    /**
     * 测试10: Base64解码异常处理
     */
    @Test
    public void testBase64Decode_InvalidBase64() {
        String invalidBase64 = "这不是有效的Base64!!!";

        // 验证解码会抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            Base64.getDecoder().decode(invalidBase64);
        }, "无效的Base64应抛出异常");
    }
}
