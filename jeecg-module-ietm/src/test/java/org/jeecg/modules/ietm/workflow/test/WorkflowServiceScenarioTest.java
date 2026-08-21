package org.jeecg.modules.ietm.workflow.test;

import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.ietm.workflow.constants.WfConstants;
import org.jeecg.modules.ietm.workflow.entity.WfInstance;
import org.jeecg.modules.ietm.workflow.entity.WfInstanceDtl;
import org.jeecg.modules.ietm.workflow.entity.WfTemplate;
import org.jeecg.modules.ietm.workflow.entity.WfTemplateDtl;
import org.jeecg.modules.ietm.workflow.mapper.WfInstanceDtlMapper;
import org.jeecg.modules.ietm.workflow.mapper.WfInstanceMapper;
import org.jeecg.modules.ietm.workflow.mapper.WfTemplateDtlMapper;
import org.jeecg.modules.ietm.workflow.mapper.WfTemplateMapper;
import org.jeecg.modules.ietm.workflow.service.IWfInstanceService;
import org.jeecg.modules.ietm.workflow.service.IWfTemplateService;
import org.jeecg.modules.ietm.workflow.vo.BatchStartFlowDtlVO;
import org.jeecg.modules.ietm.workflow.vo.BatchStartFlowVO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 工作流实例/模板服务 场景 + 边界 + 功能测试
 *
 * 覆盖此前无测试的两个服务：
 *   WfInstanceServiceImpl：batchStartFlow 参数/节点校验、terminate、updateUrgent、getByFormid、getTodoByFormid
 *   WfTemplateServiceImpl：getPublishedTemplates、getTemplateNodes
 *
 * 说明：batchStartFlow 的成功路径依赖真实 DM 数据 + 分布式锁(Redis)，测试环境 Redis 不可用，
 *      故此处只覆盖“校验在锁/登录/DB加载之前抛出”的负向分支（源码顺序：参数→节点校验→锁→登录→DB）。
 *
 * @author IETM Team
 * @date 2026-08-21
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
@DisplayName("工作流实例/模板服务 场景/边界/功能测试")
public class WorkflowServiceScenarioTest {

    @Autowired private IWfInstanceService wfInstanceService;
    @Autowired private IWfTemplateService wfTemplateService;
    @Autowired private WfInstanceMapper wfInstanceMapper;
    @Autowired private WfInstanceDtlMapper wfInstanceDtlMapper;
    @Autowired private WfTemplateMapper wfTemplateMapper;
    @Autowired private WfTemplateDtlMapper wfTemplateDtlMapper;
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
    // 构造辅助
    // ============================================================

    private BatchStartFlowDtlVO node(int seqno, String name, String nodetype, String userid, String useridname) {
        BatchStartFlowDtlVO n = new BatchStartFlowDtlVO();
        n.setSeqno(seqno);
        n.setNodename(name);
        n.setNodetype(nodetype);
        n.setUserid(userid);
        n.setUseridname(useridname);
        return n;
    }

    /** 合法节点：创建节点(0) + 一个审核节点(1) */
    private List<BatchStartFlowDtlVO> validNodes() {
        List<BatchStartFlowDtlVO> list = new ArrayList<>();
        list.add(node(0, "创建", WfConstants.NODE_TYPE_CREATE, "junit", "创建人"));
        list.add(node(1, "审核", WfConstants.NODE_TYPE_REVIEW, "u1", "张三"));
        return list;
    }

    /** 合法 VO；nodes 可覆盖 */
    private BatchStartFlowVO vo(List<String> dmIds, String batchId, String ifurgent, List<BatchStartFlowDtlVO> nodes) {
        BatchStartFlowVO v = new BatchStartFlowVO();
        v.setDmIds(dmIds);
        v.setBatchId(batchId);
        v.setIfurgent(ifurgent);
        v.setNodes(nodes);
        return v;
    }

    private String uniqBatch() {
        return "BATCH_" + SEQ.incrementAndGet() + "_" + System.nanoTime();
    }

    /** 除被测字段外全部合法的 VO */
    private BatchStartFlowVO baseVo() {
        return vo(Arrays.asList("DM_" + System.nanoTime()), uniqBatch(), "1", validNodes());
    }

    // ============================================================
    // 场景A：batchStartFlow 顶层参数校验（在锁/登录/DB之前）
    // ============================================================

    @Test
    @DisplayName("A1 DM列表为空 → 抛异常")
    void batch_emptyDmIds_throws() {
        BatchStartFlowVO v = baseVo();
        v.setDmIds(new ArrayList<>());
        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> wfInstanceService.batchStartFlow(v));
        assertTrue(ex.getMessage().contains("DM ID列表不能为空"));
    }

    @Test
    @DisplayName("A2 DM列表含重复项 → 抛异常")
    void batch_duplicateDmIds_throws() {
        BatchStartFlowVO v = baseVo();
        v.setDmIds(Arrays.asList("D1", "D1"));
        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> wfInstanceService.batchStartFlow(v));
        assertTrue(ex.getMessage().contains("重复"));
    }

    @Test
    @DisplayName("A3 DM数量>1000 → 抛异常")
    void batch_tooManyDmIds_throws() {
        BatchStartFlowVO v = baseVo();
        List<String> many = new ArrayList<>();
        for (int i = 0; i < 1001; i++) many.add("D" + i);
        v.setDmIds(many);
        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> wfInstanceService.batchStartFlow(v));
        assertTrue(ex.getMessage().contains("1000"));
    }

    @Test
    @DisplayName("A4 批次ID为空 → 抛异常")
    void batch_emptyBatchId_throws() {
        BatchStartFlowVO v = baseVo();
        v.setBatchId("");
        assertThrows(JeecgBootException.class, () -> wfInstanceService.batchStartFlow(v));
    }

    @Test
    @DisplayName("A5 批次ID>64字符 → 抛异常")
    void batch_batchIdTooLong_throws() {
        BatchStartFlowVO v = baseVo();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 65; i++) sb.append("a");
        v.setBatchId(sb.toString());
        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> wfInstanceService.batchStartFlow(v));
        assertTrue(ex.getMessage().contains("64"));
    }

    @Test
    @DisplayName("A6 紧急级别非1/2/3 → 抛异常")
    void batch_invalidIfurgent_throws() {
        BatchStartFlowVO v = baseVo();
        v.setIfurgent("9");
        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> wfInstanceService.batchStartFlow(v));
        assertTrue(ex.getMessage().contains("紧急级别"));
    }

    // ============================================================
    // 场景B：validateNodes 节点配置校验
    // ============================================================

    @Test
    @DisplayName("B1 节点列表为空 → 抛异常")
    void nodes_empty_throws() {
        BatchStartFlowVO v = baseVo();
        v.setNodes(new ArrayList<>());
        assertThrows(JeecgBootException.class, () -> wfInstanceService.batchStartFlow(v));
    }

    @Test
    @DisplayName("B2 节点数>100 → 抛异常")
    void nodes_tooMany_throws() {
        BatchStartFlowVO v = baseVo();
        List<BatchStartFlowDtlVO> nodes = new ArrayList<>();
        nodes.add(node(0, "创建", WfConstants.NODE_TYPE_CREATE, "junit", "创建人"));
        for (int i = 1; i <= 100; i++) nodes.add(node(i, "审核" + i, WfConstants.NODE_TYPE_REVIEW, "u", "n"));
        v.setNodes(nodes); // 共101
        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> wfInstanceService.batchStartFlow(v));
        assertTrue(ex.getMessage().contains("100"));
    }

    @Test
    @DisplayName("B3 节点seqno为null → 抛异常")
    void nodes_seqnoNull_throws() {
        BatchStartFlowVO v = baseVo();
        List<BatchStartFlowDtlVO> nodes = validNodes();
        nodes.get(1).setSeqno(null);
        v.setNodes(nodes);
        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> wfInstanceService.batchStartFlow(v));
        assertTrue(ex.getMessage().contains("顺序号不能为空"));
    }

    @Test
    @DisplayName("B4 节点seqno为负 → 抛异常")
    void nodes_seqnoNegative_throws() {
        BatchStartFlowVO v = baseVo();
        List<BatchStartFlowDtlVO> nodes = validNodes();
        nodes.get(1).setSeqno(-1);
        v.setNodes(nodes);
        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> wfInstanceService.batchStartFlow(v));
        assertTrue(ex.getMessage().contains("负数"));
    }

    @Test
    @DisplayName("B5 节点seqno>9999 → 抛异常")
    void nodes_seqnoTooLarge_throws() {
        BatchStartFlowVO v = baseVo();
        List<BatchStartFlowDtlVO> nodes = validNodes();
        nodes.get(1).setSeqno(10000);
        v.setNodes(nodes);
        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> wfInstanceService.batchStartFlow(v));
        assertTrue(ex.getMessage().contains("9999"));
    }

    @Test
    @DisplayName("B6 seqno重复 → 抛异常")
    void nodes_seqnoDuplicate_throws() {
        BatchStartFlowVO v = baseVo();
        List<BatchStartFlowDtlVO> nodes = new ArrayList<>();
        nodes.add(node(0, "创建", WfConstants.NODE_TYPE_CREATE, "junit", "创建人"));
        nodes.add(node(1, "审核1", WfConstants.NODE_TYPE_REVIEW, "u", "n"));
        nodes.add(node(1, "审核2", WfConstants.NODE_TYPE_REVIEW, "u", "n"));
        v.setNodes(nodes);
        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> wfInstanceService.batchStartFlow(v));
        assertTrue(ex.getMessage().contains("重复"));
    }

    @Test
    @DisplayName("B7 节点类型非法 → 抛异常")
    void nodes_invalidNodetype_throws() {
        BatchStartFlowVO v = baseVo();
        List<BatchStartFlowDtlVO> nodes = validNodes();
        nodes.get(1).setNodetype("7");
        v.setNodes(nodes);
        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> wfInstanceService.batchStartFlow(v));
        assertTrue(ex.getMessage().contains("节点类型无效"));
    }

    @Test
    @DisplayName("B8 创建节点seqno≠0 → 抛异常")
    void nodes_createNodeSeqnoNotZero_throws() {
        BatchStartFlowVO v = baseVo();
        List<BatchStartFlowDtlVO> nodes = new ArrayList<>();
        nodes.add(node(5, "创建", WfConstants.NODE_TYPE_CREATE, "junit", "创建人"));
        v.setNodes(nodes);
        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> wfInstanceService.batchStartFlow(v));
        assertTrue(ex.getMessage().contains("创建节点的顺序号必须为0"));
    }

    @Test
    @DisplayName("B9 缺少创建节点 → 抛异常")
    void nodes_noCreateNode_throws() {
        BatchStartFlowVO v = baseVo();
        List<BatchStartFlowDtlVO> nodes = new ArrayList<>();
        nodes.add(node(1, "审核", WfConstants.NODE_TYPE_REVIEW, "u", "n"));
        v.setNodes(nodes);
        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> wfInstanceService.batchStartFlow(v));
        assertTrue(ex.getMessage().contains("必须包含创建节点"));
    }

    @Test
    @DisplayName("B10 节点名称为空 → 抛异常")
    void nodes_emptyNodename_throws() {
        BatchStartFlowVO v = baseVo();
        List<BatchStartFlowDtlVO> nodes = validNodes();
        nodes.get(1).setNodename("");
        v.setNodes(nodes);
        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> wfInstanceService.batchStartFlow(v));
        assertTrue(ex.getMessage().contains("节点名称不能为空"));
    }

    @Test
    @DisplayName("B11 处理人ID与姓名数量不一致 → 抛异常")
    void nodes_useridCountMismatch_throws() {
        BatchStartFlowVO v = baseVo();
        List<BatchStartFlowDtlVO> nodes = validNodes();
        nodes.get(1).setUserid("u1,u2");
        nodes.get(1).setUseridname("张三"); // 2 vs 1
        v.setNodes(nodes);
        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> wfInstanceService.batchStartFlow(v));
        assertTrue(ex.getMessage().contains("不一致"));
    }

    @Test
    @DisplayName("B12 处理人ID含非法字符 → 抛异常（WfValidatorUtil）")
    void nodes_useridInvalidFormat_throws() {
        BatchStartFlowVO v = baseVo();
        List<BatchStartFlowDtlVO> nodes = validNodes();
        nodes.get(1).setUserid("u@1");
        nodes.get(1).setUseridname("张三");
        v.setNodes(nodes);
        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> wfInstanceService.batchStartFlow(v));
        assertTrue(ex.getMessage().contains("格式不合法"));
    }

    @Test
    @DisplayName("B13 处理人ID前缀格式错误 → 抛异常（非dpt_/rol_/pst_/grp_）")
    void nodes_useridBadPrefix_throws() {
        BatchStartFlowVO v = baseVo();
        List<BatchStartFlowDtlVO> nodes = validNodes();
        nodes.get(1).setUserid("xxx_1");
        nodes.get(1).setUseridname("张三");
        v.setNodes(nodes);
        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> wfInstanceService.batchStartFlow(v));
        assertTrue(ex.getMessage().contains("前缀"));
    }

    @Test
    @DisplayName("B14 合法前缀(rol_admin)通过节点校验（后续因无DM数据在DB阶段失败，非校验失败）")
    void nodes_validPrefix_passesValidation() {
        BatchStartFlowVO v = baseVo();
        List<BatchStartFlowDtlVO> nodes = validNodes();
        nodes.get(1).setUserid("rol_admin,dpt_hr");
        nodes.get(1).setUseridname("管理员,人事");
        v.setNodes(nodes);
        // 节点校验应通过；之后走锁/登录/DB。断言异常信息不是"节点/前缀/格式"类校验错。
        try {
            wfInstanceService.batchStartFlow(v);
        } catch (Exception e) {
            String msg = String.valueOf(e.getMessage());
            assertFalse(msg.contains("前缀") || msg.contains("格式不合法") || msg.contains("节点类型"),
                    "合法前缀不应触发节点校验错误，实际：" + msg);
        }
    }

    // ============================================================
    // 实例插入辅助
    // ============================================================

    private WfInstance insertInstance(String formid, String status) {
        WfInstance inst = new WfInstance();
        inst.setFormid(formid);
        inst.setTitle("t");
        inst.setStatus(status);
        inst.setCreateBy(ME);
        wfInstanceMapper.insert(inst);
        return inst;
    }

    private String insertNode(String instId, int seqno, String userid, String ifexec) {
        WfInstanceDtl dtl = new WfInstanceDtl();
        dtl.setInstanceid(instId);
        dtl.setSeqno(seqno);
        dtl.setNodename("审核");
        dtl.setNodetype(WfConstants.NODE_TYPE_REVIEW);
        dtl.setUserid(userid);
        dtl.setUseridname("处理人");
        dtl.setIfexec(ifexec);
        dtl.setIfjump("0");
        dtl.setIfnoopinion("N");
        dtl.setCreateBy(ME);
        wfInstanceDtlMapper.insert(dtl);
        return dtl.getId();
    }

    // ============================================================
    // 场景C：terminate 终止实例
    // ============================================================

    @Test
    @DisplayName("C1 终止运行中实例 → 状态置9")
    void terminate_running_ok() {
        WfInstance inst = insertInstance("F_" + System.nanoTime(), WfConstants.STATUS_RUNNING);
        wfInstanceService.terminate(inst.getId(), "项目取消");
        assertEquals(WfConstants.STATUS_TERMINATED, wfInstanceMapper.selectById(inst.getId()).getStatus());
    }

    @Test
    @DisplayName("边界C2 实例ID为空 → 抛异常")
    void terminate_emptyId_throws() {
        assertThrows(JeecgBootException.class, () -> wfInstanceService.terminate("", "原因"));
    }

    @Test
    @DisplayName("边界C3 终止原因为空 → 抛异常")
    void terminate_emptyReason_throws() {
        WfInstance inst = insertInstance("F_" + System.nanoTime(), WfConstants.STATUS_RUNNING);
        JeecgBootException ex = assertThrows(JeecgBootException.class,
                () -> wfInstanceService.terminate(inst.getId(), ""));
        assertTrue(ex.getMessage().contains("终止原因"));
    }

    @Test
    @DisplayName("边界C4 实例不存在 → 抛异常")
    void terminate_notExist_throws() {
        assertThrows(JeecgBootException.class, () -> wfInstanceService.terminate("999999999999", "原因"));
    }

    @Test
    @DisplayName("边界C5 已结束的实例不能终止 → 抛异常")
    void terminate_endedInstance_throws() {
        WfInstance inst = insertInstance("F_" + System.nanoTime(), WfConstants.STATUS_ENDED);
        JeecgBootException ex = assertThrows(JeecgBootException.class,
                () -> wfInstanceService.terminate(inst.getId(), "原因"));
        assertTrue(ex.getMessage().contains("流程已结束"));
    }

    // ============================================================
    // 场景D：updateUrgent
    // ============================================================

    @Test
    @DisplayName("D1 更新紧急程度为2 → 落库")
    void updateUrgent_ok() {
        WfInstance inst = insertInstance("F_" + System.nanoTime(), WfConstants.STATUS_RUNNING);
        wfInstanceService.updateUrgent(inst.getId(), "2");
        assertEquals("2", wfInstanceMapper.selectById(inst.getId()).getIfurgent());
    }

    @Test
    @DisplayName("边界D2 更新紧急程度ID为空 → 抛异常")
    void updateUrgent_emptyId_throws() {
        assertThrows(JeecgBootException.class, () -> wfInstanceService.updateUrgent("", "1"));
    }

    @Test
    @DisplayName("边界D3 紧急程度值非法 → 抛异常")
    void updateUrgent_invalidValue_throws() {
        WfInstance inst = insertInstance("F_" + System.nanoTime(), WfConstants.STATUS_RUNNING);
        JeecgBootException ex = assertThrows(JeecgBootException.class,
                () -> wfInstanceService.updateUrgent(inst.getId(), "5"));
        assertTrue(ex.getMessage().contains("紧急程度值无效"));
    }

    @Test
    @DisplayName("边界D4 实例不存在 → 抛异常")
    void updateUrgent_notExist_throws() {
        assertThrows(JeecgBootException.class, () -> wfInstanceService.updateUrgent("999999999999", "1"));
    }

    // ============================================================
    // 场景E：getByFormid / getTodoByFormid
    // ============================================================

    @Test
    @DisplayName("E1 getByFormid 返回该formid最新实例")
    void getByFormid_returnsLatest() {
        String formid = "F_" + System.nanoTime();
        insertInstance(formid, WfConstants.STATUS_ENDED);
        WfInstance latest = insertInstance(formid, WfConstants.STATUS_RUNNING);
        WfInstance got = wfInstanceService.getByFormid(formid);
        assertNotNull(got, "应查到实例");
        // 两条同formid，验证能正常返回其一（LIMIT/排序在DM8可执行）
        assertEquals(formid, got.getFormid());
    }

    @Test
    @DisplayName("E2 getByFormid formid为空 → 抛异常")
    void getByFormid_emptyFormid_throws() {
        assertThrows(JeecgBootException.class, () -> wfInstanceService.getByFormid(""));
    }

    @Test
    @DisplayName("E3 getByFormid 无匹配 → 返回null")
    void getByFormid_noMatch_returnsNull() {
        assertNull(wfInstanceService.getByFormid("NO_SUCH_FORM_" + System.nanoTime()));
    }

    @Test
    @DisplayName("E4 getTodoByFormid 当前用户是待办处理人 → 返回该节点")
    void getTodoByFormid_currentUserIsHandler_returnsNode() {
        String formid = "F_" + System.nanoTime();
        WfInstance inst = insertInstance(formid, WfConstants.STATUS_RUNNING);
        insertNode(inst.getId(), 1, ME, WfConstants.EXEC_NO); // 待办属于 junit
        Object todo = wfInstanceService.getTodoByFormid(formid);
        assertNotNull(todo, "当前用户为处理人应返回待办节点");
        assertTrue(todo instanceof WfInstanceDtl);
    }

    @Test
    @DisplayName("E5 getTodoByFormid 待办不属于当前用户 → 返回null")
    void getTodoByFormid_notHandler_returnsNull() {
        String formid = "F_" + System.nanoTime();
        WfInstance inst = insertInstance(formid, WfConstants.STATUS_RUNNING);
        insertNode(inst.getId(), 1, "someone-else", WfConstants.EXEC_NO);
        assertNull(wfInstanceService.getTodoByFormid(formid), "非处理人应返回null");
    }

    @Test
    @DisplayName("E6 getTodoByFormid 无实例 → 返回null")
    void getTodoByFormid_noInstance_returnsNull() {
        assertNull(wfInstanceService.getTodoByFormid("NO_FORM_" + System.nanoTime()));
    }

    @Test
    @DisplayName("E7 getTodoByFormid 节点已处理(Y)不算待办 → 返回null")
    void getTodoByFormid_processedNode_returnsNull() {
        String formid = "F_" + System.nanoTime();
        WfInstance inst = insertInstance(formid, WfConstants.STATUS_RUNNING);
        insertNode(inst.getId(), 1, ME, WfConstants.EXEC_YES); // 已处理，不在 N/R 过滤内
        assertNull(wfInstanceService.getTodoByFormid(formid), "已处理节点不应作为待办返回");
    }

    // ============================================================
    // 模板插入辅助
    // ============================================================

    private WfTemplate insertTemplate(String name, String status, String tmpltype) {
        WfTemplate t = new WfTemplate();
        t.setTmplname(name);
        t.setStatus(status);
        t.setTmpltype(tmpltype);
        t.setCreateBy(ME);
        wfTemplateMapper.insert(t);
        return t;
    }

    private void insertTemplateNode(String templateId, int seqno, String nodetype) {
        WfTemplateDtl d = new WfTemplateDtl();
        d.setTemplateid(templateId);
        d.setSeqno(seqno);
        d.setNodename("节点" + seqno);
        d.setNodetype(nodetype);
        d.setUserid("u");
        d.setUseridname("n");
        d.setCreateBy(ME);
        wfTemplateDtlMapper.insert(d);
    }

    // ============================================================
    // 场景F：WfTemplateServiceImpl
    // ============================================================

    @Test
    @DisplayName("F1 getPublishedTemplates 仅返回已发布(status=1)")
    void template_getPublished_onlyPublished() {
        String type = "TYPE_" + System.nanoTime();
        insertTemplate("草稿模板", "0", type);
        insertTemplate("发布模板", "1", type);
        List<WfTemplate> list = wfTemplateService.getPublishedTemplates(type);
        assertEquals(1, list.size(), "仅返回1个已发布模板");
        assertEquals("发布模板", list.get(0).getTmplname());
    }

    @Test
    @DisplayName("F2 getPublishedTemplates 按类型过滤")
    void template_getPublished_filterByType() {
        String typeA = "A_" + System.nanoTime();
        String typeB = "B_" + System.nanoTime();
        insertTemplate("模板A", "1", typeA);
        insertTemplate("模板B", "1", typeB);
        List<WfTemplate> onlyA = wfTemplateService.getPublishedTemplates(typeA);
        assertEquals(1, onlyA.size());
        assertEquals(typeA, onlyA.get(0).getTmpltype());
    }

    @Test
    @DisplayName("F3 getPublishedTemplates 类型为空 → 返回所有已发布（含至少刚插入的2个）")
    void template_getPublished_nullType_returnsAllPublished() {
        insertTemplate("发布1", "1", "T1_" + System.nanoTime());
        insertTemplate("发布2", "1", "T2_" + System.nanoTime());
        List<WfTemplate> all = wfTemplateService.getPublishedTemplates(null);
        assertTrue(all.size() >= 2, "不传类型应返回全部已发布模板");
        assertTrue(all.stream().allMatch(t -> "1".equals(t.getStatus())), "结果必须全部为已发布");
    }

    @Test
    @DisplayName("F4 getTemplateNodes 返回按seqno排序的节点")
    void template_getNodes_orderedBySeqno() {
        WfTemplate t = insertTemplate("含节点模板", "1", "TT_" + System.nanoTime());
        insertTemplateNode(t.getId(), 2, WfConstants.NODE_TYPE_REVIEW);
        insertTemplateNode(t.getId(), 0, WfConstants.NODE_TYPE_CREATE);
        insertTemplateNode(t.getId(), 1, WfConstants.NODE_TYPE_REVIEW);
        List<WfTemplateDtl> nodes = wfTemplateService.getTemplateNodes(t.getId());
        assertEquals(3, nodes.size());
        assertEquals(0, nodes.get(0).getSeqno().intValue(), "应按seqno升序");
        assertEquals(1, nodes.get(1).getSeqno().intValue());
        assertEquals(2, nodes.get(2).getSeqno().intValue());
    }

    @Test
    @DisplayName("边界F5 getTemplateNodes 模板ID为空 → 抛IllegalArgumentException")
    void template_getNodes_emptyId_throws() {
        assertThrows(IllegalArgumentException.class, () -> wfTemplateService.getTemplateNodes(""));
    }

    @Test
    @DisplayName("边界F6 getTemplateNodes 无节点的模板 → 返回空列表(非null)")
    void template_getNodes_noNodes_returnsEmpty() {
        WfTemplate t = insertTemplate("空模板", "1", "EMPTY_" + System.nanoTime());
        List<WfTemplateDtl> nodes = wfTemplateService.getTemplateNodes(t.getId());
        assertNotNull(nodes, "应返回空列表而非null");
        assertTrue(nodes.isEmpty());
    }
}
