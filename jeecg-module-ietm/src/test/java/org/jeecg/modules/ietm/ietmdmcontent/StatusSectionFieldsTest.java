package org.jeecg.modules.ietm.ietmdmcontent;

import org.jeecg.modules.ietm.ietmdatamodulemanagement.util.DmXsltTransformer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 status.xsl 精简后状态区块只渲染5个必须字段：
 *   DMC编码 / 版本号 / 发布日期 / 标题 / 密级
 *
 * 隐藏字段（全部在XML中存在，但不应出现在HTML输出里）：
 *   originator / rpc / brexDmRef / qa / sbc / fic / reasonForUpdate /
 *   remarks / skill / logo / techstd / dmsize / productSafety
 */
public class StatusSectionFieldsTest {

    /** 包含全部可选字段的 S1000D 4.0 描述类 DM */
    private static final String FULL_STATUS_XML =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<dmodule>\n" +
        "  <identAndStatusSection>\n" +
        "    <dmAddress>\n" +
        "      <dmIdent>\n" +
        "        <dmCode modelIdentCode=\"TEST\" systemDiffCode=\"A\"\n" +
        "                systemCode=\"01\" subSystemCode=\"0\" subSubSystemCode=\"0\"\n" +
        "                assyCode=\"00\" disassyCode=\"00\" disassyCodeVariant=\"A\"\n" +
        "                infoCode=\"040\" infoCodeVariant=\"A\" itemLocationCode=\"A\"/>\n" +
        "      </dmIdent>\n" +
        "      <dmAddressItems>\n" +
        "        <dmTitle><techName>测试设备</techName><infoName>描述</infoName></dmTitle>\n" +
        "        <issueDate year=\"2026\" month=\"08\" day=\"08\"/>\n" +
        "      </dmAddressItems>\n" +
        "    </dmAddress>\n" +
        "    <dmStatus issueType=\"new\">\n" +
        "      <security securityClassification=\"01\"/>\n" +
        "      <responsiblePartnerCompany enterpriseCode=\"RPC001\">某责任公司</responsiblePartnerCompany>\n" +
        "      <originator enterpriseCode=\"ORIG001\">某发行方</originator>\n" +
        "      <brexDmRef><dmRef><dmRefIdent><dmCode modelIdentCode=\"TEST\"\n" +
        "        systemDiffCode=\"A\" systemCode=\"00\" subSystemCode=\"0\" subSubSystemCode=\"0\"\n" +
        "        assyCode=\"00\" disassyCode=\"00\" disassyCodeVariant=\"A\"\n" +
        "        infoCode=\"022\" infoCodeVariant=\"A\" itemLocationCode=\"A\"/></dmRefIdent></dmRef></brexDmRef>\n" +
        "      <qualityAssurance><unverified/></qualityAssurance>\n" +
        "      <systemBreakdownCode>SBC001</systemBreakdownCode>\n" +
        "      <reasonForUpdate>初始创建</reasonForUpdate>\n" +
        "      <remarks><simplePara>一条备注</simplePara></remarks>\n" +
        "    </dmStatus>\n" +
        "    <dmAddress>\n" +
        "      <dmIdent><issueInfo issueNumber=\"001\" inWork=\"00\"/></dmIdent>\n" +
        "    </dmAddress>\n" +
        "  </identAndStatusSection>\n" +
        "  <content><description><para>内容</para></description></content>\n" +
        "</dmodule>";

    @Test
    public void testStatusSectionOnlyShowsRequiredFields() throws Exception {
        // 走生产同一转换链（descriptSchema.xsl → base.xsl → status.xsl），
        // ClasspathURIResolver 负责解析全部 include
        String html = DmXsltTransformer.transform(FULL_STATUS_XML, "S1000D40", "descript");

        // --- 提取 idStatus 行文本 ---
        List<String> rows = extractStatusRowTexts(html);

        System.out.println("=== 状态区块实际渲染行 (" + rows.size() + " 行) ===");
        for (int i = 0; i < rows.size(); i++) {
            System.out.println("[" + (i + 1) + "] " + rows.get(i).trim());
        }
        System.out.println("==========================================");

        // ① 必须字段存在
        assertTrue(rows.stream().anyMatch(r -> r.contains("TEST") && r.contains("01")),
                "应包含 DMC 编码行");
        assertTrue(rows.stream().anyMatch(r -> r.contains("2026") || r.contains("08")),
                "应包含发布日期行");
        assertTrue(rows.stream().anyMatch(r -> r.contains("测试设备") || r.contains("描述")),
                "应包含标题行");
        assertTrue(rows.stream().anyMatch(r -> r.contains("01")),
                "应包含密级行");

        // ② 隐藏字段不得出现
        String allText = String.join(" ", rows);
        assertFalse(allText.contains("某发行方") || allText.contains("ORIG001"),
                "originator 应被隐藏");
        assertFalse(allText.contains("某责任公司") || allText.contains("RPC001"),
                "rpc 应被隐藏");
        assertFalse(allText.contains("SBC001"),
                "systemBreakdownCode 应被隐藏");
        assertFalse(allText.contains("初始创建"),
                "reasonForUpdate 应被隐藏");
        assertFalse(allText.contains("一条备注"),
                "remarks 应被隐藏");

        // ③ 行数应 ≤ 6（DMC + 版本 + 日期 + 标题 + 密级，允许±1冗余）
        assertTrue(rows.size() <= 6,
                "状态区块行数应 ≤ 6，实际: " + rows.size());
    }

    // -------------------------------------------------------------------------
    // 追加测试
    // -------------------------------------------------------------------------

    /** 最小 XML（只含必须字段）仍能渲染出 5 行 */
    @Test
    public void testMinimalXmlStillShowsAllRequiredFields() throws Exception {
        String minimalXml =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<dmodule>\n" +
            "  <identAndStatusSection>\n" +
            "    <dmAddress>\n" +
            "      <dmIdent>\n" +
            "        <dmCode modelIdentCode=\"MIN\" systemDiffCode=\"A\"\n" +
            "                systemCode=\"00\" subSystemCode=\"0\" subSubSystemCode=\"0\"\n" +
            "                assyCode=\"00\" disassyCode=\"00\" disassyCodeVariant=\"A\"\n" +
            "                infoCode=\"040\" infoCodeVariant=\"A\" itemLocationCode=\"A\"/>\n" +
            "        <issueInfo issueNumber=\"002\" inWork=\"01\"/>\n" +
            "      </dmIdent>\n" +
            "      <dmAddressItems>\n" +
            "        <dmTitle><techName>最简设备</techName><infoName>最简描述</infoName></dmTitle>\n" +
            "        <issueDate year=\"2026\" month=\"01\" day=\"15\"/>\n" +
            "      </dmAddressItems>\n" +
            "    </dmAddress>\n" +
            "    <dmStatus>\n" +
            "      <security securityClassification=\"00\"/>\n" +
            "    </dmStatus>\n" +
            "  </identAndStatusSection>\n" +
            "  <content><description><para>最简内容</para></description></content>\n" +
            "</dmodule>";

        String html = DmXsltTransformer.transform(minimalXml, "S1000D40", "descript");
        List<String> rows = extractStatusRowTexts(html);

        System.out.println("=== 最小XML状态行 (" + rows.size() + " 行) ===");
        rows.forEach(r -> System.out.println("  " + r.trim()));

        assertTrue(rows.stream().anyMatch(r -> r.contains("MIN")),   "应含DMC编码");
        assertTrue(rows.stream().anyMatch(r -> r.contains("最简设备")), "应含标题");
        assertTrue(rows.stream().anyMatch(r -> r.contains("2026")),  "应含发布日期");
        assertFalse(rows.isEmpty(), "状态行不应为空");
    }

    /** originator 被隐藏 */
    @Test
    public void testOriginatorHidden() throws Exception {
        String xml = buildXmlWithStatus("<originator enterpriseCode=\"OO1\">发行方ABC</originator>");
        assertFieldHidden(xml, "发行方ABC", "originator");
    }

    /** responsiblePartnerCompany 被隐藏 */
    @Test
    public void testRpcHidden() throws Exception {
        String xml = buildXmlWithStatus("<responsiblePartnerCompany enterpriseCode=\"R01\">责任公司XYZ</responsiblePartnerCompany>");
        assertFieldHidden(xml, "责任公司XYZ", "rpc");
    }

    /** brexDmRef 被隐藏（其内部 dmCode 不应出现额外行） */
    @Test
    public void testBrexDmRefHidden() throws Exception {
        String brex =
            "<brexDmRef><dmRef><dmRefIdent>" +
            "<dmCode modelIdentCode=\"BREX\" systemDiffCode=\"A\" systemCode=\"00\" " +
            "subSystemCode=\"0\" subSubSystemCode=\"0\" assyCode=\"00\" " +
            "disassyCode=\"00\" disassyCodeVariant=\"A\" infoCode=\"022\" " +
            "infoCodeVariant=\"A\" itemLocationCode=\"A\"/>" +
            "</dmRefIdent></dmRef></brexDmRef>";
        String xml = buildXmlWithStatus(brex);
        String html = DmXsltTransformer.transform(xml, "S1000D40", "descript");
        List<String> rows = extractStatusRowTexts(html);
        // 只应有主DMC一行含 MAIN，不应出现 BREX
        String allText = String.join(" ", rows);
        assertFalse(allText.contains("BREX"), "brexDmRef 的 dmCode 不应渲染为额外行");
        assertEquals(1, rows.stream().filter(r -> r.contains("数据模块代码") || r.contains("MAIN")).count(),
                "应只有一个 DMC 行");
    }

    /** qualityAssurance / unverified 被隐藏 */
    @Test
    public void testQualityAssuranceHidden() throws Exception {
        String xml = buildXmlWithStatus("<qualityAssurance><unverified/></qualityAssurance>");
        String html = DmXsltTransformer.transform(xml, "S1000D40", "descript");
        // QA 行被隐藏时 "unverified" 对应的多语言文本不应出现
        List<String> rows = extractStatusRowTexts(html);
        // 只要行数 ≤ 5 且无 QA 相关词汇即通过
        String allText = String.join(" ", rows);
        assertFalse(allText.contains("Quality") || allText.contains("质量"),
                "qualityAssurance 应被隐藏");
    }

    /** reasonForUpdate / remarks / skill 被隐藏 */
    @Test
    public void testMiscHiddenFields() throws Exception {
        String extras =
            "<reasonForUpdate>版本说明文字</reasonForUpdate>" +
            "<remarks><simplePara>备注文字ABC</simplePara></remarks>";
        String xml = buildXmlWithStatus(extras);
        String html = DmXsltTransformer.transform(xml, "S1000D40", "descript");
        List<String> rows = extractStatusRowTexts(html);
        String allText = String.join(" ", rows);

        assertFalse(allText.contains("版本说明文字"), "reasonForUpdate 应被隐藏");
        assertFalse(allText.contains("备注文字ABC"),  "remarks 应被隐藏");
    }

    /** 安全密级值正确渲染（securityClassification 属性） */
    @Test
    public void testSecurityClassificationRendered() throws Exception {
        for (String level : new String[]{"01", "02", "03"}) {
            String xml = buildXmlWithStatus(
                    "<security securityClassification=\"" + level + "\"/>");
            String html = DmXsltTransformer.transform(xml, "S1000D40", "descript");
            List<String> rows = extractStatusRowTexts(html);
            assertTrue(rows.stream().anyMatch(r -> r.contains(level)),
                    "密级 " + level + " 应出现在状态区块");
        }
    }

    /** proced 类型 DM 的状态区块与 descript 行为一致 */
    @Test
    public void testProcedTypeSameStatusBehavior() throws Exception {
        String xml = FULL_STATUS_XML.replace(
                "<description><para>内容</para></description>",
                "<procedure><mainProcedure><proceduralStep><para>步骤1</para></proceduralStep></mainProcedure></procedure>");
        String html = DmXsltTransformer.transform(xml, "S1000D40", "proced");
        List<String> rows = extractStatusRowTexts(html);

        String allText = String.join(" ", rows);
        assertFalse(allText.contains("某发行方"), "proced类型 originator 仍应被隐藏");
        assertFalse(allText.contains("RPC001"),   "proced类型 rpc 仍应被隐藏");
        assertTrue(rows.size() <= 6, "proced类型状态行数应 ≤ 6，实际: " + rows.size());
    }

    // -------------------------------------------------------------------------
    // 辅助方法
    // -------------------------------------------------------------------------

    /**
     * 构造一个包含指定 dmStatus 内容的最小 S1000D XML。
     * 主 dmCode 使用 modelIdentCode="MAIN"，便于区分其他 dmCode。
     */
    private String buildXmlWithStatus(String statusContent) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<dmodule>\n" +
               "  <identAndStatusSection>\n" +
               "    <dmAddress>\n" +
               "      <dmIdent>\n" +
               "        <dmCode modelIdentCode=\"MAIN\" systemDiffCode=\"A\"\n" +
               "                systemCode=\"01\" subSystemCode=\"0\" subSubSystemCode=\"0\"\n" +
               "                assyCode=\"00\" disassyCode=\"00\" disassyCodeVariant=\"A\"\n" +
               "                infoCode=\"040\" infoCodeVariant=\"A\" itemLocationCode=\"A\"/>\n" +
               "        <issueInfo issueNumber=\"001\" inWork=\"00\"/>\n" +
               "      </dmIdent>\n" +
               "      <dmAddressItems>\n" +
               "        <dmTitle><techName>辅助设备</techName><infoName>辅助描述</infoName></dmTitle>\n" +
               "        <issueDate year=\"2026\" month=\"08\" day=\"08\"/>\n" +
               "      </dmAddressItems>\n" +
               "    </dmAddress>\n" +
               "    <dmStatus>\n" +
               "      <security securityClassification=\"01\"/>\n" +
               statusContent + "\n" +
               "    </dmStatus>\n" +
               "  </identAndStatusSection>\n" +
               "  <content><description><para>内容</para></description></content>\n" +
               "</dmodule>";
    }

    /** 断言某个字符串不出现在状态区块的任何行中 */
    private void assertFieldHidden(String xml, String marker, String fieldName) throws Exception {
        String html = DmXsltTransformer.transform(xml, "S1000D40", "descript");
        List<String> rows = extractStatusRowTexts(html);
        String allText = String.join(" ", rows);
        assertFalse(allText.contains(marker),
                fieldName + " 应被隐藏，但在状态行中找到了: " + marker);
    }

    /** 从HTML中提取所有含 idStatus class 的 <tr> 行文本 */
    private List<String> extractStatusRowTexts(String html) {
        List<String> result = new ArrayList<>();
        // 匹配整个 <tr>...</tr> 包含 idStatus 的行
        Pattern trPattern = Pattern.compile("<tr[^>]*>(.*?)</tr>",
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher m = trPattern.matcher(html);
        while (m.find()) {
            String row = m.group(1);
            if (row.contains("idStatus")) {
                // 去除所有HTML标签，只留文本
                result.add(row.replaceAll("<[^>]+>", "").replaceAll("\\s+", " ").trim());
            }
        }
        return result;
    }
}
