package org.jeecg.modules.ietm.ietmdatamodulemanagement.stability;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
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

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ICN引用系统稳定性测试
 * <p>
 * 测试目标：
 * 1. 高并发场景（100/500/1000线程）
 * 2. 长时间运行（持续压测）
 * 3. 内存泄漏检测
 * 4. 数据一致性验证
 * 5. 性能基准测试
 * </p>
 *
 * @author IETM Team
 * @since 2026-08-31
 */
@Slf4j
@SpringBootTest
@DisplayName("ICN引用系统稳定性测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IcnReferenceStabilityTest {

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

    private static final String TEST_USERNAME = "stability_test";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // 测试统计数据
    private static class TestStatistics {
        AtomicLong totalRequests = new AtomicLong(0);
        AtomicLong successCount = new AtomicLong(0);
        AtomicLong failureCount = new AtomicLong(0);
        AtomicLong totalResponseTime = new AtomicLong(0);
        AtomicLong minResponseTime = new AtomicLong(Long.MAX_VALUE);
        AtomicLong maxResponseTime = new AtomicLong(0);
        List<Long> responseTimes = new CopyOnWriteArrayList<>();

        void recordRequest(long responseTime, boolean success) {
            totalRequests.incrementAndGet();
            if (success) {
                successCount.incrementAndGet();
            } else {
                failureCount.incrementAndGet();
            }

            totalResponseTime.addAndGet(responseTime);
            responseTimes.add(responseTime);

            // 更新最小最大响应时间
            updateMin(minResponseTime, responseTime);
            updateMax(maxResponseTime, responseTime);
        }

        private void updateMin(AtomicLong atomicValue, long value) {
            long current;
            do {
                current = atomicValue.get();
                if (value >= current) break;
            } while (!atomicValue.compareAndSet(current, value));
        }

        private void updateMax(AtomicLong atomicValue, long value) {
            long current;
            do {
                current = atomicValue.get();
                if (value <= current) break;
            } while (!atomicValue.compareAndSet(current, value));
        }

        double getAvgResponseTime() {
            long total = totalRequests.get();
            return total == 0 ? 0 : (double) totalResponseTime.get() / total;
        }

        long getP95ResponseTime() {
            if (responseTimes.isEmpty()) return 0;
            List<Long> sorted = new ArrayList<>(responseTimes);
            Collections.sort(sorted);
            int index = (int) (sorted.size() * 0.95);
            return sorted.get(Math.min(index, sorted.size() - 1));
        }

        long getP99ResponseTime() {
            if (responseTimes.isEmpty()) return 0;
            List<Long> sorted = new ArrayList<>(responseTimes);
            Collections.sort(sorted);
            int index = (int) (sorted.size() * 0.99);
            return sorted.get(Math.min(index, sorted.size() - 1));
        }

        String generateReport() {
            StringBuilder report = new StringBuilder();
            report.append("\n========================================\n");
            report.append("测试统计报告\n");
            report.append("========================================\n");
            report.append(String.format("总请求数: %d\n", totalRequests.get()));
            report.append(String.format("成功数: %d (%.2f%%)\n",
                successCount.get(),
                100.0 * successCount.get() / Math.max(1, totalRequests.get())));
            report.append(String.format("失败数: %d (%.2f%%)\n",
                failureCount.get(),
                100.0 * failureCount.get() / Math.max(1, totalRequests.get())));
            report.append(String.format("平均响应时间: %.2f ms\n", getAvgResponseTime()));
            report.append(String.format("最小响应时间: %d ms\n", minResponseTime.get()));
            report.append(String.format("最大响应时间: %d ms\n", maxResponseTime.get()));
            report.append(String.format("P95响应时间: %d ms\n", getP95ResponseTime()));
            report.append(String.format("P99响应时间: %d ms\n", getP99ResponseTime()));
            report.append("========================================\n");
            return report.toString();
        }
    }

    @AfterEach
    void cleanup() {
        log.info("清理测试数据...");
        try {
            icnReferenceMapper.delete(
                new LambdaQueryWrapper<IetmIcnReference>()
                    .eq(IetmIcnReference::getCreateBy, TEST_USERNAME)
            );
            dataModuleMapper.delete(
                new LambdaQueryWrapper<IetmDataModule>()
                    .eq(IetmDataModule::getCreateBy, TEST_USERNAME)
            );
            icnManageMapper.delete(
                new LambdaQueryWrapper<IetmIcnManage>()
                    .eq(IetmIcnManage::getCreateBy, TEST_USERNAME)
            );
        } catch (Exception e) {
            log.error("清理测试数据失败", e);
        }
    }

    // ==================== ST01: 高并发测试 ====================

    @Test
    @Order(1)
    @DisplayName("ST01: 高并发测试 - 100线程")
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    void testHighConcurrency100Threads() throws Exception {
        runConcurrencyTest(100, 10, "ST01-100线程");
    }

    @Test
    @Order(2)
    @DisplayName("ST02: 高并发测试 - 500线程")
    @Timeout(value = 10, unit = TimeUnit.MINUTES)
    void testHighConcurrency500Threads() throws Exception {
        runConcurrencyTest(500, 5, "ST02-500线程");
    }

    @Test
    @Order(3)
    @DisplayName("ST03: 极限并发测试 - 1000线程")
    @Timeout(value = 15, unit = TimeUnit.MINUTES)
    void testHighConcurrency1000Threads() throws Exception {
        runConcurrencyTest(1000, 3, "ST03-1000线程");
    }

    private void runConcurrencyTest(int threadCount, int icnCountPerDm, String testName) throws Exception {
        log.info("\n========================================");
        log.info("开始测试: {}", testName);
        log.info("线程数: {}, 每个DM的ICN数: {}", threadCount, icnCountPerDm);
        log.info("========================================");

        // 准备测试数据
        List<String> icnIds = prepareIcnData(icnCountPerDm);
        String xmlTemplate = buildXmlWithMultipleIcns(icnIds);

        TestStatistics stats = new TestStatistics();
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        long testStartTime = System.currentTimeMillis();

        // 提交任务
        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            executor.submit(() -> {
                try {
                    // 等待统一开始
                    startLatch.await();

                    // 准备DM数据
                    String dmId = generateId();
                    setupTestDm(dmId);

                    // 执行保存操作
                    long requestStart = System.currentTimeMillis();
                    String result = dmContentService.saveContent(dmId, xmlTemplate, 1, TEST_USERNAME);
                    long requestTime = System.currentTimeMillis() - requestStart;

                    boolean success = (result == null);
                    stats.recordRequest(requestTime, success);

                    if (!success) {
                        log.warn("线程{} 保存失败: {}", threadIndex, result);
                    }

                } catch (Exception e) {
                    stats.recordRequest(0, false);
                    log.error("线程{} 执行异常", threadIndex, e);
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // 启动所有线程
        log.info("启动所有线程...");
        startLatch.countDown();

        // 等待所有线程完成
        boolean completed = endLatch.await(10, TimeUnit.MINUTES);
        executor.shutdown();

        long testDuration = System.currentTimeMillis() - testStartTime;

        // 验证结果
        assertTrue(completed, "所有线程应在超时时间内完成");

        // 验证数据一致性
        verifyDataConsistency(icnIds, threadCount);

        // 输出统计报告
        log.info(stats.generateReport());
        log.info("测试总耗时: {} ms", testDuration);
        log.info("吞吐量: {:.2f} 请求/秒",
            1000.0 * stats.totalRequests.get() / testDuration);

        // 性能断言
        double successRate = 100.0 * stats.successCount.get() / stats.totalRequests.get();
        assertTrue(successRate >= 95.0,
            String.format("成功率应>=95%%, 实际: %.2f%%", successRate));

        assertTrue(stats.getAvgResponseTime() < 5000,
            String.format("平均响应时间应<5000ms, 实际: %.2fms", stats.getAvgResponseTime()));
    }

    // ==================== ST04: 持续压测 ====================

    @Test
    @Order(4)
    @DisplayName("ST04: 持续压测 - 5分钟持续负载")
    @Timeout(value = 10, unit = TimeUnit.MINUTES)
    void testSustainedLoad() throws Exception {
        log.info("\n========================================");
        log.info("开始持续压测: 5分钟");
        log.info("========================================");

        int concurrency = 50;  // 并发数
        long durationMinutes = 5;  // 持续时间（分钟）

        List<String> icnIds = prepareIcnData(5);
        String xmlTemplate = buildXmlWithMultipleIcns(icnIds);

        TestStatistics stats = new TestStatistics();
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        AtomicInteger activeThreads = new AtomicInteger(concurrency);

        long testStartTime = System.currentTimeMillis();
        long testEndTime = testStartTime + durationMinutes * 60 * 1000;

        // 启动工作线程
        for (int i = 0; i < concurrency; i++) {
            final int threadIndex = i;
            executor.submit(() -> {
                try {
                    while (System.currentTimeMillis() < testEndTime) {
                        String dmId = generateId();
                        setupTestDm(dmId);

                        long requestStart = System.currentTimeMillis();
                        String result = dmContentService.saveContent(dmId, xmlTemplate, 1, TEST_USERNAME);
                        long requestTime = System.currentTimeMillis() - requestStart;

                        stats.recordRequest(requestTime, result == null);

                        // 短暂休息，避免过度压力
                        Thread.sleep(100);
                    }
                } catch (Exception e) {
                    log.error("线程{} 异常", threadIndex, e);
                } finally {
                    activeThreads.decrementAndGet();
                }
            });
        }

        // 定期输出统计信息
        while (activeThreads.get() > 0) {
            Thread.sleep(30000);  // 每30秒输出一次
            log.info("进行中... 请求数: {}, 成功率: {:.2f}%, 平均响应: {:.2f}ms",
                stats.totalRequests.get(),
                100.0 * stats.successCount.get() / Math.max(1, stats.totalRequests.get()),
                stats.getAvgResponseTime());
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        // 输出最终报告
        log.info(stats.generateReport());

        // 验证稳定性
        double successRate = 100.0 * stats.successCount.get() / stats.totalRequests.get();
        assertTrue(successRate >= 98.0,
            String.format("持续压测成功率应>=98%%, 实际: %.2f%%", successRate));
    }

    // ==================== ST05: 内存泄漏检测 ====================

    @Test
    @Order(5)
    @DisplayName("ST05: 内存泄漏检测 - 10000次操作")
    @Timeout(value = 15, unit = TimeUnit.MINUTES)
    void testMemoryLeak() throws Exception {
        log.info("\n========================================");
        log.info("开始内存泄漏检测");
        log.info("========================================");

        List<String> icnIds = prepareIcnData(10);
        String xmlTemplate = buildXmlWithMultipleIcns(icnIds);

        Runtime runtime = Runtime.getRuntime();
        List<Long> memorySnapshots = new ArrayList<>();

        // 强制GC获取基准内存
        System.gc();
        Thread.sleep(1000);
        long baselineMemory = runtime.totalMemory() - runtime.freeMemory();
        memorySnapshots.add(baselineMemory);
        log.info("基准内存: {} MB", baselineMemory / 1024 / 1024);

        // 执行10000次操作
        int iterations = 10000;
        for (int i = 0; i < iterations; i++) {
            String dmId = generateId();
            setupTestDm(dmId);
            dmContentService.saveContent(dmId, xmlTemplate, 1, TEST_USERNAME);

            // 每1000次记录内存快照
            if ((i + 1) % 1000 == 0) {
                System.gc();
                Thread.sleep(100);
                long currentMemory = runtime.totalMemory() - runtime.freeMemory();
                memorySnapshots.add(currentMemory);
                log.info("迭代{}: 内存 {} MB", i + 1, currentMemory / 1024 / 1024);
            }
        }

        // 分析内存趋势
        long finalMemory = memorySnapshots.get(memorySnapshots.size() - 1);
        long memoryGrowth = finalMemory - baselineMemory;
        double growthPercentage = 100.0 * memoryGrowth / baselineMemory;

        log.info("内存增长: {} MB ({:.2f}%)", memoryGrowth / 1024 / 1024, growthPercentage);

        // 验证没有严重内存泄漏（增长<50%）
        assertTrue(growthPercentage < 50.0,
            String.format("内存增长应<50%%, 实际: %.2f%%", growthPercentage));
    }

    // ==================== ST06: 数据一致性验证 ====================

    @Test
    @Order(6)
    @DisplayName("ST06: 数据一致性验证 - 重复操作1000次")
    @Timeout(value = 10, unit = TimeUnit.MINUTES)
    void testDataConsistency() throws Exception {
        log.info("\n========================================");
        log.info("开始数据一致性验证");
        log.info("========================================");

        List<String> icnIds = prepareIcnData(5);
        String dmId = generateId();
        setupTestDm(dmId);
        String xmlTemplate = buildXmlWithMultipleIcns(icnIds);

        // 重复保存1000次
        int iterations = 1000;
        for (int i = 0; i < iterations; i++) {
            String result = dmContentService.saveContent(dmId, xmlTemplate, i + 1, TEST_USERNAME);
            assertNull(result, "第" + (i + 1) + "次保存应成功");
        }

        // 验证ICN引用记录数量
        long refCount = icnReferenceMapper.selectCount(
            new LambdaQueryWrapper<IetmIcnReference>()
                .eq(IetmIcnReference::getDmCode, dmId)
        );

        assertEquals(icnIds.size(), refCount,
            String.format("应有%d条ICN引用记录（不应重复）", icnIds.size()));

        log.info("数据一致性验证通过: 1000次操作后仅有{}条引用记录", refCount);
    }

    // ==================== ST07: 性能基准测试 ====================

    @Test
    @Order(7)
    @DisplayName("ST07: 性能基准测试")
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    void testPerformanceBenchmark() throws Exception {
        log.info("\n========================================");
        log.info("开始性能基准测试");
        log.info("========================================");

        Map<Integer, Long> benchmarks = new LinkedHashMap<>();

        // 测试不同ICN数量的性能
        int[] icnCounts = {1, 5, 10, 50, 100, 500};

        for (int icnCount : icnCounts) {
            List<String> icnIds = prepareIcnData(icnCount);
            String xmlTemplate = buildXmlWithMultipleIcns(icnIds);
            String dmId = generateId();
            setupTestDm(dmId);

            long startTime = System.currentTimeMillis();
            dmContentService.saveContent(dmId, xmlTemplate, 1, TEST_USERNAME);
            long elapsed = System.currentTimeMillis() - startTime;

            benchmarks.put(icnCount, elapsed);
            log.info("ICN数量: {}, 耗时: {} ms", icnCount, elapsed);
        }

        // 输出基准报告
        log.info("\n性能基准报告:");
        benchmarks.forEach((count, time) -> {
            log.info("  {} ICN → {} ms", count, time);
        });

        // 验证性能符合预期
        assertTrue(benchmarks.get(100) < 5000,
            "100个ICN应在5秒内完成");
    }

    // ==================== 辅助方法 ====================

    private List<String> prepareIcnData(int count) {
        List<String> icnIds = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String icnId = generateId();
            String icnCode = "ICN-STAB-" + String.format("%05d", i);

            IetmIcnManage icn = new IetmIcnManage();
            icn.setId(icnId);
            icn.setIcn(icnCode);
            icn.setIcnType("graphic");
            icn.setIsdeleted(IetmDataModuleConstants.ISDELETED_NO);
            icn.setCreateBy(TEST_USERNAME);
            icn.setCreateTime(new Date());

            icnManageMapper.insert(icn);
            icnIds.add(icnCode);
        }
        return icnIds;
    }

    private void setupTestDm(String dmId) {
        IetmDataModule dm = new IetmDataModule();
        dm.setId(dmId);
        dm.setDmcCode("DMC-STAB-" + System.currentTimeMillis());
        dm.setTechName("稳定性测试DM");
        dm.setInfoName("测试");
        dm.setCheckoutUser(TEST_USERNAME);
        dm.setVersion(1);
        dm.setCreateBy(TEST_USERNAME);
        dm.setCreateTime(new Date());
        dataModuleMapper.insert(dm);
    }

    private String buildXmlWithMultipleIcns(List<String> icnCodes) {
        StringBuilder xml = new StringBuilder();
        xml.append("<dmodule><content><description><para>");
        for (String icnCode : icnCodes) {
            xml.append("<graphic infoEntityIdent='").append(icnCode).append("'/>");
        }
        xml.append("</para></description></content></dmodule>");
        return xml.toString();
    }

    private void verifyDataConsistency(List<String> icnCodes, int expectedDmCount) {
        // 验证每个ICN的引用数量
        for (String icnCode : icnCodes) {
            IetmIcnManage icn = icnManageMapper.selectOne(
                new LambdaQueryWrapper<IetmIcnManage>()
                    .eq(IetmIcnManage::getIcn, icnCode)
            );

            if (icn != null) {
                long refCount = icnReferenceMapper.selectCount(
                    new LambdaQueryWrapper<IetmIcnReference>()
                        .eq(IetmIcnReference::getSourceIcnId, icn.getId())
                        .eq(IetmIcnReference::getCreateBy, TEST_USERNAME)
                );

                log.info("ICN {} 引用数: {} (期望: {})", icnCode, refCount, expectedDmCount);
                assertEquals(expectedDmCount, refCount,
                    String.format("ICN %s 应被%d个DM引用", icnCode, expectedDmCount));
            }
        }
    }

    private String generateId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 19);
    }
}
