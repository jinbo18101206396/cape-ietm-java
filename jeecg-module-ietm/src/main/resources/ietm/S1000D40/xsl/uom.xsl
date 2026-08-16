<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<!--2012 xmlns:aadext="java:com.ptc.aad.xsltext.StringFunctions?path=file:///E:/IETM4/xsl/java/" exclude-result-prefixes="aadext"-->
    <!--===============================================-->
    <xsl:template name="t_UOM">
        <xsl:param name="UOMCode">??</xsl:param>
        <xsl:variable name="UOM_Temp">
            <xsl:if test="upper-case($UOMCode) = 'TH01'">FH</xsl:if>
            <xsl:if test="upper-case($UOMCode)= 'TH02'">FC</xsl:if>
            <xsl:if test="upper-case($UOMCode)= 'TH03'">M</xsl:if>
            <xsl:if test="upper-case($UOMCode)= 'TH04'">W</xsl:if>
            <xsl:if test="upper-case($UOMCode)= 'TH05'">Y</xsl:if>
            <xsl:if test="upper-case($UOMCode)= 'TH06'">D</xsl:if>
            <xsl:if test="upper-case($UOMCode)= 'TH07'">S</xsl:if>
            <xsl:if test="upper-case($UOMCode)= 'TH08'">P</xsl:if>
            <xsl:if test="upper-case($UOMCode)= 'TH09'">E</xsl:if>
            <xsl:if test="upper-case($UOMCode)= 'TH10'">ENG CNG</xsl:if>
            <xsl:if test="upper-case($UOMCode)= 'TH11'">SHP VSP</xsl:if>
            <xsl:if test="upper-case($UOMCode)= 'TH12'">APU CNG</xsl:if>
            <xsl:if test="upper-case($UOMCode)= 'TH13'">LDG CNG</xsl:if>
            <xsl:if test="upper-case($UOMCode)= 'TH14'">WHL CNG</xsl:if>
            <xsl:if test="upper-case($UOMCode)= 'TH15'">ENG STRT</xsl:if>
            <!--Matches for the 1.9 spec-->
            <xsl:if test="upper-case($UOMCode)= 'FH'">FH</xsl:if>
            <xsl:if test="upper-case($UOMCode)= 'FC'">FC</xsl:if>
            <xsl:if test="upper-case($UOMCode)= 'M'">M</xsl:if>
            <xsl:if test="upper-case($UOMCode)= 'W'">W</xsl:if>
            <xsl:if test="upper-case($UOMCode)= 'Y'">Y</xsl:if>
            <xsl:if test="upper-case($UOMCode)= 'D'">D</xsl:if>
            <xsl:if test="upper-case($UOMCode)= 'S'">S</xsl:if>
            <xsl:if test="upper-case($UOMCode)= 'P'">P</xsl:if>
            <xsl:if test="upper-case($UOMCode)= 'E'">E</xsl:if>
            <xsl:if test="upper-case($UOMCode)= 'OC'">OC</xsl:if>
            <xsl:if test="upper-case($UOMCode)= 'OPC'">OPC</xsl:if>
            <xsl:if test="upper-case($UOMCode)= 'OPH'">OPH</xsl:if>
            <xsl:if test="upper-case($UOMCode)= 'ENG CNG'">ENG CNG</xsl:if>
            <xsl:if test="upper-case($UOMCode)= 'SHP VSP'">SHP VSP</xsl:if>
            <xsl:if test="upper-case($UOMCode)= 'APU CNG'">APU CNG</xsl:if>
            <xsl:if test="upper-case($UOMCode)= 'LDG CNG'">LDG CNG</xsl:if>
            <xsl:if test="upper-case($UOMCode)= 'WHL CNG'">WHL CNG</xsl:if>
        </xsl:variable>
        <!-- If the UOM attribute does not match a pre-determined code, use the code directly -->
        <xsl:choose>
            <xsl:when test="$UOM_Temp = '' ">
                <xsl:value-of select="$UOMCode"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$UOM_Temp"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

</xsl:stylesheet>
