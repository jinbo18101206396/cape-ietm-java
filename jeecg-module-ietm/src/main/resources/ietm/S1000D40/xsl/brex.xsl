<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:fo="http://www.w3.org/1999/XSL/Format" xmlns:xlink="http://www.w3.org/1999/xlink"
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    xmlns:dc="http://www.purl.org/dc/elements/1.1/"> 
    <xsl:include href="fig_tab.xsl"/>
    <xsl:include href="wcnp.xsl"/>
    <!-- ================================= -->
    <xsl:template match="brex">
        <xsl:call-template name="importCSS"/>
        <center>
            <h3><xsl:value-of select="$v02438"/></h3>
        </center>
        <xsl:apply-templates/>
        <xsl:call-template name="initOutPutArea"/>
    </xsl:template>
    <!-- ================================= -->
    <xsl:template match="contextrules|contextRules">
        <table border="1" cellpadding="5" width="100%">
            <tr class="dr-pnl-h rich-panel-header tabletitle">
                <th width="33%"><xsl:value-of select="$v01157"/></th>
                <th width="33%"><xsl:value-of select="$v01158"/></th>
                <th width="34%"><xsl:value-of select="$v01159"/></th>
            </tr>
            <xsl:apply-templates/>
        </table>
    </xsl:template>
    <!-- ================================= -->
    <xsl:template match="snsRules"> </xsl:template>
    <!-- ================================= -->
    <xsl:template match="snsDescr">
        <p>
            <xsl:apply-templates/>
        </p>
    </xsl:template>
    <!-- ================================= -->
    <xsl:template match="snsSubSystem">
        <table border="1" cellpadding="5" width="100%">
            <tr class="dr-pnl-h rich-panel-header tabletitle">
                <th width="30%">SNS Code</th>
                <th width="30%">SNS Title</th>
                <th width="40%"/>
            </tr>
        </table>
    </xsl:template>
    <!-- ================================= -->
    <xsl:template match="objrule|structureObjectRule">
        <tr valign="top">
            <td width="33%">
                <xsl:apply-templates select="objpath|objectPath"/>&#160; </td>
            <td width="33%">
                <xsl:apply-templates select="objuse|objectUse"/>&#160; </td>
            <td width="34%">
                <xsl:apply-templates select="objval|objectValue"/>&#160; </td>
        </tr>
    </xsl:template>
    <!-- ================================= -->
    <xsl:template match="objpath|objectPath">
        <xsl:if test="@objappl"> Object Applic : <xsl:value-of select="@objappl"/>
            <br/>
        </xsl:if>
        <xsl:apply-templates/>
    </xsl:template>
    <!-- ================================= -->
    <xsl:template match="objuse|objectUse">
        <xsl:apply-templates/>
    </xsl:template>
    <!-- ================================= -->
    <xsl:template match="objval">
       <xsl:apply-templates/>
    </xsl:template>
    <!-- ================================= -->
    <!-- iss4.0 -->
    <xsl:template match="objectValue">
        <xsl:value-of select="@valueForm"/>&#160;<xsl:value-of select="@valueAllowed"/>&#160;<xsl:apply-templates/>
        <br/>
    </xsl:template>
    <!-- ================================= -->
    <xsl:template match="@valtype['range']"> Value Range:&#160;<xsl:value-of select="../@val1"/> to
            <xsl:value-of select="../@val2"/>
    </xsl:template>
    <!-- ================================= -->
    <xsl:template match="@valtype[not('range')]">Value:&#160;<xsl:value-of select="../@val1|../@subscript"/>
        <xsl:apply-templates/>
    </xsl:template>
    <!-- ================================= -->
</xsl:stylesheet>
