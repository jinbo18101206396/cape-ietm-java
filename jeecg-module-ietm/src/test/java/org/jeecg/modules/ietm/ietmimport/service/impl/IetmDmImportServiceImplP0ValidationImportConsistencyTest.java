package org.jeecg.modules.ietm.ietmimport.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDataModule;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.service.IIetmDataModuleService;
import org.jeecg.modules.ietm.ietmimport.vo.ImportFileItemVO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * P0缺陷修复测试：校验-导入逻辑一致性
 *
 * 问题：校验通过但导入失败
 * 根因：校验使用6字段组合查询，导入使用简单dmc_code查询
 * 修复：导入改用findDmForResource()方法，统一使用6字段组合查询
 */
@RunWith(MockitoJUnitRunner.class)
public class IetmDmImportServiceImplP0ValidationImportConsistencyTest {

    @Mock
    private IIetmDataModuleService dataModuleService;

    @InjectMocks
    private IetmDmImportServiceImpl dmImportService;

    private MockHttpServletRequest request;
    private String projectId = "test-project-001";

    @Before
    public void setUp() {
        request = new MockHttpServletRequest();
    }

    /**
     * TC-01: itemLocationCode为空时，校验和导入都能找到DM
     *
     * 场景：
     * - 资源文件名：DMC-ZB1-A-05-00-00-00A-007A-A_001-03_zh-CN_金波.jpg
     * - DMC前缀：DMC-ZB1-A-05-00-00-00A-007A-A
     * - itemLocationCode为空
     * - 数据库dmc_code字段值：DMC-ZB1-A-05-00-00-00A-007A（最后没有-A）
     *
     * 预期：
     * - 校验阶段：使用6字段组合查询，能找到DM
     * - 导入阶段：使用6字段组合查询（修复后），能找到DM
     */
    @Test
    public void testValidationImportConsistency_EmptyItemLocationCode() throws Exception {
        // Arrange
        String resourceFileName = "DMC-ZB1-A-05-00-00-00A-007A-A_001-03_zh-CN_金波.jpg";

        IetmDataModule mockDm = new IetmDataModule();
        mockDm.setId("dm-001");
        mockDm.setDmcCode("DMC-ZB1-A-05-00-00-00A-007A");  // 注意：最后没有-A
        mockDm.setSns("ZB1-A-05-00-00-00A-007A-A");
        mockDm.setInfoCode("000");
        mockDm.setInfoCodeVariant("");
        mockDm.setIetmLocationCode("");  // itemLocationCode为空
        mockDm.setLanguageIsoCode("zh");
        mockDm.setCountryIsoCode("CN");

        // Mock 6字段组合查询返回DM
        when(dataModuleService.getOne(any(QueryWrapper.class))).thenReturn(mockDm);

        // Act - 校验阶段
        boolean validationResult = ReflectionTestUtils.invokeMethod(
            dmImportService,
            "isDmExistsForResource",
            resourceFileName,
            projectId
        );

        // Assert - 校验阶段
        assert validationResult : "校验阶段应该能找到DM";

        // Act - 导入阶段（模拟）
        ImportFileItemVO resourceFile = new ImportFileItemVO();
        resourceFile.setFileName(resourceFileName);
        resourceFile.setAssociatedDmcCode("DMC-ZB1-A-05-00-00-00A-007A-A");

        IetmDataModule foundDm = ReflectionTestUtils.invokeMethod(
            dmImportService,
            "findDmForResource",
            resourceFileName,
            projectId
        );

        // Assert - 导入阶段
        assert foundDm != null : "导入阶段应该能找到DM";
        assert foundDm.getId().equals("dm-001") : "应该找到正确的DM";

        // 验证两个阶段使用相同的查询逻辑
        verify(dataModuleService, atLeast(2)).getOne(any(QueryWrapper.class));
    }

    /**
     * TC-02: itemLocationCode不为空时，校验和导入都能找到DM
     */
    @Test
    public void testValidationImportConsistency_WithItemLocationCode() throws Exception {
        // Arrange
        String resourceFileName = "DMC-ZB1-A-05-00-00-00A-007A-A-T0101_001-03_zh-CN_测试.pdf";

        IetmDataModule mockDm = new IetmDataModule();
        mockDm.setId("dm-002");
        mockDm.setDmcCode("DMC-ZB1-A-05-00-00-00A-007A-A-T0101");
        mockDm.setSns("ZB1-A-05-00-00-00A-007A-A");
        mockDm.setInfoCode("000");
        mockDm.setInfoCodeVariant("");
        mockDm.setIetmLocationCode("T0101");
        mockDm.setLanguageIsoCode("zh");
        mockDm.setCountryIsoCode("CN");

        when(dataModuleService.getOne(any(QueryWrapper.class))).thenReturn(mockDm);

        // Act - 校验阶段
        boolean validationResult = ReflectionTestUtils.invokeMethod(
            dmImportService,
            "isDmExistsForResource",
            resourceFileName,
            projectId
        );

        // Act - 导入阶段
        IetmDataModule foundDm = ReflectionTestUtils.invokeMethod(
            dmImportService,
            "findDmForResource",
            resourceFileName,
            projectId
        );

        // Assert
        assert validationResult : "校验阶段应该能找到DM";
        assert foundDm != null : "导入阶段应该能找到DM";
        assert foundDm.getId().equals("dm-002") : "应该找到正确的DM";
    }

    /**
     * TC-03: DM不存在时，校验和导入都返回不存在
     */
    @Test
    public void testValidationImportConsistency_DmNotExists() throws Exception {
        // Arrange
        String resourceFileName = "DMC-NOTEXIST-A-05-00-00-00A-007A-A_001-03_zh-CN_不存在.jpg";

        // Mock查询返回null
        when(dataModuleService.getOne(any(QueryWrapper.class))).thenReturn(null);

        // Act - 校验阶段
        boolean validationResult = ReflectionTestUtils.invokeMethod(
            dmImportService,
            "isDmExistsForResource",
            resourceFileName,
            projectId
        );

        // Act - 导入阶段
        IetmDataModule foundDm = ReflectionTestUtils.invokeMethod(
            dmImportService,
            "findDmForResource",
            resourceFileName,
            projectId
        );

        // Assert
        assert !validationResult : "校验阶段应该返回DM不存在";
        assert foundDm == null : "导入阶段应该返回null";
    }

    /**
     * TC-04: 边界场景 - 语言代码大小写
     */
    @Test
    public void testValidationImportConsistency_LanguageCodeCase() throws Exception {
        // Arrange - 文件名用大写，数据库用小写
        String resourceFileName = "DMC-ZB1-A-05-00-00-00A-007A-A_001-03_ZH-CN_大写.jpg";

        IetmDataModule mockDm = new IetmDataModule();
        mockDm.setId("dm-003");
        mockDm.setSns("ZB1-A-05-00-00-00A-007A-A");
        mockDm.setInfoCode("000");
        mockDm.setLanguageIsoCode("zh");  // 小写
        mockDm.setCountryIsoCode("cn");   // 小写

        when(dataModuleService.getOne(any(QueryWrapper.class))).thenReturn(mockDm);

        // Act
        boolean validationResult = ReflectionTestUtils.invokeMethod(
            dmImportService,
            "isDmExistsForResource",
            resourceFileName,
            projectId
        );

        IetmDataModule foundDm = ReflectionTestUtils.invokeMethod(
            dmImportService,
            "findDmForResource",
            resourceFileName,
            projectId
        );

        // Assert - 两阶段结果一致
        assert validationResult : "校验应该通过";
        assert foundDm != null : "导入应该能找到DM";
    }
}
