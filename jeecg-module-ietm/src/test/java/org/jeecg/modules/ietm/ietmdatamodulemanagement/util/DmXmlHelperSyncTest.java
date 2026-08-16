package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDataModule;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * DmXmlHelper 同步功能测试
 */
public class DmXmlHelperSyncTest {

    /**
     * 测试用例1：验证 syncDmIdentToXml() 方法能正确同步 SNS 到 XML
     */
    @Test
    public void testSyncDmIdent_SNS() {
        // 准备测试数据：原始 XML（SNS 为 ZB1-A-05-10-00-00A）
        String originalXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<dmodule>\n" +
            "  <identAndStatusSection>\n" +
            "    <dmAddress>\n" +
            "      <dmIdent>\n" +
            "        <dmCode modelIdentCode=\"ZB1\" systemDiffCode=\"A\" systemCode=\"05\" " +
            "subSystemCode=\"1\" subSubSystemCode=\"0\" assyCode=\"00\" " +
            "disassyCode=\"00\" disassyCodeVariant=\"A\" infoCode=\"212\" " +
            "infoCodeVariant=\"A\" itemLocationCode=\"A\"/>\n" +
            "        <language languageIsoCode=\"zh\" countryIsoCode=\"CN\"/>\n" +
            "        <issueInfo issueNumber=\"001\" inWork=\"00\"/>\n" +
            "      </dmIdent>\n" +
            "    </dmAddress>\n" +
            "  </identAndStatusSection>\n" +
            "</dmodule>";

        // 准备数据库实体：新 SNS 为 ZB1-A-05-20-00-00A
        IetmDataModule dm = new IetmDataModule();
        dm.setSns("ZB1-A-05-20-00-00A");  // 新 SNS（systemCode=05, subSystemCode=2）
        dm.setInfoCode("212");
        dm.setInfoCodeVariant("A");
        dm.setIetmLocationCode("A");
        dm.setIssueNo("001");
        dm.setInWork("00");
        dm.setLanguageIsoCode("zh");
        dm.setCountryIsoCode("CN");

        // 执行同步
        String syncedXml = DmXmlHelper.syncDmIdentToXml(originalXml, dm);

        // 验证结果
        assertNotNull("同步后的 XML 不应为 null", syncedXml);
        assertTrue("应包含新的 systemCode=\"05\"", syncedXml.contains("systemCode=\"05\""));
        assertTrue("应包含新的 subSystemCode=\"2\"", syncedXml.contains("subSystemCode=\"2\""));
        assertFalse("不应包含旧的 subSystemCode=\"1\"", syncedXml.contains("subSystemCode=\"1\""));

        System.out.println("=== 测试用例1通过：SNS 同步成功 ===");
        System.out.println("原始 XML 片段: " + originalXml.substring(originalXml.indexOf("<dmCode"), originalXml.indexOf("/>", originalXml.indexOf("<dmCode")) + 2));
        System.out.println("同步后 XML 片段: " + syncedXml.substring(syncedXml.indexOf("<dmCode"), syncedXml.indexOf("/>", syncedXml.indexOf("<dmCode")) + 2));
    }

    /**
     * 测试用例2：验证版本号同步
     */
    @Test
    public void testSyncDmIdent_Version() {
        String originalXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<dmodule>\n" +
            "  <identAndStatusSection>\n" +
            "    <dmAddress>\n" +
            "      <dmIdent>\n" +
            "        <dmCode modelIdentCode=\"ZB1\" systemDiffCode=\"A\" systemCode=\"05\" " +
            "subSystemCode=\"1\" subSubSystemCode=\"0\" assyCode=\"00\" " +
            "disassyCode=\"00\" disassyCodeVariant=\"A\" infoCode=\"212\" " +
            "infoCodeVariant=\"A\" itemLocationCode=\"A\"/>\n" +
            "        <language languageIsoCode=\"zh\" countryIsoCode=\"CN\"/>\n" +
            "        <issueInfo issueNumber=\"001\" inWork=\"00\"/>\n" +
            "      </dmIdent>\n" +
            "    </dmAddress>\n" +
            "  </identAndStatusSection>\n" +
            "</dmodule>";

        // 版本号从 001-00 升级到 001-05
        IetmDataModule dm = new IetmDataModule();
        dm.setSns("ZB1-A-05-10-00-00A");
        dm.setInfoCode("212");
        dm.setInfoCodeVariant("A");
        dm.setIetmLocationCode("A");
        dm.setIssueNo("001");
        dm.setInWork("05");  // 升级到 05
        dm.setLanguageIsoCode("zh");
        dm.setCountryIsoCode("CN");

        String syncedXml = DmXmlHelper.syncDmIdentToXml(originalXml, dm);

        assertNotNull(syncedXml);
        assertTrue("应包含新的 inWork=\"05\"", syncedXml.contains("inWork=\"05\""));
        assertFalse("不应包含旧的 inWork=\"00\"", syncedXml.contains("inWork=\"00\""));

        System.out.println("=== 测试用例2通过：版本号同步成功 ===");
        int start = syncedXml.indexOf("<issueInfo");
        int end = syncedXml.indexOf("/>", start) + 2;
        System.out.println("同步后 issueInfo: " + syncedXml.substring(start, end));
    }

    /**
     * 测试用例3：验证语言/国家码同步
     */
    @Test
    public void testSyncDmIdent_Language() {
        String originalXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<dmodule>\n" +
            "  <identAndStatusSection>\n" +
            "    <dmAddress>\n" +
            "      <dmIdent>\n" +
            "        <dmCode modelIdentCode=\"ZB1\" systemDiffCode=\"A\" systemCode=\"05\" " +
            "subSystemCode=\"1\" subSubSystemCode=\"0\" assyCode=\"00\" " +
            "disassyCode=\"00\" disassyCodeVariant=\"A\" infoCode=\"212\" " +
            "infoCodeVariant=\"A\" itemLocationCode=\"A\"/>\n" +
            "        <language languageIsoCode=\"zh\" countryIsoCode=\"CN\"/>\n" +
            "        <issueInfo issueNumber=\"001\" inWork=\"00\"/>\n" +
            "      </dmIdent>\n" +
            "    </dmAddress>\n" +
            "  </identAndStatusSection>\n" +
            "</dmodule>";

        // 语言从 zh-CN 改为 en-US
        IetmDataModule dm = new IetmDataModule();
        dm.setSns("ZB1-A-05-10-00-00A");
        dm.setInfoCode("212");
        dm.setInfoCodeVariant("A");
        dm.setIetmLocationCode("A");
        dm.setIssueNo("001");
        dm.setInWork("00");
        dm.setLanguageIsoCode("en");  // 改为英文
        dm.setCountryIsoCode("US");   // 改为美国

        String syncedXml = DmXmlHelper.syncDmIdentToXml(originalXml, dm);

        assertNotNull(syncedXml);
        assertTrue("应包含新的 languageIsoCode=\"en\"", syncedXml.contains("languageIsoCode=\"en\""));
        assertTrue("应包含新的 countryIsoCode=\"US\"", syncedXml.contains("countryIsoCode=\"US\""));
        assertFalse("不应包含旧的 languageIsoCode=\"zh\"", syncedXml.contains("languageIsoCode=\"zh\""));

        System.out.println("=== 测试用例3通过：语言/国家码同步成功 ===");
        int start = syncedXml.indexOf("<language");
        int end = syncedXml.indexOf("/>", start) + 2;
        System.out.println("同步后 language: " + syncedXml.substring(start, end));
    }

    /**
     * 测试用例4：验证空值处理
     */
    @Test
    public void testSyncDmIdent_NullHandling() {
        String emptyXml = "";
        IetmDataModule dm = new IetmDataModule();
        dm.setSns("ZB1-A-05-10-00-00A");

        // 空 XML 应直接返回
        String result = DmXmlHelper.syncDmIdentToXml(emptyXml, dm);
        assertEquals("空 XML 应原样返回", emptyXml, result);

        // null 实体应返回原 XML
        String xml = "<dmodule></dmodule>";
        result = DmXmlHelper.syncDmIdentToXml(xml, null);
        assertEquals("null 实体应返回原 XML", xml, result);

        System.out.println("=== 测试用例4通过：空值处理正确 ===");
    }

    /**
     * 测试用例5：验证异常处理
     */
    @Test
    public void testSyncDmIdent_InvalidXml() {
        String invalidXml = "这不是有效的XML";
        IetmDataModule dm = new IetmDataModule();
        dm.setSns("ZB1-A-05-10-00-00A");
        dm.setInfoCode("212");
        dm.setInfoCodeVariant("A");

        // 无效 XML 应返回原内容（不抛异常）
        String result = DmXmlHelper.syncDmIdentToXml(invalidXml, dm);
        assertEquals("无效 XML 应返回原内容", invalidXml, result);

        System.out.println("=== 测试用例5通过：异常处理正确 ===");
    }
}
