package org.jeecg.modules.ietm.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.ietm.workflow.entity.WfInstanceDtl;

import java.util.List;

/**
 * @Description: 工作流节点明细Mapper接口
 * @Author: jeecg-boot
 * @Date: 2026-07-25
 * @Version: V1.0
 */
public interface WfInstanceDtlMapper extends BaseMapper<WfInstanceDtl> {

    /**
     * 根据实例ID查询所有节点，按seqno排序
     * @param instanceId 工作流实例ID
     * @return 节点列表
     */
    List<WfInstanceDtl> selectByInstanceIdOrderBySeqno(@Param("instanceId") String instanceId);

    /**
     * 查询实例中未执行节点的最小seqno
     * @param instanceId 工作流实例ID
     * @return 最小seqno，如果没有未执行节点返回null
     */
    Integer selectMinSeqnoOfUnexecuted(@Param("instanceId") String instanceId);

    /**
     * 批量插入节点明细
     * @param dtlList 节点列表
     * @return 影响行数
     */
    int batchInsert(@Param("list") List<WfInstanceDtl> dtlList);

    /**
     * 根据实例ID查询节点列表
     * @param instid 实例ID
     * @return 节点列表
     */
    List<WfInstanceDtl> selectByInstId(@Param("instid") String instid);

    /**
     * 批量更新节点状态
     * @param ids 节点ID列表
     * @param status 状态值
     * @return 影响行数
     */
    int batchUpdateStatus(@Param("ids") List<String> ids, @Param("status") String status);

    /**
     * 查询最大顺序号
     * @param instid 实例ID
     * @return 最大顺序号
     */
    Integer selectMaxSeqno(@Param("instid") String instid);

    /**
     * 查询已处理节点的最大顺序号
     * @param instid 实例ID
     * @return 最大顺序号
     */
    Integer selectExecutedMaxSeqno(@Param("instid") String instid);

    /**
     * P0-07: 查询指定阶段的最后一个节点
     * @param instid 实例ID
     * @param stagename 阶段名称
     * @return 节点（按seqno降序第一个）
     */
    WfInstanceDtl selectLastNodeByStage(@Param("instid") String instid, @Param("stagename") String stagename);
}
