<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:tir-proc="java:java/com.ptc.arbortext.aad.ietp.tir.IetpTIRProcessor" 
    xmlns:tir-node="java:java/com.ptc.arbortext.aad.ietp.tir.IetpTIRNode"
    >
    <!--2012 xmlns:tir-proc="java:java/com.ptc.arbortext.aad.ietp.tir.IetpTIRProcessor?path=file:///E:/IETM4/IETP/IETP_Data_Update/xsl/java/" exclude-result-prefixes="tir-proc"
    xmlns:tir-node="java:java/com.ptc.arbortext.aad.ietp.tir.IetpTIRNode?
path=file:///E:/IETM4/IETP/IETP_Data_Update/xsl/java/" exclude-result-prefixes="tir-node"-->
    <!-- =============================================== -->
    <xsl:output indent="yes"/>
    <!-- =============================================== -->
    <xsl:include href="capgrp.xsl"/>
    <xsl:include href="fig_tab.xsl"/>
    <xsl:include href="lists.xsl"/>
    <xsl:include href="wcnp.xsl"/>
    <!--===============================================-->
    <xsl:template match="ipc|illustratedPartsCatalog">
        <script language="JavaScript">setIsoViewInstalled('<xsl:value-of select="$ISOViewInstalled"
            />')</script>
        <xsl:call-template name="importCSS"/>
        <xsl:call-template name="ipcFigures"/>
        <!--<xsl:call-template name="ipcEffectivityRange"/>-->
        <xsl:choose>
            <xsl:when test="/dmodule/@presentation = 'ATA100'">
                <xsl:call-template name="ipcTableATA100"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="ipcTable"/>
            </xsl:otherwise>
        </xsl:choose>
        <xsl:call-template name="initOutPutArea"/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template name="ipcFigures">
        <!-- this should show all the graphics with their respective title info-->
<center>
        <xsl:for-each select="figure">
            <xsl:apply-templates select="legend">
                <xsl:with-param name="figureCount" select="@count"/>
            </xsl:apply-templates>
            <!-- show all graphics per figure -->
            <xsl:call-template name="ipcGraphic">
                <xsl:with-param name="figCount" select="@count"/>
                <xsl:with-param name="figTitle">
                    <xsl:value-of select="$v00355"/>
                    <xsl:text>&#160;</xsl:text>
                    <xsl:value-of select="@count"/>
                    <xsl:text>&#160;-&#160;</xsl:text>
                    <xsl:value-of select="./title"/>
                </xsl:with-param>
            </xsl:call-template>
            	<!-- now show the title info for the figure 
				<xsl:call-template name="ipcFigureTitle"/>-->
        </xsl:for-each>
        <xsl:for-each select="multimedia">
            <xsl:call-template name="ipcMultimedia"/>
        </xsl:for-each>
</center>
    </xsl:template>
    <!--===============================================-->
    <xsl:template name="ipcGraphic">
        <xsl:param name="figCount" select="1"/>
        <xsl:param name="figTitle" select="''"/>
        <!--
	this template uses a nasty table hack to lay out the graphics and
	graphic hyperlinks in an ipd dm
		-->
        <div class="ipcFigure">
            <!-- will show all graphics under current firgure -->
            <xsl:for-each select="graphic">
                <span>
                    <!-- display inline shows the tables on one line, will wrap if required-->
                    <table style="display: inline;">
                        <tr>
                            <td>
                                <!-- show image-->
                                <xsl:call-template name="displayGraphic">
									<xsl:with-param name="figCount" select="$figCount"/>
									<xsl:with-param name="figTitle" select="$figTitle"/>
									<xsl:with-param name="graphicSheetCountJS" select="@graphicSheetCountJS"/>
								</xsl:call-template>
                            </td>
                        </tr>
                        <tr>
                            <td>
                                <!-- show image link-->
                              	<xsl:call-template name="graphicHyperLink">
									<xsl:with-param name="figCount" select="$figCount"/>
									<xsl:with-param name="figTitle" select="$figTitle"/>
									<xsl:with-param name="graphicSheetCountJS" select="@graphicSheetCountJS"/>
								</xsl:call-template>-->
                            </td>
                        </tr>
                    </table>
                </span>
            </xsl:for-each>
        </div>
    </xsl:template>
    <!--===============================================-->
    <xsl:template name="ipcMultimedia">
        <div class="ipcMultimedia">
            <!-- will show all graphics under current firgure -->
            <xsl:for-each select="multimediaobject | multimediaObject">
                <span>
                    <!-- display inline shows the tables on one line, will wrap if required-->
                    <table style="display: none">
                        <tr>
                            <td>
                                <!-- show image-->
                                <xsl:call-template name="displayGraphic"/>
                            </td>
                        </tr>
                       	<!--<tr>
                            <td>-->
                                <!-- show image link-->
                               <!-- <xsl:call-template name="graphicHyperLink"/>
                            </td>
                        </tr>-->
                    </table>
                </span>
            </xsl:for-each>
        </div>
    </xsl:template>
    <!--===============================================-->
    <xsl:template name="displayGraphic">
	<xsl:param name="figCount" select="''"/>
		<xsl:param name="figTitle" select="''"/>
		<xsl:param name="graphicSheetCountJS" select="''"/>
        <xsl:variable name="boardnum">
            	<xsl:value-of select="translate(@boardno|@infoEntityIdent,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')"/>
        </xsl:variable>
        <xsl:choose>
            <!-- load the image into a the ISOview control when we have a CGM or tif file and ISOview is available. -->
            <xsl:when
                test="(contains($boardnum, '.CGM') or contains($boardnum, '.TIF') or contains($boardnum, '.ISO')) and $ISOViewInstalled='yes'">
                <!--
		the isoview control tends to get confused when dealing with multiple objects
		of the same type, especially when they get instantiated on top of each other causing
		issues with properties getting passed between the two instances.
		Here we have the cgm hidden in the background to enable interogation of the cmg for
		hotspotting, we then show the jpg
	 -->
                	<object id="cgmimage" classid="CLSID:865B2280-2B71-11D1-BC01-006097AC382A" border="0" style="width:0px; height:0px;">
					
					<param name="src">
						<xsl:attribute name="value">BinaryResourceStreamer?pub=<xsl:value-of select="$Publication"/>&amp;string=<xsl:value-of select="$boardnum"/></xsl:attribute>
					</param>
					<param name="file">
						<xsl:attribute name="value"><xsl:value-of select="$boardnum"/></xsl:attribute>
					</param>
					<!--<param name="Enabled">0</param>
					<param name="Tools">0</param>-->
				</object>
                <xsl:call-template name="doJPGImage"/>
            </xsl:when>
            <!-- load the the alternative jpg image when we have a CGM file and ISOview is NOT available. -->
            <xsl:when
                test="(contains($boardnum, '.cgm') or contains($boardnum, '.iso'))  and ($ISOViewInstalled='no')">
                <xsl:call-template name="doJPGImage"/>
            </xsl:when>
            <xsl:otherwise>
                <!-- load the the alternative jpg image when we have a CGM file and ISOview is NOT available. -->
                <img width="100px" height="100px" border="0">
                    <xsl:attribute name="alt">BinaryResourceStreamer?pub=<xsl:value-of
                            select="$Publication"/>&amp;string=<xsl:value-of select="$boardnum"
                        /></xsl:attribute>
                    <xsl:attribute name="src">BinaryResourceStreamer?pub=<xsl:value-of
                            select="$Publication"/>&amp;string=<xsl:value-of select="$boardnum"
                        /></xsl:attribute>
                </img>
            </xsl:otherwise>
        </xsl:choose>
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    	<xsl:template name="doJPGImage">
		<!--<img width="100px" height="100px" border="0">-->
		<img border="0">
			<xsl:attribute name="alt">BinaryResourceStreamer?pub=<xsl:value-of select="$Publication"/>&amp;string=<xsl:value-of select="substring-before(@boardno|@infoEntityIdent,'.')"/>.jpg</xsl:attribute>
			<xsl:attribute name="src">BinaryResourceStreamer?pub=<xsl:value-of select="$Publication"/>&amp;string=<xsl:value-of select="substring-before(@boardno|@infoEntityIdent,'.')"/>.jpg</xsl:attribute>
		</img>
	</xsl:template>
    <!--===============================================-->
    <xsl:template name="graphicHyperLink">
        <xsl:param name="figCount" select="''"/>
        <xsl:param name="figTitle" select="''"/>
        <xsl:param name="graphicSheetCountJS" select="''"/>

        <!-- called at graphic context to produce a link to the graphic -->
        <script> addFigure('<xsl:value-of select="@boardno|@infoEntityIdent"/>'); </script>
        <xsl:element name="span">
            <xsl:attribute name="id">
                <xsl:value-of select="@boardno|@infoEntityIdent"/>
            </xsl:attribute>
            <xsl:attribute name="onclick"> updateLegendDiv('<xsl:value-of select="$figCount"/>',
                'mainForm:legendDiv'); graphicTitle.updateTitleDiv('<xsl:value-of
                    select="$figCount"/>','<xsl:value-of select="$graphicSheetCountJS"/>',
                'mainForm:graphicTitleDiv'); loadImage('<xsl:value-of
                    select="@boardno|@infoEntityIdent"/>','<xsl:value-of select="$Publication"
                    />','<xsl:value-of select="$ISOViewInstalled"/>'); </xsl:attribute>
            <xsl:attribute name="style">cursor:hand; color:blue</xsl:attribute>
            <xsl:value-of select="@boardno|@infoEntityIdent"/>
            <div style="display:none">
                <xsl:value-of select="locator/@href"/>
            </div>
        </xsl:element>

        <xsl:variable name="figureSheetNumberText">
            <xsl:if test="@graphicSheetDisplayNumber and @graphicSheetTotal">
                <xsl:call-template name="sheetNumberingText">
                    <xsl:with-param name="sheetCount" select="@graphicSheetDisplayNumber"/>
                    <xsl:with-param name="sheetTotal" select="@graphicSheetTotal"/>
                </xsl:call-template>
            </xsl:if>
        </xsl:variable>

        <xsl:variable name="graphicTitleText">
            <xsl:value-of select="$figTitle"/>
            <xsl:if test="$figureSheetNumberText != '' ">
                <xsl:text>&#160;&#40;</xsl:text>
                <xsl:value-of select="$figureSheetNumberText"/>
                <xsl:text>&#41;</xsl:text>
            </xsl:if>
        </xsl:variable>

        <script> graphicTitle.add('<xsl:value-of select="$figCount"/>', '<xsl:value-of
                select="$graphicSheetCountJS"/>', '<xsl:value-of select="$graphicTitleText"/>');
        </script>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="ipc/ipp">
        <!-- == -->
    </xsl:template>
    <!--===============================================-->
    <xsl:template name="ipcFigureTitle">
        <!-- outputs a title for the ipc -->
        <div class="ipcFigureTitle">
            <xsl:value-of select="$v00357"/>&#160;:&#160;<xsl:apply-templates select="title"/>
            <br/>
            <xsl:call-template name="dmcOutput"/>
        </div>
    </xsl:template>
    <!--===============================================-->
    <xsl:template name="ipcEffectivityRange">
        <!-- outputs and formats the effectivity range -->
        <div class="ipcEffectivityRange">
            <xsl:value-of select="$v00283"/>
            <xsl:choose>
                <xsl:when test="/dmodule/idstatus/status/applic/model/version/versrank/range">
                        (<xsl:value-of select="$v00547"/>
                    <xsl:for-each
                        select="/dmodule/idstatus/status/applic/model/version/versrank/range">
                        <xsl:value-of select="@from"/>
                        <xsl:value-of select="@to"/>
                        <xsl:if test="following-sibling::range">, </xsl:if>
                    </xsl:for-each>) </xsl:when>
                <xsl:otherwise>(<xsl:value-of select="$v00546"/>)</xsl:otherwise>
            </xsl:choose>
        </div>
    </xsl:template>
    <!--===============================================-->
    <xsl:template name="ipcTable">
        <!-- do ipd table -->
        <table class="ipcTable">
            <tr>
                <th class="ipcTableTH1">
                    <xsl:value-of select="$v00354"/>
                </th>
                <th class="ipcTableTH2">
                    <xsl:value-of select="$v00475"/>
                </th>
                <th class="ipcTableTH3">
                    <xsl:value-of select="$v01125"/>
                </th>
                <th class="ipcTableTH4">
                    <xsl:value-of select="$v00558"/>
                </th>
                <th class="ipcTableTH5">
                    <xsl:value-of select="$v01114"/>
                </th>
                <th class="ipcTableTH6">
                    <xsl:value-of select="$v00583"/>
                </th>
                <th class="ipcTableTH7">*<xsl:value-of select="$v01017"/>
                </th>
                <th class="ipcTableTH8">
                    <xsl:value-of select="$v00550"/>
                </th>
                <th class="ipcTableTH9">
                    <xsl:value-of select="$v00401"/>
                </th>
            </tr>
            <tbody>
                <tr>
                    <td>
                    	<xsl:call-template name="t_changeMarker"/>
                        <xsl:value-of
                            select="substring(csn/@csn|catalogSeqNumber/@catalogSeqNumberValue,7,3)"
                        />
                    </td>
                </tr>
                <!-- each csn holds an isn which is a table row -->
                <xsl:apply-templates select="csn|catalogSeqNumber"/>
            </tbody>
        </table>
    </xsl:template>
    <!--===============================================-->
    <!--

        PIL only - don't update with issue 4.0 support.

    -->
    <xsl:template name="ipcTableATA100">
        <!-- do ipd table -->
        <table class="ipcTable">
            <tr>
                <th class="ipcTableTH1ATA">
                    <xsl:value-of select="$v00354"/>
                </th>
                <th class="ipcTableTH2ATA">
                    <xsl:value-of select="$v00475"/>
                </th>
                <th class="ipcTableTH3ATA">
                    <xsl:value-of select="$v01126"/>
                </th>
                <th class="ipcTableTH4ATA">
                    <xsl:value-of select="$v00528"/>
                </th>
                <th class="ipcTableTH5ATA">
                    <xsl:value-of select="$v00583"/>
                </th>
                <th class="ipcTableTH6ATA">
                    <xsl:value-of select="$v00742"/>
                </th>
                <th class="ipcTableTH7ATA">
                    <xsl:value-of select="$v00278"/>
                </th>
            </tr>
            <tbody>
                <tr>
                    <td>
                        <xsl:value-of select="substring(csn/@csn,7,3)"/>
                    </td>
                </tr>
                <!-- each csn holds an isn which is a table row -->
                <xsl:apply-templates select="csn" mode="ATA100"/>
            </tbody>
        </table>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="isn | itemSequenceNumber">

        <xsl:variable name="item">
            <xsl:value-of select="../@item| ../@catalogItemNumber"/>
        </xsl:variable>
        <xsl:variable name="boardno">
            <xsl:value-of select="//multimediaobject[param/@id=$item]/@boardno | @infoEntityIdent"/>
        </xsl:variable>
        <xsl:variable name="paramid">
            <xsl:value-of select="//multimediaobject/param[@id=$item]/@paramid"/>
        </xsl:variable>
        <xsl:variable name="paramname">
            <xsl:value-of select="//multimediaobject/param[@id=$item]/@paramname"/>
        </xsl:variable>
        <xsl:variable name="paramvalue">
            <xsl:value-of select="//multimediaobject/param[@id=$item]/@paramvalue"/>
        </xsl:variable>

        <tr>
			<xsl:variable name="rowClass">ipcTableCursor</xsl:variable>
			<xsl:attribute name="class"><xsl:value-of select="$rowClass"/></xsl:attribute>
			<xsl:call-template name="t_changeMarker">
				<!-- the existing style class for the row is passed in to the change marker template  so that change marker template can 
					just append its own style calss, rather than overwrite it  -->
				<xsl:with-param name="existingClasses"><xsl:value-of select="$rowClass"/></xsl:with-param>	
			</xsl:call-template>
			
			<xsl:attribute name="id"><xsl:call-template name="getCsnItemNumber"/></xsl:attribute>
			
            <xsl:variable name="NotIll">
                <xsl:value-of select="@nil|@notIllustrated"/>
            </xsl:variable>
            <xsl:choose>
                <xsl:when test="//multimediaobject |//multimediaObject">
                    <!--
					The ISO 3D animation linking is being suppressed for the time being.
					The linking mechanism has not been thought though for 4.3 M020.
					This will be addressed at a later stage.
					(NS 17/12/2009)

					  -->
                    <!--
					<xsl:attribute name="onclick">isnRowClicked(this);linkToParam('<xsl:value-of select="$boardno"/>',
      													'<xsl:value-of select="$ISOViewInstalled"/>',
      													'<xsl:value-of select="$Publication"/>',
      													'<xsl:value-of select="$paramid"/>',
      													'<xsl:value-of select="$paramname"/>',
      													'<xsl:value-of select="$paramvalue"/>',
      													'<xsl:value-of select="$v02404"/>');
      													displayISOLegend(isoLegendDiv<xsl:value-of select="generate-id(.)"/>,'mainForm:legendDiv');</xsl:attribute>
      				 -->
                </xsl:when>
                <xsl:otherwise>

                    <xsl:attribute name="onclick"
                            >isnRowClicked(this);linkToImage(this,'<xsl:value-of
                            select="$ISOViewInstalled"/>');</xsl:attribute>
                </xsl:otherwise>
            </xsl:choose>
            <xsl:call-template name="nilColumn"/>
            <xsl:call-template name="csnItemNumber"/>
            <xsl:call-template name="qnaColumn"/>
            <xsl:call-template name="mfcColumn"/>
            <xsl:call-template name="pnrColumn"/>
            <xsl:call-template name="nomenclatureColumn"/>
            <xsl:call-template name="ucaColumn"/>
            <xsl:call-template name="cesColumn"/>
            <xsl:call-template name="icyColumn"/>
            <td>
                <span>
                    <xsl:attribute name="id">isoLegendDiv<xsl:value-of select="generate-id(.)"/></xsl:attribute>
                    <xsl:attribute name="style">display:none</xsl:attribute>
                    <xsl:call-template name="ParamLegendContent">
                        <xsl:with-param name="boardno" select="$boardno"/>
                    </xsl:call-template>
                </span>
            </td>
        </tr>

    </xsl:template>
    <!--===============================================-->
    <xsl:template match="isn" mode="ATA100">
        <!-- every isn is a table row -->
        <tr>
			<xsl:variable name="rowClass">ipcTableCursor</xsl:variable>
			<xsl:attribute name="class"><xsl:value-of select="$rowClass"/></xsl:attribute>
			<xsl:call-template name="t_changeMarker">
				<!-- the existing style class for the row is passed in to the change marker template  so that change marker template can 
					just append its own style calss, rather than overwrite it  -->
				<xsl:with-param name="existingClasses"><xsl:value-of select="$rowClass"/></xsl:with-param>	
			</xsl:call-template>
			
			<xsl:attribute name="id"><xsl:call-template name="getCsnItemNumber"/></xsl:attribute>
			
            <xsl:variable name="NotIll">
                <xsl:value-of select="@nil"/>
            </xsl:variable>
            <xsl:attribute name="onclick">isnRowClicked(this);linkToImage(this,'<xsl:value-of
                    select="$ISOViewInstalled"/>')</xsl:attribute>
            <!-- TODO implement this matrix1 functionality
			<xsl:choose>
					<xsl:when test="NotIll != '' ">
						<xsl:attribute name="onclick">ClearTableHighLights();selectRow(this)</xsl:attribute>
					</xsl:when>
					<xsl:otherwise>
						<xsl:attribute name="onclick">ClearTableHighLights();selectRow(this);linkToImage(this);</xsl:attribute>
					</xsl:otherwise>
				</xsl:choose>
			-->
            <!-- each named template call creates a table cell and then applies templates-->
            <xsl:call-template name="nilColumn"/>
            <xsl:call-template name="csnItemNumber"/>
            <xsl:call-template name="vendorColumn"/>
            <xsl:call-template name="pnrColumnATA100"/>
            <xsl:call-template name="nomenclatureColumnATA100"/>
            <xsl:call-template name="qnaColumn"/>
            <xsl:call-template name="cesColumn"/>
        </tr>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="csn" mode="ATA100">
        <xsl:choose>
            <xsl:when test="./isn/cbs/asp">
                <xsl:if test="not(preceding-sibling::csn[1]/isn/cbs/asp/@asp)">
                    <tr>
                        <td/>
                        <td/>
                        <td/>
                        <td/>
                        <td>
                            <div align="center" style="text-align : left; font: bold 16px">- - - -
                                    <xsl:value-of select="$v00056"/> - - - -</div>
                        </td>
                        <td/>
                        <td/>
                    </tr>
                </xsl:if>
            </xsl:when>
            <xsl:otherwise>
                <xsl:if test="preceding-sibling::csn[1]/isn/cbs/asp/@asp">
                    <tr>
                        <td/>
                        <td/>
                        <td/>
                        <td/>
                        <td>
                            <div align="center" style="text-align : left; font: bold 16px">- - - - -
                                - - - - * - - - - - - - - - -</div>
                        </td>
                        <td/>
                        <td/>
                    </tr>
                </xsl:if>
            </xsl:otherwise>
        </xsl:choose>
        <!--end attaching parts-->
        <!--get the rest of the content-->
        <xsl:apply-templates mode="ATA100"/>
        <!--if last csn is part of attaching parts-->
        <!-- 'close' attaching part group -->
        <xsl:if test="./isn/cbs/asp/@asp and not(following-sibling::csn)">
            <tr>
                <td/>
                <td/>
                <td/>
                <td/>
                <td>
                    <div align="center" style="text-align : left; font: bold 16px">- - - - - - - - -
                        * - - - - - - - - - -</div>
                </td>
                <td/>
                <td/>
            </tr>
        </xsl:if>
    </xsl:template>
    <!--===============================================-->
    <xsl:template name="nilColumn">
        <td>
        	<xsl:call-template name="t_changeMarker"/>
            <xsl:attribute name="id">csnref<xsl:value-of
                    select="translate(normalize-space(../@csn|../@catalogSeqNumberValue), ' ', '_') "
                /></xsl:attribute>
            <xsl:apply-templates select=".//nil | .//notIllustrated"/>
        </td>
    </xsl:template>
    <!--===============================================-->
    <xsl:template name="csnItemNumber">
        <!-- called from isn level -->
        <td id="itemName">
        	<xsl:call-template name="t_changeMarker"/>
			<xsl:call-template name="getCsnItemNumber"/>
		</td>
	</xsl:template>
	
	
	<xsl:template name="getCsnItemNumber">
            <xsl:choose>
                <xsl:when test="number(../@item|../@catalogItemNumber)">
                    <xsl:value-of select="format-number(../@item|../@catalogItemNumber,0)"/>
                </xsl:when>
                <xsl:when test="not(../@item|../@catalogItemNumber)">
                    <!-- Show nothing when we don't have an item attribute. -->
                </xsl:when>
                <xsl:otherwise>
                    <xsl:variable name="itemnum">
                        <xsl:if test="../@item | ../@catalogItemNumber">
							<xsl:value-of select="format-number(substring(../@item | ../@catalogItemNumber,1,3),0)"/>
                        </xsl:if>
                    </xsl:variable>
                    <xsl:variable name="variant">
                        <xsl:value-of select="substring(../@item | ../@catalogItemNumber,4,1)"/>
                    </xsl:variable>
					<xsl:if test="not($itemnum = '0') and not($itemnum = 'NaN')">
                        <xsl:value-of select="$itemnum"/>
						<xsl:value-of select="translate($variant,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')"/>
                    </xsl:if>
                </xsl:otherwise>
            </xsl:choose>
    </xsl:template>
    <!--===============================================-->
    <xsl:template name="qnaColumn">
        <td>
        	<xsl:call-template name="t_changeMarker"/>
            <xsl:apply-templates select="qna|quantityPerNextHigherAssy"/>
        </td>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="qna|quantityPerNextHigherAssy">
        <xsl:choose>
            <xsl:when test="number(text())">
                <xsl:value-of select="format-number(text(), &quot;#&quot;)"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:apply-templates/>
            </xsl:otherwise>
        </xsl:choose> &#160;<xsl:apply-templates
            select="../pas/uoi|../partIdentSegment/unitOfIssue"/>
        <div style="display:none">
            <!-- store the whole unformated nsn in a hidden div. this will be used to as search value when the row is selected. -->
            <xsl:value-of select="."/>
        </div>
    </xsl:template>
    <!--===============================================-->
    <xsl:template name="mfcColumn">
        <td>
        	<xsl:call-template name="t_changeMarker"/>
            <xsl:apply-templates select="mfc|manufacturerCode"/>
        </td>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="uoi|unitOfIssue"> / <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template name="pnrColumn">
        <td>
        	<xsl:call-template name="t_changeMarker"/>
            <xsl:attribute name="title">
                <xsl:for-each select="altpart"><xsl:sort select="@type" order="descending"
                        /><xsl:value-of select="@type"/>: <xsl:value-of select="$v00534"/>:
                        <xsl:value-of select="altmfc"/><xsl:value-of select="$v00675"/>
                        :<xsl:value-of select="altpnr"/>; </xsl:for-each>
            </xsl:attribute>
            <xsl:apply-templates select="pnr|partNumber"/>
            <br/>
            <xsl:if test="nsn/@nsn|natoStockNumber">
                <div style="display:none">
                    <xsl:value-of select="nsn/@nsn|natoStockNumber"/>
                </div>
                <xsl:call-template name="T_FormatNSN"/>
            </xsl:if>
        </td>
    </xsl:template>
    <!--===============================================-->
    <xsl:template name="pnrColumnATA100">
        <td>
        	<xsl:call-template name="t_changeMarker"/>
            <xsl:attribute name="title">
                <xsl:for-each select="altpart">
                    <xsl:sort select="@type" order="descending"/>
                    <xsl:choose>
                        <xsl:when test="/dmodule/@spec='PIL-IPC'">
                            <xsl:if test="not(@type='VENDOR')"><xsl:value-of select="@type"/>:
                                    <xsl:value-of select="$v00534"/>: <xsl:value-of select="altmfc"
                                    /><xsl:value-of select="$v00675"/>: <xsl:value-of
                                    select="altpnr"/></xsl:if>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:if test="not(following-sibling::csn/isn/altpart)"><xsl:value-of
                                    select="@type"/>: <xsl:value-of select="$v00534"/>:
                                    <xsl:value-of select="altmfc"/><xsl:value-of select="$v00675"/>:
                                    <xsl:value-of select="altpnr"/></xsl:if>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:for-each>
            </xsl:attribute>
            <xsl:apply-templates select="pnr"/>
            <br/>
            <xsl:if test="nsn/@nsn">
                <div style="display:none">
                    <xsl:value-of select="nsn/@nsn"/>
                </div>
                <xsl:call-template name="T_FormatNSN"/>
            </xsl:if>
        </td>
    </xsl:template>
    <!--===============================================-->
    <xsl:template name="cesColumn">
        <td>
        	<xsl:call-template name="t_changeMarker"/>
            <xsl:apply-templates select="ces | .//locationRcmd"/>
        </td>
    </xsl:template>
    <!--===============================================-->
    <xsl:template name="vendorColumn">
        <td>
        	<xsl:call-template name="t_changeMarker"/>
            <xsl:if test="./altpart/@type='VENDOR'">
                <xsl:value-of select="./altpart[@type='VENDOR']/altmfc"/>
            </xsl:if>
        </td>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="ces | locationRcmd">
        <xsl:apply-templates select="mov | modelVersion"/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="mov | modelVersion">
        <xsl:value-of select="@mov | @modelVersionValue"/>
        <xsl:apply-templates select="efy"/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="efy">
        <br/>
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template name="ucaColumn">
        <td>
        	<xsl:call-template name="t_changeMarker"/>
            <xsl:apply-templates select="ccs/uca | applicabilitySegment/usableOnCodeAssy"/>
        </td>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="uca | usableOnCodeAssy"> *<xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template name="T_FormatNSN">
        <!-- formats the nsn-->
        <xsl:variable name="NSN_Temp">
            <xsl:value-of select="nsn/@nsn | natoStockNumber"/>
        </xsl:variable>
        <xsl:variable name="NatoStockNumberFull">
            <xsl:value-of select="translate($NSN_Temp,'- ' ,'')"/>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="string-length($NatoStockNumberFull) &gt; 4">
                <xsl:variable name="NatoStockNumber">
                    <xsl:value-of select="substring($NatoStockNumberFull,1,4)"/>-<xsl:value-of
                        select="substring($NatoStockNumberFull,5,2)"/>-<xsl:value-of
                        select="substring($NatoStockNumberFull,7,3)"/>-<xsl:value-of
                        select="substring($NatoStockNumberFull,10,4)"/>
                </xsl:variable>
                <xsl:value-of select="$NatoStockNumber"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$NatoStockNumberFull"/>- </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <!--===============================================-->
    <xsl:template name="nomenclatureColumn">
        <td>
        	<xsl:call-template name="t_changeMarker"/>
            <xsl:call-template name="dfpContent"/>
            <!-- store the DFP in a hidden div. this will be used to as search value when the row is selected. -->
            <div style="display:none">
                <!--<xsl:value-of select="pas/dfp|partIdentSegment/descrForPart"/>-->
                <xsl:call-template name="t_ipd_name"/>
            </div>
        </td>
    </xsl:template>
    <!--===============================================-->
    <!-- Specialised output for ATA100 presentation. -->
    <xsl:template name="nomenclatureColumnATA100">
        <td>
        	<xsl:call-template name="t_changeMarker"/>
            <xsl:call-template name="dfpContentATA100"/>
            <!-- store the DFP in a hidden div. this will be used to as search value when the row is selected. -->
            <div style="display:none">
                <xsl:value-of select="pas/dfp"/>
            </div>
        </td>
    </xsl:template>
    <!--===============================================-->
    <xsl:template name="dfpContent">
        <table class="dfpContentTable" width="100%">
            <tr>
                <td>
                    <xsl:call-template name="nomenclatureColumnInd"/>
                    <!-- Now Build up the data section for Nomenclature. -->
                    <!--<xsl:apply-templates select="pas/dfp|partIdentSegment/descrForPart"/>-->
                    <xsl:call-template name="t_ipd_name"/>
                    <xsl:if test="altpart[@type='VENDOR']/altpnr">
                        <br/>
                        <xsl:apply-templates select="altpart[@type='VENDOR']/altpnr"/>
                    </xsl:if>
                    <!-- RTX-->
                    <xsl:call-template name="rtxContent"/>
                </td>
            </tr>
        </table>
    </xsl:template>
    <!--===============================================-->
    <!--
		Specialised output for ATA100 presentation.
	 -->
    <xsl:template name="dfpContentATA100">
        <table class="dfpContentTable" width="100%">
            <tr>
                <td>
                    <xsl:call-template name="nomenclatureColumnInd"/>
                </td>
                <!-- Now Build up the data section for Nomenclature. -->
                <td>
                    <xsl:apply-templates select="pas/dfp"/>
                    <xsl:if test="altpart[@type='VENDOR']/altpnr">
                        <br/>
                        <xsl:apply-templates select="altpart[@type='VENDOR']/altpnr"/>
                    </xsl:if>
                    <xsl:if test="cbs/dfl/text()">
                        <br/>
                        <xsl:apply-templates select="cbs/dfl"/>
                    </xsl:if>
                    <!-- RTX-->
                    <xsl:call-template name="rtxContent"/>
                </td>
            </tr>
        </table>
    </xsl:template>
    <!--===============================================-->
    <xsl:template name="nomenclatureColumnInd">
        <xsl:variable name="pattern">
            <xsl:choose>
                <xsl:when test="cbs/asp/@asp = '1'">* </xsl:when>
                <xsl:otherwise>
                    <xsl:choose>
                        <xsl:when
                            test="(../@ind &gt; 0 and ../@ind &lt; 10) or (../@indenture &gt; 0 and ../@indenture &lt; 10)"
                            >. </xsl:when>
                        <xsl:otherwise>* </xsl:otherwise>
                    </xsl:choose>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:call-template name="T_PatternLoop">
            <xsl:with-param name="pattern" select="$pattern"/>
            <xsl:with-param name="index" select="(../@ind | ../@indenture) - 1"/>
        </xsl:call-template>
    </xsl:template>
    <!--===============================================-->
    <xsl:template name="rtxContent">
        <!-- called from isn level -->
        <!-- Check to is if the Extra values are there if they are output will be within brackets. -->
        <xsl:choose>
            <xsl:when
                test="pas/pcs/qui | cbs/smf/mfm | ctl/@refisn | ils | can | cbs/rtx | partLocationSegment/referTo | ftc | ctl "
                > (<xsl:value-of select="$v00809"/>
                <xsl:value-of select="cbs/dfl"/>
                <xsl:if test="can"> /<xsl:value-of select="can"/>
                </xsl:if>
                <xsl:for-each select="ils"> /<xsl:value-of select="text()"/>
                </xsl:for-each>
                <xsl:if test="cbs/asp/@asp = '2'">(<xsl:value-of select="$v00881"/>)</xsl:if>
                <xsl:if test="cbs/asp/@asp= '3'">(<xsl:value-of select="$v00861"/>)</xsl:if>
                <xsl:if test="cbs/rtx/csnref | partLocationSegment/referTo/catalogSeqNumberRef">
                    <xsl:variable name="refcsnVal"
                        select="cbs/rtx/csnref/@refcsn | partLocationSegment/referTo/catalogSeqNumberRef/@catalogSeqNumberValue"/>
                    <xsl:variable name="CSNLink">
                        <xsl:value-of
                            select="/dmodule//dmaddres//modelic | /dmodule//dmAddress//@modelIdentCode"
                            />-<xsl:value-of
                            select="/dmodule//dmaddres//sdc | /dmodule//dmAddress//@systemDiffCode"
                            />-<xsl:value-of select="substring($refcsnVal,1,2)"/>-<xsl:value-of
                            select="substring($refcsnVal,3,2)"/>-<xsl:value-of
                            select="substring($refcsnVal,5,2)"/>-<xsl:value-of
                            select="substring($refcsnVal,7,2)"/>
                        <xsl:value-of
                            select="/dmodule//dmaddres//discodev | /dmodule//dmAddress//@disassyCodeVariant"
                            />-<xsl:value-of
                            select="/dmodule//incode | /dmodule//dmAddress//@infoCode"/>
                        <xsl:value-of
                            select="/dmodule//dmaddres//incodev | /dmodule//dmAddress//@infoCodeVariant"
                            />-<xsl:value-of
                            select="/dmodule//dmaddres//itemloc | /dmodule//dmAddress//@itemLocationCode"
                        />
                    </xsl:variable>
                    <!-- the id of the isn row (current context is the item attribute value  -->
					<xsl:variable name="javascriptCall">nestedObjectEventHandler(event.type,'<xsl:call-template name="getCsnItemNumber"/>', isnOnclick)</xsl:variable>
                    <!-- Link to CSNRef -->
                    <span>
                        <xsl:attribute name="onmouseover">
                            <xsl:value-of select="$javascriptCall"/>
                        </xsl:attribute>
                        <xsl:attribute name="onmouseout">
                            <xsl:value-of select="$javascriptCall"/>
                        </xsl:attribute>
                        <xsl:attribute name="title"><xsl:value-of select="$v00771"
                                />&#160;<xsl:value-of select="$refcsnVal"/></xsl:attribute>
                        <xsl:attribute name="style">text-decoration: underline;color:
                            rgb(0,0,255)};cursor:hand;</xsl:attribute>
                        <xsl:attribute name="onclick">Richfaces.showModalPanel('busyPanel');
                                locateCSN('<xsl:value-of select="normalize-space($refcsnVal)"/>',
                                '<xsl:value-of select="$Publication"/>')</xsl:attribute>
                        <xsl:value-of select="$refcsnVal"/>
                    </span>
                </xsl:if>
                <xsl:if test="cbs/rtx/@refipp">/<xsl:value-of select="$v00770"/>:<xsl:value-of
                        select="cbs/rtx/@refipp"/>
                </xsl:if>
                <xsl:if test="ctl/@refcsn"> /<xsl:value-of select="$v00175"/>
                    <xsl:value-of select="ctl/@refisn"/>
                    <xsl:if test="ftc/@value='1'">/<xsl:value-of select="$v00539"/>/</xsl:if>
                </xsl:if>
                <xsl:if test="cbs/smf/@value='M'">/<xsl:value-of select="$v00525"/>
                    <xsl:value-of select="cbs/smf/mfm/text()"/>
                </xsl:if>
                <xsl:if test="pas/pcs/uom/text()">/<xsl:value-of select="$v00895"/>
                    <xsl:value-of select="pas/pcs/uom/text()"/>,<xsl:value-of
                        select="pas/pcs/qui/text()"/>
                </xsl:if> ) </xsl:when>
            <xsl:otherwise>
                <xsl:if test="cbs/asp/@asp = '2'">(<xsl:value-of select="$v00881"/>)</xsl:if>
                <xsl:if test="cbs/asp/@asp= '3'">(<xsl:value-of select="$v00861"/>)</xsl:if>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <!--===============================================-->
    <xsl:template name="T_PatternLoop">
        <xsl:param name="pattern"/>
        <xsl:param name="index"/>
        <xsl:if test="$index &gt; 0">
            <xsl:value-of select="$pattern"/>
            <xsl:call-template name="T_PatternLoop">
                <xsl:with-param name="index" select="$index - 1"/>
                <xsl:with-param name="pattern" select="$pattern"/>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>
    <!--===============================================-->
    <xsl:template name="icyColumn">
        <td>
        	<xsl:call-template name="t_changeMarker"/>
            <xsl:apply-templates select="ccs/icy | applicabilitySegment/interchangeability"/>
        </td>
    </xsl:template>
    <!--===============================================-->
    <xsl:template name="t_ipd_name">
        <!--2012<xsl:choose>
            <xsl:when
                test="(child::mfc|child::manufacturerCode) and (child::pnr|child::partNumber) and tir-proc:validateTIRReference(.)">
                <xsl:variable name="tirItem" select="tir-proc:dereferenceTIRNode(.)"/>
                <xsl:variable name="tirName">
                    <xsl:value-of select="tir-node:query($tirItem, '//itemIdentData/name/text()|//nomdata/nomen/text()')"/>
                </xsl:variable>
                <xsl:variable name="pnr" select="pnr/text()|partNumber/text()"/>
                <xsl:variable name="mfc" select="mfc/text()|manufacturerCode/text()"></xsl:variable>
                <span class="tirRef" title="Reference to part: MFC: {$mfc} / Pt No: {$pnr}, {$tirName}">
                    <xsl:call-template name="t_generate_tir_click_event">
                        <xsl:with-param name="tirItem" select="$tirItem"/>
                    </xsl:call-template> MFC:&#160;<xsl:value-of select="$mfc"
                    /> / Pt No:<xsl:value-of select="$pnr"/><xsl:if test="$tirName"
                            >,&#160;<xsl:value-of select="$tirName"/></xsl:if>
                </span>
            </xsl:when>
            <xsl:when
                test="(child::mfc|child::manufacturerCode) and (child::pnr|child::partNumber) ">
                <xsl:variable name="tirItem" select="tir-proc:dereferenceTIRNode(.)"/>
                <xsl:variable name="tirName">
                    <xsl:value-of select="tir-node:query($tirItem, '//itemIdentData/name/text()|//nomdata/nomen/text()')"/>
                </xsl:variable>
                <xsl:variable name="pnr" select="pnr/text()|partNumber/text()"/>
                <xsl:variable name="mfc" select="mfc/text()|manufacturerCode/text()"></xsl:variable>
                <span class="tirRef" title="Reference to part: MFC: {$mfc} / Pt No: {$pnr}, {$tirName}">
                    <xsl:call-template name="t_generate_tir_click_event">
                        <xsl:with-param name="tirItem" select="$tirItem"/>
                    </xsl:call-template> MFC:&#160;<xsl:value-of select="$mfc"
                    /> / Pt No:<xsl:value-of select="$pnr"/><xsl:if test="$tirName"
                            >,&#160;<xsl:value-of select="$tirName"/></xsl:if>
                </span>
            </xsl:when>
            <xsl:otherwise> &#160;<xsl:apply-templates
                    select="pas/dfp|partIdentSegment/descrForPart"/>
            </xsl:otherwise>
        </xsl:choose>-->
    </xsl:template>
    <!--===============================================-->
    <!-- Default Template matches-->
    <xsl:template
        match="csn | catalogSeqNumber | rfs | reasonForSelection | pas | dfp | pcs | qui | str | ftc | psc | cmk | icy | cbs | asp | nil | rtx | ippref | smf | mfm | dfl | ccs | applicabilitySegment | uce | ctl | srv | service | smr | sourceMaintRecoverability | rfd | ils | can ">
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
</xsl:stylesheet>
