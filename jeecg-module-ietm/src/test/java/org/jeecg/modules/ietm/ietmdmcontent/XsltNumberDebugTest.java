package org.jeecg.modules.ietm.ietmdmcontent;

import org.junit.jupiter.api.Test;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;

/**
 * 调试xsl:number问题 - 使用简化的XSLT直接测试
 */
public class XsltNumberDebugTest {

    @Test
    public void testXslNumberWithSimpleXslt() throws Exception {
        // 简化的XML
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<dmodule>\n" +
                "  <content>\n" +
                "    <description>\n" +
                "      <levelledPara>\n" +
                "        <title>第一章</title>\n" +
                "        <levelledPara>\n" +
                "          <title>第一节</title>\n" +
                "        </levelledPara>\n" +
                "      </levelledPara>\n" +
                "      <levelledPara>\n" +
                "        <title>第二章</title>\n" +
                "      </levelledPara>\n" +
                "    </description>\n" +
                "  </content>\n" +
                "</dmodule>";

        // 简化的XSLT - 直接测试xsl:number
        String xslt = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n" +
                "  <xsl:output method=\"html\" encoding=\"UTF-8\"/>\n" +
                "  \n" +
                "  <xsl:template match=\"/\">\n" +
                "    <html>\n" +
                "      <body>\n" +
                "        <h1>测试xsl:number</h1>\n" +
                "        <xsl:apply-templates select=\"//content\"/>\n" +
                "      </body>\n" +
                "    </html>\n" +
                "  </xsl:template>\n" +
                "  \n" +
                "  <xsl:template match=\"content\">\n" +
                "    <table border=\"1\">\n" +
                "      <xsl:for-each select=\".//levelledPara\">\n" +
                "        <tr>\n" +
                "          <td>\n" +
                "            序号: <xsl:number count=\"levelledPara\" from=\"content\" level=\"multiple\" format=\"1.1.1.1.1\"/>\n" +
                "          </td>\n" +
                "          <td>\n" +
                "            标题: <xsl:value-of select=\"./title\"/>\n" +
                "          </td>\n" +
                "        </tr>\n" +
                "      </xsl:for-each>\n" +
                "    </table>\n" +
                "  </xsl:template>\n" +
                "</xsl:stylesheet>";

        // 执行转换
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer(
                new StreamSource(new StringReader(xslt))
        );

        StreamSource xmlSource = new StreamSource(new StringReader(xml));
        StringWriter writer = new StringWriter();
        transformer.transform(xmlSource, new StreamResult(writer));

        String result = writer.toString();

        System.out.println("========================================");
        System.out.println("转换结果:");
        System.out.println("========================================");
        System.out.println(result);
        System.out.println("========================================");

        // 检查是否包含序号
        boolean hasNumber1 = result.contains("序号: 1");
        boolean hasNumber11 = result.contains("序号: 1.1");
        boolean hasNumber2 = result.contains("序号: 2");

        System.out.println("\n结果分析:");
        System.out.println("包含 '序号: 1': " + hasNumber1);
        System.out.println("包含 '序号: 1.1': " + hasNumber11);
        System.out.println("包含 '序号: 2': " + hasNumber2);

        if (!hasNumber1 && !hasNumber11 && !hasNumber2) {
            System.out.println("\n⚠️ 警告: xsl:number没有生成任何序号!");
        }
    }
}
