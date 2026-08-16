<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<!--===============================================-->
	<xsl:include href="applic.xsl"/>
	<xsl:include href="datarest.xsl"/>
	<!--===============================================-->
	<xsl:template name="t_statusChangeMarker">
		<xsl:if test="parent::status/@change = 'ADD'">
			<xsl:attribute name="class">changeMarker</xsl:attribute>
		</xsl:if>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="status|dmStatus">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="dmsize"/><!-- hidden -->
	<!--===============================================-->
	<xsl:template match="security">
		<xsl:if test="@class or @securityClassification">
			<tr>
				<xsl:call-template name="t_statusChangeMarker"/>
				<td class="idStatus">
					<xsl:value-of select="$v01116"/>:</td>
				<td class="idStatus">
					<xsl:value-of select="@class|@securityClassification"/>
				</td>
			</tr>
		</xsl:if>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="logo"/><!-- hidden -->
	<!--===============================================-->
	<xsl:template match="rpc|responsiblePartnerCompany"/><!-- hidden -->
	<!--===============================================-->
	<xsl:template match="orig|originator"/><!-- hidden -->
	<!--===============================================-->
	<xsl:template match="productSafety"/><!-- hidden -->
	<!--===============================================-->
	<xsl:template match="techstd"/><!-- hidden -->
	<!--===============================================-->
	<xsl:template match="applicCrossRefTableRef">
		<xsl:apply-templates/>
	</xsl:template><!-- row hidden; children still traversed for dmCode -->
	<!--===============================================-->
	<xsl:template match="brexDmRef"/><!-- hidden -->
	<!--===============================================-->
	<xsl:template match="autandtp">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="authblk">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="tpbase">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="authex">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="inline">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="exmod">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="addmod">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="retrofit">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="mod">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="modtitle">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="notes">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="qa|qualityAssurance"/><!-- hidden -->
	<!--===============================================-->
	<xsl:template match="unverif|unverified">
		<xsl:value-of select="$v01008"/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="firstver|firstVerification">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="secver">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="sbc|systemBreakdownCode"/><!-- hidden -->
	<!--===============================================-->
	<xsl:template match="fic"/><!-- hidden -->
	<!--===============================================-->
	<xsl:template match="rfu|reasonForUpdate"/><!-- hidden -->
	<!--===============================================-->
	<xsl:template match="remarks"/><!-- hidden -->
	<!--===============================================-->
	<xsl:template match="skill"/><!-- hidden -->
	<!--===============================================-->
	<xsl:template match="status/ein|dmStatus/functionalItemRef"/><!-- hidden -->
	<!--===============================================-->
	<xsl:template match="idstatus">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="dmtitle|dmTitle">
		<xsl:if test="techname|techName or infoname|infoName">
			<tr>
				<td class="idStatus">
					<xsl:value-of select="$v01105"/>:</td>
				<td class="idStatus">
					<xsl:apply-templates select="techname|techName"/>-<xsl:apply-templates select="infoname|infoName"/>
				</td>
			</tr>
		</xsl:if>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="techname|techName">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="infoname|infoName">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="issno"/><!-- hidden -->
	<!--===============================================-->
	<xsl:template match="issdate">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="language">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="ein"/><!-- hidden -->
	<!--===============================================-->
	<xsl:template match="idstatus/srcdmaddres | status/srcdmaddres"/><!-- hidden -->
	<!--===============================================-->
	<xsl:template match="idstatus/dmaddres"/><!-- hidden -->
	<!--===============================================-->
	<xsl:template match="identAndStatusSection/dmAddress">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="identAndStatusSection/dmAddress/dmIdent">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="identAndStatusSection/dmAddress/dmIdent/identExtension"/><!-- hidden -->
	<!--===============================================-->
	<xsl:template match="identAndStatusSection//dmCode">
		<!-- 只在至少有一个字段有值时显示整行 -->
		<xsl:variable name="hasData" select="
			@modelIdentCode or @systemDiffCode or @systemCode or @subSystemCode or
			@subSubSystemCode or @assyCode or @disassyCode or @disassyCodeVariant or
			@infoCode or @infoCodeVariant or @itemLocationCode or
			../identExtension/@extensionProducer or ../identExtension/@extensionCode
		"/>
		<xsl:if test="$hasData">
			<tr>
				<td class="idStatus">
					<xsl:value-of select="$v00260"/>:
	            </td>
				<td class="idStatus">
					<!-- DMC前缀 -->
					<xsl:text>DMC-</xsl:text>
					<!-- 扩展代码（如果有） -->
					<xsl:if test="../identExtension">
						<xsl:value-of select="../identExtension/@extensionProducer"/>-<xsl:value-of select="../identExtension/@extensionCode"/>-
					</xsl:if>
					<!-- 主体DMC代码 -->
					<xsl:value-of select="@modelIdentCode"/>-<xsl:value-of select="@systemDiffCode"/>-<xsl:value-of select="@systemCode"/>-<xsl:value-of select="@subSystemCode"/>
					<xsl:value-of select="@subSubSystemCode"/>-<xsl:value-of select="@assyCode"/>-<xsl:value-of select="@disassyCode"/>
					<xsl:value-of select="@disassyCodeVariant"/>-<xsl:value-of select="@infoCode"/>
					<xsl:value-of select="@infoCodeVariant"/>-<xsl:value-of select="@itemLocationCode"/>
					<!-- 版本号（从兄弟元素issueInfo获取） -->
					<xsl:if test="../issueInfo/@issueNumber">
						<xsl:text>_</xsl:text>
						<xsl:value-of select="../issueInfo/@issueNumber"/>
						<xsl:if test="../issueInfo/@inWork">
							<xsl:text>-</xsl:text>
							<xsl:value-of select="../issueInfo/@inWork"/>
						</xsl:if>
					</xsl:if>
					<!-- 语言代码（从兄弟元素language获取） -->
					<xsl:if test="../language/@languageIsoCode">
						<xsl:text>_</xsl:text>
						<xsl:value-of select="../language/@languageIsoCode"/>
						<xsl:if test="../language/@countryIsoCode">
							<xsl:text>-</xsl:text>
							<xsl:value-of select="../language/@countryIsoCode"/>
						</xsl:if>
					</xsl:if>
				</td>
			</tr>
		</xsl:if>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="issueInfo">
		<xsl:if test="@issueNumber or @inWork">
			<tr>
				<td class="idStatus">
					<xsl:value-of select="$v00466"/>:&#160;<xsl:value-of select="@issueNumber"/>
				</td>
				<td class="idStatus">
					<!-- <xsl:value-of select="$v00466"/>:&#160;<xsl:value-of select="@issueNumber"/> -->
					<xsl:text>临时版本:&#160;</xsl:text>
					<xsl:value-of select="@inWork"/>
				</td>
			</tr>
		</xsl:if>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="dmAddressItems">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="dmAddressItems/issueDate">
		<xsl:if test="@year or @month or @day">
			<tr>
				<td class="idStatus">
					<xsl:value-of select="$v00454"/>
				</td>
				<td class="idStatus">
					<xsl:value-of select="@year"/>-<xsl:value-of select="@month"/>-<xsl:value-of select="@day"/>
				</td>
			</tr>
		</xsl:if>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="actref"/><!-- hidden -->
	<!--===============================================-->
	<xsl:template name="dmcOutput2">
		<!-- the calling template should do the div-->
		<xsl:if test="dmcextension">
			<xsl:value-of select="dmcextension/dmeproducer"/>
			<xsl:text>-</xsl:text>
			<xsl:value-of select="dmcextension/dmecode"/>
			<xsl:text>-</xsl:text>
		</xsl:if>
		<xsl:choose>
			<xsl:when test="dmc/avee">
				<xsl:value-of select="dmc/avee/modelic"/>-<xsl:value-of select="dmc/avee/sdc"/>-<xsl:value-of select="dmc/avee/chapnum"/>-<xsl:value-of select="dmc/avee/section"/>
				<xsl:value-of select="dmc/avee/subsect"/>-<xsl:value-of select="dmc/avee/subject"/>-<xsl:value-of select="dmc/avee/discode"/>
				<xsl:value-of select="dmc/avee/discodev"/>-<xsl:value-of select="dmc/avee/incode"/>
				<xsl:value-of select="dmc/avee/incodev"/>-<xsl:value-of select="dmc/avee/itemloc"/>
			</xsl:when>
			<xsl:when test="dmc/age">
				<xsl:value-of select="dmc/age/modelic"/>-<xsl:value-of select="dmc/age/supeqvc"/>-<xsl:value-of select="dmc/age/ecscs"/>-<xsl:value-of select="dmc/age/eidc"/>-<xsl:value-of select="dmc/age/cidc"/>-<xsl:value-of select="dmc/age/discode"/>
				<xsl:value-of select="dmc/age/discodev"/>-<xsl:value-of select="dmc/age/incode"/>
				<xsl:value-of select="dmc/age/incodev"/>-<xsl:value-of select="dmc/age/itemloc"/>
			</xsl:when>
		</xsl:choose>
	</xsl:template>
	<!--===============================================-->
</xsl:stylesheet>
