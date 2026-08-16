<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<!--
	this is factored out code to enable logical sharing between fault.xsl and dmToc.xsl through dmTocUtil.xsl
-->
<!--===============================================-->
	<xsl:template name="T_AirFaultTitle">
		<xsl:choose>
			<xsl:when test="self::afi|self::faultIsolation"><xsl:value-of select="$v00347"/></xsl:when>
			<xsl:when test="self::afr|self::faultReporting"><xsl:value-of select="$v00351"/></xsl:when>
			<xsl:when test="self::dfault|self::detectedFault"><xsl:value-of select="$v00343"/></xsl:when>
			<xsl:when test="self::ofault|self::observedFault "><xsl:value-of select="$v00349"/></xsl:when>
			<xsl:when test="self::cfault|self::correlatedFault"><xsl:value-of select="$v01100"/></xsl:when>
			<xsl:when test="self::ifault|self::isolatedFault"><xsl:value-of select="$v00347"/></xsl:when>
			<xsl:when test="self::fcontext|self::faultContext"><xsl:value-of select="$v00181"/></xsl:when>
			<xsl:when test="self::test|self::faultIsolationTest"><xsl:value-of select="$v00925"/></xsl:when>
			<xsl:when test="name()='sru'"><xsl:value-of select="$v00610"/></xsl:when>
			<xsl:when test="name()='lru'"><xsl:value-of select="$v00609"/></xsl:when>
		</xsl:choose>
	</xsl:template>
	<!--===============================================-->
	<xsl:template name="T_AirFaultRefsTitles">
		<xsl:choose>
			<xsl:when test="parent::repair"><xsl:value-of select="$v00021"/></xsl:when>
			<xsl:when test="parent::lru or parent::afiref"><xsl:value-of select="$v00020"/></xsl:when>
			<xsl:when test="parent::diagnost"><xsl:value-of select="$v00019"/></xsl:when>
			<xsl:when test="parent::testdesc or parent::testproc or parent::test"><xsl:value-of select="$v00926"/></xsl:when>
			<xsl:when test="parent::describe"><xsl:value-of select="$v00240"/></xsl:when>
			<xsl:otherwise><xsl:value-of select="$v00781"/></xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<!-- ============================================-->
</xsl:stylesheet>