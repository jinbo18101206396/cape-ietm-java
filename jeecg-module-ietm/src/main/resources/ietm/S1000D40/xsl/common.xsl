<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet  
    version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<!--xmlns:aadext="java:com.ptc.aad.xsltext.StringFunctions?path=file:///E:/IETM4/IETP/IETP_Data_Update/xsl/java/" exclude-result-prefixes="aadext"-->
    <!--===============================================-->
    <xsl:include href="commonTitles.xsl"/>
	<xsl:include href="dmTocUtil.xsl"/>
	<xsl:include href="multimedia.xsl"/>
    <xsl:include href="techrepRef.xsl"/>
    <xsl:include href="xlink.xsl"/>
    <xsl:include href="xref.xsl"/>
    <!--===============================================-->
    <!--
        t_changeMarker
        Template used to determine whether a change marker is necessary for a given element.
        If it is determined that the change marker is required, then an attribute is inserted specifying the use of the changeMarker class from the cascading stylesheets.
    -->
    <xsl:template name="t_changeMarker">
        <xsl:param name="existingClasses"></xsl:param>
    	<xsl:variable name="issno">
			<xsl:value-of select="//dmodule/idstatus/dmaddres/issno/@issno"/>
		</xsl:variable>
		<xsl:if test="(ancestor-or-self::*/@changeMark='1') or (ancestor-or-self::*/@mark = '1' and not(ancestor-or-self::*/@level)) or 
					((ancestor-or-self::*/@mark = '1') and (ancestor-or-self::*/@level = ($issno)-1) and ($issno >1))">
	    	<xsl:attribute name="class">
	        	<xsl:value-of select="$existingClasses"/>
	        	<xsl:text> changeMarker</xsl:text>
	    	</xsl:attribute>
		</xsl:if>
    </xsl:template>
    <!--===============================================-->
    <xsl:template name="importCSS">
        <!--  This uses the param telling us where the main.css can be found -->
        <!--  修复：移除无效的style标签输出（BUG-PREVIEW-01）-->
        <!--  说明：CSS样式由后端Java代码统一注入（DmXsltTransformer.enhancePreviewHtml），-->
        <!--       XSLT不应该输出CSS路径字符串到style标签中 -->
        <xsl:choose>
            <xsl:when test="$Publication = $cssDir">
                <xsl:element name="link">
                    <xsl:attribute name="href"><xsl:value-of select="$cssDir"/>.css</xsl:attribute>
                    <xsl:attribute name="rel">stylesheet</xsl:attribute>
                    <xsl:attribute name="type">text/css</xsl:attribute>
                </xsl:element>
            </xsl:when>
            <xsl:otherwise>
                <!-- 移除了错误的style标签输出：<xsl:value-of select="$cssDir"/> -->
                <!-- CSS由Java后端统一处理，此处不需要输出任何内容 -->
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="acronym">
    <!-- The definition of the term now appears as a tool tip of the acronym. -->
    	<span>
    	    <xsl:attribute name="title"><xsl:apply-templates select="acrodef"/></xsl:attribute>
    	    <xsl:apply-templates select="acroterm"/>
    	</span>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="indxflag">
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="change">
        <span>
            <xsl:call-template name="t_changeMarker"/>
            <xsl:apply-templates/>
        </span>
    </xsl:template>
    <!--===============================================-->
    <xsl:template
        match="emphasis[not(@emph)] |emphasis[@emph='em01'] | emphasis[@emph=''] | emphasis[@emph='bold'] | emphasis[@emph='Bold' or @emphasisType='em01']">
        <span class="boldemphasis">
            <xsl:apply-templates/>
        </span>
    </xsl:template>
    <!--===============================================-->
    <xsl:template
        match="emphasis[@emph='em02'] | emphasis[@emph='italic'] |emphasis[@emph='ITALIC' or @emphasisType='em02']">
        <span class="italicemphasis">
            <xsl:apply-templates/>
        </span>
    </xsl:template>
    <!--===============================================-->
    <xsl:template
        match="emphasis[@emph='em03'] | emphasis[@emph='underscr'] | emphasis[@emph='UNDERSCR' or @emphasisType='em03']">
        <span class="underlineemphasis">
            <xsl:apply-templates/>
        </span>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="emphasis[@emph='em04' or @emphasisType='em04']">
        <span class="overlinemphasis">
            <xsl:apply-templates/>
        </span>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="emphasis[@emph='em05' or @emphasisType='em05']">
        <span class="strikethroughemphasis">
            <xsl:apply-templates/>
        </span>
    </xsl:template>
    <!--=======处理techpub========================================-->
    <xsl:template match="techpub">
    	<xsl:element name="a">
    	    <xsl:call-template name="t_changeMarker"/>
            	<xsl:attribute name="href">RefTP?pub=<xsl:value-of select="$Publication"/>&amp;string=<xsl:apply-templates/>
                </xsl:attribute>
            	<xsl:attribute name="target">_blank</xsl:attribute>
            <xsl:apply-templates/>
        </xsl:element> &#160; 
    </xsl:template>
    <!--======处理symbol,symbols used for warnings, cautions and notes have now 
    to be marked up by element <symbol> instead of element <graphic>.默认高度100px，宽度100px=========================================-->
    <xsl:template match="symbol">
    	<!-- Modified to support CGM / tiffs 17th Nov 2009 S.Parker-->
    	<xsl:variable name="file">
    		<xsl:value-of select="@boardno|@infoEntityIdent"/>
    	</xsl:variable>
    	<!--2012<xsl:variable name="ext"><xsl:value-of select="aadext:fileExt($file)"/></xsl:variable>-->
    	
    	<!-- Issue 4 allows for the setting of the dimensions, before 4 states "Projects must decide the sizes and scaling of symbols." -->
    	<xsl:variable name="wid">
    		<xsl:choose>
    			<xsl:when test="@reproductionWidth"><xsl:value-of select="@reproductionWidth"/></xsl:when>
    			<xsl:otherwise>100px</xsl:otherwise>
    		</xsl:choose>
    	</xsl:variable>
    	<xsl:variable name="hght">
    		<xsl:choose>
    			<xsl:when test="@reproductionHeight"><xsl:value-of select="@reproductionHeight"/></xsl:when>
    			<xsl:otherwise>100px</xsl:otherwise>
    		</xsl:choose>
    	</xsl:variable>
    	
    	<!--<xsl:choose> 
    		<xsl:when test="translate($ext,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')='CGM' or translate($ext,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')='TIF'">
    			<xsl:element name="object">
    				<xsl:attribute name="id">ivx<xsl:value-of select="generate-id(.)"/></xsl:attribute>
		    		<xsl:attribute name="type">application/x-isoview</xsl:attribute>
		    		<xsl:attribute name="width"><xsl:value-of select="$wid"/></xsl:attribute>
		    		<xsl:attribute name="height"><xsl:value-of select="$hght"/></xsl:attribute>
		    		<xsl:attribute name="border">0</xsl:attribute>
		    		<xsl:attribute name="tools">0</xsl:attribute>
		    		<xsl:element name="param">
		    			<xsl:attribute name="name">src</xsl:attribute>
		    			<xsl:attribute name="value">BinaryResourceStreamer?pub=<xsl:value-of select="$Publication"/>&amp;string=<xsl:value-of select="@boardno|@infoEntityIdent"/></xsl:attribute>
		    		</xsl:element>
		    		<xsl:element name="param">
		    			<xsl:attribute name="name">tools</xsl:attribute>
		    			<xsl:attribute name="value">0</xsl:attribute>
		    		</xsl:element>
			        <xsl:element name="img">
			            <xsl:attribute name="src">BinaryResourceStreamer?pub=<xsl:value-of select="$Publication"
			                    />&amp;string=<xsl:value-of select="@boardno|@infoEntityIdent"
			            /></xsl:attribute>
			            <xsl:attribute name="width"><xsl:value-of select="$wid"/></xsl:attribute>
			        </xsl:element>
			       
				</xsl:element>
    		</xsl:when>
    		<xsl:otherwise>
    			<xsl:element name="img">
		            <xsl:attribute name="src">BinaryResourceStreamer?pub=<xsl:value-of select="$Publication"
		                    />&amp;string=<xsl:value-of select="@boardno|@infoEntityIdent"
		            /></xsl:attribute>
		            <xsl:attribute name="width"><xsl:value-of select="$wid"/></xsl:attribute>
		        </xsl:element>
    		</xsl:otherwise>
    	</xsl:choose>-->
        <!--<xsl:apply-templates/>-->
    </xsl:template>
   <!--=========处理graphic======================================-->   
    <xsl:template match="graphic">
        <xsl:call-template name="t_inlineApplicability">
            <xsl:with-param name="annotation"><xsl:value-of select="$v00034"/>:&#160;</xsl:with-param>
        </xsl:call-template>
        <xsl:apply-templates/>
    </xsl:template>
    <!--========处理热点=======================================-->
    <xsl:template match="hotspot">
        <xsl:call-template name="t_inlineApplicability">
            <xsl:with-param name="annotation"><xsl:value-of select="$v00034"/>:&#160;</xsl:with-param>
        </xsl:call-template>
        
        <xsl:variable name="id">
            <xsl:value-of select="@id"/>
        </xsl:variable>
        <xsl:variable name="params">
            <xsl:call-template name="getAllHotspotsApsname">
                <xsl:with-param name="Xref_ID" select="$id"/>
            </xsl:call-template>
        </xsl:variable>

        <script type="text/javascript"> addHotspotRef ( '<xsl:value-of select="$id"/>', new HotspotLink( "linkToHotSpot( '<xsl:value-of select="//graphic[.//hotspot[translate(@id,'-','-') = $id]]/@boardno|//graphic[.//hotspot[translate(@id,'-','-') = $id]]/@infoEntityIdent"/>' , '<xsl:value-of select="$ISOViewInstalled"/>','<xsl:value-of select="$Publication"/>', <xsl:value-of select="$params"/> )" ) ); </script>
        <xsl:apply-templates/>
    </xsl:template>
    <!-- ============处理param | parameter========================================================= -->
    <xsl:template match="param | parameter">
			<xsl:variable name="boardno"><xsl:value-of select="../@boardno | ../@infoEntityIdent"/></xsl:variable>
			<xsl:variable name="paramid"><xsl:value-of select="@paramid | @parameterIdent"/></xsl:variable>
			<xsl:variable name="paramname"><xsl:value-of select="@paramname | @parameterName"/></xsl:variable>
			<xsl:variable name="paramvalue"><xsl:value-of select="@paramvalue | @parameterValue"/></xsl:variable>
			<xsl:variable name="id"><xsl:value-of select="translate(./@id,'-','-')"/></xsl:variable>
				<script type="text/javascript">addParamRef('<xsl:value-of select="$id"/>', new ParamLink("linkToParam('<xsl:value-of select="$boardno"/>', '<xsl:value-of select="$ISOViewInstalled"/>','<xsl:value-of select="$Publication"/>', '<xsl:value-of select="$paramid"/>', '<xsl:value-of select="$paramname"/>', '<xsl:value-of select="$paramvalue"/>', '<xsl:value-of select="$v02404"/>' ); displayISOLegend(isoLegendDiv<xsl:value-of select="generate-id(..)"/>,'mainForm:legendDiv');  ") )</script>
		    </xsl:template>
    <!-- ===================================================================== -->
    <xsl:template match="hotspot/csnref | hotspot/catalogSeqNumberRef">
        <xsl:variable name="id">
            <xsl:value-of select="../@apsid | ../@applicationStructureIdent"/>
        </xsl:variable>
        <script type="text/javascript"> addLink ( '<xsl:value-of select="ancestor::graphic/@boardno"/><xsl:value-of select="$id"/>', new CSNREFLink("locateCSN('<xsl:value-of select="normalize-space(@refcsn | @catalogSeqNumberValue)"/>','<xsl:value-of select="$Publication"/>')" ) ) </script>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="subscrpt">
        <span class="subscript">
            <xsl:apply-templates/>
        </span>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="supscrpt">
        <span class="supscript">
            <xsl:apply-templates/>
        </span>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="pubcode">
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="pubtitle">
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="pubdate">
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template name="initOutPutArea">
        <script type="text/javascript"> setActualContentHeight(); </script>
    </xsl:template>
    <!--===============================================-->

    <!--===============================================-->
</xsl:stylesheet>
