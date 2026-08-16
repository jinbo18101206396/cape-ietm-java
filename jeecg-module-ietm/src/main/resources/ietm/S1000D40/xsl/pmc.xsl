<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" >
	<!--===============================================-->
	<xsl:template match="pmissuer">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="pmnumber">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="pmvolume">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
</xsl:stylesheet>
