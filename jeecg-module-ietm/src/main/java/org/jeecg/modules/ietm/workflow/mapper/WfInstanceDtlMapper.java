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
}
