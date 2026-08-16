<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<!--===============================================-->
	<xsl:template match="proced|procedure">		
		<!-- import css first -->
		<xsl:call-template name="importCSS"/>	
		<xsl:apply-templates />
		<xsl:call-template name="initOutPutArea"/>
	</xsl:template>
	<!--===================程序============================-->
	<xsl:template match="mainfunc|mainProcedure">
		<div class="secondTitle">
			<!--<xsl:choose>
				<xsl:when test="/dmodule">
					<xsl:value-of select="$v00714" />
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="$v00715" />
				</xsl:otherwise>
			</xsl:choose>-->
			程序
		</div>
		<xsl:apply-templates />
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="closeup|closeRqmts">
		<xsl:call-template name="t_closeup" />
		<xsl:apply-templates />
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
		<xsl:call-template name="t_closeup" />
		<xsl:apply-templates />
	</xsl:template>
	<!--===============================================-->
	<xsl:template name="t_closeup">
		<div class="secondTitle">
			<!--<xsl:value-of select="$v00796" />-->后续工作
		</div>
	</xsl:template>
	<!--===============================================-->
</xsl:stylesheet>