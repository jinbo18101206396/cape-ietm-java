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
		<div class="prelreqTitle">
			<xsl:attribute name="id"><xsl:value-of select="generate-id(.)"/></xsl:attribute>
        操作准备
        </div>
		<!--<hr/>-->
		<xsl:apply-templates/>
		<!--<hr/>-->
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
						<!--<thead>
                            <tr>
                                <th class="title" width="50%">
                                    <xsl:value-of select="$v00963"/>
                                </th>
                                <th class="dataModule">
                                    <xsl:value-of select="$v00206"/>
                                </th>
                                <th>
                                    <xsl:value-of select="$v00034"/>
                                </th>
                            </tr>
                        </thead>-->
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
      <td>
        <xsl:number level="multiple" format="1.1" count="reqCondDm|reqCondNoRef"/>
      </td>
			<td>
        <xsl:value-of select="reqcond|reqCond"/>&#160;<xsl:apply-templates select="reqdm|refdm|dmRef"/>
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
	<xsl:template match="reqCondNoRef | reqCondCircuitBreaker | reqCondExternalPub">
		<xsl:apply-templates/>
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
            消耗品（件）、材料  <xsl:apply-templates/>
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
			<table width="100%;" class="equipmentTable">
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
				<xsl:value-of select="personnel/@numRequired"/>
				<!--<xsl:value-of select="@man"/>-->
			</td>
			<td>
				<xsl:value-of select="personCategory/@personCategoryCode"/>
			</td>
			<td>
				<xsl:call-template name="t_resolve_skill_level">
					<xsl:with-param name="skill">
						<xsl:value-of select="personSkill/@skillLevelCode"/>
					</xsl:with-param>
				</xsl:call-template>
			</td>
			<!--<td>
				<xsl:value-of select="trade"/>
			</td>-->
			<td>
				<xsl:value-of select="estimatedTime"/>
				<xsl:value-of select="estimatedTime/@unitOfMeasure"/>
			</td>
		</tr>
		<!--<xsl:apply-templates/>-->
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="supportEquipDescr|supplyDescr|spareDescr">
		<tr>
			<td width="50%">
				<!--<xsl:number/>-->
				<xsl:element name="a">
					<xsl:attribute name="id"><xsl:value-of select="translate(@id,'-','-')"/></xsl:attribute>
					<xsl:attribute name="name">TOC<xsl:value-of select="translate(@id,'-','-')"/></xsl:attribute>
					<xsl:call-template name="t_extract_name"/>
				</xsl:element>
			</td>
			<td width="30%">
				<xsl:apply-templates select="identNumber/manufacturerCode"/>
				<xsl:if test="csnref | catalogSeqNumberRef">
					<xsl:apply-templates select="csnref | catalogSeqNumberRef"/>
					<xsl:text>&#x20;</xsl:text>
				</xsl:if>
				<xsl:if test=".//fullNatoStockNumber">
					<xsl:value-of select="$v00600"/>:<xsl:value-of select=".//fullNatoStockNumber"/>
					<xsl:text>&#x20;</xsl:text>
				</xsl:if>
				<xsl:apply-templates select="refs"/>
			</td>
			<td width="10%">
				<xsl:apply-templates select="reqQuantity"/>
			</td>
			<td width="10%">
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
					<xsl:value-of select="$v00660"/>
				</th>
				<th>
					<!--<xsl:value-of select="$v00094"/>-->
                    专业类别
                </th>
				<th>
					<!--<xsl:value-of select="$v00868"/>-->
                    技能等级
                </th>
				<th>
					<!--<xsl:value-of select="$v00982"/>-->
                    估计用时
                </th>
				<!--<th>
                    <xsl:value-of select="$v00325"/>
                </th>
                <th>
                    <xsl:value-of select="$v00034"/>
                </th>-->
			</tr>
		</thead>
	</xsl:template>
	<!--===============================================-->
	<xsl:template name="T_doEquipTableHead">
		<thead>
			<tr>
				<!--<th class="ref">
                    <xsl:value-of select="$v00766"/>
                </th>-->
				<th class="nomen">
					<!--<xsl:value-of select="$v00583"/>-->名称
                </th>
				<th class="indent">
					<!--<xsl:value-of select="$v00403"/>-->型号
                </th>
				<th class="quantity">
					<xsl:value-of select="$v00744"/>
				</th>
				<th class="remarks">
					<!--<xsl:value-of select="$v00785"/>-->
                    备注
                </th>
				<!-- <th>
                    <xsl:value-of select="$v00034"/>
                </th>-->
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
