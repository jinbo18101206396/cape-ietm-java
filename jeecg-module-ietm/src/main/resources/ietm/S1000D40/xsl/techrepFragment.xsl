<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
    >
    <!--2012xmlns:aadext="java:com.ptc.aad.xsltext.StringFunctions?path=file:///E:/IETM4/xsl/java/" exclude-result-prefixes="aadext"
    xmlns:tir-proc="java:com.ptc.arbortext.aad.ietp.tir.IetpTIRProcessor?path=file:///E:/IETM4/xsl/java/" exclude-result-prefixes="tir-proc"-->
    <!--========================================================================================-->
    <xsl:include href="content.xsl"/>
    <xsl:include href="languageVariables.xsl"/>
    <xsl:include href="globalParams.xsl"/>
    <xsl:include href="techrep.xsl"/>
    <xsl:include href="techrepRef.xsl"/>
 	<xsl:include href="uom.xsl"/>
    <!--========================================================================================-->
    <xsl:param name="context">fragment</xsl:param>
    <!--========================================================================================-->
    <xsl:template match="xref|internalRef" name="t_xref">
        <!-- print any pretext on the xref (if any)-->
        <span class="xrefText">
            <xsl:if test="@pretext and not(@pretext='')">
                <xsl:value-of select="@pretext"/>, </xsl:if>
            <xsl:call-template name="t_xref_processing"/>
            <xsl:apply-templates/>
            <xsl:if test="@posttext and not(@posttext='')"> ,<xsl:value-of select="@posttext"
                /></xsl:if>
        </span>
    </xsl:template>
    <!-- ===================================================================== -->
    <xsl:template name="t_xref_processing">
        <xsl:variable name="type">
            <!--2012<xsl:value-of select="lower-case(@xidtype|@internalRefTargetType)"/>-->
            <xsl:value-of select="translate(@xidtype|@internalRefTargetType, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')"/>
        </xsl:variable>
        <xsl:variable name="id">
            <xsl:value-of select="@xrefid|@internalRefId"/>
        </xsl:variable>
        <xsl:value-of select="$type"/>-<xsl:value-of select="$id"/>
        <xsl:call-template name="t_xref_link">
            <xsl:with-param name="type" select="$type"/>
            <xsl:with-param name="id" select="$id"/>
        </xsl:call-template>
    </xsl:template>
    <!-- ===================================================================== -->
    <xsl:template name="t_xref_link">
        <xsl:param name="type"/>
        <xsl:param name="id"/>
        <xsl:variable name="refID">
            <xsl:value-of select="translate($id,'-','-')"/>
        </xsl:variable>
        <span class="xrefLink">
            <a href="#">
                <xsl:attribute name="onclick">scrollTIRPanel("tir-<xsl:value-of select="$id"
                    />")</xsl:attribute>
                <xsl:call-template name="t_xref_link_label">
                    <xsl:with-param name="type" select="$type"/>
                </xsl:call-template>
            </a>
        </span>
    </xsl:template>
    <!-- ===================================================================== -->
    <xsl:template name="t_xref_link_label">
        <xsl:param name="type"/>
            <xsl:choose>
                <xsl:when test="$type='figure'"><xsl:value-of select="$v00354"/>&#160;<xsl:value-of
                    select="@xrefid|@internalRefId"/></xsl:when> 
                <xsl:when test="$type='sheet' or $type='graphic'">
                    <xsl:value-of select="$v00354"/>
                    <xsl:value-of select="position()"/>
                    <xsl:number count="figure" level="any"/>
                    <xsl:value-of select="v00860"/>. <xsl:number level="any" from="figure"/>
                </xsl:when>
                <xsl:otherwise> XREF 图<xsl:value-of select="@xidtype|@internalRefTargetType"/> NOT YET
                    HANDLED </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <!-- ===================================================================== -->
</xsl:stylesheet>
