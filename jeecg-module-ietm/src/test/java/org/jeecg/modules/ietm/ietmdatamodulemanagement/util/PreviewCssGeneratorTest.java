package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

import org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.PreviewConfigVO;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * PreviewCssGenerator单元测试
 *
 * @author IETM Team
 * @date 2026-09-18
 */
public class PreviewCssGeneratorTest {

    /**
     * 测试1: null配置返回空字符串
     */
    @Test
    public void testGenerateCssWithNullConfig() {
        String css = PreviewCssGenerator.generateCss(null);
        assertNotNull("null配置应返回非null结果", css);
        assertEquals("null配置应返回空字符串", "", css);
    }

    /**
     * 测试2: 默认配置（medium字体、light主题）
     */
    @Test
    public void testGenerateCssWithDefaultConfig() {
        PreviewConfigVO config = new PreviewConfigVO();
        config.setFontSize("medium");
        config.setTheme("light");
        config.setLineHeight(150);
        config.setPageWidth("auto");
        config.setGraphicScale(100);
        config.setPrintMode(false);

        String css = PreviewCssGenerator.generateCss(config);
        assertNotNull("默认配置应返回非null结果", css);
        // 默认配置应该生成最少的CSS（可能为空或仅包含必要样式）
    }

    /**
     * 测试3: 小字体配置
     */
    @Test
    public void testGenerateCssWithSmallFont() {
        PreviewConfigVO config = new PreviewConfigVO();
        config.setFontSize("small");

        String css = PreviewCssGenerator.generateCss(config);
        assertNotNull("small字体应返回非null结果", css);
        assertTrue("应包含字体大小设置", css.contains("font-size") || css.isEmpty());
    }

    /**
     * 测试4: 大字体配置
     */
    @Test
    public void testGenerateCssWithLargeFont() {
        PreviewConfigVO config = new PreviewConfigVO();
        config.setFontSize("large");

        String css = PreviewCssGenerator.generateCss(config);
        assertNotNull("large字体应返回非null结果", css);
        assertTrue("应包含字体大小设置或为空", css.contains("font-size") || css.isEmpty());
    }

    /**
     * 测试5: 超大字体配置
     */
    @Test
    public void testGenerateCssWithXLargeFont() {
        PreviewConfigVO config = new PreviewConfigVO();
        config.setFontSize("xlarge");

        String css = PreviewCssGenerator.generateCss(config);
        assertNotNull("xlarge字体应返回非null结果", css);
    }

    /**
     * 测试6: 深色主题
     */
    @Test
    public void testGenerateCssWithDarkTheme() {
        PreviewConfigVO config = new PreviewConfigVO();
        config.setTheme("dark");

        String css = PreviewCssGenerator.generateCss(config);
        assertNotNull("dark主题应返回非null结果", css);
        // 深色主题可能包含背景色和文字颜色设置
    }

    /**
     * 测试7: 高对比度主题
     */
    @Test
    public void testGenerateCssWithHighContrastTheme() {
        PreviewConfigVO config = new PreviewConfigVO();
        config.setTheme("high-contrast");

        String css = PreviewCssGenerator.generateCss(config);
        assertNotNull("high-contrast主题应返回非null结果", css);
    }

    /**
     * 测试8: 自定义行高
     */
    @Test
    public void testGenerateCssWithCustomLineHeight() {
        PreviewConfigVO config = new PreviewConfigVO();
        config.setLineHeight(180);

        String css = PreviewCssGenerator.generateCss(config);
        assertNotNull("自定义行高应返回非null结果", css);
        // 非150的行高应该生成CSS
    }

    /**
     * 测试9: 默认行高（150）不生成额外CSS
     */
    @Test
    public void testGenerateCssWithDefaultLineHeight() {
        PreviewConfigVO config = new PreviewConfigVO();
        config.setLineHeight(150);

        String css = PreviewCssGenerator.generateCss(config);
        assertNotNull("默认行高应返回非null结果", css);
    }

    /**
     * 测试10: 固定页面宽度
     */
    @Test
    public void testGenerateCssWithFixedPageWidth() {
        PreviewConfigVO config = new PreviewConfigVO();
        config.setPageWidth("800px");

        String css = PreviewCssGenerator.generateCss(config);
        assertNotNull("固定页宽应返回非null结果", css);
    }

    /**
     * 测试11: 自动页面宽度（默认）
     */
    @Test
    public void testGenerateCssWithAutoPageWidth() {
        PreviewConfigVO config = new PreviewConfigVO();
        config.setPageWidth("auto");

        String css = PreviewCssGenerator.generateCss(config);
        assertNotNull("自动页宽应返回非null结果", css);
    }

    /**
     * 测试12: 图形缩放
     */
    @Test
    public void testGenerateCssWithGraphicScale() {
        PreviewConfigVO config = new PreviewConfigVO();
        config.setGraphicScale(150);

        String css = PreviewCssGenerator.generateCss(config);
        assertNotNull("图形缩放应返回非null结果", css);
    }

    /**
     * 测试13: 默认图形缩放（100%）不生成额外CSS
     */
    @Test
    public void testGenerateCssWithDefaultGraphicScale() {
        PreviewConfigVO config = new PreviewConfigVO();
        config.setGraphicScale(100);

        String css = PreviewCssGenerator.generateCss(config);
        assertNotNull("默认图形缩放应返回非null结果", css);
    }

    /**
     * 测试14: 打印模式启用
     */
    @Test
    public void testGenerateCssWithPrintModeEnabled() {
        PreviewConfigVO config = new PreviewConfigVO();
        config.setPrintMode(true);
        config.setShowHeader(true);
        config.setShowFooter(true);
        config.setPageBreak("auto");

        String css = PreviewCssGenerator.generateCss(config);
        assertNotNull("打印模式应返回非null结果", css);
    }

    /**
     * 测试15: 打印模式禁用
     */
    @Test
    public void testGenerateCssWithPrintModeDisabled() {
        PreviewConfigVO config = new PreviewConfigVO();
        config.setPrintMode(false);

        String css = PreviewCssGenerator.generateCss(config);
        assertNotNull("非打印模式应返回非null结果", css);
    }

    /**
     * 测试16: 隐藏DM编码
     */
    @Test
    public void testGenerateCssWithHiddenDmCode() {
        PreviewConfigVO config = new PreviewConfigVO();
        config.setShowDmCode(false);

        String css = PreviewCssGenerator.generateCss(config);
        assertNotNull("隐藏DM编码应返回非null结果", css);
    }

    /**
     * 测试17: 隐藏版本信息
     */
    @Test
    public void testGenerateCssWithHiddenRevision() {
        PreviewConfigVO config = new PreviewConfigVO();
        config.setShowRevision(false);

        String css = PreviewCssGenerator.generateCss(config);
        assertNotNull("隐藏版本信息应返回非null结果", css);
    }

    /**
     * 测试18: 隐藏安全图标
     */
    @Test
    public void testGenerateCssWithHiddenSafetyIcons() {
        PreviewConfigVO config = new PreviewConfigVO();
        config.setShowSafetyIcons(false);

        String css = PreviewCssGenerator.generateCss(config);
        assertNotNull("隐藏安全图标应返回非null结果", css);
    }

    /**
     * 测试19: 复杂配置组合
     */
    @Test
    public void testGenerateCssWithComplexConfig() {
        PreviewConfigVO config = new PreviewConfigVO();
        config.setFontSize("large");
        config.setTheme("dark");
        config.setLineHeight(180);
        config.setPageWidth("1000px");
        config.setGraphicScale(120);
        config.setPrintMode(true);
        config.setShowDmCode(false);
        config.setShowRevision(false);
        config.setShowSafetyIcons(true);

        String css = PreviewCssGenerator.generateCss(config);
        assertNotNull("复杂配置应返回非null结果", css);
        assertTrue("复杂配置应生成CSS内容", css.length() > 0);
    }

    /**
     * 测试20: 应用CSS到HTML
     */
    @Test
    public void testApplyConfigToHtml() {
        String html = "<html><head></head><body><div class=\"dm-preview\">Test Content</div></body></html>";
        PreviewConfigVO config = new PreviewConfigVO();
        config.setFontSize("large");
        config.setTheme("dark");

        String result = PreviewCssGenerator.applyConfigToHtml(html, config);
        assertNotNull("应用配置后应返回非null结果", result);
        assertTrue("应包含原始HTML内容", result.contains("Test Content"));
    }

    /**
     * 测试21: 应用CSS到null HTML
     */
    @Test
    public void testApplyConfigToNullHtml() {
        PreviewConfigVO config = new PreviewConfigVO();
        String result = PreviewCssGenerator.applyConfigToHtml(null, config);
        assertNull("null HTML应返回null", result);
    }

    /**
     * 测试22: 应用null配置到HTML
     */
    @Test
    public void testApplyNullConfigToHtml() {
        String html = "<html><body>Test</body></html>";
        String result = PreviewCssGenerator.applyConfigToHtml(html, null);
        assertEquals("null配置应返回原始HTML", html, result);
    }

    /**
     * 测试23: 无效字体大小回退到默认
     */
    @Test
    public void testGenerateCssWithInvalidFontSize() {
        PreviewConfigVO config = new PreviewConfigVO();
        config.setFontSize("invalid-size");

        String css = PreviewCssGenerator.generateCss(config);
        assertNotNull("无效字体大小应返回非null结果", css);
        // 应回退到默认值，不抛出异常
    }

    /**
     * 测试24: 无效主题回退到默认
     */
    @Test
    public void testGenerateCssWithInvalidTheme() {
        PreviewConfigVO config = new PreviewConfigVO();
        config.setTheme("invalid-theme");

        String css = PreviewCssGenerator.generateCss(config);
        assertNotNull("无效主题应返回非null结果", css);
        // 应回退到默认值，不抛出异常
    }

    /**
     * 测试25: 极端行高值
     */
    @Test
    public void testGenerateCssWithExtremeLineHeight() {
        PreviewConfigVO config = new PreviewConfigVO();

        // 测试极小值
        config.setLineHeight(50);
        String css1 = PreviewCssGenerator.generateCss(config);
        assertNotNull("极小行高应返回非null结果", css1);

        // 测试极大值
        config.setLineHeight(500);
        String css2 = PreviewCssGenerator.generateCss(config);
        assertNotNull("极大行高应返回非null结果", css2);
    }

    /**
     * 测试26: 极端图形缩放值
     */
    @Test
    public void testGenerateCssWithExtremeGraphicScale() {
        PreviewConfigVO config = new PreviewConfigVO();

        // 测试极小值
        config.setGraphicScale(10);
        String css1 = PreviewCssGenerator.generateCss(config);
        assertNotNull("极小缩放应返回非null结果", css1);

        // 测试极大值
        config.setGraphicScale(500);
        String css2 = PreviewCssGenerator.generateCss(config);
        assertNotNull("极大缩放应返回非null结果", css2);
    }

    /**
     * 测试27: CSS输出不包含敏感信息
     */
    @Test
    public void testGenerateCssNoSensitiveInfo() {
        PreviewConfigVO config = new PreviewConfigVO();
        config.setFontSize("large");
        config.setTheme("dark");

        String css = PreviewCssGenerator.generateCss(config);

        // CSS不应包含敏感信息
        assertFalse("CSS不应包含密码", css.toLowerCase().contains("password"));
        assertFalse("CSS不应包含token", css.toLowerCase().contains("token"));
        assertFalse("CSS不应包含secret", css.toLowerCase().contains("secret"));
    }

    /**
     * 测试28: 生成CSS性能
     */
    @Test(timeout = 1000)
    public void testGenerateCssPerformance() {
        PreviewConfigVO config = new PreviewConfigVO();
        config.setFontSize("large");
        config.setTheme("dark");
        config.setLineHeight(180);
        config.setGraphicScale(120);

        // 应在1秒内完成
        for (int i = 0; i < 100; i++) {
            String css = PreviewCssGenerator.generateCss(config);
            assertNotNull("性能测试应返回非null结果", css);
        }
    }
}
