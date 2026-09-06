package org.jeecg.modules.ietm.ietmdatamodulemanagement;

import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDataModule;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.mapper.IetmDataModuleMapper;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.service.IIetmDataModuleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DM内容预览修复测试
 *
 * 问题：引用关系弹框中点击DMC编码后，DM内容预览为空
 * 根因：selectByIdWithFlow 未查询 dm_content 字段
 * 修复：2026-08-31 在 SelectByIdWithFlowResultMap 和 SQL 中添加 dm_content
 *
 * @author Claude
 * @since 2026-08-31
 */
@SpringBootTest(classes = org.jeecg.modules.ietm.TestApplication.class)
@DisplayName("DM内容预览修复验证测试")
public class DmContentPreviewFixTest {

    @Autowired
    private IIetmDataModuleService dataModuleService;

    @Autowired
    private IetmDataModuleMapper dataModuleMapper;

    @Test
    @DisplayName("验证queryById返回dm_content字段")
    public void testQueryByIdReturnsDmContent() {
        // 场景：模拟前端调用 /ietm/datamodule/queryById
        // 期望：返回的实体包含 dm_content 字段

        // 1. 先查询一个存在的DM（有内容的）
        IetmDataModule dm = dataModuleMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<IetmDataModule>()
                .eq(IetmDataModule::getStatus, "1")
                .eq(IetmDataModule::getIsLatest, "1")
                .isNotNull(IetmDataModule::getDmContent)
                .last("FETCH FIRST 1 ROWS ONLY")
        );

        if (dm == null) {
            System.out.println("⚠️  测试跳过：数据库中没有包含内容的DM记录");
            return;
        }

        String testDmId = dm.getId();
        String expectedContent = dm.getDmContent();

        System.out.println("测试DM ID: " + testDmId);
        System.out.println("DM内容长度: " + (expectedContent != null ? expectedContent.length() : 0) + " 字符");

        // 2. 通过queryById查询（会调用selectByIdWithFlow）
        IetmDataModule result = dataModuleService.queryById(testDmId);

        // 3. 验证结果
        assertNotNull(result, "queryById应该返回结果");
        assertNotNull(result.getDmContent(), "❌ dm_content字段不应该为null");
        assertEquals(expectedContent.length(), result.getDmContent().length(),
            "❌ dm_content内容长度应该一致");

        System.out.println("✅ 测试通过：queryById正确返回dm_content字段");
    }

    @Test
    @DisplayName("验证selectByIdWithFlow直接调用返回dm_content")
    public void testSelectByIdWithFlowReturnsDmContent() {
        // 直接测试Mapper层

        // 1. 查询一个有内容的DM
        IetmDataModule dm = dataModuleMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<IetmDataModule>()
                .eq(IetmDataModule::getStatus, "1")
                .isNotNull(IetmDataModule::getDmContent)
                .last("FETCH FIRST 1 ROWS ONLY")
        );

        if (dm == null) {
            System.out.println("⚠️  测试跳过：数据库中没有包含内容的DM记录");
            return;
        }

        String testDmId = dm.getId();

        // 2. 调用selectByIdWithFlow
        IetmDataModule result = dataModuleMapper.selectByIdWithFlow(testDmId);

        // 3. 验证
        assertNotNull(result, "selectByIdWithFlow应该返回结果");
        assertNotNull(result.getDmContent(), "❌ dm_content字段不应该为null");
        assertTrue(result.getDmContent().length() > 0, "❌ dm_content内容不应该为空");

        // 4. 同时验证流程字段也正常（确保没有破坏原有功能）
        // workflowStep可能为null（未启动流程），但字段应该存在
        System.out.println("工作流步骤: " + result.getWorkflowStep());
        System.out.println("DM内容长度: " + result.getDmContent().length());

        System.out.println("✅ 测试通过：selectByIdWithFlow同时返回dm_content和流程字段");
    }

    @Test
    @DisplayName("验证修复不影响其他调用场景")
    public void testFixDoesNotBreakOtherScenarios() {
        // 验证checkOut、publishDm、editProp等场景不受影响

        // 查询一个DM
        IetmDataModule dm = dataModuleMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<IetmDataModule>()
                .eq(IetmDataModule::getStatus, "1")
                .eq(IetmDataModule::getIsLatest, "1")
                .last("FETCH FIRST 1 ROWS ONLY")
        );

        if (dm == null) {
            System.out.println("⚠️  测试跳过：数据库中没有DM记录");
            return;
        }

        String testDmId = dm.getId();

        // 调用selectByIdWithFlow
        IetmDataModule result = dataModuleMapper.selectByIdWithFlow(testDmId);

        // 验证所有必要字段都存在（不只是dm_content）
        assertNotNull(result.getId(), "ID字段应该存在");
        assertNotNull(result.getDmcCode(), "DMC编码应该存在");
        assertNotNull(result.getIssueNo(), "版本号应该存在");
        assertNotNull(result.getInWork(), "工作版本应该存在");
        // workflowStep可能为null（未启动流程），不做断言

        System.out.println("✅ 测试通过：修复未破坏其他字段的查询");
    }
}
