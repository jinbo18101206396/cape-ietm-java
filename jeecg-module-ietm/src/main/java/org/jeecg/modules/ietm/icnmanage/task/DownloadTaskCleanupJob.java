package org.jeecg.modules.ietm.icnmanage.task;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.ietm.icnmanage.service.IIetmIcnManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 下载任务清理定时任务
 * 每天凌晨2点执行，清理24小时前的下载任务缓存
 *
 * @Author: jeecg-boot
 * @Date: 2026-07-21
 */
@Slf4j
@Component
public class DownloadTaskCleanupJob {

    @Autowired
    private IIetmIcnManageService icnManageService;

    /**
     * 每天凌晨2点执行清理任务
     * cron表达式：秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredTasks() {
        log.info("开始执行下载任务清理定时任务...");
        try {
            icnManageService.cleanExpiredTasks();
            log.info("下载任务清理完成");
        } catch (Exception e) {
            log.error("下载任务清理失败", e);
        }
    }
}
