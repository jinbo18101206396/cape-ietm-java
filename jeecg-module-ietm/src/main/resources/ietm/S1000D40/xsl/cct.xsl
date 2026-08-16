<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:fo="http://www.w3.org/1999/XSL/Format" xmlns:xlink="http://www.w3.org/1999/xlink"
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    xmlns:dc="http://www.purl.org/dc/elements/1.1/">
    <!-- ================================= -->
    <xsl:include href="fig_tab.xsl"/>
    <xsl:include href="wcnp.xsl"/>
    <!-- ================================= -->
    <xsl:template match="cct|condCrossRefTable">
        <xsl:call-template name="importCSS"/>
        <center>
            <h3>
                <xsl:value-of select="$v01167"/>
            </h3>
        </center>
        <p>
            <xsl:value-of select="$v01169"/>
        </p>
        <xsl:apply-templates/>
    </xsl:template>
    <!-- ================================= -->
    <xsl:template match="conditiontypelist|condTypeList">
        <p>
            <xsl:value-of select="$v01170"/>
        </p>
        <table border="1" cellpadding="5" width="100%" style="padding: 10pt">
            <tr class="dr-pnl-h rich-panel-header tabletitle">
                <th width="20%"><xsl:value-of select="$v00553"/></th>
                <th width="40%"><xsl:value-of select="$v00238"/></th>
                <th width="40%"><xsl:value-of select="$v01171"/></th>
            </tr>
            <xsl:apply-templates/>
        </table>
    </xsl:template>
    <!-- ================================= -->
    <xsl:template match="conditiontype|condType">
        <tr>
            <td><xsl:apply-templates select="name"/>&#160;</td>
            <td><xsl:apply-templates select="description|descr"/>&#160;</td>
            <td><xsl:apply-templates select="enum/@actvalues|enumeration/@applicPropertyValues"
                />&#160;</td>
        </tr>
    </xsl:template>
    <!-- ================================= -->
    <xsl:template match="conditionlist|condList">
        <p>Condition list</p>
        <table border="1" cellpadding="5" width="100%">
            <tr class="dr-pnl-h rich-panel-header tabletitle">
                <th width="30%"><xsl:value-of select="$v00553"/></th>
                <th width="70%"><xsl:value-of select="$v00238"/></th>
            </tr>
            <xsl:apply-templates/>
        </table>
    </xsl:template>
    <!-- ================================= -->
    <xsl:template match="condition|cond">
        <tr>
            <td><xsl:apply-templates select="name"/>&#160;</td>
            <td><xsl:apply-templates select="description"/>&#160;</td>
        </tr>
    </xsl:template>
    <!-- ================================= -->
    <xsl:template match="incorporation">
        <p>Incorporation</p>
        <xsl:apply-templates/>
    </xsl:template>
    <!-- ================================= -->
    <xsl:template match="refs[child::dmRef]">
        <xsl:apply-templates/>
    </xsl:template>
    <!-- ================================= -->
    <xsl:template match="techcondition|condIncorporation">
        <table border="1" cellpadding="5" width="100%" style="padding: 10pt">
            <tr class="dr-pnl-h rich-panel-header tabletitle">
                <th>
                    <xsl:value-of select="$v00773"/>
                </th>
                <th>
                    <xsl:value-of select="$v01172"/>
                </th>
            </tr>
            <xsl:apply-templates/>
        </table>
    </xsl:template>
    <!-- ================================= -->
    <xsl:template match="docincorp|documentIncorporation">
        <tr>
            <td><xsl:apply-templates select="refs"/></td>
            <td><xsl:apply-templates select="ics|incorporationStatus"/>&#160;</td>
        </tr>
    </xsl:template>
    <!-- ================================= -->
    <xsl:template match="ics|incorporationStatus">
        <xsl:value-of select="@incorpstatus|@incorporationStatus"/> - <xsl:value-of select="@day"/>/ <xsl:value-of
            select="@month"/>/ <xsl:value-of select="@year"/>
    </xsl:template>
    <!-- ================================= -->
</xsl:stylesheet>
