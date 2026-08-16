package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 状况模块（identAndStatusSection）字段显示测试
 *
 * @author claude
 * @date 2026-08-07
 */
@DisplayName("状况模块字段显示测试")
public class DmPreviewIdentAndStatusTest {

    @Test
    @DisplayName("验证状况模块HTML生成")
    public void testIdentAndStatusSectionRendered() throws Exception {
        String xml = buildTestDmWithFullStatus();

        // XSLT转换
        String xsltHtml = DmXsltTransformer.transform(xml, "S1000D40", "descript");

        // 完整流程
        String finalHtml = DmXsltTransformer.enhancePreviewHtml(xsltHtml);

        System.out.println("\n========================================");
        System.out.println("状况模块字段显示测试");
        System.out.println("========================================\n");

        // 检查是否包含idstatus div
        boolean hasIdStatusDiv = finalHtml.contains("id=\"idstatus\"");
        System.out.println("包含idstatus div: " + (hasIdStatusDiv ? "✅ 是" : "❌ 否"));

        // 检查class="hidesection"
        boolean hasHideSection = finalHtml.contains("class=\"hidesection\"");
        System.out.println("包含hidesection类: " + (hasHideSection ? "⚠️  是（可能被隐藏）" : "✅ 否"));

        // 提取idstatus部分
        if (hasIdStatusDiv) {
            int idStatusStart = finalHtml.indexOf("<div class=\"hidesection\" id=\"idstatus\">");
            if (idStatusStart == -1) {
                idStatusStart = finalHtml.indexOf("<div id=\"idstatus\"");
            }

            if (idStatusStart != -1) {
                int idStatusEnd = finalHtml.indexOf("</div>", idStatusStart);
                if (idStatusEnd > idStatusStart) {
                    String idStatusHtml = finalHtml.substring(idStatusStart, idStatusEnd + 6);

                    System.out.println("\n========== 状况模块HTML片段 ==========");
                    System.out.println(idStatusHtml.substring(0, Math.min(1000, idStatusHtml.length())));
                    System.out.println("=======================================\n");

                    // 检查关键字段
                    checkField(idStatusHtml, "DMC", "DMC-TEST-A-00-0-0-00-00A-040A-A");
                    checkField(idStatusHtml, "密级", "security");
                    checkField(idStatusHtml, "发行", "issue");
                    checkField(idStatusHtml, "语言", "language");
                    checkField(idStatusHtml, "标题", "dmTitle");
                    checkField(idStatusHtml, "技术名称", "测试技术名称");
                    checkField(idStatusHtml, "信息名称", "测试信息名称");
                }
            }
        }

        // 关键断言
        assertTrue(hasIdStatusDiv, "应该生成identAndStatusSection的HTML");

        System.out.println("\n========================================");
        System.out.println("结论");
        System.out.println("========================================");

        if (hasHideSection) {
            System.out.println("⚠️  状况模块被class=\"hidesection\"标记");
            System.out.println("   这可能导致前端不显示，需要：");
            System.out.println("   1. 检查CSS中hidesection的定义");
            System.out.println("   2. 或修改XSLT移除hidesection类");
            System.out.println("   3. 或前端JavaScript控制显示");
        } else {
            System.out.println("✅ 状况模块未被标记为隐藏");
        }

        System.out.println("========================================\n");
    }

    private void checkField(String html, String fieldName, String expectedValue) {
        boolean contains = html.contains(expectedValue);
        if (contains) {
            System.out.println("✅ 包含" + fieldName + ": " + expectedValue);
        } else {
            System.out.println("❌ 不包含" + fieldName + ": " + expectedValue);
        }
    }

    private String buildTestDmWithFullStatus() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<dmodule xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
            "xsi:noNamespaceSchemaLocation=\"http://www.s1000d.org/S1000D_4-0/xml_schema_flat/descript.xsd\">\n" +
            "  <identAndStatusSection>\n" +
            "    <dmAddress>\n" +
            "      <dmIdent>\n" +
            "        <dmCode modelIdentCode=\"TEST\" systemDiffCode=\"A\" systemCode=\"00\" " +
            "subSystemCode=\"0\" subSubSystemCode=\"0\" assyCode=\"00\" disassyCode=\"00\" " +
            "disassyCodeVariant=\"A\" infoCode=\"040\" infoCodeVariant=\"A\" itemLocationCode=\"A\"/>\n" +
            "        <language languageIsoCode=\"zh\" countryIsoCode=\"CN\"/>\n" +
            "        <issueInfo issueNumber=\"001\" inWork=\"00\"/>\n" +
            "      </dmIdent>\n" +
            "      <dmAddressItems>\n" +
            "        <issueDate year=\"2026\" month=\"08\" day=\"07\"/>\n" +
            "        <dmTitle>\n" +
            "          <techName>测试技术名称</techName>\n" +
            "          <infoName>测试信息名称</infoName>\n" +
            "        </dmTitle>\n" +
            "      </dmAddressItems>\n" +
            "    </dmAddress>\n" +
            "    <dmStatus>\n" +
            "      <security securityClassification=\"01\"/>\n" +
            "      <responsiblePartnerCompany enterpriseCode=\"TEST001\">\n" +
            "        <enterpriseName>测试公司</enterpriseName>\n" +
            "      </responsiblePartnerCompany>\n" +
            "      <originator enterpriseCode=\"ORIG001\">\n" +
            "        <enterpriseName>原始编制单位</enterpriseName>\n" +
            "      </originator>\n" +
            "      <applic>\n" +
            "        <displayText>\n" +
            "          <simplePara>适用性信息</simplePara>\n" +
            "        </displayText>\n" +
            "      </applic>\n" +
            "      <brexDmRef>\n" +
            "        <dmRef>\n" +
            "          <dmRefIdent>\n" +
            "            <dmCode modelIdentCode=\"BREX\" systemDiffCode=\"A\" systemCode=\"00\" " +
            "subSystemCode=\"0\" subSubSystemCode=\"0\" assyCode=\"00\" disassyCode=\"00\" " +
            "disassyCodeVariant=\"A\" infoCode=\"022\" infoCodeVariant=\"A\" itemLocationCode=\"D\"/>\n" +
            "          </dmRefIdent>\n" +
            "        </dmRef>\n" +
            "      </brexDmRef>\n" +
            "      <qualityAssurance>\n" +
            "        <unverified/>\n" +
            "      </qualityAssurance>\n" +
            "    </dmStatus>\n" +
            "  </identAndStatusSection>\n" +
            "  <content>\n" +
            "    <description>\n" +
            "      <levelledPara>\n" +
            "        <title>测试内容</title>\n" +
            "        <para>这是测试内容。</para>\n" +
            "      </levelledPara>\n" +
            "    </description>\n" +
            "  </content>\n" +
            "</dmodule>";
    }
}
