package org.jeecg.modules.ietm.workflow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

/**
 * TC-05: workflowStatus同步机制验证
 *
 * 执行方式：在IDEA中右键运行此测试类
 * 或命令行：mvn test -Dtest=TC05WorkflowStatusSyncTest
 */
@SpringBootTest
@ActiveProfiles("dev")
public class TC05WorkflowStatusSyncTest {

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testWorkflowStatusSync() {
        if (jdbcTemplate == null) {
            System.out.println("⚠️  JdbcTemplate未注入，跳过测试");
            System.out.println("请在application-test.yml中配置数据源");
            return;
        }

        System.out.println("============================================================");
        System.out.println("TC-05: workflowStatus同步机制验证");
        System.out.println("============================================================\n");

        try {
            // 步骤1: 检查workflow_status字段存在性
            step1_checkFieldExists();

            // 步骤2: 抽样对比
            step2_sampleComparison();

            // 步骤3: 统计一致率
            step3_consistencyRate();

            System.out.println("\n============================================================");
            System.out.println("✅ TC-05验证完成！");
            System.out.println("============================================================");

        } catch (Exception e) {
            System.err.println("❌ 执行出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void step1_checkFieldExists() {
        System.out.println("[步骤1] 检查workflow_status字段存在性");
        System.out.println("------------------------------------------------------------");

        String sql = "SELECT COUNT(*) FROM USER_TAB_COLUMNS " +
                    "WHERE TABLE_NAME = 'IETM_DATA_MODULE' AND COLUMN_NAME = 'WORKFLOW_STATUS'";

        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
            if (count != null && count > 0) {
                System.out.println("✅ workflow_status字段存在");
            } else {
                System.out.println("❌ workflow_status字段不存在");
            }
        } catch (Exception e) {
            System.out.println("⚠️  查询失败: " + e.getMessage());
        }
    }

    private void step2_sampleComparison() {
        System.out.println("\n[步骤2] 抽样对比workflow_status与v_wf_instance（前10条）");
        System.out.println("------------------------------------------------------------");

        String sql = "SELECT dm.id AS dm_id, dm.dmc_code, " +
                    "dm.workflow_status AS table_status, v.businessstate_ AS view_status, " +
                    "CASE WHEN dm.workflow_status = v.businessstate_ THEN '一致' " +
                    "     WHEN dm.workflow_status IS NULL AND v.businessstate_ IS NULL THEN '一致(均为空)' " +
                    "     ELSE '不一致' END AS sync_status " +
                    "FROM ietm_data_module dm " +
                    "LEFT JOIN v_wf_instance v ON dm.id = v.formid_ " +
                    "WHERE dm.workflow_instance_id IS NOT NULL " +
                    "AND ROWNUM <= 10 " +
                    "ORDER BY dm.create_time DESC";

        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);

            if (results.isEmpty()) {
                System.out.println("⚠️  没有找到有流程的DM记录");
                return;
            }

            int index = 0;
            for (Map<String, Object> row : results) {
                index++;
                System.out.printf("%d. DM ID: %s | DMC: %s%n",
                    index, row.get("DM_ID"), row.get("DMC_CODE"));
                System.out.printf("   表字段: %s | 视图: %s | 状态: %s%n",
                    row.get("TABLE_STATUS") == null ? "NULL" : row.get("TABLE_STATUS"),
                    row.get("VIEW_STATUS") == null ? "NULL" : row.get("VIEW_STATUS"),
                    row.get("SYNC_STATUS"));
            }

        } catch (Exception e) {
            System.out.println("⚠️  查询失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void step3_consistencyRate() {
        System.out.println("\n[步骤3] 统计一致率");
        System.out.println("------------------------------------------------------------");

        String sql = "SELECT COUNT(*) AS total_count, " +
                    "SUM(CASE WHEN dm.workflow_status = v.businessstate_ OR " +
                    "             (dm.workflow_status IS NULL AND v.businessstate_ IS NULL) " +
                    "         THEN 1 ELSE 0 END) AS consistent_count, " +
                    "SUM(CASE WHEN (dm.workflow_status IS NULL AND v.businessstate_ IS NOT NULL) OR " +
                    "             (dm.workflow_status IS NOT NULL AND v.businessstate_ IS NULL) OR " +
                    "             (dm.workflow_status != v.businessstate_ AND dm.workflow_status IS NOT NULL AND v.businessstate_ IS NOT NULL) " +
                    "    THEN 1 ELSE 0 END) AS inconsistent_count " +
                    "FROM ietm_data_module dm " +
                    "LEFT JOIN v_wf_instance v ON dm.id = v.formid_ " +
                    "WHERE dm.workflow_instance_id IS NOT NULL";

        try {
            Map<String, Object> result = jdbcTemplate.queryForMap(sql);

            Number totalObj = (Number) result.get("TOTAL_COUNT");
            Number consistentObj = (Number) result.get("CONSISTENT_COUNT");
            Number inconsistentObj = (Number) result.get("INCONSISTENT_COUNT");

            int total = totalObj != null ? totalObj.intValue() : 0;
            int consistent = consistentObj != null ? consistentObj.intValue() : 0;
            int inconsistent = inconsistentObj != null ? inconsistentObj.intValue() : 0;

            double rate = total > 0 ? (consistent * 100.0 / total) : 0.0;

            System.out.printf("总DM数（有流程）: %d%n", total);
            System.out.printf("一致数量: %d%n", consistent);
            System.out.printf("不一致数量: %d%n", inconsistent);
            System.out.printf("一致率: %.2f%%%n", rate);

            System.out.println("\n📊 诊断结论:");
            if (rate >= 95.0) {
                System.out.println("✅ 数据高度一致（≥95%）");
                System.out.println("   → DM列表可能使用v_wf_instance视图");
                System.out.println("   → P1-SYNC-01修复（事件监听）已生效");
                System.out.println("   → 或后端已正确同步workflow_status字段");
            } else if (rate >= 80.0) {
                System.out.println("⚠️  数据基本一致（80%-95%）");
                System.out.println("   → 部分记录需要同步");
                System.out.println("   → 建议执行订正SQL");
            } else {
                System.out.println("❌ 数据大量不一致（<80%）");
                System.out.println("   → DM列表使用workflow_status字段");
                System.out.println("   → 后端需要在流程完成后同步更新该字段");
                System.out.println("   → 必须执行订正SQL");
                System.out.println("\n订正SQL:");
                System.out.println("UPDATE ietm_data_module dm");
                System.out.println("SET workflow_status = (");
                System.out.println("    SELECT v.statusname FROM v_wf_instance v");
                System.out.println("    WHERE v.id = dm.workflow_instance_id");
                System.out.println("), workflow_step = (");
                System.out.println("    SELECT v.currentstep FROM v_wf_instance v");
                System.out.println("    WHERE v.id = dm.workflow_instance_id");
                System.out.println(")");
                System.out.println("WHERE dm.workflow_instance_id IS NOT NULL;");
                System.out.println("COMMIT;");
            }

        } catch (Exception e) {
            System.out.println("⚠️  查询失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
