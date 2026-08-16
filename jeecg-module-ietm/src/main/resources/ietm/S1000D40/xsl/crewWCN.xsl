<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<!--===============================================-->
	<!--
		 Crew styles mean that warnings, cautions and notes have to be 
		placed on line with steps and their indent
	-->
	<!--===============================================-->
	<xsl:template name="getAcrwWCNIndentValue">
		<xsl:value-of select="(count(ancestor::if | ancestor::case | ancestor::elseif | ancestor::step | ancestor::caseCond | ancestor::elseIf | ancestor::crewDrillStep)*40) +15"/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="acrw//warning|crew//warning">
		<xsl:variable name="indent">
			<xsl:call-template name="getAcrwWCNIndentValue"/>
		</xsl:variable>
		<xsl:call-template name="t_warning">
			<xsl:with-param name="acrwIndent" select="$indent"/>
		</xsl:call-template>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="acrw//caution|crew//caution">
		<xsl:variable name="indent">
			<xsl:call-template name="getAcrwWCNIndentValue"/>
		</xsl:variable>
		<xsl:call-template name="t_caution">
			<xsl:with-param name="acrwIndent" select="$indent"/>
		</xsl:call-template>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="acrw//note|crew//note">
		<xsl:call-template name="t_inlineApplicability">
            <xsl:with-param name="annotation"><xsl:value-of select="$v00034"/>:&#160;</xsl:with-param>
        </xsl:call-template>
		<xsl:variable name="indent">
			<xsl:call-template name="getAcrwWCNIndentValue"/>
		</xsl:variable>
		<xsl:call-template name="t_note">
			<xsl:with-param name="acrwIndent" select="$indent"/>
		</xsl:call-template>
	</xsl:template>
	<!--===============================================-->
</xsl:stylesheet>