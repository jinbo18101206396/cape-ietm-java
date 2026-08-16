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
        <!-- Set the total number of Warnings and Cautions so we can set through them. -->
        <xsl:variable name="numberOfWarningsCautions">
          <xsl:value-of
                select="count(//safety//warning | //safety//caution | //reqSafety//warning | // reqSafety//caution )"
            />
        </xsl:variable>

        <xsl:element name="script"> numberOfWarningsCautions =<xsl:value-of
                select="$numberOfWarningsCautions"/>; initFigureBrowser(); clearLinks(); var
            singleDm = <xsl:value-of select="$SingleDMView"/>; 
        </xsl:element>

        <div id="wcnDiv" class="dmview" style="display: none">
            <script> if (singleDm) { getWCNVisibility('<xsl:value-of select="$DMFileName"/>'); } </script>
            <!-- prevent any unnecessary divs that could cause harm to the browse step -->
            <xsl:if test="$numberOfWarningsCautions > 0">
                <script> if (!singleDm) { getWCNVisibility('<xsl:value-of select="$DMFileName"/>');
                    } </script>
                <div>
                    <xsl:variable name="refIds"><xsl:value-of select="//reqSafety/safetyRqmts/@warningRefs"/><xsl:text> </xsl:text><xsl:value-of select="//reqSafety/safetyRqmts/@cautionRefs"/></xsl:variable>
                    <xsl:for-each
                        select="//safety//warning | //safety//caution | //reqSafety//warning | // reqSafety//caution ">
                        <div id="safetyWC{position()}">
                            <!-- Only show the first Warning or Caution. -->
                            <xsl:if test="position() != 1">
                                <xsl:attribute name="style">display: none</xsl:attribute>
                            </xsl:if>
                            <xsl:if test="name(.) = 'warning' ">
                                <xsl:call-template name="t_warning"/>
                            </xsl:if>
                            <xsl:if test="name(.) = 'caution' ">
                                <xsl:call-template name="t_caution"/>
                            </xsl:if>
                            <p style="margin-top:10px;">
                                <center>
                                    <input type="button" onclick="acknowledged()">
                                        <xsl:attribute name="onclick">acknowledged(<xsl:value-of
                                                select="position()"/>)</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of select="$v00011"/>
                                        </xsl:attribute>
                                    </input>
                                </center>
                            </p>
                        </div>
                    </xsl:for-each>
                </div>
            </xsl:if>
        </div>
        <div class="dmview" id="dmview">
            <!-- hide the data module content if we have any safety warnings or cautions to show. -->
            <xsl:if test="$numberOfWarningsCautions > 0">
                <xsl:attribute name="style">display: none</xsl:attribute>
            </xsl:if>
            <xsl:call-template name="t_logo"/>
            <xsl:apply-templates/>
            <!-- Do not show the End of data module text when is is a fault DM. -->
            <xsl:if test="not(//isoproc) and not (processFragment)">
                <div class="dmEnd">
                    <!--<xsl:value-of select="$v00297"/>-->
					&#160;               
					 </div>
            </xsl:if>
        </div>
        <script> JumpToRow('dmview'); setContentHolderHeight(); autoJump(); autoXref();
            if(document.getElementById("mainForm:refDMLink")){
            document.getElementById("mainForm:refDMLink").value = "NON"; }
            if(document.getElementById("mainForm:autoXref")){
            document.getElementById("mainForm:autoXref").value = "NON"; }
            if(document.getElementById("mainForm:PDMRResponse")){
            document.getElementById("mainForm:PDMRResponse").value =
            "MATRIX2_PDMR_DEFAULT_NULL_RESPONSE"; } </script>
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
