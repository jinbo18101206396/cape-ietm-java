package org.jeecg.modules.ietm.workflow.test;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.jeecg.modules.ietm.workflow.entity.WfExecute;
import org.jeecg.modules.ietm.workflow.entity.WfInstance;
import org.jeecg.modules.ietm.workflow.entity.WfInstanceDtl;
import org.jeecg.modules.ietm.workflow.mapper.WfExecuteMapper;
import org.jeecg.modules.ietm.workflow.mapper.WfInstanceDtlMapper;
import org.jeecg.modules.ietm.workflow.mapper.WfInstanceMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Mapper字段映射测试 - 使用实际存在的字段名
 */
@SpringBootTest
@DisplayName("Mapper字段映射正确性测试")
public class WfMapperFieldMappingTest {

    @Autowired
    private WfInstanceMapper wfInstanceMapper;

    @Autowired
    private WfInstanceDtlMapper wfInstanceDtlMapper;

    @Autowired
    private WfExecuteMapper wfExecuteMapper;

    @Test
    @DisplayName("映射1：WfInstanceDtl.instanceid 字段读取")
    public void testWfInstanceDtlInstanceidRead() {
        List<WfInstanceDtl> dtls = wfInstanceDtlMapper.selectList(
            new LambdaQueryWrapper<WfInstanceDtl>().last("LIMIT 10")
        );

        for (WfInstanceDtl dtl : dtls) {
            assertNotNull(dtl.getInstanceid(),
                "instanceid字段应该能正确读取，节点ID: " + dtl.getId());
            assertFalse(dtl.getInstanceid().trim().isEmpty(),
                "instanceid不应为空字符串，节点ID: " + dtl.getId());
        }

        System.out.println("✓ 映射1通过：instanceid字段读取正确，检查了" + dtls.size() + "条记录");
    }

    @Test
    @DisplayName("映射2：WfInstanceDtl.instanceid WHERE查询")
    public void testWfInstanceDtlInstanceidWhereQuery() {
        WfInstanceDtl sampleDtl = wfInstanceDtlMapper.selectOne(
            new LambdaQueryWrapper<WfInstanceDtl>().last("LIMIT 1")
        );

        if (sampleDtl == null) {
            System.out.println("跳过测试：无可用数据");
            return;
        }

        String instanceid = sampleDtl.getInstanceid();
        List<WfInstanceDtl> dtls = wfInstanceDtlMapper.selectList(
            new LambdaQueryWrapper<WfInstanceDtl>().eq(WfInstanceDtl::getInstanceid, instanceid)
        );

        assertTrue(dtls.size() > 0, "应该能通过instanceid查询到节点");
        for (WfInstanceDtl dtl : dtls) {
            assertEquals(instanceid, dtl.getInstanceid(), "查询结果的instanceid应该匹配");
        }

        System.out.println("✓ 映射2通过：instanceid WHERE查询正确，找到" + dtls.size() + "个节点");
    }

    @Test
    @DisplayName("映射3：WfExecute.instdtlid 字段映射")
    public void testWfExecuteInstdtlidMapping() {
        List<WfExecute> executes = wfExecuteMapper.selectList(
            new LambdaQueryWrapper<WfExecute>()
                .isNotNull(WfExecute::getInstdtlid)
                .last("LIMIT 10")
        );

        for (WfExecute execute : executes) {
            assertNotNull(execute.getInstdtlid(), "instdtlid字段应该能正确读取");
            WfInstanceDtl dtl = wfInstanceDtlMapper.selectById(execute.getInstdtlid());
            assertNotNull(dtl, "instdtlid应该能关联到有效的详情记录");
        }

        System.out.println("✓ 映射3通过：instdtlid字段映射正确");
    }

    @Test
    @DisplayName("关联1：WfInstanceDtl.instanceid 关联 WfInstance.id")
    public void testDtlInstanceidToInstanceIdMapping() {
        List<WfInstanceDtl> dtls = wfInstanceDtlMapper.selectList(
            new LambdaQueryWrapper<WfInstanceDtl>().last("LIMIT 20")
        );

        for (WfInstanceDtl dtl : dtls) {
            String instanceid = dtl.getInstanceid();
            assertNotNull(instanceid, "instanceid不应为null");

            WfInstance instance = wfInstanceMapper.selectById(instanceid);
            assertNotNull(instance, "instanceid=" + instanceid + " 应该能找到对应的实例");
            assertEquals(instanceid, instance.getId(), "instanceid应该等于实例的ID");
        }

        System.out.println("✓ 关联1通过：WfInstanceDtl.instanceid正确关联WfInstance.id，检查了" + dtls.size() + "条记录");
    }

    @Test
    @DisplayName("关联2：WfExecute.instdtlid 关联 WfInstanceDtl.id")
    public void testExecuteInstdtlidToDtlIdMapping() {
        List<WfExecute> executes = wfExecuteMapper.selectList(
            new LambdaQueryWrapper<WfExecute>()
                .isNotNull(WfExecute::getInstdtlid)
                .last("LIMIT 20")
        );

        for (WfExecute execute : executes) {
            String instdtlid = execute.getInstdtlid();
            assertNotNull(instdtlid, "instdtlid不应为null");

            WfInstanceDtl dtl = wfInstanceDtlMapper.selectById(instdtlid);
            assertNotNull(dtl, "instdtlid=" + instdtlid + " 应该能找到对应的详情记录");
            assertEquals(instdtlid, dtl.getId(), "instdtlid应该等于详情的ID");
        }

        System.out.println("✓ 关联2通过：WfExecute.instdtlid正确关联WfInstanceDtl.id，检查了" + executes.size() + "条记录");
    }

    @Test
    @DisplayName("命名一致性：确认使用instanceid而非instid")
    public void testInstanceidNamingConsistency() {
        WfInstanceDtl dtl = wfInstanceDtlMapper.selectOne(
            new LambdaQueryWrapper<WfInstanceDtl>().last("LIMIT 1")
        );

        if (dtl == null) {
            System.out.println("跳过测试：无可用数据");
            return;
        }

        assertDoesNotThrow(() -> {
            String id = dtl.getInstanceid();
            assertNotNull(id, "getInstanceid()应该返回非null值");
        }, "应该存在getInstanceid()方法");

        System.out.println("✓ 命名一致性通过：确认使用instanceid命名");
    }
}
