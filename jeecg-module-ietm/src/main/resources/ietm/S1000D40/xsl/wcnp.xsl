<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<!--===============================================-->
<!--<xsl:include href="wcnpPara.xsl"/>-->
<xsl:include href="wcnpWarningCaution.xsl"/>
<xsl:include href="wcnpNote.xsl"/>
<!--===============================================-->
<xsl:template name="wcnLabel">
		<xsl:param name="T_NodeName"/>
		<xsl:if test="$T_NodeName='warning'">
			<xsl:choose>
				<xsl:when test="following-sibling::*[name(.)=$T_NodeName] or preceding-sibling::*[name(.)=$T_NodeName]">
					<!--<xsl:value-of select="$v01063"/>-->警&#160;告
				</xsl:when>
				<xsl:otherwise>
					<!--<xsl:value-of select="$v01062"/>-->警&#160;告					
				</xsl:otherwise>
			</xsl:choose>
		</xsl:if>
		<xsl:if test="$T_NodeName='caution'">
			<xsl:choose>
				<xsl:when test="following-sibling::*[name(.)=$T_NodeName] or preceding-sibling::*[name(.)=$T_NodeName]">
					<!--<xsl:value-of select="$v00096"/>-->注&#160;意
				</xsl:when>
				<xsl:otherwise>
					<!--<xsl:value-of select="$v00096"/>-->注&#160;意
				</xsl:otherwise>
			</xsl:choose>
		</xsl:if>
		<xsl:if test="$T_NodeName='note'">		
			<xsl:choose>
				<xsl:when test="following-sibling::*[name(.)=$T_NodeName] or preceding-sibling::*[name(.)=$T_NodeName]">
					<!--<xsl:value-of select="$v00598"/>-->[注]：
				</xsl:when>
				<xsl:otherwise>
					<!--<xsl:value-of select="$v00594"/>-->[注]：
				</xsl:otherwise>
			</xsl:choose>			
		</xsl:if>
	</xsl:template>
<!--===============================================-->
</xsl:stylesheet>