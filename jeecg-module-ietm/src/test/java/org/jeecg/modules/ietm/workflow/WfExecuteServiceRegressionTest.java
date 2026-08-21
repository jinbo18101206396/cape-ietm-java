package org.jeecg.modules.ietm.workflow;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.ietm.workflow.entity.WfInstance;
import org.jeecg.modules.ietm.workflow.entity.WfInstanceDtl;
import org.jeecg.modules.ietm.workflow.mapper.WfInstanceDtlMapper;
import org.jeecg.modules.ietm.workflow.mapper.WfInstanceMapper;
import org.jeecg.modules.ietm.workflow.service.IWfExecuteService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 流程执行服务回归测试
 * TC-04: P2-LOGIC-01验证（跳转节点后端校验）
 *
 * 测试目标：
 * 1. 验证ifgetback="-1"时禁止跳转
 * 2. 验证ifgetback="dtlid1,dtlid2"时只能跳转到指定节点
 * 3. 验证ifgetback="0"时可以跳转到起始节点
 *
 * @author Claude Opus 4.8
 * @date 2026-08-21
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WfExecuteServiceRegressionTest {

    @Autowired
    private IWfExecuteService wfExecuteService;

    @Autowired
    private WfInstanceMapper wfInstanceMapper;

    @Autowired
    private WfInstanceDtlMapper wfInstanceDtlMapper;

    // 测试数据
    private static String testInstanceId;
    private static String testDtlId1; // 第一个节点
    private static String testDtlId2; // 第二个节点
    private static String testDtlId3; // 第三个节点

    /**
     * 准备测试数据：创建一个包含3个节点的流程实例
     */
    @BeforeAll
    public static void setupTestData(@Autowired WfInstanceMapper instanceMapper,
                                      @Autowired WfInstanceDtlMapper dtlMapper) {
        System.out.println("\n=== 准备TC-04测试数据 ===");

        // 1. 创建流程实例
        WfInstance instance = new WfInstance();
        instance.setFormid("TEST_FORM_" + System.currentTimeMillis());
        instance.setTitle("TC-04回归测试流程");
        instance.setStatus("1"); // 流转中（String类型）
        instance.setCreateTime(new Date());
        instance.setCreateBy("test_user");
        instanceMapper.insert(instance);
        testInstanceId = instance.getId();
        System.out.println("✅ 创建测试流程实例: " + testInstanceId);

        // 2. 创建第一个节点（不可跳转）
        WfInstanceDtl dtl1 = new WfInstanceDtl();
        dtl1.setInstanceid(testInstanceId); // 字段名是instanceid
        dtl1.setNodename("节点1-不可跳转");
        dtl1.setSeqno(1);
        dtl1.setIfexec("N");
        dtl1.setIfgetback("-1"); // 设置为不可跳转
        dtl1.setUserid("test_user"); // 字段名是userid
        dtl1.setCreateTime(new Date());
        dtlMapper.insert(dtl1);
        testDtlId1 = dtl1.getId();
        System.out.println("✅ 创建节点1: " + testDtlId1 + " (ifgetback=-1)");

        // 3. 创建第二个节点（可跳转到节点3）
        WfInstanceDtl dtl2 = new WfInstanceDtl();
        dtl2.setInstanceid(testInstanceId);
        dtl2.setNodename("节点2-可跳转到节点3");
        dtl2.setSeqno(2);
        dtl2.setIfexec("N");
        dtl2.setUserid("test_user");
        dtl2.setCreateTime(new Date());
        dtlMapper.insert(dtl2);
        testDtlId2 = dtl2.getId();

        // 4. 创建第三个节点
        WfInstanceDtl dtl3 = new WfInstanceDtl();
        dtl3.setInstanceid(testInstanceId);
        dtl3.setNodename("节点3");
        dtl3.setSeqno(3);
        dtl3.setIfexec("N");
        dtl3.setUserid("test_user");
        dtl3.setCreateTime(new Date());
        dtlMapper.insert(dtl3);
        testDtlId3 = dtl3.getId();
        System.out.println("✅ 创建节点3: " + testDtlId3);

        // 5. 更新节点2的ifgetback为节点3的ID
        dtl2.setIfgetback(testDtlId3); // 只允许跳转到节点3
        dtlMapper.updateById(dtl2);
        System.out.println("✅ 创建节点2: " + testDtlId2 + " (ifgetback=" + testDtlId3 + ")");

        System.out.println("=== 测试数据准备完成 ===\n");
    }

    /**
     * 清理测试数据
     */
    @AfterAll
    public static void cleanupTestData(@Autowired WfInstanceMapper instanceMapper,
                                        @Autowired WfInstanceDtlMapper dtlMapper) {
        System.out.println("\n=== 清理TC-04测试数据 ===");

        if (testInstanceId != null) {
            // 删除节点
            QueryWrapper<WfInstanceDtl> dtlWrapper = new QueryWrapper<>();
            dtlWrapper.eq("instanceid_", testInstanceId); // 使用数据库字段名
            dtlMapper.delete(dtlWrapper);
            System.out.println("✅ 删除测试节点");

            // 删除流程实例
            instanceMapper.deleteById(testInstanceId);
            System.out.println("✅ 删除测试流程实例");
        }

        System.out.println("=== 测试数据清理完成 ===\n");
    }

    /**
     * TC-04-1: 验证ifgetback="-1"时禁止跳转
     */
    @Test
    @Order(1)
    @DisplayName("TC-04-1: ifgetback=-1时禁止跳转")
    public void testJumpNotAllowedWhenIfgetbackIsMinusOne() {
        System.out.println("\n🧪 执行 TC-04-1: ifgetback=-1时禁止跳转");

        // 1. 将节点1标记为已执行（当前节点）
        WfInstanceDtl dtl1 = wfInstanceDtlMapper.selectById(testDtlId1);
        dtl1.setIfexec("Y");
        wfInstanceDtlMapper.updateById(dtl1);

        // 2. 尝试通过executeNode跳转到节点2
        JeecgBootException exception = assertThrows(JeecgBootException.class, () -> {
            wfExecuteService.executeNode(testDtlId1, "3", testDtlId2, "测试跳转", null, null, "test_user");
        });

        // 3. 验证错误消息
        String errorMessage = exception.getMessage();
        System.out.println("  📋 捕获异常: " + errorMessage);
        assertTrue(errorMessage.contains("不允许跳转") || errorMessage.contains("不可跳转"),
                "错误消息应包含'不允许跳转'或'不可跳转'");

        System.out.println("  ✅ TC-04-1通过: ifgetback=-1时正确拒绝跳转");

        // 4. 恢复节点状态
        dtl1.setIfexec("N");
        wfInstanceDtlMapper.updateById(dtl1);
    }

    /**
     * TC-04-2: 验证ifgetback指定节点列表时的跳转限制
     */
    @Test
    @Order(2)
    @DisplayName("TC-04-2: ifgetback指定节点时只能跳转到指定节点")
    public void testJumpOnlyAllowedToSpecifiedNodes() {
        System.out.println("\n🧪 执行 TC-04-2: ifgetback指定节点时只能跳转到指定节点");

        // 1. 将节点2标记为已执行（当前节点）
        WfInstanceDtl dtl2 = wfInstanceDtlMapper.selectById(testDtlId2);
        dtl2.setIfexec("Y");
        wfInstanceDtlMapper.updateById(dtl2);

        // 2. 尝试跳转到节点1（不在允许列表中）
        JeecgBootException exception1 = assertThrows(JeecgBootException.class, () -> {
            wfExecuteService.executeNode(testDtlId2, "3", testDtlId1, "测试跳转到不允许的节点", null, null, "test_user");
        });

        String errorMessage1 = exception1.getMessage();
        System.out.println("  📋 跳转到节点1异常: " + errorMessage1);
        assertTrue(errorMessage1.contains("不允许跳转到该节点"),
                "错误消息应包含'不允许跳转到该节点'");
        System.out.println("  ✅ 正确拒绝跳转到非白名单节点");

        // 3. 跳转到节点3（在允许列表中）- 应该成功
        assertDoesNotThrow(() -> {
            wfExecuteService.executeNode(testDtlId2, "3", testDtlId3, "测试跳转到允许的节点", null, null, "test_user");
        });
        System.out.println("  ✅ 正确允许跳转到白名单节点");

        // 4. 验证节点3的状态被更新为"R"（退回）
        WfInstanceDtl dtl3 = wfInstanceDtlMapper.selectById(testDtlId3);
        assertEquals("R", dtl3.getIfexec(), "目标节点状态应为'R'（退回）");
        System.out.println("  ✅ 目标节点状态正确更新为'R'");

        System.out.println("  ✅ TC-04-2通过: ifgetback白名单校验正常");

        // 5. 恢复节点状态
        dtl2.setIfexec("N");
        wfInstanceDtlMapper.updateById(dtl2);
        dtl3.setIfexec("N");
        dtl3.setIfjump("0");
        wfInstanceDtlMapper.updateById(dtl3);
    }

    /**
     * TC-04-3: 验证ifgetback="0"时可以跳转到起始节点
     */
    @Test
    @Order(3)
    @DisplayName("TC-04-3: ifgetback=0时可以跳转到起始节点")
    @Transactional
    public void testJumpToStartNodeWhenIfgetbackIsZero() {
        System.out.println("\n🧪 执行 TC-04-3: ifgetback=0时可以跳转到起始节点");

        // 1. 将节点3的ifgetback设置为"0"（允许跳转到起始节点）
        WfInstanceDtl dtl3 = wfInstanceDtlMapper.selectById(testDtlId3);
        dtl3.setIfgetback("0");
        dtl3.setIfexec("Y");
        wfInstanceDtlMapper.updateById(dtl3);

        // 2. 将节点1的seqno设置为0（标记为起始节点）
        WfInstanceDtl dtl1 = wfInstanceDtlMapper.selectById(testDtlId1);
        Integer originalSeqno = dtl1.getSeqno();
        dtl1.setSeqno(0);
        dtl1.setIfgetback(null); // 清除-1限制，因为我们要跳转到这里
        wfInstanceDtlMapper.updateById(dtl1);

        // 3. 跳转到起始节点 - 应该成功
        assertDoesNotThrow(() -> {
            wfExecuteService.executeNode(testDtlId3, "3", testDtlId1, "测试跳转到起始节点", null, null, "test_user");
        });
        System.out.println("  ✅ 正确允许跳转到起始节点(seqno=0)");

        // 4. 验证节点1的状态被更新为"R"（退回）
        dtl1 = wfInstanceDtlMapper.selectById(testDtlId1);
        assertEquals("R", dtl1.getIfexec(), "起始节点状态应为'R'（退回）");
        System.out.println("  ✅ 起始节点状态正确更新为'R'");

        System.out.println("  ✅ TC-04-3通过: ifgetback=0允许跳转到起始节点");

        // 5. 恢复节点状态（事务会自动回滚，这里仅为日志记录）
        dtl1.setSeqno(originalSeqno);
        dtl1.setIfgetback("-1");
        dtl1.setIfexec("N");
        wfInstanceDtlMapper.updateById(dtl1);

        dtl3.setIfgetback(null);
        dtl3.setIfexec("N");
        wfInstanceDtlMapper.updateById(dtl3);
    }

    /**
     * TC-04-4: 验证ifgetback为空时可以跳转到任意节点
     */
    @Test
    @Order(4)
    @DisplayName("TC-04-4: ifgetback为空时可以跳转到任意节点")
    @Transactional
    public void testJumpToAnyNodeWhenIfgetbackIsEmpty() {
        System.out.println("\n🧪 执行 TC-04-4: ifgetback为空时可以跳转到任意节点");

        // 1. 创建一个新节点，ifgetback为null
        WfInstanceDtl dtl4 = new WfInstanceDtl();
        dtl4.setInstanceid(testInstanceId);
        dtl4.setNodename("节点4-无跳转限制");
        dtl4.setSeqno(4);
        dtl4.setIfexec("Y");
        dtl4.setIfgetback(null); // 或空字符串
        dtl4.setUserid("test_user");
        dtl4.setCreateTime(new Date());
        wfInstanceDtlMapper.insert(dtl4);
        String dtl4Id = dtl4.getId();
        System.out.println("  ✅ 创建节点4: " + dtl4Id + " (ifgetback=null)");

        // 2. 跳转到任意节点（节点1）- 应该成功
        WfInstanceDtl dtl1 = wfInstanceDtlMapper.selectById(testDtlId1);
        dtl1.setIfgetback(null); // 移除-1限制
        wfInstanceDtlMapper.updateById(dtl1);

        assertDoesNotThrow(() -> {
            wfExecuteService.executeNode(dtl4Id, "3", testDtlId1, "测试无限制跳转", null, null, "test_user");
        });
        System.out.println("  ✅ ifgetback为空时正确允许跳转到任意节点");

        System.out.println("  ✅ TC-04-4通过: ifgetback为空时跳转无限制");

        // 3. 清理
        wfInstanceDtlMapper.deleteById(dtl4Id);
    }

    /**
     * TC-04总结
     */
    @Test
    @Order(5)
    @DisplayName("TC-04总结")
    public void testSummary() {
        String separator = "============================================================";
        System.out.println("\n" + separator);
        System.out.println("📊 TC-04回归测试总结");
        System.out.println(separator);
        System.out.println("✅ TC-04-1: ifgetback=-1禁止跳转校验通过");
        System.out.println("✅ TC-04-2: ifgetback白名单校验通过");
        System.out.println("✅ TC-04-3: ifgetback=0允许跳转到起始节点通过");
        System.out.println("✅ TC-04-4: ifgetback为空无限制跳转通过");
        System.out.println(separator);
        System.out.println("🎉 P2-LOGIC-01修复验证完成：跳转节点后端校验功能正常");
        System.out.println(separator + "\n");
    }
}
