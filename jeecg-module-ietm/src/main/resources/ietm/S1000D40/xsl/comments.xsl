<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="html"/>

    <!--===============================================-->
    <xsl:include href="languageVariables.xsl"/>
    <!--===============================================--> 

    <xsl:template match="/">
        <xsl:apply-templates/>
    </xsl:template>

    <xsl:template match="activeComment">
        <h2>
            <xsl:value-of select="$v01094"/>
        </h2>
        <hr width="100%" size="3"/>
        <xsl:choose>
            <xsl:when test="./*">
                <xsl:apply-templates/>
            </xsl:when>
            <xsl:otherwise>
                <p>
                    <xsl:value-of select="$v01121"/>
                </p>
                <hr width="100%" size="3"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="archiveComment">
        <h2>
            <xsl:value-of select="$v01096"/>
        </h2>
        <hr width="100%" size="3"/>
        <xsl:choose>
            <xsl:when test="./*">
                <xsl:apply-templates/>
            </xsl:when>
            <xsl:otherwise>
                <p>
                    <xsl:value-of select="$v01123"/>
                </p>
                <hr width="100%" size="3"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="comment">
        <table border="0" cellpadding="5">
            <tbody>
                <xsl:apply-templates/>
            </tbody>
        </table>
        <hr width="100%" size="3"/>
    </xsl:template>

    <xsl:template match="ccode|commentCode">
        <tr>
            <th align="left">
                <xsl:value-of select="$v01099"/>
            </th>
            <td><xsl:value-of select="./modelic | @modelIdentCode"/>-<xsl:value-of
                    select="./sendid | @senderIdent"/>-<xsl:value-of
                    select="./diyear | @yearOfDataIssue"/>-<xsl:value-of
                    select="./seqnum | @seqNumber"/>-<xsl:value-of
                    select="./ctype/@type | @commentType"/></td>
        </tr>
    </xsl:template>

    <xsl:template match="cstatus/issdate | commentAddressItems/issueDate">
        <tr>
            <th align="left">
                <xsl:value-of select="$v01103"/>
            </th>
            <td><xsl:value-of select="./@year"/>-<xsl:value-of select="./@month"/>-<xsl:value-of
                    select="./@day"/></td>
        </tr>
    </xsl:template>

    <xsl:template match="dispaddr | dmtitle | priority">
        <xsl:apply-templates/>
    </xsl:template>

    <xsl:template match="security">
        <tr>
            <th align="left">
                <xsl:value-of select="$v00844"/>
            </th>
            <td>
                <xsl:call-template name="T_getSecurityClassification">
                    <xsl:with-param name="secNum">
                        <xsl:value-of select="@class"/>
                    </xsl:with-param>
                </xsl:call-template>
            </td>
        </tr>
    </xsl:template>

    <xsl:template name="T_getSecurityClassification">
        <xsl:param name="secNum">1</xsl:param>
        <xsl:choose>
            <xsl:when test="1">
                <xsl:value-of select="$v00996"/>
            </xsl:when>
            <xsl:when test="2">
                <xsl:value-of select="$v00802"/>
            </xsl:when>
            <xsl:when test="3">
                <xsl:value-of select="$v00162"/>
            </xsl:when>
            <xsl:when test="4">
                <xsl:value-of select="$v00841"/>
            </xsl:when>
            <xsl:when test="5">
                <xsl:value-of select="$v00981"/>
            </xsl:when>
            <xsl:when test="6">TOP TOP SECRET</xsl:when>
            <xsl:when test="7">TOP TOP TOP SECRET</xsl:when>
            <xsl:when test="8">TOP TOP TOP TOP SECRET</xsl:when>
            <xsl:otherwise>....<xsl:value-of select="name()"/>....</xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="avee | age">
        <tr>
            <th align="left">
                <xsl:value-of select="$v00260"/>
            </th>
            <td><xsl:if test="../../dmcextension/dmeproducer/text()"><xsl:value-of
                        select="../../dmcextension/dmeproducer"/>-<xsl:value-of
                        select="../../dmcextension/dmecode"/>-</xsl:if>
                <xsl:value-of select="modelic"/>-<xsl:value-of select="sdc"/>-<xsl:value-of
                    select="chapnum"/>-<xsl:value-of select="section"/><xsl:value-of
                    select="subsect"/>-<xsl:value-of select="subject"/>-<xsl:value-of
                    select="discode"/><xsl:value-of select="discodev"/>-<xsl:value-of
                    select="incode"/><xsl:value-of select="incodev"/>-<xsl:value-of select="itemloc"
                /></td>
        </tr>
    </xsl:template>

    <xsl:template match="dmRef">
        <tr>
            <th align="left">
                <xsl:value-of select="$v00260"/>
            </th>
            <td>
                <xsl:apply-templates/>
            </td>
        </tr>
    </xsl:template>

    <xsl:template match="identExtension">
        <xsl:if test="not(@extensionProducer = '')">
            <xsl:value-of select="@extensionProducer"/>- </xsl:if>
        <xsl:if test="not(@extensionCode = '')">
            <xsl:value-of select="@extensionCode"/>- </xsl:if>
    </xsl:template>

    <xsl:template match="dmRefIdent">
        <xsl:value-of select="./dmCode/@modelIdentCode"/>-<xsl:value-of
            select="./dmCode/@systemDiffCode"/>-<xsl:value-of select="./dmCode/@systemCode"
            />-<xsl:value-of select="./dmCode/@subSystemCode"/>
        <xsl:value-of select="./dmCode/@subSubSystemCode"/>-<xsl:value-of
            select="./dmCode/@assyCode"/>-<xsl:value-of select="./dmCode/@disassyCode"
            />-<xsl:value-of select="./dmCode/@disassyCodeVariant"/>-<xsl:value-of
            select="./dmCode/@infoCode"/><xsl:value-of select="./dmCode/@infoCodeVariant"
            />-<xsl:value-of select="./dmCode/@itemLocationCode"/>
    </xsl:template>

    <xsl:template match="corig | commentOriginator">
        <tr>
            <th align="left">
                <xsl:value-of select="$v00626"/>
            </th>
            <td>
                <xsl:value-of
                    select="dispaddr/enterprise/ent-name | dispatchAddress/enterprise/enterpriseName"
                />
            </td>
        </tr>
    </xsl:template>

    <xsl:template match="p|simplePara">
        <tr>
            <th align="left">
                <xsl:value-of select="$v00151"/>
            </th>
        </tr>
        <tr>
            <td colspan="2" style="border:1;">
                <xsl:apply-templates/>
            </td>
        </tr>
    </xsl:template>

</xsl:stylesheet>
