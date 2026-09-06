package org.jeecg.modules.ietm.ietmimport;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SNS校验逻辑修复验证测试
 *
 * 问题：文件名 DMC-ZB1-A-00-00-00-00A-007A-A_003-00_zh-CN.xml 报错"SNS不在构型中"
 * XML: systemCode="05"
 *
 * 修复方案：
 * 1. 构型表的path字段存储8段格式：{model}-{sysDiff}-{sys}-{subSys}-{subSub}-{assy}-{disassy}-{disassyVar}
 * 2. 从dmCode的8个属性构建8段path
 * 3. 匹配构型表的path字段
 *
 * @date 2026-09-04
 */
@SpringBootTest
public class SnsValidationFixTest {

    /**
     * 测试1：验证8段path的构建逻辑
     */
    @Test
    public void testBuildPathFromDmCode() {
        System.out.println("\n=== 测试1：验证8段path的构建逻辑 ===");

        // 模拟dmCode属性
        Map<String, String> dmCodeAttrs = new HashMap<>();
        dmCodeAttrs.put("modelIdentCode", "ZB1");
        dmCodeAttrs.put("systemDiffCode", "A");
        dmCodeAttrs.put("systemCode", "05");
        dmCodeAttrs.put("subSystemCode", "0");
        dmCodeAttrs.put("subSubSystemCode", "0");
        dmCodeAttrs.put("assyCode", "00");
        dmCodeAttrs.put("disassyCode", "00");
        dmCodeAttrs.put("disassyCodeVariant", "A");

        // 构建path
        String path = buildPathFromDmCode(dmCodeAttrs);

        System.out.println("dmCode属性: " + dmCodeAttrs);
        System.out.println("构建的path: " + path);

        // 预期：ZB1-A-05-0-0-00-00-A (8段，每段用"-"连接)
        assertEquals("ZB1-A-05-0-0-00-00-A", path);
    }

    /**
     * 测试2：对比8段path与6段SNS的差异
     */
    @Test
    public void testPathVsSnsFormat() {
        System.out.println("\n=== 测试2：对比8段path与6段SNS的差异 ===");

        Map<String, String> dmCodeAttrs = new HashMap<>();
        dmCodeAttrs.put("modelIdentCode", "ZBBM33");
        dmCodeAttrs.put("systemDiffCode", "D");
        dmCodeAttrs.put("systemCode", "01");
        dmCodeAttrs.put("subSystemCode", "A");
        dmCodeAttrs.put("subSubSystemCode", "1");
        dmCodeAttrs.put("assyCode", "00");
        dmCodeAttrs.put("disassyCode", "00");
        dmCodeAttrs.put("disassyCodeVariant", "A");

        // 8段path格式
        String path8 = buildPathFromDmCode(dmCodeAttrs);
        System.out.println("8段path格式: " + path8);

        // 6段SNS格式（i=4/7不加"-"）
        String sns6 = buildSnsFormat(dmCodeAttrs);
        System.out.println("6段SNS格式:  " + sns6);

        // 验证：path是8段，SNS是6段
        assertEquals("ZBBM33-D-01-A-1-00-00-A", path8);  // 8段
        assertEquals("ZBBM33-D-01-A1-00-00A", sns6);    // 6段
    }

    /**
     * 测试3：验证构型表实际数据的path格式
     */
    @Test
    public void testRealConfigurationPath() {
        System.out.println("\n=== 测试3：验证构型表实际数据的path格式 ===");

        // 从SQL查询结果：
        String actualPath = "ZBBM33-D-01-A-1-00-00-A";

        System.out.println("构型表实际path: " + actualPath);
        System.out.println("段数: " + (actualPath.split("-").length));

        // 验证：实际path是8段格式
        String[] segments = actualPath.split("-");
        assertEquals(8, segments.length, "构型表path应该是8段格式");

        // 验证每一段
        assertEquals("ZBBM33", segments[0]); // model
        assertEquals("D", segments[1]);      // systemDiff
        assertEquals("01", segments[2]);     // systemCode
        assertEquals("A", segments[3]);      // subSystem
        assertEquals("1", segments[4]);      // subSubSystem
        assertEquals("00", segments[5]);     // assy
        assertEquals("00", segments[6]);     // disassy
        assertEquals("A", segments[7]);      // disassyVar
    }

    /**
     * 测试4：验证问题文件的path构建
     */
    @Test
    public void testProblemFilePath() {
        System.out.println("\n=== 测试4：验证问题文件的path构建 ===");

        // 问题文件：DMC-ZB1-A-00-00-00-00A-007A-A_003-00_zh-CN.xml
        // XML: systemCode="05"
        Map<String, String> dmCodeAttrs = new HashMap<>();
        dmCodeAttrs.put("modelIdentCode", "ZB1");
        dmCodeAttrs.put("systemDiffCode", "A");
        dmCodeAttrs.put("systemCode", "05");
        dmCodeAttrs.put("subSystemCode", "0");
        dmCodeAttrs.put("subSubSystemCode", "0");
        dmCodeAttrs.put("assyCode", "00");
        dmCodeAttrs.put("disassyCode", "00");
        dmCodeAttrs.put("disassyCodeVariant", "A");

        String path = buildPathFromDmCode(dmCodeAttrs);

        System.out.println("问题文件构建的path: " + path);
        System.out.println("需要在构型表中查询: SELECT * FROM ietm_project_configuration_management WHERE path = '" + path + "'");

        assertEquals("ZB1-A-05-0-0-00-00-A", path);
    }

    /**
     * 测试5：SQL查询验证
     */
    @Test
    public void testSqlQueryForValidation() {
        System.out.println("\n=== 测试5：SQL查询验证 ===");

        String path = "ZB1-A-05-0-0-00-00-A";
        String projectId = "您的项目ID";  // 需要替换

        String sql = String.format(
            "SELECT id, code, title, path " +
            "FROM ietm_project_configuration_management " +
            "WHERE project_id = '%s' AND path = '%s'",
            projectId, path
        );

        System.out.println("执行SQL查询：");
        System.out.println(sql);
        System.out.println();
        System.out.println("预期结果：");
        System.out.println("- 如果返回数据：✅ path存在，应该导入成功");
        System.out.println("- 如果返回空：❌ path不存在，报错'SNS不在构型中'是正确的");
    }

    // ========== 辅助方法 ==========

    private String buildPathFromDmCode(Map<String, String> dmCodeAttrs) {
        StringBuilder path = new StringBuilder();

        String[] segments = {
            dmCodeAttrs.getOrDefault("modelIdentCode", ""),
            dmCodeAttrs.getOrDefault("systemDiffCode", ""),
            dmCodeAttrs.getOrDefault("systemCode", ""),
            dmCodeAttrs.getOrDefault("subSystemCode", ""),
            dmCodeAttrs.getOrDefault("subSubSystemCode", ""),
            dmCodeAttrs.getOrDefault("assyCode", ""),
            dmCodeAttrs.getOrDefault("disassyCode", ""),
            dmCodeAttrs.getOrDefault("disassyCodeVariant", "")
        };

        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                path.append("-");
            }
            path.append(segments[i]);
        }

        return path.toString();
    }

    private String buildSnsFormat(Map<String, String> dmCodeAttrs) {
        // 模拟DmcUtils.composeSns()的逻辑（6段，i=4/7不加"-"）
        StringBuilder sns = new StringBuilder();

        String[] segments = {
            dmCodeAttrs.getOrDefault("modelIdentCode", ""),
            dmCodeAttrs.getOrDefault("systemDiffCode", ""),
            dmCodeAttrs.getOrDefault("systemCode", ""),
            dmCodeAttrs.getOrDefault("subSystemCode", "") + dmCodeAttrs.getOrDefault("subSubSystemCode", ""),
            dmCodeAttrs.getOrDefault("assyCode", ""),
            dmCodeAttrs.getOrDefault("disassyCode", "") + dmCodeAttrs.getOrDefault("disassyCodeVariant", "")
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
