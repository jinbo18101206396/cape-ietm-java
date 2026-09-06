package org.jeecg.modules.ietm.ietmddn;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDataModule;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.mapper.IetmDataModuleMapper;
import org.jeecg.modules.ietm.icnmanage.entity.IetmIcnManage;
import org.jeecg.modules.ietm.icnmanage.entity.IetmIcnReference;
import org.jeecg.modules.ietm.icnmanage.mapper.IetmIcnManageMapper;
import org.jeecg.modules.ietm.icnmanage.mapper.IetmIcnReferenceMapper;
import org.jeecg.modules.ietm.ietmddn.util.DdnPackageBuilder;
import org.jeecg.modules.ietm.ietmddn.vo.DdnGenerateVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DDN "引用ICN" 选项功能测试
 *
 * 测试场景：验证勾选/不勾选"引用ICN"时，生成的数据包内容是否符合预期
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
public class DdnIncludeRefIcnTest {

    @Autowired
    private DdnPackageBuilder ddnPackageBuilder;

    @Autowired
    private IetmDataModuleMapper dataModuleMapper;

    @Autowired
    private IetmIcnManageMapper icnManageMapper;

    @Autowired
    private IetmIcnReferenceMapper icnReferenceMapper;

    /**
     * TC-01: 勾选"引用ICN" - 应导出DM引用的ICN
     *
     * 场景：
     * - 创建DM-A、DM-B
     * - 创建ICN-X、ICN-Y
     * - DM-A引用ICN-X，DM-B引用ICN-Y
     * - 用户选择DM-A（不选DM-B）
     * - 勾选"引用ICN"
     *
     * 预期：
     * - 压缩包中包含DM-A
     * - 压缩包中包含ICN-X（DM-A引用的）
     * - 不包含ICN-Y（DM-B引用的，但DM-B未选择）
     */
    @Test
    public void testIncludeRefIcn_True_ShouldExportReferencedIcns() throws Exception {
        // 1. 创建测试DM
        IetmDataModule dmA = createTestDm("DMC-TEST-ICN-A-001", "<dmodule>DM A Content</dmodule>");
        IetmDataModule dmB = createTestDm("DMC-TEST-ICN-B-001", "<dmodule>DM B Content</dmodule>");

        // 2. 创建测试ICN
        IetmIcnManage icnX = createTestIcn("ICN-X-001", "test_image_x.png");
        IetmIcnManage icnY = createTestIcn("ICN-Y-001", "test_image_y.png");

        // 3. 创建引用关系：DM-A引用ICN-X，DM-B引用ICN-Y
        createIcnReference(icnX.getId(), dmA.getId());
        createIcnReference(icnY.getId(), dmB.getId());

        // 4. 构建DDN参数：只选择DM-A，勾选"引用ICN"
        DdnGenerateVO params = new DdnGenerateVO();
        params.setDmIds(Arrays.asList(dmA.getId()));  // 只选择DM-A
        params.setIncludeRefDm(false);
        params.setIncludeRefIcn(true);  // 勾选"引用ICN"
        params.setIncludeDmResource(false);
        params.setModelic("TEST");
        params.setSender("SENDER");
        params.setReceiver("RECEIVER");
        params.setIssueDate("2026-09-02");
        params.setSecurity("1");

        Map<String, Object> projectInfo = new HashMap<>();
        projectInfo.put("projectId", "test-project-icn-001");

        // 5. 生成DDN数据包
        String ddnCode = "DDN-TEST-SENDER-RECEIVER-2026-10001";
        DdnPackageBuilder.BuildResult result = ddnPackageBuilder.buildDdnPackage(ddnCode, params, projectInfo);

        // 6. 验证结果
        log.info("=== TC-01: 勾选'引用ICN' 测试结果 ===");
        log.info("选择的DM: DM-A");
        log.info("DM-A引用ICN-X, DM-B引用ICN-Y");
        log.info("includeRefIcn = true");
        log.info("实际导出DM数量: {}", result.getTotalDmCount());
        log.info("实际导出ICN数量: {}", result.getTotalIcnCount());
        log.info("导出的ICN ID列表: {}", result.getAllIcnIds());

        // 预期：DM-A导出，ICN-X导出
        assertEquals(1, result.getTotalDmCount(), "应导出1个DM（DM-A）");
        assertEquals(1, result.getTotalIcnCount(), "应导出1个ICN（ICN-X）");
        assertTrue(result.getAllDmIds().contains(dmA.getId()), "应包含DM-A");
        assertTrue(result.getAllIcnIds().contains(icnX.getId()), "应包含DM-A引用的ICN-X");
        assertFalse(result.getAllIcnIds().contains(icnY.getId()), "不应包含ICN-Y（DM-B未选择）");

        log.info("✅ TC-01 通过：勾选'引用ICN'时，正确导出DM引用的ICN");
    }

    /**
     * TC-02: 不勾选"引用ICN" - 不应导出任何ICN
     *
     * 场景：
     * - 创建DM-C
     * - 创建ICN-Z
     * - DM-C引用ICN-Z
     * - 用户选择DM-C
     * - 不勾选"引用ICN"
     *
     * 预期：
     * - 压缩包中包含DM-C
     * - 压缩包中不包含任何ICN
     */
    @Test
    public void testIncludeRefIcn_False_ShouldNotExportIcns() throws Exception {
        // 1. 创建测试DM和ICN
        IetmDataModule dmC = createTestDm("DMC-TEST-ICN-C-001", "<dmodule>DM C Content</dmodule>");
        IetmIcnManage icnZ = createTestIcn("ICN-Z-001", "test_image_z.png");

        // 2. 创建引用关系
        createIcnReference(icnZ.getId(), dmC.getId());

        // 3. 构建DDN参数：不勾选"引用ICN"
        DdnGenerateVO params = new DdnGenerateVO();
        params.setDmIds(Arrays.asList(dmC.getId()));
        params.setIncludeRefDm(false);
        params.setIncludeRefIcn(false);  // 不勾选"引用ICN"
        params.setIncludeDmResource(false);
        params.setModelic("TEST");
        params.setSender("SENDER");
        params.setReceiver("RECEIVER");
        params.setIssueDate("2026-09-02");
        params.setSecurity("1");

        Map<String, Object> projectInfo = new HashMap<>();
        projectInfo.put("projectId", "test-project-icn-002");

        // 4. 生成DDN数据包
        String ddnCode = "DDN-TEST-SENDER-RECEIVER-2026-10002";
        DdnPackageBuilder.BuildResult result = ddnPackageBuilder.buildDdnPackage(ddnCode, params, projectInfo);

        // 5. 验证结果
        log.info("=== TC-02: 不勾选'引用ICN' 测试结果 ===");
        log.info("选择的DM: DM-C");
        log.info("DM-C引用ICN-Z");
        log.info("includeRefIcn = false");
        log.info("实际导出DM数量: {}", result.getTotalDmCount());
        log.info("实际导出ICN数量: {}", result.getTotalIcnCount());

        // 预期：DM-C导出，但ICN-Z不导出
        assertEquals(1, result.getTotalDmCount(), "应导出1个DM（DM-C）");
        assertEquals(0, result.getTotalIcnCount(), "不应导出任何ICN");
        assertTrue(result.getAllDmIds().contains(dmC.getId()), "应包含DM-C");
        assertFalse(result.getAllIcnIds().contains(icnZ.getId()), "不应包含ICN-Z");

        log.info("✅ TC-02 通过：不勾选'引用ICN'时，不导出任何ICN");
    }

    /**
     * TC-03: 多个DM引用同一ICN - 应去重，只导出一次
     */
    @Test
    public void testIncludeRefIcn_MultipleDmsReferenceSameIcn_ShouldDeduplicate() throws Exception {
        // 1. 创建测试数据
        IetmDataModule dmD = createTestDm("DMC-TEST-ICN-D-001", "<dmodule>DM D</dmodule>");
        IetmDataModule dmE = createTestDm("DMC-TEST-ICN-E-001", "<dmodule>DM E</dmodule>");
        IetmIcnManage icnW = createTestIcn("ICN-W-001", "shared_image.png");

        // 2. 两个DM都引用同一个ICN
        createIcnReference(icnW.getId(), dmD.getId());
        createIcnReference(icnW.getId(), dmE.getId());

        // 3. 构建DDN参数
        DdnGenerateVO params = new DdnGenerateVO();
        params.setDmIds(Arrays.asList(dmD.getId(), dmE.getId()));
        params.setIncludeRefDm(false);
        params.setIncludeRefIcn(true);
        params.setIncludeDmResource(false);
        params.setModelic("TEST");
        params.setSender("SENDER");
        params.setReceiver("RECEIVER");
        params.setIssueDate("2026-09-02");
        params.setSecurity("1");

        Map<String, Object> projectInfo = new HashMap<>();
        projectInfo.put("projectId", "test-project-icn-003");

        // 4. 生成DDN数据包
        String ddnCode = "DDN-TEST-SENDER-RECEIVER-2026-10003";
        DdnPackageBuilder.BuildResult result = ddnPackageBuilder.buildDdnPackage(ddnCode, params, projectInfo);

        // 5. 验证结果
        log.info("=== TC-03: 去重测试结果 ===");
        log.info("选择的DM: DM-D, DM-E");
        log.info("两个DM都引用ICN-W");
        log.info("实际导出ICN数量: {}", result.getTotalIcnCount());

        // 预期：ICN-W只导出一次（自动去重）
        assertEquals(2, result.getTotalDmCount(), "应导出2个DM");
        assertEquals(1, result.getTotalIcnCount(), "应导出1个ICN（去重后）");
        assertTrue(result.getAllIcnIds().contains(icnW.getId()), "应包含共享的ICN-W");

        log.info("✅ TC-03 通过：多个DM引用同一ICN时，正确去重");
    }

    // ========== 辅助方法 ==========

    private IetmDataModule createTestDm(String dmcCode, String content) {
        IetmDataModule dm = new IetmDataModule();
        dm.setId(UUID.randomUUID().toString().replace("-", ""));
        dm.setDmcCode(dmcCode);
        dm.setDmContent(content);
        dm.setDmType("procedural");
        dm.setIssueNo("001");
        dm.setInWork("00");
        dm.setLanguage("zh-CN");
        dm.setSecurityClassification("01");
        dm.setCreateBy("test");
        dm.setCreateTime(new Date());
        dataModuleMapper.insert(dm);
        return dm;
    }

    private IetmIcnManage createTestIcn(String icnCode, String fileName) {
        IetmIcnManage icn = new IetmIcnManage();
        icn.setId(UUID.randomUUID().toString().replace("-", ""));
        icn.setSns(icnCode);
        icn.setRpcName(fileName);
        icn.setCreateBy("test");
        icn.setCreateTime(new Date());
        icnManageMapper.insert(icn);
        return icn;
    }

    private void createIcnReference(String icnId, String dmId) {
        IetmIcnReference ref = new IetmIcnReference();
        ref.setId(UUID.randomUUID().toString().replace("-", ""));
        ref.setSourceIcnId(icnId);  // ICN的ID
        ref.setDmCode(dmId);  // 注意：这里存储的是DM的ID（不是DMC编码）
        ref.setReferenceType("ICN_TO_DM");  // ICN被DM引用
        ref.setCreateBy("test");
        ref.setCreateTime(new Date());
        icnReferenceMapper.insert(ref);
    }
}
