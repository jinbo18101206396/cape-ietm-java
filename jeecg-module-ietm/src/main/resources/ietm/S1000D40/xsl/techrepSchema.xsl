<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:dc="http://www.purl.org/dc/elements/1.1/">
    <!--========================================================================================-->
    <!--<xsl:output indent="yes"></xsl:output>-->
    <xsl:output media-type= "text/html " encoding= "GB2312" method= "html"/>
    <!--========================================================================================-->	
    <xsl:include href="base.xsl"/>
    <xsl:include href="common.xsl"/>
	<xsl:include href="content.xsl"/>
	<xsl:include href="fig_tab.xsl"/>
    <xsl:include href="techrep.xsl"/>
    <xsl:include href="uom.xsl"/>
    <xsl:include href="wcnp.xsl"/>
    <!--========================================================================================-->
    <xsl:param name="context" >data-module</xsl:param>
	<!--========================================================================================-->
	<xsl:template match="techRepository">
        <xsl:call-template name="importCSS"/>
        <div class="dmTypeTitle"><xsl:value-of select="$v02187"/></div>
        <xsl:apply-templates/>
        <xsl:call-template name="initOutPutArea"/>
    </xsl:template>
    <!--========================================================================================-->
    
</xsl:stylesheet>
