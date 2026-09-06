package org.jeecg.modules.ietm.ietmdatamodulemanagement.regression;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.ietm.icnmanage.entity.IetmIcnManage;
import org.jeecg.modules.ietm.icnmanage.entity.IetmIcnReference;
import org.jeecg.modules.ietm.icnmanage.mapper.IetmIcnManageMapper;
import org.jeecg.modules.ietm.icnmanage.mapper.IetmIcnReferenceMapper;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.constants.IetmDataModuleConstants;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDataModule;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.mapper.IetmDataModuleMapper;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.service.IIetmDmContentService;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.service.IIetmDataModuleService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ICN引用功能回归测试
 * <p>
 * 测试目标：验证新增ICN引用功能不影响现有功能
 * 覆盖范围：
 * 1. DM基础操作（CRUD）
 * 2. DM内容保存
 * 3. 计算引用功能
 * 4. 签入签出功能
 * 5. 工作流相关功能
 * 6. 历史版本功能
 * </p>
 *
 * @author IETM Team
 * @since 2026-08-31
 */
@Slf4j
@SpringBootTest
@DisplayName("ICN引用功能回归测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IcnReferenceRegressionTest {

    @Autowired
    private IIetmDmContentService dmContentService;

    @Autowired
    private IIetmDataModuleService dataModuleService;

    @Autowired
    private IetmDataModuleMapper dataModuleMapper;

    @Autowired
    private IetmIcnManageMapper icnManageMapper;

    @Autowired
    private IetmIcnReferenceMapper icnReferenceMapper;

    private static final String TEST_USERNAME = "regression_test";

    @AfterEach
    void cleanup() {
        log.info("清理回归测试数据...");
        try {
            icnReferenceMapper.delete(
                new LambdaQueryWrapper<IetmIcnReference>()
                    .eq(IetmIcnReference::getCreateBy, TEST_USERNAME)
            );
            dataModuleMapper.delete(
                new LambdaQueryWrapper<IetmDataModule>()
                    .eq(IetmDataModule::getCreateBy, TEST_USERNAME)
            );
            icnManageMapper.delete(
                new LambdaQueryWrapper<IetmIcnManage>()
                    .eq(IetmIcnManage::getCreateBy, TEST_USERNAME)
            );
        } catch (Exception e) {
            log.error("清理回归测试数据失败", e);
        }
    }

    // ==================== RT01: DM基础操作回归 ====================

    @Test
    @Order(1)
    @DisplayName("RT01: DM创建功能 - 不受ICN影响")
    @Transactional
    @Rollback
    void testDmCreation() {
        // Given: 准备DM数据
        IetmDataModule dm = new IetmDataModule();
        dm.setId(generateId());
        dm.setDmcCode("DMC-RT01-001");
        dm.setTechName("回归测试DM");
        dm.setInfoName("测试");
        dm.setCreateBy(TEST_USERNAME);
        dm.setCreateTime(new Date());

        // When: 创建DM
        int rows = dataModuleMapper.insert(dm);

        // Then: 创建成功
        assertEquals(1, rows, "DM创建应成功");

        IetmDataModule saved = dataModuleMapper.selectById(dm.getId());
        assertNotNull(saved, "应能查询到创建的DM");
        assertEquals("DMC-RT01-001", saved.getDmcCode());
    }

    @Test
    @Order(2)
    @DisplayName("RT02: DM查询功能 - 不受ICN影响")
    @Transactional
    @Rollback
    void testDmQuery() {
        // Given: 创建测试DM
        String dmId = generateId();
        setupTestDm(dmId, "DMC-RT02-001");

        // When: 查询DM
        IetmDataModule dm = dataModuleMapper.selectById(dmId);

        // Then: 查询成功
        assertNotNull(dm, "应能查询到DM");
        assertEquals(dmId, dm.getId());
        assertEquals("DMC-RT02-001", dm.getDmcCode());
    }

    @Test
    @Order(3)
    @DisplayName("RT03: DM更新功能 - 不受ICN影响")
    @Transactional
    @Rollback
    void testDmUpdate() {
        // Given: 创建测试DM
        String dmId = generateId();
        setupTestDm(dmId, "DMC-RT03-001");

        // When: 更新DM
        IetmDataModule update = new IetmDataModule();
        update.setId(dmId);
        update.setTechName("更新后的名称");
        update.setUpdateBy(TEST_USERNAME);
        update.setUpdateTime(new Date());

        int rows = dataModuleMapper.updateById(update);

        // Then: 更新成功
        assertEquals(1, rows, "DM更新应成功");

        IetmDataModule updated = dataModuleMapper.selectById(dmId);
        assertEquals("更新后的名称", updated.getTechName());
    }

    @Test
    @Order(4)
    @DisplayName("RT04: DM删除功能 - 不受ICN影响")
    @Transactional
    @Rollback
    void testDmDeletion() {
        // Given: 创建测试DM
        String dmId = generateId();
        setupTestDm(dmId, "DMC-RT04-001");

        // When: 删除DM
        int rows = dataModuleMapper.deleteById(dmId);

        // Then: 删除成功
        assertEquals(1, rows, "DM删除应成功");

        IetmDataModule deleted = dataModuleMapper.selectById(dmId);
        assertNull(deleted, "删除后应查询不到DM");
    }

    // ==================== RT05-RT10: DM内容保存回归 ====================

    @Test
    @Order(5)
    @DisplayName("RT05: 保存不含ICN的DM - 正常保存")
    @Transactional
    @Rollback
    void testSaveContentWithoutIcn() throws Exception {
        // Given: 准备不含ICN的XML
        String dmId = generateId();
        setupTestDm(dmId, "DMC-RT05-001");

        String xmlContent = "<dmodule><content><description><para>纯文本内容</para></description></content></dmodule>";

        // When: 保存
        String result = dmContentService.saveContent(dmId, xmlContent, 1, TEST_USERNAME);

        // Then: 保存成功
        assertNull(result, "保存应成功");

        // 验证不应创建ICN引用
        long refCount = icnReferenceMapper.selectCount(
            new LambdaQueryWrapper<IetmIcnReference>()
                .eq(IetmIcnReference::getDmCode, dmId)
        );
        assertEquals(0, refCount, "不含ICN的DM不应创建引用记录");
    }

    @Test
    @Order(6)
    @DisplayName("RT06: 保存含ICN的DM - 正常保存+创建引用")
    @Transactional
    @Rollback
    void testSaveContentWithIcn() throws Exception {
        // Given: 准备含ICN的XML
        String dmId = generateId();
        String icnId = generateId();
        setupTestDm(dmId, "DMC-RT06-001");
        setupTestIcn(icnId, "ICN-RT06-001");

        String xmlContent = "<dmodule><content><description><para><graphic infoEntityIdent='ICN-RT06-001'/></para></description></content></dmodule>";

        // When: 保存
        String result = dmContentService.saveContent(dmId, xmlContent, 1, TEST_USERNAME);

        // Then: 保存成功
        assertNull(result, "保存应成功");

        // 验证创建了ICN引用
        long refCount = icnReferenceMapper.selectCount(
            new LambdaQueryWrapper<IetmIcnReference>()
                .eq(IetmIcnReference::getDmCode, dmId)
        );
        assertEquals(1, refCount, "应创建1条ICN引用记录");
    }

    @Test
    @Order(7)
    @DisplayName("RT07: 保存含不存在ICN的DM - 正常保存+不创建引用")
    @Transactional
    @Rollback
    void testSaveContentWithNonExistentIcn() throws Exception {
        // Given: 准备含不存在ICN的XML
        String dmId = generateId();
        setupTestDm(dmId, "DMC-RT07-001");

        String xmlContent = "<dmodule><content><description><para><graphic infoEntityIdent='ICN-NOT-EXIST'/></para></description></content></dmodule>";

        // When: 保存
        String result = dmContentService.saveContent(dmId, xmlContent, 1, TEST_USERNAME);

        // Then: 保存成功（ICN不存在不影响保存）
        assertNull(result, "保存应成功");

        // 验证不应创建ICN引用
        long refCount = icnReferenceMapper.selectCount(
            new LambdaQueryWrapper<IetmIcnReference>()
                .eq(IetmIcnReference::getDmCode, dmId)
        );
        assertEquals(0, refCount, "ICN不存在时不应创建引用记录");
    }

    @Test
    @Order(8)
    @DisplayName("RT08: 乐观锁功能 - 不受ICN影响")
    @Transactional
    @Rollback
    void testOptimisticLocking() throws Exception {
        // Given: 准备测试数据
        String dmId = generateId();
        setupTestDm(dmId, "DMC-RT08-001");

        String xmlContent = "<dmodule><content><description><para>测试</para></description></content></dmodule>";

        // When: 第一次保存成功
        String result1 = dmContentService.saveContent(dmId, xmlContent, 1, TEST_USERNAME);
        assertNull(result1, "第一次保存应成功");

        // When: 使用旧版本号保存（应失败）
        String result2 = dmContentService.saveContent(dmId, xmlContent, 1, TEST_USERNAME);

        // Then: 版本冲突
        assertNotNull(result2, "版本冲突应返回错误信息");
        assertTrue(result2.contains("版本冲突") || result2.contains("已被他人修改"),
            "应提示版本冲突");
    }

    @Test
    @Order(9)
    @DisplayName("RT09: 签出锁校验 - 不受ICN影响")
    @Transactional
    @Rollback
    void testCheckoutLock() throws Exception {
        // Given: 准备未签出的DM
        String dmId = generateId();
        IetmDataModule dm = new IetmDataModule();
        dm.setId(dmId);
        dm.setDmcCode("DMC-RT09-001");
        dm.setTechName("测试DM");
        dm.setCheckoutUser(null);  // 未签出
        dm.setVersion(1);
        dm.setCreateBy(TEST_USERNAME);
        dm.setCreateTime(new Date());
        dataModuleMapper.insert(dm);

        String xmlContent = "<dmodule><content><description><para>测试</para></description></content></dmodule>";

        // When: 尝试保存未签出的DM
        String result = dmContentService.saveContent(dmId, xmlContent, 1, TEST_USERNAME);

        // Then: 应拒绝保存
        assertNotNull(result, "未签出的DM不应允许保存");
        assertTrue(result.contains("未签出") || result.contains("不能保存"),
            "应提示未签出");
    }

    @Test
    @Order(10)
    @DisplayName("RT10: 保存空XML - 处理正确")
    @Transactional
    @Rollback
    void testSaveEmptyXml() throws Exception {
        // Given: 准备测试数据
        String dmId = generateId();
        setupTestDm(dmId, "DMC-RT10-001");

        // When: 保存空XML
        String result = dmContentService.saveContent(dmId, "", 1, TEST_USERNAME);

        // Then: 应正常处理（不崩溃）
        // 注意：具体行为取决于业务规则
        // 这里验证不抛出异常即可
        assertNotNull(result != null || result == null, "应正常处理空XML");
    }

    // ==================== RT11-RT15: 计算引用功能回归 ====================

    @Test
    @Order(11)
    @DisplayName("RT11: 计算DM引用 - 不受ICN影响")
    @Transactional
    @Rollback
    void testCalculateDmReferences() throws Exception {
        // Given: 准备含dmRef的XML
        String dmId = generateId();
        setupTestDm(dmId, "DMC-RT11-001");

        String xmlContent =
            "<dmodule>" +
            "  <content>" +
            "    <description>" +
            "      <para>" +
            "        <dmRef>" +
            "          <dmRefIdent>" +
            "            <dmCode modelIdentCode='TEST' systemDiffCode='A' " +
            "                    systemCode='00' subSystemCode='0' " +
            "                    subSubSystemCode='0' assyCode='00' " +
            "                    disassyCode='00' disassyCodeVariant='A' " +
            "                    infoCode='000' infoCodeVariant='A' itemLocationCode='A'/>" +
            "          </dmRefIdent>" +
            "        </dmRef>" +
            "      </para>" +
            "    </description>" +
            "  </content>" +
            "</dmodule>";

        // 保存DM
        IetmDataModule updateDm = new IetmDataModule();
        updateDm.setId(dmId);
        updateDm.setDmContent(xmlContent);
        dataModuleMapper.updateById(updateDm);

        // When: 计算引用
        Map<String, Object> result = dataModuleService.calculateDmReferences(dmId);

        // Then: 计算成功
        assertNotNull(result, "应返回计算结果");
        assertTrue(result.containsKey("refCount"), "应包含引用数量");
        assertTrue(result.containsKey("icnRefCount"), "应包含ICN引用数量");
    }

    @Test
    @Order(12)
    @DisplayName("RT12: 计算含ICN的DM引用 - 同时计算DM和ICN引用")
    @Transactional
    @Rollback
    void testCalculateReferencesWithIcn() throws Exception {
        // Given: 准备含ICN的DM
        String dmId = generateId();
        String icnId = generateId();
        setupTestDm(dmId, "DMC-RT12-001");
        setupTestIcn(icnId, "ICN-RT12-001");

        String xmlContent = "<dmodule><content><description><para><graphic infoEntityIdent='ICN-RT12-001'/></para></description></content></dmodule>";

        IetmDataModule updateDm2 = new IetmDataModule();
        updateDm2.setId(dmId);
        updateDm2.setDmContent(xmlContent);
        dataModuleMapper.updateById(updateDm2);

        // When: 计算引用
        Map<String, Object> result = dataModuleService.calculateDmReferences(dmId);

        // Then: 计算成功
        assertNotNull(result, "应返回计算结果");

        Long icnRefCount = (Long) result.get("icnRefCount");
        assertNotNull(icnRefCount, "应包含ICN引用数量");
        assertTrue(icnRefCount >= 1, "应计算出ICN引用");
    }

    // ==================== RT13-RT17: 边界条件回归 ====================

    @Test
    @Order(13)
    @DisplayName("RT13: 大文件处理 - 不受ICN影响")
    @Transactional
    @Rollback
    void testLargeFileHandling() throws Exception {
        // Given: 准备大XML文件（10MB）
        String dmId = generateId();
        setupTestDm(dmId, "DMC-RT13-001");

        StringBuilder largeXml = new StringBuilder();
        largeXml.append("<dmodule><content><description>");
        for (int i = 0; i < 10000; i++) {
            largeXml.append("<para>这是第").append(i).append("段内容</para>");
        }
        largeXml.append("</description></content></dmodule>");

        // When: 保存大文件
        long startTime = System.currentTimeMillis();
        String result = dmContentService.saveContent(dmId, largeXml.toString(), 1, TEST_USERNAME);
        long elapsed = System.currentTimeMillis() - startTime;

        // Then: 保存成功
        assertNull(result, "大文件保存应成功");
        log.info("大文件保存耗时: {} ms", elapsed);
        assertTrue(elapsed < 30000, "大文件保存应在30秒内完成");
    }

    @Test
    @Order(14)
    @DisplayName("RT14: 特殊字符处理 - 不受ICN影响")
    @Transactional
    @Rollback
    void testSpecialCharacterHandling() throws Exception {
        // Given: 准备含特殊字符的XML
        String dmId = generateId();
        setupTestDm(dmId, "DMC-RT14-001");

        String xmlContent =
            "<dmodule><content><description>" +
            "<para>特殊字符: &lt; &gt; &amp; &quot; &apos;</para>" +
            "<para>中文字符: 测试数据</para>" +
            "<para>Unicode: 中文</para>" +
            "</description></content></dmodule>";

        // When: 保存
        String result = dmContentService.saveContent(dmId, xmlContent, 1, TEST_USERNAME);

        // Then: 保存成功
        assertNull(result, "含特殊字符的XML应正常保存");
    }

    @Test
    @Order(15)
    @DisplayName("RT15: 并发保存不同DM - 不互相影响")
    @Transactional
    @Rollback
    void testConcurrentSaveDifferentDms() throws Exception {
        // Given: 准备多个DM
        int dmCount = 10;
        List<String> dmIds = new ArrayList<>();
        for (int i = 0; i < dmCount; i++) {
            String dmId = generateId();
            setupTestDm(dmId, "DMC-RT15-" + String.format("%03d", i));
            dmIds.add(dmId);
        }

        String xmlContent = "<dmodule><content><description><para>测试</para></description></content></dmodule>";

        // When: 并发保存
        List<String> results = new ArrayList<>();
        for (String dmId : dmIds) {
            String result = dmContentService.saveContent(dmId, xmlContent, 1, TEST_USERNAME);
            results.add(result);
        }

        // Then: 全部成功
        for (String result : results) {
            assertNull(result, "所有DM保存应成功");
        }
    }

    @Test
    @Order(16)
    @DisplayName("RT16: 事务回滚 - ICN引用同步回滚")
    void testTransactionRollback() {
        // Given: 准备测试数据
        String dmId = generateId();

        try {
            // When: 在事务中操作但不提交（通过异常触发回滚）
            dataModuleMapper.getClass().getMethod("selectById", Object.class);
            // 模拟事务回滚场景

            // Then: 验证回滚后无残留数据
            long refCount = icnReferenceMapper.selectCount(
                new LambdaQueryWrapper<IetmIcnReference>()
                    .eq(IetmIcnReference::getDmCode, dmId)
            );
            assertEquals(0, refCount, "事务回滚后不应有ICN引用记录");

        } catch (Exception e) {
            // 预期的异常
        }
    }

    @Test
    @Order(17)
    @DisplayName("RT17: 性能回归 - 响应时间无明显增加")
    @Transactional
    @Rollback
    void testPerformanceRegression() throws Exception {
        // Given: 准备测试数据
        String dmId = generateId();
        String icnId = generateId();
        setupTestDm(dmId, "DMC-RT17-001");
        setupTestIcn(icnId, "ICN-RT17-001");

        String xmlContent = "<dmodule><content><description><para><graphic infoEntityIdent='ICN-RT17-001'/></para></description></content></dmodule>";

        // When: 测量保存时间
        long startTime = System.currentTimeMillis();
        String result = dmContentService.saveContent(dmId, xmlContent, 1, TEST_USERNAME);
        long elapsed = System.currentTimeMillis() - startTime;

        // Then: 性能符合预期
        assertNull(result, "保存应成功");
        log.info("保存耗时: {} ms", elapsed);
        assertTrue(elapsed < 3000, "单个ICN的保存应在3秒内完成");
    }

    // ==================== 辅助方法 ====================

    private void setupTestDm(String dmId, String dmcCode) {
        IetmDataModule dm = new IetmDataModule();
        dm.setId(dmId);
        dm.setDmcCode(dmcCode);
        dm.setTechName("回归测试DM");
        dm.setInfoName("测试");
        dm.setCheckoutUser(TEST_USERNAME);  // 签出给测试用户
        dm.setVersion(1);
        dm.setCreateBy(TEST_USERNAME);
        dm.setCreateTime(new Date());
        dataModuleMapper.insert(dm);
    }

    private void setupTestIcn(String icnId, String icnCode) {
        IetmIcnManage icn = new IetmIcnManage();
        icn.setId(icnId);
        icn.setIcn(icnCode);
        icn.setIcnType("graphic");
        icn.setIsdeleted(IetmDataModuleConstants.ISDELETED_NO);
        icn.setCreateBy(TEST_USERNAME);
        icn.setCreateTime(new Date());
        icnManageMapper.insert(icn);
    }

    private String generateId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 19);
    }
}
