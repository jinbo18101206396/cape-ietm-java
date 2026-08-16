<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet  version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <!--2012 xmlns:aadext="java:com.ptc.aad.xsltext.StringFunctions?path=file:///E:/IETM4/IETP/IETP_Data_Update/xsl/java/" exclude-result-prefixes="aadext"-->
    <!--===============================================-->
    <!-- we currently handle warning and cautions in the same manner-->
    <xsl:template match="warning" name="t_warning">
        <xsl:param name="acrwIndent">0</xsl:param>
        <div class="wcBorderSpacing warningBGImage">
             <!--  <xsl:call-template name="t_inlineApplicability"/>
            <xsl:attribute name="style">margin-left: <xsl:value-of select="$acrwIndent"/>px;</xsl:attribute>-->
            <div class="wcContentArea">
                <xsl:call-template name="wcContentArea"/>
            </div>
        </div>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="caution" name="t_caution">
        <xsl:param name="acrwIndent">0</xsl:param>
       <div class="wcBorderSpacing cautionBGImage">
             <!--<xsl:call-template name="t_inlineApplicability"/>
            <xsl:attribute name="style">margin-left: <xsl:value-of select="$acrwIndent"/>px;</xsl:attribute>-->
            <div class="wcContentArea">
                <xsl:call-template name="wcContentArea"/>
            </div>
        </div>
    </xsl:template>
    <!--===============================================-->
    <xsl:template name="wcContentArea">
        <!-- the wc content is made up of a title and content-->
        <xsl:call-template name="wcTitle"/>
        <xsl:call-template name="wcContent"/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template name="wcTitle">
        <div class="wcTitle">
            <xsl:call-template name="wcnLabel">
                <xsl:with-param name="T_NodeName" select="name(.)"/>
            </xsl:call-template>
        </div>
    </xsl:template>
    <!--===============================================-->
    <xsl:template name="wcContent">
      <p>
      <span class="wcContent">
            <xsl:apply-templates/>
       </span>
      </p>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="warningsAndCautions">
        <!-- suppressed -->
    </xsl:template>
  <!--===============================================-->
  <xsl:template match="caution/warningAndCautionPara |warning/warningAndCautionPara">
      <xsl:choose>
        <xsl:when test="(name(.) ='warningAndCautionPara' and  (preceding-sibling::warningAndCautionPara)) and (parent::caution or parent::warning)">
          <p>
            <div>
              <xsl:attribute name="style">
                text-indent:2em;
              </xsl:attribute>
              <xsl:call-template name="t_inlineApplicability">
                <xsl:with-param name="annotation" select="$v00034"/>
              </xsl:call-template>
              <xsl:apply-templates/>
            </div>
          </p>
        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-templates/>
        </xsl:otherwise>
      </xsl:choose>
    <!-- suppressed -->
  </xsl:template>
    <!--===============================================-->
    <!--
        Named template that dereferences warningRefs and cautionsRefs from the warningsAndCautions section of the 
        data module.
        
        Templates that may contains such a reference should call this template at the appropriate time to ensure correct 
        sequence of rendering. Warnings and cautions are rendered by applying templates on the warnings/cautions that 
        have an id that is contained within the warningRefs/cautionrefs attribute of the current element by use of the 
        containsDelimited extension function. 
        -->
    <xsl:template name="t_dereference_wcn">
        <xsl:variable name="wids">
            <xsl:value-of select="@warningRefs"/>
        </xsl:variable>
        <xsl:variable name="cids">
            <xsl:value-of select="@cautionRefs"/>
        </xsl:variable>
        <!--2012 <xsl:apply-templates
            select="//dmodule/content/warningsAndCautions/warning[aadext:containsDelimited($wids, @id,' ')]"/>
        <xsl:apply-templates
            select="//dmodule/content/warningsAndCautions/caution[aadext:containsDelimited($cids, @id, ' ')]"
        />-->
    </xsl:template>
    <!--===============================================-->
</xsl:stylesheet>
