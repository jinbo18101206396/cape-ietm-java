package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * DM预览性能监控工具
 *
 * 功能：
 * 1. 收集预览各阶段性能指标
 * 2. 统计平均耗时、最大最小耗时
 * 3. 记录异常情况
 * 4. 生成性能报告
 *
 * @author IETM Team
 * @date 2026-09-18
 */
@Slf4j
public class PreviewPerformanceMonitor {

    /**
     * 性能指标数据
     */
    @Data
    public static class PerformanceMetrics {
        private String stage;           // 阶段名称
        private long duration;          // 本次耗时(ms)
        private long inputSize;         // 输入大小(bytes)
        private long outputSize;        // 输出大小(bytes)
        private String standard;        // S1000D标准版本
        private String dmType;          // DM类型
        private boolean success;        // 是否成功
        private String errorMessage;    // 错误信息
        private long timestamp;         // 时间戳

        public PerformanceMetrics(String stage) {
            this.stage = stage;
            this.timestamp = System.currentTimeMillis();
            this.success = true;
        }
    }

    /**
     * 阶段统计信息
     */
    @Data
    public static class StageStatistics {
        private String stageName;
        private AtomicLong totalCount = new AtomicLong(0);
        private AtomicLong successCount = new AtomicLong(0);
        private AtomicLong failureCount = new AtomicLong(0);
        private AtomicLong totalDuration = new AtomicLong(0);
        private long minDuration = Long.MAX_VALUE;
        private long maxDuration = 0;
        private AtomicLong totalInputSize = new AtomicLong(0);
        private AtomicLong totalOutputSize = new AtomicLong(0);

        public StageStatistics(String stageName) {
            this.stageName = stageName;
        }

        public synchronized void record(PerformanceMetrics metrics) {
            totalCount.incrementAndGet();
            if (metrics.isSuccess()) {
                successCount.incrementAndGet();
            } else {
                failureCount.incrementAndGet();
            }
            totalDuration.addAndGet(metrics.getDuration());
            totalInputSize.addAndGet(metrics.getInputSize());
            totalOutputSize.addAndGet(metrics.getOutputSize());

            if (metrics.getDuration() < minDuration) {
                minDuration = metrics.getDuration();
            }
            if (metrics.getDuration() > maxDuration) {
                maxDuration = metrics.getDuration();
            }
        }

        public double getAverageDuration() {
            long count = totalCount.get();
            return count > 0 ? (double) totalDuration.get() / count : 0.0;
        }

        public double getSuccessRate() {
            long total = totalCount.get();
            return total > 0 ? (double) successCount.get() / total * 100 : 0.0;
        }
    }

    /**
     * 全局统计数据（按阶段）
     */
    private static final ConcurrentHashMap<String, StageStatistics> STAGE_STATS = new ConcurrentHashMap<>();

    /**
     * 预览会话上下文（ThreadLocal）
     */
    private static final ThreadLocal<PreviewSession> CURRENT_SESSION = new ThreadLocal<>();

    /**
     * 预览会话
     */
    @Data
    public static class PreviewSession {
        private String sessionId;
        private long startTime;
        private String standard;
        private String dmType;
        private long xmlSize;
        private Map<String, PerformanceMetrics> stageMetrics = new HashMap<>();
        private boolean completed = false;

        public PreviewSession(String sessionId) {
            this.sessionId = sessionId;
            this.startTime = System.currentTimeMillis();
        }

        public void addMetrics(PerformanceMetrics metrics) {
            stageMetrics.put(metrics.getStage(), metrics);
        }

        public long getTotalDuration() {
            return System.currentTimeMillis() - startTime;
        }
    }

    /**
     * 开始预览会话
     */
    public static PreviewSession startSession(String dmId, long xmlSize) {
        String sessionId = dmId + "_" + System.currentTimeMillis();
        PreviewSession session = new PreviewSession(sessionId);
        session.setXmlSize(xmlSize);
        CURRENT_SESSION.set(session);
        log.debug("性能监控会话启动: sessionId={}, xmlSize={}", sessionId, xmlSize);
        return session;
    }

    /**
     * 记录阶段开始
     */
    public static long recordStageStart(String stage) {
        return System.currentTimeMillis();
    }

    /**
     * 记录阶段完成
     */
    public static void recordStageEnd(String stage, long startTime, long inputSize, long outputSize) {
        long duration = System.currentTimeMillis() - startTime;
        PerformanceMetrics metrics = new PerformanceMetrics(stage);
        metrics.setDuration(duration);
        metrics.setInputSize(inputSize);
        metrics.setOutputSize(outputSize);

        PreviewSession session = CURRENT_SESSION.get();
        if (session != null) {
            metrics.setStandard(session.getStandard());
            metrics.setDmType(session.getDmType());
            session.addMetrics(metrics);
        }

        // 更新全局统计
        StageStatistics stats = STAGE_STATS.computeIfAbsent(stage, StageStatistics::new);
        stats.record(metrics);

        // 性能警告
        if (duration > getWarningThreshold(stage)) {
            log.warn("⚠️ 性能警告: {}阶段耗时{}ms超过阈值{}ms (输入:{}KB, 输出:{}KB)",
                    stage, duration, getWarningThreshold(stage),
                    inputSize / 1024, outputSize / 1024);
        }
    }

    /**
     * 记录阶段失败
     */
    public static void recordStageFailure(String stage, long startTime, String errorMessage) {
        long duration = System.currentTimeMillis() - startTime;
        PerformanceMetrics metrics = new PerformanceMetrics(stage);
        metrics.setDuration(duration);
        metrics.setSuccess(false);
        metrics.setErrorMessage(errorMessage);

        PreviewSession session = CURRENT_SESSION.get();
        if (session != null) {
            session.addMetrics(metrics);
        }

        // 更新全局统计
        StageStatistics stats = STAGE_STATS.computeIfAbsent(stage, StageStatistics::new);
        stats.record(metrics);

        log.error("❌ 阶段失败: stage={}, duration={}ms, error={}", stage, duration, errorMessage);
    }

    /**
     * 设置会话上下文信息
     */
    public static void setSessionContext(String standard, String dmType) {
        PreviewSession session = CURRENT_SESSION.get();
        if (session != null) {
            session.setStandard(standard);
            session.setDmType(dmType);
        }
    }

    /**
     * 完成预览会话并生成报告
     */
    public static String completeSession() {
        PreviewSession session = CURRENT_SESSION.get();
        if (session == null) {
            return "无活动会话";
        }

        session.setCompleted(true);
        long totalDuration = session.getTotalDuration();

        // 生成会话报告
        StringBuilder report = new StringBuilder();
        report.append("=== DM预览性能报告 ===\n");
        report.append(String.format("会话ID: %s\n", session.getSessionId()));
        report.append(String.format("标准版本: %s\n", session.getStandard()));
        report.append(String.format("DM类型: %s\n", session.getDmType()));
        report.append(String.format("XML大小: %.2f KB\n", session.getXmlSize() / 1024.0));
        report.append(String.format("总耗时: %d ms\n", totalDuration));
        report.append("\n各阶段耗时:\n");

        // 按阶段顺序输出
        String[] stages = {"detect", "xslt", "css", "icn", "legacy"};
        for (String stage : stages) {
            PerformanceMetrics metrics = session.getStageMetrics().get(stage);
            if (metrics != null) {
                double percentage = totalDuration > 0 ? (double) metrics.getDuration() / totalDuration * 100 : 0;
                report.append(String.format("  %-10s: %4d ms (%.1f%%) [输入:%.1fKB 输出:%.1fKB] %s\n",
                        stage,
                        metrics.getDuration(),
                        percentage,
                        metrics.getInputSize() / 1024.0,
                        metrics.getOutputSize() / 1024.0,
                        metrics.isSuccess() ? "✅" : "❌"));
            }
        }

        log.info("\n{}", report);

        // 清理ThreadLocal
        CURRENT_SESSION.remove();

        return report.toString();
    }

    /**
     * 获取全局统计报告
     */
    public static String getGlobalStatistics() {
        StringBuilder report = new StringBuilder();
        report.append("\n=== DM预览全局性能统计 ===\n\n");

        String[] stages = {"detect", "xslt", "css", "icn", "legacy"};
        for (String stage : stages) {
            StageStatistics stats = STAGE_STATS.get(stage);
            if (stats != null && stats.getTotalCount().get() > 0) {
                report.append(String.format("【%s】阶段:\n", stage.toUpperCase()));
                report.append(String.format("  总调用次数: %d\n", stats.getTotalCount().get()));
                report.append(String.format("  成功率: %.2f%%\n", stats.getSuccessRate()));
                report.append(String.format("  平均耗时: %.2f ms\n", stats.getAverageDuration()));
                report.append(String.format("  最小耗时: %d ms\n", stats.getMinDuration()));
                report.append(String.format("  最大耗时: %d ms\n", stats.getMaxDuration()));
                report.append(String.format("  总输入: %.2f MB\n", stats.getTotalInputSize().get() / 1024.0 / 1024.0));
                report.append(String.format("  总输出: %.2f MB\n\n", stats.getTotalOutputSize().get() / 1024.0 / 1024.0));
            }
        }

        return report.toString();
    }

    /**
     * 重置全局统计
     */
    public static void resetStatistics() {
        STAGE_STATS.clear();
        log.info("性能统计已重置");
    }

    /**
     * 获取阶段性能警告阈值(ms)
     */
    private static long getWarningThreshold(String stage) {
        switch (stage) {
            case "detect":
                return 100;   // 检测阶段应该很快
            case "xslt":
                return 2000;  // XSLT转换可能较慢
            case "css":
                return 50;    // CSS增强应该很快
            case "icn":
                return 1000;  // ICN处理取决于图片数量
            case "legacy":
                return 100;   // 遗留函数修复应该很快
            default:
                return 500;
        }
    }

    /**
     * 获取当前会话（用于测试）
     */
    public static PreviewSession getCurrentSession() {
        return CURRENT_SESSION.get();
    }
}
