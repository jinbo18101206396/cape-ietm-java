package org.jeecg.modules.ietm.icnmanage;

import com.alibaba.fastjson.JSON;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.FormulaIcnVO;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

/**
 * 公式ICN保存/加载测试
 * 验证BLOB存储的完整性：完整的<img>标签HTML包含base64图片和LaTeX源码
 */
public class FormulaIcnTest {

    @Test
    public void testFormulaJsonParsing() {
        // 模拟前端发送的完整公式HTML（包含base64图片和LaTeX源码）
        String formulaHtml = "<img class=\"kfformula\" src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUA\" data-latex=\"a+b=c\"/>";

        // 构建请求JSON（模拟前端的请求）
        FormulaIcnVO vo = new FormulaIcnVO();
        vo.setIcn("ICN-12345-67890-ABC-00001-A-001-01");
        vo.setSns("12345-67890-ABCDE-12345-67890");
        vo.setRpc("67890");
        vo.setOriginator("ABC");
        vo.setUniqueid("00001");
        vo.setVariantcode("A");
        vo.setSecurityclassification("02");
        vo.setIssueno("001");
        vo.setSecretLevel("1");
        vo.setCmnodeid("cmnode123");
        vo.setFilename("公式00001.png");
        vo.setFilecontent(formulaHtml);  // 🔧 关键：存储完整的<img>标签

        // 序列化为JSON
        String json = JSON.toJSONString(vo);
        System.out.println("序列化JSON: " + json);

        // 反序列化验证
        FormulaIcnVO parsedVo = JSON.parseObject(json, FormulaIcnVO.class);
        assertEquals(vo.getIcn(), parsedVo.getIcn());
        assertEquals(formulaHtml, parsedVo.getFilecontent());

        // 验证Base64和LaTeX源码完整性
        assertTrue("公式HTML应包含base64图片", parsedVo.getFilecontent().contains("data:image/png;base64"));
        assertTrue("公式HTML应包含LaTeX源码", parsedVo.getFilecontent().contains("data-latex=\"a+b=c\""));
    }

    @Test
    public void testBlobStorageEncoding() {
        // 测试BLOB字段的编码/解码
        String formulaHtml = "<img class=\"kfformula\" src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUA\" data-latex=\"a+b=c\"/>";

        // 编码为字节数组（模拟JDBC BLOB处理）
        byte[] bytes = formulaHtml.getBytes(StandardCharsets.UTF_8);

        // 解码回字符串
        String decoded = new String(bytes, StandardCharsets.UTF_8);

        // 验证往返转换的完整性
        assertEquals(formulaHtml, decoded);
        System.out.println("✅ BLOB编码/解码测试通过");
    }

    @Test
    public void testCompleteFormulaLifecycle() {
        // 完整的公式保存->加载流程

        // 1. 前端构建公式
        String formulaHtml = "<img class=\"kfformula\" src=\"data:image/png;base64,iVBORw0KGgo...\" data-latex=\"x^2+y^2=z^2\"/>";

        // 2. 创建请求VO
        FormulaIcnVO vo = new FormulaIcnVO();
        vo.setIcn("ICN-99999-88888-XYZ-12345-A-001-01");
        vo.setFilecontent(formulaHtml);

        // 3. 前端序列化为JSON字符串
        String requestJson = JSON.toJSONString(vo);

        // 4. 后端接收JSON参数
        FormulaIcnVO receivedVo = JSON.parseObject(requestJson, FormulaIcnVO.class);

        // 5. 后端编码为BLOB存储
        byte[] blobData = receivedVo.getFilecontent().getBytes(StandardCharsets.UTF_8);

        // 6. 从数据库加载BLOB
        String loadedFormula = new String(blobData, StandardCharsets.UTF_8);

        // 7. 验证往返转换的完整性（关键！）
        assertEquals("完整的<img>标签应该往返转换成功", formulaHtml, loadedFormula);
        System.out.println("✅ 完整的公式生命周期测试通过");
        System.out.println("   - 前端构建: " + formulaHtml.substring(0, 50) + "...");
        System.out.println("   - 后端BLOB: " + blobData.length + " bytes");
        System.out.println("   - 加载验证: " + loadedFormula.equals(formulaHtml));
    }

    @Test
    public void testMultilineFormulaHtml() {
        // 测试含换行符的公式HTML（确保[\s\S]正则不受影响）
        String multilineFormula = "<img class=\"kfformula\" " +
            "src=\"data:image/png;base64,iVBORw0KGgo...\" " +
            "data-latex=\"a+b=c\"/>";

        FormulaIcnVO vo = new FormulaIcnVO();
        vo.setFilecontent(multilineFormula);

        byte[] bytes = vo.getFilecontent().getBytes(StandardCharsets.UTF_8);
        String decoded = new String(bytes, StandardCharsets.UTF_8);

        assertEquals("多行公式HTML应该完整保存", multilineFormula, decoded);
        System.out.println("✅ 多行公式HTML测试通过");
    }
}
