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

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * 引用DM（§14.5）后端核心逻辑单测：
 * 覆盖 buildDmRef（dmCode 从XML提取 / referredFragment BUG修复 / issueInfo/issueDate 条件 / 多条拼接 / 转义）
 * 与 getRef（含id元素收集 / 特例前缀 / 递归 / 空内容）。
 */
public class DmRefBuildTest {

    @Mock
    private IetmDataModuleMapper dataModuleMapper;

    @InjectMocks
    private IetmDmContentServiceImpl service;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    /** 构造一个含 dmCode + 若干带id元素的 DM 内容 */
    private String sampleContent() {
        return "<?xml version=\"1.0\"?>\n" +
               "<dmodule>\n" +
               "  <identAndStatusSection><dmAddress><dmIdent>\n" +
               "    <dmCode modelIdentCode=\"S1000D\" systemDiffCode=\"A\" systemCode=\"04\"" +
               " subSystemCode=\"1\" subSubSystemCode=\"0\" assyCode=\"0301\" disassyCode=\"00\"" +
               " disassyCodeVariant=\"A\" infoCode=\"022\" infoCodeVariant=\"A\" itemLocationCode=\"D\"/>\n" +
               "  </dmIdent></dmAddress></identAndStatusSection>\n" +
               "  <content><description>\n" +
               "    <para id=\"p1\">hello</para>\n" +
               "    <figure id=\"fig1\"><graphic id=\"g001\"/></figure>\n" +
               "    <table id=\"t1\"/>\n" +
               "    <multimediaObject id=\"m001\"/>\n" +
               "    <para>no-id</para>\n" +
               "  </description></content>\n" +
               "</dmodule>";
    }

    private IetmDataModule dm(String id) {
        IetmDataModule dm = new IetmDataModule();
        dm.setId(id);
        dm.setDmContent(sampleContent());
        dm.setDmcCode("DMC-S1000D-A-04-10-0301-00A-022A-D_001-00_zh-CN");
        dm.setIssueNo("001");
        dm.setInWork("00");
        dm.setLanguageIsoCode("zh");
        dm.setCountryIsoCode("CN");
        dm.setTechName("发动机");
        dm.setInfoName("描述");
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.MARCH, 5, 0, 0, 0);
        dm.setIssueDate(cal.getTime());
        return dm;
    }

    // ── buildDmRef ────────────────────────────────────────────────

    @Test
    public void buildDmRef_integral_extractsDmCode_noVersion_noFragment() {
        when(dataModuleMapper.selectById("1")).thenReturn(dm("1"));
        DmRefBuildItemVO item = new DmRefBuildItemVO();
        item.setDmId("1");
        item.setIncludeVersion(false);

        Map<String, Object> r = service.buildDmRef(Collections.singletonList(item));
        assertEquals("success", r.get("flag"));
        String xml = (String) r.get("xml");

        // dmCode 11属性从目标DM XML权威提取
        assertTrue(xml.contains("modelIdentCode=\"S1000D\""));
        assertTrue(xml.contains("itemLocationCode=\"D\""));
        assertTrue(xml.contains("assyCode=\"0301\""));
        // 整体引用：无 referredFragment
        assertFalse(xml.contains("referredFragment"));
        // 不含版本：无 issueInfo / issueDate
        assertFalse(xml.contains("<issueInfo"));
        assertFalse(xml.contains("<issueDate"));
        // language 独立于版本，仍生成
        assertTrue(xml.contains("<language languageIsoCode=\"zh\" countryIsoCode=\"CN\"/>"));
        // dmTitle 生成
        assertTrue(xml.contains("<techName>发动机</techName>"));
        assertTrue(xml.contains("<infoName>描述</infoName>"));
    }

    @Test
    public void buildDmRef_internalRef_referredFragment_bugFixed() {
        when(dataModuleMapper.selectById("1")).thenReturn(dm("1"));
        DmRefBuildItemVO item = new DmRefBuildItemVO();
        item.setDmId("1");
        item.setIncludeVersion(false);
        item.setReferredFragment("p1");   // 旧系统此值恒空，重构须真实写入

        Map<String, Object> r = service.buildDmRef(Collections.singletonList(item));
        String xml = (String) r.get("xml");
        assertTrue("referredFragment 必须携带片段id（旧BUG修复验证）",
                xml.contains("referredFragment=\"p1\""));
    }

    @Test
    public void buildDmRef_includeVersion_addsIssueInfoAndDate() {
        when(dataModuleMapper.selectById("1")).thenReturn(dm("1"));
        DmRefBuildItemVO item = new DmRefBuildItemVO();
        item.setDmId("1");
        item.setIncludeVersion(true);

        Map<String, Object> r = service.buildDmRef(Collections.singletonList(item));
        String xml = (String) r.get("xml");
        assertTrue(xml.contains("<issueInfo issueNumber=\"001\" inWork=\"00\"/>"));
        // 月份 +1 修正、两位补零
        assertTrue(xml.contains("<issueDate year=\"2026\" month=\"03\" day=\"05\"/>"));
    }

    @Test
    public void buildDmRef_multipleItems_concatenated() {
        when(dataModuleMapper.selectById("1")).thenReturn(dm("1"));
        when(dataModuleMapper.selectById("2")).thenReturn(dm("2"));
        DmRefBuildItemVO a = new DmRefBuildItemVO(); a.setDmId("1"); a.setIncludeVersion(false);
        DmRefBuildItemVO b = new DmRefBuildItemVO(); b.setDmId("2"); b.setIncludeVersion(false);

        Map<String, Object> r = service.buildDmRef(java.util.Arrays.asList(a, b));
        String xml = (String) r.get("xml");
        int count = xml.split("<dmRef ", -1).length - 1;
        assertEquals("应生成两个 dmRef", 2, count);
    }

    @Test
    public void buildDmRef_dmWithoutDmCode_skipped() {
        IetmDataModule noCode = new IetmDataModule();
        noCode.setId("9");
        noCode.setDmContent("<dmodule><content><para id=\"x\"/></content></dmodule>");
        when(dataModuleMapper.selectById("9")).thenReturn(noCode);
        DmRefBuildItemVO item = new DmRefBuildItemVO(); item.setDmId("9"); item.setIncludeVersion(false);

        Map<String, Object> r = service.buildDmRef(Collections.singletonList(item));
        assertEquals("success", r.get("flag"));
        assertEquals("无dmCode应被跳过，xml为空", "", r.get("xml"));
    }

    @Test
    public void buildDmRef_emptyItems_failure() {
        Map<String, Object> r = service.buildDmRef(Collections.emptyList());
        assertEquals("failure", r.get("flag"));
    }

    @Test
    public void buildDmRef_escapesSpecialChars() {
        IetmDataModule d = dm("1");
        d.setTechName("A<B&C");
        when(dataModuleMapper.selectById("1")).thenReturn(d);
        DmRefBuildItemVO item = new DmRefBuildItemVO(); item.setDmId("1"); item.setIncludeVersion(false);

        String xml = (String) service.buildDmRef(Collections.singletonList(item)).get("xml");
        assertTrue(xml.contains("A&lt;B&amp;C"));
        assertFalse(xml.contains("A<B&C"));
    }

    // ── getRef ────────────────────────────────────────────────────

    @Test
    public void getRef_collectsIdElements_withSpecialPrefixes() {
        when(dataModuleMapper.selectById("1")).thenReturn(dm("1"));
        Map<String, Object> r = service.getRef("1");
        assertEquals("success", r.get("flag"));
        @SuppressWarnings("unchecked")
        List<String> refs = (List<String>) r.get("refs");

        assertTrue(refs.contains("para%%%p1"));
        assertTrue(refs.contains("figure%%%fig1"));
        assertTrue(refs.contains("table%%%t1"));
        // 特例前缀
        assertTrue(refs.contains("graphic%%%g_g001"));
        assertTrue(refs.contains("multimediaObject%%%m_m001"));
        // 无id元素不收集
        assertFalse(refs.stream().anyMatch(s -> s.startsWith("para%%%no")));
        assertEquals("应恰好收集5个带id元素", 5, refs.size());
    }

    @Test
    public void getRef_emptyContent_returnsEmpty() {
        IetmDataModule empty = new IetmDataModule();
        empty.setId("1");
        empty.setDmContent("");
        when(dataModuleMapper.selectById("1")).thenReturn(empty);
        Map<String, Object> r = service.getRef("1");
        assertEquals("success", r.get("flag"));
        @SuppressWarnings("unchecked")
        List<String> refs = (List<String>) r.get("refs");
        assertTrue(refs.isEmpty());
    }

    @Test
    public void getRef_dmNotFound_failure() {
        when(dataModuleMapper.selectById("404")).thenReturn(null);
        Map<String, Object> r = service.getRef("404");
        assertEquals("failure", r.get("flag"));
    }

    @Test
    public void getRef_graphicWithoutId_notCollected() {
        // 对标老系统：graphic/multimediaObject 无 id 不作为引用片段目标（infoEntityIdent 不收集）
        IetmDataModule d = new IetmDataModule();
        d.setId("5");
        d.setDmContent(
            "<dmodule><content><description>\n" +
            "  <figure><graphic infoEntityIdent=\"ICN-001\"/></figure>\n" +
            "  <multimediaObject infoEntityIdent=\"ICN-002\"/>\n" +
            "  <graphic id=\"g010\" infoEntityIdent=\"ICN-003\"/>\n" +
            "</description></content></dmodule>");
        when(dataModuleMapper.selectById("5")).thenReturn(d);

        Map<String, Object> r = service.getRef("5");
        assertEquals("success", r.get("flag"));
        @SuppressWarnings("unchecked")
        List<String> refs = (List<String>) r.get("refs");

        // 无 id 的 graphic/multimediaObject 不收集
        assertFalse(refs.contains("graphic%%%g_ICN-001"));
        assertFalse(refs.contains("multimediaObject%%%m_ICN-002"));
        // 仅收集带 id 的 graphic（用 id，不用 infoEntityIdent）
        assertTrue(refs.contains("graphic%%%g_g010"));
        assertEquals("仅收集1个带id元素", 1, refs.size());
    }
}
