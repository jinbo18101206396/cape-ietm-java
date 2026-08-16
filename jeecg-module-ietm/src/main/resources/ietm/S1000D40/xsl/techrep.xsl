<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:fo="http://www.w3.org/1999/XSL/Format" xmlns:xlink="http://www.w3.org/1999/xlink"
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    xmlns:dc="http://www.purl.org/dc/elements/1.1/">
    <!--2012 xmlns:tir-proc="java:java/com.ptc.arbortext.aad.ietp.tir.IetpTIRProcessor?path=file:///E:/IETM4/xsl/java/" exclude-result-prefixes="tir-proc"
    xmlns:tir-node="java:java/com.ptc.arbortext.aad.ietp.tir.IetpTIRNode?
path=file:///E:/IETM4/xsl/java/" exclude-result-prefixes="tir-node"-->
    <xsl:output method="html"/>
    <!--========================================================================================-->
    <!-- Initially extracts all xref/internalRef nodes and uses the TIR Processor to resolve all-->
    <!-- figures that are referenced by them and applies templates on those results             -->
    <!--========================================================================================-->
    <xsl:template match="/">
        <xsl:if test="$context='fragment'">
        <!--2012 <xsl:apply-templates select="tir-proc:extractReferencedFigures(//xref|//internalRef)"
                mode="techrep"/>-->
        </xsl:if>
        <xsl:apply-templates/>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="tir-fragment">
        <xsl:apply-templates/>
    </xsl:template>
    <!--========================================================================================-->
    <!-- Defines the output format of a generic row                                             -->
    <!--========================================================================================-->
    <xsl:template name="t_generic_row_presentation">
        <xsl:param name="title"/>
        <xsl:param name="value"/>
        <tr>
            <td style="width: 35%;">
                <xsl:value-of select="$title"/>
            </td>
            <td style="width: 65%" colspan="2">
                <xsl:value-of select="$value"/>
            </td>
        </tr>
    </xsl:template>
    <!--========================================================================================-->
    <!-- Defines the output format of a generic row where the data content is the result of     -->
    <!-- applying templates                                                                     -->
    <!--========================================================================================-->
    <xsl:template name="t_applied_generic_row_presentation">
        <xsl:param name="title"/>
        <tr>
            <td style="width: 35%;">
                <xsl:value-of select="$title"/>
            </td>
            <td style="width: 65%" colspan="2">
                <xsl:apply-templates/>
            </td>
        </tr>
    </xsl:template>
    <!--========================================================================================-->
    <!-- Defines the output format of a generic row where the data content is the result of     -->
    <!-- applying templates on the self-context node                                            -->
    <!--========================================================================================-->
    <xsl:template name="t_applied_self_generic_row_presentation">
        <xsl:param name="title"/>
        <tr>
            <td style="width: 35%;">
                <xsl:value-of select="$title"/>
            </td>
            <td style="width: 65%" colspan="2">
                <xsl:apply-templates select="."/>
            </td>
        </tr>
    </xsl:template>
    <!--========================================================================================-->
    <!-- Generic output for handling those elements that are re-used across a number of TIR     -->
    <!-- types                                                                                  -->
    <!--========================================================================================-->
    <xsl:template match="*/applic" mode="tir">
        <tr>
            <td style="width: 35%"><xsl:value-of select="$v00034"/></td>
            <td style="width: 65%" colspan="2">
                <xsl:apply-templates/>
            </td>
        </tr>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="nomen|name">
        <xsl:call-template name="t_generic_row_presentation">
            <xsl:with-param name="title"><xsl:value-of select="$v02220"/></xsl:with-param>
            <xsl:with-param name="value">
                <xsl:apply-templates/>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="sns">
        <xsl:call-template name="t_generic_row_presentation">
            <xsl:with-param name="title">SNS</xsl:with-param>
            <xsl:with-param name="value">
                <xsl:apply-templates/>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="itemDescr">
        <tr>
            <td style="width: 35%"><xsl:value-of select="$v02524"/></td>
            <td style="width: 65%" colspan="2">
                <xsl:apply-templates/>
            </td>
        </tr>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="procdata|procurementData">
        <tr>
            <td style="width: 35%; font-weight: bold"><xsl:value-of select="$v02176"/></td>
            <td style="width: 65%" colspan="2"/>
        </tr>
        <xsl:apply-templates/>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="spl|supplierCode">
        <xsl:call-template name="t_generic_row_presentation">
            <xsl:with-param name="title"><xsl:value-of select="$v02182"/></xsl:with-param>
            <xsl:with-param name="value">
                <xsl:apply-templates/>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="osc|optionalSupplierCode">
        <xsl:call-template name="t_generic_row_presentation">
            <xsl:with-param name="title"><xsl:value-of select="$v02232"/></xsl:with-param>
            <xsl:with-param name="value">
                <xsl:apply-templates/>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="bfe|buyerFurnishedEquipFlag">
        <xsl:call-template name="t_generic_row_presentation">
            <xsl:with-param name="title"><xsl:value-of select="$v02234"/></xsl:with-param>
            <xsl:with-param name="value"> Yes </xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="techdata|techData">
        <tr>
            <td style="font-weight: bold"><xsl:value-of select="$v02186"/></td>
            <td style="width: 65%" colspan="2"/>
        </tr>
        <xsl:apply-templates/>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="note">
        <dl class="noteBlock">
            <dt class="noteTitle">
                <xsl:choose>
                    <xsl:when test="following-sibling::note or preceding-sibling::note">
                        <xsl:value-of select="$v00598"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="$v00594"/>
                    </xsl:otherwise>
                </xsl:choose>
            </dt>
            <dd>
                <xsl:apply-templates/>
            </dd>
        </dl>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="zone|zoneRef" mode="tir">
        <tr>
            <td style="width: 35%"><xsl:value-of select="$v02145"/></td>
            <td style="width: 65%" colspan="2">
                <xsl:apply-templates select="."/>
            </td>
        </tr>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="accpnl|accessPointRef" mode="tir">
        <tr>
            <td style="width: 35%"><xsl:value-of select="$v02147"/></td><!--v02147 接入点引用 -->
            <td style="width: 65%" colspan="2">
                <xsl:apply-templates select="."/>
            </td>
        </tr>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="xref|internalRef" priority="1">
        <tr>
            <td/>
            <td>
                <xsl:call-template name="t_xref"/>
            </td>
        </tr>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template name="t_tir_header">
        <xsl:param name="header"/>
        <xsl:param name="id"/>
        <thead class="dr-pnl-h rich-panel-header">
            <tr>
                <th colspan="2" style="width: 100%">
                    <xsl:value-of select="$header"/>
                </th>
                <th style="width: 16px">
                    <img src="./images/chevron_up.gif"
                        onclick="collapse_expand('{$context}{$id}{position()}{count(parent::*/preceding-sibling::*)}'); swizzle_chevron(this)"
                        style="cursor: pointer"/>
                </th>
            </tr>
        </thead>
    </xsl:template>
    <!--========================================================================================-->
    <!-- Functional item output                                                                 -->
    <!--========================================================================================-->
    <xsl:template match="eininfo|functionalItemSpec">
        <xsl:apply-templates select="einalt|functionalItemAlt"/>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="einalt|functionalItemAlt">
        <table style="width: 100%; padding-top: 5px; padding-below: 5px">
            <xsl:call-template name="t_tir_header">
                <xsl:with-param name="header"><xsl:value-of select="$v02164"/></xsl:with-param>
                <xsl:with-param name="id" >ein</xsl:with-param>
            </xsl:call-template>
            <tbody id="{$context}ein{position()}{count(parent::*/preceding-sibling::*)}"
                style="display: block">
                <xsl:apply-templates
                    select="nomen|name|self::einalt[not(nomen)]/parent::*/nomen|self::functionalItemAlt[not(name)]/parent::*/name"/>
                <xsl:call-template name="t_func_item_ident"/>
                <xsl:apply-templates select="applic" mode="tir"/>
                <xsl:apply-templates select="sns|self::einalt[not(sns)]/parent::*/sns"/>
                <xsl:apply-templates
                    select="*[not(self::nomen|self::name|self::sns|self::applic|self::einref|self::functionalItemRefGroup)]"/>
                <xsl:call-template name="t_func_items_refs"/>
                <xsl:call-template name="t_func_phys_area_refs"/>
            </tbody>
        </table>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="nature">
        <xsl:call-template name="t_generic_row_presentation">
            <xsl:with-param name="title">Nature</xsl:with-param>
            <xsl:with-param name="value">
                <xsl:apply-templates/>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template name="t_func_item_ident">
        <xsl:call-template name="t_generic_row_presentation">
            <xsl:with-param name="title"><xsl:value-of select="$v02477"/></xsl:with-param>
            <xsl:with-param name="value">
                <xsl:apply-templates
                    select="parent::*/einid/@einnbr|parent::*/functionalItemIdent/@functionalItemNumber"
                />
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="qty|reqQuantity">
        <xsl:call-template name="t_generic_row_presentation">
            <xsl:with-param name="title"><xsl:value-of select="$v00744"/></xsl:with-param>
            <xsl:with-param name="value">
                <xsl:apply-templates/>
                <xsl:call-template name="t_UOM">
                    <xsl:with-param name="UOMCode" select="@uom|@unitOfMeasure"/>
                </xsl:call-template>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="location|accessFrom">
        <tr>
            <td style="width: 35%"><xsl:value-of select="$v02146"/></td>
            <td style="width: 65%" colspan="2"/>
        </tr>&#160;
        <xsl:apply-templates mode="tir"/>
    </xsl:template>
    <!--========================================================================================-->
    <!-- Display the child functional item refs if present, if not, display those defined in the-->
    <!-- parent                                                                                 -->
    <!--========================================================================================-->
    <xsl:template name="t_func_items_refs">
        <xsl:if
            test="einref|parent::*/einref|functionalItemRefGroup|parent::*/functionalItemRefGroup">
            <xsl:apply-templates
                select="einref|functionalItemRefGroup|self::einalt[not(child::einref)]/parent::*/einref|self::functionalItemAlt[not(child::functionalItemRefGroup)]/parent::*/functionalItemRefGroup"
            />
        </xsl:if>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="einref|functionalItemRefGroup">
        <tr>
            <td><xsl:value-of select="$v02163"/></td>
            <td style="width: 65%" colspan="2">
                <xsl:apply-templates select="(ein|functionalItemRef)[1]"/>
            </td>
        </tr>
        <xsl:apply-templates select="(ein|functionalItemRef)[position() &gt; 1]"
            mode="ref-table"/>
    </xsl:template>
    <!--========================================================================================-->
    <!-- Display the child physical area refs if present, if not, display those defined in the  -->
    <!-- parent                                                                                 -->
    <!--========================================================================================-->
    <xsl:template name="t_func_phys_area_refs">
        <xsl:if test="parent::*/functionalPhysicalAreaRef|functionalPhysicalAreaRef">
            <tr>
                <td><xsl:value-of select="$v02556"/></td>
                <td style="width: 65%" colspan="2">
                    <xsl:apply-templates
                        select="(functionalPhysicalAreaRef|self::functionalItemAlt[not(functionalPhysicalAreaRef)]/parent::*/functionalPhysicalAreaRef)[1]"
                    />
                </td>
            </tr>
            <xsl:apply-templates
                select="(functionalPhysicalAreaRef|self::functionalItemAlt[not(functionalPhysicalAreaRef)]/parent::*/functionalPhysicalAreaRef)[position() &gt; 1]"
                mode="ref-table"/>
        </xsl:if>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="ein|functionalItemRef|functionalPhysicalAreaRef" mode="ref-table">
        <tr>
            <td style="width: 35%"/>
            <td style="width: 65%" colspan="2">
                <xsl:apply-templates select="."/>
            </td>
        </tr>
    </xsl:template>

    <!--========================================================================================-->
    <!-- Circuit breaker item output                                                            -->
    <!--========================================================================================-->
    <xsl:template match="cbinfo|circuitBreakerSpec">
        <xsl:apply-templates select="cbalt|circuitBreakerAlt"/>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="cbalt|circuitBreakerAlt">
        <table style="width: 100%">
            <xsl:call-template name="t_tir_header">
                <xsl:with-param name="id">cb</xsl:with-param>
                <xsl:with-param name="header"><xsl:value-of select="$v02157"/></xsl:with-param>
            </xsl:call-template>
            <tbody id="{$context}cb{position()}{count(parent::*/preceding-sibling::*)}" style="display: block">
                <xsl:apply-templates
                    select="nomen|name|self::cbalt[not(nomen)]/parent::*/nomen|self::circuitBreakerAlt[not(name)]/parent::*/name"/>
                <xsl:apply-templates select="applic" mode="tir"/>
                <xsl:apply-templates
                    select="*[not(self::applic|self::name|self::einref|self::functionalItemRef)]"/>
                <xsl:call-template name="t_func_items_refs"/>
                <xsl:call-template name="t_func_phys_area_refs"/>
            </tbody>
        </table>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template name="t_cb_ident">
        <xsl:call-template name="t_generic_row_presentation">
            <xsl:with-param name="title">Circuit breaker identification</xsl:with-param>
            <xsl:with-param name="value">
                <xsl:apply-templates
                    select="parent::*/cbnid/@cbnbr|parent::*/circuitBreakerIdent/@circuitBreakerNumber"
                />
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="cbinfo//location|circuitBreakerSpec//location">
        <tr>
            <td style="width: 35%; font-weight: bold"><xsl:value-of select="$v02492"/></td>
        </tr>
        <xsl:apply-templates mode="location"/>
    </xsl:template>

    <!--========================================================================================-->
    <!-- Access point repository output                                                         -->
    <!--========================================================================================-->

    <xsl:template match="accpnlinfo|accessPointSpec">
        <xsl:apply-templates select="accpnlalt|accessPointAlt"/>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="accpnlalt|accessPointAlt">
        <table style="width: 100%">
            <xsl:call-template name="t_tir_header">
                <xsl:with-param name="header"><xsl:value-of select="$v02217"/></xsl:with-param>
                <xsl:with-param name="id">apt</xsl:with-param>
            </xsl:call-template>
            <tbody id="{$context}apt{position()}{count(parent::*/preceding-sibling::*)}"
                style="display: block">
                <xsl:apply-templates select="parent::*/accpnlid|parent::*/accessPointIdent"/>
                <xsl:apply-templates select="applic" mode="tir"/>
                <xsl:apply-templates select="*[not(self::applic)]"/>
            </tbody>
        </table>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="accpnlinfo/accpnlid|accessPointSpec/accessPointIdent">
        <xsl:call-template name="t_generic_row_presentation">
            <xsl:with-param name="title">Access Point Number</xsl:with-param>
            <xsl:with-param name="value">
                <xsl:value-of select="@accessPointNumber"/>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="accpnlinfo//zone|accessPointSpec//zoneRef">
        <tr>
            <td style="width: 35%"><xsl:value-of select="$v02191"/></td>
            <td style="width: 65%" colspan="2">
                <xsl:if test="@zoneNumber">
                    <xsl:call-template name="t_zone_ref"/>
                </xsl:if>
            </td>
        </tr>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="accessto|accessTo">
        <tr>
            <td style="width: 35%"><xsl:value-of select="$v02367"/></td>
            <td style="width: 65%" colspan="2">
                <xsl:apply-templates/>
            </td>
        </tr>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="accpnlref|accessPointRefGroup">
        <xsl:call-template name="t_generic_row_presentation">
            <xsl:with-param name="title"><xsl:value-of select="$v02147"/></xsl:with-param>
            <xsl:with-param name="value"/>
        </xsl:call-template>
        <xsl:apply-templates mode="tir"/>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="fastener">
        <xsl:call-template name="t_generic_row_presentation">
            <xsl:with-param name="title"><xsl:value-of select="$v02162"/></xsl:with-param>
            <xsl:with-param name="value">
                <xsl:apply-templates/>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="openhour|hoursToOpen">
        <xsl:call-template name="t_generic_row_presentation">
            <xsl:with-param name="title"><xsl:value-of select="$v02166"/></xsl:with-param>
            <xsl:with-param name="value">
                <xsl:apply-templates/>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>

    <!--========================================================================================-->
    <!-- Part repository output                                                         		-->
    <!--========================================================================================-->

    <xsl:template match="partinfo|partSpec">
        <table>
            <xsl:call-template name="t_tir_header">
                <xsl:with-param name="header"><xsl:value-of select="$v02496"/></xsl:with-param>
                <xsl:with-param name="id">part</xsl:with-param>
            </xsl:call-template>
            <tbody id="{$context}part{position()}{count(parent::*/preceding-sibling::*)}"
                style="display: block">
                <xsl:apply-templates/>
            </tbody>
        </table>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="partid|partIdent">
        <xsl:call-template name="t_generic_row_presentation">
            <xsl:with-param name="title"><xsl:value-of select="$v02497"/></xsl:with-param>
            <xsl:with-param name="value"><xsl:value-of select="$v00534"/>: <xsl:value-of select="@mfc|@manufacturerCodeValue"/>,
                <xsl:value-of select="$v00645"/>: <xsl:value-of select="@pnr|@partNumberValue"/></xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="overLengthPartNumber">
        <xsl:call-template name="t_generic_row_presentation">
            <xsl:with-param name="title"><xsl:value-of select="$v02242"/></xsl:with-param>
            <xsl:with-param name="value"><xsl:apply-templates/></xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="stockNumber">
        <xsl:call-template name="t_generic_row_presentation">
            <xsl:with-param name="title"><xsl:value-of select="$v02501"/></xsl:with-param>
            <xsl:with-param name="value"><xsl:apply-templates/></xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="natoStockNumber">
        <xsl:call-template name="t_generic_row_presentation">
            <xsl:with-param name="title"><xsl:value-of select="$v00555"/></xsl:with-param>
            <xsl:with-param name="value"><xsl:apply-templates/></xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="sparePartClass">
        <xsl:call-template name="t_generic_row_presentation">
            <xsl:with-param name="title"><xsl:value-of select="$v02507"/></xsl:with-param>
            <xsl:with-param name="value">
                <xsl:apply-templates select="@sparePartClassCode"/>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="@sparePartClassCode[.='0']">non-procurable part</xsl:template>
    <xsl:template match="@sparePartClassCode[.='1']">expandable part</xsl:template>
    <xsl:template match="@sparePartClassCode[.='2']">rotable part</xsl:template>
    <xsl:template match="@sparePartClassCode[.='6']">repairable part</xsl:template>
    <!--========================================================================================-->
    <xsl:template match="spec|specDocument">
        <xsl:call-template name="t_generic_row_presentation">
            <xsl:with-param name="title"><xsl:value-of select="$v02512"/></xsl:with-param>
            <xsl:with-param name="value">
                <xsl:apply-templates select="@specDocumentNumber|@specDocumentType"/>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="psc|physicalSecurityPilferageCode">
        <xsl:call-template name="t_generic_row_presentation">
            <xsl:with-param name="title"><xsl:value-of select="$v02513"/></xsl:with-param>
            <xsl:with-param name="value">
                <xsl:apply-templates/>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="ftc|fitmentCode">
        <xsl:call-template name="t_generic_row_presentation">
            <xsl:with-param name="title"><xsl:value-of select="$v02515"/></xsl:with-param>
            <xsl:with-param name="value">
                <xsl:apply-templates/>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="str|specialStorage">
        <xsl:call-template name="t_generic_row_presentation">
            <xsl:with-param name="title"><xsl:value-of select="$v02516"/></xsl:with-param>
            <xsl:with-param name="value">
                <xsl:apply-templates/>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="cmk|calibrationMarker">
        <xsl:call-template name="t_generic_row_presentation">
            <xsl:with-param name="title">osc marker</xsl:with-param>
            <xsl:with-param name="value">
                <xsl:apply-templates/>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="partref|partRefGroup">
        <tr>
            <td style="width: 35%; font-weight: bold;">Relationships with other parts</td>
            <td style="width: 65%" colspan="2"/>
        </tr>
        <xsl:apply-templates/>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="apn|altPart">
        <tr>
            <td style="width: 35%; font-weight: bold;"><xsl:value-of select="$v02518"/></td>
            <td style="width: 65%" colspan="2">
                <xsl:for-each select="part|partRef">
                    <xsl:apply-templates select="."/>&#160;<xsl:apply-templates
                        select="following-sibling::apntext|following-sibling::altPartDescr"/>
                </xsl:for-each>
            </td>
        </tr>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="localFabricationMaterial">
        <tr>
            <td style="width: 35%;"><xsl:value-of select="$v02519"/></td>
            <td style="width: 65%" colspan="2">
                <xsl:for-each select="partRef">
                    <xsl:apply-templates select="."/>&#160;<xsl:apply-templates
                        select="following-sibling::quantity"/>
                </xsl:for-each>
            </td>
        </tr>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="optionalPart"> </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="localFabricationMaterial//quantity"> (<xsl:apply-templates/>) </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="refs[not(ancestor::dmodule)]">
        <tr>
            <td style="width: 35%"><xsl:value-of select="$v00781"/></td>
            <td style="width: 65%" colspan="2">
                <xsl:apply-templates/>
            </td>
        </tr>
    </xsl:template>

    <!--========================================================================================-->
    <!-- Zone repository output                                                                 -->
    <!--========================================================================================-->

    <xsl:template match="zoneinfo|zoneSpec">
        <xsl:apply-templates/>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="zonealt|zoneAlt">
        <table style="width: 100%; padding-top: 5px; padding-below: 5px">
            <xsl:call-template name="t_tir_header">
                <xsl:with-param name="id">zone</xsl:with-param>
                <xsl:with-param name="header"><xsl:value-of select="$v02192"/></xsl:with-param>
            </xsl:call-template>
            <tbody id="{$context}zone{position()}{count(parent::*/preceding-sibling::*)}" style="display: block">
                <xsl:apply-templates/>
            </tbody>
        </table>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="side|zoneSide">
        <xsl:call-template name="t_generic_row_presentation">
            <xsl:with-param name="title"><xsl:value-of select="$v02525"/></xsl:with-param>
            <xsl:with-param name="value">
                <xsl:apply-templates select="@hand|@zoneSideValue"/>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="zoneRefGroup">
        <xsl:apply-templates mode="zone-refs"/>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="zoneRef" mode="zone-refs">
        <xsl:call-template name="t_applied_self_generic_row_presentation">
            <xsl:with-param name="title"><xsl:value-of select="$v02191"/></xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="@hand[.='lh']|@zoneSideValue[.='lh']"><xsl:value-of select="$v02526"/></xsl:template>
    <!--========================================================================================-->
    <xsl:template match="@hand[.='rh']|@zoneSideValue[.='rh']"><xsl:value-of select="$v02527"/></xsl:template>
    <!--========================================================================================-->
    <xsl:template match="@hand[.='lr']|@zoneSideValue[.='lr']"><xsl:value-of select="$v02528"/></xsl:template>
    <!--========================================================================================-->
    <xsl:template match="bndfrom|bndto|boundaryFrom|boundaryTo">
        <tr>
            <td style="width: 35%"><xsl:value-of select="$v02529"/></td>
            <xsl:apply-templates/>
        </tr>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="bndfrom/boundary|boundaryFrom/boundary">
        <td style="width: 65%" colspan="2"><xsl:value-of select="$v02530"/>&#160;<xsl:apply-templates/></td>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="bndto/boundary|boundaryTo/boundary">
        <td style="width: 65%" colspan="2"><xsl:value-of select="$v02531"/>&#160;<xsl:apply-templates/></td>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="zoneSpec//quantity">
        <xsl:apply-templates/>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="organizationinfo|enterpriseSpec">
        <table style="width: 100%; padding-top: 5px; padding-below: 5px">
            <xsl:call-template name="t_tir_header">
                <xsl:with-param name="id">org</xsl:with-param>
                <xsl:with-param name="header"><xsl:value-of select="$v02534"/></xsl:with-param>
            </xsl:call-template>
            <tbody id="{$context}org{position()}{count(parent::*/preceding-sibling::*)}" style="display: block">
                <xsl:apply-templates/>
            </tbody>
        </table> 
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="organizationid|enterpriseIdent">
        <xsl:call-template name="t_generic_row_presentation">
            <xsl:with-param name="title"><xsl:value-of select="$v00534"/></xsl:with-param>
            <xsl:with-param name="value">
                <xsl:value-of select="@mfc|@manufacturerCodeValue"/>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="ent-name|enterpriseName">
        <xsl:call-template name="t_generic_row_presentation">
            <xsl:with-param name="title"><xsl:value-of select="$v02220"/></xsl:with-param>
            <xsl:with-param name="value">
                <xsl:apply-templates/>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="ent-unit|businessUnitName">
        <xsl:call-template name="t_generic_row_presentation">
            <xsl:with-param name="title"><xsl:value-of select="$v02535"/></xsl:with-param>
            <xsl:with-param name="value">
                <xsl:apply-templates/>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="city">
        <xsl:call-template name="t_generic_row_presentation">
            <xsl:with-param name="title">City</xsl:with-param>
            <xsl:with-param name="value">
                <xsl:apply-templates/>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="country">
        <xsl:call-template name="t_generic_row_presentation">
            <xsl:with-param name="title">Country</xsl:with-param>
            <xsl:with-param name="value">
                <xsl:apply-templates/>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    
    <!--========================================================================================-->
    <!--
    <xsl:template match="zoneSpec//internalRef">
        <tr>
            <td/>
            <td>
                <span style="color: blue; text-decoration:underline; cursor: pointer;"
                    onclick="openTIRFigure('{@internalRefId}');">This is a test</span>
            </td>
        </tr>
    </xsl:template>
    -->

    <!--========================================================================================-->
    <!-- Supply requirement repository output                                                   -->
    <!--========================================================================================-->
    <xsl:template match="conitemalt|supplyRqmtAlt">
        <table style="width: 100%; padding-top: 5px; padding-below: 5px">
            <xsl:call-template name="t_tir_header">
                <xsl:with-param name="id">supreq</xsl:with-param>
                <xsl:with-param name="header"><xsl:value-of select="$v02544"/></xsl:with-param>
            </xsl:call-template>
            <tbody id="{$context}supreq{position()}{count(parent::*/preceding-sibling::*)}"
                style="display: block">
                <xsl:apply-templates select="nomen|name"/>
                <xsl:apply-templates select="applic" mode="tir"/>
                <xsl:apply-templates
                    select="*[not(self::nomen|self::name|self::applic|self::applic)]"/>
            </tbody>
        </table>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="consupplygrp|supplySetGroup">
        <tr>
            <td style="width: 35%; font-weight: bold"><xsl:value-of select="$v02546"/></td>
            <td/>
            <td style="width: 65%" colspan="2">
            </td>
        </tr>
        <xsl:apply-templates mode="supplyGroup"/>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="consupplyset|supplySet" mode="supplyGroup">
        <tr>
            <td style="width: 35%">
                <p style="padding-left: 10px"><xsl:value-of select="$v02547"/></p>
            </td>
        </tr>
        <xsl:apply-templates mode="supplyGroup"/>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="consupply|supplyRef" mode="supplyGroup">
        <tr>
            <td style="width: 35%"><xsl:value-of select="$v02548"/></td>
            <td style="width: 65%" colspan="2">
                <xsl:apply-templates select="."/>
            </td>
        </tr>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="consupplyset/consupply|supplySet/supplyRef" mode="supplyGroup">
        <tr>
            <td style="width: 35%">
                <p style="padding-left: 20px"><xsl:value-of select="$v02548"/></p>
            </td>
            <td style="width: 65%" colspan="2">
                <xsl:apply-templates select="."/>
            </td>
        </tr>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="recommendation|usage">
        <tr>
            <td style="width: 35%"><xsl:value-of select="$v02551"/></td>
        </tr>
        <tr>
            <td style="width: 65%" colspan="2">
                <xsl:apply-templates/>
            </td>
        </tr>
    </xsl:template>

    <!--========================================================================================-->
    <!-- Supply properties repository output                                                    -->
    <!--========================================================================================-->
    <xsl:template match="supplySpec">
        <table style="width: 100%; padding-top: 5px; padding-below: 5px">
            <xsl:call-template name="t_tir_header">
                <xsl:with-param name="header"><xsl:value-of select="$v02184"/></xsl:with-param>
                <xsl:with-param name="id">sup</xsl:with-param>
            </xsl:call-template>
            <tbody id="{$context}sup{position()}{count(parent::*/preceding-sibling::*)}"
                style="display: block">
                <xsl:apply-templates/>
            </tbody>
        </table>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="supplierGroup">
        <tr>
            <td style="width: 35%; font-weight: bold"><xsl:value-of select="$v02537"/></td>
            <td style="width: 65%" colspan="2"/>
        </tr>
        <xsl:apply-templates/>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="suppliedBy">
        <tr>
            <td style="width: 35%">
                <p style="padding-left: 10px"><xsl:value-of select="$v00896"/></p>
            </td>
            <td style="width: 65%" colspan="2">
                <xsl:if test="@manufacturerCodeValue"><xsl:value-of select="$v00534"/>:&#160;<xsl:value-of
                        select="@manufacturerCodeValue"/></xsl:if>
            </td>
        </tr>
        <xsl:apply-templates/>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="shippingInfo">
        <xsl:apply-templates/>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="suppliedBy//packaging">
        <tr>
            <td style="width: 35%">
                <p style="padding-left: 20px"><xsl:value-of select="$v02288"/></p>
            </td>
            <td style="width: 65%" colspan="2">
                <xsl:apply-templates/>
            </td>
        </tr>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="suppliedBy//shelfLife">
        <tr>
            <td style="width: 35%">
                <p style="padding-left: 20px"><xsl:value-of select="$v02322"/></p>
            </td>
            <td style="width: 65%" colspan="2">
                <xsl:apply-templates/>
            </td>
        </tr>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="suppliedBy//simpleRemark">
        <tr>
            <td style="width: 35%">
                <p style="padding-left: 20px"><xsl:value-of select="$v02541"/></p>
            </td>
            <td style="width: 65%" colspan="2">
                <xsl:apply-templates/>
            </td>
        </tr>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="suppliedBy//transport">
        <tr>
            <td style="width: 35%">
                <p style="padding-left: 20px"><xsl:value-of select="$v02542"/></p>
            </td>
            <td style="width: 65%" colspan="2">
                <xsl:apply-templates/>
            </td>
        </tr>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="supplySpec//quantity">
        <tr>
            <td style="width: 35%"><xsl:value-of select="$v00744"/></td>
            <td style="width: 65%" colspan="2">
                <xsl:apply-templates/>
            </td>
        </tr>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="lowestAuthorizedLevelGroup">
        <tr>
            <td style="width: 35%"><xsl:value-of select="$v02543"/></td>
            <td>
                <xsl:value-of select="lowestAuthorizedLevel/@lowestLevel"/>
            </td>
        </tr>
    </xsl:template>

    <!--========================================================================================-->
    <!-- Tool repository output                                                                 -->
    <!--========================================================================================-->

    <!-- Display all the information by cascading data from the container into each toolAlt -->
    <xsl:template match="toolinfo|toolSpec">
        <xsl:apply-templates select="toolalt|toolAlt"/>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="toolinfo|toolSpec" mode="alt">
        <xsl:apply-templates select="*[not(self::toolalt|self::toolAlt)]"/>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="toolalt|toolAlt">
        <table style="width: 100%; padding-top: 5px; padding-below: 5px">
            <xsl:call-template name="t_tir_header">
                <xsl:with-param name="header"><xsl:value-of select="$v02552"/></xsl:with-param>
                <xsl:with-param name="id">tool</xsl:with-param>
            </xsl:call-template>
            <tbody id="{$context}tool{position()}{count(parent::*/preceding-sibling::*)}"
                style="display: block">
                <xsl:apply-templates select="parent::*/toolid|parent::*/itemIdentData"/>
                <xsl:apply-templates select="applic" mode="tir"/>
                <xsl:apply-templates select="parent::*[not(self::toolid|self::itemIdentData)]"
                    mode="alt"/>
                <tr>
                    <td style="width: 35%; font-weight: bold">Alternate Tool</td>
                    <td style="width: 65%" colspan="2"/>
                </tr>
                <xsl:apply-templates select="*[not(self::applic)]"/>
            </tbody>
        </table>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="toolinfo//nomdata|toolSpec//itemIdentData">
        <tr>
            <td style="width: 35%; font-weight: bold"><xsl:value-of select="$v02554"/></td>
            <td style="width: 65%" colspan="2"/>
        </tr>
        <xsl:apply-templates/>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="kwd|partKeyword">
        <xsl:call-template name="t_generic_row_presentation">
            <xsl:with-param name="title"><xsl:value-of select="$v02228"/></xsl:with-param>
            <xsl:with-param name="value">
                <xsl:apply-templates/>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="shortName">
        <xsl:call-template name="t_generic_row_presentation">
            <xsl:with-param name="title"><xsl:value-of select="$v02499"/></xsl:with-param>
            <xsl:with-param name="value">
                <xsl:apply-templates/>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>

    <!-- 
    <xsl:template match="overLengthPartNumber">
        <tr>
            <td>Over length PNR</td>
            <td>
                <xsl:apply-templates/>
            </td>
        </tr>
    </xsl:template>
    -->

    <xsl:template match="rcmdQuantity">
        <tr>
            <td style="width: 35%"><xsl:value-of select="$v02286"/></td>
            <td style="width: 65%" colspan="2">
                <xsl:apply-templates/>
            </td>
        </tr>
    </xsl:template>

    <xsl:template match="packaging">
        <tr>
            <td style="width: 35%"><xsl:value-of select="$v02288"/></td>
            <td style="width: 65%" colspan="2">
                <xsl:apply-templates/>
            </td>
        </tr>
    </xsl:template>

    <xsl:template match="taskCategory">
        <tr>
            <td style="width: 35%"><xsl:value-of select="$v02290"/></td>
            <td style="width: 65%" colspan="2">TODO</td>
        </tr>
    </xsl:template>

    <xsl:template match="simpleRemark">
        <tr>
            <td style="width: 35%"><xsl:value-of select="$v02541"/></td>
            <td style="width: 65%" colspan="2">
                <xsl:apply-templates/>
            </td>
        </tr>
    </xsl:template>

    <!--========================================================================================-->
    <!-- Physical Functional Area repository output                                             -->
    <!--========================================================================================-->
    <xsl:template match="functionalPhysicalAreaSpec">
        <table style="width: 100%; padding-top: 5px; padding-below: 5px">
            <xsl:call-template name="t_tir_header">
                <xsl:with-param name="id">pfa</xsl:with-param>
                <xsl:with-param name="header"><xsl:value-of select="$v02555"/></xsl:with-param>
            </xsl:call-template>
            <tbody id="{$context}pfa{position()}{count(parent::*/preceding-sibling::*)}"
                style="display: block">
                <xsl:apply-templates/>
            </tbody>
        </table>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="functionalPhysicalAreaIdent">
        <tr>
            <td style="width: 35%">Area identification</td>
            <td style="width: 65%" colspan="2">
                <xsl:call-template name="t_format_functional_area_id"/>
            </td>
        </tr>
    </xsl:template>
    <!--========================================================================================-->
    <!-- Control Indicator                                                                      -->
    <!--========================================================================================-->
    <xsl:template match="controlIndicatorSpec">
        <xsl:call-template name="t_control_indicator_figures"/>
        <table style="width: 100%; padding-top: 5px; padding-below: 5px">
            <xsl:call-template name="t_tir_header">
                <xsl:with-param name="id">ci</xsl:with-param>
                <xsl:with-param name="header"><xsl:value-of select="$v02557"/></xsl:with-param>
            </xsl:call-template>
            <tbody id="{$context}ci{position()}{count(parent::*/preceding-sibling::*)}" style="display: block">
                <xsl:apply-templates select="@controlIndicatorNumber"/>
                <xsl:apply-templates/>
            </tbody>
        </table>
    </xsl:template>
    <!--========================================================================================-->
    <!-- Control indicator figures are stored within the controlIndicatorGroup node, and do     -->
    <!-- not have a mechanism for linking within the control indicator itself. This forces a    -->
    <!-- query to be performed to retrieve the figures from the repository using a further      -->
    <!-- extension function.                                                                    -->
    <!-- The extension function returns a nodeset of figures, which a handled by a figure       -->
    <!-- template with a mode of techrep.                                                       -->
    <!--========================================================================================-->
    <xsl:template name="t_control_indicator_figures">
        <!--2012<xsl:apply-templates select="tir-proc:extractControlIndicatorFigures(.)" mode="techrep"/>-->
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="@controlIndicatorNumber">
        <xsl:call-template name="t_generic_row_presentation">
            <xsl:with-param name="title">Control indicator identification</xsl:with-param>
            <xsl:with-param name="value">
                <xsl:apply-templates/>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="controlIndicatorKey">
        <xsl:call-template name="t_generic_row_presentation">
            <xsl:with-param name="title"><xsl:value-of select="$v02558"/></xsl:with-param>
            <xsl:with-param name="value">
                <xsl:apply-templates/>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="controlIndicatorName">
        <xsl:call-template name="t_generic_row_presentation">
            <xsl:with-param name="title"><xsl:value-of select="$v02559"/></xsl:with-param>
            <xsl:with-param name="value">
                <xsl:apply-templates select="*[1]"/>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="controlIndicatorDescr">
        <tr>
            <td style="width: 35%"><xsl:value-of select="$v00238"/></td>
            <td style="width: 65%" colspan="2">
                <xsl:apply-templates select="*[1]"/>
            </td>
        </tr>
        <xsl:apply-templates select="*[position() &gt; 1]"/>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="controlIndicatorFunction">
        <xsl:call-template name="t_generic_row_presentation">
            <xsl:with-param name="title">Control indicator function</xsl:with-param>
            <xsl:with-param name="value">
                <xsl:apply-templates/>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="figure" mode="techrep">
        <div class="figure" id="tir-{@id}">
            <xsl:apply-templates select="graphic" mode="techrep">
                <xsl:with-param name="figCount" select="count(preceding-sibling::figure) + 1"/>
                <xsl:with-param name="figTitle">
                    <xsl:value-of select="$v00355"/>
                    <xsl:text>&#160;</xsl:text>
                    <xsl:value-of select="count(preceding-sibling::figure) + 1"/>
                    <xsl:text>&#160;-&#160;</xsl:text>
                    <xsl:value-of select="./title"/>
                </xsl:with-param>
            </xsl:apply-templates>
            <div style="padding-top:0.8em; padding-bottom:1.1em">
                <xsl:value-of select="$v00355"/>
                <xsl:text>&#160;</xsl:text>
                <xsl:value-of select="count(preceding-sibling::figure) + 1"/>
                <xsl:text>&#160;-&#160;</xsl:text>
                <xsl:value-of select="./title"/>
            </div>
        </div>
    </xsl:template>
    <!--========================================================================================-->
    <xsl:template match="graphic" mode="techrep">
        <xsl:param name="figCount" select="1"/>
        <xsl:param name="figTitle"/>

        <img class="figureLinkGraphic"  hspace="10"
            vspace="10" name="figureGraphicIcon">
            <xsl:attribute name="alt">
                <xsl:value-of select="substring-before(@boardno|@infoEntityIdent,'.')"/>
            </xsl:attribute>
            <xsl:attribute name="id">
                <xsl:value-of select="@boardno|@infoEntityIdent"/>
            </xsl:attribute>
            <xsl:attribute name="src">images/dmodule/image.gif</xsl:attribute>
            <xsl:attribute name="onclick">tearOffTIRFigure('<xsl:value-of
                    select="@boardno|@infoEntityIdent"/>', '<xsl:value-of select="$Publication"
            />');</xsl:attribute>
        </img>

        <xsl:variable name="figureSheetNumberText">
            <xsl:call-template name="t_sheet_number">
                <xsl:with-param name="sheetCount" select="count(preceding-sibling::graphic) + 1"/>
                <xsl:with-param name="sheetTotal" select="count(parent::figure/graphic)"/>
            </xsl:call-template>
        </xsl:variable>

        <div class="sheet">
            <xsl:value-of select="$figureSheetNumberText"/>
        </div>

    </xsl:template>
    <!--========================================================================================-->
    <xsl:template name="t_sheet_number">
        <xsl:param name="sheetCount" select="1"/>
        <xsl:param name="sheetTotal" select="1"/>
        <xsl:value-of select="$v00860"/>&#160;<xsl:value-of
            select="format-number($sheetCount, '####')"/>&#160;<xsl:value-of select="$v01111"
            />&#160;<xsl:value-of select="format-number($sheetTotal,'####')"/>
    </xsl:template>
    <!--========================================================================================-->
</xsl:stylesheet>
