package org.jeecg.modules.ietm.ietmdmcontent;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试description类型DM的目录序号生成
 */
public class DmTocNumberGenerationTest {

    @Test
    public void testDescriptionTocNumberGeneration() throws Exception {
        // 模拟description类型DM的简化XML
        String descriptionXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<dmodule xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xsi:noNamespaceSchemaLocation=\"descript.xsd\">\n" +
                "  <identAndStatusSection>\n" +
                "    <dmAddress>\n" +
                "      <dmIdent><dmCode modelIdentCode=\"TEST\" systemDiffCode=\"A\" " +
                "systemCode=\"00\" subSystemCode=\"0\" subSubSystemCode=\"0\" " +
                "assyCode=\"00\" disassyCode=\"00\" disassyCodeVariant=\"A\" " +
                "infoCode=\"040\" infoCodeVariant=\"A\" itemLocationCode=\"A\"/></dmIdent>\n" +
                "      <dmAddressItems>\n" +
                "        <dmTitle><techName>测试设备</techName><infoName>描述</infoName></dmTitle>\n" +
                "      </dmAddressItems>\n" +
                "    </dmAddress>\n" +
                "  </identAndStatusSection>\n" +
                "  <content>\n" +
                "    <description>\n" +
                "      <levelledPara>\n" +
                "        <title>第一章</title>\n" +
                "        <para>内容1</para>\n" +
                "        <levelledPara>\n" +
                "          <title>第一节</title>\n" +
                "          <para>内容1.1</para>\n" +
                "        </levelledPara>\n" +
                "        <levelledPara>\n" +
                "          <title>第二节</title>\n" +
                "          <para>内容1.2</para>\n" +
                "        </levelledPara>\n" +
                "      </levelledPara>\n" +
                "      <levelledPara>\n" +
                "        <title>第二章</title>\n" +
                "        <para>内容2</para>\n" +
                "      </levelledPara>\n" +
                "    </description>\n" +
                "  </content>\n" +
                "</dmodule>";

        // 加载XSLT
        TransformerFactory factory = TransformerFactory.newInstance();
        StreamSource xslSource = new StreamSource(
                new ClassPathResource("ietm/S1000D40/xsl/base.xsl").getInputStream()
        );
        Transformer transformer = factory.newTransformer(xslSource);

        // 设置参数
        transformer.setParameter("DMFileName", "TEST.xml");
        transformer.setParameter("Publication", "TEST-PUB");

        // 执行转换
        StreamSource xmlSource = new StreamSource(new StringReader(descriptionXml));
        StringWriter resultWriter = new StringWriter();
        transformer.transform(xmlSource, new StreamResult(resultWriter));

        String html = resultWriter.toString();

        // 调试输出
        System.out.println("========================================");
        System.out.println("生成的HTML片段（目录部分）：");
        System.out.println("========================================");

        // 提取目录部分
        int locStart = html.indexOf("正文目录");
        if (locStart > 0) {
            String tocSection = html.substring(locStart, Math.min(locStart + 2000, html.length()));
            System.out.println(tocSection);
        } else {
            System.out.println("⚠️ 未找到'正文目录'");
        }

        // 验证：应该包含目录表格
        assertTrue(html.contains("正文目录"), "应该包含正文目录标题");
        assertTrue(html.contains("class=\"loclefttd\""), "应该包含序号单元格");
        assertTrue(html.contains("class=\"locrighttd\""), "应该包含标题单元格");

        // 验证：序号单元格内应该有内容
        // 提取所有 loclefttd 单元格
        int count = 0;
        int pos = 0;
        while ((pos = html.indexOf("class=\"loclefttd\"", pos)) != -1) {
            count++;
            int endPos = html.indexOf("</td>", pos);
            String cell = html.substring(pos, endPos);
            System.out.println("序号单元格 #" + count + ": " + cell);
            pos = endPos;
        }

        System.out.println("========================================");
        System.out.println("共找到 " + count + " 个序号单元格");
        System.out.println("========================================");

        assertTrue(count > 0, "应该至少有一个序号单元格");
    }
}
