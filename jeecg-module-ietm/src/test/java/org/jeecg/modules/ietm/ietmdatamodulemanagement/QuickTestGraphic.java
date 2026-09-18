package org.jeecg.modules.ietm.ietmdatamodulemanagement;

public class QuickTestGraphic {
    public static void main(String[] args) {
        // 测试XSLT逻辑：reproductionWidth=""
        String widthAttr = "";
        
        // 模拟XSLT的<xsl:when test="@reproductionWidth">
        String wid;
        if (widthAttr != null && !widthAttr.isEmpty()) {
            wid = widthAttr;
            System.out.println("使用reproductionWidth: " + wid);
        } else {
            wid = "auto";
            System.out.println("使用默认值: " + wid);
        }
        
        // XSLT的<xsl:if test="$wid != 'auto'">
        if (!wid.equals("auto")) {
            System.out.println("会生成: width=\"" + wid + "\"");
        } else {
            System.out.println("不会生成width属性");
        }
    }
}
