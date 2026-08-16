package org.jeecg.modules.ietm.common.service;

/**
 * SNS编码计算服务接口
 * 用于ICN和DM模块共享SNS生成逻辑
 *
 * @Author: jeecg-boot
 * @Date: 2026-07-22
 */
public interface ISnsCalculateService {

    /**
     * 根据构型节点ID和编码规则计算SNS编码
     *
     * @param cmNodeId 构型节点ID
     * @return SNS编码（如：ZBBM02-A-A1-20-00-00A）
     */
    String calculateSns(String cmNodeId);

    /**
     * 为 DM 模块计算 SNS（对标老系统 IetmDmAdd.jsp 算法：coderule 补全 + i=4/7 位合并）
     */
    String calculateSnsForDm(String cmNodeId);

    /**
     * 为 ICN 模块计算 SNS（对标老系统 IetmIcnAdd.jsp 算法：取前6段 + i>=3 连写）
     */
    String calculateSnsForIcn(String cmNodeId);
}
