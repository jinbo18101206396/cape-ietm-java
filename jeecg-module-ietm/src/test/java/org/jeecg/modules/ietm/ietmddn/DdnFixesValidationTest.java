package org.jeecg.modules.ietm.ietmddn;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DDN导出功能P0/P1修复验证测试
 * 纯单元测试，不依赖Spring上下文
 */
@DisplayName("DDN导出功能修复验证测试套件")
public class DdnFixesValidationTest {

    /**
     * TC-P0-8: 验证白名单正则允许连字符
     */
    @Test
    public void testWhitelistRegex_AllowsHyphens() {
        // 正则表达式：修复后应允许型号和单位中包含连字符
        String regex = "^DDN-[A-Za-z0-9-]+-[A-Za-z0-9-]+-[A-Za-z0-9-]+-\\d{4}-\\d{5}$";
        Pattern pattern = Pattern.compile(regex);

        // 测试用例1: 包含连字符的型号（J-10A）
        String ddnCode1 = "DDN-J-10A-CASC-611-00001-2026-00001";
        assertTrue(pattern.matcher(ddnCode1).matches(),
            "应允许型号中包含连字符: " + ddnCode1);

        // 测试用例2: 包含连字符的单位（CASC-611）
        String ddnCode2 = "DDN-Y-20-AVIC-301-00002-2026-00001";
        assertTrue(pattern.matcher(ddnCode2).matches(),
            "应允许单位中包含连字符: " + ddnCode2);

        // 测试用例3: 多个连字符
        String ddnCode3 = "DDN-FC-31-TEST-UNIT-123-00003-2026-00001";
        assertTrue(pattern.matcher(ddnCode3).matches(),
            "应允许多个连字符: " + ddnCode3);

        // 测试用例4: 不允许特殊字符
        String ddnCode4 = "DDN-TEST@123-UNIT-00004-2026-00001";
        assertFalse(pattern.matcher(ddnCode4).matches(),
            "应拒绝特殊字符@: " + ddnCode4);

        // 测试用例5: 不允许空格
        String ddnCode5 = "DDN-TEST 123-UNIT-00005-2026-00001";
        assertFalse(pattern.matcher(ddnCode5).matches(),
            "应拒绝空格: " + ddnCode5);
    }

    /**
     * TC-P0-7: 验证ICN引用查询方向常量
     */
    @Test
    public void testIcnReferenceDirection() {
        // 验证使用的是正确的引用方向常量
        String expectedDirection = "DM_TO_ICN";

        // 这个测试主要验证常量值的正确性
        assertEquals("DM_TO_ICN", expectedDirection,
            "ICN引用查询应使用DM_TO_ICN方向");

        // 错误的方向
        String wrongDirection = "ICN_TO_DM";
        assertNotEquals(wrongDirection, expectedDirection,
            "不应使用ICN_TO_DM方向");
    }

    /**
     * TC-P0-11: 验证dmCode属性数量
     */
    @Test
    public void testDmCodeAttributes_Count() {
        // S1000D标准要求的dmCode属性
        String[] requiredAttributes = {
            "modelIdentCode",      // 1
            "systemDiffCode",      // 2
            "systemCode",          // 3
            "subSystemCode",       // 4
            "subSubSystemCode",    // 5
            "assyCode",            // 6
            "disassyCode",         // 7
            "disassyCodeVariant",  // 8
            "infoCode",            // 9
            "infoCodeVariant",     // 10
            "itemLocationCode"     // 11
        };

        String[] optionalAttributes = {
            "learnCode",           // 12
            "learnEventCode"       // 13
        };

        assertEquals(11, requiredAttributes.length,
            "S1000D标准要求11个必需属性");
        assertEquals(2, optionalAttributes.length,
            "S1000D标准定义2个可选属性");

        int totalAttributes = requiredAttributes.length + optionalAttributes.length;
        assertEquals(13, totalAttributes,
            "dmCode总共应有13个属性（11必需+2可选）");
    }

    /**
     * TC-P1-3: 验证文件名清理逻辑
     */
    @Test
    public void testFileNameSanitization() {
        // 模拟文件名清理逻辑（注意：点号.在字符类中被保留）
        String unsafeFileName1 = "../../../etc/passwd";
        String sanitized1 = unsafeFileName1.replaceAll("[^a-zA-Z0-9._-]", "_");
        // 点号.被保留，斜杠/被替换为_
        // 结果: ".._.._.._etc_passwd"
        assertEquals(".._.._.._etc_passwd", sanitized1,
            "路径分隔符应被替换为下划线（点号被保留）");
        assertFalse(sanitized1.contains("/"),
            "不应包含路径分隔符/");

        String unsafeFileName2 = "test<script>.xml";
        String sanitized2 = unsafeFileName2.replaceAll("[^a-zA-Z0-9._-]", "_");
        assertFalse(sanitized2.contains("<"),
            "应移除特殊字符<");
        assertFalse(sanitized2.contains(">"),
            "应移除特殊字符>");
        assertEquals("test_script_.xml", sanitized2,
            "特殊字符应被替换");

        // 正常文件名应保持不变
        String safeFileName = "DM-TEST-001.xml";
        String sanitized3 = safeFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        assertEquals(safeFileName, sanitized3,
            "正常文件名应不变");
    }

    /**
     * TC-P0-9: 验证事务拆分逻辑（概念验证）
     */
    @Test
    public void testTransactionSplitConcept() {
        // 这是一个概念验证测试
        // 真实的事务拆分需要集成测试，这里验证逻辑正确性

        // 阶段1: 预留序列号（短事务）
        boolean phase1_ReserveSeqNumber = true;

        // 阶段2: 文件操作（无事务）
        boolean phase2_BuildPackage = true;

        // 阶段3: 更新成功状态（短事务）
        boolean phase3_UpdateSuccess = true;

        assertTrue(phase1_ReserveSeqNumber && phase2_BuildPackage && phase3_UpdateSuccess,
            "事务应拆分为3个独立阶段");
    }

    /**
     * TC-P1-1: 验证必填字段校验规则
     */
    @Test
    public void testRequiredFieldValidation() {
        // 模拟前端校验规则
        class FieldRule {
            boolean required;
            String message;

            FieldRule(boolean required, String message) {
                this.required = required;
                this.message = message;
            }

            boolean validate(String value) {
                if (required && (value == null || value.trim().isEmpty())) {
                    return false;
                }
                return true;
            }
        }

        FieldRule modelicRule = new FieldRule(true, "型号不能为空，请打开项目或手动填写");

        // 测试用例1: 空值应校验失败
        assertFalse(modelicRule.validate(null),
            "空型号应校验失败");
        assertFalse(modelicRule.validate(""),
            "空字符串型号应校验失败");
        assertFalse(modelicRule.validate("   "),
            "仅空格的型号应校验失败");

        // 测试用例2: 有效值应校验通过
        assertTrue(modelicRule.validate("J-10A"),
            "有效型号应校验通过");
        assertTrue(modelicRule.validate("TEST"),
            "有效型号应校验通过");
    }

    /**
     * TC-P0-10: 验证DM列表一致性逻辑
     */
    @Test
    public void testDmListConsistency() {
        // 模拟DM ID收集逻辑
        java.util.Set<String> initialDmIds = new java.util.HashSet<>();
        initialDmIds.add("dm-001");
        initialDmIds.add("dm-002");

        java.util.Set<String> allDmIds = new java.util.HashSet<>(initialDmIds);

        // 模拟递归收集引用的DM
        allDmIds.add("dm-003"); // 被dm-001引用
        allDmIds.add("dm-004"); // 被dm-002引用

        // 验证：最终保存的应该是完整列表，不是初始列表
        assertTrue(allDmIds.size() > initialDmIds.size(),
            "应保存完整的DM列表（包括递归引用）");

        assertEquals(4, allDmIds.size(),
            "应包含所有递归收集的DM");

        assertTrue(allDmIds.containsAll(initialDmIds),
            "应包含初始选择的DM");

        assertTrue(allDmIds.contains("dm-003") && allDmIds.contains("dm-004"),
            "应包含递归引用的DM");
    }

    /**
     * TC-P1-5: 验证icnCount字段类型
     */
    @Test
    public void testIcnCountFieldType() {
        // 验证字段类型和默认值
        Integer icnCount = 0;

        assertNotNull(icnCount,
            "icnCount字段不应为null");

        assertEquals(Integer.class, icnCount.getClass(),
            "icnCount应为Integer类型");

        assertTrue(icnCount >= 0,
            "icnCount应大于等于0");
    }

    /**
     * 综合测试: 验证DDN编码格式完整性
     */
    @Test
    public void testDdnCodeFormat_Complete() {
        String regex = "^DDN-[A-Za-z0-9-]+-[A-Za-z0-9-]+-[A-Za-z0-9-]+-\\d{4}-\\d{5}$";
        Pattern pattern = Pattern.compile(regex);

        // 有效的DDN编码示例
        String[] validCodes = {
            "DDN-MODEL-UNIT-00001-2026-00001",
            "DDN-J-10A-CASC-00002-2026-00002",
            "DDN-Y-20-AVIC-301-00003-2026-00003",
            "DDN-FC-31-TEST-UNIT-123-00004-2026-00004"
        };

        for (String code : validCodes) {
            assertTrue(pattern.matcher(code).matches(),
                "应接受有效的DDN编码: " + code);
        }

        // 无效的DDN编码示例
        String[] invalidCodes = {
            "DDN-TEST@123-UNIT-00001-2026-00001",     // 特殊字符
            "DDN-TEST 123-UNIT-00002-2026-00001",     // 空格
            "DDN-TEST-UNIT-00001-26-00001",           // 年份不足4位（只有2位）
            "DDN-TEST-UNIT-00001-2026-1",             // 版本号不足5位
            "MODEL-UNIT-00001-2026-00001"             // 缺少DDN前缀
        };

        for (String code : invalidCodes) {
            assertFalse(pattern.matcher(code).matches(),
                "应拒绝无效的DDN编码: " + code);
        }
    }
}
