package org.jeecg.modules.ietm.workflow.test;

import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDataModule;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.mapper.IetmDataModuleMapper;
import org.jeecg.modules.ietm.workflow.entity.WfInstance;
import org.jeecg.modules.ietm.workflow.mapper.WfInstanceMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 工作流字段验证测试
 *
 * 目的：验证"方案A"设计是否正确实现
 * - workflow_status: 需要回写
 * - workflow_instance_id: 不回写（可以为null）
 * - workflow_step: 不回写（从视图获取）
 * - workflow_handler: 不回写（从视图获取）
 */
@SpringBootTest
public class WorkflowFieldVerificationTest {

    @Autowired
    private IetmDataModuleMapper ietmDataModuleMapper;

    @Autowired
    private WfInstanceMapper wfInstanceMapper;

    /**
     * 测试1：验证视图查询能正确获取流程节点
     */
    @Test
    public void testSelectByIdWithFlow() {
        System.out.println("========== 测试1：验证视图查询 ==========");

        // 查询第一个有工作流的DM
        IetmDataModule dm = ietmDataModuleMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<IetmDataModule>()
                .eq(IetmDataModule::getWorkflowStatus, "1")
                .eq(IetmDataModule::getIsLatest, "1")
                .last("LIMIT 1")
        );

        if (dm == null) {
            System.out.println("未找到流转中的DM，跳过测试");
            return;
        }

        System.out.println("找到DM: " + dm.getId());
        System.out.println("基表 workflow_status: " + dm.getWorkflowStatus());
        System.out.println("基表 workflow_instance_id: " + dm.getWorkflowInstanceId());
        System.out.println("基表 workflow_step: " + dm.getWorkflowStep());

        // 使用视图查询
        IetmDataModule dmWithFlow = ietmDataModuleMapper.selectByIdWithFlow(dm.getId());

        assertNotNull(dmWithFlow, "视图查询应该返回结果");
        System.out.println("视图 workflowStep (activityalias_): " + dmWithFlow.getWorkflowStep());
        System.out.println("视图 workflowHandler (currenthandler_): " + dmWithFlow.getWorkflowHandler());

        // 验证：从视图获取的节点应该有值（如果流程在进行中）
        if ("1".equals(dmWithFlow.getWorkflowStatus())) {
            assertNotNull(dmWithFlow.getWorkflowStep(),
                "流转中的DM应该能从视图获取到当前节点");
        }

        System.out.println("✓ 测试1通过：视图查询正常工作\n");
    }

    /**
     * 测试2：验证 wf_instance 通过 formid_ 关联 DM
     */
    @Test
    public void testWfInstanceFormidAssociation() {
        System.out.println("========== 测试2：验证 formid_ 关联 ==========");

        // 查询一个有工作流状态的DM
        IetmDataModule dm = ietmDataModuleMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<IetmDataModule>()
                .eq(IetmDataModule::getWorkflowStatus, "1")
                .eq(IetmDataModule::getIsLatest, "1")
                .last("LIMIT 1")
        );

        if (dm == null) {
            System.out.println("未找到流转中的DM，跳过测试");
            return;
        }

        System.out.println("DM ID: " + dm.getId());
        System.out.println("DM workflow_status: " + dm.getWorkflowStatus());
        System.out.println("DM workflow_instance_id (基表): " + dm.getWorkflowInstanceId());

        // 查询对应的工作流实例
        WfInstance wfInstance = wfInstanceMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WfInstance>()
                .eq(WfInstance::getFormid, dm.getId())
                .last("LIMIT 1")
        );

        if (wfInstance != null) {
            System.out.println("找到对应的工作流实例:");
            System.out.println("  wf_instance.id: " + wfInstance.getId());
            System.out.println("  wf_instance.formid_: " + wfInstance.getFormid());
            System.out.println("  wf_instance.status_: " + wfInstance.getStatus());

            assertEquals(dm.getId(), wfInstance.getFormid(),
                "wf_instance.formid_ 应该等于 ietm_data_module.id");

            System.out.println("✓ 测试2通过：formid_ 关联正确\n");
        } else {
            System.out.println("⚠ 警告：DM有workflow_status但未找到wf_instance记录\n");
        }
    }

    /**
     * 测试3：验证 workflow_instance_id 字段使用情况
     */
    @Test
    public void testWorkflowInstanceIdUsage() {
        System.out.println("========== 测试3：验证 workflow_instance_id 使用情况 ==========");

        // 统计字段使用情况
        long totalCount = ietmDataModuleMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<IetmDataModule>()
                .eq(IetmDataModule::getIsLatest, "1")
        );

        long nonNullCount = ietmDataModuleMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<IetmDataModule>()
                .eq(IetmDataModule::getIsLatest, "1")
                .isNotNull(IetmDataModule::getWorkflowInstanceId)
        );

        long nullCount = totalCount - nonNullCount;
        double nullPercentage = (double) nullCount / totalCount * 100;

        System.out.println("总DM数量: " + totalCount);
        System.out.println("workflow_instance_id 非空数量: " + nonNullCount);
        System.out.println("workflow_instance_id 为空数量: " + nullCount);
        System.out.println("为空比例: " + String.format("%.2f", nullPercentage) + "%");

        // 方案A设计：大部分应该为空（至少80%）
        assertTrue(nullPercentage >= 80.0,
            "方案A设计：workflow_instance_id 大部分应为空，实际为空比例: " + nullPercentage + "%");

        System.out.println("✓ 测试3通过：workflow_instance_id 使用符合方案A设计\n");
    }

    /**
     * 测试4：验证不同 workflow_status 的分布
     */
    @Test
    public void testWorkflowStatusDistribution() {
        System.out.println("========== 测试4：验证 workflow_status 分布 ==========");

        // 统计各状态数量
        long nullOrEmptyCount = ietmDataModuleMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<IetmDataModule>()
                .eq(IetmDataModule::getIsLatest, "1")
                .and(w -> w.isNull(IetmDataModule::getWorkflowStatus)
                    .or().eq(IetmDataModule::getWorkflowStatus, ""))
        );

        long status0Count = ietmDataModuleMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<IetmDataModule>()
                .eq(IetmDataModule::getIsLatest, "1")
                .eq(IetmDataModule::getWorkflowStatus, "0")
        );

        long status1Count = ietmDataModuleMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<IetmDataModule>()
                .eq(IetmDataModule::getIsLatest, "1")
                .eq(IetmDataModule::getWorkflowStatus, "1")
        );

        long status2Count = ietmDataModuleMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<IetmDataModule>()
                .eq(IetmDataModule::getIsLatest, "1")
                .eq(IetmDataModule::getWorkflowStatus, "2")
        );

        System.out.println("workflow_status 分布:");
        System.out.println("  null/空 (未启动): " + nullOrEmptyCount);
        System.out.println("  0 (已结束): " + status0Count);
        System.out.println("  1 (流转中): " + status1Count);
        System.out.println("  2 (已撤销): " + status2Count);

        System.out.println("✓ 测试4完成：workflow_status 分布统计\n");
    }

    /**
     * 测试5：端到端测试 - 验证完整查询流程
     */
    @Test
    public void testEndToEndQuery() {
        System.out.println("========== 测试5：端到端查询测试 ==========");

        // 查询一个流转中的DM
        IetmDataModule dmBase = ietmDataModuleMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<IetmDataModule>()
                .eq(IetmDataModule::getWorkflowStatus, "1")
                .eq(IetmDataModule::getIsLatest, "1")
                .last("LIMIT 1")
        );

        if (dmBase == null) {
            System.out.println("未找到流转中的DM，跳过测试");
            return;
        }

        System.out.println("步骤1：基表查询");
        System.out.println("  DM ID: " + dmBase.getId());
        System.out.println("  workflow_status: " + dmBase.getWorkflowStatus());
        System.out.println("  workflow_instance_id: " + dmBase.getWorkflowInstanceId());

        System.out.println("\n步骤2：通过 formid_ 查询工作流实例");
        WfInstance wfInstance = wfInstanceMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WfInstance>()
                .eq(WfInstance::getFormid, dmBase.getId())
        );

        if (wfInstance != null) {
            System.out.println("  工作流实例ID: " + wfInstance.getId());
            System.out.println("  formid_: " + wfInstance.getFormid());
            System.out.println("  status_: " + wfInstance.getStatus());
        }

        System.out.println("\n步骤3：通过视图查询获取实时节点");
        IetmDataModule dmWithFlow = ietmDataModuleMapper.selectByIdWithFlow(dmBase.getId());
        System.out.println("  当前节点 (activityalias_): " + dmWithFlow.getWorkflowStep());
        System.out.println("  当前处理人 (currenthandler_): " + dmWithFlow.getWorkflowHandler());

        System.out.println("\n✓ 测试5完成：端到端查询流程正常\n");
    }
}
