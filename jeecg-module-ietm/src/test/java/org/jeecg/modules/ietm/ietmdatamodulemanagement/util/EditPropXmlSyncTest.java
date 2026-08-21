package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDataModule;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * editProp XML 同步专项测试
 * <p>验证修改 techName/infoName 后，syncDmIdentToXml 能否正确同步到 XML 内部节点</p>
 */
public class EditPropXmlSyncTest {

    /**
     * 测试场景：用户通过"编辑属性"弹框修改信息名称，关系列已更新为新值，
     * 调用 syncDmIdentToXml 应同步到 &lt;infoName&gt; 节点
     */
    @Test
    public void testSyncTitleAfterEditProp_InfoName() {
        // 原始 XML（infoName = "旧信息名称"）
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
            "      <dmAddressItems>\n" +
            "        <dmTitle>\n" +
            "          <techName>旧技术名称</techName>\n" +
            "          <infoName>旧信息名称</infoName>\n" +
            "        </dmTitle>\n" +
            "      </dmAddressItems>\n" +
            "    </dmAddress>\n" +
            "  </identAndStatusSection>\n" +
            "</dmodule>";

        // 模拟 editProp 更新后的实体（关系列已更新）
        IetmDataModule dm = new IetmDataModule();
        dm.setSns("ZB1-A-05-10-00-00A");
        dm.setInfoCode("212");
        dm.setInfoCodeVariant("A");
        dm.setIetmLocationCode("A");
        dm.setIssueNo("001");
        dm.setInWork("00");
        dm.setLanguageIsoCode("zh");
        dm.setCountryIsoCode("CN");
        dm.setTechName("新技术名称");  // ← 用户修改
        dm.setInfoName("新信息名称");  // ← 用户修改

        // 执行同步（模拟 syncTitleToXml 内部调用）
        String syncedXml = DmXmlHelper.syncDmIdentToXml(originalXml, dm);

        // 验证结果
        assertNotNull("同步后的 XML 不应为 null", syncedXml);
        assertTrue("应包含新的 techName", syncedXml.contains("<techName>新技术名称</techName>"));
        assertTrue("应包含新的 infoName", syncedXml.contains("<infoName>新信息名称</infoName>"));
        assertFalse("不应包含旧的 techName", syncedXml.contains("<techName>旧技术名称</techName>"));
        assertFalse("不应包含旧的 infoName", syncedXml.contains("<infoName>旧信息名称</infoName>"));

        System.out.println("=== editProp XML 同步测试通过 ===");
        System.out.println("原始标题: <techName>旧技术名称</techName><infoName>旧信息名称</infoName>");
        System.out.println("同步后标题: <techName>新技术名称</techName><infoName>新信息名称</infoName>");
    }

    /**
     * 测试边界：空值处理（用户清空信息名称）
     */
    @Test
    public void testSyncTitleAfterEditProp_EmptyValue() {
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
            "      <dmAddressItems>\n" +
            "        <dmTitle>\n" +
            "          <techName>技术名称</techName>\n" +
            "          <infoName>信息名称</infoName>\n" +
            "        </dmTitle>\n" +
            "      </dmAddressItems>\n" +
            "    </dmAddress>\n" +
            "  </identAndStatusSection>\n" +
            "</dmodule>";

        // 实体字段为空串（用户清空了输入框）
        IetmDataModule dm = new IetmDataModule();
        dm.setSns("ZB1-A-05-10-00-00A");
        dm.setInfoCode("212");
        dm.setInfoCodeVariant("A");
        dm.setIetmLocationCode("A");
        dm.setIssueNo("001");
        dm.setInWork("00");
        dm.setLanguageIsoCode("zh");
        dm.setCountryIsoCode("CN");
        dm.setTechName("");  // 空值
        dm.setInfoName("");  // 空值

        // 同步
        String syncedXml = DmXmlHelper.syncDmIdentToXml(originalXml, dm);

        // fillTitleAndStatus 内部有 hasText 判断，空值不写
        // 预期：保留原 XML 的标题（防御性设计）
        assertTrue("空值时应保留原 techName", syncedXml.contains("<techName>技术名称</techName>"));
        assertTrue("空值时应保留原 infoName", syncedXml.contains("<infoName>信息名称</infoName>"));

        System.out.println("=== 空值处理测试通过：保留原标题 ===");
    }

    /**
     * 测试边界：null 值处理（字段未赋值）
     */
    @Test
    public void testSyncTitleAfterEditProp_NullValue() {
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
            "      <dmAddressItems>\n" +
            "        <dmTitle>\n" +
            "          <techName>技术名称</techName>\n" +
            "          <infoName>信息名称</infoName>\n" +
            "        </dmTitle>\n" +
            "      </dmAddressItems>\n" +
            "    </dmAddress>\n" +
            "  </identAndStatusSection>\n" +
            "</dmodule>";

        // techName/infoName 为 null（字段未赋值）
        IetmDataModule dm = new IetmDataModule();
        dm.setSns("ZB1-A-05-10-00-00A");
        dm.setInfoCode("212");
        dm.setInfoCodeVariant("A");
        dm.setIetmLocationCode("A");
        dm.setIssueNo("001");
        dm.setInWork("00");
        dm.setLanguageIsoCode("zh");
        dm.setCountryIsoCode("CN");
        // techName/infoName 故意不 set

        // 同步
        String syncedXml = DmXmlHelper.syncDmIdentToXml(originalXml, dm);

        // null 值同样不写，保留原 XML
        assertTrue("null 值时应保留原 techName", syncedXml.contains("<techName>技术名称</techName>"));
        assertTrue("null 值时应保留原 infoName", syncedXml.contains("<infoName>信息名称</infoName>"));

        System.out.println("=== null 值处理测试通过：保留原标题 ===");
    }

    /**
     * 测试：特殊字符 XML 转义
     */
    @Test
    public void testSyncTitleAfterEditProp_XmlEscaping() {
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
            "      <dmAddressItems>\n" +
            "        <dmTitle>\n" +
            "          <techName>旧名称</techName>\n" +
            "          <infoName>旧名称</infoName>\n" +
            "        </dmTitle>\n" +
            "      </dmAddressItems>\n" +
            "    </dmAddress>\n" +
            "  </identAndStatusSection>\n" +
            "</dmodule>";

        // 包含 XML 特殊字符的标题
        IetmDataModule dm = new IetmDataModule();
        dm.setSns("ZB1-A-05-10-00-00A");
        dm.setInfoCode("212");
        dm.setInfoCodeVariant("A");
        dm.setIetmLocationCode("A");
        dm.setIssueNo("001");
        dm.setInWork("00");
        dm.setLanguageIsoCode("zh");
        dm.setCountryIsoCode("CN");
        dm.setTechName("测试<标签>内容");  // 含 < >
        dm.setInfoName("A & B \"引号\" '单引号'");  // 含 & " '

        // 同步
        String syncedXml = DmXmlHelper.syncDmIdentToXml(originalXml, dm);

        // dom4j 的 setText 会自动转义，验证转义后的内容
        assertTrue("应正确转义 <", syncedXml.contains("测试&lt;标签&gt;内容"));
        assertTrue("应正确转义 &", syncedXml.contains("A &amp; B"));
        // " 和 ' 在文本节点中可能不转义，但不破坏 XML 结构
        assertTrue("应包含引号内容", syncedXml.contains("引号"));

        System.out.println("=== XML 转义测试通过 ===");
        System.out.println("转义后 techName: " + syncedXml.substring(
            syncedXml.indexOf("<techName>"), syncedXml.indexOf("</techName>") + 11));
    }
}
