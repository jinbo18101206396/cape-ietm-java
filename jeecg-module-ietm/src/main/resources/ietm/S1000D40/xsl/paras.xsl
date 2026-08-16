<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<!--===============================================-->
	<xsl:template match="para0 | subpara1 | subpara2 | subpara3 | subpara4 | subpara5 | subpara6 | subpara7">
		<!-- make a div for the dmtoc link-->
		<xsl:call-template name="T_makeDMTocLink"/>
		<div>
			<xsl:variable name="stylingClass">
				<xsl:value-of select="name(.)"/>
			</xsl:variable>
			<xsl:attribute name="class"><xsl:value-of select="$stylingClass"/></xsl:attribute>
			<xsl:call-template name="t_changeMarker">
				<!-- the existing style class for the row is passed in to the change marker template  so that change marker template can 
                    just append its own style calss, rather than overwrite it  -->
				<xsl:with-param name="existingClasses">
					<xsl:value-of select="$stylingClass"/>
				</xsl:with-param>
			</xsl:call-template>
			<!-- xref anchor-->
			<xsl:call-template name="createLinkAnchor"/>
			<xsl:apply-templates/>
		</div>
	</xsl:template>
	<!--===============================================-->
	<!--
        2-3 spec element. Unknown output, so only apply templates.
    -->
	<xsl:template match="para/parsingdara">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<!-- 
        4-0 Levelled para handling.
    -->
	<xsl:template match="levelledPara">
		<!--<xsl:call-template name="T_makeDMTocLink"/>-->
		<div>
			<xsl:attribute name="id"><xsl:value-of select="translate(@id,'-','-')"/></xsl:attribute>
			<xsl:call-template name="t_changeMarker"/>
			<xsl:call-template name="t_dereference_wcn"/>
			<xsl:apply-templates/>
		</div>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="levelledPara/title">
		<p>
		<xsl:attribute name="id"><xsl:value-of select="generate-id(..)"/></xsl:attribute>
			<xsl:attribute name="class"><xsl:call-template name="t_getTitleCss"><xsl:with-param name="depth"><xsl:value-of select="count(ancestor-or-self::levelledPara)"/></xsl:with-param></xsl:call-template></xsl:attribute>
			<!--<xsl:value-of select="../@ptc_level"/>-->
			<xsl:number count="levelledPara" from="content" level="multiple" format="1.1.1.1.1"/>
			<span class="paraNumberTitleIndent">
				<xsl:apply-templates/>
			</span>
		</p>
	</xsl:template>
	<!--2012 add start 添加段落样式-->
	<xsl:template match="levelledPara/para | content//para">
		<!--<xsl:attribute name="class">
			<xsl:call-template name="t_getParaCss">
			<xsl:with-param name="depth">
			<xsl:value-of select="count(ancestor-or-self::levelledPara)"/>
			</xsl:with-param>
			</xsl:call-template>
			<xsl:text> paraXPadding</xsl:text>
			</xsl:attribute>
			<xsl:apply-templates/>
			   <xsl:value-of select="../@ptc_level"/>
			<span class="paraNumberTitleIndent">
				<xsl:apply-templates/>
			</span>-->
		<xsl:choose>
			<xsl:when test="name(.) ='para' and not(child::sequentialList or child::seqlist or child::randomList) and not(parent::listItem or child::item)  and not(parent::entry or parent::crewDrillStep)">
				<p>
					<xsl:element name="span">
						<xsl:variable name="stylingClass">paramargin</xsl:variable>
						<xsl:attribute name="class"><xsl:value-of select="$stylingClass"/></xsl:attribute>
						<xsl:call-template name="t_changeMarker">
							<xsl:with-param name="existingClasses">
								<xsl:value-of select="$stylingClass"/>
							</xsl:with-param>
						</xsl:call-template>
						<xsl:call-template name="t_inlineApplicability">
							<xsl:with-param name="annotation" select="$v00034"/>
						</xsl:call-template>
						<xsl:apply-templates/>
					</xsl:element>
				</p>
			</xsl:when>
			<xsl:when test="name(.) ='para' and (child::sequentialList or child::randomList) and (parent::description or ancestor::description)">
			<div style="text-indent:2em;font-size:10.5pt;line-height:2;">
			<xsl:apply-templates/>
			</div>
			</xsl:when>
      <xsl:when test="(name(.) ='para' and not(preceding-sibling::para)) and (parent::crewDrillStep) and
								not(preceding-sibling::challeng or preceding-sibling::challenge or preceding-sibling::procd)">
                  <p>
                  <div>
                  <xsl:attribute name="style">margin-left: <xsl:value-of select="count(ancestor::crewDrillStep)*2"/>em;</xsl:attribute>
                  <xsl:attribute name="class">paramargin</xsl:attribute>
                  <xsl:call-template name="t_inlineApplicability">
							  <xsl:with-param name="annotation" select="$v00034"/>
						     </xsl:call-template>
									<xsl:variable name="stepLevel">
										<xsl:value-of select="count(ancestor::crewDrillStep)"/>
									</xsl:variable>
									<xsl:choose>
										<xsl:when test="$stepLevel=1">
											<xsl:number count="crewDrillStep" format="(1)"/>
										</xsl:when>
										<xsl:when test="$stepLevel=2">
											<xsl:number count="crewDrillStep" format="a."/>
										</xsl:when>
										<xsl:when test="$stepLevel=3">
											<xsl:number count="crewDrillStep" format="(a)"/>
										</xsl:when>
										<xsl:when test="$stepLevel=4">
											<xsl:number count="crewDrillStep" format="1)"/>
										</xsl:when>
										<xsl:when test="$stepLevel=5">
											<xsl:number count="crewDrillStep" format="a)"/>
										</xsl:when>
									</xsl:choose>
                    <text>
                      &#160;
                     </text>
									<xsl:apply-templates/>
                 </div>
                 </p>
       </xsl:when>
       <xsl:when test="(name(.) ='para' and  (preceding-sibling::para)) and (parent::crewDrillStep) and
								not(preceding-sibling::challeng or preceding-sibling::challenge or preceding-sibling::procd)">
                  <p>
                  <div>
                  <xsl:attribute name="style">margin-left: <xsl:value-of select="count(ancestor::crewDrillStep)*2"/>em;</xsl:attribute>
                  <xsl:attribute name="class">paramargin</xsl:attribute>
                  <xsl:call-template name="t_inlineApplicability">
							   <xsl:with-param name="annotation" select="$v00034"/>
						    </xsl:call-template>
									<xsl:apply-templates/>
                 </div>
                 </p>
       </xsl:when>
       <!--<xsl:when test="(name(.) ='para' and  (preceding-sibling::para)) and not(position()=1) and (parent::proceduralStep)">
         <table style="table-layout:fixed;word-wrap:break-word;width:100%;">
           <tbody>
             <tr>
               <td width="5%" style="word-break:keep-all;">
               </td>
               <td width="95%">
                 <span class="stepNumber" style="width:100%;display:block;float:left;">
                   <xsl:apply-templates/>
                 </span>
               </td>
             </tr>
           </tbody>
         </table>
       </xsl:when>-->
			<xsl:otherwise>
				<xsl:apply-templates/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<!--编号列表中的大于第二段落-->
	<xsl:template match="listItem/para | item/para">
		<xsl:choose>
      <!--and not(child::sequentialList) and not(child::randomList)   name(.) ='para' and not(position()=1)-->
			<xsl:when test="(name(.) ='para' and  (preceding-sibling::para)) and (ancestor::sequentialList or ancestor::randomList)">
        <p>
          <div>
            <!--text-indent:2em;-->
            <!--<xsl:value-of select="count(ancestor::sequentialList | ancestor::randomList)*2"/>-->
            <xsl:attribute name="style">
              text-indent:2em;
            </xsl:attribute>
            <!--<xsl:attribute name="class">seqItem</xsl:attribute>-->
            <xsl:call-template name="t_inlineApplicability">
              <xsl:with-param name="annotation" select="$v00034"/>
            </xsl:call-template>
            <xsl:apply-templates/>
          </div>
        </p>
			</xsl:when>
			<!--<xsl:when test="name(.) ='para' and (position()>1)">
				<xsl:element name="span">
					
					<xsl:attribute name="class">paramargin</xsl:attribute>
					<xsl:apply-templates/>
				</xsl:element>
			</xsl:when>-->
			<xsl:otherwise>
				<xsl:apply-templates/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<xsl:template name="t_getParaCss">
		<xsl:param name="depth">0</xsl:param>
		<xsl:choose>
			<xsl:when test="$depth = '1'">paramargin</xsl:when>
			<xsl:otherwise>paramargin</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<!--end-->
	<!--===============================================-->
	<xsl:template name="t_getIndenture">
		<xsl:param name="depth">0</xsl:param>
		<xsl:choose>
			<xsl:when test="$depth = '1'"/>
			<xsl:otherwise>subpara1</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<!--===============================================-->
	<xsl:template name="t_getTitleCss">
		<xsl:param name="depth">0</xsl:param>
		<xsl:choose>
			<xsl:when test="$depth = '1'">para0Title</xsl:when>
			<xsl:when test="$depth = '2'">subpara1Title</xsl:when>
			<xsl:when test="$depth = '3'">subpara2Title</xsl:when>
			<xsl:when test="$depth = '4'">subpara3Title</xsl:when>
			<xsl:when test="$depth = '5'">subpara4Title</xsl:when>
			<xsl:otherwise>subpara4Title</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<!--===============================================-->
</xsl:stylesheet>
