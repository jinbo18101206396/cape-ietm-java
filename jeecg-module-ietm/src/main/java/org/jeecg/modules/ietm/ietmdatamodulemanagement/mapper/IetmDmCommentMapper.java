package org.jeecg.modules.ietm.ietmdatamodulemanagement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDmComment;

import java.util.List;
import java.util.Map;

/**
 * @Description: IETM数据模块资源管理Mapper
 * @Author: jeecg-boot
 * @Date: 2026-07-22
 * @Version: V2.0
 */
public interface IetmDmCommentMapper extends BaseMapper<IetmDmComment> {

    /**
     * 根据DM ID查询关联资源列表
     * @param dmId DM记录ID
     */
    List<Map<String, Object>> selectByDmId(@Param("dmId") String dmId);

    /**
     * 根据DM ID统计资源数量
     * @param dmId DM记录ID
     */
    Integer countByDmId(@Param("dmId") String dmId);

    /**
     * 删除DM的所有关联资源记录
     * @param dmId DM记录ID
     */
    Integer deleteByDmId(@Param("dmId") String dmId);
}
