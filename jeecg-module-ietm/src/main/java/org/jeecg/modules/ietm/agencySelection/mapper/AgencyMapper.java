package org.jeecg.modules.ietm.agencySelection.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.jeecg.modules.ietm.agencySelection.entity.Agency;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

/**
 * @Description: 机构遴选-机构表
 * @Author: jeecg-boot
 * @Date: 2026-04-29
 */
public interface AgencyMapper extends BaseMapper<Agency> {

    @Update("UPDATE agency SET ratio = 0, update_time = CURRENT_TIMESTAMP")
    void resetAllRatios();

    @Update("UPDATE agency SET ut_ratio = 0, project_count = 0, update_time = CURRENT_TIMESTAMP")
    void resetAllUndertaken();

    @Select("SELECT COALESCE(SUM(project_count), 0) FROM agency WHERE del_flag = 0")
    Integer countAllUndertaken();

    @Select("SELECT COALESCE(SUM(ratio), 0) FROM agency WHERE del_flag = 0")
    Integer sumAllRatios();

    @Update("UPDATE agency SET ut_ratio = #{utRatio}, update_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    void updateUtRatio(@Param("id") String id, @Param("utRatio") BigDecimal utRatio);

    @Update("UPDATE agency SET project_count = #{projectCount}, ut_ratio = #{utRatio}, update_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    void updateProjectCountAndUtRatio(@Param("id") String id, @Param("projectCount") Integer projectCount, @Param("utRatio") BigDecimal utRatio);

    @Update("UPDATE agency SET project_count = project_count + #{count}, update_time = CURRENT_TIMESTAMP WHERE id = #{id} AND del_flag = 0")
    void incrementProjectCount(@Param("id") String id, @Param("count") Integer count);

    @Select("SELECT COALESCE(MAX(update_time), CURRENT_TIMESTAMP) FROM agency WHERE del_flag = 0")
    java.util.Date getMaxUpdateTime();
}
