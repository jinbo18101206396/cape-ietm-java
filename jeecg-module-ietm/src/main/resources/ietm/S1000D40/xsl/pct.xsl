<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:fo="http://www.w3.org/1999/XSL/Format" xmlns:xlink="http://www.w3.org/1999/xlink"
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    xmlns:dc="http://www.purl.org/dc/elements/1.1/">
    <!-- ================================= -->
    <xsl:include href="fig_tab.xsl"/>
    <xsl:include href="wcnp.xsl"/>
    <!-- ================================= -->
    <xsl:template match="pct|productCrossRefTable">
        <xsl:call-template name="importCSS"/>
        <center>
            <h3>
                <xsl:value-of select="$v00724"/>
            </h3>
        </center>
        <p>
            <xsl:value-of select="$v01173"/>
        </p>
        <xsl:apply-templates/>
    </xsl:template>
    <!-- ================================= -->
    <xsl:template match="product">
        <p><xsl:value-of select="$v02432"/></p>
        <table border="1" cellpadding="5">
            <tr class="dr-pnl-h rich-panel-header tabletitle">
                <th>
                    <xsl:value-of select="$v01174"/>
                </th>
                <th>
                    <xsl:value-of select="$v01175"/>
                </th>
                <th>
                    <xsl:value-of select="$v01176"/>
                </th>
            </tr>
            <xsl:apply-templates/>
        </table>
    </xsl:template>
    <!-- ================================= -->
    <xsl:template match="assign">
        <tr>
            <td>
                <xsl:value-of select="@actidref|@applicPropertyIdent"/>
            </td>
            <td>
                <xsl:value-of select="@actreftype|@applicPropertyType"/>
            </td>
            <td>
                <xsl:value-of select="@actvalue|@applicPropertyValue"/>
            </td>
        </tr>
    </xsl:template>
    <!-- ================================= -->
</xsl:stylesheet>
