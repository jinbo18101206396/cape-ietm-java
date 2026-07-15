package org.jeecg.modules.ietm.agencySelection.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.jeecg.modules.ietm.agencySelection.entity.Project;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 * @Description: 机构遴选-项目表
 * @Author: jeecg-boot
 * @Date: 2026-04-29
 */
public interface ProjectMapper extends BaseMapper<Project> {

    @Select("SELECT COALESCE(SUM(quantity), 0) FROM project WHERE agency_id = #{agencyId} AND del_flag = 0")
    Integer countUndertakenByAgency(@Param("agencyId") String agencyId);

    @Select("SELECT COALESCE(SUM(quantity), 0) FROM project WHERE del_flag = 0 AND status = '已分配'")
    Integer countAllUndertaken();

    @Select("SELECT COUNT(*) FROM project WHERE del_flag = 0 AND status = '待分配'")
    Integer countPending();

    @Update("UPDATE project SET agency_name = #{agencyName}, update_time = CURRENT_TIMESTAMP WHERE agency_id = #{agencyId} AND del_flag = 0")
    void updateAgencyName(@Param("agencyId") String agencyId, @Param("agencyName") String agencyName);

    @Select("SELECT * FROM project WHERE agency_id = #{agencyId} AND del_flag = 0")
    List<Project> selectByAgencyId(@Param("agencyId") String agencyId);

    @Update("UPDATE project SET agency_id=#{agencyId}, agency_name=#{agencyName}, status='已分配', extract_time=#{extractTime}, extract_user=#{extractUser}, update_time=CURRENT_TIMESTAMP WHERE id=#{id} AND del_flag=0")
    void updateSelectionResult(@Param("id") String id, @Param("agencyId") String agencyId, @Param("agencyName") String agencyName, @Param("extractTime") String extractTime, @Param("extractUser") String extractUser);

    @Select("SELECT COUNT(*) FROM project WHERE agency_id = #{agencyId} AND status = '已分配' AND del_flag = 0")
    int countAssignedByAgency(@Param("agencyId") String agencyId);
}
