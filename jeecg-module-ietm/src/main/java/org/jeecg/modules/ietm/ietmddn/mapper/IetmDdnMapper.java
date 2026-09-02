package org.jeecg.modules.ietm.ietmddn.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.ietm.ietmddn.entity.IetmDdn;

/**
 * @Description: DDN数据交换凭证Mapper
 * @Author: jeecg-boot
 * @Date: 2026-09-01
 */
public interface IetmDdnMapper extends BaseMapper<IetmDdn> {

    /**
     * 查询指定年份当前最大序列号（DM8语法，NULL兜底0）
     * @param year 年份（4位）
     * @return 最大序列号数值
     */
    Integer selectMaxSeqNumber(@Param("year") String year);

    /**
     * 查询指定年份当前最大序列号（带悲观锁，防止并发冲突）
     * 修复P0-3: 使用FOR UPDATE锁定查询结果
     * 修复DM8兼容: 使用两步法（先锁后查）
     * @param year 年份（4位）
     * @return 最大序列号数值
     */
    Integer selectMaxSeqNumberForUpdate(@Param("year") String year);

    /**
     * 锁定指定年份的一条DDN记录（用于并发控制）
     * 修复DM8兼容: DM8不允许聚合函数+FOR UPDATE，需要先锁定记录
     * @param year 年份（4位）
     * @return 被锁定的记录ID，如果当年无记录则返回null
     */
    String lockYearRecord(@Param("year") String year);
}
