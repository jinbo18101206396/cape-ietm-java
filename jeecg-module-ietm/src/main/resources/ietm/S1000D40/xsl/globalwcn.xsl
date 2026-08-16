<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:dc="http://www.purl.org/dc/elements/1.1/">
	<!--===============================================-->
	<xsl:include href="rdf.xsl"/>
	<!--===============================================-->
	<xsl:include href="dmc.xsl"/>
	<!--===============================================-->
	<xsl:include href="pmc.xsl"/>
	<!--===============================================-->
	<xsl:include href="status.xsl"/>
	<!--===============================================-->
	<xsl:include href="globalParams.xsl"/>
	<!--===============================================-->
	<xsl:include href="languageVariables.xsl"/>
	<!--===============================================-->
	<xsl:include href="common.xsl"/>
	<!--===============================================-->
	<xsl:include href="descript.xsl"/>
	<!--===============================================-->
	<xsl:include href="paras.xsl"/>
	<!--===============================================-->
	<xsl:include href="lists.xsl"/>
	<!--===============================================-->
	<xsl:include href="capgrp.xsl"/>
	<!--===============================================-->
	<xsl:include href="content.xsl"/>
	<!--===============================================-->
	<xsl:template match="/">
		<!-- import css first -->
		<xsl:call-template name="importCSS"/>		
		<!-- Set the total number of Warnings and Cautions so we can step through them. -->
		<xsl:variable name="numberOfWCN"><xsl:value-of select="count(//descript/warning | //description/warning | //descript/caution | //description/caution | //descript/note | //description/note)"/></xsl:variable>
		<xsl:element name="script">
			numberOfWarningsCautions =<xsl:value-of select="$numberOfWCN"/>;
			initFigureBrowser();
			var singleDm = <xsl:value-of select="$SingleDMView" />;
		</xsl:element>
		<div class="dmview">
			<xsl:for-each select="//descript/warning | //description/warning | //descript/caution | //description/caution | //descript/note | //description/note">
				<div id="safetyWC{position()}">
					<!-- Only show the first Warning or Caution. -->
					<xsl:if test="position() != 1">
						<xsl:attribute name="style">display: none</xsl:attribute>
					</xsl:if>
					<xsl:if test="name(.) = 'warning' ">
						<xsl:call-template name="t_warning"/>
					</xsl:if>
					<xsl:if test="name(.) = 'caution' ">
						<xsl:call-template name="t_caution"/>
					</xsl:if>
					<xsl:if test="name(.) = 'note' ">
						<xsl:call-template name="t_note"/>
					</xsl:if>
					<p>
						<center>
						<xsl:choose>
						<!-- show the JSF button when we are on the second last waring or caution. -->
							<xsl:when test="$numberOfWCN = 1">
								<script language="text/javascript">
									acknowledged(safetyWC<xsl:value-of select="position()"/>);
								</script>
							</xsl:when>
							<xsl:when test="position() = ($numberOfWCN -1)">
								<input type="button">	
									<xsl:attribute name="onclick">acknowledged(<xsl:value-of select="position()"/>);showJSFacknowledged();</xsl:attribute>
									<xsl:attribute name="value"><xsl:value-of select="$v00011"/></xsl:attribute>
								</input>
							</xsl:when>
							<xsl:when test="position() &lt; ($numberOfWCN - 1)">
									<input type="button">
										<xsl:attribute name="onclick">acknowledged(<xsl:value-of select="position()"/>)</xsl:attribute>
										<xsl:attribute name="value"><xsl:value-of select="$v00011"/></xsl:attribute>
									</input>
							</xsl:when>
						</xsl:choose>
					
						</center>
					</p>
				</div>
			</xsl:for-each>
		</div>
	</xsl:template>
	<!--===============================================-->
</xsl:stylesheet>