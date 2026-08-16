<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:fo="http://www.w3.org/1999/XSL/Format" 
    xmlns:xlink="http://www.w3.org/1999/xlink"
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    xmlns:dc="http://www.purl.org/dc/elements/1.1/">
    <!-- ================================= -->
    <xsl:include href="fig_tab.xsl"/>
    <xsl:include href="wcnp.xsl"/>
    <!-- ================================= -->
    <xsl:template match="act|applicCrossRefTable">
        <center>
            <h3>
                <xsl:value-of select="$v01174"/>
            </h3>
        </center>
        <xsl:call-template name="importCSS"/>
        <p>
            <xsl:value-of select="$v01164"/>
        </p>
        <xsl:apply-templates/>
    </xsl:template>
    <!-- ================================= -->
    <xsl:template match="productattributes|productAttributeList">
        <p>
            <xsl:value-of select="$v01165"/>
        </p>
        <table border="1" cellpadding="5" width="100%">
            <tr class="dr-pnl-h rich-panel-header tabletitle">
                <th width="20%"><xsl:value-of select="$v02220"/></th>
                <th width="40%"><xsl:value-of select="$v01166"/></th>
                <th width="40%"><xsl:value-of select="$v00238"/></th>
            </tr>
            <xsl:apply-templates/>
        </table>
    </xsl:template>
    <!-- ================================= -->
    <xsl:template match="cctref|condCrossRefTableRef">
        <p>
            <xsl:value-of select="$v01167"/>
        </p>
        <xsl:apply-templates/>
    </xsl:template>
    <!-- ================================= -->
    <xsl:template match="pctref|productCrossRefTableRef">
        <p>
            <xsl:value-of select="$v01168"/>
        </p>
        <xsl:apply-templates/>
    </xsl:template>
    <!-- ================================= -->
    <xsl:template match="refdm">
        <p><xsl:value-of select="$v02466"/></p>
        <xsl:apply-templates/>
    </xsl:template>
    <!-- ================================= -->
    <xsl:template match="prodattr|productAttribute">
        <tr>
            <td><xsl:apply-templates select="name"/>&#160;</td>
            <td><xsl:apply-templates select="displayname|displayName"/>&#160;</td>
            <td><xsl:apply-templates select="description|descr"/>&#160;</td>
            <xsl:apply-templates/>
        </tr>
    </xsl:template>
    <!-- ================================= -->
    <xsl:template match="name">
        <xsl:apply-templates/>
    </xsl:template>
    <!-- ================================= -->
    <xsl:template match="displayname|displayName">
        <xsl:apply-templates/>
    </xsl:template>
    <!-- ================================= -->
    <xsl:template match="description|descr">
        <xsl:apply-templates/>
    </xsl:template>
    <!-- ================================= -->
</xsl:stylesheet>
