<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<!--===============================================-->
	<xsl:template match="eq">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="ne">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="lt">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="gt">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="le">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="ge">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="and">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="or">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="xor">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="concat">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="substring">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="plus">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="minus">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="times">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="divide">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="idivide">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="exponent">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="modulus">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="not">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="empty">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="neg">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="trunc">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="float">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="index-value">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="index">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="defined">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="boolean">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="true">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="false">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="string">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="real">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="integer">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="nil">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="dialog">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="dialog-alt">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="fillin">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="num-range">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="low-bound">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="high-bound">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="double-bound">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="menu">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="prompt">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="choice">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="precond">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="preset">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="postset">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="assertion">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="expression">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="variable">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="variable-declarations">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="variable-ref">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="initialize">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
</xsl:stylesheet>