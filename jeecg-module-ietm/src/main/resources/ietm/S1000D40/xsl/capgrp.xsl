<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <!--=========处理capgrp|captionGroup======================================-->
    <xsl:template match="capgrp|captionGroup">
        <xsl:call-template name="t_inlineApplicability">
            <xsl:with-param name="annotation"><xsl:value-of select="$v00034"/>:&#160;</xsl:with-param>
        </xsl:call-template>
        <span class="spanWorkAround">
            <table class="capgrpTable">
                <xsl:call-template name="t_changeMarker"/>
                <xsl:apply-templates/>
            </table>
        </span>
    </xsl:template>
    <!--===============处理capbody|captionBody================================-->
    <xsl:template match="capbody|captionBody">
        <tbody>
            <xsl:apply-templates/>
        </tbody>
    </xsl:template>
    <!--=================处理caprow|captionRow==============================-->
    <xsl:template match="caprow|captionRow">
        <tr>
            <xsl:call-template name="t_changeMarker"/>
            <xsl:apply-templates/>
        </tr>
    </xsl:template>
    <!--=================处理capentry|captionEntry==============================-->
    <xsl:template match="capentry|captionEntry">
        <td>
            <xsl:call-template name="t_inlineApplicability">
                <xsl:with-param name="annotation"><xsl:value-of select="$v00034"/>:&#160;</xsl:with-param><!-- $v00034适用性 -->
            </xsl:call-template>
            <xsl:apply-templates/>
        </td>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="caption">
        <p>
            <xsl:call-template name="t_inlineApplicability">
                <xsl:with-param name="annotation"><xsl:value-of select="$v00034"/>:&#160;</xsl:with-param>
            </xsl:call-template>
            <xsl:attribute name="class">caption</xsl:attribute>
            <!-- the author has control of color and align in the xsl-->
            <xsl:attribute name="align">
                <xsl:call-template name="captionAlign"/>
            </xsl:attribute>
            <!-- do a span with background color araoung the text so the color is on the text not
					the table cell-->
            <span>
                <xsl:attribute name="class">caption <xsl:call-template name="captionGroupColours"/></xsl:attribute>
                <xsl:apply-templates/>
            </span>
        </p>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="capline|captionLine">
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="captext|captionText">
        <div>
            <xsl:call-template name="t_changeMarker"/>
            <xsl:apply-templates/>
        </div>
    </xsl:template>
    <!--===处理颜色============================================-->
    <xsl:template name="captionGroupColours">
        <!-- named template to put background color to captions, must be at caption context and within an element fo:block-->
        <!--
		co07 is issue 2.2 white
		co09 is issue 2.2 clear
		co05 and 06 are issue 2.2 'not given'(reserved for s1000d)
	-->
        <!-- if you have co10-co99 it should be white, take the alpha chars out and leave numbers-->
        <xsl:variable name="translatevalue">
            <xsl:value-of
                select="translate(@colour|@color,'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ','')"
            />
        </xsl:variable>
        <xsl:variable name="captionColour">
            <xsl:value-of
                select="translate(@colour|@color,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')"
            />
        </xsl:variable>
        <!-- if the caption colour is clear or white we put a border on to indicate this-->
        <xsl:if
            test="$captionColour='white' or $captionColour='clear'  or  $captionColour='co07' or  $captionColour='co09' or  $captionColour='co05'  or  $captionColour='co06' or $translatevalue >10"
            > captionBorder</xsl:if>
        <xsl:choose>
            <xsl:when test="$captionColour = 'amber'"> captionColourAmber</xsl:when>
            <xsl:when test="$captionColour = 'co01' or $captionColour ='green'"> captionColourCo01</xsl:when>
            <xsl:when test="$captionColour = 'co02'"> captionColourCo02</xsl:when>
            <xsl:when test="$captionColour = 'co03'"> captionColourCo03</xsl:when>
            <xsl:when test="$captionColour = 'co04'"> captionColourCo04</xsl:when>
            <xsl:when test="$captionColour = 'co08'"> captionColourCo08</xsl:when>
            <xsl:otherwise>
                <xsl:if test="not($translatevalue= '') and not($translatevalue > 10)">
                    <xsl:attribute name="background-color">
                        <xsl:value-of select="@colour|@color"/>
                    </xsl:attribute>
                </xsl:if>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <!--=====处理对齐方式==========================================-->
    <xsl:template name="captionAlign">
        <xsl:attribute name="align">
            <xsl:if test="@align = 'CENTER' or @align = 'center'">center</xsl:if>
            <xsl:if test="@align= 'LEFT' or @align= 'left'">left</xsl:if>
            <xsl:if test="@align= 'RIGHT' or @align= 'right'">right</xsl:if>
        </xsl:attribute>
    </xsl:template>
    <!--===============================================-->
</xsl:stylesheet>
