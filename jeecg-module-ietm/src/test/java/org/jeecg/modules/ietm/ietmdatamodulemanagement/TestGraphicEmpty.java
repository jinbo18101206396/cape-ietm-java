package org.jeecg.modules.ietm.ietmdatamodulemanagement;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.util.DmXsltTransformer;

public class TestGraphicEmpty {
    public static void main(String[] args) throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<dmodule>\n" +
            "<content><description><para>\n" +
            "<graphic reproductionWidth=\"\" infoEntityIdent=\"ICN-TEST-011\"/>\n" +
            "</para></description></content>\n" +
            "</dmodule>";
        
        String html = DmXsltTransformer.transform(xml, "S1000D40", "descript");
        
        System.out.println("=== HTML Output ===");
        System.out.println(html);
        System.out.println("\n=== Checks ===");
        System.out.println("Contains <img: " + html.contains("<img"));
        System.out.println("Contains width=\"\": " + html.contains("width=\"\""));
        System.out.println("Contains max-width: " + html.contains("max-width"));
        System.out.println("Contains style=: " + html.contains("style="));
    }
}
