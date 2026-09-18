package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.PreviewConfigVO;

/**
 * DM预览CSS生成器
 *
 * 根据用户配置动态生成CSS样式
 *
 * @author IETM Team
 * @date 2026-09-18
 */
@Slf4j
public class PreviewCssGenerator {

    /**
     * 生成自定义CSS
     */
    public static String generateCss(PreviewConfigVO config) {
        if (config == null) {
            return "";
        }

        StringBuilder css = new StringBuilder();

        // 1. 字体大小
        css.append(generateFontSizeCss(config.getFontSize()));

        // 2. 颜色主题
        css.append(generateThemeCss(config.getTheme()));

        // 3. 行间距
        if (config.getLineHeight() != null && config.getLineHeight() != 150) {
            css.append(generateLineHeightCss(config.getLineHeight()));
        }

        // 4. 页面宽度
        if (!"auto".equals(config.getPageWidth())) {
            css.append(generatePageWidthCss(config.getPageWidth()));
        }

        // 5. 图形缩放
        if (config.getGraphicScale() != null && config.getGraphicScale() != 100) {
            css.append(generateGraphicScaleCss(config.getGraphicScale()));
        }

        // 6. 打印模式
        if (Boolean.TRUE.equals(config.getPrintMode())) {
            css.append(generatePrintModeCss(config));
        }

        // 7. 隐藏元素
        css.append(generateHiddenElementsCss(config));

        log.debug("生成自定义CSS: 长度={}字符", css.length());
        return css.toString();
    }

    /**
     * 字体大小CSS
     */
    private static String generateFontSizeCss(String fontSize) {
        if (fontSize == null || "medium".equals(fontSize)) {
            return "";
        }

        String size;
        switch (fontSize) {
            case "small":
                size = "12px";
                break;
            case "large":
                size = "16px";
                break;
            case "xlarge":
                size = "18px";
                break;
            default:
                size = "14px"; // medium
        }

        return String.format(
            ".dm-preview { font-size: %s; }\n" +
            ".dm-preview h1 { font-size: %.1fem; }\n" +
            ".dm-preview h2 { font-size: %.1fem; }\n" +
            ".dm-preview h3 { font-size: %.1fem; }\n",
            size,
            "small".equals(fontSize) ? 1.8 : ("large".equals(fontSize) ? 2.2 : ("xlarge".equals(fontSize) ? 2.4 : 2.0)),
            "small".equals(fontSize) ? 1.5 : ("large".equals(fontSize) ? 1.8 : ("xlarge".equals(fontSize) ? 2.0 : 1.6)),
            "small".equals(fontSize) ? 1.2 : ("large".equals(fontSize) ? 1.4 : ("xlarge".equals(fontSize) ? 1.5 : 1.3))
        );
    }

    /**
     * 颜色主题CSS
     */
    private static String generateThemeCss(String theme) {
        if (theme == null || "light".equals(theme)) {
            return "";
        }

        switch (theme) {
            case "dark":
                return
                    ".dm-preview {\n" +
                    "  background-color: #1e1e1e !important;\n" +
                    "  color: #d4d4d4 !important;\n" +
                    "}\n" +
                    ".dm-preview a { color: #4fc3f7 !important; }\n" +
                    ".dm-preview table {\n" +
                    "  border-color: #555 !important;\n" +
                    "  background-color: #2d2d2d !important;\n" +
                    "}\n" +
                    ".dm-preview th {\n" +
                    "  background-color: #383838 !important;\n" +
                    "  color: #fff !important;\n" +
                    "}\n" +
                    ".dm-preview code, .dm-preview pre {\n" +
                    "  background-color: #2d2d2d !important;\n" +
                    "  color: #ce9178 !important;\n" +
                    "}\n";

            case "high-contrast":
                return
                    ".dm-preview {\n" +
                    "  background-color: #000 !important;\n" +
                    "  color: #fff !important;\n" +
                    "  font-weight: 500;\n" +
                    "}\n" +
                    ".dm-preview a {\n" +
                    "  color: #ffff00 !important;\n" +
                    "  text-decoration: underline !important;\n" +
                    "}\n" +
                    ".dm-preview table {\n" +
                    "  border: 2px solid #fff !important;\n" +
                    "}\n" +
                    ".dm-preview th {\n" +
                    "  background-color: #fff !important;\n" +
                    "  color: #000 !important;\n" +
                    "  border: 2px solid #fff !important;\n" +
                    "}\n" +
                    ".dm-preview td {\n" +
                    "  border: 1px solid #fff !important;\n" +
                    "}\n";

            default:
                return "";
        }
    }

    /**
     * 行间距CSS
     */
    private static String generateLineHeightCss(Integer lineHeight) {
        return String.format(
            ".dm-preview p, .dm-preview li, .dm-preview td {\n" +
            "  line-height: %d%% !important;\n" +
            "}\n",
            lineHeight
        );
    }

    /**
     * 页面宽度CSS
     */
    private static String generatePageWidthCss(String pageWidth) {
        String width;
        switch (pageWidth) {
            case "fixed-800":
                width = "800px";
                break;
            case "fixed-1000":
                width = "1000px";
                break;
            case "fixed-1200":
                width = "1200px";
                break;
            default:
                return "";
        }

        return String.format(
            ".dm-preview {\n" +
            "  max-width: %s;\n" +
            "  margin: 0 auto;\n" +
            "}\n",
            width
        );
    }

    /**
     * 图形缩放CSS
     */
    private static String generateGraphicScaleCss(Integer scale) {
        return String.format(
            ".dm-preview img, .dm-preview svg {\n" +
            "  transform: scale(%.2f);\n" +
            "  transform-origin: top left;\n" +
            "}\n",
            scale / 100.0
        );
    }

    /**
     * 打印模式CSS
     */
    private static String generatePrintModeCss(PreviewConfigVO config) {
        StringBuilder css = new StringBuilder();

        css.append("@media print {\n");

        // 隐藏不需要打印的元素
        css.append("  .dm-toolbar, .dm-navigation, button, .no-print {\n");
        css.append("    display: none !important;\n");
        css.append("  }\n");

        // 页面设置
        css.append("  @page {\n");
        css.append("    size: A4;\n");
        css.append("    margin: 2cm;\n");
        css.append("  }\n");

        // 避免元素跨页分割
        if ("avoid".equals(config.getPageBreak())) {
            css.append("  h1, h2, h3, h4, h5, h6 {\n");
            css.append("    page-break-after: avoid;\n");
            css.append("  }\n");
            css.append("  table, figure, img {\n");
            css.append("    page-break-inside: avoid;\n");
            css.append("  }\n");
        }

        // 强制分页
        if ("always".equals(config.getPageBreak())) {
            css.append("  h1 {\n");
            css.append("    page-break-before: always;\n");
            css.append("  }\n");
        }

        // 页眉页脚
        if (Boolean.FALSE.equals(config.getShowHeader())) {
            css.append("  @page { @top-center { content: none; } }\n");
        }
        if (Boolean.FALSE.equals(config.getShowFooter())) {
            css.append("  @page { @bottom-center { content: none; } }\n");
        }

        // 显示链接URL
        css.append("  a[href]:after {\n");
        css.append("    content: ' (' attr(href) ')';\n");
        css.append("  }\n");

        // 优化颜色
        css.append("  * {\n");
        css.append("    color: #000 !important;\n");
        css.append("    background: #fff !important;\n");
        css.append("  }\n");

        css.append("}\n");

        return css.toString();
    }

    /**
     * 隐藏元素CSS
     */
    private static String generateHiddenElementsCss(PreviewConfigVO config) {
        StringBuilder css = new StringBuilder();

        if (Boolean.FALSE.equals(config.getShowDmCode())) {
            css.append(".dm-preview .dmc-code { display: none !important; }\n");
        }

        if (Boolean.FALSE.equals(config.getShowRevision())) {
            css.append(".dm-preview .revision-mark { display: none !important; }\n");
        }

        if (Boolean.FALSE.equals(config.getShowSafetyIcons())) {
            css.append(".dm-preview .safety-icon { display: none !important; }\n");
        }

        return css.toString();
    }

    /**
     * 应用配置到HTML
     *
     * 将生成的CSS插入到HTML的<head>中
     */
    public static String applyConfigToHtml(String html, PreviewConfigVO config) {
        if (html == null || html.isEmpty()) {
            return html;
        }

        if (config == null) {
            return html;
        }

        // 生成CSS
        String customCss = generateCss(config);

        if (customCss.isEmpty()) {
            return html;
        }

        // 查找</head>标签
        int headCloseIndex = html.indexOf("</head>");
        if (headCloseIndex == -1) {
            // 如果没有</head>，尝试在开头插入
            return "<style>\n" + customCss + "</style>\n" + html;
        }

        // 在</head>前插入CSS
        String before = html.substring(0, headCloseIndex);
        String after = html.substring(headCloseIndex);

        return before + "<style>\n/* 用户自定义预览配置 */\n" + customCss + "</style>\n" + after;
    }
}
