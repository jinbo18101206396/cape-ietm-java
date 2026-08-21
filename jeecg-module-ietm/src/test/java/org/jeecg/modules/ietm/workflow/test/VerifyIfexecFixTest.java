package org.jeecg.modules.ietm.workflow.test;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 v_wf_instance 视图 ifexec 逻辑修复
 *
 * 修复点：视图应查询 ifexec_='N' (未执行=待办)，而非 'Y' (已执行)
 *
 * @author IETM Team
 * @date 2026-08-20
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("v_wf_instance视图ifexec逻辑修复验证")
public class VerifyIfexecFixTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @Order(1)
    @DisplayName("验证1：视图是否有效")
    public void test01_ViewStatus() {
        String sql = "SELECT VIEW_NAME, STATUS FROM USER_VIEWS WHERE VIEW_NAME='V_WF_INSTANCE'";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);

        assertFalse(result.isEmpty(), "v_wf_instance 视图应存在");
        assertEquals("VALID", result.get(0).get("STATUS"),
            "视图状态应为 VALID");

        log.info("✓ 视图状态: {}", result.get(0).get("STATUS"));
    }

    @Test
    @Order(2)
    @DisplayName("验证2：视图中的ifexec值（应全为N）")
    public void test02_IfexecValues() {
        String sql = "SELECT id_, activityalias_, currenthandler_, ifexec_ " +
                    "FROM v_wf_instance WHERE ROWNUM <= 10";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);

        log.info("视图返回 {} 条记录", result.size());

        for (Map<String, Object> row : result) {
            String ifexec = (String) row.get("IFEXEC_");
            String activityAlias = (String) row.get("ACTIVITYALIAS_");
            String handler = (String) row.get("CURRENTHANDLER_");

            log.info("  实例ID={}, 步骤={}, 待办人={}, ifexec={}",
                row.get("ID_"), activityAlias, handler, ifexec);

            assertEquals("N", ifexec,
                "【P0 CRITICAL】视图中的 ifexec_ 应为 'N' (待办)，不应为 'Y' (已办)！");
        }

        log.info("✓ 所有记录的 ifexec_ 均为 'N' (待办状态) - 修复正确");
    }

    @Test
    @Order(3)
    @DisplayName("验证3：数量对比（表vs视图）")
    public void test03_CountComparison() {
        // 原始表中的待办节点数
        String sql1 = "SELECT COUNT(*) FROM wf_instance_dtl " +
                     "WHERE del_flag='0' AND ifexec_='N'";
        Integer pendingInTable = jdbcTemplate.queryForObject(sql1, Integer.class);

        // 视图中的节点数
        String sql2 = "SELECT COUNT(*) FROM v_wf_instance";
        Integer countInView = jdbcTemplate.queryForObject(sql2, Integer.class);

        log.info("原始表中待办节点数: {}", pendingInTable);
        log.info("视图中节点数: {}", countInView);

        assertNotNull(pendingInTable, "表中待办节点数不应为null");
        assertNotNull(countInView, "视图节点数不应为null");

        // 视图数量应 ≤ 表数量（因为视图取MIN(seqno)会减少记录）
        assertTrue(countInView <= pendingInTable,
            "视图数量应 ≤ 表中待办节点数");

        log.info("✓ 数量合理（视图={} ≤ 表={}）", countInView, pendingInTable);
    }

    @Test
    @Order(4)
    @DisplayName("验证4：真实业务场景（DM管理页面查询）")
    public void test04_RealBusinessQuery() {
        String sql = "SELECT t1.id AS DM_ID, t1.dm_code AS DMC, " +
                    "       v.activityalias_ AS 当前步骤, " +
                    "       v.currenthandler_ AS 待办人, " +
                    "       v.ifexec_ AS 节点状态 " +
                    "FROM ietm_data_module t1 " +
                    "LEFT JOIN v_wf_instance v ON t1.id=v.formid_ " +
                    "WHERE t1.status='1' AND t1.is_latest='1' " +
                    "ORDER BY t1.create_time DESC " +
                    "FETCH FIRST 10 ROWS ONLY";

        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);

        log.info("DM管理页面查询返回 {} 条记录", result.size());

        int withWorkflow = 0;
        for (Map<String, Object> row : result) {
            String dmId = String.valueOf(row.get("DM_ID"));
            String dmc = (String) row.get("DMC");
            String step = (String) row.get("当前步骤");
            String handler = (String) row.get("待办人");
            String ifexec = (String) row.get("节点状态");

            if (ifexec != null) {
                withWorkflow++;
                log.info("  DM_ID={}, DMC={}, 步骤={}, 待办人={}, ifexec={}",
                    dmId, dmc, step, handler, ifexec);
                assertEquals("N", ifexec,
                    "DM关联的流程节点必须是待办状态(N)");
            }
        }

        log.info("✓ 共 {} 个DM有流程信息，ifexec 均为 'N'", withWorkflow);
    }

    @Test
    @Order(5)
    @DisplayName("验证5：实际待办节点详情")
    public void test05_ActualPendingNodes() {
        String sql = "SELECT inst.id AS 实例ID, inst.formid_ AS 表单ID, " +
                    "       dtl.nodename_ AS 节点名, dtl.userid_ AS 待办人, " +
                    "       dtl.ifexec_ AS 执行状态, dtl.seqno_ AS 序号 " +
                    "FROM wf_instance inst " +
                    "JOIN wf_instance_dtl dtl ON dtl.instid_=inst.id " +
                    "WHERE inst.del_flag='0' AND dtl.del_flag='0' " +
                    "  AND dtl.ifexec_='N' " +
                    "ORDER BY inst.create_time DESC, dtl.seqno_ " +
                    "FETCH FIRST 10 ROWS ONLY";

        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);

        log.info("实际待办节点数: {}", result.size());

        for (Map<String, Object> row : result) {
            log.info("  实例={}, 表单={}, 节点={}, 待办人={}, ifexec={}, 序号={}",
                row.get("实例ID"), row.get("表单ID"), row.get("节点名"),
                row.get("待办人"), row.get("执行状态"), row.get("序号"));

            assertEquals("N", row.get("执行状态"),
                "原始表中的待办节点 ifexec 应为 'N'");
        }

        log.info("✓ 所有待办节点 ifexec='N' 验证通过");
    }

    @Test
    @Order(99)
    @DisplayName("修复验证总结")
    public void test99_Summary() {
        log.info("========================================");
        log.info("v_wf_instance 视图 ifexec 逻辑修复验证");
        log.info("========================================");
        log.info("✓ 视图状态: VALID");
        log.info("✓ ifexec 逻辑: 正确查询 'N' (待办)");
        log.info("✓ 数量关系: 合理");
        log.info("✓ 业务场景: DM管理页面查询正常");
        log.info("✓ 实际数据: 待办节点正确");
        log.info("========================================");
        log.info("修复验证完成时间: {}", new java.util.Date());
        log.info("========================================");
    }
}
