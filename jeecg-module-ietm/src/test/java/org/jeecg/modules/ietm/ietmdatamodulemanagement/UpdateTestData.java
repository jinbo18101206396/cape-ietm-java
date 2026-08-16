package org.jeecg.modules.ietm.ietmdatamodulemanagement;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 修改测试数据 - 将一个DM的workflow_instance_id设置为NULL
 */
public class UpdateTestData {

    public static void main(String[] args) {
        String url = "jdbc:dm://127.0.0.1:5236/?IETM&zeroDateTimeBehavior=convertToNull&useUnicode=true&characterEncoding=utf-8";
        String username = "IETM";
        String password = "AvicCape301";

        try {
            // 加载驱动
            Class.forName("dm.jdbc.driver.DmDriver");

            // 连接数据库
            System.out.println("正在连接数据库...");
            Connection conn = DriverManager.getConnection(url, username, password);
            System.out.println("✅ 数据库连接成功！");

            // 查询当前所有DM的状态
            System.out.println("\n=== 当前DM状态 ===");
            String querySQL = "SELECT id, dm_code, workflow_instance_id, workflow_step FROM ietm_data_module ORDER BY id FETCH FIRST 10 ROWS ONLY";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(querySQL);

            String firstDmId = null;
            int count = 0;
            while (rs.next()) {
                String id = rs.getString("id");
                String dmCode = rs.getString("dm_code");
                String workflowId = rs.getString("workflow_instance_id");
                String workflowStep = rs.getString("workflow_step");

                if (firstDmId == null) {
                    firstDmId = id;
                }

                System.out.println(String.format("ID: %s, DMC: %s, workflow_instance_id: %s, workflow_step: %s",
                    id, dmCode, workflowId == null ? "NULL" : workflowId, workflowStep));
                count++;
            }
            rs.close();
            stmt.close();

            System.out.println("\n共找到 " + count + " 个DM");

            if (firstDmId != null) {
                // 修改第一个DM
                System.out.println("\n=== 修改测试数据 ===");
                System.out.println("将ID为 " + firstDmId + " 的DM设置为未启动流程状态...");

                String updateSQL = "UPDATE ietm_data_module SET workflow_instance_id = NULL, workflow_step = NULL WHERE id = ?";
                PreparedStatement pstmt = conn.prepareStatement(updateSQL);
                pstmt.setString(1, firstDmId);
                int rows = pstmt.executeUpdate();
                pstmt.close();

                System.out.println("✅ 更新了 " + rows + " 行数据");

                // 验证修改结果
                System.out.println("\n=== 验证修改结果 ===");
                String verifySQL = "SELECT id, dm_code, workflow_instance_id, workflow_step FROM ietm_data_module WHERE id = ?";
                PreparedStatement verifyStmt = conn.prepareStatement(verifySQL);
                verifyStmt.setString(1, firstDmId);
                ResultSet verifyRs = verifyStmt.executeQuery();

                if (verifyRs.next()) {
                    String workflowId = verifyRs.getString("workflow_instance_id");
                    String workflowStep = verifyRs.getString("workflow_step");
                    System.out.println(String.format("ID: %s, workflow_instance_id: %s, workflow_step: %s",
                        firstDmId, workflowId == null ? "NULL ✅" : workflowId, workflowStep == null ? "NULL ✅" : workflowStep));
                }
                verifyRs.close();
                verifyStmt.close();
            }

            conn.close();
            System.out.println("\n✅ 测试数据修改完成！现在可以进行UI测试了。");

        } catch (Exception e) {
            System.err.println("❌ 错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
