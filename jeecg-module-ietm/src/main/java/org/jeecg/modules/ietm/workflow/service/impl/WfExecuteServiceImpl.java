package org.jeecg.modules.ietm.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.SpringContextUtils;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDataModule;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.mapper.IetmDataModuleMapper;
import org.jeecg.modules.ietm.workflow.constants.WfConstants;
import org.jeecg.modules.ietm.workflow.entity.WfExecute;
import org.jeecg.modules.ietm.workflow.entity.WfInstance;
import org.jeecg.modules.ietm.workflow.entity.WfInstanceDtl;
import org.jeecg.modules.ietm.workflow.mapper.WfExecuteMapper;
import org.jeecg.modules.ietm.workflow.mapper.WfInstanceDtlMapper;
import org.jeecg.modules.ietm.workflow.mapper.WfInstanceMapper;
import org.jeecg.modules.ietm.workflow.service.IWfExecuteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @Description: 工作流执行记录服务实现
 * @Author: IETM Team
 * @Date: 2026-08-20
 * @Version: V1.0
 */
@Slf4j
@Service
public class WfExecuteServiceImpl extends ServiceImpl<WfExecuteMapper, WfExecute> implements IWfExecuteService {

    @Autowired
    private WfExecuteMapper wfExecuteMapper;

    @Autowired
    private WfInstanceMapper wfInstanceMapper;

    @Autowired
    private WfInstanceDtlMapper wfInstanceDtlMapper;

    @Autowired
    private IetmDataModuleMapper ietmDataModuleMapper;

    @Override
    public List<WfExecute> listByDtlId(String instdtlid) {
        List<WfExecute> list = wfExecuteMapper.selectByDtlId(instdtlid);
        populateCreateName(list);
        return list;
    }

    @Override
    public List<WfExecute> listByInstId(String instid) {
        List<WfExecute> list = wfExecuteMapper.selectByInstId(instid);
        populateCreateName(list);
        return list;
    }

    @Override
    public List<WfExecute> listByInstIdWithHistory(String instid) {
        // 1. 查询当前实例的执行记录
        List<WfExecute> result = wfExecuteMapper.selectByInstId(instid);

        // 2. 查询是否有旧实例ID
        WfInstance currentInstance = wfInstanceMapper.selectById(instid);
        if (currentInstance != null && currentInstance.getOldInstid() != null
                && !currentInstance.getOldInstid().trim().isEmpty()) {

            // 3. 递归查询旧实例的执行记录（支持多次重启）
            List<WfExecute> oldRecords = listByInstIdWithHistory(currentInstance.getOldInstid());

            // 4. 合并新旧记录
            result.addAll(oldRecords);

            // 5. 按创建时间排序
            result.sort((a, b) -> {
                if (a.getCreateTime() == null) return 1;
                if (b.getCreateTime() == null) return -1;
                return a.getCreateTime().compareTo(b.getCreateTime());
            });

            log.debug("[查询历史审批] 当前实例: {}, 旧实例: {}, 总记录数: {}",
                    instid, currentInstance.getOldInstid(), result.size());
        }

        // 6. 填充处理人姓名
        populateCreateName(result);

        return result;
    }

    /**
     * 填充处理人姓名（createName，对齐旧系统 CREATED_NAME 字段）
     * 使用反射调用 SysUserService，避免模块间循环依赖
     */
    private void populateCreateName(List<WfExecute> list) {
        if (list == null || list.isEmpty()) return;
        try {
            // 运行时获取 ISysUserService bean（避免编译时依赖）
            Object userService = SpringContextUtils.getBean("sysUserServiceImpl");
            if (userService == null) {
                log.warn("未找到 SysUserService，跳过 createName 填充");
                return;
            }
            Method getUserByName = userService.getClass().getMethod("getUserByName", String.class);
            for (WfExecute exec : list) {
                if (exec.getCreateBy() != null && !exec.getCreateBy().trim().isEmpty()) {
                    try {
                        Object sysUser = getUserByName.invoke(userService, exec.getCreateBy());
                        if (sysUser != null) {
                            Method getRealname = sysUser.getClass().getMethod("getRealname");
                            String realname = (String) getRealname.invoke(sysUser);
                            exec.setCreateName(realname);
                        }
                    } catch (Exception e) {
                        log.warn("获取用户姓名失败: {}, 错误: {}", exec.getCreateBy(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("populateCreateName 失败: {}", e.getMessage());
        }
    }

    @Override
    public WfExecute getLatestByDtlId(String instdtlid) {
        return wfExecuteMapper.selectLatestByDtlId(instdtlid);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void executeNode(String instdtlid, String ifpass, String targetDtlid,
                            String opinion, String filename, byte[] filecontent, String userId) throws Exception {

        // 1. 查询当前节点
        WfInstanceDtl currentNode = wfInstanceDtlMapper.selectById(instdtlid);
        if (currentNode == null) {
            throw new JeecgBootException("节点不存在");
        }

        // 2. 查询流程实例
        WfInstance instance = wfInstanceMapper.selectById(currentNode.getInstanceid());
        if (instance == null) {
            throw new JeecgBootException("流程实例不存在");
        }

        // 3. 校验流程状态
        if (WfConstants.STATUS_ENDED.equals(instance.getStatus()) || WfConstants.STATUS_TERMINATED.equals(instance.getStatus())) {
            throw new JeecgBootException("流程已结束，不能操作");
        }

        // 4. 校验节点状态
        if (WfConstants.EXEC_YES.equals(currentNode.getIfexec()) || WfConstants.EXEC_SKIP.equals(currentNode.getIfexec())) {
            throw new JeecgBootException("节点已处理，不能重复操作");
        }

        // 5. 根据处理结果执行不同逻辑
        switch (ifpass) {
            case "1": // 通过
                handlePass(currentNode, instance, opinion, filename, filecontent, userId);
                break;
            case "2": // 不同意
                handleReject(currentNode, instance, opinion, filename, filecontent, userId);
                break;
            case "3": // 跳转（退回/跳过）
                handleJump(currentNode, instance, targetDtlid, opinion, filename, filecontent, userId);
                break;
            case "9": // 终止
                handleTerminate(currentNode, instance, opinion, filename, filecontent, userId);
                break;
            default:
                throw new JeecgBootException("无效的处理结果：" + ifpass);
        }
    }

    /**
     * 处理：通过
     */
    private void handlePass(WfInstanceDtl currentNode, WfInstance instance,
                            String opinion, String filename, byte[] filecontent, String userId) {

        // 1. 标记当前节点为已处理
        currentNode.setIfexec(WfConstants.EXEC_YES);
        int nodeRows = wfInstanceDtlMapper.updateById(currentNode);
        if (nodeRows != 1) {
            log.error("节点状态更新失败，节点ID: {}, ifexec: Y", currentNode.getId());
            throw new JeecgBootException("节点状态更新失败，请重试");
        }

        // 2. 判断是否为最后一个节点
        List<WfInstanceDtl> allNodes = wfInstanceDtlMapper.selectByInstId(instance.getId());
        boolean isLastNode = true;
        for (WfInstanceDtl node : allNodes) {
            if (node.getSeqno() > currentNode.getSeqno() &&
                !WfConstants.EXEC_SKIP.equals(node.getIfexec())) {
                isLastNode = false;
                break;
            }
        }

        // 3. 更新流程状态
        if (isLastNode) {
            instance.setStatus("2"); // 完成 (DB约束只接受0/1/2)
        } else {
            instance.setStatus(WfConstants.STATUS_RUNNING); // 审批中
        }
        int instRows = wfInstanceMapper.updateById(instance);
        if (instRows != 1) {
            log.error("流程状态更新失败，流程ID: {}, status: {}", instance.getId(), instance.getStatus());
            throw new JeecgBootException("流程状态更新失败，请重试");
        }

        // 🔴 P0-17修复：流程结束时同步更新 ietm_data_module.workflow_status 字段
        // 对齐旧系统 - 流程结束后，列表页面的"流程状态"应显示为"已结束"
        // 🔴 P0-新增：清空签出状态，避免结束后签出按钮禁用
        if (isLastNode) {
            String businessId = instance.getFormid();
            if (businessId != null && !businessId.trim().isEmpty()) {
                LambdaUpdateWrapper<IetmDataModule> updateWrapper = new LambdaUpdateWrapper<IetmDataModule>()
                        .eq(IetmDataModule::getId, businessId)
                        .set(IetmDataModule::getWorkflowStatus, "0");  // 0=已结束

                // P2-1重构：使用公共方法清空签出状态
                clearCheckoutStatus(updateWrapper);

                int dmRows = ietmDataModuleMapper.update(null, updateWrapper);
                if (dmRows == 0) {
                    log.warn("更新DM的workflow_status失败，DM可能不存在，businessId: {}", businessId);
                } else {
                    log.info("流程结束，已更新DM的workflow_status=0并清空签出状态，businessId: {}", businessId);
                }
            }
        }

        // 4. 保存执行记录
        saveExecuteRecord(currentNode.getId(), "1", null, opinion, filename, filecontent, userId);
    }

    /**
     * 处理：不同意（记录异议，流程继续）
     */
    private void handleReject(WfInstanceDtl currentNode, WfInstance instance,
                              String opinion, String filename, byte[] filecontent, String userId) {

        // 校验意见
        if (opinion == null || opinion.trim().isEmpty()) {
            throw new JeecgBootException("不同意操作必须填写意见");
        }

        // 1. 标记当前节点为已处理
        currentNode.setIfexec(WfConstants.EXEC_YES);
        int nodeRows = wfInstanceDtlMapper.updateById(currentNode);
        if (nodeRows != 1) {
            log.error("节点状态更新失败，节点ID: {}, ifexec: Y", currentNode.getId());
            throw new JeecgBootException("节点状态更新失败，请重试");
        }

        // 🔴 修复：对齐旧系统业务逻辑 - "发表不同意见"记录异议但流程继续，不是一票否决
        // 2. 判断是否为最后一个节点
        List<WfInstanceDtl> allNodes = wfInstanceDtlMapper.selectByInstId(instance.getId());
        boolean isLastNode = true;
        for (WfInstanceDtl node : allNodes) {
            if (node.getSeqno() > currentNode.getSeqno() &&
                !WfConstants.EXEC_SKIP.equals(node.getIfexec())) {
                isLastNode = false;
                break;
            }
        }

        // 3. 更新流程状态（与"通过"逻辑一致：最后节点则结束，否则继续审批）
        if (isLastNode) {
            instance.setStatus("2"); // 完成 (DB约束只接受0/1/2)
        } else {
            instance.setStatus(WfConstants.STATUS_RUNNING); // 审批中
        }
        int instRows = wfInstanceMapper.updateById(instance);
        if (instRows != 1) {
            log.error("流程状态更新失败，流程ID: {}, status: {}", instance.getId(), instance.getStatus());
            throw new JeecgBootException("流程状态更新失败，请重试");
        }

        // 🔴 P0-17修复：流程结束时同步更新 ietm_data_module.workflow_status 字段
        // "发表不同意见"如果是最后一个节点，流程也结束
        // 🔴 P0-新增：清空签出状态，避免结束后签出按钮禁用
        if (isLastNode) {
            String businessId = instance.getFormid();
            if (businessId != null && !businessId.trim().isEmpty()) {
                LambdaUpdateWrapper<IetmDataModule> updateWrapper = new LambdaUpdateWrapper<IetmDataModule>()
                        .eq(IetmDataModule::getId, businessId)
                        .set(IetmDataModule::getWorkflowStatus, "0");  // 0=已结束

                // P2-1重构：使用公共方法清空签出状态
                clearCheckoutStatus(updateWrapper);

                int dmRows = ietmDataModuleMapper.update(null, updateWrapper);
                if (dmRows == 0) {
                    log.warn("更新DM的workflow_status失败，DM可能不存在，businessId: {}", businessId);
                } else {
                    log.info("流程结束（发表不同意见），已更新DM的workflow_status=0并清空签出状态，businessId: {}", businessId);
                }
            }
        }

        // 4. 保存执行记录
        saveExecuteRecord(currentNode.getId(), "2", null, opinion, filename, filecontent, userId);
    }

    /**
     * 处理：跳转（退回/跳过）
     */
    private void handleJump(WfInstanceDtl currentNode, WfInstance instance, String targetDtlid,
                            String opinion, String filename, byte[] filecontent, String userId) {

        // 校验目标节点
        if (targetDtlid == null || targetDtlid.trim().isEmpty()) {
            throw new JeecgBootException("跳转操作必须指定目标节点");
        }

        WfInstanceDtl targetNode = wfInstanceDtlMapper.selectById(targetDtlid);
        if (targetNode == null) {
            throw new JeecgBootException("目标节点不存在");
        }

        if (!targetNode.getInstanceid().equals(currentNode.getInstanceid())) {
            throw new JeecgBootException("目标节点不属于当前流程");
        }

        // P2-LOGIC-01修复：校验目标节点是否在ifgetback允许范围内
        String ifgetback = currentNode.getIfgetback();
        if (ifgetback != null && !ifgetback.trim().isEmpty()) {
            // -1表示不可跳转
            if ("-1".equals(ifgetback.trim())) {
                throw new JeecgBootException("当前节点不允许跳转");
            }
            // 空字符串或null表示不限制，其他情况需要校验
            if (!ifgetback.trim().isEmpty()) {
                String[] allowedIds = ifgetback.split(",");
                boolean isAllowed = false;
                for (String allowedId : allowedIds) {
                    String trimmedId = allowedId.trim();
                    // 0表示可以跳转到创建节点
                    if (WfConstants.SEQNO_START.equals(trimmedId) && targetNode.getSeqno() == 0) {
                        isAllowed = true;
                        break;
                    }
                    // 匹配目标节点ID
                    if (trimmedId.equals(targetDtlid)) {
                        isAllowed = true;
                        break;
                    }
                }
                if (!isAllowed) {
                    throw new JeecgBootException("不允许跳转到该节点，请选择可跳转的节点");
                }
            }
        }

        // 判断是退回还是跳过
        if (targetNode.getSeqno() < currentNode.getSeqno()) {
            // 退回操作
            handleReturn(currentNode, targetNode, opinion, filename, filecontent, userId);
            // 退回后流程状态为审批中
            instance.setStatus(WfConstants.STATUS_RUNNING);
        } else {
            // 跳过操作
            handleSkip(currentNode, targetNode, opinion, filename, filecontent, userId);
            // 根据目标节点类型设置流程状态
            if ("END".equals(targetNode.getNodetype())) {
                // 跳转到终止节点，流程完成 (DB约束只接受0/1/2)
                instance.setStatus("2");
            } else {
                // 跳转到普通节点，流程继续审批
                instance.setStatus(WfConstants.STATUS_RUNNING);
            }
        }

        // 更新流程状态
        int instRows = wfInstanceMapper.updateById(instance);
        if (instRows != 1) {
            log.error("流程状态更新失败，流程ID: {}, status: {}", instance.getId(), instance.getStatus());
            throw new JeecgBootException("流程状态更新失败，请重试");
        }
    }

    /**
     * 处理：退回
     */
    private void handleReturn(WfInstanceDtl currentNode, WfInstanceDtl targetNode,
                              String opinion, String filename, byte[] filecontent, String userId) {

        // 1. 目标节点标记为退回状态，退回次数+1
        String currentIfjump = targetNode.getIfjump();
        int ifjumpValue = (currentIfjump == null || currentIfjump.trim().isEmpty()) ? 0 : Integer.parseInt(currentIfjump);
        ifjumpValue++;
        targetNode.setIfexec(WfConstants.EXEC_RETURN);
        targetNode.setIfjump(String.valueOf(ifjumpValue));
        int targetRows = wfInstanceDtlMapper.updateById(targetNode);
        if (targetRows != 1) {
            log.error("目标节点状态更新失败，节点ID: {}, ifexec: R", targetNode.getId());
            throw new JeecgBootException("目标节点状态更新失败，请重试");
        }

        // 2. 中间节点重置为未处理
        List<WfInstanceDtl> allNodes = wfInstanceDtlMapper.selectByInstId(currentNode.getInstanceid());
        for (WfInstanceDtl node : allNodes) {
            if (node.getSeqno() > targetNode.getSeqno() && node.getSeqno() < currentNode.getSeqno()) {
                node.setIfexec(WfConstants.EXEC_NO);
                int nodeRows = wfInstanceDtlMapper.updateById(node);
                if (nodeRows != 1) {
                    log.error("中间节点状态重置失败，节点ID: {}, ifexec: N", node.getId());
                    throw new JeecgBootException("中间节点状态重置失败，请重试");
                }
            }
        }

        // 3. 当前节点标记为已处理
        currentNode.setIfexec(WfConstants.EXEC_YES);
        int currentRows = wfInstanceDtlMapper.updateById(currentNode);
        if (currentRows != 1) {
            log.error("当前节点状态更新失败，节点ID: {}, ifexec: Y", currentNode.getId());
            throw new JeecgBootException("当前节点状态更新失败，请重试");
        }

        // 4. 保存执行记录
        saveExecuteRecord(currentNode.getId(), "3", String.valueOf(ifjumpValue), opinion, filename, filecontent, userId);
    }

    /**
     * 处理：跳过
     */
    private void handleSkip(WfInstanceDtl currentNode, WfInstanceDtl targetNode,
                            String opinion, String filename, byte[] filecontent, String userId) {

        // 1. 当前节点到目标节点之间的所有节点标记为跳过
        List<WfInstanceDtl> allNodes = wfInstanceDtlMapper.selectByInstId(currentNode.getInstanceid());
        for (WfInstanceDtl node : allNodes) {
            if (node.getSeqno() >= currentNode.getSeqno() && node.getSeqno() < targetNode.getSeqno()) {
                node.setIfexec(WfConstants.EXEC_SKIP);
                int rows = wfInstanceDtlMapper.updateById(node);
                if (rows != 1) {
                    log.error("节点状态更新失败，节点ID: {}, ifexec: J", node.getId());
                    throw new JeecgBootException("节点状态更新失败，请重试");
                }
            }
        }

        // 2. 保存执行记录
        saveExecuteRecord(currentNode.getId(), "3", null, opinion, filename, filecontent, userId);
    }

    /**
     * 处理：终止
     */
    private void handleTerminate(WfInstanceDtl currentNode, WfInstance instance,
                                 String opinion, String filename, byte[] filecontent, String userId) {

        // 校验意见
        if (opinion == null || opinion.trim().isEmpty()) {
            throw new JeecgBootException("终止操作必须填写终止原因");
        }

        // 1. 标记当前节点为已处理
        currentNode.setIfexec(WfConstants.EXEC_YES);
        int nodeRows = wfInstanceDtlMapper.updateById(currentNode);
        if (nodeRows != 1) {
            log.error("节点状态更新失败，节点ID: {}, ifexec: Y", currentNode.getId());
            throw new JeecgBootException("节点状态更新失败，请重试");
        }

        // 2. 流程状态改为终止 (DB约束只接受0/1/2，终止也使用"2")
        instance.setStatus("2");
        int instRows = wfInstanceMapper.updateById(instance);
        if (instRows != 1) {
            log.error("流程状态更新失败，流程ID: {}, status: 2", instance.getId());
            throw new JeecgBootException("流程状态更新失败，请重试");
        }

        // 🔴 P0-17修复：同步更新 ietm_data_module.workflow_status 字段
        // 对齐旧系统 - 流程终止后，列表页面的"流程状态"应显示为"已终止"
        // wf_instance.status='9' 对应 ietm_data_module.workflow_status='9'
        // 🔴 P0-新增：清空签出状态，避免终止后签出按钮禁用
        String businessId = instance.getFormid();
        if (businessId != null && !businessId.trim().isEmpty()) {
            LambdaUpdateWrapper<IetmDataModule> updateWrapper = new LambdaUpdateWrapper<IetmDataModule>()
                    .eq(IetmDataModule::getId, businessId)
                    .set(IetmDataModule::getWorkflowStatus, "9");  // 9=已终止

            // P2-1重构：使用公共方法清空签出状态
            clearCheckoutStatus(updateWrapper);

            int dmRows = ietmDataModuleMapper.update(null, updateWrapper);
            if (dmRows == 0) {
                log.warn("更新DM的workflow_status失败，DM可能不存在，businessId: {}", businessId);
            } else {
                log.info("流程终止，已更新DM的workflow_status=9并清空签出状态，businessId: {}", businessId);
            }
        }

        // 3. 保存执行记录
        saveExecuteRecord(currentNode.getId(), "9", null, opinion, filename, filecontent, userId);
    }

    /**
     * 保存执行记录
     */
    private void saveExecuteRecord(String instdtlid, String ifpass, String ifjump,
                                   String opinion, String filename, byte[] filecontent, String userId) {
        WfExecute execute = new WfExecute();
        execute.setInstdtlid(instdtlid);
        execute.setIfpass(ifpass);
        execute.setIfjump(ifjump);
        execute.setOpinion(opinion);
        execute.setFilename(filename);
        execute.setFilecontent(filecontent);
        wfExecuteMapper.insert(execute);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addOpinion(String instdtlid, String opinion, String userId) throws Exception {

        // 1. 查询节点
        WfInstanceDtl node = wfInstanceDtlMapper.selectById(instdtlid);
        if (node == null) {
            throw new JeecgBootException("节点不存在");
        }

        // P0-04: 追加意见权限校验（对标旧系统7项）

        // 校验1: 顺序号不能为0（创建节点不能追加意见）
        if (node.getSeqno() == null || node.getSeqno() == 0) {
            throw new JeecgBootException("创建节点不能追加意见");
        }

        // 校验2: 节点状态（必须是已处理状态）
        if (!WfConstants.EXEC_YES.equals(node.getIfexec())) {
            throw new JeecgBootException("只能对已处理的节点追加意见");
        }

        // 校验3: 处理人列表必须包含当前用户
        String userid = node.getUserid();
        if (userid == null || !("," + userid + ",").contains("," + userId + ",")) {
            throw new JeecgBootException("请选择一个处理人为自己的节点");
        }

        // 校验4: 执行历史不能为空（必须有执行记录）
        List<WfExecute> executes = wfExecuteMapper.selectByDtlId(instdtlid);
        if (executes == null || executes.isEmpty()) {
            throw new JeecgBootException("该节点无执行历史，不能追加意见");
        }

        // 校验5: 当前用户必须是该节点的实际处理人（有执行记录）
        boolean isHandler = false;
        for (WfExecute exec : executes) {
            if (userId.equals(exec.getCreateBy())) {
                isHandler = true;
                break;
            }
        }
        if (!isHandler) {
            throw new JeecgBootException("只有该节点的处理人才能追加意见");
        }

        // 6. 保存追加意见记录
        saveExecuteRecord(instdtlid, "4", null, opinion, null, null, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void takeBack(String instdtlid, String userId) throws Exception {

        // 1. 查询节点
        WfInstanceDtl node = wfInstanceDtlMapper.selectById(instdtlid);
        if (node == null) {
            throw new JeecgBootException("节点不存在");
        }

        // 2. 校验节点状态（必须是已处理状态）
        if (!WfConstants.EXEC_YES.equals(node.getIfexec())) {
            throw new JeecgBootException("只能拿回已处理的节点");
        }

        // 3. 校验用户权限（必须是该节点的实际处理人）
        // 🔴 P0-16修复：对齐旧系统 - 只允许"通过"(1)和"发表不同意见"(2)的处理人拿回
        // 旧系统行为：
        //   ✅ 可拿回："通过"(1)、"发表不同意见"(2)
        //   ❌ 不可拿回："跳转"(3)、"流程终止"(9)
        // 修改前：只允许 ifpass='1'，导致"发表不同意见"(2)的处理人被误拦截
        List<WfExecute> executes = wfExecuteMapper.selectByDtlId(instdtlid);
        boolean isHandler = false;
        for (WfExecute exec : executes) {
            // 允许"通过"或"发表不同意见"的处理人拿回
            if (userId.equals(exec.getCreateBy()) &&
                (WfConstants.IFPASS_APPROVED.equals(exec.getIfpass()) || WfConstants.IFPASS_REJECTED.equals(exec.getIfpass()))) {
                isHandler = true;
                break;
            }
        }
        if (!isHandler) {
            throw new JeecgBootException("只有该节点的通过或发表不同意见的处理人才能拿回");
        }

        // 4. 校验后续节点状态（后续节点不能已处理）
        List<WfInstanceDtl> allNodes = wfInstanceDtlMapper.selectByInstId(node.getInstanceid());
        for (WfInstanceDtl n : allNodes) {
            if (n.getSeqno() > node.getSeqno() && WfConstants.EXEC_YES.equals(n.getIfexec())) {
                throw new JeecgBootException("后续节点已处理，不能拿回");
            }
        }

        // 5. 将节点状态改为未处理
        node.setIfexec(WfConstants.EXEC_NO);
        int nodeRows = wfInstanceDtlMapper.updateById(node);
        if (nodeRows != 1) {
            log.error("节点状态更新失败，节点ID: {}, ifexec: N", node.getId());
            throw new JeecgBootException("节点状态更新失败，请重试");
        }

        // 6. 更新流程状态为审批中
        WfInstance instance = wfInstanceMapper.selectById(node.getInstanceid());
        instance.setStatus(WfConstants.STATUS_RUNNING);
        int instRows = wfInstanceMapper.updateById(instance);
        if (instRows != 1) {
            log.error("流程状态更新失败，流程ID: {}, status: 1", instance.getId());
            throw new JeecgBootException("流程状态更新失败，请重试");
        }

        // 7. P0-4修复：删除该节点的所有执行记录（对齐旧系统 wfnodeDefExt.jsp Line 1455-1464）
        // 使用逻辑删除保留审计轨迹，而非物理删除
        int deletedCount = wfExecuteMapper.deleteByDtlId(instdtlid);
        log.info("拿回节点执行记录，节点ID: {}, 删除记录数: {}", instdtlid, deletedCount);

        // 🔴 修复：对齐旧系统 - 拿回后不保存拿回记录，处理情况列应完全清空
        // 注释掉原有的保存拿回记录逻辑
        // saveExecuteRecord(instdtlid, "5", null, "拿回操作", null, null, userId);
    }

    /**
     * P2-1重构：清空DM签出状态的公共方法
     * <p>
     * 在以下3种流程操作后调用，清空签出状态避免签出按钮被禁用：
     * 1. 终止流程（handleTerminate）
     * 2. 审批通过最后节点（handlePass）
     * 3. 审批拒绝（handleReject）
     * </p>
     *
     * @param updateWrapper 已配置查询条件的UpdateWrapper，方法会在其上添加清空签出状态的set操作
     * @return 修改后的UpdateWrapper，支持链式调用
     */
    private LambdaUpdateWrapper<IetmDataModule> clearCheckoutStatus(
            LambdaUpdateWrapper<IetmDataModule> updateWrapper) {
        return updateWrapper
                .set(IetmDataModule::getCheckoutUser, null)
                .set(IetmDataModule::getCheckoutTime, null)
                .set(IetmDataModule::getCheckoutDmId, null);
    }
}
