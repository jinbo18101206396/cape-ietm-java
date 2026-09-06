package org.jeecg.modules.ietm.ietmimport;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.ietm.icnmanage.entity.IetmIcnManage;
import org.jeecg.modules.ietm.icnmanage.service.IIetmIcnManageService;
import org.jeecg.modules.ietm.ietmimport.service.IIetmDmImportService;
import org.jeecg.modules.ietm.ietmimport.vo.ImportFileItemVO;
import org.jeecg.modules.ietm.projectconfigurationmanagement.entity.IetmProjectConfigurationManagement;
import org.jeecg.modules.ietm.projectconfigurationmanagement.service.IIetmProjectConfigurationManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ICN导入自动匹配构型节点测试
 * 验证修复：自动设置 cmNodeId、originator、security、uniqueId
 *
 * @author IETM Team
 * @date 2026-09-04
 */
@SpringBootTest
@ActiveProfiles("test")
@Slf4j
@Transactional
public class IcnImportAutoMatchTest {

    @Autowired
    private IIetmDmImportService importService;

    @Autowired
    private IIetmIcnManageService icnManageService;

    @Autowired
    private IIetmProjectConfigurationManagementService configurationService;

    private String testProjectId;
    private String testCmNodeId;

    @BeforeEach
    public void setUp() {
        // 创建测试项目构型节点
        testProjectId = UUID.randomUUID().toString().replace("-", "");

        // 创建根节点
        IetmProjectConfigurationManagement rootNode = new IetmProjectConfigurationManagement();
        rootNode.setId(UUID.randomUUID().toString().replace("-", ""));
        rootNode.setProjectId(testProjectId);
        rootNode.setPid("0");
        rootNode.setCode("ZB1");
        rootNode.setTitle("直升机");
        rootNode.setSeq(1);
        rootNode.setHasChild("1");
        configurationService.save(rootNode);

        // 创建子节点
        IetmProjectConfigurationManagement childNode = new IetmProjectConfigurationManagement();
        testCmNodeId = UUID.randomUUID().toString().replace("-", "");
        childNode.setId(testCmNodeId);
        childNode.setProjectId(testProjectId);
        childNode.setPid(rootNode.getId());
        childNode.setCode("A");
        childNode.setTitle("总体");
        childNode.setSeq(1);
        childNode.setHasChild("1");
        configurationService.save(childNode);

        // 创建孙节点
        IetmProjectConfigurationManagement grandChildNode = new IetmProjectConfigurationManagement();
        grandChildNode.setId(UUID.randomUUID().toString().replace("-", ""));
        grandChildNode.setProjectId(testProjectId);
        grandChildNode.setPid(childNode.getId());
        grandChildNode.setCode("05");
        grandChildNode.setTitle("机身");
        grandChildNode.setSeq(1);
        grandChildNode.setHasChild("0");
        configurationService.save(grandChildNode);

        log.info("测试环境初始化完成：projectId={}, 构型树：ZB1 -> A -> 05", testProjectId);
    }

    /**
     * 测试用例1：验证cmNodeId自动匹配
     */
    @Test
    public void testAutoMatchCmNodeId() throws Exception {
        // 准备：ICN文件名包含SNS = ZB1-A
        String icnFileName = "ICN-ZB1-A-05-00-00-00A-007A-A-001-01.CGM";

        ImportFileItemVO icnFile = new ImportFileItemVO();
        icnFile.setFileName(icnFileName);
        icnFile.setTempFilePath(null); // 暂不测试文件保存

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("username", "testUser");

        // 执行：调用私有方法（通过反射）
        java.lang.reflect.Method method = importService.getClass()
                .getDeclaredMethod("importSingleIcn", ImportFileItemVO.class, String.class, javax.servlet.http.HttpServletRequest.class);
        method.setAccessible(true);

        try {
            method.invoke(importService, icnFile, testProjectId, request);

            // 验证：查询导入的ICN记录
            QueryWrapper<IetmIcnManage> qw = new QueryWrapper<>();
            qw.eq("icn", "ICN-ZB1-A-05-00-00-00A-007A-A-001-01");
            IetmIcnManage icn = icnManageService.getOne(qw);

            assertNotNull(icn, "ICN记录应该被保存");
            assertNotNull(icn.getCmNodeId(), "cmNodeId应该被自动设置");
            assertEquals("ZB1-A", icn.getSns(), "SNS应该正确提取");

            // 验证cmNodeId匹配了正确的构型节点
            IetmProjectConfigurationManagement matchedNode = configurationService.getById(icn.getCmNodeId());
            assertNotNull(matchedNode, "匹配的构型节点应该存在");
            assertEquals("A", matchedNode.getCode(), "应该匹配到code=A的节点");

            log.info("✅ 测试通过：cmNodeId自动匹配成功，ICN={}, SNS={}, cmNodeId={}",
                    icn.getIcn(), icn.getSns(), icn.getCmNodeId());

        } catch (Exception e) {
            fail("导入ICN失败：" + e.getMessage());
        }
    }

    /**
     * 测试用例2：验证originator、security、uniqueId字段解析
     */
    @Test
    public void testParseRequiredFields() throws Exception {
        // 准备：ICN文件名
        // parts[4]=00 (originator), parts[5]=00A (uniqueId), parts[8]=01 (security)
        String icnFileName = "ICN-ZB1-A-05-00-00-00A-007A-A-001-01.CGM";

        ImportFileItemVO icnFile = new ImportFileItemVO();
        icnFile.setFileName(icnFileName);
        icnFile.setTempFilePath(null);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("username", "testUser");

        // 执行
        java.lang.reflect.Method method = importService.getClass()
                .getDeclaredMethod("importSingleIcn", ImportFileItemVO.class, String.class, javax.servlet.http.HttpServletRequest.class);
        method.setAccessible(true);

        try {
            method.invoke(importService, icnFile, testProjectId, request);

            // 验证
            QueryWrapper<IetmIcnManage> qw = new QueryWrapper<>();
            qw.eq("icn", "ICN-ZB1-A-05-00-00-00A-007A-A-001-01");
            IetmIcnManage icn = icnManageService.getOne(qw);

            assertNotNull(icn, "ICN记录应该被保存");

            // ✅ 验证必需字段
            assertNotNull(icn.getOriginator(), "originator应该被设置");
            assertEquals("00", icn.getOriginator(), "originator应该是parts[4]");

            assertNotNull(icn.getUniqueId(), "uniqueId应该被设置");
            assertEquals("00A", icn.getUniqueId(), "uniqueId应该是parts[5]");

            assertNotNull(icn.getSecurity(), "security应该被设置");
            assertEquals(1, icn.getSecurity(), "security应该是parts[8]=01解析为1");

            // 验证可选字段
            assertEquals("05", icn.getRpc(), "rpc应该是parts[3]");
            assertEquals("007A", icn.getVariantCode(), "variantCode应该是parts[6]");
            assertEquals("A", icn.getIssueNo(), "issueNo应该是parts[7]");

            log.info("✅ 测试通过：必需字段解析成功，originator={}, uniqueId={}, security={}",
                    icn.getOriginator(), icn.getUniqueId(), icn.getSecurity());

        } catch (Exception e) {
            fail("导入ICN失败：" + e.getMessage());
        }
    }

    /**
     * 测试用例3：验证SNS不存在时拒绝导入
     */
    @Test
    public void testRejectWhenSnsNotExist() throws Exception {
        // 准备：ICN文件名包含不存在的SNS = ZB2-B（构型树中只有ZB1-A）
        String icnFileName = "ICN-ZB2-B-05-00-00-00A-007A-A-001-01.CGM";

        ImportFileItemVO icnFile = new ImportFileItemVO();
        icnFile.setFileName(icnFileName);
        icnFile.setTempFilePath(null);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("username", "testUser");

        // 执行
        java.lang.reflect.Method method = importService.getClass()
                .getDeclaredMethod("importSingleIcn", ImportFileItemVO.class, String.class, javax.servlet.http.HttpServletRequest.class);
        method.setAccessible(true);

        // 验证：应该抛出异常
        Exception exception = assertThrows(Exception.class, () -> {
            method.invoke(importService, icnFile, testProjectId, request);
        });

        String errorMessage = exception.getCause().getMessage();
        assertTrue(errorMessage.contains("SNS") && errorMessage.contains("不存在"),
                "错误消息应该提示SNS不存在：" + errorMessage);

        log.info("✅ 测试通过：SNS不存在时正确拒绝导入，错误消息={}", errorMessage);
    }

    /**
     * 测试用例4：验证SNS映射表构建
     */
    @Test
    public void testBuildSnsToNodeIdMap() throws Exception {
        // 执行：调用私有方法构建SNS映射表
        java.lang.reflect.Method method = importService.getClass()
                .getDeclaredMethod("buildSnsToNodeIdMap", String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        java.util.Map<String, String> snsMap = (java.util.Map<String, String>) method.invoke(importService, testProjectId);

        // 验证：映射表包含正确的SNS路径
        assertNotNull(snsMap, "SNS映射表不应该为空");
        assertTrue(snsMap.size() >= 3, "应该有至少3个节点：ZB1, ZB1-A, ZB1-A-05");

        assertTrue(snsMap.containsKey("ZB1"), "应该包含根节点SNS=ZB1");
        assertTrue(snsMap.containsKey("ZB1-A"), "应该包含子节点SNS=ZB1-A");
        assertTrue(snsMap.containsKey("ZB1-A-05"), "应该包含孙节点SNS=ZB1-A-05");

        log.info("✅ 测试通过：SNS映射表构建成功，映射关系：");
        snsMap.forEach((sns, nodeId) -> log.info("  {} -> {}", sns, nodeId));
    }

    /**
     * 测试用例5：验证完整SNS路径构建（递归算法）
     */
    @Test
    public void testBuildFullSnsPath() throws Exception {
        // 查询孙节点（code=05）
        QueryWrapper<IetmProjectConfigurationManagement> qw = new QueryWrapper<>();
        qw.eq("project_id", testProjectId);
        qw.eq("code", "05");
        IetmProjectConfigurationManagement grandChildNode = configurationService.getOne(qw);
        assertNotNull(grandChildNode, "孙节点应该存在");

        // 查询所有节点
        QueryWrapper<IetmProjectConfigurationManagement> allQw = new QueryWrapper<>();
        allQw.eq("project_id", testProjectId);
        List<IetmProjectConfigurationManagement> allNodes = configurationService.list(allQw);

        // 执行：调用私有方法构建完整SNS路径
        java.lang.reflect.Method method = importService.getClass()
                .getDeclaredMethod("buildFullSnsPath",
                        IetmProjectConfigurationManagement.class,
                        List.class);
        method.setAccessible(true);

        String fullSnsPath = (String) method.invoke(importService, grandChildNode, allNodes);

        // 验证：完整路径应该是 ZB1-A-05
        assertNotNull(fullSnsPath, "完整SNS路径不应该为空");
        assertEquals("ZB1-A-05", fullSnsPath, "完整SNS路径应该是 ZB1-A-05");

        log.info("✅ 测试通过：完整SNS路径构建成功，节点code=05 -> 完整SNS={}", fullSnsPath);
    }
}
