package org.jeecg.modules.ietm.ietmdmcontent;

import org.junit.jupiter.api.Test;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试description类型DM的目录序号生成
 * 验证xsl:number format="1.1"是否正确生成层级编号
 */
public class DmDescriptionTocNumberTest {

    @Test
    public void testDescriptionTocNumbersGenerated() throws Exception {
        // 构造简化的description类型DM XML
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<dmodule>\n" +
                "  <identAndStatusSection>\n" +
                "    <dmAddress>\n" +
                "      <dmIdent>\n" +
                "        <dmCode modelIdentCode=\"TEST\" systemDiffCode=\"A\" " +
                "systemCode=\"00\" subSystemCode=\"0\" subSubSystemCode=\"0\" " +
                "assyCode=\"00\" disassyCode=\"00\" disassyCodeVariant=\"A\" " +
                "infoCode=\"040\" infoCodeVariant=\"A\" itemLocationCode=\"A\"/>\n" +
                "      </dmIdent>\n" +
                "      <dmAddressItems>\n" +
                "        <dmTitle>\n" +
                "          <techName>测试设备</techName>\n" +
                "          <infoName>描述</infoName>\n" +
                "        </dmTitle>\n" +
                "      </dmAddressItems>\n" +
                "    </dmAddress>\n" +
                "  </identAndStatusSection>\n" +
                "  <content>\n" +
                "    <description>\n" +
                "      <levelledPara>\n" +
                "        <title>概述</title>\n" +
                "        <para>第一章内容</para>\n" +
                "        <levelledPara>\n" +
                "          <title>基本信息</title>\n" +
                "          <para>第一节内容</para>\n" +
                "        </levelledPara>\n" +
                "        <levelledPara>\n" +
                "          <title>技术参数</title>\n" +
                "          <para>第二节内容</para>\n" +
                "        </levelledPara>\n" +
                "      </levelledPara>\n" +
                "      <levelledPara>\n" +
                "        <title>详细说明</title>\n" +
                "        <para>第二章内容</para>\n" +
                "      </levelledPara>\n" +
                "    </description>\n" +
                "  </content>\n" +
                "</dmodule>";

        // 加载XSLT
        InputStream xslStream = getClass().getClassLoader()
                .getResourceAsStream("ietm/S1000D40/xsl/base.xsl");
        assertNotNull(xslStream, "base.xsl应该存在");

        TransformerFactory factory = TransformerFactory.newInstance();
        StreamSource xslSource = new StreamSource(xslStream);
        Transformer transformer = factory.newTransformer(xslSource);

        // 设置参数
        transformer.setParameter("DMFileName", "TEST.xml");
        transformer.setParameter("Publication", "TEST-PUB");

        // 执行转换
        StreamSource xmlSource = new StreamSource(new StringReader(xml));
        StringWriter writer = new StringWriter();
        transformer.transform(xmlSource, new StreamResult(writer));

        String html = writer.toString();

        System.out.println("========================================");
        System.out.println("生成的HTML长度: " + html.length() + " 字符");
        System.out.println("========================================");

        // 验证：应该包含目录
        assertTrue(html.contains("正文目录"), "应该包含正文目录标题");

        // 提取目录部分
        int tocStart = html.indexOf("正文目录");
        int tocEnd = html.indexOf("</table>", tocStart);
        if (tocStart > 0 && tocEnd > tocStart) {
            String tocSection = html.substring(tocStart, tocEnd + 8);
            System.out.println("\n目录HTML片段：");
            System.out.println("========================================");
            System.out.println(tocSection.substring(0, Math.min(1500, tocSection.length())));
            System.out.println("========================================\n");
        }

        // 提取所有序号单元格
        Pattern pattern = Pattern.compile("<td class=\"loclefttd\">(.*?)</td>", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(html);

        System.out.println("提取的序号单元格内容：");
        System.out.println("========================================");

        int count = 0;
        while (matcher.find()) {
            count++;
            String cellContent = matcher.group(1).trim();
            System.out.println("序号 #" + count + ": [" + cellContent + "]");

            // 提取纯文本（去除HTML标签）
            String textOnly = cellContent.replaceAll("<[^>]+>", "").trim();
            System.out.println("  纯文本: [" + textOnly + "]");
            System.out.println("");
        }

        System.out.println("========================================");
        System.out.println("共找到 " + count + " 个序号单元格");
        System.out.println("========================================");

        // 验证：应该至少有序号单元格
        assertTrue(count > 0, "应该至少有1个序号单元格");

        // 验证：序号不应该全是空的
        Pattern numberPattern = Pattern.compile("<td class=\"loclefttd\">.*?([0-9.]+).*?</td>", Pattern.DOTALL);
        Matcher numberMatcher = numberPattern.matcher(html);
        int numbersFound = 0;
        while (numberMatcher.find()) {
            numbersFound++;
            System.out.println("找到序号: " + numberMatcher.group(1));
        }

        System.out.println("\n实际包含数字的序号单元格: " + numbersFound);

        assertTrue(numbersFound > 0, "至少应该有一个序号单元格包含数字");
    }
}
