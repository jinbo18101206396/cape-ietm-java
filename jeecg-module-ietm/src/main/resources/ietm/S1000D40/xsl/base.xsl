<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xlink="http://www.w3.org/1999/xlink"
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    xmlns:dc="http://www.purl.org/dc/elements/1.1/"
    >
<!--2012xmlns:msxsl="urn:schemas-microsoft-com:xslt" exclude-result-prefixes="msxsl"
    xmlns:pl="http://www.polarlight.net/xslt/example"xmlns:msxsl="urn:schemas-microsoft-com:xslt" exclude-result-prefixes="msxsl"
    xmlns:pl="http://www.polarlight.net/xslt/example"
  <msxsl:script language="C#" implements-prefix="pl">
    <msxsl:assembly name="AssemblyJava"/>
    <msxsl:using namespace="AssemblyJava"/>
    <![CDATA[ 
    public static int delimitedSubstringCounter(String source, String delimiter)
        {
            return AssemblyTest.delimitedSubstringCounter(source, delimiter);
        }
        public static Boolean containsDelimited(String source, String target, String delimiter)
        {
            return  AssemblyTest.containsDelimited(String source, String target, String delimiter);
        }
]]>
  </msxsl:script> -->
    <!--===============================================-->
    <xsl:include href="rdf.xsl"/>
    <!--===============================================-->
    <xsl:include href="dmc.xsl"/>
    <!--===============================================-->
    <xsl:include href="pmc.xsl"/>
    <!--===============================================-->
    <xsl:include href="status.xsl"/>
    <!--===============================================-->
    <xsl:include href="globalParams.xsl"/>
    <!--===============================================-->
    <xsl:include href="languageVariables.xsl"/>

<!--===============================================-->
	<xsl:include href="symbols.xsl"/>
	<!--===============================================-->
     <xsl:template name="getWCNVisability">
        <xsl:element name="script"> showWCN('<xsl:value-of select="$DMFileName"/>') </xsl:element>
    </xsl:template>
    <!--=======入口，处理dmodule根节点========================================-->
    <xsl:template match="dmodule" name="t_root">
        <!--
            修改说明（2026-11-15）：
            原始设计：wcnDiv/dmview 默认 display:none，依赖外部 JS 函数 JumpToRow()
            恢复显示，但该 JS 库仅存在于 PTC Arbortext JSP 框架中，Saxon-HE 生成
            的独立 HTML（Blob URL）无法加载，导致有安全条件的 DM 预览完全空白。
            同时，原 wcnDiv 安全弹窗逻辑已废弃：
              - prelreq.xsl 的 reqSafety 模板已在内容区"安全条件"标题下完整渲染警告
              - 保留 wcnDiv 会导致警告重复显示且位置在"状况"模块之上
            修改方式：
              1. 移除 $numberOfWarningsCautions 计数变量（不再需要）
              2. 移除 #wcnDiv 块（废弃的安全弹窗，警告已由 prelreq.xsl 在内容区渲染）
              3. #dmview 始终可见，不依赖 JS 控制
              4. 移除调用未定义函数的 <script> 块
        -->

        <!-- 主内容区：始终可见，顺序：状况 → 内容（含安全条件） -->
        <div class="dmview" id="dmview">
            <xsl:call-template name="t_logo"/>
            <!-- 🔧 2026-09-17修复：先处理identAndStatusSection，再处理其他节点 -->
            <xsl:apply-templates select="identAndStatusSection"/>
            <xsl:apply-templates select="*[not(self::identAndStatusSection)]"/>
            <!-- Do not show the End of data module text when is is a fault DM. -->
            <xsl:if test="not(//isoproc) and not (processFragment)">
                <div class="dmEnd">
                    <!--<xsl:value-of select="$v00297"/>-->
					&#160;               
					 </div>
            </xsl:if>
        </div>
        <!-- 移除原始调用未定义 JS 函数的 script 块（JumpToRow/setContentHolderHeight 等为
             Arbortext JSP 框架专有函数，在独立预览 HTML 中不存在，调用会静默失败） -->
    </xsl:template>
    <!--========处理DM数据包idstatus|identAndStatusSection节点=======================================-->
    <xsl:template match="idstatus|identAndStatusSection">
        <div class="hidesection" id="idstatus">
            <div class="statusHeader">
                <span class="statusTitle"><xsl:value-of select="$v00877"/></span>
            </div>
            <div class="statusContent">
                <table class="statusTable">
                    <xsl:apply-templates/>
                </table>
            </div>
        </div>
    </xsl:template>
    <!--==========处理idstatus/dmaddres/dmc（不处理）=====================================-->
    <xsl:template match="idstatus/dmaddres/dmc">
        <!--  do not output the DMC when are in the dmaddres section. -->
    </xsl:template>
    <!--==========如果包含Logo则处理Logo=====================================-->
    <xsl:template name="t_logo">
    	<xsl:if test="/dmodule/identAndStatusSection/dmStatus/logo">
    		<xsl:apply-templates select="/dmodule/identAndStatusSection/dmStatus/logo/symbol" />
    	</xsl:if>
    </xsl:template>
</xsl:stylesheet>
