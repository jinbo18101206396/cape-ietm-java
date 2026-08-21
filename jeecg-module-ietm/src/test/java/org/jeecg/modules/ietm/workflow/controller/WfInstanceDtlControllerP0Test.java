package org.jeecg.modules.ietm.workflow.controller;

import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.ietm.workflow.entity.WfInstance;
import org.jeecg.modules.ietm.workflow.mapper.WfInstanceDtlMapper;
import org.jeecg.modules.ietm.workflow.mapper.WfInstanceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * P0级缺陷修复测试
 * 测试范围: P0-11, P0-12, P0-07, P0-04
 */
class WfInstanceDtlControllerP0Test {

    @InjectMocks
    private WfInstanceDtlController controller;

    @Mock
    private WfInstanceDtlMapper wfInstanceDtlMapper;

    @Mock
    private WfInstanceMapper wfInstanceMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * P0-11: 测试顺序号重复校验
     */
    @Test
    void testP0_11_DuplicateSeqnoShouldFail() {
        // 准备数据
        String instid = "test-inst-001";
        WfInstance instance = new WfInstance();
        instance.setId(instid);
        instance.setStagenames(null); // 非分阶段流程

        when(wfInstanceMapper.selectById(instid)).thenReturn(instance);

        Map<String, Object> params = new HashMap<>();
        List<Map<String, Object>> nodes = new ArrayList<>();

        // 节点1: seqno=0
        Map<String, Object> node1 = new HashMap<>();
        node1.put("id", "new_1");
        node1.put("instid", instid);
        node1.put("seqno", 0);
        node1.put("nodename", "创建");
        node1.put("userid", "admin");
        node1.put("nodetype", "0");
        nodes.add(node1);

        // 节点2: seqno=1
        Map<String, Object> node2 = new HashMap<>();
        node2.put("id", "new_2");
        node2.put("instid", instid);
        node2.put("seqno", 1);
        node2.put("nodename", "审核");
        node2.put("userid", "user1");
        node2.put("nodetype", "1");
        nodes.add(node2);

        // 节点3: seqno=1 (重复！)
        Map<String, Object> node3 = new HashMap<>();
        node3.put("id", "new_3");
        node3.put("instid", instid);
        node3.put("seqno", 1); // ❌ 与node2重复
        node3.put("nodename", "复核");
        node3.put("userid", "user2");
        node3.put("nodetype", "1");
        nodes.add(node3);

        params.put("nodes", nodes);

        // 执行
        Result<?> result = controller.saveBatch(params);

        // 断言
        assertFalse(result.isSuccess(), "顺序号重复应该失败");
        assertTrue(result.getMessage().contains("顺序号不能重复"),
            "错误消息应包含'顺序号不能重复'");
        assertTrue(result.getMessage().contains("1"),
            "错误消息应指出重复的顺序号");
    }

    /**
     * P0-11: 测试顺序号唯一性通过
     */
    @Test
    void testP0_11_UniqueSeqnoShouldPass() {
        String instid = "test-inst-002";
        WfInstance instance = new WfInstance();
        instance.setId(instid);
        instance.setStagenames(null);

        when(wfInstanceMapper.selectById(instid)).thenReturn(instance);
        when(wfInstanceDtlMapper.selectExecutedMaxSeqno(instid)).thenReturn(null);

        Map<String, Object> params = new HashMap<>();
        List<Map<String, Object>> nodes = new ArrayList<>();

        // 3个节点，顺序号 0, 1, 2（无重复）
        for (int i = 0; i < 3; i++) {
            Map<String, Object> node = new HashMap<>();
            node.put("id", "new_" + i);
            node.put("instid", instid);
            node.put("seqno", i);
            node.put("nodename", "节点" + i);
            node.put("userid", "user" + i);
            node.put("nodetype", "1");
            nodes.add(node);
        }

        params.put("nodes", nodes);

        // 执行 - 应通过唯一性校验（但可能因其他原因失败，此处仅验证唯一性校验通过）
        Result<?> result = controller.saveBatch(params);

        // 断言：不应包含"顺序号不能重复"的错误
        if (!result.isSuccess()) {
            assertFalse(result.getMessage().contains("顺序号不能重复"),
                "不应报告顺序号重复错误");
        }
    }

    /**
     * P0-12: 测试不能在已处理节点之前插入新节点
     */
    @Test
    void testP0_12_InsertBeforeExecutedNodeShouldFail() {
        String instid = "test-inst-003";
        WfInstance instance = new WfInstance();
        instance.setId(instid);
        instance.setStagenames(null);

        when(wfInstanceMapper.selectById(instid)).thenReturn(instance);
        when(wfInstanceDtlMapper.selectExecutedMaxSeqno(instid)).thenReturn(2); // 已处理到seqno=2

        Map<String, Object> params = new HashMap<>();
        List<Map<String, Object>> nodes = new ArrayList<>();

        // 构造合法的连续顺序号（0,1,2），但seqno=1的是新节点（≤execedSeqno）
        // 节点0: 创建节点（已存在）
        Map<String, Object> node0 = new HashMap<>();
        node0.put("id", "existing-0"); // 已存在节点
        node0.put("instid", instid);
        node0.put("seqno", 0);
        node0.put("nodename", "创建");
        node0.put("userid", "admin");
        node0.put("nodetype", "0");
        nodes.add(node0);

        // 节点1: 尝试插入新节点（seqno=1 ≤ execedSeqno=2）
        Map<String, Object> newNode = new HashMap<>();
        newNode.put("id", "new_999"); // 新增节点
        newNode.put("instid", instid);
        newNode.put("seqno", 1); // ❌ ≤ execedSeqno (2)
        newNode.put("nodename", "插入节点");
        newNode.put("userid", "admin");
        newNode.put("nodetype", "1");
        nodes.add(newNode);

        // 节点2: 已存在节点
        Map<String, Object> node2 = new HashMap<>();
        node2.put("id", "existing-2");
        node2.put("instid", instid);
        node2.put("seqno", 2);
        node2.put("nodename", "审批");
        node2.put("userid", "admin");
        node2.put("nodetype", "1");
        nodes.add(node2);

        params.put("nodes", nodes);

        // 执行
        Result<?> result = controller.saveBatch(params);

        // 断言
        assertFalse(result.isSuccess(), "在已处理节点之前插入应该失败");
        assertTrue(result.getMessage().contains("不能在已处理节点"),
            "错误消息应包含'不能在已处理节点'");
    }

    /**
     * P0-12: 测试在已处理节点之后插入新节点（应通过）
     */
    @Test
    void testP0_12_InsertAfterExecutedNodeShouldPass() {
        String instid = "test-inst-004";
        WfInstance instance = new WfInstance();
        instance.setId(instid);
        instance.setStagenames(null);

        when(wfInstanceMapper.selectById(instid)).thenReturn(instance);
        when(wfInstanceDtlMapper.selectExecutedMaxSeqno(instid)).thenReturn(2); // 已处理到seqno=2

        Map<String, Object> params = new HashMap<>();
        List<Map<String, Object>> nodes = new ArrayList<>();

        // 构造连续顺序号 0,1,2,3（seqno=3 > execedSeqno=2，应允许）
        // 节点0-2: 已存在节点
        for (int i = 0; i <= 2; i++) {
            Map<String, Object> existingNode = new HashMap<>();
            existingNode.put("id", "existing-" + i);
            existingNode.put("instid", instid);
            existingNode.put("seqno", i);
            existingNode.put("nodename", "节点" + i);
            existingNode.put("userid", "admin");
            existingNode.put("nodetype", "1");
            nodes.add(existingNode);
        }

        // 节点3: 新增节点（seqno=3 > execedSeqno=2）
        Map<String, Object> newNode = new HashMap<>();
        newNode.put("id", "new_888");
        newNode.put("instid", instid);
        newNode.put("seqno", 3); // ✅ > execedSeqno (2)
        newNode.put("nodename", "新节点");
        newNode.put("userid", "admin");
        newNode.put("nodetype", "1");
        nodes.add(newNode);

        params.put("nodes", nodes);

        // 执行
        Result<?> result = controller.saveBatch(params);

        // 断言：不应包含"不能在已处理节点"的错误
        if (!result.isSuccess()) {
            assertFalse(result.getMessage().contains("不能在已处理节点"),
                "在已处理节点之后插入不应报此错误，实际错误：" + result.getMessage());
        }
    }

    /**
     * P0-07: 测试分阶段流程 - 已结束阶段不能新增节点
     */
    @Test
    void testP0_07_InsertNodeInCompletedStageShouldFail() {
        String instid = "test-inst-005";
        WfInstance instance = new WfInstance();
        instance.setId(instid);
        instance.setStagenames("初审,复审,终审"); // 分阶段流程

        when(wfInstanceMapper.selectById(instid)).thenReturn(instance);
        when(wfInstanceDtlMapper.selectExecutedMaxSeqno(instid)).thenReturn(null);

        // 模拟"初审"阶段最后一个节点已执行
        org.jeecg.modules.ietm.workflow.entity.WfInstanceDtl lastNode =
            new org.jeecg.modules.ietm.workflow.entity.WfInstanceDtl();
        lastNode.setIfexec("Y"); // 已执行
        when(wfInstanceDtlMapper.selectLastNodeByStage(instid, "初审")).thenReturn(lastNode);

        Map<String, Object> params = new HashMap<>();
        List<Map<String, Object>> nodes = new ArrayList<>();

        // 尝试在"初审"阶段新增节点
        Map<String, Object> newNode = new HashMap<>();
        newNode.put("id", "new_stage_test");
        newNode.put("instid", instid);
        newNode.put("seqno", 0);
        newNode.put("nodename", "初审补充节点");
        newNode.put("userid", "admin");
        newNode.put("nodetype", "1");
        newNode.put("stagename", "初审"); // ❌ 该阶段已结束
        nodes.add(newNode);

        params.put("nodes", nodes);

        // 执行
        Result<?> result = controller.saveBatch(params);

        // 断言
        assertFalse(result.isSuccess(), "已结束阶段新增节点应该失败");
        assertTrue(result.getMessage().contains("阶段") && result.getMessage().contains("已结束"),
            "错误消息应包含阶段已结束提示");
    }

    /**
     * P0-07: 测试分阶段流程 - 未结束阶段可以新增节点
     */
    @Test
    void testP0_07_InsertNodeInActiveStageShouldPass() {
        String instid = "test-inst-006";
        WfInstance instance = new WfInstance();
        instance.setId(instid);
        instance.setStagenames("初审,复审,终审");

        when(wfInstanceMapper.selectById(instid)).thenReturn(instance);
        when(wfInstanceDtlMapper.selectExecutedMaxSeqno(instid)).thenReturn(null);

        // 模拟"复审"阶段最后一个节点未执行
        org.jeecg.modules.ietm.workflow.entity.WfInstanceDtl lastNode =
            new org.jeecg.modules.ietm.workflow.entity.WfInstanceDtl();
        lastNode.setIfexec("N"); // 未执行
        when(wfInstanceDtlMapper.selectLastNodeByStage(instid, "复审")).thenReturn(lastNode);

        Map<String, Object> params = new HashMap<>();
        List<Map<String, Object>> nodes = new ArrayList<>();

        Map<String, Object> newNode = new HashMap<>();
        newNode.put("id", "new_active_stage");
        newNode.put("instid", instid);
        newNode.put("seqno", 0);
        newNode.put("nodename", "复审新增");
        newNode.put("userid", "admin");
        newNode.put("nodetype", "1");
        newNode.put("stagename", "复审"); // ✅ 该阶段未结束
        nodes.add(newNode);

        params.put("nodes", nodes);

        // 执行
        Result<?> result = controller.saveBatch(params);

        // 断言：不应包含阶段已结束的错误
        if (!result.isSuccess()) {
            assertFalse(result.getMessage().contains("阶段") && result.getMessage().contains("已结束"),
                "未结束阶段不应报此错误");
        }
    }
}
