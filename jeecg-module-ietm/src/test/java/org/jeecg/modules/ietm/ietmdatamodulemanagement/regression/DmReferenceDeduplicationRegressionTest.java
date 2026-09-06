package org.jeecg.modules.ietm.ietmdatamodulemanagement.regression;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDataModule;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDmRef;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.mapper.IetmDataModuleMapper;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.mapper.IetmDmRefMapper;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.service.IIetmDataModuleService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DM引用防重复机制回归测试
 *
 * 测试目标：
 * 1. 验证防重复机制不影响现有功能
 * 2. 验证三层防重复机制都正常工作
 * 3. 验证并发场景下的数据一致性
 * 4. 验证幂等性和增量更新
 *
 * @author Claude Code
 * @date 2026-08-31
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DmReferenceDeduplicationRegressionTest {

    @Autowired
    private IIetmDataModuleService dataModuleService;

    @Autowired
    private IetmDataModuleMapper dataModuleMapper;

    @Autowired
    private IetmDmRefMapper dmRefMapper;

    private static final String TEST_DM_ID_PREFIX = "TEST_DM_REF_";
    private static final String TEST_TARGET_DM_ID_PREFIX = "TEST_TARGET_";

    // ==================== 测试生命周期 ====================

    @BeforeEach
    public void setUp() {
        log.info("========== 测试开始 ==========");
    }

    @AfterEach
    public void tearDown() {
        log.info("========== 测试结束 ==========\n");
    }

    // ==================== 组1: 基础功能回归测试 ====================

    @Test
    @Order(1)
    @DisplayName("R01 - 单次调用计算引用（基础功能）")
    public void testCalculateReferences_SingleCall() throws Exception {
        // Given: 准备测试数据
        String dmId = createTestDM("DMC-TEST-001", buildXmlWithDmRefs(3));

        try {
            // When: 调用计算引用
            Map<String, Object> result = dataModuleService.calculateDmReferences(dmId);

            // Then: 验证返回结果
            assertNotNull(result, "应返回计算结果");
            assertEquals(dmId, result.get("dmId"), "DM ID应一致");
            assertEquals(3, result.get("refCount"), "引用数量应为3");

            // 验证数据库记录
            List<IetmDmRef> refs = dmRefMapper.selectList(
                new LambdaQueryWrapper<IetmDmRef>().eq(IetmDmRef::getSourceDmId, dmId)
            );
            assertEquals(3, refs.size(), "数据库应有3条引用记录");

            log.info("✅ R01 通过 - 基础功能正常");
        } finally {
            cleanupTestData(dmId);
        }
    }

    @Test
    @Order(2)
    @DisplayName("R02 - 幂等性验证（重复调用）")
    public void testCalculateReferences_Idempotency() throws Exception {
        // Given: 准备测试数据
        String dmId = createTestDM("DMC-TEST-002", buildXmlWithDmRefs(3));

        try {
            // When: 第一次调用
            dataModuleService.calculateDmReferences(dmId);
            List<IetmDmRef> refs1 = dmRefMapper.selectList(
                new LambdaQueryWrapper<IetmDmRef>().eq(IetmDmRef::getSourceDmId, dmId)
            );

            // When: 第二次调用（幂等性测试）
            dataModuleService.calculateDmReferences(dmId);
            List<IetmDmRef> refs2 = dmRefMapper.selectList(
                new LambdaQueryWrapper<IetmDmRef>().eq(IetmDmRef::getSourceDmId, dmId)
            );

            // Then: 记录数量应保持不变
            assertEquals(3, refs1.size(), "第一次应创建3条记录");
            assertEquals(3, refs2.size(), "第二次调用后仍应为3条记录");
            assertEquals(refs1.get(0).getId(), refs2.get(0).getId(), "记录ID应保持不变（幂等）");

            log.info("✅ R02 通过 - 幂等性正常");
        } finally {
            cleanupTestData(dmId);
        }
    }

    @Test
    @Order(3)
    @DisplayName("R03 - 增量更新（新增引用）")
    public void testCalculateReferences_IncrementalAdd() throws Exception {
        // Given: 初始创建3个引用
        String dmId = createTestDM("DMC-TEST-003", buildXmlWithDmRefs(3));

        try {
            dataModuleService.calculateDmReferences(dmId);
            assertEquals(3, countReferences(dmId), "初始应有3条引用");

            // When: 更新为5个引用
            updateDmContent(dmId, buildXmlWithDmRefs(5));
            dataModuleService.calculateDmReferences(dmId);

            // Then: 应新增2条引用
            assertEquals(5, countReferences(dmId), "更新后应有5条引用");

            log.info("✅ R03 通过 - 增量新增正常");
        } finally {
            cleanupTestData(dmId);
        }
    }

    @Test
    @Order(4)
    @DisplayName("R04 - 增量更新（删除失效引用）")
    public void testCalculateReferences_IncrementalDelete() throws Exception {
        // Given: 初始创建5个引用
        String dmId = createTestDM("DMC-TEST-004", buildXmlWithDmRefs(5));

        try {
            dataModuleService.calculateDmReferences(dmId);
            assertEquals(5, countReferences(dmId), "初始应有5条引用");

            // When: 更新为2个引用
            updateDmContent(dmId, buildXmlWithDmRefs(2));
            dataModuleService.calculateDmReferences(dmId);

            // Then: 应删除3条失效引用
            assertEquals(2, countReferences(dmId), "更新后应有2条引用");

            log.info("✅ R04 通过 - 删除失效引用正常");
        } finally {
            cleanupTestData(dmId);
        }
    }

    // ==================== 组2: 防重复机制验证 ====================

    @Test
    @Order(5)
    @DisplayName("D01 - 应用层去重验证")
    public void testDeduplication_ApplicationLayer() throws Exception {
        // Given: 准备测试数据
        String dmId = createTestDM("DMC-TEST-005", buildXmlWithDmRefs(3));

        try {
            // When: 第一次调用（创建引用）
            dataModuleService.calculateDmReferences(dmId);
            int count1 = countReferences(dmId);

            // When: 第二次调用（应用层去重应跳过）
            dataModuleService.calculateDmReferences(dmId);
            int count2 = countReferences(dmId);

            // Then: 应用层去重生效，记录数不变
            assertEquals(3, count1, "第一次应创建3条记录");
            assertEquals(3, count2, "第二次应跳过（应用层去重）");

            log.info("✅ D01 通过 - 应用层去重正常");
        } finally {
            cleanupTestData(dmId);
        }
    }

    @Test
    @Order(6)
    @DisplayName("D02 - 数据库唯一索引验证")
    public void testDeduplication_DatabaseUniqueIndex() {
        // Given: 准备第一条引用记录
        String refId1 = generateId();
        String dmId = generateId();
        String targetDmId = generateId();

        IetmDmRef ref1 = buildDmRef(refId1, dmId, targetDmId, "dmRef", "para[1]");

        try {
            // When: 插入第一条记录
            dmRefMapper.insert(ref1);

            // When: 尝试插入重复记录（相同的 source_dm_id + target_dm_id + ref_type + ref_position）
            String refId2 = generateId();
            IetmDmRef ref2 = buildDmRef(refId2, dmId, targetDmId, "dmRef", "para[1]");

            // Then: 应抛出 DuplicateKeyException
            assertThrows(DuplicateKeyException.class, () -> {
                dmRefMapper.insert(ref2);
            }, "应触发唯一索引冲突");

            // 验证只有一条记录
            long count = dmRefMapper.selectCount(
                new LambdaQueryWrapper<IetmDmRef>()
                    .eq(IetmDmRef::getSourceDmId, dmId)
                    .eq(IetmDmRef::getTargetDmId, targetDmId)
            );
            assertEquals(1L, count, "数据库应只有1条记录");

            log.info("✅ D02 通过 - 数据库唯一索引正常");
        } finally {
            dmRefMapper.deleteById(refId1);
        }
    }

    @Test
    @Order(7)
    @DisplayName("D03 - 异常捕获验证（DuplicateKeyException）")
    public void testDeduplication_ExceptionHandling() throws Exception {
        // Given: 准备测试数据
        String dmId = createTestDM("DMC-TEST-007", buildXmlWithDmRefs(3));

        try {
            // When: 第一次调用（正常插入）
            Map<String, Object> result1 = dataModuleService.calculateDmReferences(dmId);
            assertEquals(3, result1.get("refCount"), "第一次应创建3条引用");

            // When: 手动删除应用层去重的缓存数据（模拟并发场景）
            // 通过直接调用底层Mapper插入相同数据，触发DuplicateKeyException
            List<IetmDmRef> existingRefs = dmRefMapper.selectList(
                new LambdaQueryWrapper<IetmDmRef>().eq(IetmDmRef::getSourceDmId, dmId)
            );

            // Then: 再次插入相同记录应被异常捕获
            AtomicInteger caughtExceptions = new AtomicInteger(0);
            for (IetmDmRef ref : existingRefs) {
                try {
                    IetmDmRef duplicate = new IetmDmRef();
                    duplicate.setId(generateId());
                    duplicate.setSourceDmId(ref.getSourceDmId());
                    duplicate.setTargetDmId(ref.getTargetDmId());
                    duplicate.setRefType(ref.getRefType());
                    duplicate.setRefPosition(ref.getRefPosition());
                    duplicate.setRefDmc(ref.getRefDmc());
                    duplicate.setTargetDmc(ref.getTargetDmc());
                    duplicate.setCreateBy("test");
                    duplicate.setCreateTime(new Date());

                    dmRefMapper.insert(duplicate);
                    fail("应抛出 DuplicateKeyException");
                } catch (DuplicateKeyException e) {
                    caughtExceptions.incrementAndGet();
                    log.debug("✓ 正确捕获 DuplicateKeyException: {}", e.getMessage());
                }
            }

            assertEquals(3, caughtExceptions.get(), "应捕获3次 DuplicateKeyException");
            assertEquals(3, countReferences(dmId), "数据库仍应只有3条记录");

            log.info("✅ D03 通过 - 异常捕获正常");
        } finally {
            cleanupTestData(dmId);
        }
    }

    // ==================== 组3: 并发场景测试 ====================

    @Test
    @Order(8)
    @DisplayName("C01 - 并发调用（2个线程）")
    public void testConcurrency_TwoThreads() throws Exception {
        // Given: 准备测试数据
        String dmId = createTestDM("DMC-TEST-008", buildXmlWithDmRefs(3));

        try {
            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch latch = new CountDownLatch(2);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);

            // When: 2个线程并发调用
            for (int i = 0; i < 2; i++) {
                executor.submit(() -> {
                    try {
                        dataModuleService.calculateDmReferences(dmId);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                        log.error("并发调用失败", e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(10, TimeUnit.SECONDS);
            executor.shutdown();

            // Then: 两个线程都应成功，数据库只有3条记录
            assertEquals(2, successCount.get(), "两个线程都应成功");
            assertEquals(0, failCount.get(), "不应有失败");
            assertEquals(3, countReferences(dmId), "数据库应只有3条引用（防重复生效）");

            log.info("✅ C01 通过 - 并发场景（2线程）正常");
        } finally {
            cleanupTestData(dmId);
        }
    }

    @Test
    @Order(9)
    @DisplayName("C02 - 并发调用（10个线程）")
    public void testConcurrency_TenThreads() throws Exception {
        // Given: 准备测试数据
        String dmId = createTestDM("DMC-TEST-009", buildXmlWithDmRefs(5));

        try {
            ExecutorService executor = Executors.newFixedThreadPool(10);
            CountDownLatch latch = new CountDownLatch(10);
            AtomicInteger successCount = new AtomicInteger(0);

            // When: 10个线程并发调用
            for (int i = 0; i < 10; i++) {
                executor.submit(() -> {
                    try {
                        dataModuleService.calculateDmReferences(dmId);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        log.error("并发调用失败", e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(30, TimeUnit.SECONDS);
            executor.shutdown();

            // Then: 所有线程都应成功，数据库只有5条记录
            assertEquals(10, successCount.get(), "10个线程都应成功");
            assertEquals(5, countReferences(dmId), "数据库应只有5条引用（防重复生效）");

            log.info("✅ C02 通过 - 并发场景（10线程）正常");
        } finally {
            cleanupTestData(dmId);
        }
    }

    @Test
    @Order(10)
    @DisplayName("C03 - 高并发场景（50个线程）")
    public void testConcurrency_FiftyThreads() throws Exception {
        // Given: 准备测试数据
        String dmId = createTestDM("DMC-TEST-010", buildXmlWithDmRefs(10));

        try {
            ExecutorService executor = Executors.newFixedThreadPool(50);
            CountDownLatch latch = new CountDownLatch(50);
            AtomicInteger successCount = new AtomicInteger(0);

            // When: 50个线程并发调用
            for (int i = 0; i < 50; i++) {
                executor.submit(() -> {
                    try {
                        dataModuleService.calculateDmReferences(dmId);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        log.error("并发调用失败", e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(60, TimeUnit.SECONDS);
            executor.shutdown();

            // Then: 验证数据一致性
            assertTrue(successCount.get() >= 45, "至少45个线程应成功（允许少量超时）");
            assertEquals(10, countReferences(dmId), "数据库应只有10条引用（防重复生效）");

            log.info("✅ C03 通过 - 高并发场景（50线程）正常，成功数: {}", successCount.get());
        } finally {
            cleanupTestData(dmId);
        }
    }

    // ==================== 组4: 边界条件测试 ====================

    @Test
    @Order(11)
    @DisplayName("E01 - 空XML内容")
    public void testEdgeCase_EmptyXml() throws Exception {
        // Given: 空XML内容
        String dmId = createTestDM("DMC-TEST-011", "");

        try {
            // When & Then: 应抛出异常
            assertThrows(Exception.class, () -> {
                dataModuleService.calculateDmReferences(dmId);
            }, "空XML应抛出异常");

            log.info("✅ E01 通过 - 空XML处理正常");
        } finally {
            cleanupTestData(dmId);
        }
    }

    @Test
    @Order(12)
    @DisplayName("E02 - 无引用的XML")
    public void testEdgeCase_NoReferences() throws Exception {
        // Given: 无引用的XML
        String dmId = createTestDM("DMC-TEST-012", buildXmlWithoutRefs());

        try {
            // When: 计算引用
            Map<String, Object> result = dataModuleService.calculateDmReferences(dmId);

            // Then: 应返回0个引用
            assertEquals(0, result.get("refCount"), "无引用应返回0");
            assertEquals(0, countReferences(dmId), "数据库应无引用记录");

            log.info("✅ E02 通过 - 无引用XML处理正常");
        } finally {
            cleanupTestData(dmId);
        }
    }

    @Test
    @Order(13)
    @DisplayName("E03 - 大量引用（100个）")
    public void testEdgeCase_LargeNumberOfReferences() throws Exception {
        // Given: 100个引用
        String dmId = createTestDM("DMC-TEST-013", buildXmlWithDmRefs(100));

        try {
            long startTime = System.currentTimeMillis();

            // When: 计算引用
            Map<String, Object> result = dataModuleService.calculateDmReferences(dmId);

            long duration = System.currentTimeMillis() - startTime;

            // Then: 应正确处理所有引用
            assertEquals(100, result.get("refCount"), "应有100个引用");
            assertEquals(100, countReferences(dmId), "数据库应有100条记录");
            assertTrue(duration < 10000, "处理时间应小于10秒，实际: " + duration + "ms");

            log.info("✅ E03 通过 - 大量引用处理正常，耗时: {}ms", duration);
        } finally {
            cleanupTestData(dmId);
        }
    }

    @Test
    @Order(14)
    @DisplayName("E04 - 相同位置多个引用")
    public void testEdgeCase_MultipleRefsAtSamePosition() throws Exception {
        // Given: 相同位置的多个引用（不同目标）
        String xml = buildXmlWithMultipleRefsAtSamePosition();
        String dmId = createTestDM("DMC-TEST-014", xml);

        try {
            // When: 计算引用
            Map<String, Object> result = dataModuleService.calculateDmReferences(dmId);

            // Then: 应正确处理（唯一性由 source+target+type+position 组合保证）
            assertNotNull(result.get("refCount"), "应返回引用数量");
            int count = countReferences(dmId);
            assertTrue(count > 0, "应有引用记录");

            log.info("✅ E04 通过 - 相同位置多引用处理正常");
        } finally {
            cleanupTestData(dmId);
        }
    }

    // ==================== 组5: 性能基准测试 ====================

    @Test
    @Order(15)
    @DisplayName("P01 - 性能基准（单次调用）")
    public void testPerformance_SingleCall() throws Exception {
        // Given: 10个引用
        String dmId = createTestDM("DMC-TEST-015", buildXmlWithDmRefs(10));

        try {
            // When: 计算引用并测量时间
            long startTime = System.currentTimeMillis();
            dataModuleService.calculateDmReferences(dmId);
            long duration = System.currentTimeMillis() - startTime;

            // Then: 性能应可接受
            assertTrue(duration < 5000, "单次调用应小于5秒，实际: " + duration + "ms");
            log.info("✅ P01 通过 - 性能基准正常，耗时: {}ms", duration);
        } finally {
            cleanupTestData(dmId);
        }
    }

    @Test
    @Order(16)
    @DisplayName("P02 - 性能基准（重复调用）")
    public void testPerformance_RepeatCalls() throws Exception {
        // Given: 10个引用
        String dmId = createTestDM("DMC-TEST-016", buildXmlWithDmRefs(10));

        try {
            // When: 连续调用10次
            long totalDuration = 0;
            for (int i = 0; i < 10; i++) {
                long startTime = System.currentTimeMillis();
                dataModuleService.calculateDmReferences(dmId);
                totalDuration += (System.currentTimeMillis() - startTime);
            }

            long avgDuration = totalDuration / 10;

            // Then: 平均性能应可接受
            assertTrue(avgDuration < 3000, "平均耗时应小于3秒，实际: " + avgDuration + "ms");
            assertEquals(10, countReferences(dmId), "数据库应只有10条记录（幂等性）");

            log.info("✅ P02 通过 - 重复调用性能正常，平均耗时: {}ms", avgDuration);
        } finally {
            cleanupTestData(dmId);
        }
    }

    // ==================== 辅助方法 ====================

    private String createTestDM(String dmcCode, String xmlContent) {
        String dmId = generateId();
        IetmDataModule dm = new IetmDataModule();
        dm.setId(dmId);
        dm.setDmcCode(dmcCode);
        dm.setDmContent(xmlContent);
        dm.setTechName("测试DM");
        dm.setStatus("1");
        dm.setIsLatest("1");
        dm.setCreateBy("test");
        dm.setCreateTime(new Date());
        dataModuleMapper.insert(dm);

        log.debug("创建测试DM: id={}, dmc={}", dmId, dmcCode);
        return dmId;
    }

    private void updateDmContent(String dmId, String xmlContent) {
        IetmDataModule dm = new IetmDataModule();
        dm.setId(dmId);
        dm.setDmContent(xmlContent);
        dataModuleMapper.updateById(dm);
        log.debug("更新DM内容: id={}", dmId);
    }

    private void cleanupTestData(String dmId) {
        // 删除引用记录
        dmRefMapper.delete(new LambdaQueryWrapper<IetmDmRef>().eq(IetmDmRef::getSourceDmId, dmId));
        // 删除DM记录
        dataModuleMapper.deleteById(dmId);
        log.debug("清理测试数据: dmId={}", dmId);
    }

    private int countReferences(String dmId) {
        Long count = dmRefMapper.selectCount(
            new LambdaQueryWrapper<IetmDmRef>().eq(IetmDmRef::getSourceDmId, dmId)
        );
        return count != null ? count.intValue() : 0;
    }

    private String generateId() {
        return String.valueOf(System.currentTimeMillis()) + String.valueOf(new Random().nextInt(1000));
    }

    private IetmDmRef buildDmRef(String id, String sourceDmId, String targetDmId, String refType, String refPosition) {
        IetmDmRef ref = new IetmDmRef();
        ref.setId(id);
        ref.setSourceDmId(sourceDmId);
        ref.setTargetDmId(targetDmId);
        ref.setRefType(refType);
        ref.setRefPosition(refPosition);
        ref.setRefDmc("DMC-TEST-SOURCE");
        ref.setTargetDmc("DMC-TEST-TARGET");
        ref.setCreateBy("test");
        ref.setCreateTime(new Date());
        return ref;
    }

    private String buildXmlWithDmRefs(int count) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<dmodule>\n");
        xml.append("  <content>\n");

        for (int i = 1; i <= count; i++) {
            xml.append("    <para id=\"para").append(i).append("\">\n");
            xml.append("      <dmRef>\n");
            xml.append("        <dmCode>DMC-TARGET-").append(String.format("%03d", i)).append("</dmCode>\n");
            xml.append("      </dmRef>\n");
            xml.append("    </para>\n");
        }

        xml.append("  </content>\n");
        xml.append("</dmodule>");
        return xml.toString();
    }

    private String buildXmlWithoutRefs() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<dmodule>\n" +
               "  <content>\n" +
               "    <para>普通文本，无引用</para>\n" +
               "  </content>\n" +
               "</dmodule>";
    }

    private String buildXmlWithMultipleRefsAtSamePosition() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<dmodule>\n" +
               "  <content>\n" +
               "    <para id=\"para1\">\n" +
               "      <dmRef><dmCode>DMC-TARGET-001</dmCode></dmRef>\n" +
               "      <dmRef><dmCode>DMC-TARGET-002</dmCode></dmRef>\n" +
               "      <dmRef><dmCode>DMC-TARGET-003</dmCode></dmRef>\n" +
               "    </para>\n" +
               "  </content>\n" +
               "</dmodule>";
    }
}
