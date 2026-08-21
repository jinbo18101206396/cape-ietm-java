package org.jeecg.modules.ietm.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.ietm.workflow.entity.WfExecute;

import java.util.List;

/**
 * @Description: 工作流执行记录Mapper
 * @Author: IETM Team
 * @Date: 2026-08-20
 * @Version: V1.0
 */
public interface WfExecuteMapper extends BaseMapper<WfExecute> {

    /**
     * 根据明细ID查询执行记录列表
     * @param instdtlid 明细ID
     * @return 执行记录列表
     */
    List<WfExecute> selectByDtlId(@Param("instdtlid") String instdtlid);

    /**
     * 根据实例ID查询所有执行记录
     * @param instid 实例ID
     * @return 执行记录列表
     */
    List<WfExecute> selectByInstId(@Param("instid") String instid);

    /**
     * 查询节点的最新执行记录
     * @param instdtlid 明细ID
     * @return 最新执行记录
     */
    WfExecute selectLatestByDtlId(@Param("instdtlid") String instdtlid);

    /**
     * 批量删除执行记录（逻辑删除）
     * @param dtlIds 明细ID列表
     * @return 影响行数
     */
    int logicDeleteByDtlIds(@Param("dtlIds") List<String> dtlIds);
}
