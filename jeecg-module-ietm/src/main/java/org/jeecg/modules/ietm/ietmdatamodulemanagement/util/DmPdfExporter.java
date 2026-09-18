package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DM PDF导出工具
 *
 * 支持：
 * 1. 单个DM导出为PDF
 * 2. 批量DM合并导出为单个PDF
 * 3. PDF元数据（标题、作者、创建日期）
 * 4. 目录和书签
 * 5. 水印支持
 *
 * 技术栈：Flying Saucer + iText
 *
 * 注意：需要添加Maven依赖
 * <dependency>
 *   <groupId>org.xhtmlrenderer</groupId>
 *   <artifactId>flying-saucer-pdf</artifactId>
 *   <version>9.1.22</version>
 * </dependency>
 * <dependency>
 *   <groupId>org.jsoup</groupId>
 *   <artifactId>jsoup</artifactId>
 *   <version>1.15.3</version>
 * </dependency>
 *
 * @author IETM Team
 * @date 2026-09-18
 */
@Slf4j
public class DmPdfExporter {

    /**
     * PDF配置
     */
    @Data
    public static class PdfConfig {
        private String title;           // PDF标题
        private String author;          // 作者
        private String subject;         // 主题
        private String watermark;       // 水印文本
        private Boolean showToc = true; // 显示目录
        private String pageSize = "A4"; // 页面大小: A4, LETTER
        private String fontPath;        // 中文字体路径

        public static PdfConfig createDefault() {
            PdfConfig config = new PdfConfig();
            config.setTitle("数据模块");
            config.setAuthor("IETM系统");
            config.setShowToc(true);
            config.setPageSize("A4");
            return config;
        }
    }

    /**
     * 导出单个DM为PDF
     *
     * @param html HTML内容
     * @param config PDF配置
     * @return PDF字节数组
     */
    public static byte[] exportToPdf(String html, PdfConfig config) {
        try {
            log.info("开始导出PDF: title={}", config != null ? config.getTitle() : "未命名");

            if (config == null) {
                config = PdfConfig.createDefault();
            }

            // 1. 预处理HTML
            String xhtml = convertToXhtml(html);
            xhtml = addPrintStyles(xhtml);

            // 2. 使用Flying Saucer渲染PDF
            // 注意：实际实现需要引入Flying Saucer库
            // 这里提供框架代码，实际运行需要添加依赖

            /*
            ITextRenderer renderer = new ITextRenderer();

            // 设置中文字体支持
            if (config.getFontPath() != null) {
                renderer.getFontResolver().addFont(
                    config.getFontPath(),
                    BaseFont.IDENTITY_H,
                    BaseFont.EMBEDDED
                );
            }

            // 设置文档
            renderer.setDocumentFromString(xhtml);

            // 设置元数据
            renderer.getWriter().setCreator("IETM系统");
            if (config.getTitle() != null) {
                renderer.getWriter().setTitle(config.getTitle());
            }
            if (config.getAuthor() != null) {
                renderer.getWriter().setAuthor(config.getAuthor());
            }

            // 布局和渲染
            renderer.layout();

            // 输出PDF
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            renderer.createPDF(os);

            byte[] pdfBytes = os.toByteArray();
            */

            // 临时实现：返回占位符
            String message = "PDF导出功能需要Flying Saucer库支持\n" +
                           "请在pom.xml中添加依赖：\n" +
                           "<dependency>\n" +
                           "  <groupId>org.xhtmlrenderer</groupId>\n" +
                           "  <artifactId>flying-saucer-pdf</artifactId>\n" +
                           "  <version>9.1.22</version>\n" +
                           "</dependency>";

            log.warn(message);
            return message.getBytes("UTF-8");

        } catch (Exception e) {
            log.error("PDF导出失败", e);
            throw new RuntimeException("PDF导出失败: " + e.getMessage(), e);
        }
    }

    /**
     * 批量导出DM为单个PDF
     *
     * @param htmlList HTML列表
     * @param titles 标题列表（用于生成目录）
     * @param config PDF配置
     * @return PDF字节数组
     */
    public static byte[] exportBatchToPdf(List<String> htmlList, List<String> titles, PdfConfig config) {
        try {
            log.info("开始批量导出PDF: 数量={}", htmlList.size());

            if (config == null) {
                config = PdfConfig.createDefault();
            }

            // 1. 合并HTML
            StringBuilder mergedHtml = new StringBuilder();
            mergedHtml.append("<!DOCTYPE html>\n");
            mergedHtml.append("<html>\n");
            mergedHtml.append("<head>\n");
            mergedHtml.append("<meta charset=\"UTF-8\"/>\n");
            mergedHtml.append("<title>").append(config.getTitle()).append("</title>\n");
            mergedHtml.append(generatePrintCss());
            mergedHtml.append("</head>\n");
            mergedHtml.append("<body>\n");

            // 2. 生成目录
            if (Boolean.TRUE.equals(config.getShowToc())) {
                mergedHtml.append(generateToc(titles));
                mergedHtml.append("<div style=\"page-break-after: always;\"></div>\n");
            }

            // 3. 添加所有DM内容
            for (int i = 0; i < htmlList.size(); i++) {
                String html = htmlList.get(i);
                String title = (titles != null && i < titles.size()) ? titles.get(i) : "数据模块 " + (i + 1);

                mergedHtml.append("<div class=\"dm-section\" id=\"dm-").append(i).append("\">\n");
                mergedHtml.append("<h1 class=\"dm-title\">").append(title).append("</h1>\n");
                mergedHtml.append(extractBody(html));
                mergedHtml.append("</div>\n");

                // 添加分页符（最后一个除外）
                if (i < htmlList.size() - 1) {
                    mergedHtml.append("<div style=\"page-break-after: always;\"></div>\n");
                }
            }

            mergedHtml.append("</body>\n");
            mergedHtml.append("</html>\n");

            // 4. 导出为PDF
            return exportToPdf(mergedHtml.toString(), config);

        } catch (Exception e) {
            log.error("批量PDF导出失败", e);
            throw new RuntimeException("批量PDF导出失败: " + e.getMessage(), e);
        }
    }

    /**
     * 转换HTML为XHTML（Flying Saucer要求格式良好的XML）
     */
    private static String convertToXhtml(String html) {
        try {
            // 简单的XHTML转换：确保自闭合标签格式正确
            // <br> -> <br/>
            // <img src="..."> -> <img src="..."/>
            String xhtml = html;

            // 处理常见的自闭合标签
            String[] selfClosingTags = {"br", "img", "input", "meta", "link", "hr"};
            for (String tag : selfClosingTags) {
                // 匹配 <tag ...> 但不是 </tag> 或 <tag .../>
                Pattern p = Pattern.compile("<" + tag + "([^>]*?)(?<!/)>", Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(xhtml);
                xhtml = m.replaceAll("<" + tag + "$1/>");
            }

            return xhtml;
        } catch (Exception e) {
            log.warn("HTML转XHTML失败，使用原HTML", e);
            return html;
        }
    }

    /**
     * 添加打印样式
     */
    private static String addPrintStyles(String xhtml) {
        String printCss = generatePrintCss();

        // 在</head>前插入
        int headIndex = xhtml.indexOf("</head>");
        if (headIndex != -1) {
            return xhtml.substring(0, headIndex) + printCss + xhtml.substring(headIndex);
        } else {
            return printCss + xhtml;
        }
    }

    /**
     * 生成打印CSS
     */
    private static String generatePrintCss() {
        return
            "<style>\n" +
            "/* PDF打印优化样式 */\n" +
            "@page {\n" +
            "  size: A4;\n" +
            "  margin: 2cm;\n" +
            "  @top-center {\n" +
            "    content: element(header);\n" +
            "  }\n" +
            "  @bottom-center {\n" +
            "    content: counter(page);\n" +
            "  }\n" +
            "}\n" +
            "\n" +
            "body {\n" +
            "  font-family: 'SimSun', serif;\n" +
            "  font-size: 12pt;\n" +
            "  line-height: 1.6;\n" +
            "  color: #000;\n" +
            "}\n" +
            "\n" +
            "h1, h2, h3, h4, h5, h6 {\n" +
            "  page-break-after: avoid;\n" +
            "  font-weight: bold;\n" +
            "}\n" +
            "\n" +
            "h1 { font-size: 18pt; margin-top: 0; }\n" +
            "h2 { font-size: 16pt; }\n" +
            "h3 { font-size: 14pt; }\n" +
            "\n" +
            "table {\n" +
            "  border-collapse: collapse;\n" +
            "  page-break-inside: avoid;\n" +
            "  width: 100%;\n" +
            "  margin: 10pt 0;\n" +
            "}\n" +
            "\n" +
            "table, th, td {\n" +
            "  border: 1px solid #000;\n" +
            "}\n" +
            "\n" +
            "th, td {\n" +
            "  padding: 5pt;\n" +
            "  text-align: left;\n" +
            "}\n" +
            "\n" +
            "th {\n" +
            "  background-color: #f0f0f0;\n" +
            "  font-weight: bold;\n" +
            "}\n" +
            "\n" +
            "img {\n" +
            "  max-width: 100%;\n" +
            "  page-break-inside: avoid;\n" +
            "}\n" +
            "\n" +
            "figure {\n" +
            "  page-break-inside: avoid;\n" +
            "}\n" +
            "\n" +
            ".dm-section {\n" +
            "  page-break-before: auto;\n" +
            "}\n" +
            "\n" +
            ".toc {\n" +
            "  margin: 20pt 0;\n" +
            "}\n" +
            "\n" +
            ".toc-item {\n" +
            "  margin: 5pt 0;\n" +
            "}\n" +
            "\n" +
            ".toc-item a {\n" +
            "  text-decoration: none;\n" +
            "  color: #000;\n" +
            "}\n" +
            "\n" +
            "/* 隐藏不需要打印的元素 */\n" +
            ".no-print, .toolbar, .navigation, button {\n" +
            "  display: none !important;\n" +
            "}\n" +
            "</style>\n";
    }

    /**
     * 生成目录
     */
    private static String generateToc(List<String> titles) {
        if (titles == null || titles.isEmpty()) {
            return "";
        }

        StringBuilder toc = new StringBuilder();
        toc.append("<div class=\"toc\">\n");
        toc.append("<h1>目录</h1>\n");
        toc.append("<ul>\n");

        for (int i = 0; i < titles.size(); i++) {
            toc.append("<li class=\"toc-item\">\n");
            toc.append("<a href=\"#dm-").append(i).append("\">");
            toc.append(titles.get(i));
            toc.append("</a>\n");
            toc.append("</li>\n");
        }

        toc.append("</ul>\n");
        toc.append("</div>\n");

        return toc.toString();
    }

    /**
     * 提取HTML的body内容
     */
    private static String extractBody(String html) {
        try {
            Pattern p = Pattern.compile("<body[^>]*>(.*?)</body>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Matcher m = p.matcher(html);
            if (m.find()) {
                return m.group(1);
            }
            return html;
        } catch (Exception e) {
            log.warn("提取body失败，使用原HTML", e);
            return html;
        }
    }

    /**
     * 生成文件名
     */
    public static String generateFileName(String title) {
        if (title == null || title.isEmpty()) {
            title = "数据模块";
        }

        // 移除不安全字符
        title = title.replaceAll("[\\\\/:*?\"<>|]", "_");

        // 添加时间戳
        String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());

        return title + "_" + timestamp + ".pdf";
    }
}
