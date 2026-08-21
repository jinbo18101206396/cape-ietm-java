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
 * 【P0缺陷修复+验证】重建 v_wf_instance 视图，修正 ifexec 逻辑
 *
 * 缺陷：视图子查询用 ifexec_='Y'（已执行），导致当前步骤永远返回创建节点。
 * 修复：改为 ifexec_='N'（待办），返回真正的待办节点。
 *
 * 由 WorkflowFixVerificationTest.test07 发现。
 *
 * @author IETM Team
 * @date 2026-08-21
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("修复v_wf_instance视图ifexec逻辑缺陷")
public class FixViewIfexecBugTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @Order(1)
    @DisplayName("步骤1：重建视图（Y→N）")
    public void test01_RecreateView() {
        jdbcTemplate.execute("DROP VIEW \"IETM\".\"v_wf_instance\"");
        log.info("已删除旧视图");

        String createSql =
            "CREATE VIEW \"IETM\".\"v_wf_instance\" AS " +
            "SELECT a.id AS \"id_\", a.formid_ AS \"formid_\", " +
            "  a.title_ || a.titleparam_ AS \"title_\", a.url_ AS \"url_\", " +
            "  CASE a.status_ WHEN '0' THEN 'start' WHEN '1' THEN 'active' " +
            "    WHEN '2' THEN 'ended' WHEN '9' THEN 'over' ELSE a.status_ END AS \"businessstate_\", " +
            "  a.ifurgent_ AS \"ifurgent_\", a.stagenames_ AS \"stagenames_\", " +
            "  a.create_by AS \"createby_\", a.create_time AS \"createtime_\", " +
            "  a.update_by AS \"updateby_\", a.update_time AS \"updatetime_\", " +
            "  CASE a.status_ WHEN '2' THEN NULL ELSE b.nodename_ END AS \"activityalias_\", " +
            "  b.userid_ AS \"currenthandler_\", b.dtlid_ AS \"dtlid_\", " +
            "  b.ifjump_ AS \"ifjump_\", b.ifexec_ AS \"ifexec_\", b.seqno_ AS \"seqno_\" " +
            "FROM wf_instance a " +
            "LEFT JOIN ( " +
            "  SELECT t1.id AS \"dtlid_\", t1.instid_, t1.userid_, t1.nodename_, " +
            "         t1.seqno_, t1.nodetype_, t1.ifexec_, t1.ifjump_ " +
            "  FROM wf_instance_dtl t1 " +
            "  WHERE t1.del_flag = '0' AND t1.ifexec_ = 'N' " +   // 修复核心：Y→N
            "    AND (t1.instid_, t1.seqno_) IN ( " +
            "      SELECT instid_, MIN(seqno_) FROM wf_instance_dtl " +
            "      WHERE del_flag = '0' AND ifexec_ = 'N' GROUP BY instid_ " +  // 修复核心：Y→N
            "    ) " +
            ") b ON a.id = b.instid_ " +
            "WHERE a.del_flag = '0'";

        jdbcTemplate.execute(createSql);
        log.info("✓ 已重建视图，ifexec逻辑修正为 'N'（待办）");

        // 验证视图有效（能查询即为有效）
        Integer cnt = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM USER_VIEWS WHERE VIEW_NAME='V_WF_INSTANCE'", Integer.class);
        assertNotNull(cnt);
        assertEquals(1, cnt.intValue(), "重建后视图应存在");
        // 实际查询一次确认可用
        jdbcTemplate.queryForList("SELECT COUNT(*) FROM v_wf_instance");
        log.info("✓ 视图重建成功且可正常查询");
    }

    @Test
    @Order(2)
    @DisplayName("步骤2：验证之前异常的流程现在返回DM编写")
    public void test02_VerifyFixedFlow() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT activityalias_, seqno_, ifexec_ FROM v_wf_instance " +
            "WHERE formid_ = '2089169182489808898'");

        assumeFlowExists(rows);

        Map<String, Object> row = rows.get(0);
        String step = (String) row.get("ACTIVITYALIAS_");
        log.info("修复后该流程当前步骤={}, seqno={}, ifexec={}",
            step, row.get("SEQNO_"), row.get("IFEXEC_"));

        assertEquals("DM编写", step, "修复后当前步骤应为DM编写(seqno=1的待办节点)");
        assertEquals("N", row.get("IFEXEC_"), "视图返回的应是待办节点(N)");
        log.info("✓ 缺陷已修复：当前步骤正确显示为'DM编写'");
    }

    @Test
    @Order(3)
    @DisplayName("步骤3：全库不应再有多节点流程显示'创建节点'")
    public void test03_NoCreateNodeAsCurrentStep() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT v.formid_ FROM v_wf_instance v " +
            "JOIN wf_instance i ON v.formid_ = i.formid_ " +
            "WHERE v.activityalias_ = '创建节点' " +
            "  AND (SELECT COUNT(*) FROM wf_instance_dtl d " +
            "       WHERE d.instid_ = i.id AND d.del_flag='0') > 1");

        log.info("多节点流程仍显示'创建节点'的数量: {}", rows.size());
        assertTrue(rows.isEmpty(),
            "修复后不应再有多节点流程显示'创建节点'作为当前步骤");
        log.info("✓ 全库验证通过：无多节点流程错误显示创建节点");
    }

    @Test
    @Order(4)
    @DisplayName("步骤4：视图中所有ifexec_均为N")
    public void test04_AllIfexecIsN() {
        List<String> distinctValues = jdbcTemplate.queryForList(
            "SELECT DISTINCT ifexec_ FROM v_wf_instance WHERE ifexec_ IS NOT NULL",
            String.class);

        log.info("视图中 ifexec_ 的不同取值: {}", distinctValues);
        for (String v : distinctValues) {
            assertEquals("N", v, "视图返回的节点ifexec_应全部为'N'(待办)");
        }
        log.info("✓ 视图ifexec逻辑完全正确");
    }

    private void assumeFlowExists(List<Map<String, Object>> rows) {
        Assumptions.assumeTrue(!rows.isEmpty(),
            "测试流程2089169182489808898不存在（数据可能已清理），跳过");
    }
}
