<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
	<!--===============================================-->
	<xsl:include href="fig_tab.xsl"/> 
	<xsl:include href="wcnp.xsl"/>
	<!--===============================================-->	
	<xsl:template match="descript|description">
		<!-- import css first -->
		<xsl:call-template name="importCSS"/>	
		<xsl:apply-templates/>
		<xsl:call-template name="initOutPutArea"/>
	</xsl:template>
	<!--===============================================-->
</xsl:stylesheet>