package org.jeecg.modules.ietm.test;

import java.sql.*;

/**
 * TC-05: workflowStatus同步机制验证工具
 *
 * 使用方法：
 * 1. 在IDEA中运行此类的main方法
 * 2. 或使用Maven exec插件：mvn exec:java -Dexec.mainClass="org.jeecg.modules.ietm.test.TC05Validator"
 */
public class TC05Validator {

    private static final String DB_URL = "jdbc:dm://127.0.0.1:5236/?IETM";
    private static final String DB_USER = "IETM";
    private static final String DB_PASSWORD = "AvicCape301";

    public static void main(String[] args) {
        System.out.println("============================================================");
        System.out.println("TC-05: workflowStatus同步机制验证");
        System.out.println("============================================================\n");

        Connection conn = null;
        try {
            // 加载DM驱动
            Class.forName("dm.jdbc.driver.DmDriver");
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

            // 步骤1: 检查workflow_status字段存在性
            System.out.println("[步骤1] 检查workflow_status字段存在性");
            System.out.println("------------------------------------------------------------");
            String sql1 = "SELECT COUNT(*) AS field_exists FROM USER_TAB_COLUMNS " +
                         "WHERE TABLE_NAME = 'IETM_DATA_MODULE' AND COLUMN_NAME = 'WORKFLOW_STATUS'";
            executeQuery(conn, sql1, "field_exists");

            // 步骤2: 抽样对比
            System.out.println("\n[步骤2] 抽样对比workflow_status与v_wf_instance（前10条）");
            System.out.println("------------------------------------------------------------");
            String sql2 = "SELECT dm.id AS dm_id, dm.dmc_code, " +
                         "dm.workflow_status AS table_status, v.statusname AS view_status, " +
                         "CASE WHEN dm.workflow_status = v.statusname THEN '一致' " +
                         "     WHEN dm.workflow_status IS NULL AND v.statusname IS NULL THEN '一致(均为空)' " +
                         "     ELSE '不一致' END AS sync_status " +
                         "FROM ietm_data_module dm " +
                         "LEFT JOIN v_wf_instance v ON dm.workflow_instance_id = v.id " +
                         "WHERE dm.workflow_instance_id IS NOT NULL AND ROWNUM <= 10";
            executeSampleQuery(conn, sql2);

            // 步骤3: 统计一致率
            System.out.println("\n[步骤3] 统计一致率");
            System.out.println("------------------------------------------------------------");
            String sql3 = "SELECT COUNT(*) AS total_count, " +
                         "SUM(CASE WHEN dm.workflow_status = v.statusname THEN 1 ELSE 0 END) AS consistent_count, " +
                         "SUM(CASE WHEN dm.workflow_status != v.statusname OR " +
                         "    (dm.workflow_status IS NULL AND v.statusname IS NOT NULL) OR " +
                         "    (dm.workflow_status IS NOT NULL AND v.statusname IS NULL) " +
                         "    THEN 1 ELSE 0 END) AS inconsistent_count, " +
                         "ROUND(SUM(CASE WHEN dm.workflow_status = v.statusname THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2) AS consistency_rate " +
                         "FROM ietm_data_module dm " +
                         "LEFT JOIN v_wf_instance v ON dm.workflow_instance_id = v.id " +
                         "WHERE dm.workflow_instance_id IS NOT NULL";
            executeStatQuery(conn, sql3);

            // 结论
            System.out.println("\n============================================================");
            System.out.println("验证完成！");
            System.out.println("============================================================");

        } catch (ClassNotFoundException e) {
            System.err.println("❌ 错误：找不到DM数据库驱动");
            System.err.println("请确认Maven依赖中包含DmJdbcDriver");
        } catch (SQLException e) {
            System.err.println("❌ 数据库连接或查询错误：" + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void executeQuery(Connection conn, String sql, String columnName) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                int count = rs.getInt(columnName);
                if (count > 0) {
                    System.out.println("✅ workflow_status字段存在");
                } else {
                    System.out.println("❌ workflow_status字段不存在");
                }
            }
        }
    }

    private static void executeSampleQuery(Connection conn, String sql) throws SQLException {
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

    private static void executeStatQuery(Connection conn, String sql) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                int total = rs.getInt("total_count");
                int consistent = rs.getInt("consistent_count");
                int inconsistent = rs.getInt("inconsistent_count");
                double rate = rs.getDouble("consistency_rate");

                System.out.printf("总DM数（有流程）: %d%n", total);
                System.out.printf("一致数量: %d%n", consistent);
                System.out.printf("不一致数量: %d%n", inconsistent);
                System.out.printf("一致率: %.2f%%%n", rate);

                System.out.println("\n📊 诊断结论:");
                if (rate >= 95.0) {
                    System.out.println("✅ 数据高度一致（≥95%）");
                    System.out.println("   → DM列表可能使用v_wf_instance视图");
                    System.out.println("   → P1-SYNC-01修复（事件监听）已生效");
                } else if (rate >= 80.0) {
                    System.out.println("⚠️  数据基本一致（80%-95%）");
                    System.out.println("   → 部分记录需要同步");
                } else {
                    System.out.println("❌ 数据大量不一致（<80%）");
                    System.out.println("   → DM列表使用workflow_status字段");
                    System.out.println("   → 后端需要在流程完成后同步更新该字段");
                    System.out.println("   → 建议执行订正SQL");
                }
            }
        }
    }
}
