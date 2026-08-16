<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<!--2012 xmlns:refdm="java:com.lbsltd.matrix2.content.RefDMExtensionFunctions?path=file:///E:/IETM4/IETP/IETP_Data_Update/xsl/java/" exclude-result-prefixes="refdm"-->
	<!--===============================================-->
	<xsl:variable name="iss4xpath" select="'//identAndStatusSection/dmAddress/dmIdent/dmCode'"/>
	<xsl:variable name="iss3xpath" select="'//dmodule/idstatus/dmaddres/dmc/*'"/>
	<xsl:variable name="iss4predicate" select="'[attribute::'"/>
	<xsl:variable name="iss3predicate" select="'['"/>
	<!--==========处理正文content=====================================-->
	<xsl:template match="content">
		<!-- set global js vars that are needed later -->
		<script type="javascript"> setSelectedDMFileName('<xsl:value-of select="$DMFileName"/>');
                setSelectedPublicationCode('<xsl:value-of select="$Publication"/>'); </script>
		<br/>
		<br/>
		<div class="maincontent">
		    <div class="techTitle"><!-- DM标题 -->
				<xsl:value-of select="ancestor::dmodule/identAndStatusSection/dmAddress/dmAddressItems/dmTitle/techName"/>-<xsl:value-of select="ancestor::dmodule/identAndStatusSection/dmAddress/dmAddressItems/dmTitle/infoName"/>
			</div>
			<xsl:if test=".//description|.//procedure|.//afi|.//proced|.//faultIsolation|.//ipc|.//crew">
				<xsl:if test=".//levelledPara">
					<!--================正文目录==================-->
					<xsl:call-template name="print_loc"/>
				</xsl:if>
				<xsl:if test=".//figure">
					<!--================插图目录==================-->
					<xsl:call-template name="print_lof"/>
				</xsl:if>
				<xsl:if test=".//table">
					<!--================表格目录==================-->
					<xsl:call-template name="print_lot"/>
				</xsl:if>
			</xsl:if>
			<br/>
			<xsl:apply-templates/>
		</div>
	</xsl:template>
	<!--======================正文目录=========================-->
	<xsl:template name="print_loc">
		<br/>
		<div class="para0Title">
				正文目录
			</div>
		<div align="left">
			<table width="100%" class="toc-table">
				<tbody>
					<tr>
						<td colspan="2"/>
					</tr>
					<xsl:choose>
						<xsl:when test=".//procedure">
							<tr>
								<td class="loclefttd">
									<span class="xrefLink">
								1</span>
								</td>
								<td class="locrighttd">
									<span class="para0Title">
										<xsl:attribute name="onclick">getPos("<xsl:value-of select="generate-id(./procedure/preliminaryRqmts)"/>")</xsl:attribute>
									操作准备
								</span>
								</td>
							</tr>
							<tr>
								<td class="loclefttd">
									<span class="xrefLink">
								1.1</span>
								</td>
								<td class="locrighttd">
									<span class="para0Title">
										<xsl:attribute name="onclick">getPos("<xsl:value-of select="generate-id(./procedure/preliminaryRqmts/reqCondGroup)"/>")</xsl:attribute>
									必要条件
								</span>
								</td>
							</tr>
							<tr>
								<td class="loclefttd">
									<span class="xrefLink">
								1.2</span>
								</td>
								<td class="locrighttd">
									<span class="para0Title">
										<xsl:attribute name="onclick">getPos("<xsl:value-of select="generate-id(./procedure/preliminaryRqmts/reqPersons)"/>")</xsl:attribute>
									人员要求
								</span>
								</td>
							</tr>
							<tr>
								<td class="loclefttd">
									<span class="xrefLink">
								1.3</span>
								</td>
								<td class="locrighttd">
									<span class="para0Title">
										<xsl:attribute name="onclick">getPos("<xsl:value-of select="generate-id(./procedure/preliminaryRqmts/reqSupportEquips)"/>")</xsl:attribute>
									保障设备
								</span>
								</td>
							</tr>
							<tr>
								<td class="loclefttd">
									<span class="xrefLink">
								1.4</span>
								</td>
								<td class="locrighttd">
									<span class="para0Title">
										<xsl:attribute name="onclick">getPos("<xsl:value-of select="generate-id(./procedure/preliminaryRqmts/reqSupplies)"/>")</xsl:attribute>
									消耗材料
								</span>
								</td>
							</tr>
							<tr>
								<td class="loclefttd">
									<span class="xrefLink">
								1.5</span>
								</td>
								<td class="locrighttd">
									<span class="para0Title">
										<xsl:attribute name="onclick">getPos("<xsl:value-of select="generate-id(./procedure/preliminaryRqmts/reqSpares)"/>")</xsl:attribute>
									备件
								</span>
								</td>
							</tr>
							<tr>
								<td class="loclefttd">
									<span class="xrefLink">
								1.6</span>
								</td>
								<td class="locrighttd">
									<span class="para0Title">
										<xsl:attribute name="onclick">getPos("<xsl:value-of select="generate-id(./procedure/preliminaryRqmts/reqSafety)"/>")</xsl:attribute>
									安全条件
								</span>
								</td>
							</tr>
							<tr>
								<td class="loclefttd">
									<span class="xrefLink">
								2</span>
								</td>
								<td class="locrighttd">
									<span class="para0Title">
										<xsl:attribute name="onclick">getPos("<xsl:value-of select="generate-id(./procedure/mainProcedure)"/>")</xsl:attribute>
									操作步骤
								</span>
								</td>
							</tr>
							<tr>
								<td class="loclefttd">
									<span class="xrefLink">
								3</span>
								</td>
								<td class="locrighttd">
									<span class="para0Title"><!--xrefLink-->
										<xsl:attribute name="onclick">getPos("<xsl:value-of select="generate-id(./procedure/closeRqmts)"/>")</xsl:attribute>
									收尾工作
								</span>
								</td>
							</tr>
						</xsl:when>
						<xsl:otherwise>
							  <xsl:for-each select=".//levelledPara" >
								  <tr>
									<td class="loclefttd">
										<!--<xsl:value-of select="./@ptc_level"/>-->
										<span class="xrefLink" style="width:5%;">
                      <xsl:attribute name="style">white-space:nowrap;</xsl:attribute>
											<xsl:attribute name="onclick">getPos("<xsl:value-of select="generate-id(.)"/>")</xsl:attribute>
											<xsl:number count="levelledPara" from="content" level="multiple" format="1.1.1.1.1"/>
										</span>
									</td>
									<td class="locrighttd" style="width:95%;">
										<span class="xrefLink">
                         <xsl:attribute name="style">white-space:nowrap;</xsl:attribute>
											<xsl:attribute name="onclick">getPos("<xsl:value-of select="generate-id(.)"/>")</xsl:attribute>
											<xsl:value-of select="./title"/>
										</span>
									</td>
								</tr>
							  </xsl:for-each>
						</xsl:otherwise>
					</xsl:choose>
				</tbody>
			</table>
		</div>
	</xsl:template>
	<!--======================插图目录=========================-->
	<xsl:template name="print_lof">
		<div class="para0Title">
				插图目录
			</div>
		<!--<center>-->
		<div align="left">
			<table width="60%" class="toc-table">
				<tbody>
					<tr>
						<td colspan="2"/>
					</tr>
					<xsl:for-each select=".//figure">
						<tr>
							<td class="loclefttd" style="width:5%;">
								<span class="xrefLink">
                  <xsl:attribute name="style">white-space:nowrap;</xsl:attribute>
									<xsl:attribute name="onclick">getPos("<xsl:value-of select="./@id"/>")</xsl:attribute>
									图&#160;&#160;<xsl:value-of select="@count"/>&#160;&#160;
								</span>
							</td>
							<td class="locrighttd" style="width:95%;">
								<span class="xrefLink">
                    <xsl:attribute name="style">white-space:nowrap;</xsl:attribute>
									<xsl:attribute name="onclick">getPos("<xsl:value-of select="./@id"/>")</xsl:attribute>
									<xsl:value-of select="./title"/>
								</span>
							</td>
						</tr>
					</xsl:for-each>
				</tbody>
			</table>
		<!--</center>--></div>
	</xsl:template>
	<!--======================表格目录=========================-->
	<xsl:template name="print_lot">
		<div class="para0Title">
				表格目录
			</div>
		<div align="left">
			<table width="60%" class="toc-table">
				<tbody>
					<tr>
						<td colspan="2"/>
					</tr>
					<xsl:for-each select=".//table">
						<tr>
							<td class="loclefttd" style="width:5%;">
								<span class="xrefLink">
                  <xsl:attribute name="style">white-space:nowrap;</xsl:attribute>
									<xsl:attribute name="onclick">getPos("<xsl:value-of select="./@id"/>")</xsl:attribute>
									表&#160;&#160;<xsl:number count="table" level="any" from="content" format="1"/>&#160;&#160;
								</span>
							</td>
							<td class="locrighttd" style="width:95%;">
								<span class="xrefLink">
                    <xsl:attribute name="style">white-space:nowrap;</xsl:attribute>
									<xsl:attribute name="onclick">getPos("<xsl:value-of select="./@id"/>")</xsl:attribute>
									<xsl:value-of select="./title"/>
								</span>
							</td>
						</tr>
					</xsl:for-each>
				</tbody>
			</table>
		</div>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="refs">
		<!-- 
        refs are not displayed at the moment. A modal panel is currently shown
         -->
		<div class="hidesection" id="dmrefs">Referenced Data Modules: <hr/>
			<xsl:apply-templates/>
			<hr/>
		</div>
		<!-- <xsl:apply-templates/> -->
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="norefs">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="rdsndrt">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="refdms">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="brexref/refdm">
		<!-- Suppress output -->
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="refdm|reqdm">
		<xsl:variable name="refDMTitle">
			<xsl:call-template name="t_formatDMC"/>
		</xsl:variable>
		<span>
			<xsl:attribute name="title"><xsl:value-of select="$v02457"/><xsl:value-of select="$refDMTitle"/></xsl:attribute>
			<xsl:attribute name="style">text-decoration: underline;color:
                rgb(0,0,255);cursor:pointer;</xsl:attribute>
			<!-- we do not link refdms in singledm mode  -->
			<xsl:if test="$SingleDMView='false'">
				<xsl:attribute name="onclick"> document.getElementById("mainForm:autoXref").value=
                        '<xsl:value-of select="translate(@target,'-','-')"/>';
                    Richfaces.showModalPanel('busyPanel'); refDmLink("<xsl:apply-templates mode="refdm"/>"); </xsl:attribute>
			</xsl:if>
			<xsl:if test="$SingleDMView='true'">
				<xsl:attribute name="onclick">alert('<xsl:value-of select="$v01155"/>')</xsl:attribute>
			</xsl:if>
			<xsl:value-of select="$refDMTitle"/>
		</span>
		<xsl:if test="./dmtitle"> &#160;<xsl:value-of select=".//techname"/> - <xsl:value-of select=".//infoname"/>
		</xsl:if>
	</xsl:template>
	<!--=====================手册DM内部引用==========================-->
	<xsl:template match="dmRef">
		<xsl:variable name="refDMTitle">
			<xsl:call-template name="t_formatIss4DMC"/>
		</xsl:variable>
		<span>
			<xsl:attribute name="title"><xsl:value-of select="$v02457"/><xsl:value-of select="$refDMTitle"/></xsl:attribute>
			<xsl:attribute name="style">text-decoration: underline;color:
                rgb(0,0,255);cursor:pointer;</xsl:attribute>
            <xsl:attribute name="onclick">showDmRefInfo('<xsl:value-of select="normalize-space($refDMTitle)"/>','<xsl:value-of select="@referredFragment"/>')</xsl:attribute>
			<!-- we do not link refdms in singledm mode 
			<xsl:if test="$SingleDMView='false'">
				<xsl:attribute name="onclick"> document.getElementById("mainForm:autoXref").value=
                        '<xsl:value-of select="translate(@referredFragment,'-','_')"/>';
                    Richfaces.showModalPanel('busyPanel'); refDmLink("<xsl:apply-templates mode="refdm"/>"); </xsl:attribute>
			</xsl:if>
			<xsl:if test="$SingleDMView='true'">
				<xsl:attribute name="onclick">alert('<xsl:value-of select="$v01155"/>')</xsl:attribute>
			</xsl:if> -->
			<xsl:value-of select="substring($refDMTitle,17,35)"/>
			
		</span>
		<xsl:if test="./dmRefAddressItems/dmTitle"> &#160;<xsl:value-of select=".//techName"/> - <xsl:value-of select=".//infoName"/>
		</xsl:if>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="dmRefIdent">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="dmCode"> </xsl:template>
	<!--============处理热点链接内部DM===================================-->
	<xsl:template match="hotspot/refdm | hotspot/dmRef">
		<script>
        	var refdm = new REFDMLink("<xsl:apply-templates mode="refdm"/>", "<xsl:value-of select="$v01155"/>", "<xsl:value-of select="$SingleDMView"/>" );
        	<xsl:if test="@target | @referredFragment"> 
        		var xref = new XREFLink("<xsl:value-of select="@target | @referredFragment"/>"); 
        		refdm.addTarget(xref);
        	</xsl:if>
        	addLink("<xsl:value-of select="ancestor::graphic/@boardno|ancestor::graphic/@infoEntityIdent"/>
			<xsl:value-of select="../@apsid | ../@applicationStructureIdent"/>", refdm);
        </script>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="age">
		<xsl:if test="../dmcextension">
			<xsl:value-of select="dmcextension/dmeproducer"/>
			<xsl:text>-</xsl:text>
			<xsl:value-of select="dmcextension/dmecode"/>
			<xsl:text>-</xsl:text>
		</xsl:if>
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="avee">
		<xsl:if test="../dmcextension">
			<xsl:value-of select="dmcextension/dmeproducer"/>
			<xsl:text>-</xsl:text>
			<xsl:value-of select="dmcextension/dmecode"/>
			<xsl:text>-</xsl:text>
		</xsl:if>
		<xsl:apply-templates/>
	</xsl:template>
	<xsl:template name="t_formatDMC">
		<xsl:if test="dmcextension">
			<xsl:value-of select="dmcextension/dmeproducer"/>
			<xsl:text>-</xsl:text>
			<xsl:value-of select="dmcextension/dmecode"/>
			<xsl:text>-</xsl:text>
		</xsl:if>
		<xsl:choose>
			<xsl:when test="avee">
				<xsl:value-of select="avee/modelic"/>-<xsl:value-of select="avee/sdc"/>-<xsl:value-of select="avee/chapnum"/>-<xsl:value-of select="avee/section"/>
				<xsl:value-of select="avee/subsect"/>-<xsl:value-of select="avee/subject"/>-<xsl:value-of select="avee/discode"/>
				<xsl:value-of select="avee/discodev"/>-<xsl:value-of select="avee/incode"/>
				<xsl:value-of select="avee/incodev"/>-<xsl:value-of select="avee/itemloc"/>
			</xsl:when>
			<xsl:when test="age">
				<xsl:value-of select="age/modelic"/>-<xsl:value-of select="age/supeqvc"/>-<xsl:value-of select="age/ecscs"/>-<xsl:value-of select="age/eidc"/>-<xsl:value-of select="age/cidc"/>-<xsl:value-of select="age/discode"/>
				<xsl:value-of select="age/discodev"/>-<xsl:value-of select="age/incode"/>
				<xsl:value-of select="age/incodev"/>-<xsl:value-of select="age/itemloc"/>
			</xsl:when>
		</xsl:choose>
	</xsl:template>
	<!--===============================================-->
	<xsl:template name="t_formatIss4DMC">
		<xsl:value-of select=".//dmCode/@modelIdentCode"/>-<xsl:value-of select=".//dmCode/@systemDiffCode"/>-<xsl:value-of select=".//dmCode/@systemCode"/>-<xsl:value-of select=".//dmCode/@subSystemCode"/>
		<xsl:value-of select=".//dmCode/@subSubSystemCode"/>-<xsl:value-of select=".//dmCode/@assyCode"/>-<xsl:value-of select=".//dmCode/@disassyCode"/>
		<xsl:value-of select=".//dmCode/@disassyCodeVariant"/>-<xsl:value-of select=".//dmCode/@infoCode"/>
		<xsl:value-of select=".//dmCode/@infoCodeVariant"/>-<xsl:value-of select=".//dmCode/@itemLocationCode"/>
		<xsl:if test="//dmCode/@learnCode">-<xsl:value-of select="//dmCode/@learnCode"/>
		</xsl:if>
		<xsl:if test="//dmCode/@learnEventCode">-<xsl:value-of select="//dmCode/@learnEventCode"/>
		</xsl:if>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="quantity">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="quantityGroup">
		<xsl:apply-templates select="quantityValue"/>
		<xsl:apply-templates select="quantityTolerance"/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="quantityValue">
		<xsl:apply-templates/>
		<xsl:value-of select="@quantityUnitOfMeasure"/>
	</xsl:template>
	<!--===============================================-->
  <xsl:template match="quantityTolerance">
		<xsl:apply-templates select="attribute::quantityToleranceType"/>
		<xsl:apply-templates/>
		<xsl:value-of select="@quantityUnitOfMeasure"/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="@quantityToleranceType['plus']">&#160;+&#160;</xsl:template>
	<!--===============================================-->
	<xsl:template match="@quantityToleranceType['minus']">&#160;-&#160;</xsl:template>
	<!--===============================================-->
	<xsl:template match="@quantityToleranceType['plusorminus']">&#160;+/-&#160;</xsl:template>
	<!--======================DMC-601S0000-A-34-00-00-00A-279A-D_001.xml 标题显示 两行线改成一行线=========================-->
	<xsl:template match="commonInfo">
		<!--<div class="prelreqTitle"><xsl:value-of select="$v02119"/>&#160;<xsl:if test="title">-
                    <xsl:value-of select="title"/></xsl:if></div>
        <hr/>
        <xsl:apply-templates/>
        <hr/>-->
	</xsl:template>
	<!--===============================================-->
	<!-- 
        The following templates generate an xquery from a refdm or dmRef element.
        This works by taking the route xpath to a dmaddress element and adding the appropriate 
        predicates to match the data module code to the one defined in the refdm.
    -->
	<xsl:template match="avee | age" mode="refdm">
		<xsl:value-of select="$iss3xpath"/>
		<xsl:apply-templates mode="refdm"/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="avee/* | age/* " mode="refdm">
		<xsl:value-of select="$iss3predicate"/>./<xsl:value-of select="name()"/>/text()='<xsl:value-of select="./text()"/>'] </xsl:template>
	<!--===============================================-->
	<xsl:template match="dmCode" mode="refdm">
		<xsl:value-of select="$iss4xpath"/>
		<xsl:apply-templates select="@*" mode="refdm"/>
	</xsl:template>
	<!--===============================================-->
  <xsl:template match="dmCode/attribute::node()" mode="refdm">
		<xsl:value-of select="$iss4predicate"/>
		<xsl:value-of select="name()"/>='<xsl:value-of select="."/>']</xsl:template>
	<!--===============================================-->
  <xsl:template match="dmCode/attribute::PositionNumberInDM" priority="2" mode="refdm"/>
	<!--===============================================-->
	<xsl:template match="text()" mode="refdm">
		<xsl:apply-templates mode="refdm"/>
	</xsl:template>
</xsl:stylesheet>
