<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<!--====================注格式===========================-->
	<xsl:template match="note" name="t_note">
	<xsl:param name="acrwIndent">count(ancestor::proceduralStep)</xsl:param>
		<!-- the default definition list structure currently satisifies what we need for notes -->
		<xsl:call-template name="t_inlineApplicability">
            <xsl:with-param name="annotation"><xsl:value-of select="$v00034"/>:&#160;</xsl:with-param>
        </xsl:call-template>
        <p>
		<span class="noteBlock">
			<xsl:call-template name="t_changeMarker"/>
			<!--<xsl:attribute name="style">margin-left: <xsl:value-of select="$acrwIndent+2"/>px;</xsl:attribute>-->
       <xsl:attribute name="style">text-indent:2em;</xsl:attribute>
			<span class="noteTitle">
				<xsl:call-template name="wcnLabel">
					<xsl:with-param name="T_NodeName" select="name(.)"/>
				</xsl:call-template>
			</span>
			<!--=======注格式调整2012 注内容调整缩进二个字符=======-->
			<span class="noteContent">
				<xsl:apply-templates/>
			</span>
		</span>
		</p>
	</xsl:template>
	<!--===============================================-->	
	<xsl:template match="safecond/note | safetyRqmts/note">
		<div class="wcBorderSpacing noteBGImage">
			<div class="wcContentArea">
				<!--<xsl:call-template name="wcContentArea"/>-->
        <xsl:call-template name="wcnLabel">
          <xsl:with-param name="T_NodeName" select="name(.)"/>
        </xsl:call-template>
        &#160;<span class="noteTitle">
        <xsl:apply-templates/>
      </span>
			</div>
		</div>
	</xsl:template>
	<!--===============================================-->
</xsl:stylesheet>
