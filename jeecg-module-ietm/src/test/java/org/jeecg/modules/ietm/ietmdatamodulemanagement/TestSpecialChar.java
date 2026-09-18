package org.jeecg.modules.ietm.ietmdatamodulemanagement;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.util.DmXsltTransformer;

public class TestSpecialChar {
    public static void main(String[] args) throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<dmodule>\n" +
            "<content><description><para>\n" +
            "<graphic reproductionWidth=\"100&amp;200\" infoEntityIdent=\"ICN-TEST-013\"/>\n" +
            "</para></description></content>\n" +
            "</dmodule>";
        
        String html = DmXsltTransformer.transform(xml, "S1000D40", "descript");
        
        System.out.println("=== HTML Output (width相关部分) ===");
        int widthIdx = html.indexOf("width=");
        if (widthIdx > 0) {
            String snippet = html.substring(widthIdx, Math.min(widthIdx + 50, html.length()));
            System.out.println(snippet);
        } else {
            System.out.println("未找到width属性");
        }
        
        System.out.println("\n=== 检查 ===");
        System.out.println("Contains 'width=\"100&200\"': " + html.contains("width=\"100&200\""));
        System.out.println("Contains 'width=\"100&amp;200\"': " + html.contains("width=\"100&amp;200\""));
        System.out.println("Contains 'width=\"100&amp;amp;200\"': " + html.contains("width=\"100&amp;amp;200\""));
    }
}
