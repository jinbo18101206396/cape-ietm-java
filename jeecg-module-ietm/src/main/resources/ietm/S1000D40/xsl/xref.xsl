<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <!-- ==============================此xsl已经修改完毕======================================= -->
    <xsl:strip-space elements="*"/>

    <xsl:template match="xref|internalRef" name="t_xref">
        <!-- print any pretext on the xref (if any)-->
        <span class="xrefText">
            <xsl:if test="@pretext and not(@pretext='')">
                <xsl:value-of select="@pretext"/>, </xsl:if>
            <!-- carry out xref processing-->
            <xsl:call-template name="xrefProcessing"/>
            <!-- only used in step mode -->
            <xsl:if test="ancestor::stepview">
                <xsl:variable name="XrefType">
                    <xsl:value-of
                        select="translate(@xidtype|@internalRefTargetType, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')"
                    />
                </xsl:variable>
                <xsl:variable name="XrefID">
                    <xsl:value-of select="@xrefid|@internalRefId"/>
                </xsl:variable>
                <xsl:call-template name="xrefStepLinkLabel">
                    <xsl:with-param name="vXrefType" select="$XrefType"/>
                    <xsl:with-param name="vXrefID" select="$XrefID"/>
                </xsl:call-template>
            </xsl:if>
            <xsl:apply-templates/><!-- output the text content of the xref -->
            <!-- print any post-text on the xref (if any)-->
            <xsl:if test="@posttext and not(@posttext='')"> ,<xsl:value-of select="@posttext"
            /></xsl:if>
        </span>
    </xsl:template>
    <!-- ===================================================================== -->
    <xsl:template name="xrefProcessing">
        <!--
        this template called from context xref will choose the type of xref we are doing and run the
        required templates
        -->
        <!--2012 <xsl:variable name="vXrefType">
            <xsl:value-of select="lower-case(@xidtype|@internalRefTargetType)"/>
        </xsl:variable>-->
        <xsl:variable name="vXrefType">
			<xsl:value-of select="translate(@xidtype|@internalRefTargetType, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')"/>
		</xsl:variable>
        <xsl:variable name="Xref_ID">
            <xsl:value-of select="translate(@xrefid|@internalRefId,'-','-')"/>
        </xsl:variable>

        <xsl:choose>
        	       <!-- the xref will not be handled if the xref id is empty.-->
            <xsl:when test="$Xref_ID =''">
                <xsl:call-template name="xrefLink">
                    <xsl:with-param name="vXrefType" select="$vXrefType"/>
                    <xsl:with-param name="Xref_ID" select="$Xref_ID"/>
                </xsl:call-template>
            </xsl:when>
            <!-- ALL xref types APART FROM hotspot and other-->
            <xsl:when test="not($vXrefType='hotspot' or $vXrefType='param' or $vXrefType='other')">
                <xsl:for-each select="ancestor::dmodule//*[@id][translate(@id,'-','-') = $Xref_ID]">
                    <xsl:call-template name="xrefLink">
                        <xsl:with-param name="vXrefType" select="$vXrefType"/>
                        <xsl:with-param name="Xref_ID" select="$Xref_ID"/>
                    </xsl:call-template>
                </xsl:for-each>
            </xsl:when>
            <xsl:when test="$vXrefType = 'hotspot'">
                <xsl:for-each
                    select="ancestor::dmodule//figure[descendant::hotspot[translate(@id,'-','-') = $Xref_ID]]">
                    <xsl:variable name="vXrefHotSpotLabel">
                        <xsl:value-of
                            select="descendant::hotspot[translate(@id,'-','-') = $Xref_ID]/@apsname|descendant::hotspot[translate(@id,'-','-') = $Xref_ID]/@applicationStructureName"
                        />, <xsl:value-of select="$v00354"/>&#160;<xsl:number level="any"
                            from="dmodule"/>
                    </xsl:variable>
                    <xsl:call-template name="xrefHotSpotLink">
                        <xsl:with-param name="vXrefType" select="$vXrefType"/>
                        <xsl:with-param name="Xref_ID" select="$Xref_ID"/>
                        <xsl:with-param name="vXrefHotSpotLabel" select="$vXrefHotSpotLabel"/>
                    </xsl:call-template>
                </xsl:for-each>
            </xsl:when>
            <xsl:when test="$vXrefType = 'param'">
            	<xsl:call-template name="xrefParamLink">
              	 	<xsl:with-param name="Xref_ID" select="$Xref_ID"/>
            	</xsl:call-template>
            </xsl:when>             
            <xsl:when
                test="@xidtype='OTHER' or @xidtype='other' or @internalRefTargetType='OTHER' or @internalRefTargetType='other'">
                <!--
                The Other type is NOT handled at the moment.
                -->
            </xsl:when>
        </xsl:choose>
    </xsl:template>
    <!-- ===================================================================== -->
     <xsl:template name="xrefParamLink">
     	<xsl:param name="Xref_ID"/>
		<xsl:variable name="boardno"><xsl:value-of select="//multimediaobject[param[translate(@id,'-','-')=$Xref_ID]]/@boardno"/><xsl:value-of select="//multimediaObject[parameter[translate(@id,'-','-')=$Xref_ID]]/@infoEntityIdent"/></xsl:variable> 
     	<xsl:variable name="paramid"><xsl:value-of select="//multimediaobject/param[translate(@id,'-','-')=$Xref_ID]/@paramid"/><xsl:value-of select="//multimediaObject/parameter[translate(@id,'-','-')=$Xref_ID]/@parameterIdent"/></xsl:variable>
     	<xsl:variable name="paramname"><xsl:value-of select="//multimediaobject/param[translate(@id,'-','-')=$Xref_ID]/@paramname"/><xsl:value-of select="//multimediaObject/parameter[translate(@id,'-','-')=$Xref_ID]/@parameterName"/></xsl:variable>
     	<xsl:variable name="paramvalue"><xsl:value-of select="//multimediaobject/param[translate(@id,'-','-')=$Xref_ID]/@paramvalue"/><xsl:value-of select="//multimediaObject/parameter[translate(@id,'-','-')=$Xref_ID]/@parameterValue"/></xsl:variable>
     		<span class="xrefLink">
     			  <a href="#">
     			<xsl:attribute name="onclick">
     				linkToParam('<xsl:value-of select="$boardno"/>', 
      													'<xsl:value-of select="$ISOViewInstalled"/>',
      													'<xsl:value-of select="$Publication"/>',
      													'<xsl:value-of select="$paramid"/>',
      													'<xsl:value-of select="$paramname"/>',
      													'<xsl:value-of select="$paramvalue"/>',
      													'<xsl:value-of select="$v02404"/>');
      													displayISOLegend(isoLegendDiv<xsl:value-of select="generate-id(.)"/>,'mainForm:legendDiv');</xsl:attribute>
     			Param
     			</a>
     			<span>
			<xsl:attribute name="id">isoLegendDiv<xsl:value-of select="generate-id(.)"/></xsl:attribute>
				<!--2012xsl:attribute name="style">display:none</xsl:attribute>-->
			<xsl:call-template name="ParamLegendContent"><xsl:with-param name="boardno" select="$boardno"/></xsl:call-template>
			</span>
     		</span>
     		
     		
     </xsl:template>  
     <!-- ===================================================================== -->
     
    <xsl:template name="xrefHotSpotLink">
        <!--
        this template will open find the image associated with the hotspot, open it, and if its a cgm
        mark the hotspot
        -->
        <xsl:param name="vXrefType"/>
        <xsl:param name="vXrefHotSpotLabel"/>
        <xsl:param name="Xref_ID"/>
        <span class="xrefLink">
            <xsl:variable name="params">
                <xsl:call-template name="getAllHotspotsApsname">
                    <xsl:with-param name="Xref_ID" select="$Xref_ID"/>
                </xsl:call-template>
            </xsl:variable>

            <!-- sometimes the author will use the apsid other time he uses the apsname -->
            <!-- These spans enable enable use to link back to the page from a cgm hotspot -->
            <span>
                <xsl:attribute name="id">
                    <xsl:value-of
                        select=".//hotspot[@id = $Xref_ID]/@apsid|.//hotspot[@id = $Xref_ID]/@applicationStructureIdent"
                    />
                </xsl:attribute> &#32; </span>
            <span>
                <xsl:attribute name="id">
                    <xsl:value-of
                        select=".//hotspot[@id = $Xref_ID]/@apsname|.//hotspot[@id = $Xref_ID]/@applicationStructureName"
                    />
                </xsl:attribute> &#32; </span>
            <a href="#">
                <xsl:attribute name="id">
                    <xsl:value-of
                        select=".//hotspot[@id = $Xref_ID]/@apsid|.//hotspot[@id = $Xref_ID]/@applicationStructureIdent"
                    />
                </xsl:attribute>
                <!-- if there is an apsid user that, else use the apsname-->
                <xsl:choose>
                    <xsl:when test=".//hotspot[@id = $Xref_ID]/@apsid">
                        <xsl:attribute name="onclick">updateLegendDiv('<xsl:call-template
                                name="figureCount"/>', 'mainForm:legendDiv');
                                linkToHotSpot('<xsl:value-of select=".//graphic[.//hotspot[translate(@id,'-','-') = $Xref_ID]]/@boardno|.//graphic[.//hotspot[translate(@id,'-','-') = $Xref_ID]]/@infoEntityIdent"/>', '<xsl:value-of select="$ISOViewInstalled"/>','<xsl:value-of select="$Publication"/>' , <xsl:value-of select="$params"/>);</xsl:attribute>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:attribute name="onclick">updateLegendDiv('<xsl:call-template
                                name="figureCount"/>', 'mainForm:legendDiv');
                                linkToHotSpot('<xsl:value-of
                                select=".//graphic[.//hotspot[translate(@id,'-','-') = $Xref_ID]]/@boardno|.//graphic[.//hotspot[translate(@id,'-','-') = $Xref_ID]]/@infoEntityIdent"
                            />', '<xsl:value-of select="$ISOViewInstalled"/>','<xsl:value-of
                                select="$Publication"/>' , <xsl:value-of select="$params"
                        />)</xsl:attribute>
                    </xsl:otherwise>
                </xsl:choose>
                <xsl:call-template name="xrefLinkLabel">
                    <xsl:with-param name="vXrefType" select="$vXrefType"/>
                    <xsl:with-param name="vXrefHotSpotLabel" select="$vXrefHotSpotLabel"/>
                    <xsl:with-param name="Xref_ID" select="$Xref_ID"/>
                </xsl:call-template>
            </a>
        </span>
    </xsl:template>
    <!-- ===================================================================== -->
    <!--2012 <xsl:template name="xrefLink">
        This template will make the anchor link and call the label template to display
        at this temlate you will be at the context of the target you are targetting 
        <xsl:param name="vXrefType"/>
        <xsl:param name="vXrefHotSpotLabel"/>
        <xsl:param name="Xref_ID"/>
        <xsl:variable name="refID">
            <xsl:value-of select="translate($Xref_ID,'_','-')"/>
        </xsl:variable>
        <span class="xrefLink">
            <a href="#">
           		<xsl:if test="$vXrefType='supply' or $vXrefType='spares' or $vXrefType='supequip'">
           			 <xsl:attribute name="onmouseover">fL.removeAndAddListeners(this, "<xsl:value-of
                           select="$v00583"/>", "<xsl:value-of
                           select="//node()[@id=$refID]/nomen | //node()[@id=$refID]/name"/>",
                           "<xsl:value-of select="$v00555"/>", "<xsl:value-of
                           select="//node()[@id=$refID]/nsn/@nsn | //node()[@id=$refID]/natoStockNumber"
                       />", "<xsl:value-of select="$v00646"/>", "<xsl:value-of
                           select="//node()[@id=$refID]/identno/pnr | //node()[@id=$refID]/identNumber/partAndSerialNumber/partNumber"
                       />", "<xsl:value-of select="$v00526"/>", "<xsl:value-of
                           select="//node()[@id=$refID]/identno/mfc | //node()[@id=$refID]/identNumber/manufacturerCode"
                       />", "<xsl:value-of select="$v00092"/>", "<xsl:value-of
                           select="//node()[@id=$refID]/csnref/@refcsn | //node()[@id=$refID]/catalogSeqNumberRef/@catalogSeqNumberValue"
                       />", "<xsl:value-of select="$v00744"/>", "<xsl:value-of
                           select="//node()[@id=$refID]/qty | //node()[@id=$refID]/reqQuantity"/>",
                           "<xsl:value-of select="$v01000"/>", "<xsl:value-of
                           select="//node()[@id=$refID]/qty/@uom | //node()[@id=$refID]/reqQuantity/@unitOfMeasure"
                       />")</xsl:attribute>
                	<xsl:attribute name="class">preReqsLink</xsl:attribute>
				</xsl:if>
                <xsl:attribute name="onclick">JumpToRow("<xsl:value-of select="$Xref_ID"/>")</xsl:attribute>
                <xsl:call-template name="xrefLinkLabel">
                    <xsl:with-param name="vXrefType" select="$vXrefType"/>
                    <xsl:with-param name="vXrefHotSpotLabel" select="$vXrefHotSpotLabel"/>
                </xsl:call-template>
            </a>
        </span>
    </xsl:template>-->
    <xsl:template name="xrefLink">
		<!-- This template will make the anchor link and call the label template to display -->
		<!-- at this temlate you will be at the context of the target you are targetting -->
		<xsl:param name="vXrefType"/>
		<xsl:param name="vXrefHotSpotLabel"/>
		<xsl:param name="Xref_ID"/>
        <xsl:variable name="refID">
            <xsl:value-of select="translate($Xref_ID,'-','-')"/>
        </xsl:variable>	
		<!--2012文字链接图片-->
      <span class="xrefLink" style="cursor:pointer;">
      <!--<xsl:attribute name="id"><xsl:value-of select="$Xref_ID"/></xsl:attribute>-->
			<!----> 
		  <xsl:attribute name="onclick">getPos("<xsl:value-of select="$Xref_ID"/>")</xsl:attribute>
			<!--<a href="#">-->
			
			<xsl:call-template name="xrefLinkLabel">
					<xsl:with-param name="vXrefType" select="$vXrefType"/>
					<xsl:with-param name="vXrefHotSpotLabel" select="$vXrefHotSpotLabel"/>
					<xsl:with-param name="Xref_ID" select="$Xref_ID"/>
				</xsl:call-template>
			<!--</a>-->
			<!--
			<script>
			   document.getElementById("<xsl:value-of select="$Xref_ID"/>"). = alter("<xsl:value-of select="$vXrefType"/>");
			</script>-->
		</span>
		
		
	</xsl:template>
    <!-- ================================定义了各种类型的转到某一行的标签===================================== -->
    <xsl:template name="xrefLinkLabel">
		<!-- this will print the xref label out -->
		<xsl:param name="vXrefType"/>
		<xsl:param name="vXrefHotSpotLabel"/>
		<xsl:param name="Xref_ID"/>
		<xsl:variable name="multimediaNumb"><xsl:number level="any"/></xsl:variable>
		
		<!-- STILL TO DO HERE ARE MULTIMEDIA, HOTSPOTS, PARAM and OTHER-->
		<xsl:choose>
			<xsl:when test="$vXrefType='figure'"><!--<xsl:value-of select="$v00354"/>&#160;<xsl:value-of select="//figure[@id=$Xref_ID]/title"/>   -->
				图&#160;<xsl:value-of select="$multimediaNumb"/>        
			</xsl:when>
			<xsl:when test="$vXrefType='sheet'">
				<xsl:value-of select="$v00354"/>
				<xsl:value-of select="@count"/>
				<xsl:number count="figure" level="any" from="dmodule"/>
				<xsl:value-of select="v00860"/>. <xsl:number level="any" from="figure"/>
			</xsl:when>
			<xsl:when test="$vXrefType='para'">
			<!-- title attribute of link -->
			<xsl:attribute name="title"><xsl:value-of select="$v00501"/></xsl:attribute>
				<!--<xsl:value-of select="$v00639"/>-->段落.
					<xsl:choose>
					<xsl:when test="parent::specpara">
						<xsl:value-of select="../../@count"/>
					</xsl:when>
					<xsl:when test="parent::step1 or parent::step2 or parent::step3 or parent::step4 or parent::step5">
						<xsl:value-of select="../@count"/>
					</xsl:when>
					<xsl:when test="self::levelledPara">
                        <xsl:number count="levelledPara" from="content" level="multiple" format="1"/>
                    </xsl:when>
                    <xsl:when test="parent::levelledPara and not(self::levelledPara)">
                        <xsl:number count="levelledPara" from="content" level="multiple" format="1"/>
                    </xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="@count"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:when>
			<!-- Dot removed as table is not an abbreviation -->
			<!--DMC-601S0000-A-34-00-00-00A-030A-D_001.xml-->
			<xsl:when test="$vXrefType='table'">
				<xsl:attribute name="title"><xsl:value-of select="$v00503"/></xsl:attribute><!--<xsl:value-of select="$v00907"/>&#160;<xsl:value-of select="//table[@id=$Xref_ID]/title"/>-->
				表&#160;<xsl:value-of select="$multimediaNumb"/>
			</xsl:when>
			<!--<xsl:when test="$vXrefType='table'">
				<Dot removed as table is not an abbreviation 
				<xsl:attribute name="title"><xsl:value-of select="$v00503"/></xsl:attribute>
				<xsl:value-of select="$v00907"/>&#160;<xsl:value-of select="@count"/>
			</xsl:when>-->
			<xsl:when test="$vXrefType='step'">
			<xsl:attribute name="title"><xsl:value-of select="$v00502"/></xsl:attribute>
				<xsl:value-of select="$v00878"/>&#160;<xsl:value-of select="@count"/>
			</xsl:when>
			<!-- We match on both 'supequip' or 'supeqiup' due to a typo in the S1000D spec. -->
			<xsl:when test="$vXrefType='supply' or $vXrefType='spares' or $vXrefType='supequip' or $vXrefType='supeqiup'">
				<xsl:value-of select="./nomen"/>
				<xsl:choose>
					<!-- when you have a csnref show it only if there is no part number -->
					<xsl:when test="csnref and not(./identno/pnr)">
								(CSNRef. 							
								<xsl:call-template name="T_csnref">
							<xsl:with-param name="CSN" select="./csnref/@refcsn"/>
						</xsl:call-template>
						<xsl:if test="./csnref/@refisn">/<xsl:value-of select="./csnref/@refisn"/>
						</xsl:if>)</xsl:when>
					<xsl:when test="nsn and not(./identno/pnr and nsn)">
									(NSN. 
									<xsl:call-template name="T_nsn">
							<xsl:with-param name="NSN_Temp" select="./nsn/@nsn"/>
						</xsl:call-template>)</xsl:when>
					<xsl:otherwise>
								(Pt. No. <xsl:value-of select="./identno/pnr"/>)</xsl:otherwise>
				</xsl:choose>
			</xsl:when>
			<xsl:when test="$vXrefType='hotspot'">
				<xsl:value-of select="$vXrefHotSpotLabel"/>
			</xsl:when>
			<!--2012 定义了多媒体的标题标签-->
			<xsl:when test="$vXrefType='multimediaobject'">
				<!--2012<xsl:value-of select="$v00548"/>-<xsl:value-of select="count(..)"/>&#160;<xsl:value-of select="../title"/>-->
				见<xsl:value-of select="$v00548"/>&#160;<xsl:value-of select="$multimediaNumb"/>
			</xsl:when>
			<!--2012 定义了多媒体的标题标签-->
			<xsl:when test="$vXrefType='multimedia'"> 
				见<xsl:value-of select="$v00548"/>&#160;<xsl:value-of select="$multimediaNumb"/>
			</xsl:when>			
           <xsl:otherwise>				
            XREF <xsl:value-of select="@xidtype"/> NOT YET HANDLED  
			</xsl:otherwise>
		</xsl:choose>
		
	</xsl:template>
    <!-- ===================================================================== -->
    <xsl:template name="getAllHotspotsApsname">
        <xsl:param name="Xref_ID"/>
        	<xsl:for-each select="//hotspot[translate(@id,'-','-') = $Xref_ID]">'<xsl:value-of select="@apsname|@applicationStructureName"/>'<xsl:if test="hotspot">,</xsl:if><xsl:for-each select="hotspot">'<xsl:value-of select="@apsname|@applicationStructureName"/>'<xsl:if test="following-sibling::hotspot">,</xsl:if></xsl:for-each>
        </xsl:for-each>
    </xsl:template>
    <!-- ===================================================================== -->
    <xsl:template match="hotspot/xref">
        <xsl:variable name="vXrefId">
            <xsl:value-of select="@xrefid"/>
        </xsl:variable>
      <script type="text/javascript"> addLink ( '<xsl:value-of select="ancestor::graphic/(@boardno|@infoEntityIdent)"/><xsl:value-of select="../@apsid|../@applicationStructureIdent"/>', new
            XREFLink( "JumpToRow('<xsl:value-of select="$vXrefId"/>') ") );</script>
    </xsl:template>
    <!-- ===================================================================== -->
    <!--
		Matches on an hotspot xref element which is of type hotspot.
		Creates a javascript function object with a string which evaluates to a call to
		the linkToHotspot function with the appropriate parameters.
	-->
    <xsl:template
        match="hotspot/xref[@xidtype='hotspot']|hotspot/internalRef[@internalRefTargetType='hotspot']">
        <xsl:variable name="vXrefId">
            <xsl:value-of select="@xrefid"/>
        </xsl:variable>
        <xsl:variable name="params">
            <xsl:call-template name="getAllHotspotsApsname">
                <xsl:with-param name="Xref_ID" select="$vXrefId"/>
            </xsl:call-template>
        </xsl:variable>
        <script type="text/javascript">
         addLink ( '<xsl:value-of select="ancestor::graphic/@boardno|ancestor::graphic/@infoEntityIdent"/><xsl:value-of select="../@apsid"/>', new XREFLink( "linkToHotSpot( '<xsl:value-of select="//graphic[.//hotspot[translate(@id,'-','-') = $vXrefId]]/(@boardno|@infoEntityIdent)"/>' ,'<xsl:value-of select="$ISOViewInstalled"/>','<xsl:value-of select="$Publication"/>', <xsl:value-of select="$params"/> )" ) ); 
        </script>
    </xsl:template>
    <!-- ===================================================================== -->
    <xsl:template name="xrefStepLinkLabel">
        <!-- this will print the xref label out during step mode -->
        <xsl:param name="vXrefType"/>
        <xsl:param name="vXrefID"/>
        <xsl:if test="$vXrefType='figure'"><xsl:value-of select="$v00354"/>&#160;<xsl:value-of
                select="@xrefid"/></xsl:if>
        <xsl:if test="$vXrefType='table'"><xsl:value-of select="$v00907"/>&#160;<xsl:value-of
                select="@xrefid"/></xsl:if>
        <xsl:for-each select="../../../supeqli/supequi/@id">
            <xsl:if test="$vXrefID = .">
                <xsl:value-of select="../nomen"/>&#160;(Pt. No. <xsl:value-of
                    select="../identno/pnr"/>)</xsl:if>
        </xsl:for-each>
        <xsl:for-each select="../../../../supeqli/supequi/@id">
            <xsl:if test="$vXrefID = .">
                <xsl:value-of select="../nomen"/>&#160;(Pt. No. <xsl:value-of
                    select="../identno/pnr"/>)</xsl:if>
        </xsl:for-each>
        <xsl:for-each select="../../../supplyli/supply/@id">
            <xsl:if test="$vXrefID = .">
                <xsl:value-of select="../nomen"/>&#160;(Pt. No. <xsl:value-of
                    select="../identno/pnr"/>)</xsl:if>
        </xsl:for-each>
        <xsl:for-each select="../../../../supplyli/supply/@id">
            <xsl:if test="$vXrefID = .">
                <xsl:value-of select="../nomen"/>&#160;(Pt. No. <xsl:value-of
                    select="../identno/pnr"/>)</xsl:if>
        </xsl:for-each>
        <xsl:for-each select="../../../sparesli/spare/@id">
            <xsl:if test="$vXrefID = .">
                <xsl:value-of select="../nomen"/>&#160;(Pt. No. <xsl:value-of
                    select="../identno/pnr"/>)</xsl:if>
        </xsl:for-each>
        <xsl:for-each select="../../../../sparesli/spare/@id">
            <xsl:if test="$vXrefID = .">
                <xsl:value-of select="../nomen"/>&#160;(Pt. No. <xsl:value-of
                    select="../identno/pnr"/>)</xsl:if>
        </xsl:for-each>
    </xsl:template>
    <!-- ===================================================================== -->
    <xsl:template name="figureCount">
        <xsl:value-of select="@count"/>
    </xsl:template>

</xsl:stylesheet>
