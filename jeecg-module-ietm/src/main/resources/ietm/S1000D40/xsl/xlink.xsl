<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<!--===============================================-->
<xsl:template match="resource">
	<xsl:apply-templates/>
</xsl:template>
<!--===============================================-->
<xsl:template match="locator">
	<xsl:apply-templates/>
</xsl:template>
<!--===============================================-->
<xsl:template match="arc">
	<xsl:apply-templates/>
</xsl:template>
<!--===============================================-->
<xsl:template name="createLinkAnchor">
<!-- creates an  anchor tag for xreffing the current node -->
	<xsl:element name="a">
		<xsl:attribute name="name"><xsl:value-of select="translate(@id,'-','-')"/></xsl:attribute>
	</xsl:element>
</xsl:template>
<!--===============================================-->
</xsl:stylesheet>