<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:dc="http://www.purl.org/dc/elements/1.1/">	
	<!--===============================================-->	
	<xsl:template match="rdf:Description">	
		<!-- stop for now -->
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="dc:title">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="dc:creator">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="dc:subject">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="dc:publisher">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="dc:contributor">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="dc:date">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="Type">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="dc:format">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="dc:identifier">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="dc:language">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="dc:rights">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
</xsl:stylesheet>