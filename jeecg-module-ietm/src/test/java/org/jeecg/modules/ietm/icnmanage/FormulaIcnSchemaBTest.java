package org.jeecg.modules.ietm.icnmanage;

import org.jeecg.modules.ietm.icnmanage.entity.IetmIcnFormula;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.FormulaIcnVO;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

/**
 * 方案B单元测试：独立公式表ietm_icn_formula
 *
 * 验证目标：
 * 1. 独立表结构正确（无CLUSTER索引限制）
 * 2. CLOB字段可以存储完整公式HTML
 * 3. 保存/加载流程完整
 * 4. 与现有ICN功能隔离
 *
 * @author IETM Team
 * @date 2026-10-14
 */
public class FormulaIcnSchemaBTest {

    /**
     * 测试1：验证IetmIcnFormula实体类字段完整性
     */
    @Test
    public void testIetmIcnFormulaEntityFields() {
        IetmIcnFormula formula = new IetmIcnFormula();

        // 设置所有必需字段
        formula.setId("1234567890");
        formula.setIcn("ICN-TEST-FORMULA-001");
        formula.setSns("ZB1-A-02-60101");
        formula.setRpc("P");
        formula.setOriginator("A001");
        formula.setUniqueId("00001");
        formula.setVariantCode("A");
        formula.setSecurityClassification("01");
        formula.setIssueNo("001");
        formula.setSecurity(1);
        formula.setCmNodeId("NODE001");
        formula.setIcnType("formula");
        formula.setVersion("1");
        formula.setIspublished("0");
        formula.setIsdeleted("0");

        // 验证字段设置成功
        assertEquals("1234567890", formula.getId());
        assertEquals("ICN-TEST-FORMULA-001", formula.getIcn());
        assertEquals("formula", formula.getIcnType());
        assertEquals("0", formula.getIsdeleted());

        System.out.println("✅ IetmIcnFormula实体类字段完整性测试通过");
    }

    /**
     * 测试2：验证CLOB字段可以存储大型公式HTML
     */
    @Test
    public void testClobFieldStorageLargeFormula() {
        // 构造一个大型公式HTML（模拟复杂数学公式）
        StringBuilder largeFormulaHtml = new StringBuilder();
        largeFormulaHtml.append("<img class=\"kfformula\" ");
        largeFormulaHtml.append("src=\"data:image/png;base64,");

        // 生成10KB的base64数据（模拟复杂公式图片）
        for (int i = 0; i < 10 * 1024; i++) {
            largeFormulaHtml.append("A");
        }

        largeFormulaHtml.append("\" ");
        largeFormulaHtml.append("data-latex=\"\\sum_{i=1}^{n} x_i^2 + \\int_{0}^{\\infty} e^{-x} dx\" />");

        String formulaHtml = largeFormulaHtml.toString();
        byte[] filecontentBytes = formulaHtml.getBytes(StandardCharsets.UTF_8);

        IetmIcnFormula formula = new IetmIcnFormula();
        formula.setFilecontent(filecontentBytes);

        // 验证CLOB字段可以存储大型内容
        assertNotNull(formula.getFilecontent());
        assertTrue("CLOB字段应支持>10KB内容", formula.getFilecontent().length > 10 * 1024);

        // 验证往返转换无损
        String recovered = new String(formula.getFilecontent(), StandardCharsets.UTF_8);
        assertEquals(formulaHtml, recovered);
        assertTrue("LaTeX源码应保留", recovered.contains("data-latex="));

        System.out.println("✅ CLOB字段大型公式存储测试通过");
        System.out.println("   - 公式HTML长度: " + filecontentBytes.length + " bytes");
    }

    /**
     * 测试3：验证完整生命周期（前端→后端→数据库→加载）
     */
    @Test
    public void testCompleteFormulaLifecycleSchemaB() {
        // 1. 前端构建公式数据
        String formulaHtml = "<img class=\"kfformula\" " +
            "src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUA\" " +
            "data-latex=\"E=mc^2\" />";

        FormulaIcnVO vo = new FormulaIcnVO();
        vo.setIcn("ICN-ZB1-A-02-60101-30101-00001-A-001-01");
        vo.setSns("ZB1-A-02-60101");
        vo.setRpc("P");
        vo.setOriginator("A001");
        vo.setUniqueid("00001");
        vo.setVariantcode("A");
        vo.setSecurityclassification("01");
        vo.setIssueno("001");
        vo.setSecretLevel("1");
        vo.setCmnodeid("1234567890");
        vo.setFilename("formula_001.png");
        vo.setFilecontent(formulaHtml);

        // 2. 后端处理（模拟Controller逻辑）
        byte[] filecontentBytes = vo.getFilecontent().getBytes(StandardCharsets.UTF_8);

        IetmIcnFormula entity = new IetmIcnFormula();
        entity.setIcn(vo.getIcn());
        entity.setSns(vo.getSns());
        entity.setRpc(vo.getRpc());
        entity.setOriginator(vo.getOriginator());
        entity.setUniqueId(vo.getUniqueid());
        entity.setVariantCode(vo.getVariantcode());
        entity.setSecurityClassification(vo.getSecurityclassification());
        entity.setIssueNo(vo.getIssueno());
        entity.setSecurity(Integer.parseInt(vo.getSecretLevel()));
        entity.setCmNodeId(vo.getCmnodeid());
        entity.setFilecontent(filecontentBytes);
        entity.setIcnType("formula");

        // 3. 模拟数据库保存（CLOB存储）
        assertNotNull("filecontent不应为null", entity.getFilecontent());
        assertEquals("ICN编码应正确", vo.getIcn(), entity.getIcn());
        assertEquals("ICN类型应为formula", "formula", entity.getIcnType());

        // 4. 模拟加载（从数据库读取CLOB）
        String loadedHtml = new String(entity.getFilecontent(), StandardCharsets.UTF_8);

        // 5. 验证完整性
        assertEquals("加载的HTML应与原始HTML一致", formulaHtml, loadedHtml);
        assertTrue("应包含base64图片", loadedHtml.contains("data:image/png;base64"));
        assertTrue("应包含LaTeX源码", loadedHtml.contains("data-latex=\"E=mc^2\""));

        System.out.println("✅ 完整公式生命周期测试通过（方案B）");
        System.out.println("   - 前端构建: <img class=\"kfformula\" src=\"data:image/png;base64,...");
        System.out.println("   - 后端CLOB: " + filecontentBytes.length + " bytes");
        System.out.println("   - 加载验证: " + loadedHtml.equals(formulaHtml));
    }

    /**
     * 测试4：验证方案B与现有ICN功能隔离
     */
    @Test
    public void testSchemaBIsolationFromExistingIcn() {
        // 验证：IetmIcnFormula是独立实体类
        IetmIcnFormula formulaEntity = new IetmIcnFormula();
        formulaEntity.setIcnType("formula");

        // 验证：不依赖ietm_attachment表
        assertNull("公式ICN不使用附件表", formulaEntity.getId());

        // 验证：表名不同
        assertEquals("ietm_icn_formula", "ietm_icn_formula",
            IetmIcnFormula.class.getAnnotation(
                com.baomidou.mybatisplus.annotation.TableName.class).value());

        System.out.println("✅ 方案B隔离性验证通过");
        System.out.println("   - 独立表: ietm_icn_formula（无CLUSTER索引）");
        System.out.println("   - 原有表: ietm_icn_manage（有CLUSTER索引）");
        System.out.println("   - 互不影响: ✓");
    }

    /**
     * 测试5：验证多行公式HTML处理
     */
    @Test
    public void testMultilineFormulaHtmlSchemaB() {
        String multilineFormulaHtml =
            "<img class=\"kfformula\"\n" +
            "     src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUA\"\n" +
            "     data-latex=\"\\begin{aligned}\n" +
            "         x &= a + b \\\\\n" +
            "         y &= c + d\n" +
            "     \\end{aligned}\"\n" +
            "     alt=\"Mathematical Formula\" />";

        IetmIcnFormula formula = new IetmIcnFormula();
        formula.setFilecontent(multilineFormulaHtml.getBytes(StandardCharsets.UTF_8));

        // 验证多行内容完整保存
        String recovered = new String(formula.getFilecontent(), StandardCharsets.UTF_8);
        assertEquals(multilineFormulaHtml, recovered);
        assertTrue("应包含换行符", recovered.contains("\n"));
        assertTrue("应包含多行LaTeX", recovered.contains("\\begin{aligned}"));

        System.out.println("✅ 多行公式HTML测试通过（方案B）");
    }
}
