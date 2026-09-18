<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:xlink="http://www.w3.org/1999/xlink"
	xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
	xmlns:dc="http://www.purl.org/dc/elements/1.1/">
<!-- ===================================================================== -->
	<xsl:template match="supscrpt | superScript">  
	<span class="superScript">	
	<xsl:apply-templates/>
		</span>
	</xsl:template>
	<!-- ===================================================================== -->
	<xsl:template match="subscrpt | subScript">
		<span class="subScript">
		<xsl:apply-templates/>
		</span>
	</xsl:template>
	<!-- ===================================================================== -->
  <xsl:template match="symbol" priority="2">
    <xsl:call-template name="t_inlineApplicability">
      <xsl:with-param name="annotation">
        <xsl:value-of select="$v00034"/>:&#160;
      </xsl:with-param>
    </xsl:call-template>
    <xsl:variable name="boardnum">
      <xsl:value-of select="translate(@boardno|@infoEntityIdent,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')"/>
    </xsl:variable>

    <!-- 🔧 2026-09-18修复：处理S1000D 4.0尺寸属性 (§3.9.5.2.16) -->
    <xsl:variable name="wid">
      <xsl:choose>
        <xsl:when test="@reproductionWidth">
          <xsl:value-of select="@reproductionWidth"/>
        </xsl:when>
        <xsl:when test="@reproductionScale">
          <xsl:value-of select="concat(@reproductionScale, '%')"/>
        </xsl:when>
        <xsl:otherwise>auto</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="hght">
      <xsl:choose>
        <xsl:when test="@reproductionHeight">
          <xsl:value-of select="@reproductionHeight"/>
        </xsl:when>
        <xsl:otherwise>auto</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <!--hspace="60" vspace="60"-->
    <img class="figureLinkGraphic"  name="figureGraphicIcon">
      <xsl:attribute name="alt">
        <xsl:value-of select="substring-before(@boardno|@infoEntityIdent,'.')"/>
      </xsl:attribute>
      <xsl:attribute name="id">
        <xsl:value-of select="@boardno|@infoEntityIdent"/>
      </xsl:attribute>
      <xsl:attribute name="boardno">
        <xsl:value-of select="@boardno|@infoEntityIdent"/>
      </xsl:attribute>

      <!-- 应用尺寸属性 -->
      <xsl:if test="$wid != 'auto'">
        <xsl:attribute name="width"><xsl:value-of select="$wid"/></xsl:attribute>
      </xsl:if>
      <xsl:if test="$hght != 'auto'">
        <xsl:attribute name="height"><xsl:value-of select="$hght"/></xsl:attribute>
      </xsl:if>
      <xsl:if test="$wid = 'auto' and $hght = 'auto'">
        <xsl:attribute name="style">max-width: 800px; height: auto;</xsl:attribute>
      </xsl:if>
    </img>
    <script>
        var borderno="<xsl:value-of select="@boardno|@infoEntityIdent"/>";
        var ext = borderno.substring(borderno.length-4,borderno.length).toUpperCase();
        var control=document.getElementById("<xsl:value-of select="@boardno|@infoEntityIdent"/>");
      control.src = lessonPath+"Manual/"+lessonCode+"/"+"<xsl:value-of select="@boardno|@infoEntityIdent"/>";
    </script>
  </xsl:template>
  <!-- ===================================================================== -->
	</xsl:stylesheet>