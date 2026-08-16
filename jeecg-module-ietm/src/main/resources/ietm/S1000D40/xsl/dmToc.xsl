<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<!-- ============================================-->
	<xsl:include href="languageVariables.xsl"/>	
	<xsl:include href="dmTocUtil.xsl"/>
	<!-- ============================================-->
	<xsl:template match="/">
		<toc>
			<element>
				<xsl:attribute name="text"><xsl:value-of select="$v00905"/></xsl:attribute>
				<!-- get all the steps or para0 structure-->
				<xsl:apply-templates/>
				<!-- get all the tables -->
				<xsl:if test="//table[@count] or //sru or //lru">
					<element>
						<xsl:attribute name="text"><xsl:value-of select="$v02474"/></xsl:attribute>
						<xsl:call-template name="T_searchTableElement"/>
					</element>
				</xsl:if>
				<!-- get all the figures-->
				<xsl:if test="//figure[@count]">
					<element>
						<xsl:attribute name="text"><xsl:value-of select="$v00509"/></xsl:attribute>
						<xsl:call-template name="T_searchElement">
							<xsl:with-param name="searchElement"><xsl:value-of select="$v01106"/></xsl:with-param>
						</xsl:call-template>
					</element>
				</xsl:if>
			</element>
		</toc>
	</xsl:template>
	<!-- ============================================-->
	<xsl:template match="para0 | subpara1 | subpara2 | subpara3 | subpara4 | subpara5 | subpara6 | subpara7 | levelledPara | step1 | step2 | step3 | step4 | step5 | step6 | step7 | step8 | proceduralStep">
		<element>
			<xsl:attribute name="text"><xsl:call-template name="T_getNodeNameforDMToc"><xsl:with-param name="searchElement" select="name(.)"/></xsl:call-template>&#160;
<xsl:value-of select="@count | @ptc_level"/>&#160;<xsl:value-of select="title"/></xsl:attribute>
			<xsl:apply-templates/>
		</element>
	</xsl:template>
	<!-- ============================================-->
	<xsl:template name="T_searchElement">
		<!-- note: the preprocessing stylesheet ensures only the right tables get counted-->
		<!-- this means we only need to check there is a count attribute when incuding it in the TOC-->
		<xsl:param name="searchElement"/>
		<xsl:for-each select="//*[name()=$searchElement][@count]">
			<element>
				<xsl:attribute name="text"><xsl:call-template name="T_getNodeNameforDMToc"><xsl:with-param name="searchElement" select="$searchElement"/></xsl:call-template>&#160;<xsl:value-of select="@count"/><xsl:call-template name="T_TitleDisplay"/></xsl:attribute>
			</element>
		</xsl:for-each>
	</xsl:template>
	<!-- ============================================-->
	<xsl:template name="T_searchTableElement">
		<!-- this template is similar to T_searchElement but it searches for tables and table like-->
		<!-- elements. This is because there are some elements, such as fault lru and srus that have to -->
		<!-- counted like tables in the order that they appear in the document -->
		<xsl:for-each select="//*[name()='table'][@count] 
									| //sru[not((preceding-sibling::*)[position()=last()][name()='sru'])] 
									| //lru[not((preceding-sibling::*)[position()=last()][name()='lru'])] 
									| //afr//refs">
			<element>
				<xsl:attribute name="text"><xsl:call-template name="T_getNodeNameforDMToc"><xsl:with-param name="searchElement" select="name(.)"/></xsl:call-template>&#160;<xsl:value-of select="@count"/>&#160;<xsl:call-template name="T_TitleDisplay"/></xsl:attribute>
			</element>
		</xsl:for-each>
	</xsl:template>
	<!-- ============================================-->
	<xsl:template match="text()">
		<!-- stop-->
	</xsl:template>
	<!-- ============================================-->
</xsl:stylesheet>
