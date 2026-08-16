<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:fo="http://www.w3.org/1999/XSL/Format" xmlns:xlink="http://www.w3.org/1999/xlink"
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    xmlns:dc="http://www.purl.org/dc/elements/1.1/" xmlns:sch="http://www.ascc.net/xml/schematron">
    <!-- ================================= -->
    <xsl:template match="/">
        <html>
            <head>
                <title/>
            </head>
            <body>
                <center>
                    <h3>Schematron</h3>
                    <xsl:apply-templates/>
                </center>
            </body>
        </html>
    </xsl:template>
    <!-- ================================= -->
    <xsl:template match="idstatus">
        <table border="1" cellpadding="5">
            <tr class="dr-pnl-h rich-panel-header tabletitle">
                <th><xsl:value-of select="$v00402"/>&#160;<xsl:value-of select="$v00877"/></th>
                <th><xsl:text>&#160; &#160;</xsl:text></th>
            </tr>
            <xsl:apply-templates/>
        </table>
    </xsl:template>
    <!-- ================================= -->
    <xsl:template match="sch:schema">
        <table border="1" cellpadding="5">
            <tr class="dr-pnl-h rich-panel-header tabletitle">
                <th><xsl:value-of select="$v02471"/></th>
                <th><xsl:text>&#160; &#160;</xsl:text></th>
            </tr>
            <xsl:apply-templates/>
        </table>
    </xsl:template>
    <!-- ================================= -->
    <xsl:template match="sch:pattern">
        <tr class="dr-pnl-h rich-panel-header tabletitle">
            <th><xsl:value-of select="$v02463"/></th>
            <th><xsl:value-of select="$v00238"/></th>
        </tr>
        <xsl:apply-templates/>
    </xsl:template>
    <!-- ================================= -->
    <xsl:template match="sch:report">
        <tr>
            <td><xsl:value-of select="$v02468"/></td>
            <td><xsl:apply-templates/></td>
        </tr>        
    </xsl:template>
    <!-- ================================= -->
    <xsl:template match="sch:assert">
        <tr>
            <td><xsl:value-of select="$v02436"/></td>
            <td><xsl:apply-templates/></td>
        </tr>        
    </xsl:template>
    <!-- ================================= -->
</xsl:stylesheet>
