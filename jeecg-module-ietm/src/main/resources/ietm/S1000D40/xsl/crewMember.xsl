<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<!--===============================================-->
	<xsl:template match="crewmem">
		<!-- stop -->
	</xsl:template>
	<!--===============================================-->
	<xsl:template name="crewmemTerm">
		<!-- returns the crewmem term from the crewmen attribute -->
		<!-- make lower case-->
		<xsl:variable name="crewmenValue">
			<xsl:value-of select="translate(@crewmem,'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')"/>
		</xsl:variable>		
		<xsl:choose>
			<xsl:when test="$crewmenValue = 'all' or $crewmenValue = 'cm01'"><xsl:value-of select="$v00022"/></xsl:when>
			<xsl:when test="$crewmenValue = 'p' or $crewmenValue = 'cm02'">P</xsl:when>
			<xsl:when test="$crewmenValue = 'c' or $crewmenValue = 'cm03'">C</xsl:when>
			<xsl:when test="$crewmenValue = 'n' or $crewmenValue = 'cm04'">N</xsl:when>
			<xsl:when test="$crewmenValue = 'e' or $crewmenValue = 'cm05'">EH</xsl:when>
			<xsl:when test="$crewmenValue = 'g' or $crewmenValue = 'cm06'">G</xsl:when>
			<xsl:when test="$crewmenValue = 'lm' or $crewmenValue = 'cm07'">LM</xsl:when>
			<xsl:when test="$crewmenValue = 'cs' or $crewmenValue = 'cm08'">CS</xsl:when>
			<xsl:otherwise> <xsl:value-of select="$v02462"/></xsl:otherwise>
		</xsl:choose>
		<xsl:if test="following-sibling::crewmem">,</xsl:if>
	</xsl:template>
	<!--===============================================-->
</xsl:stylesheet>
