package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDataModule;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.service.IIetmDataModuleService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * 查询DM记录并诊断编辑按钮状态
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class DiagnoseButtonStateTest {

    @Autowired
    private IIetmDataModuleService dataModuleService;

    @Test
    public void testDiagnoseButtonState() {
        String dmId = "2088664648432721921";

        IetmDataModule record = dataModuleService.getById(dmId);

        if (record == null) {
            System.out.println("未找到 ID = " + dmId + " 的记录");
            return;
        }

        System.out.println("=== DM记录查询结果 ===");
        System.out.println("ID: " + record.getId());
        System.out.println("DMC编码: " + record.getDmcCode());
        System.out.println("工作流实例ID: " + record.getWorkflowInstanceId());
        System.out.println("工作流节点: " + record.getWorkflowStep());
        System.out.println("签出用户: " + record.getCheckoutUser());
        System.out.println("版本类型: " + record.getVersionType());
        System.out.println("issue_no: " + record.getIssueNo());
        System.out.println("in_work: " + record.getInWork());

        System.out.println("\n=== 编辑按钮启用条件检查 ===");

        boolean hasWorkflowStarted = record.getWorkflowInstanceId() != null && !record.getWorkflowInstanceId().trim().isEmpty();
        boolean isDmWriteStep = "DM编写".equals(record.getWorkflowStep());
        boolean notCheckedOutByOthers = record.getCheckoutUser() == null || record.getCheckoutUser().trim().isEmpty();

        System.out.println("✓ 工作流已启动: " + hasWorkflowStarted + (hasWorkflowStarted ? "" : " ❌"));
        System.out.println("✓ 当前节点为DM编写: " + isDmWriteStep + (isDmWriteStep ? "" : " ❌ (实际值: " + record.getWorkflowStep() + ")"));
        System.out.println("✓ 未被他人签出: " + notCheckedOutByOthers + (notCheckedOutByOthers ? "" : " ❌ (签出人: " + record.getCheckoutUser() + ")"));

        boolean canEditProp = hasWorkflowStarted && isDmWriteStep && notCheckedOutByOthers;
        System.out.println("\n最终结果: canEditProp = " + canEditProp);

        if (!canEditProp) {
            System.out.println("\n【原因分析】编辑按钮被禁用，因为:");
            if (!hasWorkflowStarted) {
                System.out.println("  → 工作流未启动，需要先点击【启动流程】按钮");
            }
            if (!isDmWriteStep) {
                System.out.println("  → 当前流程节点不是'DM编写'，当前节点为: " + (record.getWorkflowStep() == null ? "null" : record.getWorkflowStep()));
            }
            if (!notCheckedOutByOthers) {
                System.out.println("  → 已被用户 '" + record.getCheckoutUser() + "' 签出");
            }
        } else {
            System.out.println("\n✅ 编辑按钮应该是启用状态！");
        }
    }
}
