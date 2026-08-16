<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<!--===============================================-->
	<xsl:template
		match="para0/title |subpara1/title | subpara2/title | subpara3/title | subpara4/title | subpara5/title | subpara6/title | subpara7/title"
		name="T_doSubParaTitleBlock">
		<p>
			<xsl:attribute name="class">paraXPadding <xsl:call-template
					name="T_FormatDescriptiveTitleBlock" />
			</xsl:attribute>
			<xsl:value-of select="../@count" />
			.
			<span class="paraNumberTitleIndent">
				<xsl:apply-templates />
			</span>
		</p>
	</xsl:template>
	<!--===============================================-->
	<xsl:template name="T_FormatDescriptiveTitleBlock">
		<!-- para0s and subparas have different font sizes wieghts and styles-->
		<!-- TODO -->
		<!-- USE CSS LOGIC HERE TO STYLE -->
		<xsl:if test="parent::para0">para0Title</xsl:if>
		<xsl:if test="parent::subpara1">subpara1Title</xsl:if>
		<xsl:if test="parent::subpara2">subpara2Title</xsl:if>
		<xsl:if test="parent::subpara3">subpara3Title</xsl:if>
		<xsl:if test="parent::subpara4">subpara4Title</xsl:if>
		<xsl:if test="parent::subpara5">subpara4Title</xsl:if>
		<xsl:if test="parent::subpara6">subpara4Title</xsl:if>
		<xsl:if test="parent::subpara7">subpara4Title</xsl:if>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="title">
		<xsl:apply-templates />
	</xsl:template>
	<!--=====================DMC-601S0000-A-34-00-00-00A-030A-D_001.xml打印表格==========================-->
	<xsl:template match="table/title">
		<!-- Titre rendu comme texte nu ; le div.TableTitle parent gère centrage + italique -->
		<xsl:text>表</xsl:text>
		<xsl:number count="table" level="any" format="1"/>
		<xsl:text>&#160;</xsl:text>
		<xsl:apply-templates/>
	</xsl:template>
	<!--=======================分离表格========================-->
	<xsl:template name="T_TearOff">
		<!-- Tear off table icon. src="./images/dmodule/Tear.gif"-->
		<img src="./images/dmodule/Tear.gif" class="tearOff" id="tearOff">
			<xsl:attribute name="title"><xsl:value-of
					select="$v00914" />
			</xsl:attribute>
			<!-- Pass the table object into the displayTearOffTable function. -->
			<xsl:attribute name="onclick">prepTableForTearOff(getElementById('<xsl:value-of select="generate-id(..)"/>'),'<xsl:value-of select="../@PositionNumberInDM"/>')</xsl:attribute>
		</img>
		
	</xsl:template>
	<!--=====================打印==========================-->
	<xsl:template name="T_PrintTable">
		<!-- Tear off table icon. -->		
		<xsl:if test="$SingleDMView='false'">
		<img src="./images/dmodule/PrintTable.png" class="printTable" id="printTable">
			<xsl:attribute name="title"><xsl:value-of
					select="$v00708" />
			</xsl:attribute>
			<!-- Pass the table object into the displayTearOffTable function. -->
			<xsl:attribute name="onclick">doTearOffPrint('<xsl:value-of select="$DMFileName"/>', '<xsl:value-of select="$Publication"/>', '<xsl:value-of select="../@PositionNumberInDM"/>');</xsl:attribute>
		</img>
		</xsl:if>
	</xsl:template>
	<!--===============================================-->
</xsl:stylesheet>
