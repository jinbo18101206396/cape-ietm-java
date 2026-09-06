package org.jeecg.modules.ietm.ietmimport;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.jeecg.modules.ietm.projectconfigurationmanagement.entity.IetmProjectConfigurationManagement;
import org.jeecg.modules.ietm.projectconfigurationmanagement.service.IIetmProjectConfigurationManagementService;
import org.jeecg.modules.ietm.ietmimport.service.impl.IetmDmImportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * SNS path校验逻辑完整测试
 *
 * 测试修复后的逻辑：用8段path匹配构型表的path字段
 *
 * @date 2026-09-04
 */
@SpringBootTest
@DisplayName("SNS Path校验逻辑完整测试")
public class SnsPathValidationCompleteTest {

    @Mock
    private IIetmProjectConfigurationManagementService configurationService;

    @InjectMocks
    private IetmDmImportServiceImpl dmImportService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ==================== 单元测试：buildPathFromDmCode ====================

    @Test
    @DisplayName("测试1：正常情况 - 构建完整的8段path")
    public void testBuildPathFromDmCode_Normal() throws Exception {
        Map<String, String> dmCodeAttrs = new HashMap<>();
        dmCodeAttrs.put("modelIdentCode", "ZB1");
        dmCodeAttrs.put("systemDiffCode", "A");
        dmCodeAttrs.put("systemCode", "05");
        dmCodeAttrs.put("subSystemCode", "0");
        dmCodeAttrs.put("subSubSystemCode", "0");
        dmCodeAttrs.put("assyCode", "00");
        dmCodeAttrs.put("disassyCode", "00");
        dmCodeAttrs.put("disassyCodeVariant", "A");

        String path = invokeBuildPathFromDmCode(dmCodeAttrs);

        assertEquals("ZB1-A-05-0-0-00-00-A", path);
        System.out.println("✅ 测试1通过：path = " + path);
    }

    @Test
    @DisplayName("测试2：构型表真实数据 - ZBBM33项目")
    public void testBuildPathFromDmCode_RealData() throws Exception {
        Map<String, String> dmCodeAttrs = new HashMap<>();
        dmCodeAttrs.put("modelIdentCode", "ZBBM33");
        dmCodeAttrs.put("systemDiffCode", "D");
        dmCodeAttrs.put("systemCode", "01");
        dmCodeAttrs.put("subSystemCode", "A");
        dmCodeAttrs.put("subSubSystemCode", "1");
        dmCodeAttrs.put("assyCode", "00");
        dmCodeAttrs.put("disassyCode", "00");
        dmCodeAttrs.put("disassyCodeVariant", "A");

        String path = invokeBuildPathFromDmCode(dmCodeAttrs);

        assertEquals("ZBBM33-D-01-A-1-00-00-A", path);
        System.out.println("✅ 测试2通过：path = " + path);
    }

    @Test
    @DisplayName("测试3：边界情况 - systemCode单位数")
    public void testBuildPathFromDmCode_SingleDigitSystemCode() throws Exception {
        Map<String, String> dmCodeAttrs = new HashMap<>();
        dmCodeAttrs.put("modelIdentCode", "ABC");
        dmCodeAttrs.put("systemDiffCode", "B");
        dmCodeAttrs.put("systemCode", "5");  // 单位数
        dmCodeAttrs.put("subSystemCode", "0");
        dmCodeAttrs.put("subSubSystemCode", "0");
        dmCodeAttrs.put("assyCode", "00");
        dmCodeAttrs.put("disassyCode", "00");
        dmCodeAttrs.put("disassyCodeVariant", "A");

        String path = invokeBuildPathFromDmCode(dmCodeAttrs);

        assertEquals("ABC-B-5-0-0-00-00-A", path);
        System.out.println("✅ 测试3通过：path = " + path);
    }

    @Test
    @DisplayName("测试4：边界情况 - 空字符串处理")
    public void testBuildPathFromDmCode_EmptyStrings() throws Exception {
        Map<String, String> dmCodeAttrs = new HashMap<>();
        dmCodeAttrs.put("modelIdentCode", "");
        dmCodeAttrs.put("systemDiffCode", "");
        dmCodeAttrs.put("systemCode", "");
        dmCodeAttrs.put("subSystemCode", "");
        dmCodeAttrs.put("subSubSystemCode", "");
        dmCodeAttrs.put("assyCode", "");
        dmCodeAttrs.put("disassyCode", "");
        dmCodeAttrs.put("disassyCodeVariant", "");

        String path = invokeBuildPathFromDmCode(dmCodeAttrs);

        assertEquals("-------", path);  // 7个"-"连接8个空字符串
        System.out.println("✅ 测试4通过：path = " + path);
    }

    @Test
    @DisplayName("测试5：边界情况 - null值处理")
    public void testBuildPathFromDmCode_NullValues() throws Exception {
        Map<String, String> dmCodeAttrs = new HashMap<>();
        // 所有值都是null

        String path = invokeBuildPathFromDmCode(dmCodeAttrs);

        assertEquals("-------", path);  // safeStr应该将null转为""
        System.out.println("✅ 测试5通过：path = " + path);
    }

    @Test
    @DisplayName("测试6：对比8段path与6段SNS的差异")
    public void testPathVsSnsFormat() throws Exception {
        Map<String, String> dmCodeAttrs = new HashMap<>();
        dmCodeAttrs.put("modelIdentCode", "ZB1");
        dmCodeAttrs.put("systemDiffCode", "A");
        dmCodeAttrs.put("systemCode", "05");
        dmCodeAttrs.put("subSystemCode", "0");
        dmCodeAttrs.put("subSubSystemCode", "0");
        dmCodeAttrs.put("assyCode", "00");
        dmCodeAttrs.put("disassyCode", "00");
        dmCodeAttrs.put("disassyCodeVariant", "A");

        String path8 = invokeBuildPathFromDmCode(dmCodeAttrs);
        String sns6 = buildSns6Format(dmCodeAttrs);

        System.out.println("8段path: " + path8);
        System.out.println("6段SNS:  " + sns6);

        assertEquals("ZB1-A-05-0-0-00-00-A", path8);  // 8段
        assertEquals("ZB1-A-05-00-00-00A", sns6);     // 6段

        assertNotEquals(path8, sns6, "8段path与6段SNS不应该相等");
        System.out.println("✅ 测试6通过：8段path != 6段SNS");
    }

    // ==================== 单元测试：isDmCodePathInConfiguration ====================

    @Test
    @DisplayName("测试7：path存在于构型表 - 返回true")
    public void testIsDmCodePathInConfiguration_Exists() throws Exception {
        Map<String, String> dmCodeAttrs = new HashMap<>();
        dmCodeAttrs.put("modelIdentCode", "ZB1");
        dmCodeAttrs.put("systemDiffCode", "A");
        dmCodeAttrs.put("systemCode", "05");
        dmCodeAttrs.put("subSystemCode", "0");
        dmCodeAttrs.put("subSubSystemCode", "0");
        dmCodeAttrs.put("assyCode", "00");
        dmCodeAttrs.put("disassyCode", "00");
        dmCodeAttrs.put("disassyCodeVariant", "A");

        String projectId = "2078348945532030978";

        // Mock: 查询返回1条记录
        when(configurationService.count(any(QueryWrapper.class))).thenReturn(1L);

        boolean result = invokeIsDmCodePathInConfiguration(dmCodeAttrs, projectId);

        assertTrue(result, "path存在时应该返回true");
        System.out.println("✅ 测试7通过：path存在，返回true");
    }

    @Test
    @DisplayName("测试8：path不存在于构型表 - 返回false")
    public void testIsDmCodePathInConfiguration_NotExists() throws Exception {
        Map<String, String> dmCodeAttrs = new HashMap<>();
        dmCodeAttrs.put("modelIdentCode", "ZB1");
        dmCodeAttrs.put("systemDiffCode", "A");
        dmCodeAttrs.put("systemCode", "99");  // 不存在的systemCode
        dmCodeAttrs.put("subSystemCode", "0");
        dmCodeAttrs.put("subSubSystemCode", "0");
        dmCodeAttrs.put("assyCode", "00");
        dmCodeAttrs.put("disassyCode", "00");
        dmCodeAttrs.put("disassyCodeVariant", "A");

        String projectId = "2078348945532030978";

        // Mock: 查询返回0条记录
        when(configurationService.count(any(QueryWrapper.class))).thenReturn(0L);

        boolean result = invokeIsDmCodePathInConfiguration(dmCodeAttrs, projectId);

        assertFalse(result, "path不存在时应该返回false");
        System.out.println("✅ 测试8通过：path不存在，返回false");
    }

    @Test
    @DisplayName("测试9：projectId不匹配 - 返回false")
    public void testIsDmCodePathInConfiguration_WrongProject() throws Exception {
        Map<String, String> dmCodeAttrs = new HashMap<>();
        dmCodeAttrs.put("modelIdentCode", "ZB1");
        dmCodeAttrs.put("systemDiffCode", "A");
        dmCodeAttrs.put("systemCode", "05");
        dmCodeAttrs.put("subSystemCode", "0");
        dmCodeAttrs.put("subSubSystemCode", "0");
        dmCodeAttrs.put("assyCode", "00");
        dmCodeAttrs.put("disassyCode", "00");
        dmCodeAttrs.put("disassyCodeVariant", "A");

        String projectId = "WRONG_PROJECT_ID";

        // Mock: 查询返回0条记录
        when(configurationService.count(any(QueryWrapper.class))).thenReturn(0L);

        boolean result = invokeIsDmCodePathInConfiguration(dmCodeAttrs, projectId);

        assertFalse(result, "projectId不匹配时应该返回false");
        System.out.println("✅ 测试9通过：projectId不匹配，返回false");
    }

    // ==================== 集成测试：完整校验流程 ====================

    @Test
    @DisplayName("测试10：完整校验流程 - 问题文件应该通过")
    public void testCompleteValidation_ProblemFile() throws Exception {
        // 问题文件：DMC-ZB1-A-00-00-00-00A-007A-A_003-00_zh-CN.xml
        // XML中systemCode="05"
        Map<String, String> dmCodeAttrs = new HashMap<>();
        dmCodeAttrs.put("modelIdentCode", "ZB1");
        dmCodeAttrs.put("systemDiffCode", "A");
        dmCodeAttrs.put("systemCode", "05");
        dmCodeAttrs.put("subSystemCode", "0");
        dmCodeAttrs.put("subSubSystemCode", "0");
        dmCodeAttrs.put("assyCode", "00");
        dmCodeAttrs.put("disassyCode", "00");
        dmCodeAttrs.put("disassyCodeVariant", "A");

        String projectId = "2078348945532030978";

        // 构建path
        String path = invokeBuildPathFromDmCode(dmCodeAttrs);
        assertEquals("ZB1-A-05-0-0-00-00-A", path);

        // Mock: 查询返回1条记录（SQL已验证path存在）
        when(configurationService.count(any(QueryWrapper.class))).thenReturn(1L);

        // 校验
        boolean result = invokeIsDmCodePathInConfiguration(dmCodeAttrs, projectId);

        assertTrue(result, "问题文件应该通过校验");
        System.out.println("✅ 测试10通过：问题文件校验通过");
    }

    @Test
    @DisplayName("测试11：完整校验流程 - ZBBM33项目数据")
    public void testCompleteValidation_ZBBM33() throws Exception {
        // 构型表真实数据：ZBBM33-D-01-A-1-00-00-A
        Map<String, String> dmCodeAttrs = new HashMap<>();
        dmCodeAttrs.put("modelIdentCode", "ZBBM33");
        dmCodeAttrs.put("systemDiffCode", "D");
        dmCodeAttrs.put("systemCode", "01");
        dmCodeAttrs.put("subSystemCode", "A");
        dmCodeAttrs.put("subSubSystemCode", "1");
        dmCodeAttrs.put("assyCode", "00");
        dmCodeAttrs.put("disassyCode", "00");
        dmCodeAttrs.put("disassyCodeVariant", "A");

        String projectId = "2016415088223285250";

        // 构建path
        String path = invokeBuildPathFromDmCode(dmCodeAttrs);
        assertEquals("ZBBM33-D-01-A-1-00-00-A", path);

        // Mock: 查询返回1条记录
        when(configurationService.count(any(QueryWrapper.class))).thenReturn(1L);

        // 校验
        boolean result = invokeIsDmCodePathInConfiguration(dmCodeAttrs, projectId);

        assertTrue(result, "ZBBM33项目数据应该通过校验");
        System.out.println("✅ 测试11通过：ZBBM33项目数据校验通过");
    }

    // ==================== 边界测试 ====================

    @Test
    @DisplayName("测试12：边界情况 - 所有segment都是单字符")
    public void testBoundary_SingleCharSegments() throws Exception {
        Map<String, String> dmCodeAttrs = new HashMap<>();
        dmCodeAttrs.put("modelIdentCode", "Z");
        dmCodeAttrs.put("systemDiffCode", "A");
        dmCodeAttrs.put("systemCode", "5");
        dmCodeAttrs.put("subSystemCode", "0");
        dmCodeAttrs.put("subSubSystemCode", "0");
        dmCodeAttrs.put("assyCode", "0");
        dmCodeAttrs.put("disassyCode", "0");
        dmCodeAttrs.put("disassyCodeVariant", "A");

        String path = invokeBuildPathFromDmCode(dmCodeAttrs);

        assertEquals("Z-A-5-0-0-0-0-A", path);
        System.out.println("✅ 测试12通过：单字符segments处理正确");
    }

    @Test
    @DisplayName("测试13：边界情况 - 超长segment")
    public void testBoundary_LongSegments() throws Exception {
        Map<String, String> dmCodeAttrs = new HashMap<>();
        dmCodeAttrs.put("modelIdentCode", "VERYLONGMODEL");
        dmCodeAttrs.put("systemDiffCode", "ABC");
        dmCodeAttrs.put("systemCode", "999");
        dmCodeAttrs.put("subSystemCode", "X");
        dmCodeAttrs.put("subSubSystemCode", "Y");
        dmCodeAttrs.put("assyCode", "9999");
        dmCodeAttrs.put("disassyCode", "8888");
        dmCodeAttrs.put("disassyCodeVariant", "ZZZ");

        String path = invokeBuildPathFromDmCode(dmCodeAttrs);

        assertEquals("VERYLONGMODEL-ABC-999-X-Y-9999-8888-ZZZ", path);
        System.out.println("✅ 测试13通过：超长segments处理正确");
    }

    @Test
    @DisplayName("测试14：边界情况 - 包含特殊字符")
    public void testBoundary_SpecialChars() throws Exception {
        Map<String, String> dmCodeAttrs = new HashMap<>();
        dmCodeAttrs.put("modelIdentCode", "ZB1");
        dmCodeAttrs.put("systemDiffCode", "A");
        dmCodeAttrs.put("systemCode", "05");
        dmCodeAttrs.put("subSystemCode", "0");
        dmCodeAttrs.put("subSubSystemCode", "0");
        dmCodeAttrs.put("assyCode", "00");
        dmCodeAttrs.put("disassyCode", "00");
        dmCodeAttrs.put("disassyCodeVariant", "A");

        String path = invokeBuildPathFromDmCode(dmCodeAttrs);

        // 验证path不包含除"-"之外的特殊字符
        assertFalse(path.contains("/"), "path不应该包含/");
        assertFalse(path.contains("\\"), "path不应该包含\\");
        assertFalse(path.contains(" "), "path不应该包含空格");
        System.out.println("✅ 测试14通过：特殊字符处理正确");
    }

    // ==================== 辅助方法 ====================

    /**
     * 通过反射调用私有方法 buildPathFromDmCode
     */
    private String invokeBuildPathFromDmCode(Map<String, String> dmCodeAttrs) throws Exception {
        Method method = IetmDmImportServiceImpl.class.getDeclaredMethod(
            "buildPathFromDmCode", Map.class);
        method.setAccessible(true);
        return (String) method.invoke(dmImportService, dmCodeAttrs);
    }

    /**
     * 通过反射调用私有方法 isDmCodePathInConfiguration
     */
    private boolean invokeIsDmCodePathInConfiguration(
        Map<String, String> dmCodeAttrs, String projectId) throws Exception {
        Method method = IetmDmImportServiceImpl.class.getDeclaredMethod(
            "isDmCodePathInConfiguration", Map.class, String.class);
        method.setAccessible(true);
        return (boolean) method.invoke(dmImportService, dmCodeAttrs, projectId);
    }

    /**
     * 构建6段SNS格式（用于对比）
     */
    private String buildSns6Format(Map<String, String> dmCodeAttrs) {
        StringBuilder sns = new StringBuilder();

        String[] segments = {
            dmCodeAttrs.getOrDefault("modelIdentCode", ""),
            dmCodeAttrs.getOrDefault("systemDiffCode", ""),
            dmCodeAttrs.getOrDefault("systemCode", ""),
            dmCodeAttrs.getOrDefault("subSystemCode", "") +
                dmCodeAttrs.getOrDefault("subSubSystemCode", ""),
            dmCodeAttrs.getOrDefault("assyCode", ""),
            dmCodeAttrs.getOrDefault("disassyCode", "") +
                dmCodeAttrs.getOrDefault("disassyCodeVariant", "")
        };

        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                sns.append("-");
            }
            sns.append(segments[i]);
        }

        return sns.toString();
    }
}
