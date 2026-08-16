<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<!-- ============================================-->
	<xsl:include href="airfaultNaming.xsl"/>
	<!-- ============================================-->
	<!-- requires language translation file -->
	<xsl:template name="T_getNodeNameforDMToc">
		<xsl:param name="searchElement"/>
		<xsl:if test="$searchElement = 'table' or $searchElement = 'lru' or $searchElement = 'sru' or $searchElement = 'refs'">
			<xsl:value-of select="$v00907"/>
		</xsl:if>
		<xsl:if test="$searchElement = 'figure'">
			<xsl:value-of select="$v00355"/>
		</xsl:if>
		<xsl:if test="$searchElement ='step1' or $searchElement='step2' or $searchElement='step3' or $searchElement ='step4' or $searchElement ='step5' or $searchElement ='step6' or $searchElement ='step7' or $searchElement ='step8' or $searchElement = 'proceduralStep'">
			<xsl:value-of select="$v00878"/>
		</xsl:if>
	</xsl:template>
	<!-- ============================================-->
	<xsl:template name="T_makeDMTocLink">
		<xsl:variable name="dmtocID">dmtoc<xsl:call-template name="T_getNodeNameforDMToc">
				<xsl:with-param name="searchElement" select="name(.)"/>
			</xsl:call-template>
			<xsl:value-of select="@count|@ptc_level"/>
			<xsl:call-template name="T_TitleDisplay"/>
		</xsl:variable>
		<div style="height: 0px;">
			<xsl:attribute name="id"><xsl:value-of select="translate(normalize-space($dmtocID), '&#x20;&#x9;&#xD;&#xA;&#160;', '')"/></xsl:attribute> 
			&#160;			
		</div>
	</xsl:template>
	<!-- ============================================-->
	<xsl:template name="T_TitleDisplay">
		<xsl:choose>
			<!-- if in fault call the specific fault naming function -->
			<!-- at the moment i only do afr refs-->
			<!-- to handle more refs we are going to need more login in here -->
			<xsl:when test="name() = 'refs'">
				<xsl:call-template name="T_AirFaultRefsTitles"/>
			</xsl:when>
			<xsl:when test="name() = 'lru' or name() = 'sru'">
				<xsl:call-template name="T_AirFaultTitle"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:if test="title">&#160;<xsl:value-of select="title"/>
				</xsl:if>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<!-- ============================================-->
</xsl:stylesheet>
