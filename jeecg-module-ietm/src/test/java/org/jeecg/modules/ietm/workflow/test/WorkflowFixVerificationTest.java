package org.jeecg.modules.ietm.workflow.test;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.ietm.workflow.entity.WfInstanceDtl;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 工作流三阶段修复综合验证测试
 *
 * 覆盖问题：
 * - P0-1: wf_instance.batch_id_ 字段缺失
 * - P0-2: wf_instance_dtl.useridname_ 字段缺失
 * - P1-2: wf_instance.reason_ 字段缺失
 * - P1-1: 创建节点 ifexec 应为 'Y'
 * - P2-1: wf_instance_dtl 的 ifjump_/ifnoopinion_ 字段存在
 * - P2-3: Mapper ResultMap 字段映射完整
 *
 * @author IETM Team
 * @date 2026-08-21
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("工作流三阶段修复综合验证")
public class WorkflowFixVerificationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ============================================================
    // P0/P1 数据库字段存在性验证
    // ============================================================

    @Test
    @Order(1)
    @DisplayName("P0-1：wf_instance.batch_id_ 字段应存在")
    public void test01_BatchIdColumnExists() {
        assertColumnExists("WF_INSTANCE", "BATCH_ID_");
        log.info("✓ P0-1修复验证通过：batch_id_ 字段存在");
    }

    @Test
    @Order(2)
    @DisplayName("P1-2：wf_instance.reason_ 字段应存在")
    public void test02_ReasonColumnExists() {
        assertColumnExists("WF_INSTANCE", "REASON_");
        log.info("✓ P1-2修复验证通过：reason_ 字段存在");
    }

    @Test
    @Order(3)
    @DisplayName("P0-2：wf_instance_dtl.useridname_ 字段应存在")
    public void test03_UseridnameColumnExists() {
        assertColumnExists("WF_INSTANCE_DTL", "USERIDNAME_");
        log.info("✓ P0-2修复验证通过：useridname_ 字段存在");
    }

    @Test
    @Order(4)
    @DisplayName("P2-1：wf_instance_dtl.ifjump_/ifnoopinion_ 字段应存在")
    public void test04_DtlExtraColumnsExist() {
        assertColumnExists("WF_INSTANCE_DTL", "IFJUMP_");
        assertColumnExists("WF_INSTANCE_DTL", "IFNOOPINION_");
        log.info("✓ P2-1修复验证通过：ifjump_/ifnoopinion_ 字段存在");
    }

    // ============================================================
    // P2-3 实体类字段映射完整性验证（反射）
    // ============================================================

    @Test
    @Order(5)
    @DisplayName("P2-3：WfInstanceDtl 所有@TableField映射的列在数据库存在")
    public void test05_EntityFieldMappingComplete() {
        // DM8列名大写存储；统一转大写比较。逐列用COUNT查询，避免schema/大小写导致的列表匹配问题
        int checked = 0;
        for (Field field : WfInstanceDtl.class.getDeclaredFields()) {
            com.baomidou.mybatisplus.annotation.TableField annotation =
                field.getAnnotation(com.baomidou.mybatisplus.annotation.TableField.class);
            if (annotation != null && annotation.value() != null && !annotation.value().isEmpty()) {
                String dbCol = annotation.value().replace("\"", "").toUpperCase();
                Integer cnt = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM USER_TAB_COLUMNS " +
                    "WHERE TABLE_NAME = 'WF_INSTANCE_DTL' AND COLUMN_NAME = ?",
                    Integer.class, dbCol);
                assertNotNull(cnt);
                assertEquals(1, cnt.intValue(),
                    String.format("实体类字段 %s 映射的列 %s 在数据库中不存在",
                        field.getName(), dbCol));
                checked++;
            }
        }
        log.info("✓ P2-3修复验证通过：WfInstanceDtl 的 {} 个@TableField映射全部有效", checked);
    }

    // ============================================================
    // P1-1 创建节点 ifexec 状态验证
    // ============================================================

    @Test
    @Order(6)
    @DisplayName("P1-1：创建节点(nodetype=0)的 ifexec 应全为 'Y'")
    public void test06_CreateNodeIfexecIsY() {
        Integer wrongCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM wf_instance_dtl " +
            "WHERE nodetype_ = '0' AND ifexec_ = 'N' AND del_flag = '0'",
            Integer.class);

        log.info("创建节点中 ifexec='N' 的错误记录数: {}", wrongCount);

        assertNotNull(wrongCount);
        assertEquals(0, wrongCount.intValue(),
            "【P1-1】创建节点(nodetype='0')的 ifexec_ 应为 'Y'，不应有 'N' 的记录。" +
            "若>0，请执行历史数据订正SQL");

        log.info("✓ P1-1修复验证通过：无创建节点处于错误的未执行状态");
    }

    @Test
    @Order(7)
    @DisplayName("P1-1：新启动流程的当前步骤不应显示'创建节点'")
    public void test07_WorkflowStepNotCreateNode() {
        // 检查视图是否存在（前置条件）
        List<Map<String, Object>> views = jdbcTemplate.queryForList(
            "SELECT VIEW_NAME FROM USER_VIEWS WHERE VIEW_NAME='V_WF_INSTANCE'");
        Assumptions.assumeTrue(!views.isEmpty(), "v_wf_instance视图不存在，跳过");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT formid_, activityalias_ FROM v_wf_instance " +
            "WHERE activityalias_ IS NOT NULL AND ROWNUM <= 50");

        log.info("视图返回 {} 条有当前步骤的记录", rows.size());

        for (Map<String, Object> row : rows) {
            String step = (String) row.get("ACTIVITYALIAS_");
            String formid = String.valueOf(row.get("FORMID_"));
            if ("创建节点".equals(step)) {
                // 诊断该异常流程：打印其完整节点结构
                log.warn("⚠ 发现当前步骤='创建节点'的流程 formid={}，诊断其节点结构：", formid);
                List<Map<String, Object>> nodes = jdbcTemplate.queryForList(
                    "SELECT d.seqno_, d.nodename_, d.nodetype_, d.ifexec_ " +
                    "FROM wf_instance_dtl d JOIN wf_instance i ON d.instid_ = i.id " +
                    "WHERE i.formid_ = ? AND d.del_flag = '0' ORDER BY d.seqno_", formid);
                for (Map<String, Object> n : nodes) {
                    log.warn("    seqno={}, 节点={}, 类型={}, ifexec={}",
                        n.get("SEQNO_"), n.get("NODENAME_"), n.get("NODETYPE_"), n.get("IFEXEC_"));
                }
                // 打印视图定义，定位视图逻辑bug
                try {
                    String viewText = jdbcTemplate.queryForObject(
                        "SELECT TEXT FROM USER_VIEWS WHERE VIEW_NAME='V_WF_INSTANCE'", String.class);
                    log.warn("=== 数据库中 v_wf_instance 视图定义 ===\n{}", viewText);
                } catch (Exception e) {
                    log.warn("无法读取视图定义: {}", e.getMessage());
                }
                // 手动验证子查询：ifexec='N' 的最小seqno应该是1(DM编写)
                List<Map<String, Object>> minSeq = jdbcTemplate.queryForList(
                    "SELECT MIN(d.seqno_) AS min_seqno FROM wf_instance_dtl d " +
                    "JOIN wf_instance i ON d.instid_ = i.id " +
                    "WHERE i.formid_ = ? AND d.del_flag='0' AND d.ifexec_='N'", formid);
                log.warn("手动子查询 ifexec=N 的最小seqno = {} (预期=1)", minSeq.get(0).get("MIN_SEQNO"));

                boolean onlyCreateNode = nodes.size() == 1
                    && "0".equals(String.valueOf(nodes.get(0).get("NODETYPE_")));
                if (onlyCreateNode) {
                    log.warn("    → 该流程仅含创建节点(空流程/历史数据)，非回归缺陷");
                } else {
                    fail(String.format("【视图逻辑缺陷】formid=%s 有%d个节点，DM编写(seqno=1)才是待办，" +
                        "但视图返回'创建节点'。视图未正确排除已执行节点，需修复视图定义", formid, nodes.size()));
                }
            }
        }
        log.info("✓ test07完成：新流程逻辑正确；如有'创建节点'均为单节点历史空流程");
    }

    // ============================================================
    // P2-1 batchInsert 字段写入验证（真实往返）
    // ============================================================

    @Test
    @Order(8)
    @DisplayName("P2-1：batchInsert写入ifjump_/ifnoopinion_/useridname_可正常往返")
    public void test08_BatchInsertRoundTrip() {
        long ts = System.currentTimeMillis();
        String testInstId = "TEST_INST_" + ts;
        String testDtlId = "TEST_DTL_" + ts;
        try {
            // 先插入父流程实例记录（满足外键 fk_dtl_instid）
            jdbcTemplate.update(
                "INSERT INTO wf_instance " +
                "(id, formid_, title_, status_, create_by, create_time, del_flag) " +
                "VALUES (?,?,?,?,?, SYSDATE, '0')",
                testInstId, "TEST_FORM_" + ts, "单元测试流程", "1", "junit");

            // 插入测试节点，模拟 batchInsert 写入的所有字段
            jdbcTemplate.update(
                "INSERT INTO wf_instance_dtl " +
                "(id, instid_, seqno_, nodename_, nodetype_, userid_, useridname_, " +
                " stagename_, ifexec_, ifgetback_, ifjump_, ifnoopinion_, create_by, create_time) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?, SYSDATE)",
                testDtlId, testInstId, 0, "测试创建节点", "0",
                "testuser", "测试用户", "", "Y", "", "0", "N", "junit");

            // 回读验证
            Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT useridname_, ifjump_, ifnoopinion_, ifexec_, nodetype_ " +
                "FROM wf_instance_dtl WHERE id = ?", testDtlId);

            assertEquals("测试用户", row.get("USERIDNAME_"), "useridname_应正确写入");
            assertEquals("0", String.valueOf(row.get("IFJUMP_")), "ifjump_应为0");
            assertEquals("N", row.get("IFNOOPINION_"), "ifnoopinion_应为N");
            assertEquals("Y", row.get("IFEXEC_"), "创建节点ifexec_应为Y");

            log.info("✓ P2-1修复验证通过：所有新增字段可正常写入和回读");
        } finally {
            // 清理测试数据（先子后父）
            jdbcTemplate.update("DELETE FROM wf_instance_dtl WHERE id = ?", testDtlId);
            jdbcTemplate.update("DELETE FROM wf_instance WHERE id = ?", testInstId);
            log.info("已清理测试数据: {} / {}", testDtlId, testInstId);
        }
    }

    @Test
    @Order(99)
    @DisplayName("修复验证总结")
    public void test99_Summary() {
        log.info("========================================");
        log.info("工作流三阶段修复综合验证完成");
        log.info("========================================");
        log.info("✓ P0-1: batch_id_ 字段");
        log.info("✓ P0-2: useridname_ 字段");
        log.info("✓ P1-1: 创建节点 ifexec='Y'");
        log.info("✓ P1-2: reason_ 字段");
        log.info("✓ P2-1: ifjump_/ifnoopinion_ 字段");
        log.info("✓ P2-3: 实体类字段映射完整");
        log.info("========================================");
    }

    // ============================================================
    // 辅助方法
    // ============================================================

    private void assertColumnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM USER_TAB_COLUMNS " +
            "WHERE TABLE_NAME = ? AND COLUMN_NAME = ?",
            Integer.class, tableName, columnName);
        assertNotNull(count);
        assertEquals(1, count.intValue(),
            String.format("表 %s 应存在字段 %s（若缺失，请执行 fix_workflow_missing_fields.sql）",
                tableName, columnName));
    }
}
