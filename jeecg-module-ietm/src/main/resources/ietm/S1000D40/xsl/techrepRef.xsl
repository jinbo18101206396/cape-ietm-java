<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	xmlns:tir-proc="java:java/com.ptc.arbortext.aad.ietp.tir.IetpTIRProcessor" 
    xmlns:tir-node="java:java/com.ptc.arbortext.aad.ietp.tir.IetpTIRNode">
    <!--2012 xmlns:tir-proc="java:java/com.ptc.arbortext.aad.ietp.tir.IetpTIRProcessor?path=file:///E:/IETM4/xsl/java/" exclude-result-prefixes="tir-proc"
    xmlns:tir-node="java:java/com.ptc.arbortext.aad.ietp.tir.IetpTIRNode?path=file:///E:/IETM4/xsl/java/" exclude-result-prefixes="tir-node"-->

    <!--========================================================================================-->
    <!-- Handles a reference to a supply requirement                                            -->
    <!-- The reference is firstly validated to determine if the reference is valid. If it is, a -->
    <!-- TIR link is generated that points to the specified TIR.                                -->
    <!-- param: annotation    The annotation used for rendering the reference                   -->
    <!--========================================================================================-->
    <xsl:template match="ein | functionalItemRef" name="t_ein_ref">
        <xsl:param name="annotation">EIN:</xsl:param>
        <xsl:variable name="id">
            <xsl:value-of select="@einnbr|@functionalItemNumber"/>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="tir-proc:validateTIRReference(.)">
                <xsl:variable name="tirItem" select="tir-proc:dereferenceTIRNode(.)"/>
                <xsl:variable name="tirPath" select="tir-node:getQueryPath($tirItem)"/>
                <xsl:variable name="tirName">
                    <xsl:variable name="numAlts">
                        <xsl:value-of
                            select="count(tir-node:query($tirItem, '//functionalItemAlt|//einalt'))"
                        />
                    </xsl:variable>
                    <xsl:choose>
                       
                        <xsl:when
                            test="$numAlts = 1 and tir-node:query($tirItem, '//functionalItemAlt/name|//einalt/nomen')">
                            <xsl:value-of
                                select="tir-node:query($tirItem, '//einalt/nomen/text()|//functionalItemAlt/name/text()')"
                            />
                        </xsl:when>
                        
                        <xsl:when
                            test="($numAlts &gt; 1 and tir-node:query($tirItem, './name/text()|./nomen')) or ($numAlts = 1 and not(tir-node:query($tirItem, '//functionalItemAlt/name|//einalt/nomen')))">
                            <xsl:value-of
                                select="tir-node:query($tirItem, '//name/text()|//nomen/text()')"/>
                        </xsl:when>
                       
                        <xsl:otherwise>
                            <xsl:value-of select="nomen/text()|name/text()"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>
                <span class="tirRef" title="{$v02478} {$id}, {$tirName}">
                    <xsl:call-template name="t_generate_tir_click_event">
                        <xsl:with-param name="tirItem" select="$tirItem"/>
                    </xsl:call-template>
                    <xsl:value-of select="$annotation"/>&#160;<xsl:value-of select="$id"
                        />,&#160;<xsl:value-of select="$tirName"/>
                </span>
            </xsl:when>
            <xsl:otherwise>
                <span title="{$v02115}">
                    <img src="./images/wi0028-16.gif"/>
                    <xsl:value-of select="$annotation"/>
                    <xsl:value-of select="$id"/>
                </span>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!--========================================================================================-->
    <!-- Handles a reference to a circuit breaker                                               -->
    <!-- The reference is firstly validated to determine if the reference can be resolved to a  -->
    <!-- TIR item. If it is, a  TIR link is generated that points to the specified TIR.         -->
    <!-- The name is derived by interrogating the TIR node to determine the number of alternate -->
    <!-- items and selecting one based upon the following rules                                 -->
    <!-- -If there are no alternate items, the container name is used.                          -->
    <!-- -If there is more than one applicable alternate, the parent is used.                   -->
    <!-- -If there is one single applicable alternate item, its name is used.                   -->
    <!--========================================================================================-->
    <xsl:template match="cb | circuitBreakerRef" name="cbRef">
        <xsl:param name="annotation">CB:</xsl:param>
        <xsl:variable name="id">
            <xsl:value-of select="@cbnbr|@circuitBreakerNumber"/>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="tir-proc:validateTIRReference(.)">
                <xsl:variable name="tirItem" select="tir-proc:dereferenceTIRNode(.)"/>
                <xsl:variable name="tirName">
                    <xsl:variable name="numAlts">
                        <xsl:value-of
                            select="count(tir-node:query($tirItem, '//cbalt|//circuitBreakerAlt'))"
                        />
                    </xsl:variable>
                    <xsl:choose>
                        <xsl:when
                            test="$numAlts = 1 and tir-node:query($tirItem, '//cbalt/nomen|//circuitBreakerAlt/name')">
                            <xsl:value-of
                                select="tir-node:query($tirItem, '//cbalt/nomen/text()|//circuitBreakerAlt/name/text()')"
                            />
                        </xsl:when>
                        <xsl:when
                            test="($numAlts &gt; 1 or ($numAlts = 1 and not(tir-node:query($tirItem, '//cbalt/nomen/text()|//circuitBreakerAlt/name/text()')))) and tir-node:query($tirItem, '//name|//nomen')">
                            <xsl:value-of
                                select="tir-node:query($tirItem, '//name/text()|//nomen/text()')"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="./nomen/text()|./name/text()"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>
                <span class="tirRef" title="{$v02476} {$id}, {$tirName}">
                    <xsl:call-template name="t_generate_tir_click_event">
                        <xsl:with-param name="tirItem" select="$tirItem"/>
                    </xsl:call-template>
                    <xsl:value-of select="$annotation"/>&#160;<xsl:value-of select="$id"
                        /><xsl:if test="$tirName">,&#160;<xsl:value-of select="$tirName"
                    /></xsl:if>
                </span>
            </xsl:when>
            <xsl:otherwise>
                <span title="{$v02115}">
                    <img src="./images/wi0028-16.gif"/>
                    <xsl:value-of select="$annotation"/>
                    <xsl:value-of select="$id"/>
                </span>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <!--========================================================================================-->
    <!-- Handles an inferred reference to a part                                                -->
    <!-- If an element with both an mfc and part number child is found, then this element may   -->
    <!-- be a TIR reference. If a TIR item is found within the repository, then we display this -->
    <!-- as a TIR link.																			-->
    <!--========================================================================================-->
    <xsl:template match="*[child::mfc|child::manufacturerCode][descendant::partno|descendant::pnr|descendant::partNumber][not(ancestor::ipc|ancestor::illustratedPartsCatalog)]" priority="3">
        <xsl:variable name="mfc">
            <xsl:value-of select=".//mfc|.//manufacturerCode"/>
        </xsl:variable>
        <xsl:variable name="pnr">
            <xsl:value-of select=".//pnr|.//partNumber"/>
        </xsl:variable>
       	<xsl:choose>
            <xsl:when test="tir-proc:validateTIRReference(.)">
                <xsl:variable name="tirItem" select="tir-proc:dereferenceTIRNode(.)"/>
                <xsl:variable name="tirName">
                    <xsl:value-of
                        select="tir-node:query($tirItem, '//itemIdentData/name/text()|//nomdata/nomen/text()')"
                    />
                </xsl:variable>
                <span class="tirRef" title="{$v00534}: {$mfc} / {$v00645}: {$pnr}, {$tirName}">
                    <xsl:call-template name="t_generate_tir_click_event">
                        <xsl:with-param name="tirItem" select="$tirItem"/>
                    </xsl:call-template><xsl:value-of select="$v00534"/>:&#160;<xsl:value-of
                        select="$mfc"/> / <xsl:value-of select="$v00645"/>: <xsl:value-of
                            select="$pnr"/><xsl:if test="$tirName">,&#160;<xsl:value-of
                                select="$tirName"/></xsl:if>
                </span>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$v00534"/>: <xsl:value-of select="$mfc"/> / <xsl:value-of
                        select="$v00645"/>: <xsl:value-of select="$pnr"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <!--========================================================================================-->
    <!-- Handles an explicit or implied reference to a part                                                          -->
    <!--========================================================================================-->
    <xsl:template match="part|partRef" name="partRef">
        <xsl:variable name="mfc">
            <xsl:value-of select="@mfc|@manufacturerCodeValue"/>
        </xsl:variable>
        <xsl:variable name="pnr">
            <xsl:value-of select="@pnr|@partNumberValue"/>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="tir-proc:validateTIRReference(.)">
                <xsl:variable name="tirItem" select="tir-proc:dereferenceTIRNode(.)"/>
                <xsl:variable name="tirName">
                    <xsl:value-of
                        select="tir-node:query($tirItem, '//itemIdentData/name/text()|//nomdata/nomen/text()')"
                    />
                </xsl:variable>
                <span class="tirRef" title="{$v00534}: {$mfc} / {$v00645}: {$pnr}, {$tirName}">
                    <xsl:call-template name="t_generate_tir_click_event">
                        <xsl:with-param name="tirItem" select="$tirItem"/>
                    </xsl:call-template><xsl:value-of select="$v00534"/>:&#160;<xsl:value-of
                        select="$mfc"/> / <xsl:value-of select="$v00645"/>: <xsl:value-of
                        select="$pnr"/><xsl:if test="$tirName">,&#160;<xsl:value-of
                            select="$tirName"/></xsl:if>
                </span>
            </xsl:when>
            <xsl:otherwise>
                <span title="{$v02115}">
                    <img src="./images/wi0028-16.gif"/>
                    <xsl:value-of select="$v00534"/>: <xsl:value-of select="$mfc"/> / <xsl:value-of
                        select="$v00645"/>: <xsl:value-of select="$pnr"/>
                </span>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!--========================================================================================-->
    <!-- Handles a reference to an organisation or enterprise                                   -->
    <!-- When any element is encountered that contains a single valid enterprise reference,     -->
    <!-- then we display a link to an organisation/enterprise instance. If the construct also   -->
    <!-- contains a part number, then this template is ignored, as this is actually a part      -->
    <!-- reference                                                                              -->
    <!--========================================================================================-->
    <xsl:template
        match="*[child::mfc|child::manufacturerCode][not(descendant::partno|descendant::pnr|descendant::partNumber)]"
        priority="3">
        <xsl:variable name="mfc">
            <xsl:value-of select=".//mfc|.//manufacturerCode"/>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="tir-proc:validateTIRReference(.)">
                <xsl:variable name="tirItem" select="tir-proc:dereferenceTIRNode(.)"/>
                <xsl:variable name="tirName">
                    <xsl:value-of
                        select="tir-node:query($tirItem, '//enterpriseName/text()|//ent-name/text()')"
                    />
                </xsl:variable>
                <span class="tirRef" title="{$v00534}: {$mfc}">
                    <xsl:call-template name="t_generate_tir_click_event">
                        <xsl:with-param name="tirItem" select="$tirItem"/>
                    </xsl:call-template><xsl:value-of select="$v00534"/>:&#160;<xsl:value-of
                        select="$mfc"/>, <xsl:value-of select="$tirName"/>
                </span>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$v00534"/>&#160;<xsl:value-of select="$mfc"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!--========================================================================================-->
    <!-- Handles a reference to an organisation or enterprise                                   -->
    <!--========================================================================================-->
    <xsl:template match="organisationref|enterpriseRef" name="t_org_ref" priority="3">
        <xsl:variable name="mfc">
            <xsl:value-of select="@mfc|@manufacturerCode"/>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="tir-proc:validateTIRReference(.)">
                <xsl:variable name="tirItem" select="tir-proc:dereferenceTIRNode(.)"/>
                <xsl:variable name="tirName">
                    <xsl:value-of
                        select="tir-node:query($tirItem, '//enterpriseName/text()|//ent-name/text()')"
                    />
                </xsl:variable>
                <span class="tirRef" title="{$v00534}: {$mfc}">
                    <xsl:call-template name="t_generate_tir_click_event">
                        <xsl:with-param name="tirItem" select="$tirItem"/>
                    </xsl:call-template><xsl:value-of select="$v00534"/>:&#160;<xsl:value-of
                        select="$mfc"/>, <xsl:value-of select="$tirName"/>
                </span>
            </xsl:when>
            <xsl:otherwise>
                <span title="{$v02115}">
                    <img src="./images/wi0028-16.gif"/>
                    <xsl:value-of select="$v00534"/>: <xsl:value-of select="$mfc"/>
                </span>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!--========================================================================================-->
    <!-- Handles a reference to a zone                                                          -->
    <!-- The reference is firstly validated to determine if the reference is valid. If it is, a -->
    <!-- TIR link is generated that points to the specified TIR.                                -->
    <!--========================================================================================-->
    <xsl:template match="zone | zoneRef" name="t_zone_ref">
        <xsl:param name="annotation"><xsl:value-of select="$v02145"/>:</xsl:param>
        <xsl:variable name="id">
            <xsl:value-of select="@zoneNumber"/>
        </xsl:variable>
       	<xsl:choose>
            <xsl:when test="tir-proc:validateTIRReference(.)">
                <xsl:variable name="tirItem" select="tir-proc:dereferenceTIRNode(.)"/>
                <span class="tirRef" title="{$v02479} {$id}, {./nomen|./name}">
                    <xsl:call-template name="t_generate_tir_click_event">
                        <xsl:with-param name="tirItem" select="$tirItem"/>
                    </xsl:call-template>
                    <xsl:value-of select="$annotation"/>&#160;<xsl:value-of select="$id"
                        /><xsl:if test="./name">,&#160;<xsl:value-of select="./name"/></xsl:if>
                </span>
            </xsl:when>
            <xsl:otherwise>
                <span title="{$v02115}">
                    <img src="./images/wi0028-16.gif"/>
                    <xsl:value-of select="$annotation"/>
                    <xsl:value-of select="$id"/>
                </span>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!--========================================================================================-->
    <!-- Handles a reference to an access point                                                 -->
    <!-- The reference is firstly validated to determine if the reference can be resolved to a  -->
    <!-- TIR item. If it is, a  TIR link is generated that points to the specified TIR.         -->
    <!-- The query path is the xpath expression that will resolve to the exact TIR expressed by -->
    <!-- the reference node and is passed to the javascript event to open the TIR.              -->
    <!--========================================================================================-->
    <xsl:template match="accpnl|accessPointRef">
        <xsl:param name="annotation">APT:</xsl:param>
        <xsl:variable name="id">
            <xsl:value-of select="@accpnlnbr|@accessPointNumber"/>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="tir-proc:validateTIRReference(.)">
                <xsl:variable name="tirItem" select="tir-proc:dereferenceTIRNode(.)"/>
                <span class="tirRef" title="{$v02481} {$id}">
                    <xsl:call-template name="t_generate_tir_click_event">
                        <xsl:with-param name="tirItem" select="$tirItem"/>
                    </xsl:call-template>
                    <xsl:value-of select="$annotation"/>&#160;<xsl:value-of select="$id"/>
                    <xsl:if test="nomen|name">, <xsl:value-of select="nomen|name"/>
                    </xsl:if>
                </span>
            </xsl:when>
            <xsl:otherwise>
                <span title="{$v02115}">
                    <img src="./images/wi0028-16.gif"/>
                    <xsl:value-of select="$annotation"/>
                    <xsl:value-of select="$id"/>
                </span>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!--========================================================================================-->
    <!-- Handles an implied reference to a supply requirement                                   -->
    <!-- The reference is firstly validated to determine if the reference is valid. If it is, a -->
    <!-- TIR link is generated that points to the specified TIR.                                -->
    <!--========================================================================================-->
    <xsl:template match="con | supplyRqmtRef" name="conRef">
        <xsl:param name="annotation">CON:</xsl:param>
        <xsl:variable name="id">
            <xsl:value-of select="@connbr|@supplyRqmtNumber"/>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="tir-proc:validateTIRReference(.)">
                <xsl:variable name="tirItem" select="tir-proc:dereferenceTIRNode(.)"/>
                <xsl:variable name="tirName"
                    select="tir-node:query($tirItem, './supplyRqmtAlt/name/text()')"/>
                <span class="tirRef" title="{$v02484} {$id}, {$tirName}">
                    <xsl:call-template name="t_generate_tir_click_event">
                        <xsl:with-param name="tirItem" select="$tirItem"/>
                    </xsl:call-template>
                    <xsl:value-of select="$annotation"/>&#160;<xsl:value-of select="$id"
                        />,&#160;<xsl:value-of select="$tirName"/>
                </span>
            </xsl:when>
            <xsl:otherwise>
                <span title="{$v02115}">
                    <img src="./images/wi0028-16.gif"/>
                    <xsl:value-of select="$annotation"/>
                    <xsl:value-of select="$id"/>
                </span>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!--========================================================================================-->
    <!-- Handles a reference to a supply product                                                -->
    <!-- The reference is firstly validated to determine if the reference is valid. If it is, a -->
    <!-- TIR link is generated that points to the specified TIR.                                -->
    <!--========================================================================================-->
    <xsl:template match="supplyRef" name="supplyRef">
        <xsl:param name="annotation">SUP:</xsl:param>
        <xsl:variable name="id">
            <xsl:value-of select="@supnbr|@supplyNumber"/>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="tir-proc:validateTIRReference(.)">
                <xsl:variable name="tirItem" select="tir-proc:dereferenceTIRNode(.)"/>
                <xsl:variable name="tirName" select="tir-node:query($tirItem, './name/text()')"/>
                <span class="tirRef" title="{$v02485} {$id}, {$tirName}">
                    <xsl:call-template name="t_generate_tir_click_event">
                        <xsl:with-param name="tirItem" select="$tirItem"/>
                    </xsl:call-template>
                    <xsl:value-of select="$annotation"/>&#160;<xsl:value-of select="$id"
                        />,&#160;<xsl:value-of select="$tirName"/>
                </span>
            </xsl:when>
            <xsl:otherwise>
                <span title="{$v02115}">
                    <img src="./images/wi0028-16.gif"/>
                    <xsl:value-of select="$annotation"/>
                    <xsl:value-of select="$id"/>
                </span>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!--========================================================================================-->
    <!-- Handles a reference to a support equipment item or tool (issue 4.0)                    -->
    <!--========================================================================================-->
    <xsl:template match="tool|toolRef" name="toolRef">
        <xsl:param name="annotation">Tool:</xsl:param>
        <xsl:variable name="id">
            <xsl:value-of select="@toolnbr|@toolNumber"/>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="tir-proc:validateTIRReference(.)">
                <xsl:variable name="tirItem" select="tir-proc:dereferenceTIRNode(.)"/>
                <xsl:variable name="tirName"
                    select="tir-node:query($tirItem, '//nomdata/nomen/text()|//itemIdentData/name/text()')"/>
                <span class="tirRef" title="{$v02486} {$id}, {$tirName}">
                    <xsl:call-template name="t_generate_tir_click_event">
                        <xsl:with-param name="tirItem" select="$tirItem"/>
                    </xsl:call-template>
                    <xsl:value-of select="$annotation"/>&#160;<xsl:value-of select="$id"
                        />,&#160;<xsl:value-of select="$tirName"/>
                </span>
            </xsl:when>
            <xsl:otherwise>
                <span title="{$v02115}">
                    <img src="./images/wi0028-16.gif"/>
                    <xsl:value-of select="$annotation"/>
                    <xsl:value-of select="$id"/>
                </span>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!--========================================================================================-->
    <!-- Handles a reference to a functional physical area                                      -->
    <!-- The reference is firstly validated to determine if the reference is valid. If it is, a -->
    <!-- TIR link is generated that points to the specified TIR.                                -->
    <!--========================================================================================-->
    <xsl:template match="functionalPhysicalAreaRef" name="t_func_phys_area_ref">
        <xsl:param name="annotation">EIN Area:</xsl:param>
        <xsl:variable name="id">
            <xsl:call-template name="t_format_functional_area_id"/>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="tir-proc:validateTIRReference(.)">
                <xsl:variable name="tirItem" select="tir-proc:dereferenceTIRNode(.)"/>
                <xsl:variable name="tirName" select="tir-node:query($tirItem, './name/text()')"/>
                <span class="tirRef" title="{$v02489} {$id}, {$tirName}">
                    <xsl:call-template name="t_generate_tir_click_event">
                        <xsl:with-param name="tirItem" select="$tirItem"/>
                    </xsl:call-template>
                    <xsl:value-of select="$annotation"/>&#160;<xsl:value-of select="$id"
                        /><xsl:if test="$tirName">,&#160;<xsl:value-of select="$tirName"
                    /></xsl:if>
                </span>
            </xsl:when>
            <xsl:otherwise>
                <span title="{$v02115}">
                    <img src="./images/wi0028-16.gif"/>
                    <xsl:value-of select="$annotation"/>
                    <xsl:value-of select="$id"/>
                </span>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!--========================================================================================-->
    <!-- Handles a reference to a control indicator.                                            -->
    <!--========================================================================================-->
    <xsl:template match="controlIndicatorRef">
        <xsl:param name="annotation">CI:</xsl:param>
        <xsl:variable name="id">
            <xsl:value-of select="@controlIndicatorNumber"/>
        </xsl:variable>
       	<xsl:choose>
            <xsl:when test="tir-proc:validateTIRReference(.)">
                <xsl:variable name="tirItem" select="tir-proc:dereferenceTIRNode(.)"/>
                <xsl:variable name="tirName"
                    select="tir-node:query($tirItem, './controlIndicatorName/text()')"/>
                <span class="tirRef" title="Reference to control indicator: {$id}, {$tirName}">
                    <xsl:call-template name="t_generate_tir_click_event">
                        <xsl:with-param name="tirItem" select="$tirItem"/>
                    </xsl:call-template>
                    <xsl:value-of select="$annotation"/>&#160;<xsl:value-of select="$id"
                        /><xsl:if test="$tirName">,&#160;<xsl:value-of select="$tirName"
                    /></xsl:if>
                </span>
            </xsl:when>
            <xsl:otherwise>
                <span title="{$v02115}">
                    <img src="./images/wi0028-16.gif"/>
                    <xsl:value-of select="$annotation"/>
                    <xsl:value-of select="$id"/>
                </span>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!--========================================================================================-->
    <!-- Formats the partial DMC that comprises the identifier for a physical and/or functional -->
    <!-- area reference. The formatting follows the same principles that a complete DMC follows -->
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
    <!-- Generates the correct onclick event for a tir reference. This may be embedded within   -->
    <!-- any HTML component that supports an onclick event.                                     -->
    <!--========================================================================================-->
    <xsl:template name="t_generate_tir_click_event">
        <xsl:param name="tirItem" select="tir-proc:dereferenceTIRNode(.)"/>
        <xsl:variable name="tirPath" select="tir-node:getQueryPath($tirItem)"/>
        <xsl:attribute name="onclick">if (isFireFox()) tirEvent = event;
                tirPopupClicked("<xsl:value-of select="$tirPath"/>");</xsl:attribute>
    </xsl:template>

</xsl:stylesheet>
