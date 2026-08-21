<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<!--===============================================-->
	<xsl:strip-space elements="*"/>
	<!--===============================================-->
	<xsl:template match="para[not(parent::listItem and position() = 1)] | copyrightPara | simplePara | attentionListItemPara[position() &gt; 1] | notePara">
		<p class="subparaX">
			<xsl:call-template name="t_inlineApplicability"/>
			<xsl:call-template name="t_changeMarker"/>
			<xsl:apply-templates/>
		</p>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="para0/para | subpara1/para | subpara2/para | subpara3/para | subpara4/para | subpara5/para | subpara6/para | subpara7/para | step/para | drill/para" name="T_NormalSubparaPara">
		<!--  used by both aircrew and descriptive -->
		<p class="subparaX">
			<xsl:call-template name="t_inlineApplicability">
            	<xsl:with-param name="annotation"><xsl:value-of select="$v00034"/>:&#160;</xsl:with-param>
        	</xsl:call-template>
			<xsl:call-template name="t_changeMarker"/>
			<xsl:apply-templates/>
		</p>				
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="para0[not(title)]/para[1][not(preceding-sibling::*)] | subpara1[not(title)]/para[1][not(preceding-sibling::*)] | subpara2[not(title)]/para[1][not(preceding-sibling::*)] | subpara3[not(title)]/para[1][not(preceding-sibling::*)] | subpara4[not(title)]/para[1][not(preceding-sibling::*)]">
	<!--when the para0 /subpara has no title  -->
	<!-- the idea here is to keep the formatting of the title's number but have a standard format for the para text-->
		<p class="subparaX">
			<xsl:call-template name="t_inlineApplicability">
            	<xsl:with-param name="annotation"><xsl:value-of select="$v00034"/>:&#160;</xsl:with-param>
        	</xsl:call-template>
			<xsl:call-template name="t_changeMarker"/>
			<xsl:attribute name="class"><xsl:call-template name="T_FormatDescriptiveTitleBlock"/></xsl:attribute>
			<xsl:value-of select="../@count"/>.		
			<span class="subparaXNoTitlePara">
			<xsl:apply-templates/>
			</span>
		</p>	
	</xsl:template>
	<!--===============================================-->
</xsl:stylesheet>