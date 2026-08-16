<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<!--2012 xmlns:refdm="java:com.lbsltd.matrix2.content.RefDMExtensionFunctions?path=file:///E:/IETM4/xsl/java/" exclude-result-prefixes="refdm"
    xmlns:aadext="java:com.ptc.aad.xsltext.StringFunctions?path=file:///E:/IETM4/xsl/java/" exclude-result-prefixes="aadext"-->
    <xsl:template match="checkList[@checkListCategory='clc01']">
        <!-- import css first -->
        <xsl:call-template name="importCSS"/>
        <div class="checkListTitle">
            <xsl:value-of select="$v02250"/>
        </div>
        <xsl:apply-templates select="preliminaryRqmts|commonInfo"/>
        <div class="checkListTitle">
            <xsl:value-of select="$v02250"/>
        </div>
        <xsl:apply-templates mode="clc01" select="checkListInfo"/>
        <xsl:call-template name="initOutPutArea"/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="checkListInfo" mode="clc01">
        <xsl:call-template name="T_CheckList_Totals"/>
        <xsl:call-template name="t_inlineApplicability">
            <xsl:with-param name="annotation"><xsl:value-of select="$v00034"/>:&#160;</xsl:with-param>
        </xsl:call-template>
        <div class="checkListTitle">
            <xsl:value-of select="title"/>
        </div>
        <div>
            <xsl:call-template name="t_changeMarker"/>
            <table class="checkListTable">
                <thead>
                    <tr valign="top">
                        <th>
                            <xsl:value-of select="$v02255"/>
                        </th>
                        <th>
                            <xsl:value-of select="$v02138"/>
                        </th>
                        <xsl:apply-templates select="checkListIntervals" mode="clc01"/>
                        <th>
                            <xsl:value-of select="$v02258"/>
                        </th>
                    </tr>
                </thead>
                <tbody>
                    <xsl:apply-templates select="checkListItems" mode="clc01"/>
                </tbody>
            </table>
        </div>
    </xsl:template>
    <!--===============================================-->
    <xsl:template name="T_CheckList_Totals">
        <div align="center">
            <table>
                <body>
                    <xsl:apply-templates select=".//checkListIntervals/checkListInterval"
                        mode="totals"/>
                </body>
            </table>
        </div>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="checkListInterval" mode="totals">
        <tr>
            <td>
                <div/>
            </td>
            <td align="right">
                <xsl:variable name="ct">
                    <xsl:value-of select="position()"/>
                </xsl:variable>
                <xsl:if test="$ct = 1">
                    <xsl:value-of select="$v02254"/>
                </xsl:if>
            </td>
            <td align="center">
                <div>
                    <xsl:apply-templates/>
                </div>
            </td>
            <td>
                <div style="border-bottom:1pt solid black; width: 100px"/>
            </td>
            <td>
                <div/>
            </td>
        </tr>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="checkListIntervals" mode="clc01">
        <xsl:apply-templates mode="clc01"/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="checkListInterval" mode="clc01">
        <th>
            <xsl:apply-templates/>
        </th>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="checkListItems" mode="clc01">
        <xsl:apply-templates mode="clc01"/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="workArea" mode="clc01">
        <tr>
            <td/>
            <td style="text-align:center;">
                <xsl:apply-templates/>
            </td>
            <xsl:for-each select="../..//checkListInterval">
                <td/>
            </xsl:for-each>
        </tr>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="checkListItem" mode="clc01">
        <tr style="vertical-align: middle;">
            <td>
                <xsl:apply-templates select="itemNumber" mode="clc01"/>
            </td>
            <td>
                <xsl:apply-templates select="checkListProcedure" mode="clc01"/>
            </td>
            <xsl:call-template name="t_Threshholds"/>
            <td> &#160; </td>
        </tr>
    </xsl:template>
    <!--===============================================-->
    <xsl:template name="t_Threshholds">
        <xsl:call-template name="t_doLoop">
            <xsl:with-param name="i">1</xsl:with-param>
            <xsl:with-param name="count">
                <xsl:number count="//checkList/checkListInfo/checkListIntervals/checkListInterval"
                    level="any"/>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    <!--===============================================-->
    <xsl:template name="t_doLoop">
        <xsl:param name="i">1</xsl:param>
        <xsl:param name="count"/>

        <xsl:if test="$i &lt;= $count">
            <td align="center">
                <xsl:variable name="t_int_id">
                    <xsl:value-of
                        select="//checkList/checkListInfo/checkListIntervals/checkListInterval[position()=$i]/@id"
                    />
                </xsl:variable>
                <xsl:for-each select="threshold">
                    <div>
                        <xsl:variable name="t_thres_id">
                            <xsl:value-of select="@thresholdUnitOfMeasure"/>
                        </xsl:variable>
                        <xsl:if test="$t_thres_id = $t_int_id">
                            <xsl:value-of select="thresholdValue"/>
                        </xsl:if>
                    </div>
                </xsl:for-each>
            </td>
        </xsl:if>
        <xsl:if test="$i &lt;= $count">
            <xsl:call-template name="t_doLoop">
                <xsl:with-param name="i">
                    <xsl:value-of select="$i + 1"/>
                </xsl:with-param>
                <xsl:with-param name="count">
                    <xsl:value-of select="$count"/>
                </xsl:with-param>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="itemNumber" mode="clc01">
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="checkListProcedure" mode="clc01">
        <div>
            <xsl:call-template name="t_changeMarker"/>
            <xsl:apply-templates mode="clc01"/>
        </div>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="checkListPara" mode="clc01">
        <div>
            <xsl:call-template name="t_changeMarker"/>
            <xsl:apply-templates/>
        </div>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="threshold" mode="clc01">
        <td>
            <xsl:apply-templates mode="clc01"/>
        </td>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="thresholdValue" mode="clc01">
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="warning | caution | note" mode="clc01">
        <div>
            <xsl:apply-templates select="."/>
        </div>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="tolerance" mode="clc01">
        <!-- supress
        <xsl:apply-templates select="." mode="chkList"/>
		-->
    </xsl:template>
    <!--===============================================-->
    <!-- Checklist - Preventive Maintenance Checks and Services      -->
    <!--===============================================-->

    <xsl:template match="checkList[@checkListCategory = 'clc02']">
        <xsl:call-template name="importCSS"/>
        <div class="checkListTitle">
            <xsl:value-of select="$v02252"/>
        </div>
        <xsl:apply-templates select="preliminaryRqmts|commonInfo"/>
        <div class="checkListTitle">
            <xsl:value-of select="$v02252"/>
        </div>
        <div>
            <xsl:call-template name="t_changeMarker"/>
            <xsl:apply-templates select="checkListInfo" mode="clc02"/>
        </div>
        <xsl:call-template name="initOutPutArea"/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="checkListInfo" mode="clc02">
        <xsl:apply-templates select="preliminaryRqmts|commonInfo"/>
        <xsl:call-template name="t_inlineApplicability">
            <xsl:with-param name="annotation"><xsl:value-of select="$v00034"/>:&#160;</xsl:with-param>
        </xsl:call-template>
        <div class="checkListTitle">
            <xsl:value-of select="title"/>
        </div>
        <div>
            <xsl:call-template name="t_changeMarker"/>
            <table style="border-bottom-width: thin; border-bottom-style: solid; cellpadding:5">
                <thead>
                    <tr>
                        <th style="border-bottom-width: thin; border-bottom-style: solid;"
                            align="left">
                            <xsl:value-of select="$v02142"/>
                        </th>
                        <th style="border-bottom-width: thin; border-bottom-style: solid;"
                            align="left">
                            <xsl:value-of select="$v02136"/>
                        </th>
                        <th style="border-bottom-width: thin; border-bottom-style: solid;"
                            align="left">
                            <xsl:value-of select="$v02139"/>
                        </th>
                        <th style="border-bottom-width: thin; border-bottom-style: solid;"
                            align="left">
                            <xsl:value-of select="$v00716"/>
                        </th>
                        <th style="border-bottom-width: thin; border-bottom-style: solid;"
                            align="left">
                            <xsl:value-of select="$v02135"/>
                        </th>
                    </tr>
                </thead>
                <tbody>
                    <xsl:apply-templates select="checkListItems" mode="clc02"/>
                </tbody>
            </table>
        </div>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="checkListItems" mode="clc02">
        <xsl:apply-templates mode="clc02"/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="checkListItem" mode="clc02">
        <tr valign="top">
            <td>
                <xsl:apply-templates select="itemNumber" mode="clc02"/>
            </td>
            <td>
                <xsl:apply-templates select="threshold" mode="clc02"/>
            </td>
            <td>
                <xsl:apply-templates select="equip" mode="clc02"/>
            </td>
            <td>
                <xsl:call-template name="T_CrewMemberType">
                    <xsl:with-param name="t_CM">
                        <xsl:value-of select="checkListProcedure/@crewMemberType"/>
                    </xsl:with-param>
                </xsl:call-template>
            </td>
            <td>&#160;</td>
        </tr>
        <xsl:apply-templates select="checkListProcedure | refs" mode="clc02"/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template name="T_CrewMemberType">
        <xsl:param name="t_CM"/>
        <xsl:choose>
            <xsl:when test="$t_CM='cm01'">
                <xsl:value-of select="$v00023"/>
            </xsl:when>
            <xsl:when test="$t_CM='cm02'">
                <xsl:value-of select="$v02203"/>
            </xsl:when>
            <xsl:when test="$t_CM='cm03'">
                <xsl:value-of select="$v02205"/>
            </xsl:when>
            <xsl:when test="$t_CM='cm04'">
                <xsl:value-of select="$v02207"/>
            </xsl:when>
            <xsl:when test="$t_CM='cm05'">
                <xsl:value-of select="$v02209"/>
            </xsl:when>
            <xsl:when test="$t_CM='cm06'">
                <xsl:value-of select="$v02211"/>
            </xsl:when>
            <xsl:when test="$t_CM='cm07'">
                <xsl:value-of select="$v02213"/>
            </xsl:when>
            <xsl:when test="$t_CM='cm08'">
                <xsl:value-of select="$v02215"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$t_CM"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <!--===============================================-->
    <xsl:template name="T_Checklist_Warnings">
        <tr valign="top">
            <td>&#160;</td>
            <td>&#160;</td>
            <td>&#160;</td>
            <td>
                <xsl:apply-templates select="warning | caution | note" mode="clc02"/>
            </td>
            <td>&#160;</td>
        </tr>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="itemNumber" mode="clc02">
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="threshold" mode="clc02">
        <xsl:apply-templates select="thresholdValue"/>
        <xsl:text> </xsl:text>
        <xsl:call-template name="t_UOM">
            <xsl:with-param name="UOMCode">
                <xsl:value-of select="@thresholdUnitOfMeasure"/>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="equip" mode="clc02">
        <div>
            <xsl:call-template name="t_changeMarker"/>
            <xsl:apply-templates mode="chklist"/>
        </div>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="equip/name" mode="chklist">
        <div>
            <xsl:call-template name="t_changeMarker"/>
            <xsl:apply-templates/>
        </div>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="refs" mode="clc02">
        <tr valign="top">
            <td>&#160;</td>
            <td>&#160;</td>
            <td>&#160;</td>
            <td>
                <div>
                    <xsl:call-template name="t_changeMarker"/>
                    <xsl:apply-templates mode="clc02"/>
                </div>
                <xsl:apply-templates select="following-sibling::*" mode="clc02"/>
            </td>
            <td>&#160;</td>
        </tr>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="checkListItem/zoneRef" mode="clc02">
        <div>
            <xsl:call-template name="t_changeMarker"/>
            <xsl:apply-templates/>
        </div>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="checkListItem/remarks" mode="clc02">
        <div>
            <xsl:call-template name="t_changeMarker"/>
            <xsl:apply-templates/>
        </div>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="dmRef " mode="clc02">
        <xsl:call-template name="t_inlineApplicability">
            <xsl:with-param name="annotation"><xsl:value-of select="$v00034"/>:&#160;</xsl:with-param>
        </xsl:call-template>
        <div>
            <xsl:call-template name="t_changeMarker"/>
            <xsl:value-of select="$v00260"/>: <xsl:apply-templates select="."/>
        </div>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="externalPubRef" mode="clc02">
        <xsl:call-template name="t_inlineApplicability">
            <xsl:with-param name="annotation"><xsl:value-of select="$v00034"/>:&#160;</xsl:with-param>
        </xsl:call-template>
        <div>
            <xsl:call-template name="t_changeMarker"/>
            <xsl:value-of select="@href"/>
        </div>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="pmRef" mode="clc02">
        <xsl:call-template name="t_inlineApplicability">
            <xsl:with-param name="annotation"><xsl:value-of select="$v00034"/>:&#160;</xsl:with-param>
        </xsl:call-template>
        <div>
            <xsl:call-template name="t_changeMarker"/>
            <xsl:value-of select="$v00674"/>: <xsl:value-of select=".//@modelIdentCode"
                />-<xsl:value-of select=".//@pmIssuer"/>-<xsl:value-of select=".//@pmNumber"
                />-<xsl:value-of select=".//@pmVolume"/>
        </div>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="checkListProcedure" mode="clc02">
        <xsl:apply-templates mode="clc02"/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="checkListProcedure/checkListPara" mode="clc02">
        <tr valign="top">
            <td>&#160;</td>
            <td>&#160;</td>
            <td>&#160;</td>
            <td>
                <div>
                    <xsl:call-template name="t_changeMarker"/>
                    <xsl:apply-templates select="*[not(name()='equipmentNotAvailable')]"
                        mode="clc02"/>
                </div>
            </td>
            <td>
                <div>
                    <xsl:call-template name="t_changeMarker"/>
                    <xsl:apply-templates select="equipmentNotAvailable" mode="clc02"/>
                </div>
            </td>
        </tr>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="checkListProcedure/title" mode="clc02">
        <tr valign="top">
            <td>&#160;</td>
            <td>&#160;</td>
            <td>&#160;</td>
            <td>
                <div>
                    <xsl:call-template name="t_changeMarker"/>
                    <xsl:apply-templates/>
                </div>
            </td>
            <td>&#160;</td>
        </tr>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="checkListStep" mode="clc02">
        <!-- do notes / warnings and cautions -->
        <xsl:call-template name="T_Checklist_Warnings"/>
        <tr valign="top">
            <td>&#160;</td>
            <td>&#160;</td>
            <td>&#160;</td>
            <td>
                <div>
                    <xsl:call-template name="t_changeMarker"/>
                    <xsl:apply-templates select="para" mode="clc02"/>
                </div>
            </td>
            <td>
                <div>
                    <xsl:call-template name="t_changeMarker"/>
                    <xsl:apply-templates select="equipmentNotAvailable | remarks" mode="clc02"/>
                </div>
            </td>
        </tr>
        <xsl:apply-templates select="checkListStep" mode="clc02"/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="checkListStep/para" mode="clc02">
        <xsl:call-template name="t_inlineApplicability">
            <xsl:with-param name="annotation"><xsl:value-of select="$v00034"/>:&#160;</xsl:with-param>
        </xsl:call-template>
        <div>
            <xsl:call-template name="t_changeMarker"/>
            <xsl:value-of select="../@ptc_level"/>. <xsl:apply-templates mode="clc02"/>
        </div>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="equipmentNotAvailable | remarks" mode="clc02">
        <xsl:call-template name="t_inlineApplicability">
            <xsl:with-param name="annotation"><xsl:value-of select="$v00034"/>:&#160;</xsl:with-param>
        </xsl:call-template>
        <div>
            <xsl:call-template name="t_changeMarker"/>
            <xsl:apply-templates mode="clc02"/>
        </div>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="equipmentNotAvailable/para" mode="clc02">
        <xsl:call-template name="t_inlineApplicability">
            <xsl:with-param name="annotation"><xsl:value-of select="$v00034"/>:&#160;</xsl:with-param>
        </xsl:call-template>
        <div>
            <xsl:call-template name="t_changeMarker"/>
            <xsl:apply-templates/>
        </div>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="checkListPara/para" mode="clc02">
        <xsl:call-template name="t_inlineApplicability">
            <xsl:with-param name="annotation"><xsl:value-of select="$v00034"/>:&#160;</xsl:with-param>
        </xsl:call-template>
        <div>
            <xsl:call-template name="t_changeMarker"/>
            <xsl:apply-templates/>
        </div>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="warning | caution | note" mode="clc02">
        <div>
            <xsl:call-template name="t_changeMarker"/>
            <xsl:apply-templates select="."/>
        </div>
    </xsl:template>
    <!--===============================================-->
    <!-- Checklist - Checking unpacked equipment conditions           -->
    <!--===============================================-->
    <xsl:template match="checkList[@checkListCategory='clc98']">
        <xsl:call-template name="importCSS"/>
        <div class="checkListTitle">
            <xsl:value-of select="$v02262"/>
        </div>
        <xsl:apply-templates select="preliminaryRqmts|commonInfo"/>
        <xsl:apply-templates mode="clc98" select="checkListInfo"/>
        <xsl:call-template name="initOutPutArea"/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="checkListInfo" mode="clc98">
        <xsl:apply-templates select="preliminaryRqmts|commonInfo"/>
        <div class="checkListTitle">
            <xsl:value-of select="title"/>
        </div>
        <xsl:call-template name="t_inlineApplicability">
            <xsl:with-param name="annotation"><xsl:value-of select="$v00034"/>:&#160;</xsl:with-param>
        </xsl:call-template>
        <table style="border-bottom-width: thin; border-bottom-style: solid; cellpadding:5">
            <thead>
                <tr>
                    <th style="border-bottom-width: thin; border-bottom-style: solid;" width="20%"
                        align="left">
                        <xsl:value-of select="$v02140"/>
                    </th>
                    <th style="border-bottom-width: thin; border-bottom-style: solid;" width="20%"
                        align="left">
                        <xsl:value-of select="$v02265"/>
                    </th>
                    <th style="border-bottom-width: thin; border-bottom-style: solid;" width="40%"
                        align="left">
                        <xsl:value-of select="$v00012"/>
                    </th>
                    <th style="border-bottom-width: thin; border-bottom-style: solid;" width="20%"
                        align="left">
                        <xsl:value-of select="$v00785"/>
                    </th>
                </tr>
            </thead>
            <tbody>
                <xsl:apply-templates select="checkListItems" mode="clc98"/>
            </tbody>
        </table>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="checkListItems" mode="clc98">
        <xsl:apply-templates mode="clc98"/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="checkListItem" mode="clc98">
        <tr>
            <td valign="top">
                <xsl:if test="not(preceding-sibling::checkListItem)">
                    <xsl:value-of select="../workArea|../zoneRef"/>
                </xsl:if>
            </td>
            <td valign="top">
                <xsl:apply-templates select="equip" mode="clc98"/>
            </td>
            <td valign="top">
                <xsl:apply-templates select="checkListProcedure" mode="clc98"/>
            </td>
            <td valign="top">
                <xsl:apply-templates select="remarks" mode="clc98"/>
            </td>
        </tr>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="workArea" mode="clc98">
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="equip" mode="clc98">
        <div>
            <xsl:call-template name="t_changeMarker"/>
            <xsl:apply-templates mode="clc98"/>
        </div>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="equip/name" mode="clc98">
        <div>
            <xsl:call-template name="t_changeMarker"/>
            <xsl:apply-templates/>
        </div>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="identNumber" mode="clc98">
        <div>
            <xsl:call-template name="t_changeMarker"/>
            <xsl:apply-templates mode="clc98"/>
        </div>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="manufacturerCode" mode="clc98">
        <xsl:apply-templates select="." mode="chklist"/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="partAndSerialNumber" mode="clc98">
        <xsl:apply-templates mode="clc98"/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="partNumber" mode="clc98">
        <div>
            <xsl:call-template name="t_changeMarker"/>
            <xsl:value-of select="$v00726"/>: <xsl:apply-templates/>
        </div>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="serialNumber" mode="clc98">
        <xsl:if test="not(preceding-sibling::serialNumber)">
            <div><xsl:value-of select="$v02472"/></div>
        </xsl:if>
        <div>
            <xsl:value-of select="@serialNumberValue"/>
        </div>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="refs" mode="clc98">
        <xsl:apply-templates mode="clc02"/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="checkListProcedure" mode="clc98">
        <div>
            <xsl:call-template name="t_changeMarker"/>
            <xsl:apply-templates mode="clc98"/>
        </div>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="checkListPara/para" mode="clc98">
        <xsl:call-template name="t_inlineApplicability">
            <xsl:with-param name="annotation"><xsl:value-of select="$v00034"/>:&#160;</xsl:with-param>
        </xsl:call-template>
        <div>
            <xsl:call-template name="t_changeMarker"/>
            <xsl:apply-templates mode="clc98"/>
        </div>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="checkListStep" mode="clc98">
        <xsl:apply-templates mode="clc98"/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="checkListStep/para" mode="clc98">
        <xsl:call-template name="t_inlineApplicability">
            <xsl:with-param name="annotation"><xsl:value-of select="$v00034"/>:&#160;</xsl:with-param>
        </xsl:call-template>
        <div>
            <xsl:call-template name="t_changeMarker"/>
            <xsl:value-of select="../@ptc_level"/>. <xsl:apply-templates mode="clc98"/>
        </div>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="remarks" mode="clc98">
        <xsl:call-template name="t_inlineApplicability">
            <xsl:with-param name="annotation"><xsl:value-of select="$v00034"/>:&#160;</xsl:with-param>
        </xsl:call-template>
        <div>
            <xsl:call-template name="t_changeMarker"/>
            <xsl:apply-templates/>
        </div>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="warning | caution | note" mode="clc98">
        <div>
            <xsl:apply-templates select="."/>
        </div>
    </xsl:template>
    <!--===============================================-->
    <!-- Checklist Special Instructions                                                       -->
    <!--===============================================-->
    <xsl:template match="checkList[@checkListCategory = 'clc99']">
        <xsl:call-template name="importCSS"/>
        <div class="checkListTitle">
            <xsl:value-of select="$v02269"/>
        </div>
        <xsl:apply-templates select="preliminaryRqmts|commonInfo"/>
        <xsl:call-template name="t_inlineApplicability">
            <xsl:with-param name="annotation"><xsl:value-of select="$v00034"/>:&#160;</xsl:with-param>
        </xsl:call-template>
        <xsl:apply-templates mode="clc99" select="checkListInfo"/>
        <xsl:call-template name="initOutPutArea"/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="checkListInfo" mode="clc99">
        <div class="checkListTitle">
            <xsl:apply-templates select="title"/>
        </div>
        <div>
            <xsl:call-template name="t_changeMarker"/>
            <table width="100%">
                <thead>
                    <tr>
                        <th style="border-bottom-width: thin; border-bottom-style: solid;"
                            align="left">
                            <xsl:value-of select="$v02145"/>
                        </th>
                        <th style="border-bottom-width: thin; border-bottom-style: solid;"
                            align="left">
                            <xsl:value-of select="$v02260"/>
                        </th>
                        <th style="border-bottom-width: thin; border-bottom-style: solid;"
                            align="left">
                            <xsl:value-of select="$v02136"/>
                        </th>
                        <th style="border-bottom-width: thin; border-bottom-style: solid;"
                            align="left">
                            <xsl:value-of select="$v02265"/>
                        </th>
                        <th style="border-bottom-width: thin; border-bottom-style: solid;"
                            align="left">
                            <xsl:value-of select="$v02137"/>
                        </th>
                    </tr>
                </thead>
                <tbody>
                    <xsl:apply-templates mode="clc99"/>
                </tbody>
            </table>
        </div>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="checkListItems" mode="clc99">
        <xsl:apply-templates mode="clc99"/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="checkListItem" mode="clc99">
        <tr valign="top">
            <td>
                <xsl:if test="not(preceding-sibling::checkListItem)">
                    <xsl:value-of select="../workArea|../zoneRef"/>
                </xsl:if>
            </td>
            <td>
                <xsl:apply-templates select="itemNumber" mode="clc99"/>
            </td>
            <td>
                <xsl:apply-templates select="threshold" mode="clc99"/>
            </td>
            <td>
                <xsl:apply-templates select="equip" mode="clc99"/>
            </td>
            <td>
                <xsl:apply-templates select="checkListProcedure" mode="clc99"/>
            </td>
        </tr>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="workArea" mode="clc99">
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="itemNumber" mode="clc99">
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="threshold" mode="clc99">
        <xsl:apply-templates select="thresholdValue"/>
        <xsl:text> </xsl:text>
        <xsl:call-template name="t_UOM">
            <xsl:with-param name="UOMCode">
                <xsl:value-of select="@thresholdUnitOfMeasure"/>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="equip" mode="clc99">
        <div>
            <xsl:call-template name="t_changeMarker"/>
            <xsl:apply-templates mode="clc98"/>
        </div>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="checkListProcedure" mode="clc99">
        <xsl:apply-templates mode="clc98"/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="checkList//catalogSeqNumberRef" mode="chklist">
        <div>
            <xsl:call-template name="t_changeMarker"/>
            <div>
                <xsl:apply-templates select="."/>
            </div>

        </div>
        <xsl:if test="ancestor::checkList[@checkListCategory='clc02']">
            <div>
                <xsl:apply-templates select="refs/*" mode="clc02"/>
            </div>
        </xsl:if>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="checkList//natoStockNumber" mode="chklist">
        <div>
            <xsl:call-template name="t_changeMarker"/>
            <div>
                <xsl:value-of select="$v00600"/>: <xsl:call-template name="T_nsn">
                    <xsl:with-param name="NSN_Temp">
                        <xsl:apply-templates/>
                    </xsl:with-param>
                </xsl:call-template>
            </div>
        </div>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="checkList//manufacturerCode" mode="chklist">
        <div>
            <xsl:call-template name="t_changeMarker"/>
            <div>
                <xsl:value-of select="$v00534"/>: <xsl:apply-templates/>
            </div>
        </div>
    </xsl:template>
    <!--===============================================-->
</xsl:stylesheet>
