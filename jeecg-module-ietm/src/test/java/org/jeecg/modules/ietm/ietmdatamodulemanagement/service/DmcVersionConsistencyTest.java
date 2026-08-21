package org.jeecg.modules.ietm.ietmdatamodulemanagement.service;

import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDataModule;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.service.impl.IetmDataModuleServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DMC 与版本号一致性测试
 *
 * 背景：editProp 方法在升级版本号时曾遗漏重新生成 DMC，
 * 导致历史版本页面出现 DMC 含旧版本号（_001-01_）但字段显示新版本（001-02）的矛盾。
 *
 * 本测试验证 generateDmc() 方法能正确将 issueNo/inWork 反映到 DMC，
 * 从而证明「升级版本号后调用 generateDmc」的修复能消除不一致。
 *
 * 注：generateDmc 为纯逻辑方法（不依赖数据库/Spring bean），可直接实例化测试。
 */
public class DmcVersionConsistencyTest {

    private final IetmDataModuleServiceImpl service = new IetmDataModuleServiceImpl();

    /**
     * 构造一个用户报告场景的 DM：SNS=ZB1-A-02-00-00-00A, infoCode=212, variant=A, 位置码=A
     */
    private IetmDataModule buildDm(String issueNo, String inWork) {
        IetmDataModule dm = new IetmDataModule();
        dm.setSns("ZB1-A-02-00-00-00A");
        dm.setInfoCode("212");
        dm.setInfoCodeVariant("A");
        dm.setIetmLocationCode("A");
        dm.setIssueNo(issueNo);
        dm.setInWork(inWork);
        dm.setLanguageIsoCode("zh");
        dm.setCountryIsoCode("CN");
        return dm;
    }

    // ==================== 核心：版本号正确进入 DMC ====================

    @Test
    @DisplayName("DMC应包含issueNo-inWork段（复现用户报告的DMC格式）")
    public void testDmcContainsVersion() {
        IetmDataModule dm = buildDm("001", "01");
        String dmc = service.generateDmc(dm);

        assertEquals("DMC-ZB1-A-02-00-00-00A-212A-A_001-01_zh-CN", dmc,
                "DMC应精确匹配用户报告的原始格式");
        assertTrue(dmc.contains("_001-01_"), "DMC应包含版本号段 _001-01_");
    }

    @Test
    @DisplayName("inWork升级后DMC同步变化（01→02，复现根因场景）")
    public void testDmcChangesWhenInWorkUpgrades() {
        // 复现用户场景：版本号从 001-01 升级到 001-02
        IetmDataModule oldDm = buildDm("001", "01");
        IetmDataModule newDm = buildDm("001", "02");

        String oldDmc = service.generateDmc(oldDm);
        String newDmc = service.generateDmc(newDm);

        assertNotEquals(oldDmc, newDmc, "版本号变化后DMC必须不同");
        assertTrue(oldDmc.contains("_001-01_"), "旧DMC应含 _001-01_");
        assertTrue(newDmc.contains("_001-02_"), "新DMC应含 _001-02_");
        assertEquals("DMC-ZB1-A-02-00-00-00A-212A-A_001-02_zh-CN", newDmc,
                "升级后DMC应正确反映 001-02");
    }

    @Test
    @DisplayName("issueNo升级后DMC同步变化（发布场景 001→002）")
    public void testDmcChangesWhenIssueNoUpgrades() {
        IetmDataModule oldDm = buildDm("001", "05");
        IetmDataModule newDm = buildDm("002", "00");

        String oldDmc = service.generateDmc(oldDm);
        String newDmc = service.generateDmc(newDm);

        assertTrue(oldDmc.contains("_001-05_"), "旧DMC应含 _001-05_");
        assertTrue(newDmc.contains("_002-00_"), "新DMC应含 _002-00_");
    }

    // ==================== 核心：一致性校验（模拟修复方法逻辑） ====================

    @Test
    @DisplayName("模拟fixInconsistentDmc：检测出DMC滞后于版本号字段")
    public void testDetectInconsistency() {
        // 模拟脏数据：字段是 001-02，但 dmc_code 存的是旧的 001-01
        IetmDataModule dirtyDm = buildDm("001", "02");
        dirtyDm.setDmcCode("DMC-ZB1-A-02-00-00-00A-212A-A_001-01_zh-CN"); // 旧DMC

        String expectedDmc = service.generateDmc(dirtyDm);
        String currentDmc = dirtyDm.getDmcCode();

        // fixInconsistentDmc 的核心判断逻辑
        assertNotEquals(expectedDmc, currentDmc, "应检测出不一致");
        assertEquals("DMC-ZB1-A-02-00-00-00A-212A-A_001-02_zh-CN", expectedDmc,
                "预期DMC应基于当前字段 001-02 生成");
    }

    @Test
    @DisplayName("模拟fixInconsistentDmc：一致数据不应触发更新")
    public void testConsistentDataNotUpdated() {
        IetmDataModule cleanDm = buildDm("001", "02");
        cleanDm.setDmcCode(service.generateDmc(cleanDm)); // DMC已同步

        String expectedDmc = service.generateDmc(cleanDm);
        String currentDmc = cleanDm.getDmcCode();

        assertEquals(expectedDmc, currentDmc, "一致数据不应被判定为需修复");
    }

    // ==================== 边界与稳定性 ====================

    @Test
    @DisplayName("generateDmc幂等性：同一输入多次调用结果一致")
    public void testGenerateDmcIdempotent() {
        IetmDataModule dm = buildDm("003", "12");
        String first = service.generateDmc(dm);
        String second = service.generateDmc(dm);
        String third = service.generateDmc(dm);

        assertEquals(first, second, "两次生成结果应一致");
        assertEquals(second, third, "三次生成结果应一致");
    }

    @Test
    @DisplayName("inWork/issueNo为空时使用默认值（001-00）")
    public void testDefaultVersionWhenNull() {
        IetmDataModule dm = buildDm(null, null);
        String dmc = service.generateDmc(dm);

        assertTrue(dmc.contains("_001-00_"), "版本号为空时应使用默认 001-00，实际=" + dmc);
    }

    @Test
    @DisplayName("无变体时DMC不含变体位（infoCodeVariant为空）")
    public void testDmcWithoutVariant() {
        IetmDataModule dm = buildDm("001", "01");
        dm.setInfoCodeVariant(null);
        String dmc = service.generateDmc(dm);

        // infoCode=212, 无变体 → DMC 段应为 -212-
        assertTrue(dmc.contains("-212-"), "无变体时应为 -212-，实际=" + dmc);
        assertFalse(dmc.contains("-212A-"), "不应含变体A");
    }

    @Test
    @DisplayName("版本号跨99进位：inWork达上限后issueNo进位的DMC")
    public void testVersionRolloverDmc() {
        // 模拟 calculateVersion 处理后的结果：inWork 99 → issueNo+1, inWork=00
        IetmDataModule dm = buildDm("002", "00"); // 进位后的值
        String dmc = service.generateDmc(dm);

        assertTrue(dmc.contains("_002-00_"), "进位后DMC应含 _002-00_，实际=" + dmc);
    }
}
