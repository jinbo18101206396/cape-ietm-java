package org.jeecg.modules.ietm.workflow.util;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @Description: 工作流分布式锁工具类（可选，需Redis支持）
 * @Author: jeecg-boot
 * @Date: 2026-07-25
 * @Version: V1.0
 */
@Slf4j
@Component
public class WfLockUtil {

    @Autowired(required = false)  // 可选依赖，如果没有Redis不会报错
    private RedisTemplate<String, String> redisTemplate;

    private static final String LOCK_PREFIX = "workflow:lock:batch:";
    private static final long DEFAULT_LOCK_TIMEOUT = 10;  // 秒

    /**
     * 尝试获取批次ID锁
     * @param batchId 批次ID
     * @return 是否成功获取锁
     */
    public boolean tryLock(String batchId) {
        return tryLock(batchId, DEFAULT_LOCK_TIMEOUT);
    }

    /**
     * 尝试获取批次ID锁
     * @param batchId 批次ID
     * @param timeout 超时时间（秒）
     * @return 是否成功获取锁
     */
    public boolean tryLock(String batchId, long timeout) {
        if (redisTemplate == null) {
            log.warn("Redis未配置，跳过分布式锁检查");
            return true;  // 如果没有Redis，直接返回成功
        }

        String lockKey = LOCK_PREFIX + batchId;
        try {
            Boolean success = redisTemplate.opsForValue().setIfAbsent(
                lockKey,
                Thread.currentThread().getName(),
                timeout,
                TimeUnit.SECONDS
            );
            return Boolean.TRUE.equals(success);
        } catch (Exception e) {
            log.error("获取分布式锁失败：{}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 释放批次ID锁
     * @param batchId 批次ID
     */
    public void unlock(String batchId) {
        if (redisTemplate == null) {
            return;
        }

        String lockKey = LOCK_PREFIX + batchId;
        try {
            redisTemplate.delete(lockKey);
        } catch (Exception e) {
            log.error("释放分布式锁失败：{}", e.getMessage(), e);
        }
    }

    /**
     * 执行带锁的操作
     * @param batchId 批次ID
     * @param action 要执行的操作
     * @param <T> 返回值类型
     * @return 操作结果
     */
    public <T> T executeWithLock(String batchId, LockAction<T> action) {
        if (!tryLock(batchId)) {
            throw new JeecgBootException("系统繁忙，请稍后重试");
        }

        try {
            return action.execute();
        } finally {
            unlock(batchId);
        }
    }

    /**
     * 带锁的操作接口
     */
    @FunctionalInterface
    public interface LockAction<T> {
        T execute();
    }
}
