<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<!--===============================================-->
	<xsl:strip-space elements="*"/>
	<xsl:template match="figure">
		<!-- make a div for the dmtoc link-->
		<xsl:call-template name="T_makeDMTocLink"/>
		<!-- xref anchor-->
		<xsl:call-template name="createLinkAnchor"/>
		<center>
			<xsl:apply-templates select="applic"/>
			<xsl:apply-templates select="graphic">
				<xsl:with-param name="figCount" select="@count"/>
				<xsl:with-param name="figTitle">
					<!--<xsl:value-of select="$v00355"/>
					<xsl:text>图</xsl:text>
					<xsl:text>&#160;</xsl:text>
					<xsl:value-of select="@count"/>
					<xsl:text>&#160;-&#160;</xsl:text>
					<xsl:value-of select="./title"/>-->
				</xsl:with-param>
			</xsl:apply-templates>
			<xsl:apply-templates select="legend">
				<xsl:with-param name="figureCount" select="@count"/>
			</xsl:apply-templates>
		   <div style="padding-top:0.2em; padding-bottom:0.2em;font-size:10.5pt;font-family:@Arial;">
			 <xsl:value-of select="substring-before(./graphic/@boardno|./graphic/@infoEntityIdent,'.')"/>
		   </div>
			<div class="figure">
				<xsl:call-template name="t_changeMarker"/>
				<xsl:if test="parent::sheet/@change='ADD'">
					<xsl:attribute name="class">changeMarker</xsl:attribute>
				</xsl:if>
				<!--<xsl:value-of select="$v00355"/>-->
				<xsl:text>图</xsl:text>
				<!--<xsl:text>&#160;</xsl:text>-->
				<xsl:value-of select="@count"/>
				<xsl:text>&#160;</xsl:text>
				<xsl:value-of select="./title"/>
			</div>
		</center>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="graphic">
		<xsl:param name="figCount" select="1"/>
		<xsl:param name="figTitle"/>
		<xsl:call-template name="t_inlineApplicability">
			<xsl:with-param name="annotation">
				<xsl:value-of select="$v00034"/>:&#160;</xsl:with-param>
		</xsl:call-template>
		<xsl:variable name="boardnum">
			<xsl:value-of select="translate(@boardno|@infoEntityIdent,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')"/>
		</xsl:variable>
		<script> addFigure('<xsl:value-of select="@boardno|@infoEntityIdent"/>'); </script>
		<img class="figureLinkGraphic" hspace="10" vspace="10" name="figureGraphicIcon">
			<xsl:attribute name="alt"><xsl:value-of select="substring-before(@boardno|@infoEntityIdent,'.')"/></xsl:attribute>
			<!--<xsl:attribute name="id"><xsl:value-of select="@boardno|@infoEntityIdent"/></xsl:attribute>-->
			<xsl:attribute name="id"><xsl:value-of select="../@id"/></xsl:attribute>
			<xsl:attribute name="src">ShowSmallImage</xsl:attribute>
			<!--<xsl:attribute name="src"><xsl:choose><xsl:when test="contains($boardnum, '.CGM')">images/cgm.gif</xsl:when>
			<xsl:when test="contains($boardnum, '.WRL')">images/wrl.gif</xsl:when>
			<xsl:when test="contains($boardnum, '.ISO')">images/iso.gif</xsl:when>
			<xsl:otherwise>images/3d.gif</xsl:otherwise></xsl:choose></xsl:attribute>images/dmodule/image.gifBinaryResourceStreamer?pub=<xsl:value-of select="$Publication"/>&amp;string=<xsl:value-of select="@boardno|@infoEntityIdent"/>-->
			<xsl:attribute name="boardno"><xsl:value-of select="@boardno|@infoEntityIdent"/></xsl:attribute>
			<xsl:attribute name="onclick">
				showMultimediaInfo('<xsl:value-of select="@infoEntityIdent"/>');
			</xsl:attribute>
			<!--<xsl:attribute name="width">100px</xsl:attribute>-->
		</img>
		<!-- 
		<script>
			var borderno="<xsl:value-of select="@boardno|@infoEntityIdent"/>";
			var ext = borderno.substring(borderno.length-4,borderno.length).toUpperCase();
			var control=document.getElementById("<xsl:value-of select="@boardno|@infoEntityIdent"/>");
			switch(ext)
			{
			    case ".CGM":
				   control.src = lessonPath+"image/cgm.gif";
				   break;
				case ".WRL":
				   control.src = lessonPath+"image/wrl.gif";
				   break;
				case ".ISO":
				   control.src = lessonPath+"image/iso.gif";
				   break;
				default :
				   control.src = lessonPath+"image/image.gif";
				   break; 
			}
		</script>
		 -->
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
			<!-- Output sheet numbering to the content section -->
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
<!--===================20131031 start============================-->
<xsl:template match="deflist|definitionList">
		<xsl:param name="showLegend">1</xsl:param>
		<xsl:call-template name="t_inlineApplicability">
			<xsl:with-param name="annotation">
				<xsl:value-of select="$v00034"/>:&#160;</xsl:with-param>
		</xsl:call-template>
		<div>
			<xsl:choose>
				<xsl:when test="ancestor::legend">
					<xsl:if test="$showLegend=1">
						<xsl:if test="ancestor::foldout">
							<xsl:attribute name="break-before">page</xsl:attribute>
						</xsl:if>
						<div alternate-font="黑体" font-weight="bold" font-size="10.5pt" text-align="left" keep-with-next="always" line-height="1.5">
						图注：
						</div>
						<table width="100%" font-family="宋体" font-size="8.5pt" class="tableBorders">
               <!--<tr>
							<td width="5%" class="bottomBorders"/>序号</td>
							<td width="20%" class="bottomBorders"/>定义</td>
							<td width="5%" class="bottomBorders"/>序号</td>
							<td width="20%" class="bottomBorders"/>定义</td>
							<td width="5%" class="bottomBorders"/>序号</td>
							<td width="20%" class="bottomBorders"/>定义</td>
							<td width="5%" class="bottomBorders"/>序号</td>
							<td width="20%" class="bottomBorders"/>定义</td>
               </tr>-->
								<xsl:for-each select="definitionListItem">
									<xsl:if test="position() mod 4 = 1">
										<tr>											
                         <xsl:call-template name="T_LegendNote">												
                           <xsl:with-param name="currentNode" select="."/>
                           <xsl:with-param name="pIndex" select="position()"/>					
                          </xsl:call-template>
										</tr>
									</xsl:if>
								</xsl:for-each>
						</table>
						<div>&#160;</div>
					</xsl:if>
				</xsl:when>
				<xsl:otherwise>
					<div text-align="center" alternate-font="黑体" font-weight="bold" font-size="10.5pt" margin-bottom="4mm" keep-with-next="always">
						<xsl:value-of select="./title"/>
					</div>
					<table width="150mm" font-family="宋体" font-size="10.5pt" text-align="left" class="tableBorders">
						<!--上边框-->
						<xsl:attribute name="border-top-width.conditionality">retain</xsl:attribute>
						<xsl:attribute name="border-top-width.length">1pt</xsl:attribute>
						<xsl:attribute name="border-top-style">solid</xsl:attribute>
						<!--下边框-->
						<xsl:attribute name="border-bottom-width.conditionality">retain</xsl:attribute>
						<xsl:attribute name="border-bottom-width.length">1pt</xsl:attribute>
						<xsl:attribute name="border-bottom-style">solid</xsl:attribute>
						<tr>
						<td></td>
             <td></td>
             </tr>
						<body>
							<!--<tr>
                   <td>
									<xsl:attribute name="border-bottom">solid black 1pt</xsl:attribute>
						术&#160;&#160;语
								 </td>
								 <td>
									<xsl:attribute name="border-bottom">solid black 1pt</xsl:attribute>
						定&#160;&#160;义
								</td>
							</tr>-->
							<xsl:apply-templates select="term |.//listItemTerm | definitionListHeader "/>
						</body>
					</table>
				</xsl:otherwise>
			</xsl:choose>
		</div>
	</xsl:template>
	<!--=========================图注=============================-->
	<xsl:template name="T_LegendNote">
		<!--<xsl:template match="definitionListItem">-->
		<xsl:param name="currentNode">
			<xsl:value-of select="."/>
		</xsl:param>
		<xsl:param name="pIndex">1</xsl:param>
		<xsl:choose>
			<xsl:when test="$pIndex mod 4 = 1">
				<td style="width:5%;" class="bottomBorders">
						<xsl:value-of select="$currentNode/listItemTerm"/>
				</td>
				<td style="width:20%;" class="bottomBorders">
						<xsl:value-of select="$currentNode/listItemDefinition"/>
				</td>
				<xsl:choose>
					<xsl:when test="..//definitionListItem[position()=$pIndex+1]">
						<xsl:call-template name="T_LegendNote">
							<xsl:with-param name="currentNode" select="..//definitionListItem[position()=$pIndex+1]"/>
							<xsl:with-param name="pIndex" select="$pIndex+1"/>
						</xsl:call-template>
					</xsl:when>
					<xsl:otherwise>
						<td style="width:5%;" class="bottomBorders">
						</td>
						<td style="width:20%;" class="bottomBorders">
						</td>
						<td style="width:5%;" class="bottomBorders">
						</td>
						<td style="width:20%;" class="bottomBorders">
						</td>
						<td style="width:5%;" class="bottomBorders">
						</td>
						<td style="width:20%;" class="bottomBorders">
						</td>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:when>
			<xsl:when test="$pIndex mod 4 = 2">
				<td style="width:5%;" class="bottomBorders">
						<xsl:value-of select="$currentNode/listItemTerm"/>
				</td>
				<td style="width:20%;" class="bottomBorders">
						<xsl:value-of select="$currentNode/listItemDefinition"/>
				</td>
				<xsl:choose>
					<xsl:when test="..//definitionListItem[position()=$pIndex+1]">
						<xsl:call-template name="T_LegendNote">
							<xsl:with-param name="currentNode" select="..//definitionListItem[position()=$pIndex+1]"/>
							<xsl:with-param name="pIndex" select="$pIndex+1"/>
						</xsl:call-template>
					</xsl:when>
					<xsl:otherwise>
						<td style="width:5%;" class="bottomBorders">
							
						</td>
						<td style="width:20%;" class="bottomBorders">
						
						</td>
						<td style="width:5%;" class="bottomBorders">
						
						</td>
						<td style="width:20%;" class="bottomBorders">
						
						</td>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:when>
			<xsl:when test="$pIndex mod 4 = 3">
				<td style="width:5%;" class="bottomBorders">
						<xsl:value-of select="$currentNode/listItemTerm"/>
				</td>
				<td style="width:20%;" class="bottomBorders">
						<xsl:value-of select="$currentNode/listItemDefinition"/>
				</td>
				<xsl:choose>
					<xsl:when test="..//definitionListItem[position()=$pIndex+1]">
						<xsl:call-template name="T_LegendNote">
							<xsl:with-param name="currentNode" select="..//definitionListItem[position()=$pIndex+1]"/>
							<xsl:with-param name="pIndex" select="$pIndex+1"/>
						</xsl:call-template>
					</xsl:when>
					<xsl:otherwise>
						<td style="width:5%;" class="bottomBorders">
						
						</td>
						<td style="width:20%;" class="bottomBorders">
						
						</td>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:when>
			<xsl:otherwise>
				<td style="width:5%;" class="bottomBorders">
						<xsl:value-of select="$currentNode/listItemTerm"/>
				</td>
				<td style="width:20%;" class="bottomBorders">
						<xsl:value-of select="$currentNode/listItemDefinition"/>
				</td>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
<!--===================20131031 end============================-->



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
