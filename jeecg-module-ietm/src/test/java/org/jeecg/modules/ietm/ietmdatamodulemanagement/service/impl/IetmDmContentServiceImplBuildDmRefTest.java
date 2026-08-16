package org.jeecg.modules.ietm.ietmdatamodulemanagement.service.impl;

import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDataModule;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.mapper.IetmDataModuleMapper;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.DmRefBuildItemVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * buildDmRef 单元测试：验证 dm_content 为空时回退从实体字段构建 dmCode
 */
@ExtendWith(MockitoExtension.class)
class IetmDmContentServiceImplBuildDmRefTest {

    @Mock
    private IetmDataModuleMapper dataModuleMapper;

    @InjectMocks
    private IetmDmContentServiceImpl service;

    /**
     * 场景1：dm_content 含有效 dmCode → 优先提取
     */
    @Test
    void testBuildDmRef_withValidDmContent() {
        IetmDataModule dm = new IetmDataModule();
        dm.setId("dm001");
        dm.setDmContent("<?xml version=\"1.0\"?><dmodule><dmCode modelIdentCode=\"TEST\" systemDiffCode=\"A\" " +
                "systemCode=\"00\" subSystemCode=\"0\" subSubSystemCode=\"0\" assyCode=\"00\" " +
                "disassyCode=\"00\" disassyCodeVariant=\"A\" infoCode=\"040\" infoCodeVariant=\"A\" " +
                "itemLocationCode=\"D\"/></dmodule>");
        dm.setTechName("测试技术名");
        dm.setInfoName("测试信息名");

        when(dataModuleMapper.selectById("dm001")).thenReturn(dm);

        DmRefBuildItemVO item = new DmRefBuildItemVO();
        item.setDmId("dm001");
        item.setIncludeVersion(false);

        Map<String, Object> result = service.buildDmRef(Collections.singletonList(item));

        assertEquals("success", result.get("flag"));
        String xml = (String) result.get("xml");
        assertNotNull(xml);
        assertFalse(xml.isEmpty(), "dm_content 含 dmCode 应生成非空 xml");
        assertTrue(xml.contains("<dmRef"), "应含 dmRef 标签");
        assertTrue(xml.contains("modelIdentCode=\"TEST\""), "应提取 dm_content 中的 dmCode");
        assertTrue(xml.contains("<techName>测试技术名</techName>"), "应含 techName");
    }

    /**
     * 场景2：dm_content 为空 → 回退从实体字段构建 dmCode
     */
    @Test
    void testBuildDmRef_nullDmContent_fallbackToEntity() {
        IetmDataModule dm = new IetmDataModule();
        dm.setId("dm002");
        dm.setDmContent(null);  // dm_content 为空（新建 DM、工作副本原始记录等场景）
        // 实体字段（与 DmXmlHelper.fillDmCode 用的字段一致）
        dm.setSns("ZB001-A-01-0-0-01-01-A");  // SNS 按定长位拆解为 dmCode 各段
        dm.setInfoCode("040");
        dm.setInfoCodeVariant("A");
        dm.setIetmLocationCode("D");
        dm.setSchema("descript");  // resolveModelIdentCode 用（AA 映射）
        dm.setTechName("测试技术名2");
        dm.setInfoName("测试信息名2");

        when(dataModuleMapper.selectById("dm002")).thenReturn(dm);

        DmRefBuildItemVO item = new DmRefBuildItemVO();
        item.setDmId("dm002");
        item.setIncludeVersion(false);

        Map<String, Object> result = service.buildDmRef(Collections.singletonList(item));

        assertEquals("success", result.get("flag"));
        String xml = (String) result.get("xml");
        assertNotNull(xml);
        assertFalse(xml.isEmpty(), "dm_content 为空时应从实体字段构建 dmCode，xml 不应为空");
        assertTrue(xml.contains("<dmRef"), "应含 dmRef 标签");
        assertTrue(xml.contains("infoCode=\"040\""), "应从实体字段构建 infoCode");
        assertTrue(xml.contains("itemLocationCode=\"D\""), "应从实体字段构建 itemLocationCode");
        assertTrue(xml.contains("<techName>测试技术名2</techName>"), "应含 techName");
    }

    /**
     * 场景3：dm_content 为空且 sns/infoCode 缺失 → 跳过该 DM，返回空 xml
     */
    @Test
    void testBuildDmRef_nullDmContent_invalidEntity_skipped() {
        IetmDataModule dm = new IetmDataModule();
        dm.setId("dm003");
        dm.setDmContent(null);
        dm.setSns(null);  // SNS 为空
        dm.setInfoCode(null);  // infoCode 为空

        when(dataModuleMapper.selectById("dm003")).thenReturn(dm);

        DmRefBuildItemVO item = new DmRefBuildItemVO();
        item.setDmId("dm003");
        item.setIncludeVersion(false);

        Map<String, Object> result = service.buildDmRef(Collections.singletonList(item));

        assertEquals("success", result.get("flag"));
        String xml = (String) result.get("xml");
        assertEquals("", xml, "sns/infoCode 缺失时应跳过该 DM，返回空 xml");
    }

    /**
     * 场景4：dm_content 含无效 XML（无 dmCode） → 回退从实体构建
     */
    @Test
    void testBuildDmRef_dmContentWithoutDmCode_fallbackToEntity() {
        IetmDataModule dm = new IetmDataModule();
        dm.setId("dm004");
        dm.setDmContent("<?xml version=\"1.0\"?><dmodule><para>无 dmCode 的 XML</para></dmodule>");
        dm.setSns("ZB002-A-01-0-0-01-01-A");
        dm.setInfoCode("041");
        dm.setInfoCodeVariant("A");
        dm.setIetmLocationCode("A");
        dm.setSchema("descript");

        when(dataModuleMapper.selectById("dm004")).thenReturn(dm);

        DmRefBuildItemVO item = new DmRefBuildItemVO();
        item.setDmId("dm004");
        item.setIncludeVersion(false);

        Map<String, Object> result = service.buildDmRef(Collections.singletonList(item));

        assertEquals("success", result.get("flag"));
        String xml = (String) result.get("xml");
        assertNotNull(xml);
        assertFalse(xml.isEmpty(), "dm_content 不含 dmCode 时应回退从实体构建");
        assertTrue(xml.contains("infoCode=\"041\""), "应从实体字段构建");
    }

    /**
     * 场景5：空参数 → flag=failure
     */
    @Test
    void testBuildDmRef_emptyItems() {
        Map<String, Object> result = service.buildDmRef(Collections.emptyList());
        assertEquals("failure", result.get("flag"));
        assertEquals("参数不能为空", result.get("message"));
    }
}
