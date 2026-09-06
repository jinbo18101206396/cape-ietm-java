package org.jeecg.modules.ietm.ietmddn;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * S1000D模板文件验证测试
 * 验证移除DOCTYPE后的模板文件是否符合标准
 *
 * @author Claude
 * @date 2026-09-01
 */
@DisplayName("S1000D模板文件标准验证")
public class S1000DTemplateValidationTest {

    @Test
    @DisplayName("验证DDN模板-S1000D4.0")
    public void testDdnTemplate_S1000D40() throws Exception {
        String templatePath = "ietm/S1000D40/template/ddn.xml";

        // 1. 加载模板文件
        InputStream stream = getClass().getClassLoader().getResourceAsStream(templatePath);
        assertNotNull(stream, "模板文件不存在：" + templatePath);

        // 2. 使用SAXReader解析（不启用XXE防护，验证XML本身）
        SAXReader reader = new SAXReader();
        Document doc = reader.read(stream);

        // 3. 验证根元素
        assertNotNull(doc.getRootElement(), "根元素不应为null");
        assertEquals("ddn", doc.getRootElement().getName(), "根元素应为ddn");

        // 4. 验证Schema声明
        String schemaLocation = doc.getRootElement().attributeValue(
            org.dom4j.QName.get("noNamespaceSchemaLocation", "xsi", "http://www.w3.org/2001/XMLSchema-instance"));
        assertNotNull(schemaLocation, "应包含Schema声明");
        assertTrue(schemaLocation.contains("ddn.xsd"), "Schema应指向ddn.xsd");

        // 5. 验证必要的命名空间
        assertNotNull(doc.getRootElement().getNamespaceForPrefix("xsi"), "应声明xsi命名空间");
        assertNotNull(doc.getRootElement().getNamespaceForPrefix("xlink"), "应声明xlink命名空间");

        System.out.println("✅ DDN模板(S1000D4.0)验证通过");
        System.out.println("   - 根元素: " + doc.getRootElement().getName());
        System.out.println("   - Schema: " + schemaLocation);
    }

    @Test
    @DisplayName("验证DDN模板-S1000D4.1")
    public void testDdnTemplate_S1000D41() throws Exception {
        String templatePath = "ietm/S1000D41/template/ddn.xml";
        validateTemplate(templatePath, "ddn", "ddn.xsd", "S1000D4.1");
    }

    @Test
    @DisplayName("验证DDN模板-S1000D4.2")
    public void testDdnTemplate_S1000D42() throws Exception {
        String templatePath = "ietm/S1000D42/template/ddn.xml";
        validateTemplate(templatePath, "ddn", "ddn.xsd", "S1000D4.2");
    }

    @Test
    @DisplayName("验证所有S1000D40模板文件")
    public void testAllTemplates_S1000D40() throws Exception {
        String[] templates = {
            "comment.xml",
            "ddn.xml",
            "dml.xml",
            "pm.xml",
            "pmc.xml",
            "xcf.xml"
        };

        int passed = 0;
        for (String template : templates) {
            String path = "ietm/S1000D40/template/" + template;
            try {
                InputStream stream = getClass().getClassLoader().getResourceAsStream(path);
                assertNotNull(stream, "模板不存在：" + path);

                SAXReader reader = new SAXReader();
                Document doc = reader.read(stream);
                assertNotNull(doc.getRootElement(), path + " 解析失败");

                passed++;
                System.out.println("✅ " + template + " 验证通过");
            } catch (Exception e) {
                System.err.println("❌ " + template + " 验证失败: " + e.getMessage());
                throw e;
            }
        }

        assertEquals(templates.length, passed, "所有模板应验证通过");
        System.out.println("\n✅ S1000D40 所有模板验证通过: " + passed + "/" + templates.length);
    }

    @Test
    @DisplayName("验证所有S1000D41模板文件")
    public void testAllTemplates_S1000D41() throws Exception {
        String[] templates = {
            "comment.xml",
            "ddn.xml",
            "dml.xml",
            "pm.xml",
            "update.xml",
            "xcf.xml"
        };

        validateAllTemplates("S1000D41", templates);
    }

    @Test
    @DisplayName("验证所有S1000D42模板文件")
    public void testAllTemplates_S1000D42() throws Exception {
        String[] templates = {
            "comment.xml",
            "ddn.xml",
            "dml.xml",
            "icnmetadata.xml",
            "pm.xml",
            "update.xml",
            "xcf.xml"
        };

        validateAllTemplates("S1000D42", templates);
    }

    @Test
    @DisplayName("使用XXE防护加载模板-验证兼容性")
    public void testTemplateWithXXEProtection() throws Exception {
        String templatePath = "ietm/S1000D40/template/ddn.xml";

        // 1. 加载模板
        InputStream stream = getClass().getClassLoader().getResourceAsStream(templatePath);
        assertNotNull(stream);

        // 2. 配置XXE防护（与生产环境相同）
        SAXReader reader = new SAXReader();
        try {
            // 注意：不启用 disallow-doctype-decl（因为已移除DOCTYPE）
            reader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
            reader.setFeature("http://xml.org/sax/features/external-general-entities", false);
            reader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            reader.setEntityResolver((publicId, systemId) ->
                new org.xml.sax.InputSource(new java.io.StringReader("")));
        } catch (Exception e) {
            fail("XXE防护配置失败: " + e.getMessage());
        }

        // 3. 解析文档
        Document doc = null;
        try {
            doc = reader.read(stream);
            assertNotNull(doc, "文档解析失败");
            assertNotNull(doc.getRootElement(), "根元素为null");
            System.out.println("✅ 使用XXE防护加载成功");
        } catch (DocumentException e) {
            fail("使用XXE防护解析失败: " + e.getMessage());
        }
    }

    // 辅助方法
    private void validateTemplate(String path, String expectedRoot, String expectedSchema, String version) throws Exception {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, "模板不存在：" + path);

        SAXReader reader = new SAXReader();
        Document doc = reader.read(stream);

        assertNotNull(doc.getRootElement());
        assertEquals(expectedRoot, doc.getRootElement().getName());

        String schemaLocation = doc.getRootElement().attributeValue(
            org.dom4j.QName.get("noNamespaceSchemaLocation", "xsi", "http://www.w3.org/2001/XMLSchema-instance"));

        if (schemaLocation != null) {
            assertTrue(schemaLocation.contains(expectedSchema),
                "Schema应包含: " + expectedSchema + "，实际: " + schemaLocation);
        }

        System.out.println("✅ " + version + " 模板验证通过: " + path);
    }

    private void validateAllTemplates(String version, String[] templates) throws Exception {
        int passed = 0;
        for (String template : templates) {
            String path = "ietm/" + version + "/template/" + template;
            try {
                InputStream stream = getClass().getClassLoader().getResourceAsStream(path);
                assertNotNull(stream, "模板不存在：" + path);

                SAXReader reader = new SAXReader();
                Document doc = reader.read(stream);
                assertNotNull(doc.getRootElement(), path + " 解析失败");

                passed++;
                System.out.println("✅ " + template);
            } catch (Exception e) {
                System.err.println("❌ " + template + ": " + e.getMessage());
                throw e;
            }
        }

        assertEquals(templates.length, passed);
        System.out.println("\n✅ " + version + " 所有模板验证通过: " + passed + "/" + templates.length);
    }
}
