<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink">
	<xsl:output method="xml" version="1.0" encoding="UTF-16" indent="yes"/>
	<!-- -->
	<xsl:template match="/">
		<toc>
			<xsl:apply-templates/>
		</toc>
	</xsl:template>
	<!-- -->
	<xsl:template match="pmentry">
		<toc>
			<xsl:apply-templates/>
		</toc>
	</xsl:template>
	<!-- -->
	<xsl:template match="pmtitle | title">
		<title>
			<xsl:apply-templates/>
		</title>
	</xsl:template>
	<!-- -->
	<xsl:template match="refdm">
		<refdm>
			<xsl:attribute name="xlink:type">simple</xsl:attribute>
			<xsl:attribute name="xlink:href"><xsl:call-template name="formatLink"/></xsl:attribute>
			<xsl:attribute name="xlink:title"><xsl:apply-templates select="dmtitle"/></xsl:attribute>
			<xsl:attribute name="xlink:show">replace</xsl:attribute>
			<xsl:attribute name="xlink:actuate">onRequest</xsl:attribute>
			<xsl:copy-of select="./*"/>
		</refdm>
	</xsl:template>
	<!-- -->
	<xsl:template name="formatLink">
		<xsl:choose>
			<xsl:when test="./dmc/avee">
					<xsl:call-template name="formatAveeDMC"/>
			</xsl:when>
			<xsl:when test="./dmc/age">
					<xsl:call-template name="formatAgeDMC"/>
			</xsl:when>
		</xsl:choose>
	</xsl:template>
	<!-- -->
	<xsl:template name="formatAveeDMC">
		<xsl:call-template name="DMC_DME"/>
		<xsl:value-of select="./dmc/avee/modelic"/>-<xsl:value-of select="./dmc/avee/sdc"/>-<xsl:value-of select="./dmc/avee/chapnum"/>-<xsl:value-of select="./dmc/avee/section"/>
		<xsl:value-of select="./dmc/avee/subsect"/>-<xsl:value-of select="./dmc/avee/subject"/>-<xsl:value-of select="dmc/avee/discode"/>
		<xsl:value-of select="./dmc/avee/discodev"/>-<xsl:value-of select="./dmc/avee/incode"/>
		<xsl:value-of select="./dmc/avee/incodev"/>-<xsl:value-of select="./dmc/avee/itemloc"/>_<xsl:value-of select="./issno/@issno"/>.xml</xsl:template>
	<!-- -->
	<xsl:template name="formatAgeDMC">
		<xsl:call-template name="DMC_DME"/>
		<xsl:value-of select="./dmc/age/modelic"/>-<xsl:value-of select="./dmc/age/supeqvc"/>-<xsl:value-of select="./dmc/age/ecscs"/>-<xsl:value-of select="./dmc/age/eidc"/>-<xsl:value-of select="./dmc/age/cidc"/>-<xsl:value-of select="./dmc/age/discode"/>
		<xsl:value-of select="./dmc/age/discodev"/>-<xsl:value-of select="./dmc/age/incode"/>
		<xsl:value-of select="./dmc/age/incodev"/>-<xsl:value-of select="./dmc/age/itemloc"/>_<xsl:value-of select="./issno/@issno"/>.xml</xsl:template>
	<!-- -->
<xsl:template name="DMC_DME">
		<xsl:choose>
			<xsl:when test="./dmcextension">DME-<xsl:value-of select="./dmcextension/dmeproducer"/>-<xsl:value-of select="./dmcextension/dmecode"/>-</xsl:when>
			<xsl:otherwise>DMC-</xsl:otherwise>
		</xsl:choose>
	</xsl:template>	
	<!-- -->	
	<xsl:template match="pmc| pmstatus">
		<!-- Do NOT show. -->
	</xsl:template>
</xsl:stylesheet>
