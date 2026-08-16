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
import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDataModule;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.mapper.IetmDataModuleMapper;
import org.jeecg.modules.ietm.workflow.entity.WfInstance;
import org.jeecg.modules.ietm.workflow.entity.WfInstanceDtl;
import org.jeecg.modules.ietm.workflow.mapper.WfInstanceDtlMapper;
import org.jeecg.modules.ietm.workflow.mapper.WfInstanceMapper;
import org.jeecg.modules.ietm.workflow.service.IWfInstanceService;
import org.jeecg.modules.ietm.workflow.constants.WfConstants;
import org.jeecg.modules.ietm.workflow.util.WfValidatorUtil;
import org.jeecg.modules.ietm.workflow.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            Map<String, IetmDataModule> dmMap = validateAndLoadDms(dmIds);
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
            // 释放分布式锁
            if (locked && wfLockUtil != null) {
                wfLockUtil.unlock(lockKey);
                log.debug("释放分布式锁，lockKey：{}", lockKey);
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

            // 构建节点明细
            for (BatchStartFlowDtlVO nodeConfig : vo.getNodes()) {
                WfInstanceDtl dtl = new WfInstanceDtl();
                dtl.setId(String.valueOf(IdWorker.getId()));  // K-003修复：生成雪花ID
                dtl.setInstanceid(instanceId);
                dtl.setSeqno(nodeConfig.getSeqno());
                dtl.setNodename(nodeConfig.getNodename());
                dtl.setNodetype(nodeConfig.getNodetype());
                dtl.setUserid(nodeConfig.getUserid());
                dtl.setUseridname(nodeConfig.getUseridname());
                dtl.setStagename(oConvertUtils.getString(nodeConfig.getStagename(), ""));
                dtl.setIfgetback(oConvertUtils.getString(nodeConfig.getIfgetback(), ""));
                dtl.setIfexec(WfConstants.EXEC_NO);
                dtl.setCreateBy(currentUsername);  // P0-005修复：设置创建人
                dtl.setCreateTime(new Date());      // P0-005修复：设置创建时间
                allDtlList.add(dtl);
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
        if (!dmToTodoJsonMap.isEmpty()) {
            // 由于attribute_05每条DM的值不同，无法使用单一UPDATE语句
            // 采用批量收集+循环更新的方式（已是当前场景下的最优方案）
            for (Map.Entry<String, String> entry : dmToTodoJsonMap.entrySet()) {
                String dmId = entry.getKey();
                IetmDataModule dm = dmMap.get(dmId);
                String dmcCode = oConvertUtils.getString(dm.getDmcCode(), dmId);

                // 修复：使用workflowStatus字段表示流程状态
                // status字段含义：0=已删除，1=草稿/编辑中，2=已发布
                // workflowStatus字段含义：null/空=未启动，0=已结束，1=流转中，2=已撤销
                String currentWorkflowStatus = dm.getWorkflowStatus();

                // ===== 诊断：获取要更新的用户名 =====
                String usernameValue = entry.getValue();
                log.info("========== 准备更新数据库 ==========");
                log.info("DM ID: {}", dmId);
                log.info("DMC编码: {}", dmcCode);
                log.info("attribute_05值（待办用户名）: {}", usernameValue);
                log.info("attribute_05长度: {} 字符", usernameValue.length());
                log.info("attribute_05字节数: {} 字节", usernameValue.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
                log.info("当前DM状态:");
                log.info("  - status: {}", dm.getStatus());
                log.info("  - workflowStatus: {}", currentWorkflowStatus);
                log.info("====================================");

                // 乐观锁：只更新workflowStatus为空或0的记录（未启动或已结束）
                LambdaUpdateWrapper<IetmDataModule> updateWrapper = new LambdaUpdateWrapper<IetmDataModule>()
                    .eq(IetmDataModule::getId, dmId)
                    .eq(IetmDataModule::getStatus, "1")  // status='1'表示草稿/编辑中状态
                    .set(IetmDataModule::getAttribute05, usernameValue)  // 存储用户名（纯文本）
                    .set(IetmDataModule::getWorkflowStatus, "1");  // 更新workflowStatus为"1"（流转中）
                    // 方案A：不回写 workflow_instance_id、workflow_step、workflow_handler
                    // 这些字段从 v_wf_instance 视图动态查询，通过 wf_instance.formid_ 关联

                // 添加workflowStatus条件：必须为null、空或0
                if (currentWorkflowStatus == null || "".equals(currentWorkflowStatus)) {
                    updateWrapper.and(w -> w.isNull(IetmDataModule::getWorkflowStatus).or().eq(IetmDataModule::getWorkflowStatus, ""));
                } else if ("0".equals(currentWorkflowStatus)) {
                    updateWrapper.eq(IetmDataModule::getWorkflowStatus, "0");
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

        // ============ 第2步：获取登录用户（参数校验之后） ============
        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        String currentUsername = loginUser.getUsername();

        // ===== 前置校验（事务内，但操作很快） =====
        long validationStartTime = System.currentTimeMillis();
        log.debug("开始前置校验...");  // R-005修复
        Map<String, IetmDataModule> dmMap = new HashMap<>();

        // Step1: 校验所有DM是否已发布且流程已结束
        for (BatchRestartDataVO restartData : dataList) {
            IetmDataModule dm = ietmDataModuleMapper.selectById(restartData.getDmId());
            if (dm == null) {
                throw new JeecgBootException("DM不存在，ID：" + restartData.getDmId());
            }

            // 校验DM状态是否为已发布
            if (!WfConstants.STATUS_ENDED.equals(dm.getStatus())) {
                String dmcCode = oConvertUtils.getString(dm.getDmcCode(), restartData.getDmId());
                throw new JeecgBootException("DM未发布，无法重启流程，DMC：" + dmcCode);
            }

            // 校验旧实例是否已结束
            String oldInstanceId = restartData.getOldInstanceId();
            if (oConvertUtils.isEmpty(oldInstanceId)) {
                log.warn("DM[{}]未提供旧实例ID，跳过校验", restartData.getDmId());
                continue;
            }

            WfInstance oldInstance = wfInstanceMapper.selectById(oldInstanceId);
            if (oldInstance == null) {
                throw new JeecgBootException("旧流程实例不存在，ID：" + oldInstanceId);
            }
            if (!WfConstants.STATUS_ENDED.equals(oldInstance.getStatus())) {
                String dmcCode = oConvertUtils.getString(dm.getDmcCode(), restartData.getDmId());
                throw new JeecgBootException("旧流程未结束，无法重启，DMC：" + dmcCode);
            }

            dmMap.put(restartData.getDmId(), dm);
        }
        long validationTime = System.currentTimeMillis() - validationStartTime;
        log.debug("前置校验完成，耗时：{}ms", validationTime);

        // ===== 执行数据修改 =====
        int result = doRestartFlowInTransaction(vo, dmMap, currentUsername);

        long totalTime = System.currentTimeMillis() - startTime;
        log.info("批量重启流程成功，批次ID：{}，成功数量：{}，总耗时：{}ms", batchId, result, totalTime);

        return result;
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
        Map<String, String> allTodoJsonMap = new HashMap<>();

        for (BatchRestartDataVO restartData : dataList) {
            IetmDataModule dm = dmMap.get(restartData.getDmId());

            // 收集要终止的实例ID
            allTerminateIds.add(restartData.getOldInstanceId());

            // 构建新实例
            WfInstance newInstance = new WfInstance();
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
            newInstance.setCreateBy(currentUsername);  // P0-005修复：设置创建人
            newInstance.setCreateTime(new Date());      // P0-005修复：设置创建时间
            allNewInstances.add(newInstance);
        }
        long collectTime = System.currentTimeMillis() - collectStartTime;
        log.debug("数据收集完成，耗时：{}ms", collectTime);

        // ===== 批量执行：终止旧实例 =====
        long terminateStartTime = System.currentTimeMillis();
        if (!allTerminateIds.isEmpty()) {
            wfInstanceMapper.batchTerminate(allTerminateIds, currentUsername);  // P0-004修复：传递update_by
        }
        long terminateTime = System.currentTimeMillis() - terminateStartTime;
        log.debug("批量终止旧实例完成，数量：{}，耗时：{}ms", allTerminateIds.size(), terminateTime);

        // ===== 批量执行：插入新实例 =====
        long insertStartTime = System.currentTimeMillis();
        for (WfInstance instance : allNewInstances) {
            wfInstanceMapper.insert(instance);
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

            // 构建节点明细（seqno+100偏移）
            for (BatchStartFlowDtlVO dtlVO : vo.getNodes()) {
                WfInstanceDtl dtl = new WfInstanceDtl();
                dtl.setId(String.valueOf(IdWorker.getId()));  // K-003修复：生成雪花ID
                dtl.setInstanceid(newInstanceId);
                dtl.setSeqno(dtlVO.getSeqno() + WfConstants.SEQNO_OFFSET);
                dtl.setNodename(dtlVO.getNodename());
                dtl.setNodetype(dtlVO.getNodetype());
                dtl.setUserid(dtlVO.getUserid());
                dtl.setUseridname(dtlVO.getUseridname());
                dtl.setStagename(oConvertUtils.getString(dtlVO.getStagename(), ""));
                dtl.setIfexec(WfConstants.EXEC_NO);
                dtl.setIfgetback(oConvertUtils.getString(dtlVO.getIfgetback(), ""));
                dtl.setCreateBy(currentUsername);  // P0-005修复：设置创建人
                dtl.setCreateTime(new Date());      // P0-005修复：设置创建时间
                allDtls.add(dtl);
            }

            // P0-002修复：从内存构建待办JSON
            BatchStartFlowDtlVO firstNode = vo.getNodes().stream()
                .filter(n -> WfConstants.NODE_TYPE_CREATE.equals(n.getNodetype()))
                .findFirst()
                .orElse(null);
            if (firstNode != null) {
                JSONObject json = new JSONObject();
                json.put("instanceId", newInstanceId);
                json.put("nodename", firstNode.getNodename());
                json.put("userid", firstNode.getUserid());
                json.put("useridname", firstNode.getUseridname());
                json.put("seqno", firstNode.getSeqno() + WfConstants.SEQNO_OFFSET);
                json.put("nodetype", firstNode.getNodetype());
                allTodoJsonMap.put(restartData.getDmId(), json.toJSONString());
            }
        }
        long dtlCollectTime = System.currentTimeMillis() - dtlCollectStartTime;
        log.debug("节点明细收集完成，节点总数：{}，耗时：{}ms", allDtls.size(), dtlCollectTime);

        // ===== 批量执行：插入节点明细 =====
        long dtlInsertStartTime = System.currentTimeMillis();
        if (!allDtls.isEmpty()) {
            wfInstanceDtlMapper.batchInsert(allDtls);
        }
        long dtlInsertTime = System.currentTimeMillis() - dtlInsertStartTime;
        log.debug("批量插入节点明细完成，耗时：{}ms", dtlInsertTime);

        // ===== 批量更新：创建节点状态 =====
        long updateStartTime = System.currentTimeMillis();
        if (!newInstanceIds.isEmpty()) {
            wfInstanceDtlMapper.update(null,
                new LambdaUpdateWrapper<WfInstanceDtl>()
                    .in(WfInstanceDtl::getInstanceid, newInstanceIds)
                    .eq(WfInstanceDtl::getNodetype, WfConstants.NODE_TYPE_CREATE)
                    .set(WfInstanceDtl::getIfexec, WfConstants.EXEC_YES)
            );
        }

        // ===== 批量更新：DM的attribute_05、status和workflowStatus =====
        // S-007修复：添加乐观锁，防止并发冲突
        for (int i = 0; i < dataList.size(); i++) {
            BatchRestartDataVO restartData = dataList.get(i);
            String dmId = restartData.getDmId();
            IetmDataModule dm = dmMap.get(dmId);
            String dmcCode = oConvertUtils.getString(dm.getDmcCode(), dmId);

            String todoJson = allTodoJsonMap.get(dmId);

            // S-007修复：使用乐观锁更新，确保DM状态正确
            // 重启流程时，DM状态应该是2（已发布），更新为1（审批中）
            int rows = ietmDataModuleMapper.update(null,
                new LambdaUpdateWrapper<IetmDataModule>()
                    .eq(IetmDataModule::getId, dmId)
                    .eq(IetmDataModule::getStatus, "2")  // S-007修复：乐观锁，只更新status=2的记录
                    .set(IetmDataModule::getAttribute05, todoJson != null ? todoJson : "")
                    .set(IetmDataModule::getStatus, "1")  // P0-003修复：更新DM状态为审批中
                    .set(IetmDataModule::getWorkflowStatus, "1")  // 更新workflowStatus为"1"（流转中）
                    // 方案A：不回写 workflow_instance_id，通过 wf_instance.formid_ 反向关联
            );

            // S-007修复：如果更新失败，说明DM状态已被其他用户修改
            if (rows == 0) {
                throw new JeecgBootException("DM状态已被其他用户修改，请刷新后重试，DMC：" + dmcCode);
            }
        }
        long updateTime = System.currentTimeMillis() - updateStartTime;
        log.debug("批量更新状态完成，耗时：{}ms", updateTime);

        log.debug("性能分析 - 收集：{}ms, 终止：{}ms, 插入实例：{}ms, 插入明细：{}ms, 更新：{}ms",
                collectTime, terminateTime, insertTime, dtlInsertTime, updateTime);

        return dataList.size();
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
     * 批量查询DM并验证存在性、状态合法性，将结果放入Map便于后续快速访问。
     * 该方法在事务外调用，避免长时间持有事务锁。
     * </p>
     *
     * @param dmIds DM ID列表
     * @return DM映射表，Key为DM ID，Value为DM实体
     * @throws JeecgBootException 当存在不存在的DM、状态不合法时抛出
     *
     * <p><b>S-001修复：添加DM状态校验</b></p>
     * <ul>
     *   <li>只有草稿状态（status=0）的DM才能启动流程</li>
     *   <li>审批中（status=1）的DM拒绝启动</li>
     *   <li>已发布（status=2）的DM应使用重启流程功能</li>
     * </ul>
     *
     * @since 1.0
     */
    private Map<String, IetmDataModule> validateAndLoadDms(List<String> dmIds) {
        Map<String, IetmDataModule> dmMap = new HashMap<>();
        for (String dmId : dmIds) {
            IetmDataModule dm = ietmDataModuleMapper.selectById(dmId);
            if (dm == null) {
                throw new JeecgBootException("DM不存在，ID：" + dmId);
            }

            String dmcCode = oConvertUtils.getString(dm.getDmcCode(), dmId);

            // S-001修复：检查DM状态是否允许启动流程
            // 注意：status字段含义为（0=已删除，1=正常），不是流程状态
            // 流程状态应该检查workflowStatus字段
            String dmStatus = dm.getStatus();
            if ("0".equals(dmStatus)) {
                throw new JeecgBootException("DM已删除，无法启动流程，DMC：" + dmcCode);
            }

            // 检查工作流状态：只有未启动流程或流程已结束的才能启动新流程
            String workflowStatus = dm.getWorkflowStatus();
            if (workflowStatus != null && !"".equals(workflowStatus)) {
                if ("1".equals(workflowStatus)) {
                    throw new JeecgBootException("DM处于审批中，无法启动新流程，DMC：" + dmcCode);
                } else if ("2".equals(workflowStatus)) {
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
        Set<String> allowedNodeTypes = new HashSet<>(Arrays.asList("0", "1", "2"));

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
                if (node.getSeqno() != 0) {
                    throw new JeecgBootException("创建节点的顺序号必须为0");
                }
                hasCreateNode = true;
            } else {
                // S-003修复：非创建节点的seqno必须大于0
                if (node.getSeqno() <= 0) {
                    throw new JeecgBootException("非创建节点的顺序号必须大于0，节点：" + node.getNodename());
                }
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
}
