<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <!--===============================================-->
    <xsl:template match="datarest|dataRestrictions">
        <!-- Section header row — only show if there are non-empty children -->
        <xsl:variable name="hasContent" select="
            normalize-space(distrib|dataDistribution) != '' or
            normalize-space(expcont|exportControl) != '' or
            normalize-space(handling|dataHandling) != '' or
            normalize-space(destruct|dataDestruction) != '' or
            normalize-space(disclose|dataDisclosure) != '' or
            normalize-space(.//polref|.//policyStatement) != '' or
            normalize-space(.//datacond|.//dataConds) != ''
        "/>
        <xsl:if test="$hasContent">
            <tr>
                <xsl:call-template name="t_changeMarker"/>
                <td class="idStatus" colspan="2">
                    <xsl:value-of select="$v00212"/>
                </td>
            </tr>
            <xsl:apply-templates/>
        </xsl:if>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="instruct|restrictionInstructions">
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="distrib|dataDistribution">
        <xsl:if test="normalize-space(.) != ''">
            <tr>
                <xsl:call-template name="t_changeMarker"/>
                <td class="idStatus">
                    <xsl:value-of select="$v00252"/>
                </td>
                <td class="idStatus">
                    <xsl:apply-templates/>
                </td>
            </tr>
        </xsl:if>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="expcont|exportControl">
        <xsl:if test="normalize-space(.) != ''">
            <tr>
                <xsl:call-template name="t_changeMarker"/>
                <td class="idStatus">
                    <xsl:value-of select="$v00335"/>
                </td>
                <td class="idStatus">
                    <xsl:apply-templates/>
                </td>
            </tr>
        </xsl:if>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="handling|dataHandling">
        <xsl:if test="normalize-space(.) != ''">
            <tr>
                <xsl:call-template name="t_changeMarker"/>
                <td class="idStatus">
                    <xsl:value-of select="$v00383"/>
                </td>
                <td class="idStatus">
                    <xsl:apply-templates/>
                </td>
            </tr>
        </xsl:if>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="destruct|dataDestruction">
        <xsl:if test="normalize-space(.) != ''">
            <tr>
                <xsl:call-template name="t_changeMarker"/>
                <td class="idStatus">
                    <xsl:value-of select="$v00241"/>
                </td>
                <td class="idStatus">
                    <xsl:apply-templates/>
                </td>
            </tr>
        </xsl:if>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="disclose|dataDisclosure">
        <xsl:if test="normalize-space(.) != ''">
            <tr>
                <xsl:call-template name="t_changeMarker"/>
                <td class="idStatus">
                    <xsl:value-of select="$v00250"/>
                </td>
                <td class="idStatus">
                    <xsl:apply-templates/>
                </td>
            </tr>
        </xsl:if>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="inform|restrictionInfo">
        <!-- Pass-through — children (copyright/policyStatement/dataConds) generate their own <tr> rows directly -->
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="copyright">
        <tr>
            <xsl:call-template name="t_changeMarker"/>
            <td class="idStatus">
                <xsl:value-of select="v00186"/>
            </td>
            <td class="idStatus">
                <xsl:apply-templates/>
            </td>
        </tr>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="polref|policyStatement">
        <xsl:if test="normalize-space(.) != ''">
            <tr>
                <td class="idStatus">
                    <xsl:value-of select="$v00676"/>
                </td>
                <td class="idStatus">
                    <xsl:apply-templates/>
                </td>
            </tr>
        </xsl:if>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="datacond|dataConds">
        <xsl:if test="normalize-space(.) != ''">
            <tr>
                <xsl:call-template name="t_changeMarker"/>
                <td class="idStatus">
                    <xsl:value-of select="$v00160"/>
                </td>
                <td class="idStatus">
                    <xsl:apply-templates/>
                </td>
            </tr>
        </xsl:if>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="supersedure">
        <tr>
            <xsl:call-template name="t_changeMarker"/>
            <td class="idStatus"><xsl:value-of select="$v02130"/></td>
            <td class="idStatus">
                <xsl:apply-templates/>
            </td>
        </tr>
    </xsl:template>
    <!--===============================================-->
</xsl:stylesheet>
