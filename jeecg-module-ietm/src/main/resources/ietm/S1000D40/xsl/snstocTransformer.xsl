<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:xlink="http://www.w3.org/1999/xlink" version="1.0">

	<xsl:output method="html" />
	
	<xsl:output indent="no" />
	<xsl:strip-space elements="*"/>

	<xsl:template match="/">
		<div style="width:279px; padding: 10px; overflow: auto; white-space: nowrap; height: 100%;" class="tocDiv"
			onclick="tocEvent(event);">
			<xsl:apply-templates />
		</div>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="idstatus" />
	<!--===============================================-->
	<xsl:template match="toc | pmentry">
		<div>
			<xsl:variable name="id">
				<xsl:value-of select="@ptc_pos" />
			</xsl:variable>
			<!-- button -->
			<img type="colspand" param="{name(.)}{$id}" src="./images/tree/right_arrow.gif"
				class="refdmImg" />
			<span type="branch" param="{$id}" style="cursor:pointer;">
				<img alt="" src="./images/tree/iconFolder.gif" type="branch"
					param="{$id}" class="refdmImg" />
				<xsl:apply-templates select="title" />
			</span>

			<div style="padding-left: 20px; display:none;" id="{name(.)}{$id}">
				<xsl:apply-templates select="toc | pmentry | refdm | dmRef" />
			</div>
		</div>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="refdm|reqdm">
		<xsl:element name="div">
			<xsl:attribute name="class">refdmDiv</xsl:attribute>
			<xsl:element name="img">
				<xsl:attribute name="src">./images/tree/iconLeaf.gif</xsl:attribute>
				<xsl:attribute name="class">refdmImg</xsl:attribute>
				<xsl:attribute name="alt"></xsl:attribute>
			</xsl:element>
			<a class="refdmLink" type="ref" param="{@xlink:href}">
				<xsl:call-template name="t_formatDMC" />
			</a>
		</xsl:element>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="dmRef">
		<div class="refdmDiv">
			<img src="./images/tree/iconLeaf.gif" class="refdmImg" alt=""></img>
			<a class="refdmLink" type="ref" param="{@xlink:href}">
				<xsl:call-template name="t_formatIss4DMC" />
			</a>
		</div>
	</xsl:template>
	<!--===============================================-->
	<xsl:template name="t_formatDMC">
		<xsl:if test="dmcextension">
			<xsl:value-of select="dmcextension/dmeproducer" />
			<xsl:text>-</xsl:text>
			<xsl:value-of select="dmcextension/dmecode" />
			<xsl:text>-</xsl:text>
		</xsl:if>
		<xsl:choose>
			<xsl:when test=".//avee">
			    <xsl:value-of select=".//modelic" />-<xsl:value-of select=".//sdc" />-<xsl:value-of select=".//chapnum" />-<xsl:value-of select=".//section" /><xsl:value-of select=".//subsect" />-<xsl:value-of select=".//subject" />-<xsl:value-of select=".//discode" /><xsl:value-of select=".//discodev" />-<xsl:value-of select=".//incode" /><xsl:value-of select=".//incodev" />-<xsl:value-of select=".//itemloc" />
			</xsl:when>
			<xsl:when test=".//age">
			    <xsl:value-of select=".//modelic" />-<xsl:value-of select=".//supeqvc" />-<xsl:value-of select=".//ecscs" />-<xsl:value-of select=".//eidc" />-<xsl:value-of select=".//cidc" />-<xsl:value-of select=".//discode" /><xsl:value-of select=".//discodev" />-<xsl:value-of select=".//incode" /><xsl:value-of select=".//incodev" />-<xsl:value-of select=".//itemloc" />
			</xsl:when>
		</xsl:choose>
	</xsl:template>
	<!--===============================================-->
	<xsl:template name="t_formatIss4DMC">
		<xsl:value-of select=".//dmCode/@modelIdentCode" />-<xsl:value-of select=".//dmCode/@systemDiffCode" />-<xsl:value-of select=".//dmCode/@systemCode" />-<xsl:value-of select=".//dmCode/@subSystemCode" /><xsl:value-of select=".//dmCode/@subSubSystemCode" />-<xsl:value-of select=".//dmCode/@assyCode" />-<xsl:value-of select=".//dmCode/@disassyCode" /><xsl:value-of select=".//dmCode/@disassyCodeVariant" />-<xsl:value-of select=".//dmCode/@infoCode" /><xsl:value-of select="//dmCode/@infoCodeVariant" />-<xsl:value-of select=".//dmCode/@itemLocationCode" />
		<xsl:if test="//dmCode/@learnCode">-<xsl:value-of select="//dmCode/@learnCode" />
		</xsl:if>
		<xsl:if test="//dmCode/@learnEventCode">-<xsl:value-of select="//dmCode/@learnEventCode" />
		</xsl:if>
	</xsl:template>
	<!--===============================================-->
</xsl:stylesheet>
