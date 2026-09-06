package org.jeecg.modules.ietm.ietmimport;

import org.jeecg.modules.ietm.ietmdatamodulemanagement.util.DmcUtils;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * SNS与DMC调试测试 - 排查"SNS不在构型中"问题
 *
 * 问题文件：DMC-ZB1-A-00-00-00-00A-007A-A_003-00_zh-CN.xml
 * 报错：SNS不在构型中
 *
 * 目标：
 * 1. 验证从文件名解析的DMC是否正确
 * 2. 验证从dmCode属性生成的SNS是否正确
 * 3. 对比SNS格式与构型表的code字段格式
 */
public class SnsDmcDebugTest {

    /**
     * 测试场景1：解析问题文件名的DMC结构
     */
    @Test
    public void testParseDmcFromFileName() {
        String fileName = "DMC-ZB1-A-00-00-00-00A-007A-A_003-00_zh-CN.xml";

        System.out.println("=== 文件名解析 ===");
        System.out.println("原始文件名：" + fileName);

        // 去掉扩展名
        String baseName = fileName.replace(".xml", "");
        System.out.println("去扩展名：" + baseName);

        // 提取基础DMC（第一个_之前）
        int firstUnderscoreIndex = baseName.indexOf("_");
        String baseDmc = baseName.substring(0, firstUnderscoreIndex);
        System.out.println("基础DMC：" + baseDmc);

        // 解析DMC结构
        // 格式：DMC-{sns}-{infoCode}{infoCodeVariant}-{itemLocationCode}
        // 示例：DMC-ZB1-A-00-00-00-00A-007A-A
        String[] parts = baseDmc.split("-");
        System.out.println("\nDMC分段（共" + parts.length + "段）：");
        for (int i = 0; i < parts.length; i++) {
            System.out.println("  [" + i + "] " + parts[i]);
        }

        // 根据S1000D标准，DMC格式为：
        // DMC-{modelIdentCode}-{systemDiffCode}-{systemCode}-{subSystemCode+subSubSystemCode}-{assyCode}-{disassyCode+disassyCodeVariant}-{infoCode+infoCodeVariant}-{itemLocationCode}
        // 但实际SNS是压缩格式：{model}-{sysDiff}-{sys}-{subSys+subSub}-{assy}-{dis+disVar}

        System.out.println("\n=== 推测的dmCode属性（需要从XML确认） ===");
        if (parts.length >= 8) {
            System.out.println("modelIdentCode: " + parts[1]);  // ZB1
            System.out.println("systemDiffCode: " + parts[2]);  // A
            System.out.println("systemCode: " + parts[3]);      // 00
            System.out.println("subSystemCode+subSubSystemCode: " + parts[4]);  // 00
            System.out.println("assyCode: " + parts[5]);        // 00
            System.out.println("disassyCode+disassyCodeVariant: " + parts[6]);  // 00A
            System.out.println("infoCode+infoCodeVariant: " + parts[7]);        // 007A
            if (parts.length >= 9) {
                System.out.println("itemLocationCode: " + parts[8]);  // A
            }
        }
    }

    /**
     * 测试场景2：根据文件名推测的属性生成SNS
     */
    @Test
    public void testGenerateSnsFromFileName() {
        System.out.println("=== 从文件名推测生成SNS ===");

        // 根据文件名 DMC-ZB1-A-00-00-00-00A-007A-A 推测的属性
        Map<String, String> dmCodeAttrs = new HashMap<>();
        dmCodeAttrs.put("modelIdentCode", "ZB1");
        dmCodeAttrs.put("systemDiffCode", "A");
        dmCodeAttrs.put("systemCode", "00");
        dmCodeAttrs.put("subSystemCode", "00");
        dmCodeAttrs.put("subSubSystemCode", "");     // 可能为空
        dmCodeAttrs.put("assyCode", "00");
        dmCodeAttrs.put("disassyCode", "A");         // 注意：文件名中是00A，可能是00+A
        dmCodeAttrs.put("disassyCodeVariant", "");   // 可能为空

        String sns1 = DmcUtils.composeSns(
            dmCodeAttrs.get("modelIdentCode"),
            dmCodeAttrs.get("systemDiffCode"),
            dmCodeAttrs.get("systemCode"),
            dmCodeAttrs.get("subSystemCode"),
            dmCodeAttrs.get("subSubSystemCode"),
            dmCodeAttrs.get("assyCode"),
            dmCodeAttrs.get("disassyCode"),
            dmCodeAttrs.get("disassyCodeVariant")
        );

        System.out.println("推测1（subSub为空，disVar为空）：" + sns1);

        // 尝试另一种解析
        dmCodeAttrs.put("subSystemCode", "0");
        dmCodeAttrs.put("subSubSystemCode", "0");
        dmCodeAttrs.put("assyCode", "00");
        dmCodeAttrs.put("disassyCode", "00");
        dmCodeAttrs.put("disassyCodeVariant", "A");

        String sns2 = DmcUtils.composeSns(
            dmCodeAttrs.get("modelIdentCode"),
            dmCodeAttrs.get("systemDiffCode"),
            dmCodeAttrs.get("systemCode"),
            dmCodeAttrs.get("subSystemCode"),
            dmCodeAttrs.get("subSubSystemCode"),
            dmCodeAttrs.get("assyCode"),
            dmCodeAttrs.get("disassyCode"),
            dmCodeAttrs.get("disassyCodeVariant")
        );

        System.out.println("推测2（subSub=0, disVar=A）：" + sns2);
    }

    /**
     * 测试场景3：标准的SNS格式示例
     */
    @Test
    public void testStandardSnsFormat() {
        System.out.println("=== 标准SNS格式示例 ===");

        // 示例1：完整属性
        Map<String, String> attrs1 = new HashMap<>();
        attrs1.put("modelIdentCode", "ZB1");
        attrs1.put("systemDiffCode", "A");
        attrs1.put("systemCode", "00");
        attrs1.put("subSystemCode", "00");
        attrs1.put("subSubSystemCode", "00");
        attrs1.put("assyCode", "A");
        attrs1.put("disassyCode", "007");
        attrs1.put("disassyCodeVariant", "A");

        String sns1 = DmcUtils.composeSns(
            attrs1.get("modelIdentCode"),
            attrs1.get("systemDiffCode"),
            attrs1.get("systemCode"),
            attrs1.get("subSystemCode"),
            attrs1.get("subSubSystemCode"),
            attrs1.get("assyCode"),
            attrs1.get("disassyCode"),
            attrs1.get("disassyCodeVariant")
        );

        System.out.println("示例1（完整）：" + sns1);
        System.out.println("  - modelIdentCode: ZB1");
        System.out.println("  - systemDiffCode: A");
        System.out.println("  - systemCode: 00");
        System.out.println("  - subSystemCode: 00");
        System.out.println("  - subSubSystemCode: 00");
        System.out.println("  - assyCode: A");
        System.out.println("  - disassyCode: 007");
        System.out.println("  - disassyCodeVariant: A");

        // 示例2：与文件名可能匹配的属性
        Map<String, String> attrs2 = new HashMap<>();
        attrs2.put("modelIdentCode", "ZB1");
        attrs2.put("systemDiffCode", "A");
        attrs2.put("systemCode", "00");
        attrs2.put("subSystemCode", "00");
        attrs2.put("subSubSystemCode", "00");
        attrs2.put("assyCode", "00");
        attrs2.put("disassyCode", "A");
        attrs2.put("disassyCodeVariant", "");

        String sns2 = DmcUtils.composeSns(
            attrs2.get("modelIdentCode"),
            attrs2.get("systemDiffCode"),
            attrs2.get("systemCode"),
            attrs2.get("subSystemCode"),
            attrs2.get("subSubSystemCode"),
            attrs2.get("assyCode"),
            attrs2.get("disassyCode"),
            attrs2.get("disassyCodeVariant")
        );

        System.out.println("\n示例2（文件名推测）：" + sns2);
        System.out.println("  - disassyCode: A（而非007A）");

        System.out.println("\n=== 结论 ===");
        System.out.println("需要从实际XML文件中提取dmCode元素的属性值，才能确认正确的SNS");
    }

    /**
     * 测试场景4：诊断文件名中的"00A"和"007A"含义
     */
    @Test
    public void testDmcSegmentInterpretation() {
        System.out.println("=== DMC文件名分段解释 ===");
        System.out.println("文件名：DMC-ZB1-A-00-00-00-00A-007A-A_003-00_zh-CN.xml");
        System.out.println();

        System.out.println("可能的解释1（标准S1000D压缩格式）：");
        System.out.println("  DMC-{sns}-{infoCode}{infoCodeVariant}-{itemLocationCode}");
        System.out.println("  DMC-ZB1-A-00-00-00-00A-007A-A");
        System.out.println("    sns = ZB1-A-00-00-00-00A");
        System.out.println("    infoCode+Variant = 007A");
        System.out.println("    itemLocationCode = A");
        System.out.println();

        System.out.println("可能的解释2（展开格式，但不符合composeSns）：");
        System.out.println("  DMC-{model}-{sysDiff}-{sys}-{subSys}-{subSub}-{assy}-{dis+disVar}-{info+infoVar}-{loc}");
        System.out.println("  DMC-ZB1-A-00-00-00-00A-007A-A");
        System.out.println("    但这样有9段，不符合标准");
        System.out.println();

        System.out.println("⚠️ 关键疑问：");
        System.out.println("1. 文件名中的'00A'是否是 disassyCode='00' + disassyCodeVariant='A' ？");
        System.out.println("2. 还是 assyCode='00A' ？");
        System.out.println("3. '007A'是否是 infoCode='007' + infoCodeVariant='A' ？");
        System.out.println("4. 还是 disassyCode='007' + disassyCodeVariant='A' ？");
        System.out.println();
        System.out.println("✅ 解决方案：读取XML文件的<dmCode>元素属性值");
    }

    /**
     * 测试场景5：验证构型表查询逻辑
     */
    @Test
    public void testConfigurationQuery() {
        System.out.println("=== 构型表查询逻辑 ===");
        System.out.println("查询条件：");
        System.out.println("  - project_id = ? （当前项目ID）");
        System.out.println("  - code = ? （SNS值）");
        System.out.println();
        System.out.println("⚠️ 关键：构型表的code字段存储的是什么格式的SNS？");
        System.out.println("  - 是否是完整的6段格式？如：ZB1-A-00-0000-A-007A");
        System.out.println("  - 还是简化格式？如：ZB1-A-00-00-00-00A");
        System.out.println("  - 还是仅systemCode？如：00");
        System.out.println();
        System.out.println("✅ 解决方案：查询ietm_project_configuration_management表，查看实际数据");
    }
}
