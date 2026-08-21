<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<!--===============================================-->
	<xsl:strip-space elements="*"/>
	<xsl:template match="figure">
		<!-- make a div for the dmtoc link
		<xsl:call-template name="T_makeDMTocLink"/>-->
		<!-- xref anchor
		<xsl:call-template name="createLinkAnchor"/>-->
		<div class="figure">
			<xsl:apply-templates select="applic"/>
			<xsl:apply-templates select="graphic">
				<xsl:with-param name="figCount" select="@count"/>
				<xsl:with-param name="figTitle">
					<xsl:value-of select="$v00355"/>
					<xsl:text>&#160;</xsl:text>
					<xsl:value-of select="@count"/>
					<xsl:text>&#160;-&#160;</xsl:text>
					<xsl:value-of select="./title"/>
				</xsl:with-param>
			</xsl:apply-templates>
			<xsl:apply-templates select="legend">
				<xsl:with-param name="figureCount" select="@count"/>
			</xsl:apply-templates>
			<div style="padding-top:0.8em; padding-bottom:1.1em">
				<!--<xsl:call-template name="t_changeMarker"/>-->
				<xsl:if test="parent::sheet/@change='ADD'">
					<xsl:attribute name="class">changeMarker</xsl:attribute>
				</xsl:if>
				<xsl:value-of select="$v00355"/>
				<xsl:text>&#160;</xsl:text>
				<xsl:value-of select="@count"/>
				<xsl:text>&#160;-&#160;</xsl:text>
				<xsl:value-of select="./title"/>
			</div>
		</div>
	</xsl:template>
	<!--===============================================-->
	<!--2012 <xsl:template match="graphic">
		<xsl:param name="figCount" select="1"/>
		<xsl:param name="figTitle"/>
		<script> addFigure('<xsl:value-of select="@boardno|@infoEntityIdent"/>'); </script>
		<img class="figureLinkGraphic"  hspace="10" vspace="10" name="figureGraphicIcon">
			<xsl:attribute name="alt"><xsl:value-of select="substring-before(@boardno|@infoEntityIdent,'.')"/></xsl:attribute>
			<xsl:attribute name="id"><xsl:value-of select="@boardno|@infoEntityIdent"/></xsl:attribute>
			<xsl:attribute name="src">images/dmodule/image.gif</xsl:attribute>
			<xsl:attribute name="boardno"><xsl:value-of select="@boardno|@infoEntityIdent"/></xsl:attribute>
			<xsl:attribute name="onclick">
				updateLegendDiv('<xsl:value-of select="$figCount"/>', 'mainForm:legendDiv');
				graphicTitle.updateTitleDiv('<xsl:value-of select="$figCount"/>','<xsl:value-of select="@graphicSheetCountJS"/>', 'mainForm:graphicTitleDiv');
				loadImage('<xsl:value-of select="@boardno|@infoEntityIdent"/>','<xsl:value-of select="$Publication"/>','<xsl:value-of select="$ISOViewInstalled"/>');
			</xsl:attribute>
		</img>
		<xsl:variable name="figureSheetNumberText">
			<xsl:if test="@graphicSheetDisplayNumber and @graphicSheetTotal">
				<xsl:call-template name="sheetNumberingText">
					<xsl:with-param name="sheetCount" select="@graphicSheetDisplayNumber"/>
					<xsl:with-param name="sheetTotal" select="@graphicSheetTotal"/>
				</xsl:call-template>
			</xsl:if>
		</xsl:variable>
		<div class="sheet">
			<xsl:call-template name="createLinkAnchor"/>
			<xsl:call-template name="t_inlineApplicability">
				<xsl:with-param name="annotation">
					<xsl:value-of select="$v00034"/>:&#160;</xsl:with-param>
			</xsl:call-template>
			<xsl:value-of select="$figureSheetNumberText"/>
			
		</div>
		<xsl:variable name="graphicTitleText">
			<xsl:value-of select="$figTitle"/>
			<xsl:if test="$figureSheetNumberText != '' ">
				<xsl:text>&#160;&#40;</xsl:text>
				<xsl:value-of select="$figureSheetNumberText"/>
				<xsl:text>&#41;</xsl:text>
			</xsl:if>
		</xsl:variable>
		<script>
			graphicTitle.add('<xsl:value-of select="$figCount"/>',
				'<xsl:value-of select="@graphicSheetCountJS"/>',
				'<xsl:value-of select="$graphicTitleText"/>');
		</script>
		<xsl:apply-templates/>
	</xsl:template>-->
	<xsl:template match="graphic">
		<!--
			<script> addFigure('<xsl:value-of select="@boardno"/>'); </script>
		-->
		<img class="figureLinkGraphic">
			<xsl:attribute name="alt">
				<xsl:value-of select="substring-before(@infoEntityIdent,'.')"/>
			</xsl:attribute>
			<xsl:attribute name="id">
				<xsl:value-of select="@infoEntityIdent"/>
			</xsl:attribute>
			<!--
				<xsl:attribute name="src">
					<xsl:value-of select="@boardno"/>
				</xsl:attribute>
			-->
			<!--xml中boardno 的值改为 infoEntityIdent （SNS码）-->
			<xsl:attribute name="boardno">
				<xsl:value-of select="@infoEntityIdent"/>
			</xsl:attribute>
			<xsl:attribute name="onclick">
				<xsl:text>window.external.ShowMultimedia('</xsl:text>
				<xsl:value-of select="@infoEntityIdent"/>
				<xsl:text>');</xsl:text>
			</xsl:attribute>
			<!--
				<xsl:attribute name="onclick">
					loadImage('<xsl:value-of select="@boardno"
						/>','<xsl:value-of select="$Publication"/>','<xsl:value-of
						select="$ISOViewInstalled"/>')
				</xsl:attribute>
			-->
		</img>
		<script>
		    var boardNo = "<xsl:value-of select="@infoEntityIdent"/>";
			document.getElementById("<xsl:value-of select="@infoEntityIdent"/>").src = lessonPath+"image/image.gif";
		</script>
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template name="sheetNumberingText">
		<xsl:param name="sheetCount" select="1"/>
		<!-- default value -->
		<xsl:param name="sheetTotal" select="1"/>
		<!-- default value -->
		<xsl:value-of select="$v00860"/>
		<!-- Sheet -->
		<xsl:text>&#160;</xsl:text>
		<xsl:value-of select="format-number($sheetCount, '####')"/>
		<!-- remove leading zeros  from sheet number, possible 9999 should be enough-->
		<xsl:text>&#160;</xsl:text>
		<xsl:value-of select="$v01111"/>
		<!-- of -->
		<xsl:text>&#160;</xsl:text>
		<xsl:value-of select="format-number($sheetTotal,'####')"/>
		<!-- remove leading zeros  from sheet total, possible 9999 should be enough-->
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="rfa">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="legend">
		<xsl:param name="figureCount" select="1"/>
		<xsl:element name="div">
			<xsl:attribute name="id"><xsl:text>figLegendDiv-</xsl:text><xsl:value-of select="$figureCount"/></xsl:attribute>
			<xsl:attribute name="style">
					display:none
				</xsl:attribute>
			<div style="width:95%">
				<xsl:apply-templates/>
			</div>
		</xsl:element>
		<xsl:variable name="content">
			<xsl:apply-templates/>
		</xsl:variable>
		<script>
				storeLegend('<xsl:value-of select="$figureCount"/>', document.getElementById("figLegendDiv-<xsl:value-of select="$figureCount"/>").innerHTML);
				removeElementById("figLegendDiv-<xsl:value-of select="$figureCount"/>");
			</script>
	</xsl:template>
	<!--===============================================-->
	<xsl:template name="ParamLegendContent">
		<xsl:param name="boardno"/>
		<xsl:if test="//multimediaobject[@boardno=$boardno] or //multimediaObject[@infoEntityIdent=$boardno]">
			<table>
				<xsl:for-each select="//multimediaobject|//multimediaObject">
					<xsl:if test="@boardno=$boardno or @infoEntityIdent=$boardno">
						<tr>
							<td>
								<a href="#">
									<xsl:attribute name="onclick">showHomeView();playAnimation('<xsl:value-of select="param/@paramid | parameter/@parameterIdent"/>', '<xsl:value-of select="param/@paramname | parameter/@parameterName"/>', '<xsl:value-of select="param/@paramvalue | parameter/@parameterValue"/>');</xsl:attribute>
									<xsl:value-of select="param/@paramname"/>
									<xsl:value-of select="parameter/@parameterName"/>
								</a>
							</td>
						</tr>
					</xsl:if>
				</xsl:for-each>
			</table>
		</xsl:if>
	</xsl:template>
	<!--===============================================-->
</xsl:stylesheet>
