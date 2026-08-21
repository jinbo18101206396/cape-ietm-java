package org.jeecg.modules.ietm.workflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.ietm.workflow.entity.WfExecute;

import java.util.List;

/**
 * @Description: 工作流执行记录服务接口
 * @Author: IETM Team
 * @Date: 2026-08-20
 * @Version: V1.0
 */
public interface IWfExecuteService extends IService<WfExecute> {

    /**
     * 根据明细ID查询执行记录列表
     * @param instdtlid 明细ID
     * @return 执行记录列表
     */
    List<WfExecute> listByDtlId(String instdtlid);

    /**
     * 根据实例ID查询所有执行记录
     * @param instid 实例ID
     * @return 执行记录列表
     */
    List<WfExecute> listByInstId(String instid);

    /**
     * 查询节点的最新执行记录
     * @param instdtlid 明细ID
     * @return 最新执行记录
     */
    WfExecute getLatestByDtlId(String instdtlid);

    /**
     * 执行节点处理（核心业务方法）
     * @param instdtlid 明细ID
     * @param ifpass 处理结果：1=通过,2=不同意,3=跳转,9=终止
     * @param targetDtlid 跳转目标节点ID（跳转时必填）
     * @param opinion 处理意见
     * @param filename 附件名称
     * @param filecontent 附件内容
     * @param userId 当前用户ID
     * @throws Exception 业务异常
     */
    void executeNode(String instdtlid, String ifpass, String targetDtlid,
                     String opinion, String filename, byte[] filecontent, String userId) throws Exception;

    /**
     * 追加意见
     * @param instdtlid 明细ID
     * @param opinion 追加的意见
     * @param userId 当前用户ID
     * @throws Exception 业务异常
     */
    void addOpinion(String instdtlid, String opinion, String userId) throws Exception;

    /**
     * 拿回已处理的节点
     * @param instdtlid 明细ID
     * @param userId 当前用户ID
     * @throws Exception 业务异常
     */
    void takeBack(String instdtlid, String userId) throws Exception;
}
