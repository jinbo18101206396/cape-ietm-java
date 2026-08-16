<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <!--===================================================-->
    <xsl:template match="status/applic | dmStatus/applic">
        <!--<tr>
            <xsl:if test="parent::status/@change = 'ADD'">
                <xsl:attribute name="class">changeMarker</xsl:attribute>
            </xsl:if>
            <xsl:call-template name="t_changeMarker"/>
            <td>
                <xsl:value-of select="$v00034"/>
            </td>
            <td>
                <xsl:apply-templates mode="applic"/>
            </td>
        </tr>-->
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="inlineapplics|referencedApplicGroup">
        <tr>
            <xsl:if test="parent::status/@change = 'ADD'">
                <xsl:attribute name="class">changeMarker</xsl:attribute>
            </xsl:if>
            <xsl:call-template name="t_changeMarker"/>
            <td>
                <xsl:value-of select="$v00428"/>
            </td>
            <td>
                <xsl:apply-templates/>
            </td>
        </tr>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="inlineapplics/applic">
        <tr>
            <td>
                <span class="inlineApplic">
                    <xsl:value-of select="$v00766"/>
                </span>
            </td>
            <td>
                <xsl:value-of select="@id"/>
            </td>
        </tr>
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <!--
		Inline applicability.
		All elements that have the @refapplic attribute reference corresponding <applic> elements within the
		<idstatus> fragment by matching the @id attribute of the <applic> element. When this match is made,
		the T_Content_Effectivity template is called to complete the output, therefore inserting the referenced
		<applic> element inline.
	-->
    <xsl:template name="t_inlineApplicability">
        <xsl:param name="annotation" select="''"/>
        <xsl:param name="class">applicAnnotation</xsl:param>
        <xsl:if test="@refapplic|@applicRefId">
            <xsl:variable name="applicReference">
                <xsl:value-of select="@refapplic|@applicRefId"/>
            </xsl:variable>
            <xsl:for-each select="//applic[@id = $applicReference]">
                <div>
                    <xsl:call-template name="t_changeMarker"/>
                    <div>
                    	<xsl:attribute name="class"><xsl:value-of select="$class"/></xsl:attribute>
                        <xsl:value-of select="$annotation"/><xsl:value-of select="displaytext|displayText"/>
                    </div>
                </div>
            </xsl:for-each>
        </xsl:if>
    </xsl:template>

    <!--===============================================-->
    <!-- Template re-added to deal with none referenced pre issue 4 inline applics only -->
    <xsl:template match="displaytext" mode="applic">
        <div>
            <xsl:call-template name="t_changeMarker"/>
            <div class="applicAnnotation">
                <xsl:value-of select="$v00034"/>
                <xsl:text>:&#160;</xsl:text>
                <xsl:value-of select="text()"/>
            </div>
        </div>
    </xsl:template>

    <!--===============================================-->
    <xsl:template
        match="content//applic[not(child::evaluate)]|processFragment//applic[not(child::evaluate)]|stepview//applic[not(child::evaluate)]">
        <xsl:apply-templates mode="applic"/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template
        match="content//applic[child::evaluate]|processFragment//applic[child::evaluate]|stepview//applic[child::evaluate]">
        <xsl:apply-templates mode="applic"/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="status//applic/displaytext|dmStatus//applic/displayText" mode="applic">
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="type">
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="type" mode="applic">
        <xsl:value-of select="$v00985"/>:&#160;<xsl:apply-templates mode="applic"
            /><xsl:call-template name="T_InsertColon"/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="model">
        <tr>
            <td>
                <span class="inlineApplic">
                    <xsl:value-of select="$v00542"/>
                </span>
            </td>
            <td>
                <xsl:value-of select="@model"/>
            </td>
        </tr>
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="model" mode="applic">
        <xsl:value-of select="@model"/>
        <xsl:if test="version/@version">,</xsl:if>
        <xsl:apply-templates mode="applic"/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="version">
        <tr>
            <td>
                <span class="inlineApplic">
                    <xsl:value-of select="$v01047"/>
                </span>
            </td>
            <td>
                <xsl:value-of select="@version"/>
            </td>
        </tr>
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="version" mode="applic">
        <xsl:value-of select="@version"/>
        <xsl:apply-templates mode="applic"/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="versrank">
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="versrank" mode="applic">
        <xsl:value-of select="$v00753"/>:&#160;<xsl:apply-templates mode="applic"/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="csnref">
        <xsl:element name="a">
            <xsl:call-template name="t_changeMarker"/>
            <!-- call to javascript that calls a servlet to search the db for the csn -->
            <xsl:attribute name="href">javascript:locateCSN('<xsl:value-of
                    select="normalize-space(@refcsn)"/>','<xsl:value-of select="$Publication"/>')</xsl:attribute>
            <xsl:attribute name="onclick">Richfaces.showModalPanel('busyPanel');</xsl:attribute>
            <xsl:attribute name="title">
                <xsl:value-of select="$v00771"/>&#160;<xsl:value-of select="@refcsn"/>
            </xsl:attribute>
            <xsl:value-of select="$v00190"/>: <xsl:call-template name="T_csnref">
                <xsl:with-param name="CSN" select="@refcsn"/>
                <xsl:with-param name="ISNREF" select="@refisn"/>
                <!-- We currently don't do anything with the refisn here or printservices -->
            </xsl:call-template>
        </xsl:element>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="catalogSeqNumberRef">
        <xsl:element name="a">
            <xsl:call-template name="t_changeMarker"/>
            <!-- call to javascript that calls a servlet to search the db for the csn -->
            <xsl:attribute name="href">javascript:locateCSN('<xsl:value-of
                    select="normalize-space(@catalogSeqNumberValue)"/>','<xsl:value-of
                    select="$Publication"/>')</xsl:attribute>
            <xsl:attribute name="onclick">Richfaces.showModalPanel('busyPanel');</xsl:attribute>
            <xsl:attribute name="title">
                <xsl:value-of select="$v00771"/>&#160;<xsl:value-of
                    select=" @catalogSeqNumberValue"/>
            </xsl:attribute>
            <xsl:value-of select="$v00190"/>: <xsl:call-template name="T_csnref_iss4">
                <xsl:with-param name="CSN" select="@catalogSeqNumberValue"/>
                <xsl:with-param name="ISNREF" select="@itemSeqNumberValue"/>
                <!-- We currently don't do anything with the refisn here or printservices -->
            </xsl:call-template>
        </xsl:element>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="batchno">
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="batchno" mode="applic">
        <xsl:value-of select="$v00535"/>:&#160;<xsl:apply-templates mode="applic"/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="single">
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="single" mode="applic">
        <xsl:apply-templates/>
        <xsl:call-template name="T_InsertColon"/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="range">
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="range" mode="applic">
        <xsl:value-of select="@from"/>-<xsl:value-of select="@to"/><xsl:call-template
            name="T_InsertColon"/><xsl:apply-templates mode="applic"/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="moduleno">
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="serialno">
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="serialno" mode="applic">
        <xsl:value-of select="$v00537"/>:&#160;<xsl:apply-templates/><xsl:call-template
            name="T_InsertColon"/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="pnr">
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="customer">
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="softprog">
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="level">
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="config">
        <div>
            <xsl:call-template name="t_changeMarker"/>
            <xsl:apply-templates/>
        </div>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="sb">
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="mfc">
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="nsn">
        <xsl:if test="@nsn">
            <span>
                <xsl:call-template name="t_changeMarker"/> &#160; <xsl:value-of
                    select="substring(@nsn,1,4)"/>-<xsl:value-of select="substring(@nsn,5,2)"
                    />-<xsl:value-of select="substring(@nsn,7,3)"/>-<xsl:value-of
                    select="substring(@nsn,10,4)"/>
            </span>
        </xsl:if>
    </xsl:template>
    <!--===============================================-->
    <xsl:template name="T_csnref">
        <!-- gets csn, strips out any spaces and puts them back correctly-->
        <!-- we get passed the nsn in a param from whatever context to reduce code-->
        <!-- the csn can be either 8, 12 or 13 characters long, all are broken up the same with the exception of the last part which can
				be either 3 or 4 characters long, the 9th character may be a space or not-->
        <xsl:param name="CSN"/>
        <xsl:variable name="strlength">
            <xsl:value-of select="string-length($CSN)"/>
        </xsl:variable>
        <xsl:variable name="csnref">
            <xsl:value-of select="substring($CSN,1,2)"/>-<xsl:value-of select="substring($CSN,3,2)"
                />-<xsl:value-of select="substring($CSN,5,2)"/>-<xsl:value-of
                select="translate(substring($CSN,7,3),' ','')"/>
            <xsl:if test="string-length($CSN) &gt; 11">&#160;<xsl:value-of
                    select="translate(substring($CSN,10,$strlength),' ', '')"/></xsl:if>
        </xsl:variable>
        <xsl:if test="not($csnref = '----')">
            <xsl:value-of select="$csnref"/>
        </xsl:if>
    </xsl:template>
    <!--===============================================-->
    <xsl:template name="T_csnref_iss4">
        <!-- As above, but works with catalogSeqNumberRef elements -->
        <xsl:param name="CSN"/>
        <xsl:variable name="strlength">
            <xsl:value-of select="string-length($CSN)"/>
        </xsl:variable>
        <xsl:variable name="csnref">
            <xsl:value-of select="substring($CSN,1,3)"/>-<xsl:value-of select="substring($CSN,4,2)"
                />-<xsl:value-of select="substring($CSN,6,4)"/>-<xsl:value-of
                select="translate(substring($CSN,10,3),' ','')"/><xsl:if
                test="string-length($CSN) &gt; 12">&#160;<xsl:value-of
                    select="translate(substring($CSN,13,$strlength),' ', '')"/></xsl:if>
        </xsl:variable>
        <xsl:if test="not($csnref = '----')">
            <xsl:value-of select="$csnref"/>
        </xsl:if>
    </xsl:template>
    <!--===============================================-->
    <xsl:template name="T_nsn">
        <!-- gets nsn, strips out any dashes and puts them back correctly-->
        <!-- we get passed the nsn in a param from whatever context to reduce code-->
        <xsl:param name="NSN_Temp"/>
        <xsl:variable name="NatoStockNumberFull">
            <xsl:value-of select="translate($NSN_Temp,'-' ,'')"/>
        </xsl:variable>
        <xsl:variable name="NatoStockNumber">
            <xsl:value-of select="substring($NatoStockNumberFull,1,4)"/>-<xsl:value-of
                select="substring($NatoStockNumberFull,5,2)"/>-<xsl:value-of
                select="substring($NatoStockNumberFull,7,3)"/>-<xsl:value-of
                select="substring($NatoStockNumberFull,10,4)"/>
        </xsl:variable>
        <xsl:if test="not($NatoStockNumber = '---')">
            <xsl:value-of select="$NatoStockNumber"/>
        </xsl:if>
    </xsl:template>
    <!--===============================================-->
    <xsl:template name="T_InsertColon">
        <xsl:if
            test="(following-sibling::*) or (ancestor::version and ancestor::applic/model/batchno) or ((ancestor::version or parent::batchno) and ancestor::applic/model/serialno)"
            >; </xsl:if>
    </xsl:template>
    <!--===============================================-->
    <!-- This replaces the pipe('|') with the string ' or ' for issue 3.0 -->
    <xsl:template name="replace-pipe">
        <xsl:param name="text"/>
        <xsl:choose>
            <xsl:when test="contains($text,'|')">
                <xsl:value-of select="substring-before($text,'|')"/>
                <xsl:value-of select="' or '"/>
                <xsl:call-template name="replace-pipe">
                    <xsl:with-param name="text" select="substring-after($text,'|')"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$text"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <!--===============================================-->
</xsl:stylesheet>
