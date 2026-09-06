package org.jeecg.modules.ietm.ietmdatamodulemanagement.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.jeecg.modules.ietm.icnmanage.entity.IetmIcnManage;
import org.jeecg.modules.ietm.icnmanage.entity.IetmIcnReference;
import org.jeecg.modules.ietm.icnmanage.mapper.IetmIcnManageMapper;
import org.jeecg.modules.ietm.icnmanage.mapper.IetmIcnReferenceMapper;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.constants.IetmDataModuleConstants;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDataModule;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.mapper.IetmDataModuleMapper;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.service.IIetmDmContentService;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.service.IIetmDataModuleService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ICN引用防重复机制集成测试
 * <p>
 * 测试目标：验证三层防重复机制的有效性
 * 1. 应用层去重
 * 2. 并发冲突捕获
 * 3. 数据库唯一索引
 * </p>
 *
 * @author IETM Team
 * @since 2026-08-31
 */
@SpringBootTest
@DisplayName("ICN引用防重复机制集成测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IcnReferenceDuplicationPreventionTest {

    @Autowired
    private IIetmDmContentService dmContentService;

    @Autowired
    private IIetmDataModuleService dataModuleService;

    @Autowired
    private IetmDataModuleMapper dataModuleMapper;

    @Autowired
    private IetmIcnManageMapper icnManageMapper;

    @Autowired
    private IetmIcnReferenceMapper icnReferenceMapper;

    private String testDmId;
    private String testIcnId;
    private String testUsername = "test_user";

    @BeforeEach
    void setUp() {
        // 生成测试ID
        testDmId = generateId();
        testIcnId = generateId();
    }

    @AfterEach
    void tearDown() {
        // 清理测试数据
        cleanupTestData();
    }

    // ==================== 第一层：应用层去重测试 ====================

    @Test
    @Order(1)
    @DisplayName("T01: 多次保存同一DM - 应用层去重生效")
    @Transactional
    @Rollback
    void testApplicationLayerDeduplication() throws Exception {
        // Given: 准备测试数据
        setupTestData();

        String xmlContent = buildXmlWithIcn(testIcnId);

        // When: 第一次保存
        String result1 = dmContentService.saveContent(testDmId, xmlContent, 1, testUsername);

        // Then: 第一次保存成功
        assertNull(result1, "第一次保存应成功");

        long count1 = countIcnReferences(testDmId, testIcnId);
        assertEquals(1, count1, "第一次保存应创建1条ICN引用记录");

        // When: 第二次保存（相同内容）
        String result2 = dmContentService.saveContent(testDmId, xmlContent, 2, testUsername);

        // Then: 第二次保存成功，但不增加记录
        assertNull(result2, "第二次保存应成功");

        long count2 = countIcnReferences(testDmId, testIcnId);
        assertEquals(1, count2, "第二次保存不应增加ICN引用记录（应用层去重生效）");
    }

    @Test
    @Order(2)
    @DisplayName("T02: 点击计算引用按钮 - 应用层去重生效")
    @Transactional
    @Rollback
    void testCalculateReferencesDeduplication() throws Exception {
        // Given: 准备测试数据并保存
        setupTestData();
        String xmlContent = buildXmlWithIcn(testIcnId);
        dmContentService.saveContent(testDmId, xmlContent, 1, testUsername);

        long countBefore = countIcnReferences(testDmId, testIcnId);
        assertEquals(1, countBefore, "保存后应有1条ICN引用记录");

        // When: 点击"计算引用"按钮
        Map<String, Object> result = dataModuleService.calculateDmReferences(testDmId);

        // Then: 不应增加重复记录
        assertNotNull(result, "计算引用应返回结果");

        long countAfter = countIcnReferences(testDmId, testIcnId);
        assertEquals(1, countAfter, "计算引用不应增加重复记录（应用层去重生效）");

        // 验证返回的ICN引用数量
        Object icnRefCount = result.get("icnRefCount");
        assertNotNull(icnRefCount, "返回结果应包含icnRefCount");
        assertTrue((Long) icnRefCount >= 1, "ICN引用数量应>=1");
    }

    @Test
    @Order(3)
    @DisplayName("T03: XML中同一ICN被引用多次 - HashSet去重生效")
    @Transactional
    @Rollback
    void testXmlDuplicateIcnDeduplication() throws Exception {
        // Given: 准备测试数据
        setupTestData();

        // XML中同一ICN被引用3次
        String xmlContent =
            "<dmodule>" +
            "  <content>" +
            "    <description>" +
            "      <para>" +
            "        <graphic infoEntityIdent='" + testIcnId + "'/>" +
            "        <symbol infoEntityIdent='" + testIcnId + "'/>" +  // 重复
            "        <multimedia infoEntityIdent='" + testIcnId + "'/>" +  // 重复
            "      </para>" +
            "    </description>" +
            "  </content>" +
            "</dmodule>";

        // When: 保存
        String result = dmContentService.saveContent(testDmId, xmlContent, 1, testUsername);

        // Then: 仅创建1条记录
        assertNull(result, "保存应成功");

        long count = countIcnReferences(testDmId, testIcnId);
        assertEquals(1, count, "XML中重复ICN应仅创建1条记录（HashSet去重生效）");
    }

    // ==================== 第二层：并发冲突捕获测试 ====================

    @Test
    @Order(4)
    @DisplayName("T04: 并发保存同一DM - 异常捕获生效")
    void testConcurrentSaveDeduplication() throws Exception {
        // Given: 准备测试数据
        setupTestData();
        String xmlContent = buildXmlWithIcn(testIcnId);

        int threadCount = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // When: 5个线程并发保存
        for (int i = 0; i < threadCount; i++) {
            final int version = i + 1;
            executor.submit(() -> {
                try {
                    startLatch.await();  // 等待统一开始

                    String result = dmContentService.saveContent(testDmId, xmlContent, version, testUsername);

                    if (result == null) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();  // 启动所有线程
        endLatch.await();  // 等待所有线程完成
        executor.shutdown();

        // Then: 仅应有1条ICN引用记录
        long count = countIcnReferences(testDmId, testIcnId);
        assertEquals(1, count, "并发保存应仅创建1条ICN引用记录（异常捕获生效）");

        System.out.println("并发测试结果: 成功=" + successCount.get() + ", 失败=" + failureCount.get() + ", ICN引用记录=" + count);
    }

    // ==================== 第三层：数据库唯一索引测试 ====================

    @Test
    @Order(5)
    @DisplayName("T05: 验证唯一索引存在")
    void testUniqueIndexExists() {
        // 查询索引信息（需要根据实际数据库调整SQL）
        // 这里仅演示逻辑，实际需要执行原生SQL

        // 验证：尝试手动插入重复记录应失败
        setupTestData();

        IetmIcnReference ref1 = new IetmIcnReference();
        ref1.setSourceIcnId(testIcnId);
        ref1.setDmCode(testDmId);
        ref1.setReferenceType(IetmDataModuleConstants.REF_TYPE_ICN_TO_DM);
        ref1.setRemark("测试1");
        ref1.setCreateBy(testUsername);
        ref1.setCreateTime(new Date());

        // 第一次插入应成功
        int rows1 = icnReferenceMapper.insert(ref1);
        assertEquals(1, rows1, "第一次插入应成功");

        // 第二次插入相同记录应失败
        IetmIcnReference ref2 = new IetmIcnReference();
        ref2.setSourceIcnId(testIcnId);
        ref2.setDmCode(testDmId);
        ref2.setReferenceType(IetmDataModuleConstants.REF_TYPE_ICN_TO_DM);
        ref2.setRemark("测试2");
        ref2.setCreateBy(testUsername);
        ref2.setCreateTime(new Date());

        // 第二次插入应抛出 DuplicateKeyException
        assertThrows(org.springframework.dao.DuplicateKeyException.class, () -> {
            icnReferenceMapper.insert(ref2);
        }, "插入重复记录应抛出DuplicateKeyException（唯一索引生效）");
    }

    @Test
    @Order(6)
    @DisplayName("T06: 不同DM引用同一ICN - 允许")
    @Transactional
    @Rollback
    void testDifferentDmSameIcn() throws Exception {
        // Given: 准备测试数据
        setupTestData();
        String anotherDmId = generateId();
        setupTestDm(anotherDmId);

        String xmlContent = buildXmlWithIcn(testIcnId);

        // When: 两个DM引用同一ICN
        dmContentService.saveContent(testDmId, xmlContent, 1, testUsername);
        dmContentService.saveContent(anotherDmId, xmlContent, 1, testUsername);

        // Then: 应创建2条记录
        long count1 = countIcnReferences(testDmId, testIcnId);
        long count2 = countIcnReferences(anotherDmId, testIcnId);

        assertEquals(1, count1, "DM1应有1条ICN引用记录");
        assertEquals(1, count2, "DM2应有1条ICN引用记录");

        // 总计2条
        long totalCount = icnReferenceMapper.selectCount(
            new LambdaQueryWrapper<IetmIcnReference>()
                .eq(IetmIcnReference::getSourceIcnId, testIcnId)
        );
        assertEquals(2, totalCount, "同一ICN被两个DM引用应创建2条记录");
    }

    @Test
    @Order(7)
    @DisplayName("T07: 同一DM引用不同ICN - 允许")
    @Transactional
    @Rollback
    void testSameDmDifferentIcn() throws Exception {
        // Given: 准备测试数据
        setupTestData();
        String anotherIcnId = generateId();
        setupTestIcn(anotherIcnId, "ICN-ANOTHER-001");

        String xmlContent =
            "<dmodule>" +
            "  <content>" +
            "    <description>" +
            "      <para>" +
            "        <graphic infoEntityIdent='ICN-TEST-001'/>" +
            "        <graphic infoEntityIdent='ICN-ANOTHER-001'/>" +
            "      </para>" +
            "    </description>" +
            "  </content>" +
            "</dmodule>";

        // When: 保存
        String result = dmContentService.saveContent(testDmId, xmlContent, 1, testUsername);

        // Then: 应创建2条记录
        assertNull(result, "保存应成功");

        long count = icnReferenceMapper.selectCount(
            new LambdaQueryWrapper<IetmIcnReference>()
                .eq(IetmIcnReference::getDmCode, testDmId)
        );
        assertEquals(2, count, "同一DM引用两个不同ICN应创建2条记录");
    }

    // ==================== 综合场景测试 ====================

    @Test
    @Order(8)
    @DisplayName("T08: 综合场景 - 保存→计算引用→再保存")
    @Transactional
    @Rollback
    void testComprehensiveScenario() throws Exception {
        // Given: 准备测试数据
        setupTestData();
        String xmlContent = buildXmlWithIcn(testIcnId);

        // 场景1: 保存DM
        dmContentService.saveContent(testDmId, xmlContent, 1, testUsername);
        long count1 = countIcnReferences(testDmId, testIcnId);
        assertEquals(1, count1, "第一次保存应创建1条记录");

        // 场景2: 点击"计算引用"
        dataModuleService.calculateDmReferences(testDmId);
        long count2 = countIcnReferences(testDmId, testIcnId);
        assertEquals(1, count2, "计算引用不应增加记录");

        // 场景3: 再次保存
        dmContentService.saveContent(testDmId, xmlContent, 2, testUsername);
        long count3 = countIcnReferences(testDmId, testIcnId);
        assertEquals(1, count3, "再次保存不应增加记录");

        // 场景4: 再次计算引用
        dataModuleService.calculateDmReferences(testDmId);
        long count4 = countIcnReferences(testDmId, testIcnId);
        assertEquals(1, count4, "再次计算引用不应增加记录");

        // 最终验证
        assertEquals(1, count4, "综合场景下应始终保持1条ICN引用记录");
    }

    @Test
    @Order(9)
    @DisplayName("T09: 性能测试 - 100个ICN引用")
    @Transactional
    @Rollback
    void testPerformanceWith100Icns() throws Exception {
        // Given: 准备100个ICN
        setupTestData();
        int icnCount = 100;

        StringBuilder xmlBuilder = new StringBuilder();
        xmlBuilder.append("<dmodule><content><description><para>");

        for (int i = 1; i <= icnCount; i++) {
            String icnId = generateId();
            String icnCode = "ICN-PERF-" + String.format("%03d", i);
            setupTestIcn(icnId, icnCode);
            xmlBuilder.append("<graphic infoEntityIdent='").append(icnCode).append("'/>");
        }

        xmlBuilder.append("</para></description></content></dmodule>");

        // When: 保存
        long startTime = System.currentTimeMillis();
        String result = dmContentService.saveContent(testDmId, xmlBuilder.toString(), 1, testUsername);
        long elapsed = System.currentTimeMillis() - startTime;

        // Then: 验证
        assertNull(result, "保存应成功");

        long count = icnReferenceMapper.selectCount(
            new LambdaQueryWrapper<IetmIcnReference>()
                .eq(IetmIcnReference::getDmCode, testDmId)
        );
        assertEquals(icnCount, count, "应创建" + icnCount + "条ICN引用记录");

        System.out.println("性能测试: " + icnCount + "个ICN引用, 耗时: " + elapsed + "ms");
        assertTrue(elapsed < 5000, "100个ICN引用的保存应在5秒内完成");
    }

    @Test
    @Order(10)
    @DisplayName("T10: 边界条件 - ICN不存在")
    @Transactional
    @Rollback
    void testIcnNotExist() throws Exception {
        // Given: 准备DM但不创建ICN
        setupTestDm(testDmId);

        String xmlContent =
            "<dmodule>" +
            "  <content>" +
            "    <description>" +
            "      <para>" +
            "        <graphic infoEntityIdent='ICN-NOT-EXIST'/>" +
            "      </para>" +
            "    </description>" +
            "  </content>" +
            "</dmodule>";

        // When: 保存
        String result = dmContentService.saveContent(testDmId, xmlContent, 1, testUsername);

        // Then: 保存成功，但不创建ICN引用记录
        assertNull(result, "保存应成功（ICN不存在不影响保存）");

        long count = icnReferenceMapper.selectCount(
            new LambdaQueryWrapper<IetmIcnReference>()
                .eq(IetmIcnReference::getDmCode, testDmId)
        );
        assertEquals(0, count, "ICN不存在时不应创建引用记录");
    }

    // ==================== 辅助方法 ====================

    private void setupTestData() {
        setupTestDm(testDmId);
        setupTestIcn(testIcnId, "ICN-TEST-001");
    }

    private void setupTestDm(String dmId) {
        IetmDataModule dm = new IetmDataModule();
        dm.setId(dmId);
        dm.setDmcCode("DMC-TEST-001");
        dm.setTechName("测试DM");
        dm.setInfoName("测试");
        dm.setCheckoutUser(testUsername);  // 签出给测试用户
        dm.setVersion(1);
        dm.setCreateBy(testUsername);
        dm.setCreateTime(new Date());
        dataModuleMapper.insert(dm);
    }

    private void setupTestIcn(String icnId, String icnCode) {
        IetmIcnManage icn = new IetmIcnManage();
        icn.setId(icnId);
        icn.setIcn(icnCode);
        icn.setIcnType("graphic");  // 设置ICN类型
        icn.setIsdeleted(IetmDataModuleConstants.ISDELETED_NO);
        icn.setCreateBy(testUsername);
        icn.setCreateTime(new Date());
        icnManageMapper.insert(icn);
    }

    private String buildXmlWithIcn(String icnCode) {
        return "<dmodule>" +
               "  <content>" +
               "    <description>" +
               "      <para>" +
               "        <graphic infoEntityIdent='ICN-TEST-001'/>" +
               "      </para>" +
               "    </description>" +
               "  </content>" +
               "</dmodule>";
    }

    private long countIcnReferences(String dmId, String icnId) {
        return icnReferenceMapper.selectCount(
            new LambdaQueryWrapper<IetmIcnReference>()
                .eq(IetmIcnReference::getSourceIcnId, icnId)
                .eq(IetmIcnReference::getDmCode, dmId)
                .eq(IetmIcnReference::getReferenceType, IetmDataModuleConstants.REF_TYPE_ICN_TO_DM)
        );
    }

    private String generateId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 19);
    }

    private void cleanupTestData() {
        // 清理ICN引用记录
        icnReferenceMapper.delete(
            new LambdaQueryWrapper<IetmIcnReference>()
                .eq(IetmIcnReference::getCreateBy, testUsername)
        );

        // 清理DM记录
        dataModuleMapper.delete(
            new LambdaQueryWrapper<IetmDataModule>()
                .eq(IetmDataModule::getCreateBy, testUsername)
        );

        // 清理ICN记录
        icnManageMapper.delete(
            new LambdaQueryWrapper<IetmIcnManage>()
                .eq(IetmIcnManage::getCreateBy, testUsername)
        );
    }
}
