<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<!--===============================================-->
	<xsl:include href="wcnp.xsl"/>
	<xsl:include href="lists.xsl"/>
	<xsl:include href="prelreq.xsl"/>
	<xsl:include href="fig_tab.xsl"/>
	<!--===============================================-->
	<xsl:template match="afr | afi | faultReporting | faultIsolation">
		<!-- import css first -->
		<xsl:call-template name="importCSS"/>
		<div class="para0Title">
			<xsl:call-template name="T_AirFaultTitle"/>
		</div>
		<xsl:apply-templates/>
		<xsl:call-template name="initOutPutArea"/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="fault | dfault | detectedFault | ofault  | observedFault | ifault | isolatedFault | cfault | correlatedFault">
		<div class="para0Title">
			<xsl:call-template name="T_AirFaultTitle"/>
			<xsl:if test="@fcode | @faultCode">
				<xsl:value-of select="$v00337"/>: <xsl:value-of select="@fcode | @faultCode"/>
			</xsl:if>
		</div>
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="action">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="describe|faultDescr">
		<!--<div class="describeTitle">
			<xsl:value-of select="$v00238"/> : <span class="describeContent"><xsl:apply-templates/></span>
     </div>-->
		<!--故障描述与说明换行显示-->
		<div class="describeTitle">
			<table>
				<tbody>
					<tr>
						<td>
							<span class="describeContent">
								<xsl:value-of select="$v00238"/> :</span>
						</td>
					</tr>
					<tr>
						<td>
							<span class="describeContent">
								<xsl:apply-templates/>
							</span>
						</td>
					</tr>
				</tbody>
			</table>
		</div>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="isolatep | isolationMainProcedure">
		<div class="secondTitle">隔离过程</div><!--prelreqTitle-->
		<div id="isolatepID">
			<xsl:apply-templates/>
		</div>
	</xsl:template>
	<!--===============================================-->
	<!-- 
	<xsl:template match="isostep">
		<xsl:element name="div">
			<xsl:choose>
				<xsl:when test="position() = 2">
					<xsl:attribute name="style">display:inline</xsl:attribute>
				</xsl:when>
				<xsl:otherwise>
					<xsl:attribute name="style">display:none</xsl:attribute>
				</xsl:otherwise>
			</xsl:choose>
			<xsl:attribute name="id"><xsl:value-of select="translate(@id,'-','-')"/></xsl:attribute>
			<p>
				<xsl:apply-templates/>
			</p>
		</xsl:element>
	</xsl:template> 
	-->
	<!--===============================================-->
	<xsl:template match="isostep[1]|isolationStep[1]">
		<xsl:element name="div">
			<xsl:attribute name="style">display:inline;text-align:center;</xsl:attribute>
			<xsl:attribute name="id"><xsl:value-of select="translate(@id,'-','-')"/></xsl:attribute>
			<xsl:attribute name="title"><xsl:value-of select="./isolationStepQuestion"/></xsl:attribute>
			<p>
				<xsl:apply-templates/>
			</p>
		</xsl:element>
		<!--<xsl:apply-templates select="./isolationStepQuestion" mode="path_step"/>
		<div style="margin-left:2em;padding-top: 10pt;line-height:1.5;display:none;">
			<xsl:attribute name="id">path_<xsl:value-of select="translate(./@id,'-','-')"/></xsl:attribute>
			<p class="questionText">
				
			</p>
		</div>-->
	</xsl:template>
	<xsl:template match="isostep|isolationStep">
		<xsl:element name="div">
			<xsl:attribute name="style">display:none;text-align: center;</xsl:attribute>
			<xsl:attribute name="id"><xsl:value-of select="translate(@id,'-','-')"/></xsl:attribute>
			<xsl:attribute name="title"><xsl:value-of select="./isolationStepQuestion"/></xsl:attribute>
			<p>
				<xsl:apply-templates/>
			</p>
		</xsl:element>
		<!--<xsl:apply-templates select="./isolationStepQuestion" mode="path_step"/>
		<div style="margin-left:2em;padding-top: 10pt;line-height:1.5;display:none;">
			<xsl:attribute name="id">path_<xsl:value-of select="translate(./@id,'-','-')"/></xsl:attribute>
			<p class="questionText">
				<xsl:value-of select="./isolationStepQuestion"/>
			</p>
		</div>-->
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="action">
		<div class="action">
			<!--<p class="actionText">
				<xsl:value-of select="$v00012"/> : </p>-->
			<xsl:value-of select="$v00012"/> : <xsl:apply-templates/>
		</div>
		<xsl:call-template name="T_CloseTxt"/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="yesno | yesNoAnswer">
		<xsl:call-template name="T_yesno"/>
		<p style="text-align:center;width:400px;">
			<button id="backButton" style="width:50; height: 25; " onclick="GoBack()">
				<xsl:value-of select="$v00060"/>
			</button>
		</p>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="yesno[ancestor::isostep[not(preceding-sibling::isostep)]] | yesNoAnswer[ancestor::isolationStep[not(preceding-sibling::isolationStep)]]">
		<!-- 
	Match on yesno which is in the first isostep.
	Don't show a back button in the first step. 
        -->
		<xsl:call-template name="T_yesno"/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template name="T_yesno">
		<div class="yesno">
			<p>
				<xsl:apply-templates/>
			</p>
		</div>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="yes | yesAnswer">
		<div style="display:none;text-align: center;">
			<xsl:value-of select="translate(@refid | @nextActionRefId,'-','-')"/>
		</div>
		<button style="width ='50' ; height = '25';align:center; ">
			<xsl:attribute name="onclick">ShowNextStep('<xsl:value-of select="translate(@refid | @nextActionRefId,'-','-')"/>','<xsl:value-of select="$v01081"/>')</xsl:attribute>
			<xsl:value-of select="$v01081"/>
		</button> &#160; <xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="no | noAnswer">
		<div style="display:none;text-align: center;">
			<xsl:value-of select="translate(@refid | @nextActionRefId,'-','-')"/>
		</div>
		<button style="width ='50' ; height = '25' ;align:center;">
			<xsl:attribute name="onclick">ShowNextStep('<xsl:value-of select="translate(@refid | @nextActionRefId,'-','-')"/>','<xsl:value-of select="$v00577"/>')</xsl:attribute>
			<xsl:value-of select="$v00577"/>
		</button> &#160; <xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="question | isolationStepQuestion">
		<xsl:element name="span">
			<xsl:attribute name="class">seqItem</xsl:attribute>
			<xsl:attribute name="style">text-indent:2em;font-weight:bold;text-align:center;</xsl:attribute>
			<!--<p class="questionText">
				</p>-->
			<xsl:value-of select="$v00746"/> : <xsl:apply-templates/>
		</xsl:element>
	</xsl:template>
	<xsl:template match="question | isolationStepQuestion" mode="path_step">
		<div style="margin-left:2em;padding-top: 10pt;line-height:1.5;display:none;text-align:center;">
			<xsl:attribute name="id"><xsl:value-of select="translate(@id,'-','-')"/></xsl:attribute>
			<p class="questionText">
				<xsl:value-of select="$v00746"/> : </p>
			<xsl:apply-templates/>
		</div>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="sel-list | listOfChoices">
		<xsl:call-template name="T_sellist"/>
		<div id="BackButtonID" class="sellist">
			<button style="width:50 ; height : 25 " onclick="GoBack()">
				<xsl:value-of select="$v00060"/>
			</button>
		</div>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="sel-list[ancestor::isostep[not(preceding-sibling::isostep)]] | listOfChoices[ancestor::isolationStep[not(preceding-sibling::isolationStep)]]">
		<!-- 
			Match onsel-list which is in the first isostep.
			Don't  a back button in the first step. 
		-->
		<xsl:call-template name="T_sellist"/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template name="T_sellist">
		<p class="sellist">
			<select name="sellistID" style="width='*'">
				<xsl:attribute name="onchange">ShowNextStep(value,'<xsl:value-of select="./choice"/>')</xsl:attribute>
				<option value="empty">&#160;</option>
				<xsl:apply-templates/>
			</select>
			<br/>
			<br/>
		</p>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="choice">
		<div style="display:none;text-align: center;">
			<xsl:element name="option">
				<xsl:attribute name="value"><xsl:value-of select="translate(@refid | @nextActionRefId,'-','-')"/></xsl:attribute>
				<xsl:value-of select="text()"/>
			</xsl:element>
		</div>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="isoend | isolationProcedureEnd">
		<xsl:element name="div">
			<xsl:attribute name="style">display:none;text-align: center;</xsl:attribute>
			<xsl:attribute name="id"><xsl:value-of select="translate(@id,'-','-')"/></xsl:attribute>
			<!--<p>
				<xsl:apply-templates/>
				<xsl:call-template name="T_CloseTxt"/>
			</p>-->
			<xsl:apply-templates/>
		</xsl:element>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="detect | detectionInfo">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="locandrep">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="remarks">
		<div>
			<xsl:call-template name="t_inlineApplicability"/>
			<span class="remarkTitle">
				<xsl:value-of select="$v00785"/>:</span>
			<xsl:apply-templates/>
		</div>
	</xsl:template>
	<!--===============================================-->
	<xsl:template name="T_CloseTxt">
		<div class="closetxt">
			<p>
				
					<!--<xsl:value-of select="$v00159"/> : -->转到后续工作。
			</p>
			<!--<xsl:apply-templates select="//closetxt" mode="closeTxtStep"/>
			<br/>
			<br/>-->
			<p style="text-align:center;width:400px;">
				<button id="backButton" style="width:50; height: 25; " onclick="GoBack()">
					<xsl:value-of select="$v00060"/>
				</button>
			</p>
		</div>
		<script>initFault('<xsl:value-of select="translate(ancestor::isolationMainProcedure/isolationStep[1]/@id,'-','_')"/>');</script>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="closetxt" mode="closeTxtStep">
		<xsl:apply-templates/>
	</xsl:template>
	<xsl:template match="closetxt">
		<!-- DO Nothing, thais is handled by a moded template, to limit the close text's appearance  to the final step-->
	</xsl:template>
	<!--===============================================-->
	<xsl:template name="T_AirFaultItemCount">
		<!-- 
		This count will count all the refs that are not a direct child of content and srus and lrus that are not DIRECTLY preceeded by anything else, in other words it will only
		count the sru/lru that is first in a series of sru/lrus
	-->
		<xsl:number level="any" count="refs[not(parent::content)] | sru[not((preceding-sibling::*)[position()=last()][name()='sru'])] | lru[not((preceding-sibling::*)[position()=last()][name()='lru'])]"/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="lru[not((preceding-sibling::*)[position()=last()][name()='lru'])]">
		<div class="describeTitle">
			<xsl:value-of select="$v01104"/> : </div>
		<!-- call the common functionality required for an lru / sru table-->
		<xsl:call-template name="faultTable"/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="sru[not((preceding-sibling::*)[position()=last()][name()='sru'])]" name="faultTable">
		<!-- match on the sru that is either first in the sruitem or where its direct precvious sibling is not a sru-->
		<!-- we need an object around the table to enable tear off functionality, save in a var to spare us getting 
			this value more than once
	-->
		<xsl:call-template name="T_makeDMTocLink"/>
		<xsl:variable name="objectId">
			<xsl:value-of select="generate-id(.)"/>
		</xsl:variable>
		<xsl:element name="div">
			<xsl:attribute name="class">srulruTableArea</xsl:attribute>
			<xsl:call-template name="T_AirFaultTearOffTable">
				<xsl:with-param name="objectId">
					<xsl:value-of select="$objectId"/>
				</xsl:with-param>
				<xsl:with-param name="TableCount">
					<xsl:call-template name="T_AirFaultItemCount"/>
				</xsl:with-param>
				<xsl:with-param name="TableTitle">
					<xsl:call-template name="T_AirFaultTitle"/>
				</xsl:with-param>
			</xsl:call-template>
		</xsl:element>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="afr//refs | faultReporting//refs">
		<!-- match on the sru that is either first in the sruitem or where its direct precvious sibling is not a sru-->
		<!-- we need an object around the table to enable tear off functionality, save in a var to spare us getting 
			this value more than once
	-->
		<xsl:call-template name="T_makeDMTocLink"/>
		<xsl:variable name="objectId">
			<xsl:value-of select="generate-id(.)"/>
		</xsl:variable>
		<xsl:element name="div">
			<xsl:call-template name="T_AirFaultTearOffTable">
				<xsl:with-param name="objectId">
					<xsl:value-of select="$objectId"/>
				</xsl:with-param>
				<xsl:with-param name="TableCount">
					<xsl:call-template name="T_AirFaultItemCount"/>
				</xsl:with-param>
				<xsl:with-param name="TableTitle">
					<xsl:call-template name="T_AirFaultRefsTitles"/>
				</xsl:with-param>
			</xsl:call-template>
		</xsl:element>
	</xsl:template>
	<!--===============================================-->
	<xsl:template name="T_TearOffObject">
		<xsl:param name="objectId"/>
		<!-- Tear off table icon. -->
		<img src="./images/dmodule/Tear.gif" class="tearOff" id="tearOff">
			<xsl:attribute name="title"><xsl:value-of select="$v00914"/></xsl:attribute>
			<!-- Pass the table object into the displayTearOffTable function. -->
			<xsl:attribute name="onclick">prepTableForTearOff(getElementById('<xsl:value-of select="$objectId"/>'),'<xsl:value-of select="../@PositionNumberInDM"/>')
            </xsl:attribute>
		</img>
	</xsl:template>
	<!--===============================================-->
	<xsl:template name="T_PrintTableObject">
		<xsl:param name="objectId"/>
		<!-- Tear off table icon. -->
		<xsl:if test="$SingleDMView='false'">
			<img src="./images/dmodule/PrintTable.png" class="printTable" id="printTable">
				<xsl:attribute name="title"><xsl:value-of select="$v00708"/></xsl:attribute>
				<!-- Pass the table object into the displayTearOffTable function. -->
				<xsl:attribute name="onclick"> doTearOffPrint('<xsl:value-of select="$DMFileName"/>', '<xsl:value-of select="$Publication"/>', '<xsl:value-of select="@PositionNumberInDM"/>'); </xsl:attribute>
			</img>
		</xsl:if>
	</xsl:template>
	<!--===============================================-->
	<xsl:template name="T_AirFaultTearOffTable">
		<xsl:param name="TableTitle"/>
		<xsl:param name="TableCount"/>
		<xsl:param name="objectId"/>
		<span>
			<xsl:attribute name="style">height: expression(scrollHeight >=
                (Math.floor(document.body.clientHeight / 2)) ?
                (Math.floor(document.body.clientHeight / 2)) : 'auto')</xsl:attribute>
			<div>
				<xsl:attribute name="id"><xsl:value-of select="$objectId"/></xsl:attribute>
				<span>
					<span id="tableTitle">
						<table class="dr-pnl-h rich-panel-header tabletitle" width="100%">
							<tr>
								<td class="tearOff">
									<xsl:call-template name="T_TearOffObject">
										<xsl:with-param name="objectId">
											<xsl:value-of select="$objectId"/>
										</xsl:with-param>
									</xsl:call-template>
								</td>
								<td class="printTable">
									<xsl:call-template name="T_PrintTableObject">
										<xsl:with-param name="objectId">
											<xsl:value-of select="$objectId"/>
										</xsl:with-param>
									</xsl:call-template>
								</td>
								<td>
									<center>
										<xsl:call-template name="escape-apos">
											<xsl:with-param name="string">
												<xsl:value-of select="$v00907"/>&#160;&#160;<xsl:value-of select="$TableCount"/>.&#160;&#160;<xsl:value-of select="$TableTitle"/>
											</xsl:with-param>
										</xsl:call-template>
									</center>
								</td>
							</tr>
						</table>
					</span>
					<span width="100%">
						<!-- put table here-->
						<xsl:choose>
							<xsl:when test="name()='refs'">
								<table cellpadding="0" cellspacing="0" width="100%">
									<tr>
										<td>
											<xsl:call-template name="T_RefsTableHeader"/>
										</td>
									</tr>
									<tr>
										<td>
											<xsl:call-template name="T_RefsTableBody"/>
										</td>
									</tr>
								</table>
							</xsl:when>
							<xsl:when test="name()='lru'">
								<table cellpadding="0" cellspacing="0" width="100%">
									<tr>
										<td>
											<xsl:call-template name="T_MakeSruLruTable"/>
										</td>
									</tr>
									<tr>
										<td>
											<xsl:call-template name="T_MakeSruLruTableBody"/>
										</td>
									</tr>
								</table>
							</xsl:when>
							<xsl:when test="name()='sru'">
								<table cellpadding="0" cellspacing="0" width="100%">
									<tr>
										<td>
											<xsl:call-template name="T_MakeSruLruTable"/>
										</td>
									</tr>
									<tr>
										<td>
											<xsl:call-template name="T_MakeSruLruTableBody"/>
										</td>
									</tr>
								</table>
							</xsl:when>
						</xsl:choose>
					</span>
				</span>
			</div>
		</span>
	</xsl:template>
	<!--===============================================-->
	<xsl:template name="T_RefsTableHeader">
		<span class="headFootDiv" id="tableHeader">
			<!--<table cellpadding="0" cellspacing="0" class="tableHead" border="0" align="left" width="100%">-->
			<table cellpadding="0" cellspacing="0" border="0" align="left" width="100%" class="thead">
				<tr>
					<td width="50%">
						<xsl:value-of select="$v01101"/>
					</td>
					<td width="50%">
						<xsl:value-of select="$v00963"/>
					</td>
				</tr>
			</table>
		</span>
	</xsl:template>
	<!--===============================================-->
	<xsl:template name="T_RefsTableBody">
		<!--<span class="bodyTableDiv">-->
		<span class="tbodydiv" id="tableBody">
			<xsl:attribute name="style">height : 100px</xsl:attribute>
			<table cellpadding="0" cellspacing="0" border="0" width="100%">
				<xsl:apply-templates/>
			</table>
		</span>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="afr//norefs">
		<tr>
			<td width="50%">
				<xsl:value-of select="$v00585"/>
			</td>
			<td> </td>
		</tr>
	</xsl:template>
	<!--===============================================-->
	<xsl:template name="T_MakeSruLruTable">
		<!-- makes a generic table for lru and sru. Will only be fired off on the first
		lru or sru OR when the immeidiate previous sibling is not the same node name
		eg - not an sru when u are looking at sru
	-->
		<!--<span class="headFootDiv">-->
		<span id="tableHeader">
			<!--	<table cellpadding="0" cellspacing="0" class="tableHead" border="0" align="left" width="100%">-->
			<table cellpadding="0" cellspacing="0" border="0" align="left" width="100%" class="thead">
				<tr>
					<td width="40%">
						<xsl:value-of select="$v00583"/>
					</td>
					<td width="20%">
						<xsl:value-of select="$v00600"/>
					</td>
					<td width="20%">
						<xsl:value-of select="$v00191"/>
					</td>
					<td width="20%">
						<xsl:value-of select="$v00005"/>
					</td>
				</tr>
			</table>
		</span>
	</xsl:template>
	<!--===============================================-->
	<xsl:template name="T_MakeSruLruTableBody">
		<!--<span class="bodyTableDiv">-->
		<div class="tbodydiv" id="tableBody">
			<xsl:attribute name="style">height : 100px</xsl:attribute>
			<table cellpadding="0" cellspacing="0" border="0" width="100%" class="tbody">
				<xsl:call-template name="T_WriteSruLruDataToTableRow"/>
			</table>
		</div>
	</xsl:template>
	<!--===============================================-->
	<xsl:template name="escape-apos">
		<xsl:param name="string"/>
		<!-- create an $apos variable to make it easier to refer to -->
		<xsl:variable name="apos" select="&quot;&apos;&quot;"/>
		<xsl:choose>
			<!-- if the string contains an apostrophe... -->
			<xsl:when test="contains($string, $apos)">
				<!-- ... give the value before the apostrophe... -->
				<xsl:value-of select="substring-before($string, $apos)"/>
				<!-- ... the escaped apostrophe ... -->
				<xsl:text>\'</xsl:text>
				<!-- ... and the result of applying the template to the string after the apostrophe -->
				<xsl:call-template name="escape-apos">
					<xsl:with-param name="string" select="substring-after($string, $apos)"/>
				</xsl:call-template>
			</xsl:when>
			<!-- otherwise... -->
			<xsl:otherwise>
				<!-- ... just give the value of the string -->
				<xsl:value-of select="$string"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<!--===============================================-->
	<xsl:template name="T_WriteSruLruDataToTableRow">
		<!-- named template to write the sru data in current context to a table row.
		it will call itself for the next sru/lru at the same level when there is no other node 
		between them -->
		<!-- remeber the node name (sru or lru) so you know the what the you're looking for 
			in the name of the next sibling
			 -->
		<xsl:variable name="faultTagName">
			<xsl:value-of select="name(.)"/>
		</xsl:variable>
		<tr>
			<td width="40%">
				<xsl:apply-templates select="nomen | name"/>
			</td>
			<td width="20%">
				<xsl:apply-templates select="nsn"/>
			</td>
			<td width="20%">
				<xsl:apply-templates select="identno | identNumber"/>
			</td>
			<td width="20%">
				<xsl:apply-templates select="abbrev"/>
			</td>
		</tr>
		<!-- do the next sibling sru/lru if there is nothing in between-->
		<xsl:for-each select="(following-sibling::*)[1][name()=$faultTagName]">
			<xsl:call-template name="T_WriteSruLruDataToTableRow"/>
		</xsl:for-each>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="sru | lru">
		<!--Do Nothing -->
		<!-- A named template handles sru and lru-->
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="fcontext | faultContext">
		<div class="faultCodeTitle">
			<xsl:call-template name="T_AirFaultTitle"/>
		</div>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="test | faultIsolationTest">
		<!-- THIS IS NOT A DUMMY TEST TEMPLATE !! -->
		<div class="faultCodeTitle">
			<xsl:call-template name="T_AirFaultTitle"/>
		</div>
		<table border="0">
			<tr>
				<td width="10%">
					<xsl:value-of select="$v00929"/>:</td>
				<td>
					<xsl:value-of select="@type | @testType"/>
				</td>
			</tr>
			<tr>
				<td>
					<xsl:value-of select="$v01120"/>:</td>
				<td>
					<xsl:value-of select="@code | @testCode"/>
				</td>
			</tr>
			<!-- get data -->
			<xsl:for-each select="data | testParameters">
				<xsl:call-template name="T_GetData"/>
			</xsl:for-each>
		</table>
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template name="T_GetData">
		<!-- data- child of test -->
		<!-- data info to be included in a table created at test level-->
		<tr>
			<td>
				<xsl:value-of select="$v00211"/>:</td>
			<td>
				<xsl:value-of select="@from"/> - <xsl:value-of select="@to"/>
				<xsl:value-of select="@uom | @unitOfMeasure"/>
			</td>
		</tr>
	</xsl:template>
	<!--===============================================-->
	<!--===============================================-->
	<xsl:template match="closeup|closeRqmts">
		<xsl:call-template name="t_closeup"/>
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="closeup/refs">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="noclose">
		<xsl:value-of select="$v00585"/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="closereqs">
		<!--<xsl:call-template name="t_closeup"/>-->
		<xsl:apply-templates/>
	</xsl:template>
	<!--====================程序中的收尾工作===========================-->
	<xsl:template name="t_closeup">
		<div class="secondTitle">
			<!--<xsl:value-of select="$v00796" />-->
			后续工作
		</div>
	</xsl:template>
</xsl:stylesheet>
