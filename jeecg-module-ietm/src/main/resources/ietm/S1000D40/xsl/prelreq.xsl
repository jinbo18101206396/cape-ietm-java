<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format">
	<!--2012xmlns:tir-proc="java:java/com.ptc.arbortext.aad.ietp.tir.IetpTIRProcessor?path=file:///E:/IETM4/IETP/IETP_Data_Update/xsl/java/" exclude-result-prefixes="tir-proc"
    xmlns:tir-node="java:java/com.ptc.arbortext.aad.ietp.tir.IetpTIRNode?
path=file:///E:/IETM4/IETP/IETP_Data_Update/xsl/java/" exclude-result-prefixes="tir-node"-->
	<!--===============================================-->
	<xsl:include href="pmdata.xsl"/>
	<xsl:include href="ni.xsl"/>
	<!--===============================================-->
	<xsl:template match="prelreqs|preliminaryRqmts">
		<div class="showsection" id="prelreqs">
			<xsl:call-template name="prelreqblock"/>
		</div>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="/stepview/prelreqs|/stepview/preliminaryRqmts">
		<div class="hidesection" id="prelreqs">
			<xsl:call-template name="prelreqblock"/>
		</div>
	</xsl:template>
	<!--===============================================-->
	<xsl:template name="prelreqblock">
		<!--<xsl:value-of select="$v00687"/>-->
		<div class="prelreqTitle" style="text-align:center;font-weight:bold;">
			<xsl:attribute name="id"><xsl:value-of select="generate-id(.)"/></xsl:attribute>
        准备工作
        </div>
		<!--<hr/>-->
		<xsl:apply-templates/>
		<!--<hr/>-->
	</xsl:template>
	<!--===============================================-->
	<!-- 问题2/3/4修复：productionMaintData 基本信息模块 -->
	<xsl:template match="productionMaintData">
		<div class="para0Title basicInfoBlock" style="border:1px solid #ccc;padding:10px;margin-bottom:10px;">
			<xsl:attribute name="id"><xsl:value-of select="generate-id(.)"/></xsl:attribute>
			<div style="font-weight:bold;margin-bottom:8px;">基本信息</div>
			<xsl:apply-templates/>
		</div>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="productionMaintData/workArea">
		<div style="margin:4px 0;">
			<span style="display:inline-block;width:80px;font-weight:normal;">工作区域：</span>
			<xsl:apply-templates/>
		</div>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="productionMaintData/taskDuration">
		<div style="margin:4px 0;">
			<div style="margin-bottom:4px;">维护任务持续时间：</div>
			<xsl:if test="@startupDuration">
				<div style="margin-left:40px;">
					<span style="display:inline-block;width:120px;">准备工作</span>
					<span><xsl:value-of select="@startupDuration"/><xsl:value-of select="@unitOfMeasure"/></span>
				</div>
			</xsl:if>
			<xsl:if test="@procedureDuration">
				<div style="margin-left:40px;">
					<span style="display:inline-block;width:120px;">维护过程</span>
					<span><xsl:value-of select="@procedureDuration"/><xsl:value-of select="@unitOfMeasure"/></span>
				</div>
			</xsl:if>
			<xsl:if test="@closeupDuration">
				<div style="margin-left:40px;">
					<span style="display:inline-block;width:120px;">收尾工作时间</span>
					<span><xsl:value-of select="@closeupDuration"/><xsl:value-of select="@unitOfMeasure"/></span>
				</div>
			</xsl:if>
		</div>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="productionMaintData/zoneRef">
		<div style="margin:4px 0;">
			<span style="display:inline-block;width:80px;font-weight:normal;">区域：</span>
			<xsl:choose>
				<xsl:when test="name"><xsl:value-of select="name"/></xsl:when>
				<xsl:otherwise><xsl:value-of select="@zoneNumber"/></xsl:otherwise>
			</xsl:choose>
		</div>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="productionMaintData/accessPointRef">
		<div style="margin:4px 0;">
			<span style="display:inline-block;width:80px;font-weight:normal;">口盖：</span>
			<xsl:choose>
				<xsl:when test="name"><xsl:value-of select="name"/></xsl:when>
				<xsl:otherwise><xsl:value-of select="@accessPointNumber"/></xsl:otherwise>
			</xsl:choose>
		</div>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="reqconds|reqCondGroup">
		<div class="para0Title">
			<!--0级侧标题-->
			<xsl:attribute name="id"><xsl:value-of select="generate-id(.)"/></xsl:attribute>
			<!--<xsl:value-of select="$v00794"/>-->
            必要条件
            <!--<xsl:text> : </xsl:text>-->
			<xsl:choose>
				<xsl:when test="noconds|noConds">
					<p>
						<span class="noConds">
							<xsl:value-of select="$v00585"/>
						</span>
					</p>
				</xsl:when>
				<xsl:otherwise>
					<table class="reqcondTable">
						<thead>
                            <tr>
                                <th class="title" style="text-align:center;border:1px solid #ddd;padding:6px;" width="50%">
                                    动作/条件
                                </th>
                                <th class="dataModule" style="text-align:center;border:1px solid #ddd;padding:6px;">
                                    数据模块/技术出版物
                                </th>
                            </tr>
                        </thead>
						<tbody>
							<xsl:apply-templates/>
						</tbody>
					</table>
				</xsl:otherwise>
			</xsl:choose>
		</div>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="reqcondm|reqCondDm">
		<tr>
			<td style="border:1px solid #ddd;padding:6px;">
        <xsl:value-of select="reqcond|reqCond"/>
			</td>
			<td style="border:1px solid #ddd;padding:6px;">
        <xsl:apply-templates select="reqdm|refdm|dmRef"/>
			</td>
		</tr>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="reqCondNoRef | reqCondCircuitBreaker | reqCondExternalPub">
		<tr>
			<td style="border:1px solid #ddd;padding:6px;"><xsl:value-of select="reqCond|reqcond"/></td>
			<td style="border:1px solid #ddd;padding:6px;"></td>
		</tr>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="reqcond | reqCond">
		<xsl:choose>
			<xsl:when test="parent::reqconds | ancestor::reqCondGroup">
				<tr>
					<td>
            <xsl:number level="multiple" format="1.1" count="reqCondDm|reqCondNoRef"/>
					</td>
					<td>
            &#160; <xsl:apply-templates/>
          </td>
				</tr>
			</xsl:when>
			<xsl:otherwise>
				<xsl:apply-templates/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="reqpers | person">
		<div class="reqpersTitle">
			<xsl:value-of select="$v00795"/>: <table width="100%;" class="equipmentTable">
				<xsl:call-template name="T_doReqPersTableHead"/>
				<xsl:for-each select="person | asrequir|personnel">
					<xsl:call-template name="t_requiredpersonnel"/>
				</xsl:for-each>
			</table>
		</div>
	</xsl:template>
	<!--===============================================-->
	<xsl:template name="t_requiredpersonnel">
		<tr>
			<td>
				<xsl:choose>
					<xsl:when test="name(.) ='asrequir'">
						<xsl:value-of select="$v00051"/> . </xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="$v00523"/>&#160; <xsl:value-of select="./@man"/>
					</xsl:otherwise>
				</xsl:choose>
			</td>
			<td>
				<xsl:choose>
					<xsl:when test="name(following-sibling::*[1]) = 'perscat'">personCategoryCode
                        <xsl:value-of select="following-sibling::*[1]/@category"/>
					</xsl:when>
					<xsl:otherwise>&#160;</xsl:otherwise>
				</xsl:choose>
			</td>
			<td>
				<xsl:choose>
					<xsl:when test="name(following-sibling::*[1]) = 'perskill' ">
						<xsl:variable name="currentskill">
							<xsl:value-of select="translate(following-sibling::*[1]/@skill,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ') "/>
						</xsl:variable>
						<xsl:choose>
							<xsl:when test="$currentskill='SK03'">
								<xsl:value-of select="$v00017"/>
							</xsl:when>
							<xsl:when test="$currentskill='SK01'">
								<xsl:value-of select="$v00062"/>
							</xsl:when>
							<xsl:when test="$currentskill='SK02'">
								<xsl:value-of select="$v00444"/>
							</xsl:when>
							<xsl:otherwise>
								<xsl:value-of select="$currentskill"/>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:when>
					<xsl:when test="name(following-sibling::*[2]) = 'perskill' ">
						<xsl:variable name="currentskill">
							<xsl:value-of select="translate(following-sibling::*[2]/@skill,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ') "/>
						</xsl:variable>
						<xsl:choose>
							<xsl:when test="$currentskill='SK03'">
								<xsl:value-of select="$v00017"/>
							</xsl:when>
							<xsl:when test="$currentskill='SK01'">
								<xsl:value-of select="$v00062"/>
							</xsl:when>
							<xsl:when test="$currentskill='SK02'">
								<xsl:value-of select="$v00444"/>
							</xsl:when>
							<xsl:otherwise>
								<xsl:value-of select="$currentskill"/>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:when>
					<xsl:otherwise>&#160;</xsl:otherwise>
				</xsl:choose>
			</td>
			<!--<td>
				<xsl:choose>
					<xsl:when test="name(following-sibling::*[1]) = 'trade' ">
						<xsl:value-of select="following-sibling::*[1]/text()"/>
					</xsl:when>
					<xsl:when test="name(following-sibling::*[2]) = 'trade' ">
						<xsl:value-of select="following-sibling::*[2]/text()"/>
					</xsl:when>
					<xsl:when test="name(following-sibling::*[3]) = 'trade' ">
						<xsl:value-of select="following-sibling::*[3]/text()"/>
					</xsl:when>
					<xsl:otherwise>&#160;</xsl:otherwise>
				</xsl:choose>
			</td>-->
			<td>
				<xsl:choose>
					<xsl:when test="name(following-sibling::*[1]) = 'esttime' ">
						<xsl:value-of select="following-sibling::*[1]/text()"/>
					</xsl:when>
					<xsl:when test="name(following-sibling::*[2]) = 'esttime' ">
						<xsl:value-of select="following-sibling::*[2]/text()"/>
					</xsl:when>
					<xsl:when test="name(following-sibling::*[3]) = 'esttime' ">
						<xsl:value-of select="following-sibling::*[3]/text()"/>
					</xsl:when>
					<xsl:when test="name(following-sibling::*[4]) = 'esttime' ">
						<xsl:value-of select="following-sibling::*[4]/text()"/>
					</xsl:when>
					<xsl:otherwise>&#160;</xsl:otherwise>
				</xsl:choose>
			</td>
			<!--<td>
                <xsl:call-template name="t_inlineApplicability">
                    <xsl:with-param name="class"/>
                </xsl:call-template>
                <xsl:apply-templates select="applic"/>
            </td>-->
		</tr>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="reqcontp">
		<tr>
			<td>
				<xsl:apply-templates select="reqcond"/>
			</td>
			<td>
				<xsl:apply-templates select="reftp"/>
			</td>
		</tr>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="reqtp | asrequir | person | perscat | perskill | trade | esttime | nosupeq | supequi | qty | supply | spare | safecond">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="supeqli | supplyli | sparesli">
		<xsl:if test="not(ancestor::stepview)">
			<table width="100%;" class="equipmentTable">
				<xsl:call-template name="T_doEquipTableHead"/>
				<xsl:for-each select="supequi | supply | spare">
					<tr>
						<td>
							<xsl:number/>
						</td>
						<td>
							<xsl:element name="a">
								<xsl:attribute name="id"><xsl:value-of select="translate(@id,'-','-')"/></xsl:attribute>
								<xsl:attribute name="name">TOC<xsl:value-of select="translate(@id,'-','-')"/></xsl:attribute>
								<xsl:call-template name="t_extract_name"/>
							</xsl:element>
						</td>
						<td>
							<xsl:apply-templates select="identno"/>
							<xsl:if test="csnref | catalogSeqNumberRef">
								<xsl:apply-templates select="csnref | catalogSeqNumberRef"/>
								<xsl:text>&#x20;</xsl:text>
							</xsl:if>
							<xsl:if test="nsn/@nsn">
								<xsl:value-of select="$v00600"/>:<xsl:value-of select="nsn/@nsn"/>
								<xsl:text>&#x20;</xsl:text>
							</xsl:if>
							<xsl:apply-templates select="refs"/>
						</td>
						<td>
							<xsl:value-of select="qty"/>
						</td>
						<td>
							<xsl:apply-templates select="remarks" mode="prelreq"/>
						</td>
						<td>
							<xsl:call-template name="t_inlineApplicability">
								<xsl:with-param name="class"/>
							</xsl:call-template>
							<xsl:value-of select="applic"/>
						</td>
					</tr>
				</xsl:for-each>
			</table>
		</xsl:if>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="identno">
		<xsl:if test="pnr">
			<xsl:value-of select="$v00726"/>:<xsl:value-of select="identno/pnr"/>
			<xsl:text>&#x20;</xsl:text>
		</xsl:if>
		<xsl:if test="mfc">
			<xsl:value-of select="$v00534"/>:<xsl:value-of select="identno/mfc"/>
			<xsl:text>&#x20;</xsl:text>
		</xsl:if>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="remarks" mode="prelreq">
		<xsl:apply-templates mode="prelreq"/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="remarks/simplePara" mode="prelreq">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="supequip">
		<div class="supequipTitle">
			<xsl:value-of select="$v00899"/> : <xsl:apply-templates/>
		</div>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="nosupeq|noSupportEquips">
		<p>
			<span class="noConds">
				<xsl:value-of select="$v00585"/>
			</span>
		</p>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="supplies">
		<div class="supequipTitle">
			<xsl:value-of select="$v00174"/> : <xsl:apply-templates/>
		</div>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="nosupply|noSupplies">
		<p>
			<span class="noConds">
				<xsl:value-of select="$v00585"/>
			</span>
		</p>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="spares">
		<div class="sparesTitle">
			<xsl:value-of select="$v00874"/> : <xsl:apply-templates/>
		</div>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="nospares|noSpares">
		<p>
			<span class="noConds">
				<xsl:value-of select="$v00585"/>
			</span>
		</p>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="safety|reqSafety">
		<!--<xsl:value-of select="$v00815"/>-->
		<div class="para0Title">
			<xsl:attribute name="id"><xsl:value-of select="generate-id(.)"/></xsl:attribute>
             安全条件 <xsl:apply-templates/>
		</div>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="safetyRqmts">
		<xsl:apply-templates/>
		<xsl:call-template name="t_dereference_wcn"/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="nosafety|noSafety">
		<p>
			<span class="noConds">
				<xsl:value-of select="$v00585"/>
			</span>
		</p>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="reqTechInfoGroup">
		<div class="reqTechInfoTitle">
			<xsl:value-of select="$v02129"/>: <table width="100%;" class="reqTechInfoTable">
				<thead>
					<tr>
						<th>
							<xsl:value-of select="$v00772"/>
						</th>
					</tr>
				</thead>
				<tbody>
					<xsl:apply-templates/>
				</tbody>
			</table>
		</div>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="reqTechInfo">
		<tr>
			<td>
				<xsl:apply-templates/>
			</td>
		</tr>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="prelreqs//refs">
		<br/>
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="reqPersons">
		<div class="para0Title">
			<!--<xsl:value-of select="$v00795"/>-->人员要求 
            <table width="100%;" class="equipmentTable">
				<xsl:call-template name="T_doReqPersTableHead"/>
				<tbody>
                    <xsl:apply-templates/>
                </tbody>
			</table>
		</div>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="reqSupplies">
		<div class="para0Title">
			<xsl:attribute name="id"><xsl:value-of select="generate-id(.)"/></xsl:attribute>
			<!--<xsl:value-of select="$v00174"/>-->
            耗材  <xsl:apply-templates/>
		</div>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="supplyDescrGroup">
		<table width="100%" class="equipmentTable">
			<xsl:call-template name="T_doEquipTableHead"/>
			<tbody>
				<xsl:apply-templates/>
			</tbody>
		</table>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="reqSupportEquips"> 
		<div class="para0Title">
			<xsl:attribute name="id"><xsl:value-of select="generate-id(.)"/></xsl:attribute>
			<!--<xsl:value-of select="$v00899"/>:--> 
			保障设备
			<xsl:apply-templates/>
		</div>
	</xsl:template>
	<!--====================操作设备中的支持设备的表格===========================-->
	<xsl:template match="supportEquipDescrGroup|spareDescrGroup">
		<xsl:if test="not(ancestor::stepview)">
			<table width="100%" class="equipmentTable">
				<xsl:call-template name="T_doEquipTableHead"/>
				<tbody>
					<xsl:apply-templates/>
				</tbody>
			</table>
		</xsl:if>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="reqSpares">
		<div class="para0Title">
			<xsl:attribute name="id"><xsl:value-of select="generate-id(.)"/></xsl:attribute>
			<!--<xsl:value-of select="$v00874"/>-->备件  <xsl:apply-templates/>
		</div>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="reqPersons/person| reqPersons/personnel">
		<tr>
			<td>
				<xsl:value-of select="personCategory/@personCategoryCode"/>
			</td>
			<td>
				<xsl:value-of select="@numRequired"/>
			</td>
			<td>
				<xsl:value-of select="personSkill/@skillLevelCode"/>
			</td>
			<td>
				<xsl:value-of select="trade"/>
			</td>
			<td>
				<xsl:value-of select="estimatedTime"/>
				<xsl:if test="estimatedTime/@unitOfMeasure">
					<xsl:value-of select="estimatedTime/@unitOfMeasure"/>
				</xsl:if>
			</td>
		</tr>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="supportEquipDescr|supplyDescr|spareDescr">
		<tr>
			<td style="border:1px solid #ddd;padding:6px;">
				<!--<xsl:number/>-->
				<xsl:element name="a">
					<xsl:attribute name="id"><xsl:value-of select="translate(@id,'-','-')"/></xsl:attribute>
					<xsl:attribute name="name">TOC<xsl:value-of select="translate(@id,'-','-')"/></xsl:attribute>
				</xsl:element>
				<xsl:value-of select="name|nomen"/>
			</td>
			<td style="border:1px solid #ddd;padding:6px;">
				<xsl:if test="identNumber/manufacturerCode or identNumber/partAndSerialNumber/partNumber">
					<xsl:text>部件：</xsl:text>
					<xsl:value-of select="identNumber/manufacturerCode"/>
					<xsl:if test="identNumber/partAndSerialNumber/partNumber">
						<xsl:text>/</xsl:text>
						<xsl:element name="a">
							<xsl:attribute name="href">#<xsl:value-of select="identNumber/partAndSerialNumber/partNumber"/></xsl:attribute>
							<xsl:value-of select="identNumber/partAndSerialNumber/partNumber"/>
						</xsl:element>
					</xsl:if>
				</xsl:if>
				<xsl:if test=".//fullNatoStockNumber">
					<xsl:value-of select="$v00600"/>:<xsl:value-of select=".//fullNatoStockNumber"/>
					<xsl:text>&#x20;</xsl:text>
				</xsl:if>
				<xsl:apply-templates select="refs"/>
			</td>
			<td style="border:1px solid #ddd;padding:6px;">
				<xsl:apply-templates select="reqQuantity"/>
			</td>
			<td style="border:1px solid #ddd;padding:6px;">
				<xsl:apply-templates select="remarks" mode="prelreq"/>
			</td>
			<!--<td>
                <xsl:call-template name="t_inlineApplicability">
                    <xsl:with-param name="class"/>
                </xsl:call-template>
            </td>-->
		</tr>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="partsAndSerialNumber">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="partNumber">
		<xsl:apply-templates/>&#x20; </xsl:template>
	<!--===============================================-->
	<xsl:template match="reqQuantity">
		<xsl:apply-templates/>&#160;<xsl:value-of select="@unitOfMeasure"/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template name="T_doReqPersTableHead">
		<thead>
			<tr>
				<th>
                    专业类别
                </th>
				<th>
                    人数
                </th>
				<th>
                    技能等级
                </th>
				<th>
                    职业/职业代码
                </th>
				<th>
                    估计用时
                </th>
			</tr>
		</thead>
	</xsl:template>
	<!--===============================================-->
	<xsl:template name="T_doEquipTableHead">
		<thead>
			<tr>
				<th class="nomen" style="text-align:center;border:1px solid #ddd;padding:6px;width:20%;">
					<!--<xsl:value-of select="$v00583"/>-->名称
                </th>
				<th class="indent" style="text-align:center;border:1px solid #ddd;padding:6px;width:40%;">
					<!--<xsl:value-of select="$v00403"/>-->规格型号
                </th>
				<th class="quantity" style="text-align:center;border:1px solid #ddd;padding:6px;width:15%;">
					<xsl:value-of select="$v00744"/>
				</th>
				<th class="remarks" style="text-align:center;border:1px solid #ddd;padding:6px;width:25%;">
					<!--<xsl:value-of select="$v00785"/>-->
                    备注
                </th>
			</tr>
		</thead>
	</xsl:template>
	<!--===============================================-->
	<xsl:template name="t_resolve_skill_level">
		<xsl:param name="skill" select="sk01"/>
		<xsl:choose>
			<xsl:when test="$skill='sk03'">
				<xsl:value-of select="$v00017"/>
			</xsl:when>
			<xsl:when test="$skill='sk01'">
				<xsl:value-of select="$v00062"/>
			</xsl:when>
			<xsl:when test="$skill='sk02'">
				<xsl:value-of select="$v00444"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$skill"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<!--=======================================================================-->
	<!-- This template extracts the name of the supply or tool from the TIR	if -->
	<!-- one has been referenced within the supply/support equipment entry	   -->
	<!--=======================================================================-->
	<xsl:template name="t_extract_name">
		<xsl:choose>
			<xsl:when test="child::ein|child::functionalItemRef|child::con|child::supplyRqmtRef|child::tool|child::toolRef">
				<xsl:apply-templates select="child::ein|child::functionalItemRef|child::con|child::tool|child::supplyRqmtRef|child::toolRef"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:apply-templates select="nomen|name"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<!--=======================================================================-->
</xsl:stylesheet>
