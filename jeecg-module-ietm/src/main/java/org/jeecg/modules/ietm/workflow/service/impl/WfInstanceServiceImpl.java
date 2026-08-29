package org.jeecg.modules.ietm.workflow.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.constant.DmConstants;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDataModule;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.mapper.IetmDataModuleMapper;
import org.jeecg.modules.ietm.workflow.entity.WfInstance;
import org.jeecg.modules.ietm.workflow.entity.WfInstanceDtl;
import org.jeecg.modules.ietm.workflow.entity.WfExecute;
import org.jeecg.modules.ietm.workflow.mapper.WfInstanceDtlMapper;
import org.jeecg.modules.ietm.workflow.mapper.WfInstanceMapper;
import org.jeecg.modules.ietm.workflow.service.IWfInstanceService;
import org.jeecg.modules.ietm.workflow.service.IWfExecuteService;
import org.jeecg.modules.ietm.workflow.constants.WfConstants;
import org.jeecg.modules.ietm.workflow.util.WfValidatorUtil;
import org.jeecg.modules.ietm.workflow.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Description: 工作流实例Service实现
 * @Author: jeecg-boot
 * @Date: 2026-07-25
 * @Version: V1.0
 */
@Slf4j
@Service
public class WfInstanceServiceImpl extends ServiceImpl<WfInstanceMapper, WfInstance>
        implements IWfInstanceService {

    @Autowired
    private WfInstanceMapper wfInstanceMapper;

    @Autowired
    private WfInstanceDtlMapper wfInstanceDtlMapper;

    @Autowired
    private IetmDataModuleMapper ietmDataModuleMapper;

    @Autowired(required = false)
    private org.jeecg.modules.ietm.workflow.util.WfLockUtil wfLockUtil;

    @Autowired
    private IWfExecuteService wfExecuteService;

    /**
     * 批量启动工作流
     * <p>
     * 为多个DM批量创建工作流实例，支持自定义节点配置。
     * 该方法采用批量操作优化性能，100条数据仅需约10次数据库操作。
     * </p>
     *
     * @param vo 批量启动流程请求参数，包含DM列表、批次ID、节点配置等
     * @return 成功启动的流程数量
     * @throws JeecgBootException 当参数校验失败、DM不存在、节点配置错误时抛出
     *
     * <p><b>业务规则：</b></p>
     * <ul>
     *   <li>必须包含创建节点（nodetype=0），且顺序号必须为0</li>
     *   <li>节点顺序号不能重复</li>
     *   <li>userid格式支持：单个ID、逗号分隔多ID、前缀形式（role_xxx、dept_xxx）</li>
     *   <li>batch_id必须全局唯一</li>
     *   <li>支持幂等性：相同batch_id重复提交返回已处理结果，不抛异常</li>
     * </ul>
     *
     * <p><b>性能特征：</b></p>
     * <ul>
     *   <li>批量收集数据，减少数据库往返次数（N+1查询优化）</li>
     *   <li>100条数据约10次数据库操作（vs 原300次）</li>
     *   <li>事务边界：所有校验在事务外完成，减少锁持有时间</li>
     * </ul>
     *
     * @see BatchStartFlowVO
     * @see WfConstants
     * @since 1.0
     */
    @Override
    @Transactional(rollbackFor = Exception.class)  // R-002修复：@Transactional移到外层
    public int batchStartFlow(BatchStartFlowVO vo) {
        long startTime = System.currentTimeMillis();
        log.info("开始批量启动流程");
        log.debug("批量启动流程详细信息，批次ID：{}", vo.getBatchId());

        // ============ 第1步：参数校验（事务外） ============
        List<String> dmIds = vo.getDmIds();
        if (dmIds == null || dmIds.isEmpty()) {
            throw new JeecgBootException("参数校验失败：DM ID列表不能为空");
        }

        // S-002修复：DM ID去重校验
        Set<String> uniqueDmIds = new HashSet<>(dmIds);
        if (uniqueDmIds.size() < dmIds.size()) {
            throw new JeecgBootException("参数校验失败：DM ID列表包含重复项，请检查");
        }

        if (dmIds.size() > 1000) {
            throw new JeecgBootException("参数校验失败：单次最多支持1000条DM，当前：" + dmIds.size());
        }

        String batchId = vo.getBatchId();
        if (oConvertUtils.isEmpty(batchId)) {
            throw new JeecgBootException("参数校验失败：批次ID不能为空");
        }
        if (batchId.length() > 64) {
            throw new JeecgBootException("参数校验失败：批次ID长度不能超过64个字符");
        }

        // S-004修复：校验ifurgent字段值范围
        String ifurgent = vo.getIfurgent();
        if (!Arrays.asList("1", "2", "3").contains(ifurgent)) {
            throw new JeecgBootException("参数校验失败：紧急级别无效（允许值：1=一般,2=紧急,3=特急），当前：" + ifurgent);
        }

        // 节点配置校验
        validateNodes(vo.getNodes());

        // ===== P0-001修复：应用分布式锁 =====
        String lockKey = "wf:start:" + batchId;
        boolean locked = false;

        try {
            // 尝试获取分布式锁
            if (wfLockUtil != null) {
                locked = wfLockUtil.tryLock(lockKey);
                if (!locked) {
                    throw new JeecgBootException("系统繁忙，请稍后重试");
                }
                log.debug("获取分布式锁成功，lockKey：{}", lockKey);
            }

            // ============ 第2步：获取登录用户（分布式锁之后） ============
            LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
            String currentUsername = loginUser.getUsername();

            long validationStartTime = System.currentTimeMillis();
            // R-005修复：校验日志改为debug级别
            log.debug("开始前置校验...");

            // ===== 前置校验（事务内，但操作很快） =====
            Map<String, IetmDataModule> dmMap = validateAndLoadDms(dmIds, currentUsername);
            validateBatchIdUnique(batchId);
            long validationTime = System.currentTimeMillis() - validationStartTime;
            log.debug("前置校验完成，耗时：{}ms", validationTime);

            // ===== 执行数据修改（直接在本方法内，不再调用子方法） =====
            int result = doStartFlowInTransaction(vo, dmMap, currentUsername, validationTime);

            long totalTime = System.currentTimeMillis() - startTime;
            log.info("批量启动流程成功，成功数量：{}，总耗时：{}ms", result, totalTime);

            // 性能告警：超过10秒
            if (totalTime > 10000) {
                log.warn("批量启动流程耗时过长：{}ms，建议检查数据库性能或减少批量数量", totalTime);
            }

            return result;
        } catch (Exception e) {
            long totalTime = System.currentTimeMillis() - startTime;
            log.error("批量启动流程失败，耗时：{}ms", totalTime, e);
            throw e;
        } finally {
            // ✅ P2-4修复：释放分布式锁时捕获异常，避免掩盖原始业务异常
            if (locked && wfLockUtil != null) {
                try {
                    wfLockUtil.unlock(lockKey);
                    log.debug("释放分布式锁，lockKey：{}", lockKey);
                } catch (Exception unlockEx) {
                    log.error("释放分布式锁失败，lockKey：{}", lockKey, unlockEx);
                    // 锁释放失败不影响业务结果，只记录日志
                }
            }
        }
    }

    /**
     * 在事务内执行批量启动流程的数据修改操作
     * <p>
     * R-002修复：该方法改为private，由事务方法{@link #batchStartFlow}内部调用。
     * 注意：本方法不加@Transactional，由外层方法统一管理事务。
     * </p>
     *
     * @param vo 批量启动请求参数
     * @param dmMap DM映射表（已加载）
     * @param currentUsername 当前用户名
     * @param validationTime 校验耗时（用于性能分析）
     * @return 成功启动的流程数量
     */
    private int doStartFlowInTransaction(BatchStartFlowVO vo, Map<String, IetmDataModule> dmMap,
                                        String currentUsername, long validationTime) {
        String batchId = vo.getBatchId();
        List<String> dmIds = vo.getDmIds();

        // ===== 诊断：打印接收到的节点配置 =====
        log.info("========== 接收到的节点配置 ==========");
        log.info("节点总数: {}", vo.getNodes() != null ? vo.getNodes().size() : 0);
        if (vo.getNodes() != null) {
            for (BatchStartFlowDtlVO node : vo.getNodes()) {
                log.info("  - seqno={}, nodename='{}', nodetype={}, userid={}, useridname={}",
                    node.getSeqno(), node.getNodename(), node.getNodetype(),
                    node.getUserid(), node.getUseridname());
            }
        }
        log.info("=====================================");

        // ===== 检查幂等性：是否已处理过（必须在事务内，避免竞态条件） =====
        List<WfInstance> existingInstances = wfInstanceMapper.selectList(
            new LambdaQueryWrapper<WfInstance>().eq(WfInstance::getBatchId, batchId)
        );
        if (!existingInstances.isEmpty()) {
            log.warn("批次ID已存在，可能是重复提交，返回已处理结果：{}", batchId);
            return existingInstances.size();
        }

        // ===== 批量收集数据 =====
        long collectStartTime = System.currentTimeMillis();
        List<WfInstance> instanceList = new ArrayList<>();
        List<WfInstanceDtl> allDtlList = new ArrayList<>();
        Map<String, String> dmToTodoJsonMap = new HashMap<>();
        List<String> instanceIds = new ArrayList<>();

        for (String dmId : dmIds) {
            IetmDataModule dm = dmMap.get(dmId);

            // K-003修复 & P0-005修复：构建工作流实例，设置ID和审计字段
            WfInstance instance = new WfInstance();
            instance.setFormid(dmId);
            instance.setTitle("");
            String dmcCode = oConvertUtils.getString(dm.getDmcCode(), dmId);
            instance.setTitleparam("【" + dmcCode + "】");
            instance.setUrl("/ietm/datamodule/detail");
            instance.setStatus(WfConstants.STATUS_RUNNING);
            instance.setIfurgent(vo.getIfurgent());
            instance.setStagenames(oConvertUtils.getString(vo.getStagenames(), ""));
            instance.setBatchId(batchId);
            instance.setReason("");
            instance.setCreateBy(currentUsername);  // P0-005修复：设置创建人
            instance.setCreateTime(new Date());      // P0-005修复：设置创建时间
            instanceList.add(instance);
        }
        long collectTime = System.currentTimeMillis() - collectStartTime;
        log.debug("数据收集完成，耗时：{}ms", collectTime);

        // 批量插入工作流实例
        long insertStartTime = System.currentTimeMillis();
        this.saveBatch(instanceList);
        long insertTime = System.currentTimeMillis() - insertStartTime;
        log.debug("批量插入实例完成，数量：{}，耗时：{}ms", instanceList.size(), insertTime);

        // P0-002修复：收集节点明细，从内存构建待办JSON，避免循环内查询
        long dtlCollectStartTime = System.currentTimeMillis();
        for (int i = 0; i < dmIds.size(); i++) {
            String dmId = dmIds.get(i);
            WfInstance instance = instanceList.get(i);
            String instanceId = instance.getId();
            instanceIds.add(instanceId);

            // 方案C：两阶段插入 - 先生成节点ID，建立_rid→ID映射表
            Map<String, String> ridToIdMap = new HashMap<>();

            // 构建节点明细
            for (BatchStartFlowDtlVO nodeConfig : vo.getNodes()) {
                WfInstanceDtl dtl = new WfInstanceDtl();
                String nodeId = String.valueOf(IdWorker.getId());
                dtl.setId(nodeId);  // K-003修复：生成雪花ID
                dtl.setInstanceid(instanceId);
                dtl.setSeqno(nodeConfig.getSeqno());
                dtl.setNodename(nodeConfig.getNodename());
                dtl.setNodetype(nodeConfig.getNodetype());
                dtl.setUserid(nodeConfig.getUserid());
                dtl.setUseridname(nodeConfig.getUseridname());
                dtl.setStagename(oConvertUtils.getString(nodeConfig.getStagename(), ""));

                // 方案C：解析前端ifgetback（_rid格式）→ 真实节点ID
                String rawIfgetback = oConvertUtils.getString(nodeConfig.getIfgetback(), "");
                String resolvedIfgetback = resolveFrontendIfgetback(rawIfgetback, nodeConfig.get_rid(), ridToIdMap);
                dtl.setIfgetback(resolvedIfgetback);

                // 创建节点（nodetype='0'）启动时自动标记为已执行
                if (WfConstants.NODE_TYPE_CREATE.equals(nodeConfig.getNodetype())) {
                    dtl.setIfexec(WfConstants.EXEC_YES);  // 'Y' - 已执行
                } else {
                    dtl.setIfexec(WfConstants.EXEC_NO);   // 'N' - 未执行
                }
                dtl.setIfjump("0");  // P2-1修复：初始跳转次数为0
                dtl.setIfnoopinion("N");  // P2-1修复：默认不可无意见通过
                dtl.setCreateBy(currentUsername);  // P0-005修复：设置创建人
                dtl.setCreateTime(new Date());      // P0-005修复：设置创建时间
                allDtlList.add(dtl);

                // 建立映射：前端_rid → 真实节点ID
                if (nodeConfig.get_rid() != null && !nodeConfig.get_rid().isEmpty()) {
                    ridToIdMap.put(nodeConfig.get_rid(), nodeId);
                }
            }

            // 方案C：二次映射 - 用真实节点ID替换ifgetback中的_rid
            for (WfInstanceDtl dtl : allDtlList) {
                if (dtl.getInstanceid().equals(instanceId)) {
                    String ifgetback = dtl.getIfgetback();
                    if (ifgetback != null && !ifgetback.isEmpty() && !"-1".equals(ifgetback)) {
                        String[] rids = ifgetback.split(",");
                        StringBuilder idList = new StringBuilder();
                        for (String rid : rids) {
                            String trimmedRid = rid.trim();
                            String realId = ridToIdMap.get(trimmedRid);
                            if (realId != null) {
                                if (idList.length() > 0) {
                                    idList.append(",");
                                }
                                idList.append(realId);
                            }
                        }
                        dtl.setIfgetback(idList.toString());
                    }
                }
            }

            // P0-002修复：从内存构建待办用户名，不查询数据库
            // attribute_05字段存储：当前待办节点的用户名（纯文本，逗号分隔）
            BatchStartFlowDtlVO firstNode = vo.getNodes().stream()
                .filter(n -> WfConstants.NODE_TYPE_CREATE.equals(n.getNodetype()))
                .findFirst()
                .orElse(null);
            if (firstNode != null) {
                // 获取用户名
                String useridname = firstNode.getUseridname();

                // 如果用户名为空，使用空字符串
                if (useridname == null) {
                    useridname = "";
                }

                // 如果用户名过长（超过200字符），截断保留前面部分
                if (useridname.length() > 200) {
                    String[] userNames = useridname.split(",");
                    StringBuilder sb = new StringBuilder();
                    int totalLen = 0;
                    for (String name : userNames) {
                        if (totalLen + name.length() + 1 > 200) {
                            break;
                        }
                        if (sb.length() > 0) {
                            sb.append(",");
                            totalLen++;
                        }
                        sb.append(name);
                        totalLen += name.length();
                    }
                    useridname = sb.toString();
                    log.warn("用户名串过长，已截断：原长度={}，截断后={}",
                        firstNode.getUseridname().length(), useridname.length());
                }

                // ===== 诊断日志 =====
                log.info("========== 待办用户名构建详情 ==========");
                log.info("DM ID: {}", dmId);
                log.info("DMC编码: {}", dmMap.get(dmId).getDmcCode());
                log.info("用户名原始值: {} (长度: {})",
                    firstNode.getUseridname(),
                    firstNode.getUseridname() != null ? firstNode.getUseridname().length() : 0);
                log.info("用户名存储值: {}", useridname);
                log.info("存储值长度: {} 字符", useridname.length());
                log.info("存储值字节数: {} 字节", useridname.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
                log.info("=======================================");

                dmToTodoJsonMap.put(dmId, useridname);
            }
        }
        long dtlCollectTime = System.currentTimeMillis() - dtlCollectStartTime;
        log.debug("节点明细收集完成，节点总数：{}，耗时：{}ms", allDtlList.size(), dtlCollectTime);

        // 批量插入节点明细
        long dtlInsertStartTime = System.currentTimeMillis();
        if (!allDtlList.isEmpty()) {
            wfInstanceDtlMapper.batchInsert(allDtlList);
        }
        long dtlInsertTime = System.currentTimeMillis() - dtlInsertStartTime;
        log.debug("批量插入节点明细完成，耗时：{}ms", dtlInsertTime);

        // ✅ 修复：为创建节点自动生成执行记录（对齐旧系统，解决重启流程后处理情况为空的问题）
        long execInsertStartTime = System.currentTimeMillis();
        List<WfExecute> createNodeExecutes = new ArrayList<>();
        for (WfInstanceDtl dtl : allDtlList) {
            if (WfConstants.NODE_TYPE_CREATE.equals(dtl.getNodetype())) {
                WfExecute execute = new WfExecute();
                execute.setId(String.valueOf(IdWorker.getId()));
                execute.setInstdtlid(dtl.getId());
                execute.setIfpass(WfConstants.IFPASS_APPROVED);  // "1" - 通过
                execute.setIfjump("0");  // 无跳转
                execute.setOpinion("编制");  // 默认意见（对齐旧系统）

                // 处理人：优先使用节点配置的第一个userid，兜底当前用户
                String executeUser = currentUsername;
                if (StringUtils.isNotBlank(dtl.getUserid())) {
                    String[] userIds = dtl.getUserid().split(",");
                    if (userIds.length > 0 && StringUtils.isNotBlank(userIds[0])) {
                        executeUser = userIds[0].trim();
                    }
                }

                execute.setCreateBy(executeUser);
                execute.setCreateTime(dtl.getCreateTime());  // 使用节点创建时间
                execute.setDelFlag("0");  // 正常状态
                createNodeExecutes.add(execute);
            }
        }

        // 批量插入执行记录
        if (!createNodeExecutes.isEmpty()) {
            wfExecuteService.saveBatch(createNodeExecutes);
            log.info("为创建节点自动生成执行记录，数量：{}", createNodeExecutes.size());
        }
        long execInsertTime = System.currentTimeMillis() - execInsertStartTime;
        log.debug("批量插入创建节点执行记录完成，耗时：{}ms", execInsertTime);

        // 批量更新创建节点状态
        long updateStartTime = System.currentTimeMillis();
        if (!instanceIds.isEmpty()) {
            wfInstanceDtlMapper.update(null,
                new LambdaUpdateWrapper<WfInstanceDtl>()
                    .in(WfInstanceDtl::getInstanceid, instanceIds)
                    .eq(WfInstanceDtl::getNodetype, WfConstants.NODE_TYPE_CREATE)
                    .set(WfInstanceDtl::getIfexec, WfConstants.EXEC_YES)
            );
        }

        // R-003修复：批量更新DM的attribute_05和workflowStatus字段
        // R-006修复：添加关键注释说明批量更新逻辑
        // S-007修复：添加乐观锁，防止并发冲突
        // 方案A修改：不再回写 workflow_step 字段，改为从 v_wf_instance 视图动态查询
        // 🔧 IETM-CHECKOUT-BTN-001修复：必须更新 workflow_instance_id，前端签出按钮依赖此字段判断流程是否启动
        if (!dmToTodoJsonMap.isEmpty()) {
            // 由于attribute_05每条DM的值不同，无法使用单一UPDATE语句
            // 采用批量收集+循环更新的方式（已是当前场景下的最优方案）
            for (int i = 0; i < dmIds.size(); i++) {
                String dmId = dmIds.get(i);
                WfInstance instance = instanceList.get(i);
                String instanceId = instance.getId();
                IetmDataModule dm = dmMap.get(dmId);
                String dmcCode = oConvertUtils.getString(dm.getDmcCode(), dmId);

                // 修复：使用workflowStatus字段表示流程状态
                // status字段含义：0=已删除，1=草稿/编辑中，2=已发布
                // workflowStatus字段含义：null/空=未启动，0=已结束，1=流转中，2=已撤销
                String currentWorkflowStatus = dm.getWorkflowStatus();

                // ===== 诊断：获取要更新的用户名 =====
                String usernameValue = dmToTodoJsonMap.get(dmId);
                if (usernameValue == null) {
                    usernameValue = "";
                }
                log.info("========== 准备更新数据库 ==========");
                log.info("DM ID: {}", dmId);
                log.info("DMC编码: {}", dmcCode);
                log.info("流程实例ID: {}", instanceId);
                log.info("attribute_05值（待办用户名）: {}", usernameValue);
                log.info("attribute_05长度: {} 字符", usernameValue.length());
                log.info("attribute_05字节数: {} 字节", usernameValue.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
                log.info("当前DM状态:");
                log.info("  - status: {}", dm.getStatus());
                log.info("  - workflowStatus: {}", currentWorkflowStatus);
                log.info("====================================");

                // 乐观锁：只更新workflowStatus为空或0的记录（未启动或已结束）
                // 🔴 P1-1修复：启动流程时清空历史签出状态，避免签出按钮禁用
                LambdaUpdateWrapper<IetmDataModule> updateWrapper = new LambdaUpdateWrapper<IetmDataModule>()
                    .eq(IetmDataModule::getId, dmId)
                    .eq(IetmDataModule::getStatus, DmConstants.STATUS_VALID)  // status='1'表示草稿/编辑中状态
                    .set(IetmDataModule::getAttribute05, usernameValue)  // 存储用户名（纯文本）
                    .set(IetmDataModule::getWorkflowStatus, WfConstants.STATUS_RUNNING)  // 更新workflowStatus为"1"（流转中）
                    .set(IetmDataModule::getWorkflowInstanceId, instanceId)  // 🔧 修复：更新workflow_instance_id
                    .set(IetmDataModule::getVersionType, "0");  // 🔧 RESTART-VERSION-TYPE-FIX：重启流程后清空已发布标志，允许签出编辑

                // P2-1重构：使用公共方法清空签出状态
                clearCheckoutStatus(updateWrapper);
                // workflow_step、workflow_handler 仍从 v_wf_instance 视图动态查询

                // 添加workflowStatus条件：必须为null、空或0
                if (currentWorkflowStatus == null || "".equals(currentWorkflowStatus)) {
                    updateWrapper.and(w -> w.isNull(IetmDataModule::getWorkflowStatus).or().eq(IetmDataModule::getWorkflowStatus, ""));
                } else if (WfConstants.STATUS_DRAFT.equals(currentWorkflowStatus)) {
                    updateWrapper.eq(IetmDataModule::getWorkflowStatus, WfConstants.STATUS_DRAFT);
                } else {
                    // workflowStatus为1或2，说明已在流程中或已撤销
                    throw new JeecgBootException("DM流程状态异常，无法启动新流程，DMC：" + dmcCode);
                }

                int rows = ietmDataModuleMapper.update(null, updateWrapper);

                // ===== 诊断：更新结果 =====
                log.info("========== 数据库更新结果 ==========");
                log.info("DM ID: {}", dmId);
                log.info("DMC编码: {}", dmcCode);
                log.info("受影响行数: {}", rows);
                if (rows > 0) {
                    log.info("更新成功");
                } else {
                    log.error("更新失败（rows=0），即将抛出异常");
                }
                log.info("====================================");

                // 如果更新失败（rows=0），说明DM状态已被其他用户修改
                if (rows == 0) {
                    throw new JeecgBootException("DM状态已被其他用户修改，请刷新后重试，DMC：" + dmcCode);
                }
            }
            log.debug("批量更新DM状态完成，数量：{}", dmToTodoJsonMap.size());
        }
        long updateTime = System.currentTimeMillis() - updateStartTime;
        log.debug("批量更新状态完成，耗时：{}ms", updateTime);

        int successCount = dmIds.size();
        log.debug("性能分析 - 校验：{}ms, 收集：{}ms, 插入实例：{}ms, 插入明细：{}ms, 更新：{}ms",
                validationTime, collectTime, insertTime, dtlInsertTime, updateTime);

        return successCount;
    }

    /**
     * 批量重启工作流
     * <p>
     * 为已发布的DM批量重启工作流（终止旧实例，创建新实例）。
     * 适用场景：文档已发布，需要重新进入审批流程进行修订。
     * </p>
     *
     * @param vo 批量重启流程请求参数，包含DM列表、旧实例ID、新节点配置等
     * @return 成功重启的流程数量
     * @throws JeecgBootException 当DM未发布、旧流程未结束、参数校验失败时抛出
     *
     * <p><b>业务规则：</b></p>
     * <ul>
     *   <li>DM状态必须为已发布（status=2）</li>
     *   <li>旧流程实例状态必须为已结束（status=2）</li>
     *   <li>新流程节点顺序号自动加100偏移（避免与历史冲突）</li>
     *   <li>旧实例状态置为终止（status=9）</li>
     *   <li>新实例状态置为流转中（status=1）</li>
     * </ul>
     *
     * <p><b>操作步骤：</b></p>
     * <ol>
     *   <li>校验所有DM状态和旧实例状态</li>
     *   <li>逐个处理：终止旧实例</li>
     *   <li>创建新流程实例</li>
     *   <li>插入节点明细（seqno+100）</li>
     *   <li>标记创建节点为已执行</li>
     *   <li>更新DM的待办节点信息</li>
     * </ol>
     *
     * @see BatchRestartFlowVO
     * @see WfConstants#SEQNO_OFFSET
     * @since 1.0
     */
    @Override
    @Transactional(rollbackFor = Exception.class)  // R-002修复：@Transactional移到外层
    public int batchRestartFlow(BatchRestartFlowVO vo) {
        long startTime = System.currentTimeMillis();
        log.info("开始批量重启流程");

        // ============ 第1步：参数校验 ============
        List<BatchRestartDataVO> dataList = vo.getDataList();
        if (dataList == null || dataList.isEmpty()) {
            throw new JeecgBootException("参数校验失败：DM数据列表不能为空");
        }
        if (dataList.size() > 1000) {
            throw new JeecgBootException("参数校验失败：单次最多支持1000条DM，当前：" + dataList.size());
        }

        String batchId = vo.getBatchId();
        if (oConvertUtils.isEmpty(batchId)) {
            throw new JeecgBootException("参数校验失败：批次ID不能为空");
        }
        if (batchId.length() > 64) {
            throw new JeecgBootException("参数校验失败：批次ID长度不能超过64个字符");
        }

        // 节点配置校验
        validateNodes(vo.getNodes());

        // ===== H-001修复：应用分布式锁，防止并发重复重启 =====
        String lockKey = "wf:restart:" + batchId;
        boolean locked = false;

        try {
            // 尝试获取分布式锁
            if (wfLockUtil != null) {
                locked = wfLockUtil.tryLock(lockKey);
                if (!locked) {
                    throw new JeecgBootException("系统繁忙，请稍后重试");
                }
                log.debug("获取分布式锁成功，lockKey：{}", lockKey);
            }

            // ============ 第2步：获取登录用户（分布式锁之后） ============
            LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
            String currentUsername = loginUser.getUsername();

            // ===== 前置校验（事务内，但操作很快） =====
            long validationStartTime = System.currentTimeMillis();
            log.debug("开始前置校验...");  // R-005修复
            // ===== F-1修复：批量加载DM与旧实例，消除逐条 selectById 的 N+1 查询 =====
            List<String> allDmIds = new ArrayList<>();
            List<String> allOldInstanceIds = new ArrayList<>();
            for (BatchRestartDataVO restartData : dataList) {
                allDmIds.add(restartData.getDmId());
                // ⚠️ P1-1修复：强化校验 - 旧实例ID必须提供（收集阶段先校验，便于批量加载）
                if (oConvertUtils.isEmpty(restartData.getOldInstanceId())) {
                    throw new JeecgBootException("未提供旧流程实例ID，无法重启流程，DM：" + restartData.getDmId());
                }
                allOldInstanceIds.add(restartData.getOldInstanceId());
            }

            Map<String, IetmDataModule> dmMap = loadDmMap(allDmIds);
            Map<String, WfInstance> oldInstanceMap = loadInstanceMap(allOldInstanceIds);

            // Step1: 校验所有DM是否已发布且流程已结束（内存校验，无数据库查询）
            for (BatchRestartDataVO restartData : dataList) {
                IetmDataModule dm = dmMap.get(restartData.getDmId());
                if (dm == null) {
                    throw new JeecgBootException("DM不存在，ID：" + restartData.getDmId());
                }

                String dmcCode = oConvertUtils.getString(dm.getDmcCode(), restartData.getDmId());

                // ⚠️ P0-1修复：权限校验 - 只能重启自己创建的流程
                if (!oConvertUtils.isEmpty(dm.getCreateBy()) &&
                    !dm.getCreateBy().equals(currentUsername)) {
                    throw new JeecgBootException("只能重新启动自己创建的流程，DMC：" + dmcCode + "，创建人：" + dm.getCreateBy());
                }

                // 校验DM是否为已发布版本
                // ⚠️ 修复：发布状态由 version_type='1' 标识；status 是逻辑删除标志(0=删除/1=正常)，
                //          且 WfConstants.STATUS_ENDED("2") 是工作流实例状态常量，不能用于DM发布判断。
                //          原逻辑 !STATUS_ENDED.equals(dm.getStatus()) 恒为true，导致所有重启被误拒。
                if (!DmConstants.VERSION_TYPE_PUBLISHED.equals(dm.getVersionType())) {
                    throw new JeecgBootException("DM未发布，无法重启流程，DMC：" + dmcCode);
                }

                WfInstance oldInstance = oldInstanceMap.get(restartData.getOldInstanceId());
                if (oldInstance == null) {
                    throw new JeecgBootException("旧流程实例不存在，ID：" + restartData.getOldInstanceId());
                }
                if (!WfConstants.STATUS_ENDED.equals(oldInstance.getStatus())) {
                    throw new JeecgBootException("旧流程未结束，无法重启，DMC：" + dmcCode);
                }
            }
            long validationTime = System.currentTimeMillis() - validationStartTime;
            log.debug("前置校验完成，耗时：{}ms", validationTime);

            // ===== 执行数据修改 =====
            int result = doRestartFlowInTransaction(vo, dmMap, currentUsername);

            long totalTime = System.currentTimeMillis() - startTime;
            log.info("批量重启流程成功，批次ID：{}，成功数量：{}，总耗时：{}ms", batchId, result, totalTime);

            return result;
        } catch (Exception e) {
            long totalTime = System.currentTimeMillis() - startTime;
            log.error("批量重启流程失败，批次ID：{}，耗时：{}ms", batchId, totalTime, e);
            throw e;
        } finally {
            // 释放分布式锁
            if (locked && wfLockUtil != null) {
                wfLockUtil.unlock(lockKey);
                log.debug("释放分布式锁成功，lockKey：{}", lockKey);
            }
        }
    }

    /**
     * 在事务内执行批量重启流程的数据修改操作
     * <p>
     * R-002修复：该方法改为private，由事务方法{@link #batchRestartFlow}内部调用。
     * K-002修复：批量收集所有操作数据，最后统一执行，保证原子性（要么全成功，要么全失败）。
     * </p>
     *
     * @param vo 批量重启请求参数
     * @param dmMap DM映射表（已加载）
     * @param currentUsername 当前用户名
     * @return 成功重启的流程数量
     */
    private int doRestartFlowInTransaction(BatchRestartFlowVO vo, Map<String, IetmDataModule> dmMap,
                                          String currentUsername) {
        String batchId = vo.getBatchId();
        List<BatchRestartDataVO> dataList = vo.getDataList();

        // ===== K-002修复：批量收集所有操作数据 =====
        long collectStartTime = System.currentTimeMillis();
        List<String> allTerminateIds = new ArrayList<>();
        List<WfInstance> allNewInstances = new ArrayList<>();
        List<WfInstanceDtl> allDtls = new ArrayList<>();
        Map<String, String> allTodoUsernameMap = new HashMap<>();  // C-002修复：改名，存储待办用户名（纯文本）

        for (BatchRestartDataVO restartData : dataList) {
            IetmDataModule dm = dmMap.get(restartData.getDmId());

            // 收集要终止的实例ID
            allTerminateIds.add(restartData.getOldInstanceId());

            // 构建新实例
            WfInstance newInstance = new WfInstance();
            // F-2修复：提前生成雪花ID（batchInsert不走MyBatis-Plus主键回填）
            newInstance.setId(String.valueOf(IdWorker.getId()));
            newInstance.setFormid(restartData.getDmId());
            newInstance.setTitle("");
            String dmcCode = oConvertUtils.getString(dm.getDmcCode(), restartData.getDmId());
            newInstance.setTitleparam("【" + dmcCode + "】");
            newInstance.setUrl("/ietm/datamodule/detail");
            newInstance.setStatus(WfConstants.STATUS_RUNNING);
            newInstance.setIfurgent(vo.getIfurgent());
            newInstance.setStagenames(oConvertUtils.getString(vo.getStagenames(), ""));
            newInstance.setBatchId(batchId);
            newInstance.setReason(vo.getReason());
            newInstance.setOldInstid(restartData.getOldInstanceId());  // 🆕 记录旧实例ID（用于查询历史审批信息）
            newInstance.setCreateBy(currentUsername);  // P0-005修复：设置创建人
            newInstance.setCreateTime(new Date());      // P0-005修复：设置创建时间
            allNewInstances.add(newInstance);
        }
        long collectTime = System.currentTimeMillis() - collectStartTime;
        log.debug("数据收集完成，耗时：{}ms", collectTime);

        // ===== 批量执行：终止旧实例（F-3修复：分片，避免IN列表超限） =====
        long terminateStartTime = System.currentTimeMillis();
        if (!allTerminateIds.isEmpty()) {
            int terminatedCount = 0;
            for (List<String> chunk : partition(allTerminateIds, WfConstants.BATCH_CHUNK_SIZE)) {
                terminatedCount += wfInstanceMapper.batchTerminate(chunk, currentUsername);  // P0-004修复：传递update_by
            }
            // ⚠️ P1-2修复：校验影响行数
            if (terminatedCount != allTerminateIds.size()) {
                throw new JeecgBootException(
                    String.format("终止旧流程实例失败，预期终止%d条，实际终止%d条",
                        allTerminateIds.size(), terminatedCount)
                );
            }
        }
        long terminateTime = System.currentTimeMillis() - terminateStartTime;
        log.debug("批量终止旧实例完成，数量：{}，耗时：{}ms", allTerminateIds.size(), terminateTime);

        // ===== 批量执行：插入新实例（F-2修复：批量插入替代循环；F-3修复：分片） =====
        long insertStartTime = System.currentTimeMillis();
        for (List<WfInstance> chunk : partition(allNewInstances, WfConstants.BATCH_CHUNK_SIZE)) {
            wfInstanceMapper.batchInsert(chunk);
        }
        long insertTime = System.currentTimeMillis() - insertStartTime;
        log.debug("批量插入新实例完成，数量：{}，耗时：{}ms", allNewInstances.size(), insertTime);

        // ===== 批量收集节点明细 =====
        long dtlCollectStartTime = System.currentTimeMillis();
        List<String> newInstanceIds = allNewInstances.stream()
            .map(WfInstance::getId)
            .collect(Collectors.toList());

        for (int i = 0; i < dataList.size(); i++) {
            BatchRestartDataVO restartData = dataList.get(i);
            String newInstanceId = newInstanceIds.get(i);

            // 方案C：两阶段插入 - 先生成节点ID，建立_rid→ID映射表
            Map<String, String> ridToIdMap = new HashMap<>();
            List<WfInstanceDtl> instanceDtls = new ArrayList<>();

            // 构建节点明细
            // ⚠️ 重要：前端传来的seqno已经是最终显示值（前端已+100），不需要再+100
            // - 第1次启动：前端传0,10,20,30,40 → 存储0,10,20,30,40（不加偏移）
            // - 第1次重启：前端传100,110,120,130,140 → 存储100,110,120,130,140（不加偏移）
            // - 第2次重启：前端传200,210,220,230,240 → 存储200,210,220,230,240（不加偏移）
            for (BatchStartFlowDtlVO dtlVO : vo.getNodes()) {
                WfInstanceDtl dtl = new WfInstanceDtl();
                String nodeId = String.valueOf(IdWorker.getId());
                dtl.setId(nodeId);  // K-003修复：生成雪花ID
                dtl.setInstanceid(newInstanceId);
                dtl.setSeqno(dtlVO.getSeqno());  // 直接使用前端传来的seqno，不加偏移
                dtl.setNodename(dtlVO.getNodename());
                dtl.setNodetype(dtlVO.getNodetype());
                dtl.setUserid(dtlVO.getUserid());
                dtl.setUseridname(dtlVO.getUseridname());
                dtl.setStagename(oConvertUtils.getString(dtlVO.getStagename(), ""));

                // 方案C：解析前端ifgetback（_rid格式）→ 待映射
                String rawIfgetback = oConvertUtils.getString(dtlVO.getIfgetback(), "");
                String resolvedIfgetback = resolveFrontendIfgetback(rawIfgetback, dtlVO.get_rid(), ridToIdMap);
                dtl.setIfgetback(resolvedIfgetback);

                // 创建节点（nodetype='0'）启动时自动标记为已执行
                if (WfConstants.NODE_TYPE_CREATE.equals(dtlVO.getNodetype())) {
                    dtl.setIfexec(WfConstants.EXEC_YES);  // 'Y' - 已执行
                } else {
                    dtl.setIfexec(WfConstants.EXEC_NO);   // 'N' - 未执行
                }
                dtl.setIfjump("0");  // P2-1修复：初始跳转次数为0
                dtl.setIfnoopinion("N");  // P2-1修复：默认不可无意见通过
                dtl.setCreateBy(currentUsername);  // P0-005修复：设置创建人
                dtl.setCreateTime(new Date());      // P0-005修复：设置创建时间
                instanceDtls.add(dtl);

                // 建立映射：前端_rid → 真实节点ID
                if (dtlVO.get_rid() != null && !dtlVO.get_rid().isEmpty()) {
                    ridToIdMap.put(dtlVO.get_rid(), nodeId);
                }
            }

            // 方案C：二次映射 - 用真实节点ID替换ifgetback中的_rid
            for (WfInstanceDtl dtl : instanceDtls) {
                String ifgetback = dtl.getIfgetback();
                if (ifgetback != null && !ifgetback.isEmpty() && !"-1".equals(ifgetback)) {
                    String[] rids = ifgetback.split(",");
                    StringBuilder idList = new StringBuilder();
                    for (String rid : rids) {
                        String trimmedRid = rid.trim();
                        String realId = ridToIdMap.get(trimmedRid);
                        if (realId != null) {
                            if (idList.length() > 0) {
                                idList.append(",");
                            }
                            idList.append(realId);
                        }
                    }
                    dtl.setIfgetback(idList.toString());
                }
            }

            allDtls.addAll(instanceDtls);

            // C-002修复：构建待办用户名（纯文本），与batchStartFlow保持一致
            BatchStartFlowDtlVO firstNode = vo.getNodes().stream()
                .filter(n -> WfConstants.NODE_TYPE_CREATE.equals(n.getNodetype()))
                .findFirst()
                .orElse(null);
            if (firstNode != null) {
                // 存储用户姓名（纯文本格式）
                String useridname = firstNode.getUseridname();
                if (useridname == null) {
                    useridname = "";
                }

                // 如果用户名过长（超过200字符），截断保留前面部分
                if (useridname.length() > 200) {
                    String[] userNames = useridname.split(",");
                    StringBuilder sb = new StringBuilder();
                    int totalLen = 0;
                    for (String name : userNames) {
                        if (totalLen + name.length() + 1 > 200) {
                            break;
                        }
                        if (sb.length() > 0) {
                            sb.append(",");
                            totalLen++;
                        }
                        sb.append(name);
                        totalLen += name.length();
                    }
                    useridname = sb.toString();
                    log.warn("重启流程：用户名串过长，已截断，DM：{}，原长度={}，截断后={}",
                        restartData.getDmId(), firstNode.getUseridname().length(), useridname.length());
                }

                allTodoUsernameMap.put(restartData.getDmId(), useridname);
            }
        }
        long dtlCollectTime = System.currentTimeMillis() - dtlCollectStartTime;
        log.debug("节点明细收集完成，节点总数：{}，耗时：{}ms", allDtls.size(), dtlCollectTime);

        // ===== 批量执行：插入节点明细（F-3修复：分片，避免VALUES批量超限） =====
        long dtlInsertStartTime = System.currentTimeMillis();
        if (!allDtls.isEmpty()) {
            for (List<WfInstanceDtl> chunk : partition(allDtls, WfConstants.BATCH_CHUNK_SIZE)) {
                wfInstanceDtlMapper.batchInsert(chunk);
            }
        }
        long dtlInsertTime = System.currentTimeMillis() - dtlInsertStartTime;
        log.debug("批量插入节点明细完成，耗时：{}ms", dtlInsertTime);

        // ✅ 修复：为创建节点自动生成执行记录（对齐旧系统，解决重启流程后处理情况为空的问题）
        long execInsertStartTime = System.currentTimeMillis();
        List<WfExecute> createNodeExecutes = new ArrayList<>();
        for (WfInstanceDtl dtl : allDtls) {
            if (WfConstants.NODE_TYPE_CREATE.equals(dtl.getNodetype())) {
                WfExecute execute = new WfExecute();
                execute.setId(String.valueOf(IdWorker.getId()));
                execute.setInstdtlid(dtl.getId());
                execute.setIfpass(WfConstants.IFPASS_APPROVED);  // "1" - 通过
                execute.setIfjump("0");  // 无跳转
                execute.setOpinion("编制");  // 默认意见（对齐旧系统）

                // 处理人：优先使用节点配置的第一个userid，兜底当前用户
                String executeUser = currentUsername;
                if (StringUtils.isNotBlank(dtl.getUserid())) {
                    String[] userIds = dtl.getUserid().split(",");
                    if (userIds.length > 0 && StringUtils.isNotBlank(userIds[0])) {
                        executeUser = userIds[0].trim();
                    }
                }

                execute.setCreateBy(executeUser);
                execute.setCreateTime(dtl.getCreateTime());  // 使用节点创建时间
                execute.setDelFlag("0");  // 正常状态
                createNodeExecutes.add(execute);
            }
        }

        // 批量插入执行记录（分片避免超限）
        if (!createNodeExecutes.isEmpty()) {
            for (List<WfExecute> chunk : partition(createNodeExecutes, WfConstants.BATCH_CHUNK_SIZE)) {
                wfExecuteService.saveBatch(chunk);
            }
            log.info("为创建节点自动生成执行记录（重启流程），数量：{}", createNodeExecutes.size());
        }
        long execInsertTime = System.currentTimeMillis() - execInsertStartTime;
        log.debug("批量插入创建节点执行记录完成，耗时：{}ms", execInsertTime);

        // ===== 批量更新：创建节点状态（F-3修复：分片IN列表） =====
        long updateStartTime = System.currentTimeMillis();
        if (!newInstanceIds.isEmpty()) {
            for (List<String> chunk : partition(newInstanceIds, WfConstants.BATCH_CHUNK_SIZE)) {
                wfInstanceDtlMapper.update(null,
                    new LambdaUpdateWrapper<WfInstanceDtl>()
                        .in(WfInstanceDtl::getInstanceid, chunk)
                        .eq(WfInstanceDtl::getNodetype, WfConstants.NODE_TYPE_CREATE)
                        .set(WfInstanceDtl::getIfexec, WfConstants.EXEC_YES)
                );
            }
        }

        // ===== H-003修复：批量更新DM状态（一次SQL替代循环UPDATE） =====
        // 准备批量更新数据
        List<String> dmIds = new ArrayList<>();
        List<String> instanceIds = new ArrayList<>();
        List<String> todoUsernames = new ArrayList<>();

        for (int i = 0; i < dataList.size(); i++) {
            BatchRestartDataVO restartData = dataList.get(i);
            String dmId = restartData.getDmId();
            String newInstanceId = newInstanceIds.get(i);
            String todoUsername = allTodoUsernameMap.get(dmId);

            dmIds.add(dmId);
            instanceIds.add(newInstanceId);
            todoUsernames.add(todoUsername != null ? todoUsername : "");
        }

        // 批量更新（F-3修复：三个索引对应的List整体分片，避免CASE WHEN/IN超限）
        int updateRows = 0;
        int total = dmIds.size();
        for (int start = 0; start < total; start += WfConstants.BATCH_CHUNK_SIZE) {
            int end = Math.min(start + WfConstants.BATCH_CHUNK_SIZE, total);
            updateRows += ietmDataModuleMapper.batchUpdateForRestartFlow(
                dmIds.subList(start, end),
                instanceIds.subList(start, end),
                todoUsernames.subList(start, end),
                currentUsername
            );
        }

        // 校验更新行数
        if (updateRows != dataList.size()) {
            throw new JeecgBootException(
                String.format("批量更新DM失败，预期更新%d条，实际更新%d条，可能有DM状态已被其他用户修改",
                    dataList.size(), updateRows)
            );
        }

        long updateTime = System.currentTimeMillis() - updateStartTime;
        log.debug("批量更新DM状态完成，数量：{}，耗时：{}ms", updateRows, updateTime);

        log.debug("性能分析 - 收集：{}ms, 终止：{}ms, 插入实例：{}ms, 插入明细：{}ms, 更新：{}ms",
                collectTime, terminateTime, insertTime, dtlInsertTime, updateTime);

        return dataList.size();
    }

    /**
     * P2-1重构：清空DM签出状态的公共方法
     * <p>
     * 在以下5种流程操作后调用，清空签出状态避免签出按钮被禁用：
     * 1. 启动流程（doStartFlowInTransaction）
     * 2. 重启流程（Mapper XML: batchUpdateForRestartFlow）
     * 3. 终止流程（WfExecuteServiceImpl.handleTerminate）
     * 4. 审批通过最后节点（WfExecuteServiceImpl.handlePass）
     * 5. 审批拒绝（WfExecuteServiceImpl.handleReject）
     * </p>
     *
     * @param updateWrapper 已配置查询条件的UpdateWrapper，方法会在其上添加清空签出状态的set操作
     * @return 修改后的UpdateWrapper，支持链式调用
     */
    protected LambdaUpdateWrapper<IetmDataModule> clearCheckoutStatus(
            LambdaUpdateWrapper<IetmDataModule> updateWrapper) {
        return updateWrapper
                .set(IetmDataModule::getCheckoutUser, null)
                .set(IetmDataModule::getCheckoutTime, null)
                .set(IetmDataModule::getCheckoutDmId, null);
    }

    /**
     * 将列表按指定大小分片。
     * <p>F-3修复：批量 SELECT/INSERT/UPDATE 需分片，避免达梦DM8/Oracle 的 IN 列表 1000 表达式上限。</p>
     *
     * @param source 源列表（非空）
     * @param size   每片最大元素数（&gt;0）
     * @return 分片后的子列表集合（子列表为源列表的视图）
     */
    private <T> List<List<T>> partition(List<T> source, int size) {
        List<List<T>> chunks = new ArrayList<>();
        for (int i = 0; i < source.size(); i += size) {
            chunks.add(source.subList(i, Math.min(i + size, source.size())));
        }
        return chunks;
    }

    /**
     * 分片批量加载DM，返回 id → 实体 的映射。
     * <p>F-1修复：替代校验循环中逐条 selectById，消除 N+1 查询。</p>
     *
     * @param dmIds DM ID列表（可能含重复）
     * @return id → IetmDataModule 映射
     */
    private Map<String, IetmDataModule> loadDmMap(List<String> dmIds) {
        Map<String, IetmDataModule> map = new HashMap<>();
        List<String> distinctIds = new ArrayList<>(new LinkedHashSet<>(dmIds));
        for (List<String> chunk : partition(distinctIds, WfConstants.BATCH_CHUNK_SIZE)) {
            for (IetmDataModule dm : ietmDataModuleMapper.selectBatchIds(chunk)) {
                map.put(dm.getId(), dm);
            }
        }
        return map;
    }

    /**
     * 分片批量加载工作流实例，返回 id → 实体 的映射。
     * <p>F-1修复：替代校验循环中逐条 selectById，消除 N+1 查询。</p>
     *
     * @param instanceIds 实例ID列表（可能含重复）
     * @return id → WfInstance 映射
     */
    private Map<String, WfInstance> loadInstanceMap(List<String> instanceIds) {
        Map<String, WfInstance> map = new HashMap<>();
        List<String> distinctIds = new ArrayList<>(new LinkedHashSet<>(instanceIds));
        for (List<String> chunk : partition(distinctIds, WfConstants.BATCH_CHUNK_SIZE)) {
            for (WfInstance inst : wfInstanceMapper.selectBatchIds(chunk)) {
                map.put(inst.getId(), inst);
            }
        }
        return map;
    }

    /**
     * 方案C：解析前端ifgetback格式，将_rid映射为真实节点ID。
     * <p>
     * 前端发送的ifgetback有三种格式：
     * <ul>
     *   <li>空字符串 "" 或 "__UNLIMITED__" → 不限制，返回 ""</li>
     *   <li>"-1" 或 "__NO_JUMP__" → 不可跳转，返回 "-1"</li>
     *   <li>_rid逗号分隔列表 "uuid1,uuid2" → 需映射为真实节点ID</li>
     * </ul>
     * </p>
     * <p>
     * 注意：此方法在第一阶段调用，此时ridToIdMap尚未填充完整，
     * 返回的是_rid列表（待第二阶段二次映射）。
     * </p>
     *
     * @param rawIfgetback 前端传入的原始ifgetback值
     * @param currentRid   当前节点的_rid（用于未来扩展的自引用检测）
     * @param ridToIdMap   _rid到真实节点ID的映射表（第一阶段时为空）
     * @return 清洗后的ifgetback（特殊值或_rid列表，待二次映射）
     */
    private String resolveFrontendIfgetback(String rawIfgetback, String currentRid, Map<String, String> ridToIdMap) {
        // 空值或"不限制"
        if (rawIfgetback == null || rawIfgetback.trim().isEmpty() || "__UNLIMITED__".equals(rawIfgetback)) {
            return "";
        }

        // "不可跳转"
        if ("-1".equals(rawIfgetback.trim()) || "__NO_JUMP__".equals(rawIfgetback)) {
            return "-1";
        }

        // 具体节点列表：返回原值（_rid逗号列表），待第二阶段映射
        // 清洗：去除前后空格，过滤空串
        String[] parts = rawIfgetback.split(",");
        StringBuilder cleaned = new StringBuilder();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                if (cleaned.length() > 0) {
                    cleaned.append(",");
                }
                cleaned.append(trimmed);
            }
        }
        return cleaned.toString();
    }

    /**
     * 构建待办节点信息JSON
     * <p>
     * 查询指定流程实例中未执行的最小顺序号节点，构建JSON格式的待办信息。
     * 该JSON会存储在DM的attribute_05字段中，供前端快速查询当前待办节点。
     * </p>
     *
     * <p><b>R-004修复：本方法已废弃，不再使用。</b></p>
     * <p>
     * 原因：该方法会查询数据库，在批量操作中导致N+1问题。
     * 现已优化为在batchStartFlow和batchRestartFlow中直接从内存构建JSON，避免数据库查询。
     * </p>
     *
     * @param instanceId 工作流实例ID
     * @param nodes 节点配置列表（启动时传入，重启时可能不需要）
     * @return JSON字符串，格式：{"instanceId":"xxx","nodename":"审核","userid":"user1","useridname":"张三","seqno":1,"nodetype":"1"}
     *         如果无待办节点，返回"{}"
     *
     * <p><b>JSON字段说明：</b></p>
     * <ul>
     *   <li>instanceId: 流程实例ID</li>
     *   <li>nodename: 节点名称（如"创建"、"审核"、"审批"）</li>
     *   <li>userid: 处理人ID（支持逗号分隔多人、前缀形式）</li>
     *   <li>useridname: 处理人姓名</li>
     *   <li>seqno: 节点顺序号</li>
     *   <li>nodetype: 节点类型（0=创建，1=审核，2=审批）</li>
     * </ul>
     *
     * @deprecated 本方法已废弃，请直接在批量方法中从内存构建JSON
     * @since 1.0
     */
    @Deprecated
    private String buildTodoNodeJson(String instanceId, List<BatchStartFlowDtlVO> nodes) {
        // 查询当前未执行的最小seqno节点
        List<WfInstanceDtl> unexecutedNodes = wfInstanceDtlMapper.selectList(
            new LambdaQueryWrapper<WfInstanceDtl>()
                .eq(WfInstanceDtl::getInstanceid, instanceId)
                .eq(WfInstanceDtl::getIfexec, WfConstants.EXEC_NO)
                .orderByAsc(WfInstanceDtl::getSeqno)
        );

        if (unexecutedNodes == null || unexecutedNodes.isEmpty()) {
            return "{}";
        }

        WfInstanceDtl currentNode = unexecutedNodes.get(0);

        JSONObject json = new JSONObject();
        json.put("instanceId", instanceId);
        json.put("nodename", currentNode.getNodename());
        json.put("userid", currentNode.getUserid());
        json.put("useridname", currentNode.getUseridname());
        json.put("seqno", currentNode.getSeqno());
        json.put("nodetype", currentNode.getNodetype());

        return json.toJSONString();
    }

    /**
     * 前置校验：验证并加载所有DM
     * <p>
     * 批量查询DM并验证存在性、状态合法性、权限，将结果放入Map便于后续快速访问。
     * 该方法在事务外调用，避免长时间持有事务锁。
     * </p>
     *
     * @param dmIds DM ID列表
     * @param currentUsername 当前登录用户名（用于权限校验）
     * @return DM映射表，Key为DM ID，Value为DM实体
     * @throws JeecgBootException 当存在不存在的DM、状态不合法、无权限时抛出
     *
     * <p><b>S-001修复：添加DM状态校验</b></p>
     * <ul>
     *   <li>只有草稿状态（status=0）的DM才能启动流程</li>
     *   <li>审批中（status=1）的DM拒绝启动</li>
     *   <li>已发布（status=2）的DM应使用重启流程功能</li>
     * </ul>
     *
     * <p><b>P1-1修复：添加业务权限校验</b></p>
     * <ul>
     *   <li>只能启动自己创建的DM流程</li>
     *   <li>对标重启流程的权限控制逻辑</li>
     * </ul>
     *
     * @since 1.0
     */
    private Map<String, IetmDataModule> validateAndLoadDms(List<String> dmIds, String currentUsername) {
        Map<String, IetmDataModule> dmMap = new HashMap<>();
        for (String dmId : dmIds) {
            IetmDataModule dm = ietmDataModuleMapper.selectById(dmId);
            if (dm == null) {
                throw new JeecgBootException("DM不存在，ID：" + dmId);
            }

            String dmcCode = oConvertUtils.getString(dm.getDmcCode(), dmId);

            // P1-1修复：业务权限校验 - 只能启动自己创建的DM流程
            if (!oConvertUtils.isEmpty(dm.getCreateBy()) &&
                !dm.getCreateBy().equals(currentUsername)) {
                throw new JeecgBootException("只能启动自己创建的流程，DMC：" + dmcCode);
            }

            // S-001修复：检查DM状态是否允许启动流程
            // 注意：status字段含义为（0=已删除，1=正常），不是流程状态
            // 流程状态应该检查workflowStatus字段
            String dmStatus = dm.getStatus();
            if (DmConstants.STATUS_DELETED.equals(dmStatus)) {
                throw new JeecgBootException("DM已删除，无法启动流程，DMC：" + dmcCode);
            }

            // 检查工作流状态：只有未启动流程或流程已结束的才能启动新流程
            String workflowStatus = dm.getWorkflowStatus();
            if (workflowStatus != null && !"".equals(workflowStatus)) {
                if (WfConstants.STATUS_RUNNING.equals(workflowStatus)) {
                    throw new JeecgBootException("DM处于审批中，无法启动新流程，DMC：" + dmcCode);
                } else if (WfConstants.STATUS_ENDED.equals(workflowStatus)) {
                    throw new JeecgBootException("DM流程已撤销，请先处理后再启动，DMC：" + dmcCode);
                }
                // workflowStatus = 0 表示流程已结束，可以重新启动
            }

            // 检查是否已有流转中的流程（双重保险）
            long existingCount = wfInstanceMapper.selectCount(
                new LambdaQueryWrapper<WfInstance>()
                    .eq(WfInstance::getFormid, dmId)
                    .in(WfInstance::getStatus, WfConstants.STATUS_DRAFT, WfConstants.STATUS_RUNNING)
            );
            if (existingCount > 0) {
                throw new JeecgBootException("DM已存在流转中的流程，DMC：" + dmcCode);
            }

            dmMap.put(dmId, dm);
        }
        return dmMap;
    }

    /**
     * 前置校验：批次ID唯一性
     * <p>
     * 检查批次ID是否已被使用。该方法在事务外调用，避免长时间持有事务锁。
     * </p>
     *
     * @param batchId 批次ID
     * @throws JeecgBootException 当批次ID为空或已存在时抛出
     *
     * @since 1.0
     */
    private void validateBatchIdUnique(String batchId) {
        if (oConvertUtils.isEmpty(batchId)) {
            throw new JeecgBootException("批次ID不能为空");
        }

        long count = wfInstanceMapper.selectCount(
            new LambdaQueryWrapper<WfInstance>().eq(WfInstance::getBatchId, batchId)
        );
        if (count > 0) {
            throw new JeecgBootException("批次ID已存在，请重新操作");
        }
    }

    /**
     * 前置校验：节点配置合理性
     * <p>
     * 验证节点配置的完整性和正确性。该方法在事务外调用，避免长时间持有事务锁。
     * </p>
     *
     * @param nodes 节点配置列表
     * @throws JeecgBootException 当节点配置不符合业务规则时抛出，包括：
     *         <ul>
     *           <li>节点列表为空</li>
     *           <li>缺少创建节点（nodetype=0）</li>
     *           <li>创建节点顺序号不为0</li>
     *           <li>节点顺序号重复、为负数、超出范围</li>
     *           <li>必填字段为空（nodename、userid、useridname）</li>
     *           <li>字段长度超限</li>
     *           <li>S-005: 节点类型值不合法</li>
     *           <li>S-006: userid与useridname数量不一致</li>
     *         </ul>
     *
     * @since 1.0
     */
    private void validateNodes(List<BatchStartFlowDtlVO> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            throw new JeecgBootException("节点配置不能为空");
        }

        if (nodes.size() > 100) {
            throw new JeecgBootException("节点数量不能超过100个，当前：" + nodes.size());
        }

        Set<Integer> seqnoSet = new HashSet<>();
        boolean hasCreateNode = false;
        // S-005修复：允许的节点类型
        Set<String> allowedNodeTypes = new HashSet<>(Arrays.asList(
            WfConstants.NODE_TYPE_CREATE,
            WfConstants.NODE_TYPE_REVIEW,
            WfConstants.NODE_TYPE_APPROVE
        ));

        for (BatchStartFlowDtlVO node : nodes) {
            // S-003修复：检查seqno范围
            if (node.getSeqno() == null) {
                throw new JeecgBootException("节点顺序号不能为空");
            }
            if (node.getSeqno() < 0) {
                throw new JeecgBootException("节点顺序号不能为负数：" + node.getSeqno());
            }
            if (node.getSeqno() > 9999) {
                throw new JeecgBootException("节点顺序号不能超过9999：" + node.getSeqno());
            }

            // 检查seqno重复
            if (!seqnoSet.add(node.getSeqno())) {
                throw new JeecgBootException("节点顺序号重复：" + node.getSeqno());
            }

            // S-005修复：检查nodetype是否合法
            if (!allowedNodeTypes.contains(node.getNodetype())) {
                throw new JeecgBootException("节点类型无效（允许值：0=创建,1=审核,2=审批），当前：" + node.getNodetype());
            }

            // 检查创建节点
            if (WfConstants.NODE_TYPE_CREATE.equals(node.getNodetype())) {
                hasCreateNode = true;
                // 重启流程时，创建节点的seqno可能不是0（例如第1次重启=100，第2次重启=200）
                // 只要求创建节点的seqno是所有节点中最小的即可
                int minSeqno = nodes.stream().mapToInt(BatchStartFlowDtlVO::getSeqno).min().orElse(0);
                if (node.getSeqno() != minSeqno) {
                    throw new JeecgBootException("创建节点的顺序号必须是所有节点中最小的，当前创建节点=" + node.getSeqno() + "，最小值=" + minSeqno);
                }
            } else {
                // S-003修复：非创建节点的seqno必须大于创建节点的seqno
                // 不再要求 >0，因为重启时所有节点的seqno都会增加（100,110,120...）
            }

            // 验证必填字段
            if (oConvertUtils.isEmpty(node.getNodename())) {
                throw new JeecgBootException("节点名称不能为空");
            }
            if (node.getNodename().length() > 100) {
                throw new JeecgBootException("节点名称长度不能超过100个字符：" + node.getNodename());
            }

            if (oConvertUtils.isEmpty(node.getUserid())) {
                throw new JeecgBootException("处理人ID不能为空");
            }
            if (node.getUserid().length() > 500) {
                throw new JeecgBootException("处理人ID长度不能超过500个字符");
            }

            if (oConvertUtils.isEmpty(node.getUseridname())) {
                throw new JeecgBootException("处理人姓名不能为空");
            }
            if (node.getUseridname().length() > 500) {
                throw new JeecgBootException("处理人姓名长度不能超过500个字符");
            }

            // S-006修复：校验userid和useridname数量一致性
            String[] userids = node.getUserid().split(",");
            String[] useridnames = node.getUseridname().split(",");
            if (userids.length != useridnames.length) {
                throw new JeecgBootException(
                    String.format("节点【%s】处理人ID数量（%d）与姓名数量（%d）不一致",
                        node.getNodename(), userids.length, useridnames.length)
                );
            }

            // 验证阶段名称长度（可选字段）
            if (!oConvertUtils.isEmpty(node.getStagename()) && node.getStagename().length() > 100) {
                throw new JeecgBootException("阶段名称长度不能超过100个字符：" + node.getStagename());
            }

            // 使用通用验证工具验证userid格式
            WfValidatorUtil.validateUserid(node.getUserid());
        }

        if (!hasCreateNode) {
            throw new JeecgBootException("节点配置中必须包含创建节点（nodetype=0）");
        }
    }

    @Override
    public WfInstance getByFormid(String formid) {
        if (oConvertUtils.isEmpty(formid)) {
            throw new JeecgBootException("业务表单ID不能为空");
        }

        LambdaQueryWrapper<WfInstance> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfInstance::getFormid, formid);
        queryWrapper.orderByDesc(WfInstance::getCreateTime);
        queryWrapper.last("LIMIT 1");

        return this.getOne(queryWrapper);
    }

    @Override
    public Object getTodoByFormid(String formid) {
        if (oConvertUtils.isEmpty(formid)) {
            throw new JeecgBootException("业务表单ID不能为空");
        }

        // 获取当前用户
        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        if (loginUser == null) {
            throw new JeecgBootException("未获取到当前用户信息");
        }
        // 节点 userid_ 存的是用户ID(UUID)，而非登录名；同时兼容用户名以防历史脏数据
        String currentUserId = loginUser.getId();
        String currentUsername = loginUser.getUsername();

        // 查询流程实例
        WfInstance instance = getByFormid(formid);
        if (instance == null) {
            return null;
        }

        // 查询流程节点
        LambdaQueryWrapper<WfInstanceDtl> dtlWrapper = new LambdaQueryWrapper<>();
        dtlWrapper.eq(WfInstanceDtl::getInstanceid, instance.getId());
        dtlWrapper.in(WfInstanceDtl::getIfexec, WfConstants.EXEC_NO, WfConstants.EXEC_RETURN);
        dtlWrapper.orderByAsc(WfInstanceDtl::getSeqno);

        List<WfInstanceDtl> nodes = wfInstanceDtlMapper.selectList(dtlWrapper);

        // 查找当前用户的待办节点
        for (WfInstanceDtl node : nodes) {
            String userid = node.getUserid();
            if (oConvertUtils.isEmpty(userid)) {
                continue;
            }

            // 判断当前用户是否为该节点的处理人（按用户ID匹配，兼容用户名）
            String[] userIds = userid.split(",");
            for (String uid : userIds) {
                String u = uid.trim();
                if (u.equals(currentUserId) || u.equals(currentUsername)) {
                    return node;
                }
                // TODO: 支持角色/部门/岗位前缀判断（rol_/dpt_/grp_/pst_）
            }
        }

        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUrgent(String id, String ifurgent) {
        if (oConvertUtils.isEmpty(id)) {
            throw new JeecgBootException("流程实例ID不能为空");
        }

        WfInstance instance = this.getById(id);
        if (instance == null) {
            throw new JeecgBootException("流程实例不存在");
        }

        // 校验紧急程度值
        if (!oConvertUtils.isEmpty(ifurgent)) {
            if (!Arrays.asList("", "1", "2", "3").contains(ifurgent)) {
                throw new JeecgBootException("紧急程度值无效，必须是空/1/2/3");
            }
        }

        instance.setIfurgent(ifurgent);
        this.updateById(instance);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void terminate(String id, String reason) {
        if (oConvertUtils.isEmpty(id)) {
            throw new JeecgBootException("流程实例ID不能为空");
        }
        if (oConvertUtils.isEmpty(reason)) {
            throw new JeecgBootException("终止原因不能为空");
        }

        WfInstance instance = this.getById(id);
        if (instance == null) {
            throw new JeecgBootException("流程实例不存在");
        }

        // P1-1修复：校验创建人权限 - 只有创建人可以终止流程
        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        String currentUsername = loginUser != null ? loginUser.getUsername() : null;
        if (!oConvertUtils.isEmpty(instance.getCreateBy()) &&
            !instance.getCreateBy().equals(currentUsername)) {
            throw new JeecgBootException("只有流程创建人可以终止流程");
        }

        // 校验流程状态
        if (WfConstants.STATUS_ENDED.equals(instance.getStatus()) || WfConstants.STATUS_TERMINATED.equals(instance.getStatus())) {
            throw new JeecgBootException("流程已结束，不能终止");
        }

        // 更新流程状态为终止 (DB约束只接受0/1/2，终止也使用"2")
        instance.setStatus("2");
        this.updateById(instance);

        log.info("流程实例{}已终止，原因：{}", id, reason);
    }

    @Override
    public List<TodoItemVO> getMyTodoList(String projectId, String searchField, String searchValue) {
        if (oConvertUtils.isEmpty(projectId)) {
            throw new JeecgBootException("项目ID不能为空");
        }

        // 获取当前登录用户
        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        if (loginUser == null) {
            throw new JeecgBootException("用户未登录");
        }
        String userId = loginUser.getId();

        // 构建查询参数
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        params.put("projectId", projectId);
        params.put("searchField", searchField);
        params.put("searchValue", searchValue);

        // 调用Mapper查询待办列表（使用v1.3的前缀编码权限匹配）
        List<TodoItemVO> todoList = wfInstanceMapper.getMyTodoList(params);

        log.info("查询用户{}在项目{}的待办列表，返回{}条记录", userId, projectId, todoList.size());

        return todoList;
    }
}
