package org.jeecg.modules.ietm.ietmimport;

import org.jeecg.modules.ietm.ietmdatamodulemanagement.util.DmcUtils;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * SNS不在构型中 - 真实XML数据分析
 *
 * 文件名：DMC-ZB1-A-00-00-00-00A-007A-A_003-00_zh-CN.xml
 * XML dmCode属性（真实值）：
 *   modelIdentCode="ZB1"
 *   systemDiffCode="A"
 *   systemCode="05"          ⚠️ 注意：是"05"而非文件名中的"00"
 *   subSystemCode="0"
 *   subSubSystemCode="0"
 *   assyCode="00"
 *   disassyCode="00"
 *   disassyCodeVariant="A"
 *   infoCode="007"
 *   infoCodeVariant="A"
 *   itemLocationCode="A"
 */
public class SnsRealDataAnalysisTest {

    /**
     * 测试1：使用真实XML属性生成SNS
     */
    @Test
    public void testGenerateSnsFromRealXml() {
        System.out.println("=== 使用真实XML属性生成SNS ===");
        System.out.println();

        Map<String, String> dmCodeAttrs = new HashMap<>();
        dmCodeAttrs.put("modelIdentCode", "ZB1");
        dmCodeAttrs.put("systemDiffCode", "A");
        dmCodeAttrs.put("systemCode", "05");         // ⚠️ 真实值是"05"
        dmCodeAttrs.put("subSystemCode", "0");
        dmCodeAttrs.put("subSubSystemCode", "0");
        dmCodeAttrs.put("assyCode", "00");
        dmCodeAttrs.put("disassyCode", "00");
        dmCodeAttrs.put("disassyCodeVariant", "A");

        String sns = DmcUtils.composeSns(
            dmCodeAttrs.get("modelIdentCode"),
            dmCodeAttrs.get("systemDiffCode"),
            dmCodeAttrs.get("systemCode"),
            dmCodeAttrs.get("subSystemCode"),
            dmCodeAttrs.get("subSubSystemCode"),
            dmCodeAttrs.get("assyCode"),
            dmCodeAttrs.get("disassyCode"),
            dmCodeAttrs.get("disassyCodeVariant")
        );

        System.out.println("📋 真实XML属性：");
        System.out.println("  modelIdentCode: " + dmCodeAttrs.get("modelIdentCode"));
        System.out.println("  systemDiffCode: " + dmCodeAttrs.get("systemDiffCode"));
        System.out.println("  systemCode: " + dmCodeAttrs.get("systemCode") + " ⚠️");
        System.out.println("  subSystemCode: " + dmCodeAttrs.get("subSystemCode"));
        System.out.println("  subSubSystemCode: " + dmCodeAttrs.get("subSubSystemCode"));
        System.out.println("  assyCode: " + dmCodeAttrs.get("assyCode"));
        System.out.println("  disassyCode: " + dmCodeAttrs.get("disassyCode"));
        System.out.println("  disassyCodeVariant: " + dmCodeAttrs.get("disassyCodeVariant"));
        System.out.println();
        System.out.println("✅ 生成的SNS：" + sns);
        System.out.println();
        System.out.println("🔍 这个SNS需要在构型表中存在：");
        System.out.println("  SELECT * FROM ietm_project_configuration_management");
        System.out.println("  WHERE code = '" + sns + "';");
    }

    /**
     * 测试2：对比文件名与XML内容的差异
     */
    @Test
    public void testFileNameVsXmlContent() {
        System.out.println("=== 文件名 vs XML内容对比 ===");
        System.out.println();

        String fileName = "DMC-ZB1-A-00-00-00-00A-007A-A_003-00_zh-CN.xml";
        System.out.println("文件名：" + fileName);
        System.out.println();

        // 从文件名解析
        String fileBaseName = fileName.replace("_003-00_zh-CN.xml", "");
        System.out.println("文件名基础DMC：" + fileBaseName);
        System.out.println("  -> DMC-ZB1-A-00-00-00-00A-007A-A");
        System.out.println("                   ^^");
        System.out.println("                   文件名中systemCode位置是\"00\"");
        System.out.println();

        // 从XML内容
        System.out.println("XML中dmCode属性：");
        System.out.println("  systemCode=\"05\"");
        System.out.println("               ^^");
        System.out.println("               XML中systemCode是\"05\"");
        System.out.println();

        System.out.println("🚨 发现不一致！");
        System.out.println("  - 文件名显示systemCode=\"00\"");
        System.out.println("  - XML内容显示systemCode=\"05\"");
        System.out.println();
        System.out.println("⚠️ 这说明文件名与XML内容不匹配！");
    }

    /**
     * 测试3：生成正确的DMC文件名
     */
    @Test
    public void testGenerateCorrectFileName() {
        System.out.println("=== 生成正确的DMC文件名 ===");
        System.out.println();

        // 从XML生成完整DMC
        Map<String, String> dmCodeAttrs = new HashMap<>();
        dmCodeAttrs.put("modelIdentCode", "ZB1");
        dmCodeAttrs.put("systemDiffCode", "A");
        dmCodeAttrs.put("systemCode", "05");
        dmCodeAttrs.put("subSystemCode", "0");
        dmCodeAttrs.put("subSubSystemCode", "0");
        dmCodeAttrs.put("assyCode", "00");
        dmCodeAttrs.put("disassyCode", "00");
        dmCodeAttrs.put("disassyCodeVariant", "A");
        dmCodeAttrs.put("infoCode", "007");
        dmCodeAttrs.put("infoCodeVariant", "A");
        dmCodeAttrs.put("itemLocationCode", "A");

        // 生成SNS
        String sns = DmcUtils.composeSns(
            dmCodeAttrs.get("modelIdentCode"),
            dmCodeAttrs.get("systemDiffCode"),
            dmCodeAttrs.get("systemCode"),
            dmCodeAttrs.get("subSystemCode"),
            dmCodeAttrs.get("subSubSystemCode"),
            dmCodeAttrs.get("assyCode"),
            dmCodeAttrs.get("disassyCode"),
            dmCodeAttrs.get("disassyCodeVariant")
        );

        // 生成基础DMC
        String baseDmc = "DMC-" + sns + "-" +
                        dmCodeAttrs.get("infoCode") + dmCodeAttrs.get("infoCodeVariant") + "-" +
                        dmCodeAttrs.get("itemLocationCode");

        // 生成完整DMC（假设版本号和语言）
        String fullDmc = baseDmc + "_003-00_zh-CN.xml";

        System.out.println("根据XML内容生成的正确文件名：");
        System.out.println("  SNS: " + sns);
        System.out.println("  基础DMC: " + baseDmc);
        System.out.println("  完整文件名: " + fullDmc);
        System.out.println();
        System.out.println("对比：");
        System.out.println("  实际文件名: DMC-ZB1-A-00-00-00-00A-007A-A_003-00_zh-CN.xml");
        System.out.println("  正确文件名: " + fullDmc);
        System.out.println();

        if (fullDmc.contains("-05-")) {
            System.out.println("✅ 正确文件名包含\"-05-\"（systemCode=05）");
        } else {
            System.out.println("❌ 文件名生成逻辑可能有问题");
        }
    }

    /**
     * 测试4：诊断为什么会出现systemCode不匹配
     */
    @Test
    public void testDiagnoseSystemCodeMismatch() {
        System.out.println("=== systemCode不匹配原因诊断 ===");
        System.out.println();

        System.out.println("可能的原因：");
        System.out.println();

        System.out.println("1️⃣ 文件名生成时使用了错误的systemCode");
        System.out.println("   - 可能是手动命名时输入错误");
        System.out.println("   - 可能是导出功能有bug");
        System.out.println();

        System.out.println("2️⃣ XML内容被手动修改过");
        System.out.println("   - 原本systemCode=\"00\"");
        System.out.println("   - 后来改为systemCode=\"05\"");
        System.out.println("   - 但文件名没有同步更新");
        System.out.println();

        System.out.println("3️⃣ 这是从旧系统导出的文件");
        System.out.println("   - 旧系统的文件名生成逻辑不同");
        System.out.println("   - 导致文件名与XML内容不一致");
        System.out.println();

        System.out.println("✅ 解决方案：");
        System.out.println("   - 修改文件名为：DMC-ZB1-A-05-00-00-00A-007A-A_003-00_zh-CN.xml");
        System.out.println("   - 或者修改XML中systemCode=\"00\"（如果文件名是正确的）");
        System.out.println("   - 或者在构型表中添加SNS=\"ZB1-A-05-00-00-00A\"");
    }
}
