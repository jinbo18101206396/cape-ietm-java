<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<!--======================机组=========================-->
	<xsl:include href="wcnp.xsl"/>
	<xsl:include href="crewWCN.xsl"/>
	<xsl:include href="crewMember.xsl"/>
	<!--===============================================-->
	<xsl:template match="acrw|dmodule/content/crew">
		<!-- import css first -->
		<xsl:call-template name="importCSS"/>
		<div class="techTitle">
			<!--<xsl:value-of select="$v00188"/>-->
      机组人员操作信息
		</div>
		<xsl:apply-templates/>
		<xsl:call-template name="initOutPutArea"/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="crewRefCard">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="descacrw | descrCrew | drill | crewDrill | procd/para | step | crewDrillStep | frc | crewRefCard | subdrill | subCrewDrill | challrsp | challengeAndResponse">
		<xsl:apply-templates/>
	</xsl:template>
	<!--====================2012定点样式范例===========================-->
	<!-- <xsl:template match="drill/title | subdrill/title|crewDrill/title|subCrewDrill/title">
        <div class="drillTitle">
            <xsl:apply-templates/>
        </div>
    </xsl:template>-->
	<!-- =====================================================================	活动标题-->
	<xsl:template match="drill/title | crewDrill/title">
		<div class="crewDrillTitle">
			<xsl:variable name="pmodule_no">
				<xsl:choose>
					<xsl:when test="parent::pmentry/parent::PRINTSERVICES"/>
					<xsl:otherwise>
						<xsl:number count="pmentry" format="1" from="PRINTSERVICES/pmentry" level="multiple"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:variable>
			<!--<xsl:value-of select="$pmodule_no"/>.--><xsl:number count="crewDrill" from="crewRefCard" format="1" level="multiple"/>
			<xsl:text> </xsl:text>
			<xsl:apply-templates/>
		</div>
	</xsl:template>
	<!-- =====================================================================	子活动标题-->
	<xsl:template match="subdrill/title | subCrewDrill /title">
		<xsl:variable name="pmodule_no">
			<xsl:choose>
				<xsl:when test="parent::pmentry/parent::PRINTSERVICES"/>
				<xsl:otherwise>
					<xsl:number count="pmentry" format="1" from="PRINTSERVICES/pmentry" level="multiple"/>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<div class="crewDrillTitle">
			<!--<xsl:value-of select="$pmodule_no"/>.--><xsl:number count="crewDrill" from="crewRefCard" format="1" level="multiple"/>.<xsl:number count="subCrewDrill" format="1" level="multiple"/>
			<xsl:text> </xsl:text>
			<xsl:apply-templates/>
		</div>
	</xsl:template>
	<!-- ===================================================================== -->
	<xsl:template match="elseIf">
		<xsl:value-of select="$v00284"/>
		<xsl:apply-templates/>
	</xsl:template>
	<!-- ===================================================================== -->
	<xsl:template match="if">
		<xsl:apply-templates/>
	</xsl:template>
	<!-- ===================================================================== -->
	<!--2012 add start 额外添加段落样式
	<xsl:template match="crewDrill/para | subCrewDrill/para">
		<div>
			<xsl:attribute name="class">
			<xsl:call-template name="t_getParaCss">
			<xsl:with-param name="depth">
			<xsl:value-of select="count(ancestor-or-self::subCrewDrill)"/></xsl:with-param></xsl:call-template>
			</xsl:attribute>
			
			<span class="paraNumberTitleIndent">
				<xsl:apply-templates/>
			</span>
		</div>
	</xsl:template>-->
	<!--end
  <xsl:template match="crewDrillStep">
    
  </xsl:template>-->
  <!--=====================2012加上crewDrillStep=========== name="T_StepParaContent"===============-->
	<xsl:template match="challeng | challenge | procd | crewProcedureName/para | step/title | crewDrillStep/title">
		<div class="StepParaContent">
		<xsl:variable name="indent">
            <xsl:choose>
					  <xsl:when test="self::challeng or self::challenge">
					  <xsl:value-of select="(7*(count(ancestor::elseIf)))  + (7*(count(ancestor::elseif)))  + (7*(count(ancestor::step))) + (7*(count(ancestor::crewDrillStep)-1)) + (7*(count(ancestor::if)-1) + (7*(count(ancestor::case))) + (7*(count(ancestor::caseCond)))  )+7"/>
					  </xsl:when>
					  <xsl:when test="(self::procd or self::crewProcedureName or self::title or self::para) and count(ancestor::crewDrillStep)>1">
					  <xsl:value-of select="3.5*(count(ancestor::if  | ancestor::case |  ancestor::caseCond | ancestor::elseif | ancestor::elseIf | ancestor::step[not(position()=1)] | ancestor::crewDrillStep[not(position()=1)])-1)+7"/></xsl:when>
					  <xsl:otherwise>7px</xsl:otherwise>
			</xsl:choose>
        </xsl:variable>
			<div>
			<xsl:attribute name="style">margin-left: <xsl:value-of select="$indent"/>px; width:
            100%;</xsl:attribute>
            
				<table>
				<tr>
					<td>
						<div>
							<!-- do not show the count value if the step has a title-->
							<!-- the count value goes next the title -->
							<xsl:choose>
                <xsl:when test="(self::title or not(preceding-sibling::title)) and
								not(preceding-sibling::para or preceding-sibling::challeng or preceding-sibling::challenge or preceding-sibling::procd) ">
									<xsl:variable name="stepLevel">
										<xsl:value-of select="count(ancestor::crewDrillStep)"/>
									</xsl:variable>
									<xsl:choose>
										<xsl:when test="$stepLevel=1">
											<xsl:number count="crewDrillStep" format="(1)"/>
										</xsl:when>
										<xsl:when test="$stepLevel=2">
											<xsl:number count="crewDrillStep" format="a."/>
										</xsl:when>
										<xsl:when test="$stepLevel=3">
											<xsl:number count="crewDrillStep" format="(a)"/>
										</xsl:when>
										<xsl:when test="$stepLevel=4">
											<xsl:number count="crewDrillStep" format="1)"/>
										</xsl:when>
										<xsl:when test="$stepLevel=5">
											<xsl:number count="crewDrillStep" format="a)"/>
										</xsl:when>
									</xsl:choose>
									<xsl:value-of select="ancestor::step[1]/@count|ancestor::crewDrillStep[1]/@count"/>
                </xsl:when>
                <xsl:otherwise>&#160;</xsl:otherwise>
							</xsl:choose>
						</div>
					</td>
					<td>
						<div>
							<xsl:apply-templates/>
						</div>
					</td>
				</tr>
				</table>
			</div>
		</div>
	</xsl:template>

	<!--<xsl:template match="step/para|crewDrillStep/para">
		<xsl:call-template name="T_StepParaContent"/>
	</xsl:template>-->
	<!-- ===================================================================== -->
	<xsl:template match="condit | crewStepCondition">
	<xsl:variable name="indent">
            <xsl:value-of select="10*(count(ancestor::if | ancestor::case | ancestor::step | ancestor::elseif | ancestor::caseCond | ancestor::elseIf | ancestor::crewDrillStep))+10"/>
        </xsl:variable>
		<div>
		<xsl:attribute name="style">margin-left: <xsl:value-of select="$indent"/>mm; </xsl:attribute>
			<xsl:choose>
				<xsl:when test="parent::if">
					<xsl:value-of select="$v00405"/>&#160;</xsl:when>
				<xsl:when test="parent::elseif | parent::elseIf">
					<xsl:value-of select="$v00284"/>&#160;</xsl:when>
			</xsl:choose>
			<xsl:apply-templates/>
		</div>
	</xsl:template>
	<!-- ===================================================================== -->
	<xsl:template match="subdrill|subCrewDrill">
		<div>
			<xsl:apply-templates/>
		</div>
	</xsl:template>
	<!-- ===================================================================== -->
	<xsl:template match="challengeAndResponse">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="challeng|challenge">
		<!--Table set less than 100% as the table is indented and would otherwise go off the page.-->
		<xsl:variable name="indent">
			<xsl:value-of select="(count(ancestor::if | ancestor::case | ancestor::elseif | ancestor::step | ancestor::caseCond | ancestor::elseIf | ancestor::crewDrillStep)*8) - 10"/>
		</xsl:variable>
		<div>
			<xsl:attribute name="style">margin-left: <xsl:value-of select="$indent"/>px; width: 100%</xsl:attribute>
			<!-- table encloses challenge stuff-->
			<!-- manditory inline styles are in place to ensure correct widths of cells -->
			<!-- tables used as you need a variable width cell to contain 'leader' dots-->
			<!-- once you can use max-width or display:table-cell in IE we're laughing-->
			<table cellpadding="0" cellspacing="2" border="0" style="width: 100%">
				<tr valign="top">
					<td>
						<!-- calculate the indent -->
						<xsl:attribute name="width">30px;</xsl:attribute>
					</td>
					<td width="65px" align="right">
						<!--If the step is a substep, count the position in the current context and display with an alphabetical count, otherwise
							use the normal step count-->
						<xsl:variable name="level">
							<xsl:value-of select="count(ancestor::step | ancestor::crewDrillStep)"/>
						</xsl:variable>
						<xsl:if test="(name(.) ='title' or not(preceding-sibling::title)) and not(preceding-sibling::para or preceding-sibling::challeng or preceding-sibling::procd or parent::challrsp/preceding-sibling::procd or parent::challrsp/preceding-sibling::challeng or parent::challrsp/preceding-sibling::para)">
							<xsl:choose>
								<xsl:when test="$level mod 2= 0">
								<xsl:number count="crewDrillStep" from="crewRefCard" format="(1)" level="single"/>
									<!--<xsl:choose>
										<xsl:when test="parent::step | parent::crewDrillStep">
											<xsl:number count="crewDrillStep" from="crewRefCard" format="a" level="single"/>. </xsl:when>
										<xsl:otherwise>
											<xsl:number count="crewDrillStep" from="crewRefCard" format="a" level="single"/>. </xsl:otherwise>
									</xsl:choose>-->
								</xsl:when>
								<xsl:otherwise>
								<xsl:number count="crewDrillStep" from="crewRefCard" format="a" level="single"/>
									<!--<xsl:choose>
										<xsl:when test="parent::step | parent::crewDrillStep">
											<xsl:number count="crewDrillStep" from="crewRefCard" format="(1)" level="single"/>. </xsl:when>
										<xsl:otherwise>
											<xsl:number count="crewDrillStep" from="crewRefCard" format="(1)" level="single"/>. </xsl:otherwise>
									</xsl:choose>-->
								</xsl:otherwise>
							</xsl:choose>
						</xsl:if>
					</td>
					<td width="30px">
						<!-- Spacer -->
					</td>
					<td class="dots">
						<!-- put in the challange -->
						<span class="item">
							<xsl:value-of select="./para"/>
						</span>
					</td>
					<td class="challengCrewMember">
						<xsl:for-each select="../crew/crewmem">
							<xsl:call-template name="crewmemTerm"/>
						</xsl:for-each>
					</td>
					<td class="response">
						<xsl:call-template name="t_inlineApplicability"/>
						<xsl:for-each select="../response|../response">
							<xsl:apply-templates/>
						</xsl:for-each>
					</td>
					<!-- padding so text doesn't run off screen -->
					<td width="15%">&#160;</td>
				</tr>
			</table>
		</div>
	</xsl:template>
	<!--===============================================-->
	<!-- 
		Overrides the default para template which would otherwise cause the response to be output in
		a para construct.
	 -->
	<xsl:template match="response/para">
		<xsl:call-template name="t_inlineApplicability">
			<xsl:with-param name="annotation">
				<xsl:value-of select="$v00034"/>:&#160;</xsl:with-param>
		</xsl:call-template>
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="response">
		<!-- Handled elsewhere -->
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="condit|caseCond">
		<div class="condit" style="background-color: red">
			<xsl:attribute name="style">margin-left: <xsl:value-of select="(count(ancestor::if | ancestor::case | ancestor::elseif | ancestor::step | ancestor::caseCond | ancestor::elseIf | ancestor::crewDrillStep)*40+15)"/>px;</xsl:attribute>
			<xsl:if test="parent::if">
				<xsl:value-of select="$v00405"/>&#160;</xsl:if>
			<xsl:if test="parent::elseif| parent::elseIf">
				<xsl:value-of select="$v00284"/>&#160;</xsl:if>
			<xsl:apply-templates/>
		</div>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="tabtitle">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="endmattr/para">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
</xsl:stylesheet>
