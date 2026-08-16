<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:dc="http://www.purl.org/dc/elements/1.1/">
	<!--===============================================-->
	<xsl:output indent="yes"></xsl:output>
	<xsl:include href="LanguageVariables.xsl"/>
	<!--===============================================-->
	<xsl:template match="/">
	
	<div class="dmview">
		
		<xsl:value-of select="$v01108"/>&#160;<b><xsl:value-of select="name(/dmodule/content/*[not(name(.)='refs')])"/></b>&#160;<xsl:value-of select="$v01102"/>.
		
				
			</div>
	</xsl:template>
	<!--===============================================-->
</xsl:stylesheet>