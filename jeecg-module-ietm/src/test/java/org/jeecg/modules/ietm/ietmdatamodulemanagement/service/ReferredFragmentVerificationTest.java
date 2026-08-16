package org.jeecg.modules.ietm.ietmdatamodulemanagement.service;

import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDataModule;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.mapper.IetmDataModuleMapper;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.service.impl.IetmDmContentServiceImpl;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.DmRefBuildItemVO;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * referredFragment修复专项验证测试
 * 验证内部引用功能是否正确保存片段ID
 */
public class ReferredFragmentVerificationTest {

    @Mock
    private IetmDataModuleMapper dataModuleMapper;

    @InjectMocks
    private IetmDmContentServiceImpl service;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    private IetmDataModule createTestDm() {
        IetmDataModule dm = new IetmDataModule();
        dm.setId("test-dm-001");
        dm.setSns("ZB1");
        dm.setInfoCode("001");
        dm.setDmContent("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<dmodule>\n" +
            "  <identAndStatusSection>\n" +
            "    <dmAddress>\n" +
            "      <dmIdent>\n" +
            "        <dmCode modelIdentCode=\"ZB1\" systemDiffCode=\"A\" systemCode=\"02\" " +
            "subSystemCode=\"00\" subSubSystemCode=\"00\" assyCode=\"00\" " +
            "disassyCode=\"007\" disassyCodeVariant=\"A\" infoCode=\"001\" " +
            "infoCodeVariant=\"A\" itemLocationCode=\"A\"/>\n" +
            "        <language languageIsoCode=\"zh\" countryIsoCode=\"CN\"/>\n" +
            "        <issueInfo issueNumber=\"001\" inWork=\"03\"/>\n" +
            "      </dmIdent>\n" +
            "      <dmAddressItems>\n" +
            "        <dmTitle><techName>测试DM</techName><infoName>描述</infoName></dmTitle>\n" +
            "      </dmAddressItems>\n" +
            "    </dmAddress>\n" +
            "  </identAndStatusSection>\n" +
            "  <content>\n" +
            "    <description>\n" +
            "      <levelledPara id=\"para-001\"><title>段落1</title><para>测试内容1</para></levelledPara>\n" +
            "      <levelledPara id=\"para-002\"><title>段落2</title><para>测试内容2</para></levelledPara>\n" +
            "    </description>\n" +
            "  </content>\n" +
            "</dmodule>");
        return dm;
    }

    @Test
    public void test01_整体引用_无referredFragment() {
        System.out.println("\n=== 测试1：整体引用（无referredFragment） ===");

        when(dataModuleMapper.selectById("test-dm-001")).thenReturn(createTestDm());

        DmRefBuildItemVO item = new DmRefBuildItemVO();
        item.setDmId("test-dm-001");
        item.setIncludeVersion(false);
        item.setReferredFragment(null);  // 整体引用，无片段ID

        Map<String, Object> result = service.buildDmRef(Collections.singletonList(item));

        assertEquals("success", result.get("flag"));
        String xml = (String) result.get("xml");

        System.out.println("生成的XML：");
        System.out.println(xml);

        // 验证：不应包含referredFragment属性
        assertFalse("整体引用不应包含referredFragment属性",
            xml.contains("referredFragment"));

        // 验证：包含基本元素
        assertTrue("应包含dmRef标签", xml.contains("<dmRef"));
        assertTrue("应包含dmCode", xml.contains("modelIdentCode=\"ZB1\""));

        System.out.println("✅ 测试通过：整体引用不包含referredFragment");
    }

    @Test
    public void test02_内部引用_有referredFragment_para001() {
        System.out.println("\n=== 测试2：内部引用para-001（有referredFragment） ===");

        when(dataModuleMapper.selectById("test-dm-001")).thenReturn(createTestDm());

        DmRefBuildItemVO item = new DmRefBuildItemVO();
        item.setDmId("test-dm-001");
        item.setIncludeVersion(false);
        item.setReferredFragment("para-001");  // 内部引用，指定片段ID

        Map<String, Object> result = service.buildDmRef(Collections.singletonList(item));

        assertEquals("success", result.get("flag"));
        String xml = (String) result.get("xml");

        System.out.println("生成的XML：");
        System.out.println(xml);

        // 验证：必须包含referredFragment属性且值正确
        assertTrue("内部引用必须包含referredFragment属性",
            xml.contains("referredFragment=\"para-001\""));

        // 验证：包含基本元素
        assertTrue("应包含dmRef标签", xml.contains("<dmRef"));
        assertTrue("应包含dmCode", xml.contains("modelIdentCode=\"ZB1\""));

        System.out.println("✅ 测试通过：内部引用包含referredFragment=\"para-001\"");
    }

    @Test
    public void test03_内部引用_有referredFragment_para002() {
        System.out.println("\n=== 测试3：内部引用para-002（有referredFragment） ===");

        when(dataModuleMapper.selectById("test-dm-001")).thenReturn(createTestDm());

        DmRefBuildItemVO item = new DmRefBuildItemVO();
        item.setDmId("test-dm-001");
        item.setIncludeVersion(false);
        item.setReferredFragment("para-002");  // 内部引用，指定不同片段ID

        Map<String, Object> result = service.buildDmRef(Collections.singletonList(item));

        assertEquals("success", result.get("flag"));
        String xml = (String) result.get("xml");

        System.out.println("生成的XML：");
        System.out.println(xml);

        // 验证：必须包含referredFragment属性且值正确
        assertTrue("内部引用必须包含referredFragment属性",
            xml.contains("referredFragment=\"para-002\""));

        // 验证：不应包含错误的片段ID
        assertFalse("不应包含para-001",
            xml.contains("referredFragment=\"para-001\""));

        System.out.println("✅ 测试通过：内部引用包含referredFragment=\"para-002\"");
    }

    @Test
    public void test04_XML特殊字符转义() {
        System.out.println("\n=== 测试4：referredFragment特殊字符转义 ===");

        when(dataModuleMapper.selectById("test-dm-001")).thenReturn(createTestDm());

        DmRefBuildItemVO item = new DmRefBuildItemVO();
        item.setDmId("test-dm-001");
        item.setIncludeVersion(false);
        item.setReferredFragment("para<>&\"'");  // 包含XML特殊字符

        Map<String, Object> result = service.buildDmRef(Collections.singletonList(item));

        assertEquals("success", result.get("flag"));
        String xml = (String) result.get("xml");

        System.out.println("生成的XML：");
        System.out.println(xml);

        // 验证：特殊字符应被正确转义
        assertTrue("应正确转义特殊字符",
            xml.contains("referredFragment=") &&
            (xml.contains("&lt;") || xml.contains("&gt;") ||
             xml.contains("&amp;") || xml.contains("&quot;")));

        // 验证：不应包含未转义的特殊字符（会破坏XML结构）
        // 注意：这个测试验证转义功能存在

        System.out.println("✅ 测试通过：特殊字符正确转义");
    }

    @Test
    public void test05_完整dmRef结构验证() {
        System.out.println("\n=== 测试5：完整dmRef XML结构验证 ===");

        when(dataModuleMapper.selectById("test-dm-001")).thenReturn(createTestDm());

        DmRefBuildItemVO item = new DmRefBuildItemVO();
        item.setDmId("test-dm-001");
        item.setIncludeVersion(false);
        item.setReferredFragment("para-001");

        Map<String, Object> result = service.buildDmRef(Collections.singletonList(item));
        String xml = (String) result.get("xml");

        System.out.println("生成的完整XML：");
        System.out.println(xml);

        // 验证完整结构
        assertTrue("应包含dmRef开始标签", xml.contains("<dmRef"));
        assertTrue("应包含referredFragment属性", xml.contains("referredFragment=\"para-001\""));
        assertTrue("应包含xlink命名空间", xml.contains("xlink:type=\"simple\""));
        assertTrue("应包含dmRefIdent", xml.contains("<dmRefIdent>"));
        assertTrue("应包含dmCode", xml.contains("<dmCode"));
        assertTrue("应包含language", xml.contains("<language"));
        assertTrue("应包含dmRefAddressItems", xml.contains("<dmRefAddressItems>"));
        assertTrue("应包含dmTitle", xml.contains("<dmTitle>"));
        assertTrue("应包含dmRef结束标签", xml.contains("</dmRef>"));

        System.out.println("✅ 测试通过：完整XML结构正确");
    }
}
