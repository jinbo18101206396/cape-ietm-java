package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * PreviewPerformanceMonitor 单元测试
 *
 * @author IETM Team
 * @date 2026-09-18
 */
public class PreviewPerformanceMonitorTest {

    @Test
    public void testSessionLifecycle() {
        // 重置统计
        PreviewPerformanceMonitor.resetStatistics();

        // 启动会话
        PreviewPerformanceMonitor.PreviewSession session =
            PreviewPerformanceMonitor.startSession("test_dm_001", 10240);

        assertNotNull("会话应该被创建", session);
        assertEquals("XML大小应该正确", 10240, session.getXmlSize());

        // 设置上下文
        PreviewPerformanceMonitor.setSessionContext("S1000D40", "descript");
        assertEquals("标准应该正确", "S1000D40", session.getStandard());
        assertEquals("DM类型应该正确", "descript", session.getDmType());

        // 记录阶段
        long t1 = PreviewPerformanceMonitor.recordStageStart("detect");
        try { Thread.sleep(10); } catch (InterruptedException e) {}
        PreviewPerformanceMonitor.recordStageEnd("detect", t1, 10240, 20);

        // 完成会话
        String report = PreviewPerformanceMonitor.completeSession();

        assertNotNull("报告应该生成", report);
        assertTrue("报告应包含会话ID", report.contains("test_dm_001"));
        assertTrue("报告应包含标准版本", report.contains("S1000D40"));
        assertTrue("报告应包含DM类型", report.contains("descript"));
        assertTrue("报告应包含detect阶段", report.contains("detect"));
    }

    @Test
    public void testMultipleStages() {
        PreviewPerformanceMonitor.resetStatistics();

        PreviewPerformanceMonitor.PreviewSession session =
            PreviewPerformanceMonitor.startSession("test_dm_002", 20480);
        PreviewPerformanceMonitor.setSessionContext("S1000D40", "proced");

        // 模拟5个阶段
        String[] stages = {"detect", "xslt", "css", "icn", "legacy"};
        long[] inputSizes = {20480, 20480, 50000, 50000, 50000};
        long[] outputSizes = {20, 50000, 50000, 55000, 55000};

        for (int i = 0; i < stages.length; i++) {
            long start = PreviewPerformanceMonitor.recordStageStart(stages[i]);
            try { Thread.sleep(5); } catch (InterruptedException e) {}
            PreviewPerformanceMonitor.recordStageEnd(stages[i], start, inputSizes[i], outputSizes[i]);
        }

        String report = PreviewPerformanceMonitor.completeSession();

        // 验证所有阶段都在报告中
        for (String stage : stages) {
            assertTrue("报告应包含" + stage + "阶段", report.contains(stage));
        }
    }

    @Test
    public void testStageFailure() {
        PreviewPerformanceMonitor.resetStatistics();

        PreviewPerformanceMonitor.PreviewSession session =
            PreviewPerformanceMonitor.startSession("test_dm_003", 10240);
        PreviewPerformanceMonitor.setSessionContext("S1000D40", "descript");

        // 记录成功阶段
        long t1 = PreviewPerformanceMonitor.recordStageStart("detect");
        PreviewPerformanceMonitor.recordStageEnd("detect", t1, 10240, 20);

        // 记录失败阶段
        long t2 = PreviewPerformanceMonitor.recordStageStart("xslt");
        PreviewPerformanceMonitor.recordStageFailure("xslt", t2, "XSLT transformation failed");

        String report = PreviewPerformanceMonitor.completeSession();

        assertTrue("报告应包含detect阶段", report.contains("detect"));
        assertTrue("报告应包含xslt阶段", report.contains("xslt"));
        assertTrue("报告应标记xslt失败", report.contains("❌"));
    }

    @Test
    public void testGlobalStatistics() {
        PreviewPerformanceMonitor.resetStatistics();

        // 运行多个会话
        for (int i = 0; i < 3; i++) {
            PreviewPerformanceMonitor.PreviewSession session =
                PreviewPerformanceMonitor.startSession("test_dm_" + i, 10240);
            PreviewPerformanceMonitor.setSessionContext("S1000D40", "descript");

            long t1 = PreviewPerformanceMonitor.recordStageStart("detect");
            try { Thread.sleep(10); } catch (InterruptedException e) {}
            PreviewPerformanceMonitor.recordStageEnd("detect", t1, 10240, 20);

            long t2 = PreviewPerformanceMonitor.recordStageStart("xslt");
            try { Thread.sleep(20); } catch (InterruptedException e) {}
            PreviewPerformanceMonitor.recordStageEnd("xslt", t2, 10240, 50000);

            PreviewPerformanceMonitor.completeSession();
        }

        // 获取全局统计
        String globalStats = PreviewPerformanceMonitor.getGlobalStatistics();

        assertNotNull("全局统计应该生成", globalStats);
        assertTrue("统计应包含DETECT阶段", globalStats.contains("DETECT"));
        assertTrue("统计应包含XSLT阶段", globalStats.contains("XSLT"));
        assertTrue("统计应包含总调用次数", globalStats.contains("总调用次数: 3"));
        assertTrue("统计应包含成功率", globalStats.contains("成功率"));
        assertTrue("统计应包含平均耗时", globalStats.contains("平均耗时"));
    }

    @Test
    public void testPerformanceWarning() {
        PreviewPerformanceMonitor.resetStatistics();

        PreviewPerformanceMonitor.PreviewSession session =
            PreviewPerformanceMonitor.startSession("test_dm_slow", 10240);
        PreviewPerformanceMonitor.setSessionContext("S1000D40", "descript");

        // 模拟慢速detect阶段（超过100ms阈值）
        long t1 = System.currentTimeMillis() - 150; // 模拟150ms前开始
        PreviewPerformanceMonitor.recordStageEnd("detect", t1, 10240, 20);

        // 虽然会触发警告日志，但不应抛出异常
        String report = PreviewPerformanceMonitor.completeSession();
        assertNotNull("即使有性能警告，报告也应该生成", report);
    }

    @Test
    public void testResetStatistics() {
        PreviewPerformanceMonitor.resetStatistics();

        // 运行一个会话
        PreviewPerformanceMonitor.PreviewSession session =
            PreviewPerformanceMonitor.startSession("test_dm_reset", 10240);

        long t1 = PreviewPerformanceMonitor.recordStageStart("detect");
        PreviewPerformanceMonitor.recordStageEnd("detect", t1, 10240, 20);

        PreviewPerformanceMonitor.completeSession();

        // 重置统计
        PreviewPerformanceMonitor.resetStatistics();

        // 获取全局统计应该为空
        String globalStats = PreviewPerformanceMonitor.getGlobalStatistics();
        assertNotNull("全局统计应该返回", globalStats);
        // 重置后不应该有任何阶段统计
        assertFalse("重置后不应有DETECT统计", globalStats.contains("总调用次数: 1"));
    }

    @Test
    public void testConcurrentSessions() throws InterruptedException {
        PreviewPerformanceMonitor.resetStatistics();

        // 模拟并发会话（不同线程）
        Thread thread1 = new Thread(() -> {
            PreviewPerformanceMonitor.PreviewSession session =
                PreviewPerformanceMonitor.startSession("thread1_dm", 10240);
            long t1 = PreviewPerformanceMonitor.recordStageStart("detect");
            try { Thread.sleep(10); } catch (InterruptedException e) {}
            PreviewPerformanceMonitor.recordStageEnd("detect", t1, 10240, 20);
            PreviewPerformanceMonitor.completeSession();
        });

        Thread thread2 = new Thread(() -> {
            PreviewPerformanceMonitor.PreviewSession session =
                PreviewPerformanceMonitor.startSession("thread2_dm", 20480);
            long t1 = PreviewPerformanceMonitor.recordStageStart("detect");
            try { Thread.sleep(10); } catch (InterruptedException e) {}
            PreviewPerformanceMonitor.recordStageEnd("detect", t1, 20480, 20);
            PreviewPerformanceMonitor.completeSession();
        });

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        // 验证全局统计合并了两个线程的数据
        String globalStats = PreviewPerformanceMonitor.getGlobalStatistics();
        assertTrue("统计应包含2次调用", globalStats.contains("总调用次数: 2"));
    }

    @Test
    public void testStageStatistics() {
        PreviewPerformanceMonitor.resetStatistics();

        // 运行多个不同耗时的会话
        long[] durations = {10, 20, 30, 40, 50};

        for (long duration : durations) {
            PreviewPerformanceMonitor.PreviewSession session =
                PreviewPerformanceMonitor.startSession("test_stats", 10240);

            long start = System.currentTimeMillis() - duration;
            PreviewPerformanceMonitor.recordStageEnd("detect", start, 10240, 20);

            PreviewPerformanceMonitor.completeSession();
        }

        String globalStats = PreviewPerformanceMonitor.getGlobalStatistics();

        assertTrue("应显示5次调用", globalStats.contains("总调用次数: 5"));
        assertTrue("应显示最小耗时", globalStats.contains("最小耗时: 10 ms"));
        assertTrue("应显示最大耗时", globalStats.contains("最大耗时: 50 ms"));
        // 平均耗时应该是30ms
        assertTrue("应显示平均耗时", globalStats.contains("平均耗时: 30.00 ms"));
    }
}
