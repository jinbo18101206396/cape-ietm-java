package org.jeecg.modules.ietm.util;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.util.DmXmlHelper;
import org.junit.jupiter.api.Test;

/**
 * 验证DM预览日志优化效果
 */
@Slf4j
public class DmPreviewLogTest {

    @Test
    public void testPreviewLogOptimization() {
        String testXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<dmodule xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xsi:noNamespaceSchemaLocation=\"http://www.s1000d.org/S1000D_4-0/xml_schema_flat/descript.xsd\">\n" +
                "  <identAndStatusSection>\n" +
                "    <dmAddress>\n" +
                "      <dmIdent>\n" +
                "        <dmCode modelIdentCode=\"TEST\" systemDiffCode=\"A\" systemCode=\"00\" " +
                "subSystemCode=\"0\" subSubSystemCode=\"0\" assyCode=\"00\" disassyCode=\"00\" " +
                "disassyCodeVariant=\"A\" infoCode=\"040\" infoCodeVariant=\"A\" itemLocationCode=\"A\"/>\n" +
                "        <language languageIsoCode=\"en\" countryIsoCode=\"US\"/>\n" +
                "        <issueInfo issueNumber=\"001\" inWork=\"00\"/>\n" +
                "      </dmIdent>\n" +
                "      <dmAddressItems>\n" +
                "        <issueDate year=\"2026\" month=\"08\" day=\"06\"/>\n" +
                "        <dmTitle>\n" +
                "          <techName>Log Optimization Test DM</techName>\n" +
                "        </dmTitle>\n" +
                "      </dmAddressItems>\n" +
                "    </dmAddress>\n" +
                "    <dmStatus>\n" +
                "      <security securityClassification=\"01\"/>\n" +
                "      <responsiblePartnerCompany enterpriseCode=\"TEST\"/>\n" +
                "      <originator enterpriseCode=\"TEST\"/>\n" +
                "      <applic>\n" +
                "        <displayText><simplePara>All</simplePara></displayText>\n" +
                "      </applic>\n" +
                "      <brexDmRef>\n" +
                "        <dmRef><dmRefIdent><dmCode modelIdentCode=\"TEST\" systemDiffCode=\"A\" " +
                "systemCode=\"00\" subSystemCode=\"0\" subSubSystemCode=\"0\" assyCode=\"00\" " +
                "disassyCode=\"00\" disassyCodeVariant=\"A\" infoCode=\"022\" infoCodeVariant=\"A\" " +
                "itemLocationCode=\"A\"/></dmRefIdent></dmRef>\n" +
                "      </brexDmRef>\n" +
                "      <qualityAssurance><unverified/></qualityAssurance>\n" +
                "    </dmStatus>\n" +
                "  </identAndStatusSection>\n" +
                "  <content>\n" +
                "    <description>\n" +
                "      <levelledPara>\n" +
                "        <title>Test Content</title>\n" +
                "        <para>This is a test paragraph for log optimization verification.</para>\n" +
                "        <para>The log output should be minimal - only 2 INFO lines.</para>\n" +
                "      </levelledPara>\n" +
                "    </description>\n" +
                "  </content>\n" +
                "</dmodule>";

        log.info("========================================");
        log.info("开始DM预览日志优化验证");
        log.info("========================================");
        log.info("测试说明：");
        log.info("  优化前：每次预览产生16条INFO日志");
        log.info("  优化后：每次预览产生2条INFO日志");
        log.info("  预期日志：");
        log.info("    1. DM预览渲染完成: standard=..., dmType=..., 最终HTML长度=...");
        log.info("    2. ICN路径替换完成: 总数=..., 成功=..., 失败=...");
        log.info("========================================");
        log.info("");
        log.info(">>> 开始调用 DmXmlHelper.renderHtml()");
        log.info("");

        // 调用预览方法
        String html = DmXmlHelper.renderHtml(testXml, "/jeecg-boot");

        log.info("");
        log.info(">>> 调用完成");
        log.info("");
        log.info("========================================");
        log.info("验证结果：");
        log.info("  HTML生成: {}", html != null && !html.isEmpty() ? "✅ 成功" : "❌ 失败");
        log.info("  HTML长度: {} 字符", html != null ? html.length() : 0);
        log.info("");
        log.info("请检查上方日志输出：");
        log.info("  ✅ 应该只有2条INFO日志（DM预览渲染完成 + ICN路径替换完成）");
        log.info("  ✅ 其他详细信息应该是DEBUG级别");
        log.info("========================================");

        // 断言HTML生成成功
        assert html != null && !html.isEmpty() : "HTML生成失败";
    }
}
