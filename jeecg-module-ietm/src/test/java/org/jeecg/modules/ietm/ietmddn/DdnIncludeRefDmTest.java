package org.jeecg.modules.ietm.ietmddn;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDataModule;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.mapper.IetmDataModuleMapper;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDmRef;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.mapper.IetmDmRefMapper;
import org.jeecg.modules.ietm.ietmddn.util.DdnPackageBuilder;
import org.jeecg.modules.ietm.ietmddn.vo.DdnGenerateVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DDN "引用DM" 选项功能测试
 *
 * 测试场景：验证勾选/不勾选"引用DM"时，生成的数据包内容是否符合预期
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
    "spring.autoconfigure.exclude=org.jeecg.config.WebSocketConfig"
})
@Transactional
public class DdnIncludeRefDmTest {

    @Autowired
    private DdnPackageBuilder ddnPackageBuilder;

    @Autowired
    private IetmDataModuleMapper dataModuleMapper;

    @Autowired
    private IetmDmRefMapper dmRefMapper;

    /**
     * TC-01: 不勾选"引用DM" - 应只导出用户选择的DM，不导出引用的DM
     *
     * 场景：
     * - 用户添加了 DM-A, DM-B
     * - DM-A 引用了 DM-C
     * - 用户不勾选"引用DM"
     *
     * 预期：
     * - 压缩包中只包含 DM-A, DM-B
     * - 不包含 DM-C
     */
    @Test
    public void testIncludeRefDm_False_ShouldOnlyExportSelectedDms() throws Exception {
        // 1. 准备测试数据：创建3个DM
        IetmDataModule dmA = createTestDm("DMC-TEST-A-001", "DM A Content");
        IetmDataModule dmB = createTestDm("DMC-TEST-B-001", "DM B Content");
        IetmDataModule dmC = createTestDm("DMC-TEST-C-001", "DM C Content");

        // 2. 创建引用关系：DM-A 引用 DM-C
        IetmDmRef ref = new IetmDmRef();
        ref.setId(UUID.randomUUID().toString().replace("-", ""));
        ref.setSourceDmId(dmA.getId());
        ref.setTargetDmId(dmC.getId());
        ref.setRefType("dmRef");
        dmRefMapper.insert(ref);

        // 3. 构建DDN参数：用户选择 DM-A 和 DM-B，不勾选"引用DM"
        DdnGenerateVO params = new DdnGenerateVO();
        params.setDmIds(Arrays.asList(dmA.getId(), dmB.getId()));
        params.setIncludeRefDm(false);  // 不勾选"引用DM"
        params.setIncludeRefIcn(false);
        params.setIncludeDmResource(false);
        params.setModelic("TEST");
        params.setSender("SENDER");
        params.setReceiver("RECEIVER");
        params.setIssueDate("2026-09-02");
        params.setSecurity("1");

        Map<String, Object> projectInfo = new HashMap<>();
        projectInfo.put("projectId", "test-project-001");

        // 4. 生成DDN数据包
        String ddnCode = "DDN-TEST-SENDER-RECEIVER-2026-00001";
        DdnPackageBuilder.BuildResult result = ddnPackageBuilder.buildDdnPackage(ddnCode, params, projectInfo);

        // 5. 验证结果
        log.info("=== TC-01: 不勾选'引用DM' 测试结果 ===");
        log.info("初始DM数量: 2 (DM-A, DM-B)");
        log.info("DM-A引用DM-C");
        log.info("includeRefDm = false");
        log.info("实际导出DM数量: {}", result.getTotalDmCount());
        log.info("实际导出DM ID列表: {}", result.getAllDmIds());

        // 预期：只包含 DM-A 和 DM-B
        assertEquals(2, result.getTotalDmCount(),
            "不勾选'引用DM'时，应只导出用户选择的2个DM");
        assertTrue(result.getAllDmIds().contains(dmA.getId()),
            "应包含用户选择的 DM-A");
        assertTrue(result.getAllDmIds().contains(dmB.getId()),
            "应包含用户选择的 DM-B");
        assertFalse(result.getAllDmIds().contains(dmC.getId()),
            "不应包含引用的 DM-C");

        log.info("✅ TC-01 通过：不勾选'引用DM'时，只导出用户选择的DM");
    }

    /**
     * TC-02: 勾选"引用DM" - 应导出用户选择的DM + 递归引用的DM
     *
     * 场景：
     * - 用户添加了 DM-A, DM-B
     * - DM-A 引用了 DM-C
     * - 用户勾选"引用DM"
     *
     * 预期：
     * - 压缩包中包含 DM-A, DM-B, DM-C
     */
    @Test
    public void testIncludeRefDm_True_ShouldExportAllReferencedDms() throws Exception {
        // 1. 准备测试数据
        IetmDataModule dmA = createTestDm("DMC-TEST-A-002", "DM A Content");
        IetmDataModule dmB = createTestDm("DMC-TEST-B-002", "DM B Content");
        IetmDataModule dmC = createTestDm("DMC-TEST-C-002", "DM C Content");

        // 2. 创建引用关系：DM-A 引用 DM-C
        IetmDmRef ref = new IetmDmRef();
        ref.setId(UUID.randomUUID().toString().replace("-", ""));
        ref.setSourceDmId(dmA.getId());
        ref.setTargetDmId(dmC.getId());
        ref.setRefType("dmRef");
        dmRefMapper.insert(ref);

        // 3. 构建DDN参数：勾选"引用DM"
        DdnGenerateVO params = new DdnGenerateVO();
        params.setDmIds(Arrays.asList(dmA.getId(), dmB.getId()));
        params.setIncludeRefDm(true);  // 勾选"引用DM"
        params.setIncludeRefIcn(false);
        params.setIncludeDmResource(false);
        params.setModelic("TEST");
        params.setSender("SENDER");
        params.setReceiver("RECEIVER");
        params.setIssueDate("2026-09-02");
        params.setSecurity("1");

        Map<String, Object> projectInfo = new HashMap<>();
        projectInfo.put("projectId", "test-project-002");

        // 4. 生成DDN数据包
        String ddnCode = "DDN-TEST-SENDER-RECEIVER-2026-00002";
        DdnPackageBuilder.BuildResult result = ddnPackageBuilder.buildDdnPackage(ddnCode, params, projectInfo);

        // 5. 验证结果
        log.info("=== TC-02: 勾选'引用DM' 测试结果 ===");
        log.info("初始DM数量: 2 (DM-A, DM-B)");
        log.info("DM-A引用DM-C");
        log.info("includeRefDm = true");
        log.info("实际导出DM数量: {}", result.getTotalDmCount());
        log.info("实际导出DM ID列表: {}", result.getAllDmIds());

        // 预期：包含 DM-A, DM-B, DM-C
        assertEquals(3, result.getTotalDmCount(),
            "勾选'引用DM'时，应导出3个DM（含引用的DM）");
        assertTrue(result.getAllDmIds().contains(dmA.getId()),
            "应包含用户选择的 DM-A");
        assertTrue(result.getAllDmIds().contains(dmB.getId()),
            "应包含用户选择的 DM-B");
        assertTrue(result.getAllDmIds().contains(dmC.getId()),
            "应包含引用的 DM-C");

        log.info("✅ TC-02 通过：勾选'引用DM'时，导出所有引用的DM");
    }

    /**
     * TC-03: 多层引用 - A→B→C→D
     *
     * 场景：
     * - 用户添加了 DM-A
     * - DM-A 引用 DM-B
     * - DM-B 引用 DM-C
     * - DM-C 引用 DM-D
     * - 勾选"引用DM"
     *
     * 预期：
     * - 压缩包中包含 A, B, C, D（递归收集所有层级）
     */
    @Test
    public void testIncludeRefDm_MultipleLevels_ShouldExportAllLevels() throws Exception {
        // 1. 准备测试数据
        IetmDataModule dmA = createTestDm("DMC-TEST-A-003", "DM A Content");
        IetmDataModule dmB = createTestDm("DMC-TEST-B-003", "DM B Content");
        IetmDataModule dmC = createTestDm("DMC-TEST-C-003", "DM C Content");
        IetmDataModule dmD = createTestDm("DMC-TEST-D-003", "DM D Content");

        // 2. 创建引用链：A→B→C→D
        createDmRef(dmA.getId(), dmB.getId());
        createDmRef(dmB.getId(), dmC.getId());
        createDmRef(dmC.getId(), dmD.getId());

        // 3. 构建DDN参数
        DdnGenerateVO params = new DdnGenerateVO();
        params.setDmIds(Arrays.asList(dmA.getId()));
        params.setIncludeRefDm(true);
        params.setIncludeRefIcn(false);
        params.setIncludeDmResource(false);
        params.setModelic("TEST");
        params.setSender("SENDER");
        params.setReceiver("RECEIVER");
        params.setIssueDate("2026-09-02");
        params.setSecurity("1");

        Map<String, Object> projectInfo = new HashMap<>();
        projectInfo.put("projectId", "test-project-003");

        // 4. 生成DDN数据包
        String ddnCode = "DDN-TEST-SENDER-RECEIVER-2026-00003";
        DdnPackageBuilder.BuildResult result = ddnPackageBuilder.buildDdnPackage(ddnCode, params, projectInfo);

        // 5. 验证结果
        log.info("=== TC-03: 多层引用 A→B→C→D 测试结果 ===");
        log.info("初始DM数量: 1 (DM-A)");
        log.info("引用链: A→B→C→D");
        log.info("includeRefDm = true");
        log.info("实际导出DM数量: {}", result.getTotalDmCount());

        assertEquals(4, result.getTotalDmCount(),
            "多层引用时，应递归导出所有层级的DM");
        assertTrue(result.getAllDmIds().contains(dmA.getId()));
        assertTrue(result.getAllDmIds().contains(dmB.getId()));
        assertTrue(result.getAllDmIds().contains(dmC.getId()));
        assertTrue(result.getAllDmIds().contains(dmD.getId()));

        log.info("✅ TC-03 通过：多层引用正确递归收集");
    }

    // ========== 辅助方法 ==========

    private IetmDataModule createTestDm(String dmcCode, String content) {
        IetmDataModule dm = new IetmDataModule();
        dm.setId(UUID.randomUUID().toString().replace("-", ""));
        dm.setDmcCode(dmcCode);
        dm.setDmContent(content);
        dm.setTechName("Test DM");
        dm.setInfoName("Test Info");
        dataModuleMapper.insert(dm);
        return dm;
    }

    private void createDmRef(String sourceId, String targetId) {
        IetmDmRef ref = new IetmDmRef();
        ref.setId(UUID.randomUUID().toString().replace("-", ""));
        ref.setSourceDmId(sourceId);
        ref.setTargetDmId(targetId);
        ref.setRefType("dmRef");
        dmRefMapper.insert(ref);
    }
}
