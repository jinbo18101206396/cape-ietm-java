package org.jeecg.modules.ietm.workflow.test;

import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.ietm.workflow.constants.WfConstants;
import org.jeecg.modules.ietm.workflow.entity.WfExecute;
import org.jeecg.modules.ietm.workflow.entity.WfInstance;
import org.jeecg.modules.ietm.workflow.entity.WfInstanceDtl;
import org.jeecg.modules.ietm.workflow.mapper.WfExecuteMapper;
import org.jeecg.modules.ietm.workflow.mapper.WfInstanceDtlMapper;
import org.jeecg.modules.ietm.workflow.mapper.WfInstanceMapper;
import org.jeecg.modules.ietm.workflow.service.IWfExecuteService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 工作流状态机 场景测试 + 边界测试
 *
 * 覆盖 WfExecuteServiceImpl 的真实 API：
 *   executeNode(instdtlid, ifpass, targetDtlid, opinion, filename, filecontent, userId)
 *   addOpinion(instdtlid, opinion, userId)
 *   takeBack(instdtlid, userId)
 *
 * 设计要点：
 *  - 每个用例自建流程数据（父实例+若干节点），@Transactional 自动回滚，互不污染
 *  - @BeforeEach 绑定 Shiro 登录用户，使 MybatisInterceptor 能回填 wf_execute.create_by（NOT NULL）
 *  - RANDOM_PORT：WebSocketConfig 需要 servlet 容器
 *
 * @author IETM Team
 * @date 2026-08-21
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
@DisplayName("工作流状态机 场景/边界测试")
public class WorkflowScenarioBoundaryTest {

    @Autowired private IWfExecuteService wfExecuteService;
    @Autowired private WfInstanceMapper wfInstanceMapper;
    @Autowired private WfInstanceDtlMapper wfInstanceDtlMapper;
    @Autowired private WfExecuteMapper wfExecuteMapper;
    @Autowired private SecurityManager securityManager;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final String ME = "junit";

    @BeforeEach
    void bindLoginUser() {
        LoginUser user = new LoginUser();
        user.setId("junit-uid");
        user.setUsername(ME);
        user.setRealname("JUnit测试");
        Subject subject = new Subject.Builder(securityManager)
                .principals(new SimplePrincipalCollection(user, "myRealm"))
                .authenticated(true)
                .buildSubject();
        ThreadContext.bind(subject);
    }

    @AfterEach
    void clearLoginUser() {
        ThreadContext.unbindSubject();
    }

    // ============================================================
    // 测试数据构造辅助
    // ============================================================

    /** 新建流程实例，返回实例ID */
    private String newInstance(String status) {
        WfInstance inst = new WfInstance();
        inst.setFormid("TESTFORM_" + SEQ.incrementAndGet() + "_" + System.nanoTime());
        inst.setTitle("场景测试流程");
        inst.setStatus(status);
        inst.setCreateBy(ME);
        wfInstanceMapper.insert(inst);
        return inst.getId();
    }

    /** 新建节点，返回节点ID */
    private String newNode(String instId, int seqno, String nodename,
                           String nodetype, String ifexec) {
        WfInstanceDtl dtl = new WfInstanceDtl();
        dtl.setInstanceid(instId);
        dtl.setSeqno(seqno);
        dtl.setNodename(nodename);
        dtl.setNodetype(nodetype);
        dtl.setUserid("testuser");
        dtl.setUseridname("测试用户");
        dtl.setIfexec(ifexec);
        dtl.setIfjump("0");
        dtl.setIfnoopinion("N");
        dtl.setCreateBy(ME);
        wfInstanceDtlMapper.insert(dtl);
        return dtl.getId();
    }

    /** 便捷：一条含"创建节点(Y)+N个待办节点(N)"的标准流程；返回各节点ID，index0=创建节点 */
    private String[] newStandardFlow(int reviewNodeCount) {
        String instId = newInstance(WfConstants.STATUS_RUNNING);
        String[] ids = new String[reviewNodeCount + 1];
        ids[0] = newNode(instId, 0, "创建节点", WfConstants.NODE_TYPE_CREATE, WfConstants.EXEC_YES);
        for (int i = 1; i <= reviewNodeCount; i++) {
            ids[i] = newNode(instId, i, "审核节点" + i, WfConstants.NODE_TYPE_REVIEW, WfConstants.EXEC_NO);
        }
        return ids;
    }

    private WfInstanceDtl reload(String dtlId) {
        return wfInstanceDtlMapper.selectById(dtlId);
    }

    private WfInstance reloadInst(String dtlId) {
        return wfInstanceMapper.selectById(reload(dtlId).getInstanceid());
    }

    // ============================================================
    // 场景1：通过（ifpass=1）
    // ============================================================

    @Test
    @DisplayName("场景1.1 通过中间节点 → 节点Y，流程流转中")
    void pass_intermediateNode_flowRunning() throws Exception {
        String[] n = newStandardFlow(2); // 创建 + 审核1 + 审核2
        wfExecuteService.executeNode(n[1], "1", null, "同意", null, null, ME);

        assertEquals(WfConstants.EXEC_YES, reload(n[1]).getIfexec(), "当前节点应标记已执行");
        assertEquals(WfConstants.STATUS_RUNNING, reloadInst(n[1]).getStatus(), "还有后续待办节点，流程应为流转中");
        // 执行记录落库
        assertEquals(1, wfExecuteMapper.selectByDtlId(n[1]).size(), "应生成1条执行记录");
        assertEquals("1", wfExecuteMapper.selectByDtlId(n[1]).get(0).getIfpass());
    }

    @Test
    @DisplayName("场景1.2 通过最后一个节点 → 流程已结束(2)")
    void pass_lastNode_flowEnded() throws Exception {
        String[] n = newStandardFlow(1); // 创建 + 审核1(唯一待办)
        wfExecuteService.executeNode(n[1], "1", null, "同意", null, null, ME);

        assertEquals(WfConstants.EXEC_YES, reload(n[1]).getIfexec());
        assertEquals(WfConstants.STATUS_ENDED, reloadInst(n[1]).getStatus(), "最后节点通过后流程应结束");
    }

    @Test
    @DisplayName("场景1.3 后续均为跳过(J)时通过 → 视为最后节点，流程结束")
    void pass_withOnlySkippedFollowers_flowEnded() throws Exception {
        String instId = newInstance(WfConstants.STATUS_RUNNING);
        newNode(instId, 0, "创建节点", WfConstants.NODE_TYPE_CREATE, WfConstants.EXEC_YES);
        String cur = newNode(instId, 1, "审核1", WfConstants.NODE_TYPE_REVIEW, WfConstants.EXEC_NO);
        newNode(instId, 2, "审核2", WfConstants.NODE_TYPE_REVIEW, WfConstants.EXEC_SKIP); // 已跳过

        wfExecuteService.executeNode(cur, "1", null, "同意", null, null, ME);
        assertEquals(WfConstants.STATUS_ENDED, reloadInst(cur).getStatus(),
                "后续仅剩跳过节点，应视为最后节点 → 流程结束");
    }

    // ============================================================
    // 场景2：不同意（ifpass=2）
    // ============================================================

    @Test
    @DisplayName("场景2.1 不同意+有效意见 → 节点Y，流程结束(2)，执行记录=2")
    void reject_validOpinion() throws Exception {
        String[] n = newStandardFlow(2);
        wfExecuteService.executeNode(n[1], "2", null, "内容不符合规范，需修改", null, null, ME);

        assertEquals(WfConstants.EXEC_YES, reload(n[1]).getIfexec());
        assertEquals(WfConstants.STATUS_ENDED, reloadInst(n[1]).getStatus(), "不同意后流程结束");
        assertEquals("2", wfExecuteMapper.selectByDtlId(n[1]).get(0).getIfpass());
    }

    @Test
    @DisplayName("边界2.2 不同意+空意见 → 抛异常")
    void reject_emptyOpinion_throws() {
        String[] n = newStandardFlow(1);
        JeecgBootException ex = assertThrows(JeecgBootException.class,
                () -> wfExecuteService.executeNode(n[1], "2", null, "", null, null, ME));
        assertTrue(ex.getMessage().contains("必须填写意见"));
    }

    @Test
    @DisplayName("边界2.3 不同意+null意见 → 抛异常")
    void reject_nullOpinion_throws() {
        String[] n = newStandardFlow(1);
        assertThrows(JeecgBootException.class,
                () -> wfExecuteService.executeNode(n[1], "2", null, null, null, null, ME));
    }

    @Test
    @DisplayName("边界2.4 不同意+纯空格意见 → 抛异常")
    void reject_whitespaceOpinion_throws() {
        String[] n = newStandardFlow(1);
        assertThrows(JeecgBootException.class,
                () -> wfExecuteService.executeNode(n[1], "2", null, "   ", null, null, ME));
    }

    @Test
    @DisplayName("边界2.5 不同意意见含\"同意\"二字 → 抛异常")
    void reject_opinionContainsAgree_throws() {
        String[] n = newStandardFlow(1);
        JeecgBootException ex = assertThrows(JeecgBootException.class,
                () -> wfExecuteService.executeNode(n[1], "2", null, "基本同意但需小改", null, null, ME));
        assertTrue(ex.getMessage().contains("不能包含"));
    }

    // ============================================================
    // 场景3：跳转-退回（ifpass=3，目标 seqno < 当前）
    // ============================================================

    @Test
    @DisplayName("场景3.1 退回到前序节点 → 目标R+退回次数+1，中间节点重置N，当前Y，流程流转中")
    void jump_returnToEarlier() throws Exception {
        // 创建(0,Y) 审核1(1,Y) 审核2(2,Y) 审核3(3,N当前)
        String instId = newInstance(WfConstants.STATUS_RUNNING);
        newNode(instId, 0, "创建节点", WfConstants.NODE_TYPE_CREATE, WfConstants.EXEC_YES);
        String t = newNode(instId, 1, "审核1", WfConstants.NODE_TYPE_REVIEW, WfConstants.EXEC_YES);
        String mid = newNode(instId, 2, "审核2", WfConstants.NODE_TYPE_REVIEW, WfConstants.EXEC_YES);
        String cur = newNode(instId, 3, "审核3", WfConstants.NODE_TYPE_REVIEW, WfConstants.EXEC_NO);

        wfExecuteService.executeNode(cur, "3", t, "退回重改", null, null, ME);

        assertEquals(WfConstants.EXEC_RETURN, reload(t).getIfexec(), "目标节点应为退回R");
        assertEquals("1", reload(t).getIfjump(), "退回次数应+1");
        assertEquals(WfConstants.EXEC_NO, reload(mid).getIfexec(), "中间节点应重置为未执行N");
        assertEquals(WfConstants.EXEC_YES, reload(cur).getIfexec(), "当前节点应标记已执行Y");
        assertEquals(WfConstants.STATUS_RUNNING, reloadInst(cur).getStatus());
    }

    @Test
    @DisplayName("场景3.2 已有退回次数时再退回 → 累加(2)")
    void jump_returnCountAccumulates() throws Exception {
        String instId = newInstance(WfConstants.STATUS_RUNNING);
        newNode(instId, 0, "创建节点", WfConstants.NODE_TYPE_CREATE, WfConstants.EXEC_YES);
        String t = newNode(instId, 1, "审核1", WfConstants.NODE_TYPE_REVIEW, WfConstants.EXEC_YES);
        // 手动把目标节点已有退回次数置为1
        WfInstanceDtl tNode = reload(t);
        tNode.setIfjump("1");
        wfInstanceDtlMapper.updateById(tNode);
        String cur = newNode(instId, 2, "审核2", WfConstants.NODE_TYPE_REVIEW, WfConstants.EXEC_NO);

        wfExecuteService.executeNode(cur, "3", t, "再次退回", null, null, ME);
        assertEquals("2", reload(t).getIfjump(), "退回次数应从1累加到2");
    }

    // ============================================================
    // 场景4：跳转-跳过（ifpass=3，目标 seqno > 当前）
    // ============================================================

    @Test
    @DisplayName("场景4.1 跳过到后序普通节点 → 中间节点标记J，流程流转中")
    void jump_skipToLaterNormal() throws Exception {
        // 创建(0,Y) 当前(1,N) 中间(2,N) 目标(3,N)
        String instId = newInstance(WfConstants.STATUS_RUNNING);
        newNode(instId, 0, "创建节点", WfConstants.NODE_TYPE_CREATE, WfConstants.EXEC_YES);
        String cur = newNode(instId, 1, "审核1", WfConstants.NODE_TYPE_REVIEW, WfConstants.EXEC_NO);
        String mid = newNode(instId, 2, "审核2", WfConstants.NODE_TYPE_REVIEW, WfConstants.EXEC_NO);
        String target = newNode(instId, 3, "审核3", WfConstants.NODE_TYPE_REVIEW, WfConstants.EXEC_NO);

        wfExecuteService.executeNode(cur, "3", target, "跳过中间环节", null, null, ME);

        assertEquals(WfConstants.EXEC_SKIP, reload(cur).getIfexec(), "当前节点(含)应标记跳过J");
        assertEquals(WfConstants.EXEC_SKIP, reload(mid).getIfexec(), "中间节点应标记跳过J");
        assertEquals(WfConstants.EXEC_NO, reload(target).getIfexec(), "目标节点保持未执行N");
        assertEquals(WfConstants.STATUS_RUNNING, reloadInst(cur).getStatus());
    }

    @Test
    @DisplayName("边界4.2 【死代码取证】handleJump 的 END 分支不可达：nodetype_=VARCHAR(2)，存不下\"END\"")
    void jump_endBranch_isDeadCode() {
        // handleJump 中 "END".equals(targetNode.getNodetype()) 永远为假：
        // nodetype_ 列为 VARCHAR(2)，插入3字符"END"会抛数据完整性异常。
        // 本用例固化该缺陷，提醒后续需统一 END 语义（改列宽 或 改用其它标记）。
        String instId = newInstance(WfConstants.STATUS_RUNNING);
        assertThrows(Exception.class,
                () -> newNode(instId, 2, "结束节点", "END", WfConstants.EXEC_NO),
                "nodetype_=VARCHAR(2) 无法存储\"END\"，证明 handleJump 的 END 分支为死代码");
    }

    @Test
    @DisplayName("边界4.3 跳转未指定目标节点 → 抛异常")
    void jump_noTarget_throws() {
        String[] n = newStandardFlow(1);
        assertThrows(JeecgBootException.class,
                () -> wfExecuteService.executeNode(n[1], "3", null, "跳", null, null, ME));
    }

    @Test
    @DisplayName("边界4.4 跳转目标节点不存在 → 抛异常")
    void jump_targetNotExist_throws() {
        String[] n = newStandardFlow(1);
        assertThrows(JeecgBootException.class,
                () -> wfExecuteService.executeNode(n[1], "3", "999999999999", "跳", null, null, ME));
    }

    @Test
    @DisplayName("边界4.5 跳转目标属于其它流程 → 抛异常")
    void jump_targetOtherInstance_throws() {
        String[] a = newStandardFlow(1);
        String[] b = newStandardFlow(1);
        assertThrows(JeecgBootException.class,
                () -> wfExecuteService.executeNode(a[1], "3", b[1], "跳到别的流程", null, null, ME));
    }

    // ============================================================
    // 场景5：终止（ifpass=9）
    // ============================================================

    @Test
    @DisplayName("场景5.1 终止+原因 → 节点Y，流程终止(9)，执行记录=9")
    void terminate_withReason() throws Exception {
        String[] n = newStandardFlow(2);
        wfExecuteService.executeNode(n[1], "9", null, "项目取消", null, null, ME);

        assertEquals(WfConstants.EXEC_YES, reload(n[1]).getIfexec());
        assertEquals(WfConstants.STATUS_TERMINATED, reloadInst(n[1]).getStatus());
        assertEquals("9", wfExecuteMapper.selectByDtlId(n[1]).get(0).getIfpass());
    }

    @Test
    @DisplayName("边界5.2 终止+空原因 → 抛异常")
    void terminate_emptyReason_throws() {
        String[] n = newStandardFlow(1);
        assertThrows(JeecgBootException.class,
                () -> wfExecuteService.executeNode(n[1], "9", null, "", null, null, ME));
    }

    // ============================================================
    // 边界：executeNode 前置校验
    // ============================================================

    @Test
    @DisplayName("边界6.1 节点不存在 → 抛异常")
    void execute_nodeNotExist_throws() {
        assertThrows(JeecgBootException.class,
                () -> wfExecuteService.executeNode("999999999999", "1", null, "x", null, null, ME));
    }

    @Test
    @DisplayName("边界6.2 流程已结束时操作 → 抛异常")
    void execute_instanceEnded_throws() {
        String instId = newInstance(WfConstants.STATUS_ENDED);
        newNode(instId, 0, "创建节点", WfConstants.NODE_TYPE_CREATE, WfConstants.EXEC_YES);
        String cur = newNode(instId, 1, "审核1", WfConstants.NODE_TYPE_REVIEW, WfConstants.EXEC_NO);
        JeecgBootException ex = assertThrows(JeecgBootException.class,
                () -> wfExecuteService.executeNode(cur, "1", null, "同意", null, null, ME));
        assertTrue(ex.getMessage().contains("流程已结束"));
    }

    @Test
    @DisplayName("边界6.3 流程已终止时操作 → 抛异常")
    void execute_instanceTerminated_throws() {
        String instId = newInstance(WfConstants.STATUS_TERMINATED);
        newNode(instId, 0, "创建节点", WfConstants.NODE_TYPE_CREATE, WfConstants.EXEC_YES);
        String cur = newNode(instId, 1, "审核1", WfConstants.NODE_TYPE_REVIEW, WfConstants.EXEC_NO);
        assertThrows(JeecgBootException.class,
                () -> wfExecuteService.executeNode(cur, "1", null, "同意", null, null, ME));
    }

    @Test
    @DisplayName("边界6.4 节点已处理(Y) → 抛异常（不能重复操作）")
    void execute_nodeAlreadyDone_throws() {
        String instId = newInstance(WfConstants.STATUS_RUNNING);
        newNode(instId, 0, "创建节点", WfConstants.NODE_TYPE_CREATE, WfConstants.EXEC_YES);
        String done = newNode(instId, 1, "审核1", WfConstants.NODE_TYPE_REVIEW, WfConstants.EXEC_YES);
        JeecgBootException ex = assertThrows(JeecgBootException.class,
                () -> wfExecuteService.executeNode(done, "1", null, "同意", null, null, ME));
        assertTrue(ex.getMessage().contains("已处理"));
    }

    @Test
    @DisplayName("边界6.5 节点已跳过(J) → 抛异常")
    void execute_nodeSkipped_throws() {
        String instId = newInstance(WfConstants.STATUS_RUNNING);
        newNode(instId, 0, "创建节点", WfConstants.NODE_TYPE_CREATE, WfConstants.EXEC_YES);
        String skipped = newNode(instId, 1, "审核1", WfConstants.NODE_TYPE_REVIEW, WfConstants.EXEC_SKIP);
        assertThrows(JeecgBootException.class,
                () -> wfExecuteService.executeNode(skipped, "1", null, "同意", null, null, ME));
    }

    @Test
    @DisplayName("边界6.6 无效处理结果ifpass → 抛异常")
    void execute_invalidIfpass_throws() {
        String[] n = newStandardFlow(1);
        JeecgBootException ex = assertThrows(JeecgBootException.class,
                () -> wfExecuteService.executeNode(n[1], "99", null, "x", null, null, ME));
        assertTrue(ex.getMessage().contains("无效的处理结果"));
    }

    // ============================================================
    // 场景7：追加意见 addOpinion
    // ============================================================

    @Test
    @DisplayName("场景7.1 对已处理节点由处理人追加意见 → 成功，执行记录=4")
    void addOpinion_byHandler_ok() throws Exception {
        String[] n = newStandardFlow(2);
        // 先通过，产生 create_by=ME 的执行记录，节点变Y
        wfExecuteService.executeNode(n[1], "1", null, "同意", null, null, ME);

        wfExecuteService.addOpinion(n[1], "补充：注意格式", ME);
        boolean has4 = wfExecuteMapper.selectByDtlId(n[1]).stream()
                .anyMatch(e -> "4".equals(e.getIfpass()));
        assertTrue(has4, "应生成一条追加意见记录(ifpass=4)");
    }

    @Test
    @DisplayName("边界7.2 对未处理节点追加意见 → 抛异常")
    void addOpinion_unprocessedNode_throws() {
        String[] n = newStandardFlow(1); // n[1] 仍为 N
        assertThrows(JeecgBootException.class,
                () -> wfExecuteService.addOpinion(n[1], "追加", ME));
    }

    @Test
    @DisplayName("边界7.3 非处理人追加意见 → 抛异常")
    void addOpinion_notHandler_throws() throws Exception {
        String[] n = newStandardFlow(2);
        wfExecuteService.executeNode(n[1], "1", null, "同意", null, null, ME);
        // 换一个用户ID追加
        assertThrows(JeecgBootException.class,
                () -> wfExecuteService.addOpinion(n[1], "他人追加", "otheruser"));
    }

    // ============================================================
    // 场景8：拿回 takeBack
    // ============================================================

    @Test
    @DisplayName("场景8.1 处理人拿回已通过节点(后续未处理) → 节点回N，流程流转中")
    void takeBack_ok() throws Exception {
        String[] n = newStandardFlow(2);
        wfExecuteService.executeNode(n[1], "1", null, "同意", null, null, ME); // n[1]=Y, n[2]=N

        wfExecuteService.takeBack(n[1], ME);
        assertEquals(WfConstants.EXEC_NO, reload(n[1]).getIfexec(), "拿回后节点应回到未执行N");
        assertEquals(WfConstants.STATUS_RUNNING, reloadInst(n[1]).getStatus());
    }

    @Test
    @DisplayName("边界8.2 后续节点已处理时拿回 → 抛异常")
    void takeBack_followerProcessed_throws() throws Exception {
        String[] n = newStandardFlow(2);
        wfExecuteService.executeNode(n[1], "1", null, "同意", null, null, ME); // n[1]=Y
        wfExecuteService.executeNode(n[2], "1", null, "同意", null, null, ME); // n[2]=Y (末节点，流程结束)

        JeecgBootException ex = assertThrows(JeecgBootException.class,
                () -> wfExecuteService.takeBack(n[1], ME));
        assertTrue(ex.getMessage().contains("后续节点已处理"));
    }

    @Test
    @DisplayName("边界8.3 拿回未处理节点 → 抛异常")
    void takeBack_unprocessedNode_throws() {
        String[] n = newStandardFlow(1); // n[1]=N
        JeecgBootException ex = assertThrows(JeecgBootException.class,
                () -> wfExecuteService.takeBack(n[1], ME));
        assertTrue(ex.getMessage().contains("只能拿回已处理"));
    }

    @Test
    @DisplayName("边界8.4 非通过处理人拿回(不同意记录) → 抛异常")
    void takeBack_notPassHandler_throws() throws Exception {
        String[] n = newStandardFlow(2);
        // 不同意：节点变Y，但执行记录 ifpass=2，非"通过"
        wfExecuteService.executeNode(n[1], "2", null, "不符合要求", null, null, ME);
        JeecgBootException ex = assertThrows(JeecgBootException.class,
                () -> wfExecuteService.takeBack(n[1], ME));
        assertTrue(ex.getMessage().contains("通过处理人"));
    }

    // ============================================================
    // 数据一致性
    // ============================================================

    @Test
    @DisplayName("一致性9.1 退回后中间节点重置不影响目标之前节点")
    void consistency_returnDoesNotResetBeforeTarget() throws Exception {
        // 创建(0,Y) 审核1(1,Y 目标之前) 审核2(2,Y 目标) 审核3(3,Y 中间) 审核4(4,N 当前)
        String instId = newInstance(WfConstants.STATUS_RUNNING);
        newNode(instId, 0, "创建节点", WfConstants.NODE_TYPE_CREATE, WfConstants.EXEC_YES);
        String before = newNode(instId, 1, "审核1", WfConstants.NODE_TYPE_REVIEW, WfConstants.EXEC_YES);
        String target = newNode(instId, 2, "审核2", WfConstants.NODE_TYPE_REVIEW, WfConstants.EXEC_YES);
        String mid = newNode(instId, 3, "审核3", WfConstants.NODE_TYPE_REVIEW, WfConstants.EXEC_YES);
        String cur = newNode(instId, 4, "审核4", WfConstants.NODE_TYPE_REVIEW, WfConstants.EXEC_NO);

        wfExecuteService.executeNode(cur, "3", target, "退回到审核2", null, null, ME);

        assertEquals(WfConstants.EXEC_YES, reload(before).getIfexec(), "目标之前节点不应被重置");
        assertEquals(WfConstants.EXEC_RETURN, reload(target).getIfexec());
        assertEquals(WfConstants.EXEC_NO, reload(mid).getIfexec(), "目标与当前之间的节点应重置N");
    }

    // ============================================================
    // 场景10：多操作组合序列（生命周期）
    // ============================================================

    @Test
    @DisplayName("场景10.1 完整生命周期：连续通过两个节点 → 末节点结束，共2条通过记录")
    void seq_fullLifecycle_twoPass_ended() throws Exception {
        String[] n = newStandardFlow(2); // 创建 + 审核1 + 审核2
        wfExecuteService.executeNode(n[1], "1", null, "同意", null, null, ME);
        assertEquals(WfConstants.STATUS_RUNNING, reloadInst(n[1]).getStatus(), "过第1个节点后仍流转中");
        wfExecuteService.executeNode(n[2], "1", null, "同意", null, null, ME);
        assertEquals(WfConstants.STATUS_ENDED, reloadInst(n[2]).getStatus(), "过末节点后流程结束");
        assertEquals(WfConstants.EXEC_YES, reload(n[1]).getIfexec());
        assertEquals(WfConstants.EXEC_YES, reload(n[2]).getIfexec());
    }

    @Test
    @DisplayName("场景10.2 退回后目标节点(R)可被再次处理 → 通过；退回次数ifjump保留不被重置")
    void seq_returnThenReprocessTarget() throws Exception {
        // 创建(0,Y) 目标(1,Y) 当前(2,N)
        String instId = newInstance(WfConstants.STATUS_RUNNING);
        newNode(instId, 0, "创建节点", WfConstants.NODE_TYPE_CREATE, WfConstants.EXEC_YES);
        String target = newNode(instId, 1, "审核1", WfConstants.NODE_TYPE_REVIEW, WfConstants.EXEC_YES);
        String cur = newNode(instId, 2, "审核2", WfConstants.NODE_TYPE_REVIEW, WfConstants.EXEC_NO);

        wfExecuteService.executeNode(cur, "3", target, "退回重改", null, null, ME);
        assertEquals(WfConstants.EXEC_RETURN, reload(target).getIfexec(), "退回后目标为R");
        assertEquals("1", reload(target).getIfjump());

        // R 状态既非 Y 也非 J，应能再次处理
        wfExecuteService.executeNode(target, "1", null, "重改后同意", null, null, ME);
        assertEquals(WfConstants.EXEC_YES, reload(target).getIfexec(), "R节点重处理后应为Y");
        assertEquals("1", reload(target).getIfjump(), "通过不应重置退回次数");
    }

    @Test
    @DisplayName("场景10.3 跳过后到达目标节点可继续处理 → 目标为唯一未跳过末节点，通过即结束")
    void seq_skipThenPassTarget_ended() throws Exception {
        // 创建(0,Y) 当前(1,N) 中间(2,N) 目标(3,N)
        String instId = newInstance(WfConstants.STATUS_RUNNING);
        newNode(instId, 0, "创建节点", WfConstants.NODE_TYPE_CREATE, WfConstants.EXEC_YES);
        String cur = newNode(instId, 1, "审核1", WfConstants.NODE_TYPE_REVIEW, WfConstants.EXEC_NO);
        newNode(instId, 2, "审核2", WfConstants.NODE_TYPE_REVIEW, WfConstants.EXEC_NO);
        String target = newNode(instId, 3, "审核3", WfConstants.NODE_TYPE_REVIEW, WfConstants.EXEC_NO);

        wfExecuteService.executeNode(cur, "3", target, "跳过中间", null, null, ME);
        assertEquals(WfConstants.STATUS_RUNNING, reloadInst(cur).getStatus());

        wfExecuteService.executeNode(target, "1", null, "同意", null, null, ME);
        assertEquals(WfConstants.STATUS_ENDED, reloadInst(target).getStatus(),
                "前序均已跳过，目标为末节点，通过后流程结束");
    }
    @Test
    @DisplayName("场景10.4 通过后拿回再通过 → 最终Y，执行记录含 通过(1)+拿回(5)+通过(1)")
    void seq_passTakeBackRepass() throws Exception {
        String[] n = newStandardFlow(2);
        wfExecuteService.executeNode(n[1], "1", null, "同意", null, null, ME);
        wfExecuteService.takeBack(n[1], ME);
        assertEquals(WfConstants.EXEC_NO, reload(n[1]).getIfexec(), "拿回后应回N");
        wfExecuteService.executeNode(n[1], "1", null, "再次同意", null, null, ME);

        assertEquals(WfConstants.EXEC_YES, reload(n[1]).getIfexec());
        long pass = wfExecuteMapper.selectByDtlId(n[1]).stream().filter(e -> "1".equals(e.getIfpass())).count();
        long back = wfExecuteMapper.selectByDtlId(n[1]).stream().filter(e -> "5".equals(e.getIfpass())).count();
        assertEquals(2, pass, "应有2条通过记录");
        assertEquals(1, back, "应有1条拿回记录");
    }

    // ============================================================
    // 场景11：附件（filename/filecontent）功能测试
    // ============================================================

    @Test
    @DisplayName("场景11.1 通过时携带附件 → 附件名与二进制内容正确落库并可回读")
    void file_passWithAttachment_persisted() throws Exception {
        String[] n = newStandardFlow(1);
        byte[] content = "PDF-BINARY-内容".getBytes("UTF-8");
        wfExecuteService.executeNode(n[1], "1", null, "同意", "审核意见.pdf", content, ME);

        WfExecute rec = wfExecuteMapper.selectByDtlId(n[1]).get(0);
        assertEquals("审核意见.pdf", rec.getFilename(), "附件名应落库");
        assertNotNull(rec.getFilecontent(), "附件内容应落库");
        assertArrayEquals(content, rec.getFilecontent(), "附件二进制内容应完整回读");
    }

    @Test
    @DisplayName("场景11.2 不同意时携带附件 → 附件同样落库")
    void file_rejectWithAttachment_persisted() throws Exception {
        String[] n = newStandardFlow(1);
        byte[] content = new byte[]{0x00, 0x01, (byte) 0xFF, 0x7F};
        wfExecuteService.executeNode(n[1], "2", null, "不合格见附件", "问题清单.xlsx", content, ME);

        WfExecute rec = wfExecuteMapper.selectByDtlId(n[1]).get(0);
        assertEquals("问题清单.xlsx", rec.getFilename());
        assertArrayEquals(content, rec.getFilecontent());
    }

    // ============================================================
    // 场景12：追加意见 addOpinion 补充场景
    // ============================================================

    @Test
    @DisplayName("场景12.1 对\"不同意\"后的节点由处理人追加意见 → 成功（只校验已处理+处理人，不看ifpass）")
    void addOpinion_onRejectedNode_ok() throws Exception {
        String[] n = newStandardFlow(2);
        wfExecuteService.executeNode(n[1], "2", null, "不合格", null, null, ME); // 节点变Y
        wfExecuteService.addOpinion(n[1], "补充：请参照GJB", ME);

        boolean has4 = wfExecuteMapper.selectByDtlId(n[1]).stream().anyMatch(e -> "4".equals(e.getIfpass()));
        assertTrue(has4, "不同意后仍可由处理人追加意见");
    }

    @Test
    @DisplayName("场景12.2 同一处理人多次追加意见 → 生成多条追加记录")
    void addOpinion_multipleTimes() throws Exception {
        String[] n = newStandardFlow(2);
        wfExecuteService.executeNode(n[1], "1", null, "同意", null, null, ME);
        wfExecuteService.addOpinion(n[1], "追加1", ME);
        wfExecuteService.addOpinion(n[1], "追加2", ME);

        long cnt = wfExecuteMapper.selectByDtlId(n[1]).stream().filter(e -> "4".equals(e.getIfpass())).count();
        assertEquals(2, cnt, "应生成2条追加意见记录");
    }

    @Test
    @DisplayName("边界12.3 对不存在的节点追加意见 → 抛异常")
    void addOpinion_nodeNotExist_throws() {
        assertThrows(JeecgBootException.class,
                () -> wfExecuteService.addOpinion("999999999999", "追加", ME));
    }

    // ============================================================
    // 场景13：拿回 takeBack 补充场景
    // ============================================================

    @Test
    @DisplayName("场景13.1 后续节点为跳过(J)时拿回 → 成功（仅Y后续阻止拿回）")
    void takeBack_followerSkipped_ok() throws Exception {
        // 创建(0,Y) 当前(1,N) 后续(2,N)
        String instId = newInstance(WfConstants.STATUS_RUNNING);
        newNode(instId, 0, "创建节点", WfConstants.NODE_TYPE_CREATE, WfConstants.EXEC_YES);
        String cur = newNode(instId, 1, "审核1", WfConstants.NODE_TYPE_REVIEW, WfConstants.EXEC_NO);
        String follower = newNode(instId, 2, "审核2", WfConstants.NODE_TYPE_REVIEW, WfConstants.EXEC_NO);

        wfExecuteService.executeNode(cur, "1", null, "同意", null, null, ME); // cur=Y, follower=N, RUNNING
        // 手工把后续置为跳过
        WfInstanceDtl f = reload(follower);
        f.setIfexec(WfConstants.EXEC_SKIP);
        wfInstanceDtlMapper.updateById(f);

        wfExecuteService.takeBack(cur, ME);
        assertEquals(WfConstants.EXEC_NO, reload(cur).getIfexec(), "后续为J不阻止拿回");
        assertEquals(WfConstants.STATUS_RUNNING, reloadInst(cur).getStatus());
    }

    @Test
    @DisplayName("场景13.2 拿回生成一条拿回记录(ifpass=5)")
    void takeBack_createsRecord() throws Exception {
        String[] n = newStandardFlow(2);
        wfExecuteService.executeNode(n[1], "1", null, "同意", null, null, ME);
        wfExecuteService.takeBack(n[1], ME);

        boolean has5 = wfExecuteMapper.selectByDtlId(n[1]).stream().anyMatch(e -> "5".equals(e.getIfpass()));
        assertTrue(has5, "拿回应生成ifpass=5记录");
    }

    @Test
    @DisplayName("边界13.3 拿回不存在的节点 → 抛异常")
    void takeBack_nodeNotExist_throws() {
        assertThrows(JeecgBootException.class,
                () -> wfExecuteService.takeBack("999999999999", ME));
    }

    // ============================================================
    // 场景14：跳转边界补充
    // ============================================================

    @Test
    @DisplayName("边界14.1 跳过到相邻后一节点 → 仅当前节点标记J，目标保持N")
    void jump_skipToAdjacent_onlyCurrentMarked() throws Exception {
        String instId = newInstance(WfConstants.STATUS_RUNNING);
        newNode(instId, 0, "创建节点", WfConstants.NODE_TYPE_CREATE, WfConstants.EXEC_YES);
        String cur = newNode(instId, 1, "审核1", WfConstants.NODE_TYPE_REVIEW, WfConstants.EXEC_NO);
        String target = newNode(instId, 2, "审核2", WfConstants.NODE_TYPE_REVIEW, WfConstants.EXEC_NO);

        wfExecuteService.executeNode(cur, "3", target, "跳过本节点", null, null, ME);
        assertEquals(WfConstants.EXEC_SKIP, reload(cur).getIfexec(), "当前(含)标记J");
        assertEquals(WfConstants.EXEC_NO, reload(target).getIfexec(), "相邻目标保持N");
    }

    @Test
    @DisplayName("边界14.2【行为取证】跳转目标=当前节点(同seqno)：走跳过分支但范围为空，节点仍N可继续处理，仅落1条跳转记录")
    void jump_toSelf_behavior() throws Exception {
        String[] n = newStandardFlow(2);
        wfExecuteService.executeNode(n[1], "3", n[1], "跳到自己", null, null, ME);

        assertEquals(WfConstants.EXEC_NO, reload(n[1]).getIfexec(),
                "target==current 走跳过分支，范围[cur,cur)为空，节点未被标记，仍为N");
        assertEquals(WfConstants.STATUS_RUNNING, reloadInst(n[1]).getStatus());
        long jumps = wfExecuteMapper.selectByDtlId(n[1]).stream().filter(e -> "3".equals(e.getIfpass())).count();
        assertEquals(1, jumps, "落1条跳转记录");
    }

    @Test
    @DisplayName("场景14.3 退回执行记录携带退回次数(ifjump)")
    void jump_returnRecordCarriesIfjump() throws Exception {
        String instId = newInstance(WfConstants.STATUS_RUNNING);
        newNode(instId, 0, "创建节点", WfConstants.NODE_TYPE_CREATE, WfConstants.EXEC_YES);
        String t = newNode(instId, 1, "审核1", WfConstants.NODE_TYPE_REVIEW, WfConstants.EXEC_YES);
        String cur = newNode(instId, 2, "审核2", WfConstants.NODE_TYPE_REVIEW, WfConstants.EXEC_NO);

        wfExecuteService.executeNode(cur, "3", t, "退回", null, null, ME);
        WfExecute rec = wfExecuteMapper.selectByDtlId(cur).get(0);
        assertEquals("3", rec.getIfpass());
        assertEquals("1", rec.getIfjump(), "退回执行记录应记录退回次数=1");
    }

    // ============================================================
    // 场景15：isLastNode 判定边界
    // ============================================================

    @Test
    @DisplayName("边界15.1 后续存在退回态(R)节点时通过 → R视为未跳过，仍有后续 → 流转中")
    void pass_withReturnedFollower_running() throws Exception {
        // 创建(0,Y) 当前(1,N) 后续(2,R)
        String instId = newInstance(WfConstants.STATUS_RUNNING);
        newNode(instId, 0, "创建节点", WfConstants.NODE_TYPE_CREATE, WfConstants.EXEC_YES);
        String cur = newNode(instId, 1, "审核1", WfConstants.NODE_TYPE_REVIEW, WfConstants.EXEC_NO);
        newNode(instId, 2, "审核2", WfConstants.NODE_TYPE_REVIEW, WfConstants.EXEC_RETURN);

        wfExecuteService.executeNode(cur, "1", null, "同意", null, null, ME);
        assertEquals(WfConstants.STATUS_RUNNING, reloadInst(cur).getStatus(),
                "后续R节点非跳过(J)，isLastNode=false → 流转中");
    }

    // ============================================================
    // 场景16【行为取证】：create_by 来源与 userId 参数
    // ============================================================

    @Test
    @DisplayName("行为16.1 执行记录 create_by 恒取自 Shiro 登录人，而非 executeNode 的 userId 参数")
    void behavior_createByFromShiroNotUserIdParam() throws Exception {
        String[] n = newStandardFlow(1);
        // 传入一个与登录人(ME=junit)不同的 userId
        wfExecuteService.executeNode(n[1], "1", null, "同意", null, null, "ghost-user");

        WfExecute rec = wfExecuteMapper.selectByDtlId(n[1]).get(0);
        assertEquals(ME, rec.getCreateBy(),
                "create_by 由 MybatisInterceptor 从 Shiro 回填=junit，userId参数(ghost-user)被忽略");
    }

    @Test
    @DisplayName("行为16.2 因create_by取自Shiro：addOpinion 用真实登录人可通过，用参数userId=登录人一致时成立")
    void behavior_addOpinionHandlerMatchesShiro() throws Exception {
        String[] n = newStandardFlow(2);
        wfExecuteService.executeNode(n[1], "1", null, "同意", null, null, "ghost-user"); // 记录create_by=ME
        // addOpinion 用 ME 校验（与落库 create_by 一致）→ 通过
        wfExecuteService.addOpinion(n[1], "补充", ME);
        boolean has4 = wfExecuteMapper.selectByDtlId(n[1]).stream().anyMatch(e -> "4".equals(e.getIfpass()));
        assertTrue(has4);
        // 若换用其它 userId（与落库 create_by 不一致）→ 抛异常
        assertThrows(JeecgBootException.class, () -> wfExecuteService.addOpinion(n[1], "补充2", "ghost-user"));
    }
}
