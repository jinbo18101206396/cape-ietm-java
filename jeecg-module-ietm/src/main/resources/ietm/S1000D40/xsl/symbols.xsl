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

      <!-- 🔧 2026-10-16修复：为公式ICN添加点击事件，使其可以在预览中正确加载 -->
      <xsl:attribute name="onclick"><xsl:text>showMultimediaInfo('</xsl:text><xsl:value-of select="@boardno|@infoEntityIdent"/><xsl:text>');</xsl:text></xsl:attribute>
      <xsl:attribute name="style">cursor:pointer; max-width: 800px; height: auto;</xsl:attribute>

      <!-- 应用尺寸属性（覆盖默认style） -->
      <xsl:if test="$wid != 'auto' or $hght != 'auto'">
        <xsl:attribute name="style">
          <xsl:text>cursor:pointer;</xsl:text>
          <xsl:if test="$wid != 'auto'">
            <xsl:text>width:</xsl:text><xsl:value-of select="$wid"/><xsl:text>;</xsl:text>
          </xsl:if>
          <xsl:if test="$hght != 'auto'">
            <xsl:text>height:</xsl:text><xsl:value-of select="$hght"/><xsl:text>;</xsl:text>
          </xsl:if>
        </xsl:attribute>
      </xsl:if>
    </img>
    <script>
        var borderno="<xsl:value-of select="@boardno|@infoEntityIdent"/>";
        var control=document.getElementById("<xsl:value-of select="@boardno|@infoEntityIdent"/>");

      // 🔧 2026-10-16修复：公式ICN从数据库BLOB加载，普通图符从磁盘加载
      // 根本问题：ICN编码本身不含文件扩展名，无法通过后缀判断类型
      // 解决方案：异步调用metadata接口判断icnType，动态加载公式

      // 先显示占位图，等待前端JavaScript异步判断类型
      control.setAttribute('data-icn-code', borderno);
      control.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTAwIiBoZWlnaHQ9IjUwIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPjxyZWN0IHdpZHRoPSIxMDAiIGhlaWdodD0iNTAiIGZpbGw9IiNmN2Y3ZjciIHN0cm9rZT0iI2RkZCIgc3Ryb2tlLXdpZHRoPSIxIi8+PHRleHQgeD0iNTAiIHk9IjI1IiBmb250LXNpemU9IjEyIiBmb250LWZhbWlseT0iQXJpYWwiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGZpbGw9IiM5OTkiPuWKoOi9veS4rS4uLjwvdGV4dD48L3N2Zz4=';
      control.title = 'ICN加载中...';

      // 标记为需要异步加载（前端会扫描此属性并自动加载）
      control.setAttribute('data-need-load', 'true');
    </script>
  </xsl:template>
  <!-- ===================================================================== -->
	</xsl:stylesheet>