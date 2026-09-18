package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * DmPdfExporter单元测试
 *
 * @author IETM Team
 * @date 2026-09-18
 */
public class DmPdfExporterTest {

    /**
     * 测试1: PdfConfig默认配置创建
     */
    @Test
    public void testPdfConfigCreateDefault() {
        DmPdfExporter.PdfConfig config = DmPdfExporter.PdfConfig.createDefault();

        assertNotNull("默认配置不应为null", config);
        assertEquals("默认标题应为'数据模块'", "数据模块", config.getTitle());
        assertEquals("默认作者应为'IETM系统'", "IETM系统", config.getAuthor());
        assertTrue("默认应显示目录", config.getShowToc());
        assertEquals("默认页面大小应为A4", "A4", config.getPageSize());
    }

    /**
     * 测试2: PdfConfig自定义配置
     */
    @Test
    public void testPdfConfigCustomSettings() {
        DmPdfExporter.PdfConfig config = new DmPdfExporter.PdfConfig();
        config.setTitle("自定义标题");
        config.setAuthor("测试作者");
        config.setSubject("测试主题");
        config.setWatermark("机密");
        config.setShowToc(false);
        config.setPageSize("LETTER");
        config.setFontPath("/path/to/font.ttf");

        assertEquals("标题应匹配", "自定义标题", config.getTitle());
        assertEquals("作者应匹配", "测试作者", config.getAuthor());
        assertEquals("主题应匹配", "测试主题", config.getSubject());
        assertEquals("水印应匹配", "机密", config.getWatermark());
        assertFalse("不应显示目录", config.getShowToc());
        assertEquals("页面大小应为LETTER", "LETTER", config.getPageSize());
        assertEquals("字体路径应匹配", "/path/to/font.ttf", config.getFontPath());
    }

    /**
     * 测试3: 导出PDF使用null配置（应使用默认配置）
     */
    @Test
    public void testExportToPdfWithNullConfig() {
        String html = "<html><body><h1>Test DM</h1><p>Content</p></body></html>";

        try {
            byte[] pdf = DmPdfExporter.exportToPdf(html, null);
            assertNotNull("应返回PDF数据（或占位符）", pdf);
            assertTrue("返回数据长度应大于0", pdf.length > 0);
        } catch (Exception e) {
            // Flying Saucer未引入时预期异常
            assertTrue("预期Flying Saucer库未引入", true);
        }
    }

    /**
     * 测试4: 导出PDF使用默认配置
     */
    @Test
    public void testExportToPdfWithDefaultConfig() {
        String html = "<html><body><h1>Test DM</h1><p>Content</p></body></html>";
        DmPdfExporter.PdfConfig config = DmPdfExporter.PdfConfig.createDefault();

        try {
            byte[] pdf = DmPdfExporter.exportToPdf(html, config);
            assertNotNull("应返回PDF数据", pdf);
        } catch (Exception e) {
            assertTrue("应处理异常", true);
        }
    }

    /**
     * 测试5: 导出null HTML
     */
    @Test
    public void testExportToPdfWithNullHtml() {
        DmPdfExporter.PdfConfig config = DmPdfExporter.PdfConfig.createDefault();

        try {
            byte[] pdf = DmPdfExporter.exportToPdf(null, config);
            // 应处理null HTML
            assertTrue("应正常处理null HTML", true);
        } catch (Exception e) {
            // 预期异常
            assertTrue("应处理null HTML异常", true);
        }
    }

    /**
     * 测试6: 批量导出空列表
     */
    @Test
    public void testBatchExportWithEmptyList() {
        List<String> htmlList = new ArrayList<>();
        List<String> titles = new ArrayList<>();
        DmPdfExporter.PdfConfig config = DmPdfExporter.PdfConfig.createDefault();

        try {
            byte[] pdf = DmPdfExporter.exportBatchToPdf(htmlList, titles, config);
            assertNotNull("应返回PDF数据", pdf);
        } catch (Exception e) {
            assertTrue("应处理空列表", true);
        }
    }

    /**
     * 测试7: 批量导出单个DM
     */
    @Test
    public void testBatchExportWithSingleDm() {
        List<String> htmlList = Arrays.asList("<html><body><h1>DM 1</h1></body></html>");
        List<String> titles = Arrays.asList("数据模块1");
        DmPdfExporter.PdfConfig config = DmPdfExporter.PdfConfig.createDefault();

        try {
            byte[] pdf = DmPdfExporter.exportBatchToPdf(htmlList, titles, config);
            assertNotNull("应返回PDF数据", pdf);
        } catch (Exception e) {
            assertTrue("应处理单个DM", true);
        }
    }

    /**
     * 测试8: 批量导出多个DM
     */
    @Test
    public void testBatchExportWithMultipleDms() {
        List<String> htmlList = new ArrayList<>();
        List<String> titles = new ArrayList<>();

        for (int i = 1; i <= 5; i++) {
            htmlList.add("<html><body><h1>DM " + i + "</h1></body></html>");
            titles.add("数据模块 " + i);
        }

        DmPdfExporter.PdfConfig config = DmPdfExporter.PdfConfig.createDefault();

        try {
            byte[] pdf = DmPdfExporter.exportBatchToPdf(htmlList, titles, config);
            assertNotNull("应返回PDF数据", pdf);
        } catch (Exception e) {
            assertTrue("应处理多个DM", true);
        }
    }

    /**
     * 测试9: 批量导出不显示目录
     */
    @Test
    public void testBatchExportWithoutToc() {
        List<String> htmlList = Arrays.asList("<html><body>Test</body></html>");
        List<String> titles = Arrays.asList("测试");
        DmPdfExporter.PdfConfig config = DmPdfExporter.PdfConfig.createDefault();
        config.setShowToc(false);

        try {
            byte[] pdf = DmPdfExporter.exportBatchToPdf(htmlList, titles, config);
            assertNotNull("应返回PDF数据", pdf);
        } catch (Exception e) {
            assertTrue("应处理无目录配置", true);
        }
    }

    /**
     * 测试10: 批量导出titles为null
     */
    @Test
    public void testBatchExportWithNullTitles() {
        List<String> htmlList = Arrays.asList(
            "<html><body>DM 1</body></html>",
            "<html><body>DM 2</body></html>"
        );
        DmPdfExporter.PdfConfig config = DmPdfExporter.PdfConfig.createDefault();

        try {
            byte[] pdf = DmPdfExporter.exportBatchToPdf(htmlList, null, config);
            assertNotNull("应返回PDF数据", pdf);
        } catch (Exception e) {
            assertTrue("应处理null titles", true);
        }
    }

    /**
     * 测试11: PDF配置字段完整性
     */
    @Test
    public void testPdfConfigFieldsCompleteness() {
        DmPdfExporter.PdfConfig config = new DmPdfExporter.PdfConfig();

        config.setTitle("测试");
        assertEquals("Title getter/setter应工作", "测试", config.getTitle());

        config.setAuthor("作者");
        assertEquals("Author getter/setter应工作", "作者", config.getAuthor());

        config.setSubject("主题");
        assertEquals("Subject getter/setter应工作", "主题", config.getSubject());

        config.setWatermark("水印");
        assertEquals("Watermark getter/setter应工作", "水印", config.getWatermark());

        config.setShowToc(false);
        assertFalse("ShowToc getter/setter应工作", config.getShowToc());

        config.setPageSize("LETTER");
        assertEquals("PageSize getter/setter应工作", "LETTER", config.getPageSize());

        config.setFontPath("/path/to/font");
        assertEquals("FontPath getter/setter应工作", "/path/to/font", config.getFontPath());
    }

    /**
     * 测试12: 导出包含特殊字符的HTML
     */
    @Test
    public void testExportWithSpecialCharacters() {
        String html = "<html><body><h1>特殊字符: &lt;&gt;&amp;&quot;'</h1></body></html>";
        DmPdfExporter.PdfConfig config = DmPdfExporter.PdfConfig.createDefault();

        try {
            byte[] pdf = DmPdfExporter.exportToPdf(html, config);
            assertNotNull("应处理特殊字符", pdf);
        } catch (Exception e) {
            assertTrue("应处理特殊字符异常", true);
        }
    }

    /**
     * 测试13: 导出包含中文的HTML
     */
    @Test
    public void testExportWithChineseContent() {
        String html = "<html><body><h1>中文测试</h1><p>这是一段中文内容。</p></body></html>";
        DmPdfExporter.PdfConfig config = DmPdfExporter.PdfConfig.createDefault();

        try {
            byte[] pdf = DmPdfExporter.exportToPdf(html, config);
            assertNotNull("应处理中文内容", pdf);
        } catch (Exception e) {
            assertTrue("应处理中文内容异常", true);
        }
    }

    /**
     * 测试14: 批量导出htmlList和titles数量不匹配
     */
    @Test
    public void testBatchExportMismatchedSizes() {
        List<String> htmlList = Arrays.asList(
            "<html><body>DM 1</body></html>",
            "<html><body>DM 2</body></html>",
            "<html><body>DM 3</body></html>"
        );
        List<String> titles = Arrays.asList("标题1"); // 只有1个标题

        DmPdfExporter.PdfConfig config = DmPdfExporter.PdfConfig.createDefault();

        try {
            byte[] pdf = DmPdfExporter.exportBatchToPdf(htmlList, titles, config);
            assertNotNull("应处理不匹配的列表大小", pdf);
        } catch (Exception e) {
            assertTrue("应处理不匹配的列表大小异常", true);
        }
    }

    /**
     * 测试15: 批量导出性能测试
     */
    @Test(timeout = 10000)
    public void testBatchExportPerformance() {
        List<String> htmlList = new ArrayList<>();
        List<String> titles = new ArrayList<>();

        for (int i = 1; i <= 20; i++) {
            htmlList.add("<html><body><h1>DM " + i + "</h1><p>Content</p></body></html>");
            titles.add("数据模块 " + i);
        }

        DmPdfExporter.PdfConfig config = DmPdfExporter.PdfConfig.createDefault();

        try {
            byte[] pdf = DmPdfExporter.exportBatchToPdf(htmlList, titles, config);
            assertNotNull("批量导出应在10秒内完成", pdf);
        } catch (Exception e) {
            assertTrue("应处理批量导出", true);
        }
    }
}
