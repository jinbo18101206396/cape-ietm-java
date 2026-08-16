package org.jeecg.modules.ietm.workflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.ietm.workflow.entity.WfInstance;
import org.jeecg.modules.ietm.workflow.vo.BatchRestartFlowVO;
import org.jeecg.modules.ietm.workflow.vo.BatchStartFlowVO;

/**
 * @Description: 工作流实例Service接口
 * @Author: jeecg-boot
 * @Date: 2026-07-25
 * @Version: V1.0
 */
public interface IWfInstanceService extends IService<WfInstance> {

    /**
     * 批量启动流程
     *
     * 业务逻辑：
     * 1. 校验所有DM是否存在且未启动流程
     * 2. 防重复检查（基于batch_id）
     * 3. 为每条DM创建工作流实例（wf_instance）
     * 4. 批量插入所有节点明细（wf_instance_dtl）
     * 5. 将创建节点（nodetype='0'）的ifexec置为'Y'
     * 6. 更新DM的attribute_05字段（待办节点信息JSON）
     *
     * @param vo 批量启动请求参数
     * @return 成功启动的DM数量
     */
    int batchStartFlow(BatchStartFlowVO vo);

    /**
     * 批量重新启动流程
     *
     * 业务逻辑：
     * 1. 校验所有DM是否已发布且流程已结束
     * 2. 终止旧实例（status_改为'9'）
     * 3. 为每条DM创建新工作流实例
     * 4. 批量插入所有节点明细（seqno统一+100偏移）
     * 5. 将创建节点的ifexec置为'Y'
     * 6. 更新DM的attribute_05字段
     *
     * @param vo 批量重启请求参数
     * @return 成功重启的DM数量
     */
    int batchRestartFlow(BatchRestartFlowVO vo);
}
