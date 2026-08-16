<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<!--===============================================-->
	<xsl:strip-space elements="*"/>
	<!--===============================================-->
	<xsl:template match="step1 | step2 | step3 | step4 | step5 | step6 | step7 | step8 | proceduralStep">
		<!-- make a div for the dmtoc link-->
		<xsl:call-template name="t_inlineApplicability">
			<xsl:with-param name="annotation">
				<xsl:value-of select="$v00034"/>:&#160;</xsl:with-param>
		</xsl:call-template>
		<xsl:call-template name="T_makeDMTocLink"/>
		<div class="stepArea">
			<!-- xref anchor-->
			<xsl:call-template name="createLinkAnchor"/>
			<span>
				<!-- change marking -->
				<xsl:call-template name="t_changeMarker"/>
				<!-- content -->
				<!-- Output for wanrings and caution refs. 17/2/11 MC -->
				<xsl:if test="@warningRefs|@cautionRefs">
					<xsl:call-template name="t_dereference_wcn"/>
				</xsl:if>
				<xsl:apply-templates/>
			</span>
		</div>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="proceduralStep[count(ancestor-or-self::proceduralStep) &gt; 7]">
		<!-- make a div for the dmtoc link-->
		<xsl:call-template name="t_inlineApplicability">
			<xsl:with-param name="annotation">
				<xsl:value-of select="$v00034"/>:&#160;</xsl:with-param>
		</xsl:call-template>
		<xsl:call-template name="T_makeDMTocLink"/>
		<div style="padding-top: 2px; padding-bottom: 2px">
			<!-- xref anchor-->
			<xsl:call-template name="createLinkAnchor"/>
			<xsl:apply-templates/>
		</div>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="						step1[*[not(preceding-sibling::*)][not(name()='title' or name() = 'para' or name() = 'warning' or name() = 'caution' or name() = 'note' or name() = 'applic' or name() = 'specpara')]] |
									step2[*[not(preceding-sibling::*)][not(name()='title' or name() = 'para' or name() = 'warning' or name() = 'caution' or name() = 'note' or name() = 'applic' or name() = 'specpara')]] |
									step3[*[not(preceding-sibling::*)][not(name()='title' or name() = 'para' or name() = 'warning' or name() = 'caution' or name() = 'note' or name() = 'applic' or name() = 'specpara')]] |
									step4[*[not(preceding-sibling::*)][not(name()='title' or name() = 'para' or name() = 'warning' or name() = 'caution' or name() = 'note' or name() = 'applic' or name() = 'specpara')]] |
									step5[*[not(preceding-sibling::*)][not(name()='title' or name() = 'para' or name() = 'warning' or name() = 'caution' or name() = 'note' or name() = 'applic' or name() = 'specpara')]] |
									step6[*[not(preceding-sibling::*)][not(name()='title' or name() = 'para' or name() = 'warning' or name() = 'caution' or name() = 'note' or name() = 'applic' or name() = 'specpara')]] |
									step7[*[not(preceding-sibling::*)][not(name()='title' or name() = 'para' or name() = 'warning' or name() = 'caution' or name() = 'note' or name() = 'applic' or name() = 'specpara')]] |
									step8[*[not(preceding-sibling::*)][not(name()='title' or name() = 'para' or name() = 'warning' or name() = 'caution' or name() = 'note' or name() = 'applic' or name() = 'specpara')]] ">
		<xsl:call-template name="t_inlineApplicability">
			<xsl:with-param name="annotation">
				<xsl:value-of select="$v00034"/>:&#160;</xsl:with-param>
		</xsl:call-template>
		<!-- number a stepX where the first child is stepX + 1-->
		<div>
			<xsl:value-of select="@count"/>.</div>
		<div class="stepArea">
			<xsl:apply-templates/>
		</div>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="step1/warning[not(preceding-sibling::*)] | step1/caution[not(preceding-sibling::*)] | step1/note[not(preceding-sibling::*)] | step1/specpara[not(preceding-sibling::*)] |
									step2/warning[not(preceding-sibling::*)] | step2/caution[not(preceding-sibling::*)] | step2/note[not(preceding-sibling::*)] | step2/specpara[not(preceding-sibling::*)] |
									step3/warning[not(preceding-sibling::*)] | step3/caution[not(preceding-sibling::*)] | step3/note[not(preceding-sibling::*)] | step3/specpara[not(preceding-sibling::*)] |
									step4/warning[not(preceding-sibling::*)] | step4/caution[not(preceding-sibling::*)] | step4/note[not(preceding-sibling::*)] | step4/specpara[not(preceding-sibling::*)] |
									step5/warning[not(preceding-sibling::*)] | step5/caution[not(preceding-sibling::*)] | step5/note[not(preceding-sibling::*)] | step5/specpara[not(preceding-sibling::*)] |
									step6/warning[not(preceding-sibling::*)] | step6/caution[not(preceding-sibling::*)] | step6/note[not(preceding-sibling::*)] | step6/specpara[not(preceding-sibling::*)] |
									step7/warning[not(preceding-sibling::*)] | step7/caution[not(preceding-sibling::*)] | step7/note[not(preceding-sibling::*)] | step7/specpara[not(preceding-sibling::*)] |
									step8/warning[not(preceding-sibling::*)] | step8/caution[not(preceding-sibling::*)] | step8/note[not(preceding-sibling::*)] | step8/specpara[not(preceding-sibling::*)]">
		<xsl:if test="name()= 'warning'">
			<xsl:call-template name="t_warning"/>
		</xsl:if>
		<xsl:if test="name()= 'caution'">
			<xsl:call-template name="t_caution"/>
		</xsl:if>
		<xsl:if test="name()= 'note'">
			<xsl:call-template name="t_note"/>
		</xsl:if>
		<xsl:if test="name()= 'specpara'">
			<xsl:apply-templates/>
		</xsl:if>
		<xsl:if test="not(following-sibling::*) and ../@stepcount">
			<div class="singlestepNumber">
				<xsl:value-of select="$v00878"/>&#160;<xsl:value-of select="../@stepcount"/>. </div>
		</xsl:if>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="step1/para[1][not(preceding-sibling::title or preceding-sibling::step2)] |
									step2/para[1][not(preceding-sibling::title or preceding-sibling::step3)] |
									step3/para[1][not(preceding-sibling::title or preceding-sibling::step4)] |
									step4/para[1][not(preceding-sibling::title or preceding-sibling::step5)] |
									step5/para[1][not(preceding-sibling::title or preceding-sibling::step6)] |
									step6/para[1][not(preceding-sibling::title or preceding-sibling::step7)] |
									step7/para[1][not(preceding-sibling::title or preceding-sibling::step8)] |
									step8/para[1][not(preceding-sibling::title)]" priority="3">
		<!-- number the first para -->
		<span>
			<xsl:if test="(../@mark='1' and not(../@level)) or ../@changeMark='1'">
				<xsl:attribute name="class">changeMarker</xsl:attribute>
			</xsl:if>
			<xsl:choose>
				<xsl:when test="../@stepcount">
					<div class="singlestepNumber">
						<xsl:value-of select="$v00878"/>&#160;<xsl:value-of select="../@stepcount"/>. </div>
				</xsl:when>
				<xsl:otherwise>
					<xsl:choose>
						<xsl:when test="count(ancestor-or-self::proceduralStep) &gt; 7"> </xsl:when>
					</xsl:choose>
					<span class="stepNumber">
						<xsl:value-of select="../@count"/>. </span>
				</xsl:otherwise>
			</xsl:choose>
			<span>
				<xsl:call-template name="t_changeMarker"/>
				<xsl:apply-templates/>
			</span>
		</span>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="step1/specpara[1]/para[1][not(preceding-sibling::title or preceding-sibling::step2)] |
    					step2/specpara[1]/para[1][not(preceding-sibling::title or preceding-sibling::step3)] |
    					step3/specpara[1]/para[1][not(preceding-sibling::title or preceding-sibling::step4)] |
    					step4/specpara[1]/para[1][not(preceding-sibling::title or preceding-sibling::step5)] |
    					step5/specpara[1]/para[1][not(preceding-sibling::title or preceding-sibling::step6)] |
    					step6/specpara[1]/para[1][not(preceding-sibling::title or preceding-sibling::step7)] |
    					step7/specpara[1]/para[1][not(preceding-sibling::title or preceding-sibling::step8)] |
    					step8/specpara[1]/para[1][not(preceding-sibling::title)]" priority="3">
		<xsl:choose>
			<xsl:when test="../../@stepcount">
				<div class="singlestepNumber">
					<xsl:value-of select="$v00878"/>&#160;<xsl:value-of select="../@stepcount"/>.
                </div>
			</xsl:when>
			<xsl:otherwise>
				<span class="stepNumber">
					<xsl:value-of select="../../@count"/>. </span>
			</xsl:otherwise>
		</xsl:choose>
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="stepview">
		<!--
			The stepview tag is a generated placeholder tag for single step mode.
			We use the include the dmview class, so each single step looks the
			same as the steps in the full data module.
		-->
		<div class="dmview">
			<xsl:apply-templates/>
		</div>
		<xsl:call-template name="importCSS"/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="stepref">
		<!--
			The stepref tag is a generated ref tag for single step mode.
		-->
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="step1/title | step2/title | step3/title |step4/title | step5/title | step6/title | step7/title | step8/title">
		<xsl:choose>
			<xsl:when test="../@stepcount">
				<!--this is when you are step mode -->
				<div class="singlestepNumber"> (<xsl:value-of select="$v00878"/>&#160;<xsl:value-of select="../@stepcount"/>.&#160;<xsl:apply-templates/>
				</div>
			</xsl:when>
			<xsl:otherwise>
				<div class="stepTitle">
					<span class="stepNumber">
						<xsl:value-of select="../@count"/>. </span>
					<xsl:apply-templates/>
				</div>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="step1/specpara[1]/title | step2/specpara[1]/title | step3/specpara[1]/title |step4/specpara[1]/title | step5/specpara[1]/title | step6/specpara[1]/title | step7/specpara[1]/title | step8/specpara[1]/title">
		<xsl:choose>
			<xsl:when test="../../@stepcount">
				<!--this is when you are step mode -->
				<div class="singlestepNumber"> (<xsl:value-of select="$v00878"/>&#160;<xsl:value-of select="../../@stepcount"/>.&#160;<xsl:apply-templates/>
				</div>
			</xsl:when>
			<xsl:otherwise>
				<div class="stepTitle">
					<span class="stepNumber">
						<xsl:value-of select="../../@count"/>. </span>
					<xsl:apply-templates/>
				</div>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="step1[not(child::*)] |
									 step2[not(child::*)] |
									 step3[not(child::*)] |
									 step4[not(child::*)] |
									 step5[not(child::*)] |
									 step6[not(child::*)] |
									 step7[not(child::*)] |
									 step8[not(child::*)]">
		<div class="singlestepNumber">
			<xsl:value-of select="$v00878"/>&#160;<xsl:value-of select="@stepcount"/>. </div>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="specpara">
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="proceduralStep[not(preceding-sibling::*)][not(child::title)][not(child::para)][not(child::warning)][child::caution][child::note][not(child::applic)][not(child::specpara)]">
		<xsl:call-template name="t_inlineApplicability">
			<xsl:with-param name="annotation">
				<xsl:value-of select="$v00034"/>:&#160;</xsl:with-param>
		</xsl:call-template>
		<div>
			<xsl:value-of select="@ptc_level"/>.</div>
		<div class="stepArea">
			<xsl:apply-templates/>
		</div>
	</xsl:template>
	<!--=======================/*程序类DM操作步骤中的标签*/=======小于8层=================-->
	<xsl:template match="proceduralStep/title[count(ancestor-or-self::proceduralStep) &lt; 8]">
		<xsl:choose>
			<xsl:when test="../@stepcount">
				<!--this is when you are step mode -->
				<div class="singlestepNumber"> (<xsl:value-of select="$v00878"/>&#160;<xsl:value-of select="../@stepcount"/>.&#160;<xsl:apply-templates/>
				</div>
			</xsl:when>
			<xsl:otherwise>
			<table style="table-layout:fixed;word-wrap:break-word;width:100%;">
				<tbody>
					<tr width="100%">
						<td width="5%">
              <div class="stepTitle">
                  <xsl:number level="multiple" format="1.1" count="proceduralStep"/>
              </div>
            </td>
						<td width="95%" style="word-break:break-all;width:98%;display:block;float:left;">
              <div class="stepNumber">
                <xsl:apply-templates/>
              </div>
            </td>
					</tr>
				</tbody>
			</table>
				<!--<div class="stepTitle">-->
					<!--<xsl:value-of select="../@ptc_level"/>-->
				<!--</div>-->
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<!--====================大于7层===========================-->
	<xsl:template match="proceduralStep/title[count(ancestor-or-self::proceduralStep) &gt; 7]">
		<xsl:choose>
			<xsl:when test="../@stepcount">
				<!--this is when you are step mode -->
				<div class="singlestepNumber"> (<xsl:value-of select="$v00878"/>&#160;<xsl:value-of select="../@stepcount"/>.&#160;<xsl:apply-templates/>
				</div>
			</xsl:when>
			<xsl:otherwise>
				<div class="stepTitle" style=" background-color: yellow">
					<span>
						<xsl:value-of select="../@ptc_level"/>. </span>
					<xsl:apply-templates/>
				</div>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<!--===============================================-->
	<!--
        Browse mode display
    -->
	<xsl:template match="proceduralStep/para[1][not(preceding-sibling::title or preceding-sibling::proceduralStep)]" priority="3">
		<table style="table-layout:fixed;word-wrap:break-word;width:100%;">
				<tbody>
					<tr>
						<td width="5%" style="word-break:keep-all;">
                <span class="stepTitle">
                  <xsl:number level="multiple" format="1.1" count="proceduralStep"/>
                </span> 
            </td>
						<td width="95%">
              <span class="stepNumber" style="width:98%;display:block;float:left;">
                 <xsl:apply-templates/>
              </span>
            </td>
					</tr>
				</tbody>
		</table>
   
	</xsl:template>
  <!--=======================程序步骤中第二个段落========================-->
  <xsl:template match="proceduralStep/para">
    <xsl:choose>
      <xsl:when test="(name(.) ='para' and  (preceding-sibling::para)) and not(position()=1) and (parent::proceduralStep)">
         <table style="table-layout:fixed;word-wrap:break-word;width:100%;">
           <tbody>
             <tr>
               <td width="5%" style="word-break:keep-all;">
               </td>
               <td width="95%">
                 <span class="stepNumber" style="width:98%;display:block;float:left;">
                   <xsl:apply-templates/>
                 </span>
               </td>
             </tr>
           </tbody>
         </table>
       </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
	<!--===============================================-->
	<!--
        Step mode display
     -->
	<xsl:template match="proceduralStep/para[1][not(preceding-sibling::title or preceding-sibling::proceduralStep)][../@stepcount]" priority="3">
		<div class="singlestepNumber">
			<xsl:value-of select="$v00878"/>&#160;<xsl:value-of select="../@stepcount"/>. </div>
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
</xsl:stylesheet>
