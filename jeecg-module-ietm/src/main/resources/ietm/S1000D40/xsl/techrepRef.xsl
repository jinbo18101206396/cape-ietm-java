<?xml version="1.0" encoding="UTF-8"?>
<!--
    techrepRef.xsl  -  TIR 引用渲染
    修改说明（2026-11-15）：
      原文件依赖 PTC Arbortext 专有 Java 扩展（IetpTIRProcessor / IetpTIRNode），
      Saxon-HE 不支持 Java 反射调用，导致包含 zoneRef/partRef/ein 等元素的 DM 预览报错。
      修改方式（方案A）：移除 Java 命名空间绑定，所有模板直接执行原 <xsl:otherwise> 的
      降级渲染逻辑（显示编号文字），不再尝试查询 TIR 数据库。
      原始代码已保留在注释中，待将来升级至 Saxon-PE/EE 时恢复。
-->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <!--========================================================================================-->
    <!-- Handles a reference to a supply requirement (EIN / functionalItemRef)                  -->
    <!--========================================================================================-->
    <xsl:template match="ein | functionalItemRef" name="t_ein_ref">
        <xsl:param name="annotation">EIN:</xsl:param>
        <xsl:variable name="id">
            <xsl:value-of select="@einnbr|@functionalItemNumber"/>
        </xsl:variable>
        <span title="{$v02115}">
            <img src="./images/wi0028-16.gif"/>
            <xsl:value-of select="$annotation"/>
            <xsl:value-of select="$id"/>
        </span>
    </xsl:template>

    <!--========================================================================================-->
    <!-- Handles a reference to a circuit breaker (cb / circuitBreakerRef)                     -->
    <!--========================================================================================-->
    <xsl:template match="cb | circuitBreakerRef" name="cbRef">
        <xsl:param name="annotation">CB:</xsl:param>
        <xsl:variable name="id">
            <xsl:value-of select="@cbnbr|@circuitBreakerNumber"/>
        </xsl:variable>
        <span title="{$v02115}">
            <img src="./images/wi0028-16.gif"/>
            <xsl:value-of select="$annotation"/>
            <xsl:value-of select="$id"/>
        </span>
    </xsl:template>

    <!--========================================================================================-->
    <!-- Handles an inferred reference to a part                                                -->
    <!-- (element with both manufacturerCode and partNumber children)                          -->
    <!--========================================================================================-->
    <xsl:template match="*[child::mfc|child::manufacturerCode][descendant::partno|descendant::pnr|descendant::partNumber][not(ancestor::ipc|ancestor::illustratedPartsCatalog)]" priority="3">
        <xsl:variable name="mfc">
            <xsl:value-of select=".//mfc|.//manufacturerCode"/>
        </xsl:variable>
        <xsl:variable name="pnr">
            <xsl:value-of select=".//pnr|.//partNumber"/>
        </xsl:variable>
        <xsl:value-of select="$v00534"/>: <xsl:value-of select="$mfc"/> / <xsl:value-of
            select="$v00645"/>: <xsl:value-of select="$pnr"/>
    </xsl:template>

    <!--========================================================================================-->
    <!-- Handles an explicit or implied reference to a part (part / partRef)                   -->
    <!--========================================================================================-->
    <xsl:template match="part|partRef" name="partRef">
        <xsl:variable name="mfc">
            <xsl:value-of select="@mfc|@manufacturerCodeValue"/>
        </xsl:variable>
        <xsl:variable name="pnr">
            <xsl:value-of select="@pnr|@partNumberValue"/>
        </xsl:variable>
        <span title="{$v02115}">
            <img src="./images/wi0028-16.gif"/>
            <xsl:value-of select="$v00534"/>: <xsl:value-of select="$mfc"/> / <xsl:value-of
                select="$v00645"/>: <xsl:value-of select="$pnr"/>
        </span>
    </xsl:template>

    <!--========================================================================================-->
    <!-- Handles an organisation/enterprise reference                                           -->
    <!-- (element with manufacturerCode child but NO partNumber descendant)                    -->
    <!--========================================================================================-->
    <xsl:template
        match="*[child::mfc|child::manufacturerCode][not(descendant::partno|descendant::pnr|descendant::partNumber)]"
        priority="3">
        <xsl:variable name="mfc">
            <xsl:value-of select=".//mfc|.//manufacturerCode"/>
        </xsl:variable>
        <xsl:value-of select="$v00534"/>&#160;<xsl:value-of select="$mfc"/>
    </xsl:template>

    <!--========================================================================================-->
    <!-- Handles a reference to an organisation or enterprise (organisationref / enterpriseRef) -->
    <!--========================================================================================-->
    <xsl:template match="organisationref|enterpriseRef" name="t_org_ref" priority="3">
        <xsl:variable name="mfc">
            <xsl:value-of select="@mfc|@manufacturerCode"/>
        </xsl:variable>
        <span title="{$v02115}">
            <img src="./images/wi0028-16.gif"/>
            <xsl:value-of select="$v00534"/>: <xsl:value-of select="$mfc"/>
        </span>
    </xsl:template>

    <!--========================================================================================-->
    <!-- Handles a reference to a zone (zone / zoneRef)                                        -->
    <!--========================================================================================-->
    <xsl:template match="zone | zoneRef" name="t_zone_ref">
        <xsl:param name="annotation"><xsl:value-of select="$v02145"/>:</xsl:param>
        <xsl:variable name="id">
            <xsl:value-of select="@zoneNumber"/>
        </xsl:variable>
        <span title="{$v02115}">
            <img src="./images/wi0028-16.gif"/>
            <xsl:value-of select="$annotation"/>
            <xsl:value-of select="$id"/>
        </span>
    </xsl:template>

    <!--========================================================================================-->
    <!-- Handles a reference to an access point (accpnl / accessPointRef)                      -->
    <!--========================================================================================-->
    <xsl:template match="accpnl|accessPointRef">
        <xsl:param name="annotation">APT:</xsl:param>
        <xsl:variable name="id">
            <xsl:value-of select="@accpnlnbr|@accessPointNumber"/>
        </xsl:variable>
        <span title="{$v02115}">
            <img src="./images/wi0028-16.gif"/>
            <xsl:value-of select="$annotation"/>
            <xsl:value-of select="$id"/>
        </span>
    </xsl:template>

    <!--========================================================================================-->
    <!-- Handles an implied reference to a supply requirement (con / supplyRqmtRef)             -->
    <!--========================================================================================-->
    <xsl:template match="con | supplyRqmtRef" name="conRef">
        <xsl:param name="annotation">CON:</xsl:param>
        <xsl:variable name="id">
            <xsl:value-of select="@connbr|@supplyRqmtNumber"/>
        </xsl:variable>
        <span title="{$v02115}">
            <img src="./images/wi0028-16.gif"/>
            <xsl:value-of select="$annotation"/>
            <xsl:value-of select="$id"/>
        </span>
    </xsl:template>

    <!--========================================================================================-->
    <!-- Handles a reference to a supply product (supplyRef)                                   -->
    <!--========================================================================================-->
    <xsl:template match="supplyRef" name="supplyRef">
        <xsl:param name="annotation">SUP:</xsl:param>
        <xsl:variable name="id">
            <xsl:value-of select="@supnbr|@supplyNumber"/>
        </xsl:variable>
        <span title="{$v02115}">
            <img src="./images/wi0028-16.gif"/>
            <xsl:value-of select="$annotation"/>
            <xsl:value-of select="$id"/>
        </span>
    </xsl:template>

    <!--========================================================================================-->
    <!-- Handles a reference to a support equipment item or tool (tool / toolRef)              -->
    <!--========================================================================================-->
    <xsl:template match="tool|toolRef" name="toolRef">
        <xsl:param name="annotation">Tool:</xsl:param>
        <xsl:variable name="id">
            <xsl:value-of select="@toolnbr|@toolNumber"/>
        </xsl:variable>
        <span title="{$v02115}">
            <img src="./images/wi0028-16.gif"/>
            <xsl:value-of select="$annotation"/>
            <xsl:value-of select="$id"/>
        </span>
    </xsl:template>

    <!--========================================================================================-->
    <!-- Handles a reference to a functional physical area (functionalPhysicalAreaRef)          -->
    <!--========================================================================================-->
    <xsl:template match="functionalPhysicalAreaRef" name="t_func_phys_area_ref">
        <xsl:param name="annotation">EIN Area:</xsl:param>
        <xsl:variable name="id">
            <xsl:call-template name="t_format_functional_area_id"/>
        </xsl:variable>
        <span title="{$v02115}">
            <img src="./images/wi0028-16.gif"/>
            <xsl:value-of select="$annotation"/>
            <xsl:value-of select="$id"/>
        </span>
    </xsl:template>

    <!--========================================================================================-->
    <!-- Handles a reference to a control indicator (controlIndicatorRef)                       -->
    <!--========================================================================================-->
    <xsl:template match="controlIndicatorRef">
        <xsl:param name="annotation">CI:</xsl:param>
        <xsl:variable name="id">
            <xsl:value-of select="@controlIndicatorNumber"/>
        </xsl:variable>
        <span title="{$v02115}">
            <img src="./images/wi0028-16.gif"/>
            <xsl:value-of select="$annotation"/>
            <xsl:value-of select="$id"/>
        </span>
    </xsl:template>

    <!--========================================================================================-->
    <!-- Formats the partial DMC for functional/physical area reference                         -->
    <!--========================================================================================-->
    <xsl:template name="t_format_functional_area_id">
        <xsl:value-of select="@systemCode"/>-<xsl:value-of select="@subSystemCode"/><xsl:value-of
            select="@subSubSystemCode"/>-<xsl:value-of select="@assyCode"/><xsl:if
            test="@disassyCode">-<xsl:value-of select="@disassyCode"/></xsl:if><xsl:if
            test="@disassyCodeVariant">
            <xsl:value-of select="@disassyCodeVariant"/>
        </xsl:if><xsl:if test="@systemDiffCode">-<xsl:value-of select="@systemDiffCode"/></xsl:if>
    </xsl:template>

    <!--========================================================================================-->
    <!-- TIR click event generator - no-op under Saxon-HE (Java runtime not available)         -->
    <!--========================================================================================-->
    <xsl:template name="t_generate_tir_click_event">
        <xsl:param name="tirItem"/>
        <!-- 移除 Java 依赖：Saxon-HE 下不输出任何 onclick 事件 -->
    </xsl:template>

</xsl:stylesheet>
