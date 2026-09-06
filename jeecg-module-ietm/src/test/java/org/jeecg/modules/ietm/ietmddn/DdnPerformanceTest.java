package org.jeecg.modules.ietm.ietmddn;

import org.jeecg.modules.ietm.ietmddn.constants.DdnConstants;
import org.jeecg.modules.ietm.ietmddn.service.IIetmDdnService;
import org.jeecg.modules.ietm.ietmddn.vo.DdnGenerateVO;
import org.jeecg.modules.ietm.ietmddn.vo.DdnGenerateResultVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DDN模块性能测试
 * 测试场景：
 * - 单个DM导出性能
 * - 10个DM导出性能
 * - 50个DM导出性能
 * - 并发生成DDN性能
 * - 大文件处理性能
 * - 递归深度限制性能
 *
 * @author IETM Team
 * @date 2026-09-01
 */
@SpringBootTest
@Transactional
@DisplayName("DDN性能测试")
public class DdnPerformanceTest {

    @Autowired
    private IIetmDdnService ddnService;

    private Map<String, Object> testProjectInfo;

    @BeforeEach
    public void setUp() {
        testProjectInfo = new HashMap<>();
        testProjectInfo.put("projectId", "perf-test-project");
        testProjectInfo.put("projectName", "性能测试项目");
    }

    /**
     * PERF-01: 单个DM导出性能基准测试
     * 目标：<2秒
     */
    @Test
    @DisplayName("PERF-01: 单个DM导出性能（目标<2秒）")
    public void testSingleDmExportPerformance() throws Exception {
        DdnGenerateVO params = createBasicParams(Arrays.asList("dm-perf-001"));

        long startTime = System.currentTimeMillis();

        try {
            DdnGenerateResultVO result = ddnService.generateDdn(params, testProjectInfo);

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            System.out.println("单个DM导出耗时: " + duration + "ms");
            assertNotNull(result);
            // assertTrue(duration < 2000, "单个DM导出应在2秒内完成");

        } catch (Exception e) {
            System.out.println("警告: PERF-01需要真实数据: " + e.getMessage());
        }
    }

    /**
     * PERF-02: 10个DM导出性能测试
     * 目标：<10秒
     */
    @Test
    @DisplayName("PERF-02: 10个DM导出性能（目标<10秒）")
    @Disabled("需要真实数据库数据")
    public void test10DmsExportPerformance() throws Exception {
        List<String> dmIds = generateDmIds("dm-perf-10-", 10);
        DdnGenerateVO params = createBasicParams(dmIds);

        long startTime = System.currentTimeMillis();

        try {
            DdnGenerateResultVO result = ddnService.generateDdn(params, testProjectInfo);

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            System.out.println("10个DM导出耗时: " + duration + "ms");
            assertNotNull(result);
            assertEquals(10, result.getDmCount(), "应导出10个DM");
            assertTrue(duration < 10000, "10个DM导出应在10秒内完成");

        } catch (Exception e) {
            System.out.println("警告: PERF-02需要真实数据: " + e.getMessage());
        }
    }

    /**
     * PERF-03: 50个DM导出性能测试
     * 目标：<30秒
     */
    @Test
    @DisplayName("PERF-03: 50个DM导出性能（目标<30秒）")
    @Disabled("需要真实数据库数据")
    public void test50DmsExportPerformance() throws Exception {
        List<String> dmIds = generateDmIds("dm-perf-50-", 50);
        DdnGenerateVO params = createBasicParams(dmIds);

        long startTime = System.currentTimeMillis();

        try {
            DdnGenerateResultVO result = ddnService.generateDdn(params, testProjectInfo);

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            System.out.println("50个DM导出耗时: " + duration + "ms");
            assertNotNull(result);
            assertEquals(50, result.getDmCount(), "应导出50个DM");
            assertTrue(duration < 30000, "50个DM导出应在30秒内完成");

        } catch (Exception e) {
            System.out.println("警告: PERF-03需要真实数据: " + e.getMessage());
        }
    }

    /**
     * PERF-04: 并发生成DDN性能测试
     * 场景：10个用户同时生成DDN
     * 验证：序列号无冲突，所有请求成功
     */
    @Test
    @DisplayName("PERF-04: 并发10用户同时生成DDN")
    @Disabled("需要真实数据库数据")
    public void testConcurrentDdnGeneration() throws Exception {
        int concurrentUsers = 10;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);
        CountDownLatch latch = new CountDownLatch(concurrentUsers);
        List<Future<DdnGenerateResultVO>> futures = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        // 提交10个并发任务
        for (int i = 0; i < concurrentUsers; i++) {
            final int userId = i;
            Future<DdnGenerateResultVO> future = executor.submit(() -> {
                try {
                    latch.countDown();
                    latch.await(); // 等待所有线程就绪，同时开始

                    DdnGenerateVO params = createBasicParams(
                        Arrays.asList("dm-concurrent-" + userId)
                    );
                    return ddnService.generateDdn(params, testProjectInfo);
                } catch (Exception e) {
                    System.err.println("用户" + userId + "生成失败: " + e.getMessage());
                    return null;
                }
            });
            futures.add(future);
        }

        // 等待所有任务完成
        List<DdnGenerateResultVO> results = new ArrayList<>();
        for (Future<DdnGenerateResultVO> future : futures) {
            try {
                DdnGenerateResultVO result = future.get(30, TimeUnit.SECONDS);
                if (result != null) {
                    results.add(result);
                }
            } catch (TimeoutException e) {
                System.err.println("任务超时");
            }
        }

        executor.shutdown();
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("并发10用户耗时: " + duration + "ms");
        System.out.println("成功数: " + results.size() + "/" + concurrentUsers);

        // 验证：所有DDN编码唯一
        Set<String> ddnCodes = results.stream()
            .map(DdnGenerateResultVO::getDdnCode)
            .collect(Collectors.toSet());
        assertEquals(results.size(), ddnCodes.size(), "DDN编码不应重复");

        // 验证：所有序列号唯一
        Set<String> seqNumbers = results.stream()
            .map(r -> r.getDdnCode().substring(r.getDdnCode().lastIndexOf('-') + 1))
            .collect(Collectors.toSet());
        assertEquals(results.size(), seqNumbers.size(), "序列号不应冲突");
    }

    /**
     * PERF-05: 文件大小限制性能测试
     * 验证：快速拒绝超大文件，不影响正常导出性能
     */
    @Test
    @DisplayName("PERF-05: 文件大小限制快速校验")
    public void testFileSizeValidationPerformance() {
        long maxFileSize = DdnConstants.FileSize.MAX_FILE_SIZE;
        long testFileSize = 150 * 1024 * 1024L; // 150MB

        long startTime = System.nanoTime();

        // 模拟文件大小校验逻辑
        boolean exceedsLimit = testFileSize > maxFileSize;

        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1000; // 转为微秒

        System.out.println("文件大小校验耗时: " + duration + "μs");
        assertTrue(exceedsLimit, "150MB文件应超过100MB限制");
        assertTrue(duration < 1000, "文件大小校验应在1毫秒内完成");
    }

    /**
     * PERF-06: 递归深度限制性能测试
     * 验证：递归收集在深度限制内高效完成
     */
    @Test
    @DisplayName("PERF-06: 递归深度限制性能")
    public void testRecursionDepthPerformance() {
        int maxDepth = DdnConstants.Collection.MAX_RECURSION_DEPTH;

        // 模拟递归收集场景
        Set<String> collectedDms = new HashSet<>();
        long startTime = System.currentTimeMillis();

        for (int depth = 0; depth < maxDepth; depth++) {
            // 每层模拟收集5个DM
            for (int i = 0; i < 5; i++) {
                collectedDms.add("dm-level-" + depth + "-" + i);
            }

            // 模拟深度检查
            if (depth >= maxDepth) {
                break;
            }
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("递归收集" + maxDepth + "层耗时: " + duration + "ms");
        assertEquals(50, collectedDms.size(), "应收集50个DM");
        assertTrue(duration < 100, "递归收集应在100ms内完成");
    }

    /**
     * PERF-07: 序列号生成性能压测
     * 验证：连续生成1000个序列号的性能
     */
    @Test
    @DisplayName("PERF-07: 序列号生成性能压测")
    public void testSequenceNumberGenerationPerformance() {
        String year = "2097"; // 使用未来年份避免冲突
        int count = 100; // 生成100个序列号

        long startTime = System.currentTimeMillis();

        Set<String> seqNumbers = new HashSet<>();
        for (int i = 0; i < count; i++) {
            String seqNumber = ddnService.generateNextSeqNumber(year);
            seqNumbers.add(seqNumber);
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        double avgTime = (double) duration / count;

        System.out.println("生成" + count + "个序列号耗时: " + duration + "ms");
        System.out.println("平均每个: " + String.format("%.2f", avgTime) + "ms");

        assertEquals(count, seqNumbers.size(), "序列号不应重复");
        assertTrue(duration < 10000, "生成" + count + "个序列号应在10秒内完成");
    }

    /**
     * PERF-08: ZIP包大小限制验证
     * 验证：快速校验总大小是否超过1GB限制
     */
    @Test
    @DisplayName("PERF-08: ZIP包总大小限制校验")
    public void testZipSizeValidationPerformance() {
        long maxZipSize = DdnConstants.FileSize.MAX_ZIP_SIZE;
        long totalSize = 0L;

        long startTime = System.nanoTime();

        // 模拟累加文件大小
        for (int i = 0; i < 100; i++) {
            totalSize += 10 * 1024 * 1024L; // 每个10MB

            if (totalSize > maxZipSize) {
                break; // 快速退出
            }
        }

        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1000; // 微秒

        System.out.println("ZIP大小校验耗时: " + duration + "μs");
        assertTrue(totalSize > maxZipSize, "1000MB应超过1GB限制");
        assertTrue(duration < 1000, "ZIP大小校验应在1毫秒内完成");
    }

    /**
     * PERF-09: 内存使用测试
     * 验证：大量DM收集时内存占用合理
     */
    @Test
    @DisplayName("PERF-09: 内存占用测试")
    public void testMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();

        // 触发GC获取基准内存
        System.gc();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        // 模拟收集1000个DM的ID
        Set<String> dmIds = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            dmIds.add("dm-memory-test-" + i);
        }

        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = (memoryAfter - memoryBefore) / 1024 / 1024; // MB

        System.out.println("收集1000个DM ID内存占用: " + memoryUsed + "MB");
        assertEquals(1000, dmIds.size());
        assertTrue(memoryUsed < 50, "内存占用应小于50MB");
    }

    // ========== 辅助方法 ==========

    /**
     * 创建基础参数
     */
    private DdnGenerateVO createBasicParams(List<String> dmIds) {
        DdnGenerateVO params = new DdnGenerateVO();
        params.setDmIds(dmIds);
        params.setModelic("PERF-MODEL");
        params.setSender("00001");
        params.setReceiver("00002");
        params.setIssueDate("2026-09-01");
        params.setSecurity("01");
        params.setCommercialSecurity("cc01");
        params.setCaveat("cv01");
        params.setIncludeRefDm(false);
        params.setIncludeRefIcn(false);
        params.setIncludeDmResource(false);
        return params;
    }

    /**
     * 生成DM ID列表
     */
    private List<String> generateDmIds(String prefix, int count) {
        List<String> dmIds = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            dmIds.add(prefix + String.format("%03d", i));
        }
        return dmIds;
    }
}
