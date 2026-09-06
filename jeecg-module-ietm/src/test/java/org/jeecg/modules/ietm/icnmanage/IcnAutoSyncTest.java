package org.jeecg.modules.ietm.icnmanage;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.jeecg.modules.ietm.icnmanage.entity.IetmIcnManage;
import org.jeecg.modules.ietm.icnmanage.entity.IetmIcnReference;
import org.jeecg.modules.ietm.icnmanage.mapper.IetmIcnManageMapper;
import org.jeecg.modules.ietm.icnmanage.mapper.IetmIcnReferenceMapper;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDataModule;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.mapper.IetmDataModuleMapper;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.service.IIetmDmContentService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ICN自动同步功能单元测试
 *
 * 测试场景：
 * 1. 首次插入ICN引用
 * 2. 幂等性测试（重复保存）
 * 3. 增量更新（新增ICN引用）
 * 4. 删除引用不删除记录
 * 5. 重复ICN去重
 * 6. 部分ICN不存在
 * 7. XML解析失败回滚
 * 8. graphic和multimedia混合
 * 9. 并发保存测试
 *
 * @author Kiro AI Assistant
 * @since 2026-08-31
 */
@SpringBootTest(classes = org.jeecg.modules.ietm.TestApplication.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("ICN自动同步功能测试")
@Transactional
public class IcnAutoSyncTest {

    @Autowired
    private IIetmDmContentService dmContentService;

    @Autowired
    private IetmDataModuleMapper dataModuleMapper;

    @Autowired
    private IetmIcnManageMapper icnManageMapper;

    @Autowired
    private IetmIcnReferenceMapper icnReferenceMapper;

    private static String testDmId;
    private static String testIcn1Id;
    private static String testIcn2Id;
    private static String testIcn3Id;

    @BeforeAll
    static void setup() {
        System.out.println("========================================");
        System.out.println("  ICN自动同步功能单元测试");
        System.out.println("========================================");
    }

    @BeforeEach
    void prepareTestData() {
        // 1. 创建测试DM
        IetmDataModule dm = new IetmDataModule();
        dm.setDmcCode("TEST-DM-ICN-SYNC-001");
        dm.setTechName("ICN同步测试DM");
        dm.setInfoName("测试信息名");
        dm.setDmType("descript");
        dm.setStatus("1");
        dm.setIsLatest("1");
        dm.setIssueNo("001");
        dm.setInWork("00");
        dm.setCheckoutUser("testuser");  // 签出状态
        dm.setVersion(1);
        dm.setCreateTime(new Date());
        dataModuleMapper.insert(dm);
        testDmId = dm.getId();

        // 2. 创建测试ICN
        IetmIcnManage icn1 = new IetmIcnManage();
        icn1.setIcn("ICN-TEST-SYNC-001");
        icn1.setIcnType("graphic");
        icn1.setIsdeleted("0");
        icn1.setCreateTime(new Date());
        icnManageMapper.insert(icn1);
        testIcn1Id = icn1.getId();

        IetmIcnManage icn2 = new IetmIcnManage();
        icn2.setIcn("ICN-TEST-SYNC-002");
        icn2.setIcnType("multimedia");
        icn2.setIsdeleted("0");
        icn2.setCreateTime(new Date());
        icnManageMapper.insert(icn2);
        testIcn2Id = icn2.getId();

        IetmIcnManage icn3 = new IetmIcnManage();
        icn3.setIcn("ICN-TEST-SYNC-003");
        icn3.setIcnType("graphic");
        icn3.setIsdeleted("0");
        icn3.setCreateTime(new Date());
        icnManageMapper.insert(icn3);
        testIcn3Id = icn3.getId();

        System.out.println("✅ 测试数据准备完成:");
        System.out.println("   DM ID: " + testDmId);
        System.out.println("   ICN1 ID: " + testIcn1Id + " (ICN-TEST-SYNC-001)");
        System.out.println("   ICN2 ID: " + testIcn2Id + " (ICN-TEST-SYNC-002)");
        System.out.println("   ICN3 ID: " + testIcn3Id + " (ICN-TEST-SYNC-003)");
    }

    @AfterEach
    void cleanup() {
        // 清理测试数据
        if (testDmId != null) {
            icnReferenceMapper.delete(
                new LambdaQueryWrapper<IetmIcnReference>()
                    .eq(IetmIcnReference::getDmCode, testDmId)
            );
            dataModuleMapper.deleteById(testDmId);
        }
        if (testIcn1Id != null) {
            icnManageMapper.deleteById(testIcn1Id);
        }
        if (testIcn2Id != null) {
            icnManageMapper.deleteById(testIcn2Id);
        }
        if (testIcn3Id != null) {
            icnManageMapper.deleteById(testIcn3Id);
        }
    }

    // ==================== 测试用例 ====================

    @Test
    @Order(1)
    @DisplayName("TC-01: 首次插入ICN引用 - 应该创建1条记录")
    void testFirstInsert() throws Exception {
        // 准备：XML中引用1个ICN
        String xml = buildTestXml(
            "<graphic infoEntityIdent=\"ICN-TEST-SYNC-001\"/>"
        );

        // 执行：保存DM
        String error = dmContentService.saveContent(testDmId, xml, 1, "testuser");

        // 验证：保存成功
        assertNull(error, "保存应该成功");

        // 验证：ietm_icn_reference表中有1条记录
        List<IetmIcnReference> refs = icnReferenceMapper.selectList(
            new LambdaQueryWrapper<IetmIcnReference>()
                .eq(IetmIcnReference::getDmCode, testDmId)
                .eq(IetmIcnReference::getReferenceType, "ICN_TO_DM")
        );

        assertEquals(1, refs.size(), "应该创建1条引用记录");
        assertEquals(testIcn1Id, refs.get(0).getSourceIcnId(), "ICN ID应该匹配");
        assertEquals("DM保存时自动创建", refs.get(0).getRemark(), "备注应该匹配");

        System.out.println("✅ TC-01通过：首次插入成功");
    }

    @Test
    @Order(2)
    @DisplayName("TC-02: 幂等性测试 - 重复保存不应创建重复记录")
    void testIdempotent() throws Exception {
        // 准备：XML中引用1个ICN
        String xml = buildTestXml(
            "<graphic infoEntityIdent=\"ICN-TEST-SYNC-001\"/>"
        );

        // 执行：保存3次
        dmContentService.saveContent(testDmId, xml, 1, "testuser");

        IetmDataModule dm = dataModuleMapper.selectById(testDmId);
        dmContentService.saveContent(testDmId, xml, dm.getVersion(), "testuser");

        dm = dataModuleMapper.selectById(testDmId);
        dmContentService.saveContent(testDmId, xml, dm.getVersion(), "testuser");

        // 验证：ietm_icn_reference表中只有1条记录
        List<IetmIcnReference> refs = icnReferenceMapper.selectList(
            new LambdaQueryWrapper<IetmIcnReference>()
                .eq(IetmIcnReference::getDmCode, testDmId)
        );

        assertEquals(1, refs.size(), "重复保存不应创建重复记录");

        System.out.println("✅ TC-02通过：幂等性验证成功");
    }

    @Test
    @Order(3)
    @DisplayName("TC-03: 增量更新 - 新增ICN应该创建新记录")
    void testIncrementalUpdate() throws Exception {
        // 第1次：保存1个ICN
        String xml1 = buildTestXml(
            "<graphic infoEntityIdent=\"ICN-TEST-SYNC-001\"/>"
        );
        dmContentService.saveContent(testDmId, xml1, 1, "testuser");

        // 第2次：新增1个ICN
        String xml2 = buildTestXml(
            "<graphic infoEntityIdent=\"ICN-TEST-SYNC-001\"/>" +
            "<graphic infoEntityIdent=\"ICN-TEST-SYNC-002\"/>"
        );
        IetmDataModule dm = dataModuleMapper.selectById(testDmId);
        dmContentService.saveContent(testDmId, xml2, dm.getVersion(), "testuser");

        // 验证：应该有2条记录
        List<IetmIcnReference> refs = icnReferenceMapper.selectList(
            new LambdaQueryWrapper<IetmIcnReference>()
                .eq(IetmIcnReference::getDmCode, testDmId)
        );

        assertEquals(2, refs.size(), "应该有2条引用记录");

        System.out.println("✅ TC-03通过：增量更新成功");
    }

    @Test
    @Order(4)
    @DisplayName("TC-04: 删除引用不删除记录 - 历史记录应该保留")
    void testNoDeleteOnRemove() throws Exception {
        // 第1次：保存1个ICN
        String xml1 = buildTestXml(
            "<graphic infoEntityIdent=\"ICN-TEST-SYNC-001\"/>"
        );
        dmContentService.saveContent(testDmId, xml1, 1, "testuser");

        // 第2次：删除该ICN引用
        String xml2 = buildTestXml("");
        IetmDataModule dm = dataModuleMapper.selectById(testDmId);
        dmContentService.saveContent(testDmId, xml2, dm.getVersion(), "testuser");

        // 验证：记录仍然存在
        List<IetmIcnReference> refs = icnReferenceMapper.selectList(
            new LambdaQueryWrapper<IetmIcnReference>()
                .eq(IetmIcnReference::getDmCode, testDmId)
        );

        assertEquals(1, refs.size(), "删除引用不应删除历史记录");

        System.out.println("✅ TC-04通过：只增不删验证成功");
    }

    @Test
    @Order(5)
    @DisplayName("TC-05: 重复ICN去重 - 同一ICN多次引用只创建1条记录")
    void testDuplicateIcnDeduplication() throws Exception {
        // 准备：XML中多次引用同一个ICN
        String xml = buildTestXml(
            "<graphic infoEntityIdent=\"ICN-TEST-SYNC-001\"/>" +
            "<graphic infoEntityIdent=\"ICN-TEST-SYNC-001\"/>" +
            "<graphic infoEntityIdent=\"ICN-TEST-SYNC-001\"/>"
        );

        // 执行：保存DM
        dmContentService.saveContent(testDmId, xml, 1, "testuser");

        // 验证：只创建1条记录
        List<IetmIcnReference> refs = icnReferenceMapper.selectList(
            new LambdaQueryWrapper<IetmIcnReference>()
                .eq(IetmIcnReference::getDmCode, testDmId)
        );

        assertEquals(1, refs.size(), "重复ICN应该去重");

        System.out.println("✅ TC-05通过：重复ICN去重成功");
    }

    @Test
    @Order(6)
    @DisplayName("TC-06: 部分ICN不存在 - 应该只创建存在的ICN记录")
    void testPartialIcnNotFound() throws Exception {
        // 准备：XML中引用2个ICN，其中1个不存在
        String xml = buildTestXml(
            "<graphic infoEntityIdent=\"ICN-TEST-SYNC-001\"/>" +
            "<graphic infoEntityIdent=\"ICN-NOT-EXIST\"/>"
        );

        // 执行：保存DM
        dmContentService.saveContent(testDmId, xml, 1, "testuser");

        // 验证：只创建存在的ICN记录
        List<IetmIcnReference> refs = icnReferenceMapper.selectList(
            new LambdaQueryWrapper<IetmIcnReference>()
                .eq(IetmIcnReference::getDmCode, testDmId)
        );

        assertEquals(1, refs.size(), "应该只创建存在的ICN记录");
        assertEquals(testIcn1Id, refs.get(0).getSourceIcnId(), "应该是ICN-TEST-SYNC-001");

        System.out.println("✅ TC-06通过：部分ICN不存在处理正确");
    }

    @Test
    @Order(7)
    @DisplayName("TC-07: XML解析失败 - 应该回滚事务")
    void testXmlParseFailureRollback() {
        // 准备：无效的XML
        String invalidXml = "<dmodule><invalid>";

        // 执行 & 验证：应该抛出异常
        Exception exception = assertThrows(Exception.class, () -> {
            dmContentService.saveContent(testDmId, invalidXml, 1, "testuser");
        });

        // 验证：事务回滚，DM内容未更新
        IetmDataModule dm = dataModuleMapper.selectById(testDmId);
        assertNull(dm.getDmContent(), "事务回滚后，DM内容应该保持原状");

        // 验证：没有创建ICN引用记录
        List<IetmIcnReference> refs = icnReferenceMapper.selectList(
            new LambdaQueryWrapper<IetmIcnReference>()
                .eq(IetmIcnReference::getDmCode, testDmId)
        );
        assertEquals(0, refs.size(), "事务回滚后，不应有ICN引用记录");

        System.out.println("✅ TC-07通过：XML解析失败事务回滚成功");
    }

    @Test
    @Order(8)
    @DisplayName("TC-08: graphic和multimedia混合 - 应该都创建记录")
    void testMixedGraphicAndMultimedia() throws Exception {
        // 准备：XML中同时引用graphic和multimedia
        String xml = buildTestXml(
            "<graphic infoEntityIdent=\"ICN-TEST-SYNC-001\"/>" +
            "<multimedia infoEntityIdent=\"ICN-TEST-SYNC-002\"/>"
        );

        // 执行：保存DM
        dmContentService.saveContent(testDmId, xml, 1, "testuser");

        // 验证：创建2条记录
        List<IetmIcnReference> refs = icnReferenceMapper.selectList(
            new LambdaQueryWrapper<IetmIcnReference>()
                .eq(IetmIcnReference::getDmCode, testDmId)
        );

        assertEquals(2, refs.size(), "应该创建2条记录");

        System.out.println("✅ TC-08通过：graphic和multimedia混合处理成功");
    }

    @Test
    @Order(9)
    @DisplayName("TC-09: 大量ICN引用 - 性能测试")
    void testLargeNumberOfIcns() throws Exception {
        // 准备：创建多个测试ICN
        StringBuilder icnXml = new StringBuilder();
        for (int i = 1; i <= 50; i++) {
            IetmIcnManage icn = new IetmIcnManage();
            icn.setIcn("ICN-TEST-BULK-" + String.format("%03d", i));
            icn.setIcnType("graphic");
            icn.setIsdeleted("0");
            icn.setCreateTime(new Date());
            icnManageMapper.insert(icn);

            icnXml.append("<graphic infoEntityIdent=\"ICN-TEST-BULK-")
                  .append(String.format("%03d", i))
                  .append("\"/>");
        }

        String xml = buildTestXml(icnXml.toString());

        // 执行：保存DM并测量时间
        long start = System.currentTimeMillis();
        dmContentService.saveContent(testDmId, xml, 1, "testuser");
        long duration = System.currentTimeMillis() - start;

        // 验证：创建50条记录
        List<IetmIcnReference> refs = icnReferenceMapper.selectList(
            new LambdaQueryWrapper<IetmIcnReference>()
                .eq(IetmIcnReference::getDmCode, testDmId)
        );

        assertEquals(50, refs.size(), "应该创建50条记录");
        assertTrue(duration < 2000, "性能：50个ICN同步应该在2秒内完成，实际：" + duration + "ms");

        System.out.println("✅ TC-09通过：大量ICN引用性能测试通过 (耗时: " + duration + "ms)");

        // 清理
        icnManageMapper.delete(
            new LambdaQueryWrapper<IetmIcnManage>()
                .likeRight(IetmIcnManage::getIcn, "ICN-TEST-BULK-")
        );
    }

    @Test
    @Order(10)
    @DisplayName("TC-10: 空XML - 应该不创建任何记录")
    void testEmptyXml() throws Exception {
        // 准备：空内容的XML
        String xml = buildTestXml("");

        // 执行：保存DM
        dmContentService.saveContent(testDmId, xml, 1, "testuser");

        // 验证：没有创建记录
        List<IetmIcnReference> refs = icnReferenceMapper.selectList(
            new LambdaQueryWrapper<IetmIcnReference>()
                .eq(IetmIcnReference::getDmCode, testDmId)
        );

        assertEquals(0, refs.size(), "空XML不应创建任何记录");

        System.out.println("✅ TC-10通过：空XML处理正确");
    }

    // ==================== 辅助方法 ====================

    /**
     * 构建测试用XML
     */
    private String buildTestXml(String content) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<dmodule>\n" +
               "  <identAndStatusSection>\n" +
               "    <dmAddress>\n" +
               "      <dmIdent>\n" +
               "        <dmCode modelIdentCode=\"TEST\" systemDiffCode=\"A\" systemCode=\"00\" " +
               "subSystemCode=\"0\" subSubSystemCode=\"0\" assyCode=\"00\" disassyCode=\"00\" " +
               "disassyCodeVariant=\"A\" infoCode=\"001\" infoCodeVariant=\"A\" itemLocationCode=\"A\"/>\n" +
               "      </dmIdent>\n" +
               "      <dmAddressItems>\n" +
               "        <issueInfo issueNumber=\"001\" inWork=\"00\"/>\n" +
               "      </dmAddressItems>\n" +
               "    </dmAddress>\n" +
               "  </identAndStatusSection>\n" +
               "  <content>\n" +
               content +
               "  </content>\n" +
               "</dmodule>";
    }

    @AfterAll
    static void summary() {
        System.out.println("========================================");
        System.out.println("  测试完成");
        System.out.println("========================================");
    }
}
