<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <!--===============================================-->
    <xsl:template match="dmc" name="dmcOutput">
        <!-- the calling template should do the div-->
        <xsl:if test="../dmcextension">
            <xsl:value-of select="../dmcextension/dmeproducer"/>
            <xsl:text>-</xsl:text>
            <xsl:value-of select="../dmcextension/dmecode"/>
            <xsl:text>-</xsl:text>
        </xsl:if>
        <xsl:choose>
            <xsl:when test="avee">
                <xsl:value-of select="ancestor::dmodule//modelic"/>-<xsl:value-of
                    select="ancestor::dmodule//sdc"/>-<xsl:value-of
                    select="ancestor::dmodule//chapnum"/>-<xsl:value-of
                    select="ancestor::dmodule//section"/>
                <xsl:value-of select="ancestor::dmodule//subsect"/>-<xsl:value-of
                    select="ancestor::dmodule//subject"/>-<xsl:value-of
                    select="ancestor::dmodule//discode"/>
                <xsl:value-of select="ancestor::dmodule//discodev"/>-<xsl:value-of
                    select="ancestor::dmodule//incode"/>
                <xsl:value-of select="ancestor::dmodule//incodev"/>-<xsl:value-of
                    select="ancestor::dmodule//itemloc"/>
            </xsl:when>
            <xsl:when test="age">
                <xsl:value-of select="ancestor::dmodule//modelic"/>-<xsl:value-of
                    select="ancestor::dmodule//supeqvc"/>-<xsl:value-of
                    select="ancestor::dmodule//ecscs"/>-<xsl:value-of
                    select="ancestor::dmodule//eidc"/>
                <xsl:value-of select="ancestor::dmodule//cidc"/>-<xsl:value-of
                    select="ancestor::dmodule//discode"/>
                <xsl:value-of select="ancestor::dmodule//discodev"/>-<xsl:value-of
                    select="ancestor::dmodule//incode"/>
                <xsl:value-of select="ancestor::dmodule//incodev"/>-<xsl:value-of
                    select="ancestor::dmodule//itemloc"/>
            </xsl:when>
        </xsl:choose>
    </xsl:template>
    <!--===============================================-->
    <xsl:template
        match="age | avee | modelic | upeqvc | ecscs | eidc | cidc | discode | discodev | incode | incodev | itemloc | subject | sdc | chapnum | section | subsect">
        <!-- stop -->
    </xsl:template>
    <!--===============================================-->
</xsl:stylesheet>
