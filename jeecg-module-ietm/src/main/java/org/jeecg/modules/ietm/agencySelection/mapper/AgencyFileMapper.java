package org.jeecg.modules.ietm.agencySelection.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.jeecg.modules.ietm.agencySelection.entity.AgencyFile;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 * @Description: 机构遴选-附件表
 * @Author: jeecg-boot
 * @Date: 2026-04-29
 */
public interface AgencyFileMapper extends BaseMapper<AgencyFile> {

    @Select("SELECT * FROM agency_file WHERE biz_id = #{bizId} AND biz_type = #{bizType} AND del_flag = 0 ORDER BY create_time DESC")
    List<AgencyFile> selectByBizId(@Param("bizId") String bizId, @Param("bizType") String bizType);

    @Delete("DELETE FROM agency_file WHERE biz_id = #{bizId} AND biz_type = #{bizType}")
    void deleteByBizId(@Param("bizId") String bizId, @Param("bizType") String bizType);

    @Select("SELECT * FROM agency_file WHERE biz_id = #{bizId} AND biz_type = #{bizType} AND file_name = #{fileName} AND del_flag = 0")
    AgencyFile selectByFileName(@Param("bizId") String bizId, @Param("bizType") String bizType, @Param("fileName") String fileName);

    @Select("SELECT * FROM agency_file WHERE del_flag = 0")
    List<AgencyFile> selectAll();
}
