package org.jeecg.modules.ietm.workflow;

import java.sql.*;

/**
 * TC-05: workflowStatus同步机制验证（纯JDBC版本）
 *
 * 不依赖Spring Boot，可直接运行
 * 执行方式：java -cp [classpath] org.jeecg.modules.ietm.workflow.TC05WorkflowStatusValidator
 */
public class TC05WorkflowStatusValidator {

    private static final String DB_URL = "jdbc:dm://127.0.0.1:5236/?IETM";
    private static final String DB_USER = "IETM";
    private static final String DB_PASSWORD = "AvicCape301";
    private static final String DRIVER_CLASS = "dm.jdbc.driver.DmDriver";

    public static void main(String[] args) {
        System.out.println("============================================================");
        System.out.println("TC-05: workflowStatus同步机制验证");
        System.out.println("============================================================\n");

        Connection conn = null;
        try {
            // 加载驱动
            Class.forName(DRIVER_CLASS);
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("✅ 数据库连接成功\n");

            // 步骤1: 检查字段存在性
            checkFieldExists(conn);

            // 步骤2: 抽样对比
            sampleComparison(conn);

            // 步骤3: 统计一致率
            consistencyRate(conn);

            System.out.println("\n============================================================");
            System.out.println("✅ TC-05验证完成！");
            System.out.println("============================================================");

        } catch (ClassNotFoundException e) {
            System.err.println("❌ 找不到DM数据库驱动: " + DRIVER_CLASS);
            System.err.println("请确认classpath中包含DmJdbcDriver jar包");
        } catch (SQLException e) {
            System.err.println("❌ 数据库错误: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeConnection(conn);
        }
    }

    private static void checkFieldExists(Connection conn) throws SQLException {
        System.out.println("[步骤1] 检查workflow_status字段存在性");
        System.out.println("------------------------------------------------------------");

        String sql = "SELECT COUNT(*) FROM USER_TAB_COLUMNS " +
                    "WHERE TABLE_NAME = 'IETM_DATA_MODULE' AND COLUMN_NAME = 'WORKFLOW_STATUS'";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                int count = rs.getInt(1);
                if (count > 0) {
                    System.out.println("✅ workflow_status字段存在");
                } else {
                    System.out.println("❌ workflow_status字段不存在");
                }
            }
        }
    }

    private static void sampleComparison(Connection conn) throws SQLException {
        System.out.println("\n[步骤2] 抽样对比workflow_status与v_wf_instance（前10条）");
        System.out.println("------------------------------------------------------------");

        // 修正：v_wf_instance字段带下划线，关联条件是 dm.id = v.formid_
        String sql = "SELECT dm.id AS dm_id, dm.dmc_code, " +
                    "dm.workflow_status AS table_status, v.businessstate_ AS view_status, " +
                    "CASE WHEN dm.workflow_status = v.businessstate_ THEN '一致' " +
                    "     WHEN dm.workflow_status IS NULL AND v.businessstate_ IS NULL THEN '一致(均为空)' " +
                    "     ELSE '不一致' END AS sync_status " +
                    "FROM ietm_data_module dm " +
                    "LEFT JOIN v_wf_instance v ON dm.id = v.formid_ " +
                    "WHERE dm.workflow_instance_id IS NOT NULL " +
                    "AND ROWNUM <= 10";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            int index = 0;
            while (rs.next()) {
                index++;
                String dmId = rs.getString("dm_id");
                String dmcCode = rs.getString("dmc_code");
                String tableStatus = rs.getString("table_status");
                String viewStatus = rs.getString("view_status");
                String syncStatus = rs.getString("sync_status");

                System.out.printf("%d. DM ID: %s | DMC: %s%n", index, dmId, dmcCode);
                System.out.printf("   表字段: %s | 视图: %s | 状态: %s%n",
                    tableStatus == null ? "NULL" : tableStatus,
                    viewStatus == null ? "NULL" : viewStatus,
                    syncStatus);
            }

            if (index == 0) {
                System.out.println("⚠️  没有找到有流程的DM记录");
            }
        }
    }

    private static void consistencyRate(Connection conn) throws SQLException {
        System.out.println("\n[步骤3] 统计一致率");
        System.out.println("------------------------------------------------------------");

        // 修正：使用正确的字段名和关联条件
        String sql = "SELECT COUNT(*) AS total_count, " +
                    "SUM(CASE WHEN dm.workflow_status = v.businessstate_ OR " +
                    "             (dm.workflow_status IS NULL AND v.businessstate_ IS NULL) " +
                    "         THEN 1 ELSE 0 END) AS consistent_count, " +
                    "SUM(CASE WHEN dm.workflow_status IS NULL AND v.businessstate_ IS NOT NULL THEN 1 " +
                    "         WHEN dm.workflow_status IS NOT NULL AND v.businessstate_ IS NULL THEN 1 " +
                    "         WHEN dm.workflow_status != v.businessstate_ AND dm.workflow_status IS NOT NULL AND v.businessstate_ IS NOT NULL THEN 1 " +
                    "         ELSE 0 END) AS inconsistent_count " +
                    "FROM ietm_data_module dm " +
                    "LEFT JOIN v_wf_instance v ON dm.id = v.formid_ " +
                    "WHERE dm.workflow_instance_id IS NOT NULL";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                int total = rs.getInt("total_count");
                int consistent = rs.getInt("consistent_count");
                int inconsistent = rs.getInt("inconsistent_count");
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
                    printCorrectionSQL();
                }
            }
        }
    }

    private static void printCorrectionSQL() {
        System.out.println("\n📝 订正SQL（在确认后执行）:");
        System.out.println("------------------------------------------------------------");
        System.out.println("UPDATE ietm_data_module dm");
        System.out.println("SET workflow_status = (");
        System.out.println("    SELECT v.businessstate_ FROM v_wf_instance v");
        System.out.println("    WHERE v.formid_ = dm.id");
        System.out.println("),");
        System.out.println("workflow_step = (");
        System.out.println("    SELECT v.activityalias_ FROM v_wf_instance v");
        System.out.println("    WHERE v.formid_ = dm.id");
        System.out.println(")");
        System.out.println("WHERE dm.workflow_instance_id IS NOT NULL;");
        System.out.println();
        System.out.println("COMMIT;");
        System.out.println("------------------------------------------------------------");
    }

    private static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
                System.out.println("\n✅ 数据库连接已关闭");
            } catch (SQLException e) {
                System.err.println("⚠️  关闭连接失败: " + e.getMessage());
            }
        }
    }
}
