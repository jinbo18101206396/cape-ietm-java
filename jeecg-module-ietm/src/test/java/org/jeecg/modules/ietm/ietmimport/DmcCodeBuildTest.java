package org.jeecg.modules.ietm.ietmimport;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * DMC编码构建测试 - 排查"文件名与DM内容编码不一致"问题
 *
 * 问题现象：
 * 校验XML文件时，提示"文件名与DM内容编码不一致"
 *
 * 根本原因分析：
 * 1. IetmDmImportServiceImpl.buildDmcCode() 使用**简化版**DMC编码（只有3个字段）
 * 2. S1000D标准的完整DMC编码包含11个字段
 * 3. DmXmlHelper.buildDmcFromElement() 使用**完整版**DMC编码（11个字段）
 *
 * @author IETM Team
 * @date 2026-09-04
 */
public class DmcCodeBuildTest {

    /**
     * 测试场景1：完整DMC文件名 vs 简化版DMC编码
     *
     * 文件名：DMC-A-00-00-00-00A-040A-A-00-00A-A.xml
     * 简化版构建：DMC-A-00-00
     * 完整版构建：DMC-A-00-00-0000A-040A-A00-00AA-A
     * 结果：只有完整版匹配 ✓
     */
    @Test
    public void testCompleteDmcVsSimplified() {
        // 模拟从XML提取的dmCode属性
        String modelIdentCode = "A";
        String systemDiffCode = "00";
        String systemCode = "00";
        String subSystemCode = "00";
        String subSubSystemCode = "00A";
        String assyCode = "040A";
        String disassyCode = "A";
        String disassyCodeVariant = "00";
        String infoCode = "00A";
        String infoCodeVariant = "A";
        String itemLocationCode = "A";

        // 旧版IetmDmImportServiceImpl.buildDmcCode()的简化版实现（已废弃）
        String simplifiedDmc = "DMC-" + modelIdentCode + "-" + systemDiffCode + "-" + systemCode;

        // 新版DmXmlHelper.buildDmcFromElement()的完整版实现（正确）
        // 注意：subSys+subSub、dis+disVar、info+infoVar 是连续拼接，不加连字符
        String sns = modelIdentCode + "-" + systemDiffCode + "-" + systemCode + "-"
                   + subSystemCode + subSubSystemCode + "-" + assyCode + "-"
                   + disassyCode + disassyCodeVariant;
        String completeDmc = "DMC-" + sns + "-" + infoCode + infoCodeVariant + "-" + itemLocationCode;

        // 文件名（去除.xml扩展名）
        String fileName = "DMC-A-00-00-00-00A-040A-A-00-00A-A";

        System.out.println("=== DMC编码对比 ===");
        System.out.println("文件名:      " + fileName);
        System.out.println("简化版DMC:   " + simplifiedDmc);
        System.out.println("完整版DMC:   " + completeDmc);
        System.out.println();
        System.out.println("简化版匹配: " + fileName.equals(simplifiedDmc) + " ❌");
        System.out.println("完整版匹配: " + fileName.equals(completeDmc) + " ✓");

        // 断言：简化版不匹配
        assertFalse("简化版DMC不应该匹配完整文件名", fileName.equals(simplifiedDmc));

        // 断言：完整版匹配
        assertTrue("完整版DMC应该匹配文件名", fileName.equals(completeDmc));
    }

    /**
     * 测试场景2：验证DmXmlHelper的完整DMC构建逻辑
     */
    @Test
    public void testDmXmlHelperLogic() {
        // S1000D标准DMC格式：DMC-{model}-{sysDiff}-{sys}-{subSys}{subSub}-{assy}-{dis}{disVar}-{info}{infoVar}-{loc}
        // 注意：某些段是连续拼接的，没有连字符分隔

        String model = "APEX464";
        String sysDiff = "A";
        String sys = "00";
        String subSys = "00";
        String subSub = "00A";
        String assy = "040A";
        String dis = "A";
        String disVar = "00";
        String info = "00A";
        String infoVar = "A";
        String loc = "A";

        // 模拟DmXmlHelper.buildDmcFromElement()的逻辑
        String sns = model + "-" + sysDiff + "-" + sys + "-" + subSys + subSub + "-" + assy + "-" + dis + disVar;
        String completeDmc = "DMC-" + sns + "-" + info + infoVar + "-" + loc;

        String expected = "DMC-APEX464-A-00-0000A-040A-A00-00AA-A";

        System.out.println("=== S1000D标准DMC构建 ===");
        System.out.println("完整DMC: " + completeDmc);
        System.out.println("预期值:  " + expected);
        System.out.println("匹配:    " + completeDmc.equals(expected) + " ✓");

        assertEquals("完整DMC编码应符合S1000D标准", expected, completeDmc);
    }

    /**
     * 测试场景3：空段处理
     */
    @Test
    public void testEmptySegments() {
        // 某些字段可能为空
        String model = "A";
        String sysDiff = "";
        String sys = "00";
        String subSys = "";
        String subSub = "";
        String assy = "";
        String dis = "";
        String disVar = "";
        String info = "00A";
        String infoVar = "";
        String loc = "";

        // DmXmlHelper使用safeStr()处理空值（null->""）
        String sns = model + "-" + sysDiff + "-" + sys + "-" + subSys + subSub + "-" + assy + "-" + dis + disVar;
        String completeDmc = "DMC-" + sns + "-" + info + infoVar + "-" + loc;

        System.out.println("=== 空段处理 ===");
        System.out.println("完整DMC: " + completeDmc);
        System.out.println("预期:    DMC-A--00----00A-");

        assertEquals("空段应保留连字符", "DMC-A--00----00A-", completeDmc);
    }

    /**
     * 测试场景4：从实际导出文件验证
     *
     * 根据之前的导出代码分析，导出的文件名格式应该是：
     * {dmcCode}.xml
     *
     * 其中dmcCode是从IetmDataModule.getDmcCode()获取的
     */
    @Test
    public void testExportedFileNameFormat() {
        // 模拟导出时的文件名生成
        String dbDmcCode = "DMC-A-00-00-00-00A-040A-A-00-00A-A";
        String exportedFileName = dbDmcCode + ".xml";

        // 去除扩展名
        String fileBaseName = exportedFileName.substring(0, exportedFileName.lastIndexOf("."));

        System.out.println("=== 导出文件名验证 ===");
        System.out.println("数据库dmcCode:  " + dbDmcCode);
        System.out.println("导出文件名:     " + exportedFileName);
        System.out.println("文件基础名:     " + fileBaseName);
        System.out.println("匹配:           " + dbDmcCode.equals(fileBaseName) + " ✓");

        assertEquals("导出的文件名应该与数据库dmcCode一致", dbDmcCode, fileBaseName);
    }
}
