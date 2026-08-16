<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<!--2012 xmlns:aadext="java:com.ptc.aad.xsltext.StringFunctions?path=file:///E:/IETM4/IETP/IETP_Data_Update/xsl/java/" exclude-result-prefixes="aadext"-->
    <!--===============================================-->
    <xsl:include href="prelreq.xsl"/>
    <xsl:include href="lists.xsl"/>
    <xsl:include href="wcnp.xsl"/>
    <xsl:include href="uom.xsl"/>
    <xsl:include href="fig_tab.xsl"/>
    <!--===============================================-->
    <xsl:template match="schedule|maintPlanning">
        <!-- import css first -->
        <xsl:call-template name="importCSS"/>
        <div class="scheduleTitle">
            <xsl:value-of select="$v00818"/>
        </div>
        <xsl:apply-templates/>
        <xsl:call-template name="initOutPutArea"/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="timeLimitInfo">
        <!--<xsl:apply-templates/>-->
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="definspec|inspectionDefinition">
        <!-- todo -->
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="threshold">
        <xsl:variable name="UOM">
            <xsl:call-template name="t_UOM">
                <xsl:with-param name="UOMCode">
                    <xsl:value-of select="@uom|@thresholdUnitOfMeasure"/>
                </xsl:with-param>
            </xsl:call-template>
        </xsl:variable>
        <xsl:if test="position() &gt; 2">&#160;</xsl:if>
        <xsl:value-of select="thresholdValue"/>&#160;
        <xsl:value-of select="$UOM"/>
        <xsl:if test="tolerance">
        	&#160;(-<xsl:value-of select="tolerance/@minus|tolerance/@toleranceLow"/>&#160;<xsl:value-of select="$UOM"/>&#160;+<xsl:value-of select="tolerance/@plus|tolerance/@toleranceHigh"/>&#160;<xsl:value-of select="$UOM"/>)
        </xsl:if>&#160;
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="equip">
        <!-- todo -->
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="value">
        <!-- todo -->
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="limrange">
        <!-- todo -->
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="timelimit|timeLimit">
        <!--stop -->
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="timelim[1]|timeLimitInfo[1]">
        <center>
            <xsl:value-of select="$v00954"/>
        </center>
        <table border="0" width="100%">
            <!--separator -->
            <tr style="border:0px solid #CC88CC; background-color:black">
                <td height="2px" colspan="9"/>
            </tr>
            <tr>
                <td>
                    <xsl:value-of select="$v00583"/>
                </td>
                <td>
                    <xsl:value-of select="$v00646"/>
                </td>
                <td><xsl:value-of select="$v00190"/></td>
                <td>
                    <xsl:value-of select="$v00953"/>
                </td>
                <td>
                    <xsl:value-of select="$v00741"/>
                </td>
                <td><xsl:value-of select="$v01098"/></td>
                <td>
                    <xsl:value-of select="$v00034"/>
                </td>
            </tr>
            <tr style="border:0px solid #CC88CC; background-color:black">
                <td height="2px" colspan="9"/>
            </tr>
            <xsl:for-each select="../timelim|../timeLimitInfo">
                <tr>
                    <td>
                        <xsl:value-of select="equip//nomen|equipGroup//name"/>
                    </td>
                    <td>
                        <xsl:value-of select="equip//pnr|equipGroup//partNumber"/>
                    </td>
                    <td>
                        <xsl:apply-templates select="equip//csnref|equipGroup//catalogSeqNumberRef"
                        />
                    </td>
                    <td>
                        <xsl:apply-templates select="timelimit/limittype|timeLimit/limitType"/>
                    </td>
                    <td>
                        <xsl:apply-templates select="qty|reqQuantity"/>
                    </td>
                    <td>
                        <xsl:if test="cat/@cat = '1'"> I </xsl:if>
                        <xsl:if test="cat/@cat = '2'"> II </xsl:if>
                    </td>
                    <td>
                        <xsl:apply-templates select="applic" mode="sched"/>
                        <xsl:call-template name="t_inlineApplicability"/>
                    </td>
                </tr>
            </xsl:for-each>
        </table>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="tasklist|taskGroup">
        <div style="text-align: center;">
            <xsl:value-of select="$v00441"/>
        </div>
        <table width="100%" border="0">
            <!-- separator -->
            <tr style="border:0px solid #CC88CC; background-color:black">
                <td height="2px" colspan="9"/>
            </tr>
            <tr>
                <td>
                    <xsl:value-of select="$v00772"/> (<xsl:value-of select="$v00260"/>)</td>
                <td>
                    <xsl:value-of select="$v00913"/>
                </td>
                <td>
                    <xsl:value-of select="$v00034"/>
                </td>
            </tr>
            <!-- separator -->
            <tr style="border:0px solid #CC88CC; background-color:black">
                <td height="2px" colspan="9"/>
            </tr>
            <xsl:apply-templates/>
        </table>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="from">
        <!-- todo -->
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="to">
        <!-- todo -->
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="sampling">
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="tolerance">
        <!-- todo -->
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="deftask/limit">
        <div>
            <xsl:apply-templates/>
        </div>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="trigger">
        <!-- todo -->
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="timelim">
        <!-- todo -->
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="limittype|limitType">
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="supervis">
        <!-- todo -->
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="deftask|taskDefinition">
        <!-- stop -->
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="deftask[1]|taskDefinition[1]">
        <!-- we are going to match here and then do a for each on all deftasks-->
        <div style="text-align:center;">
            <xsl:value-of select="$v00520"/>
        </div>
        <!-- table of schedule -->
        <table border="0" width="100%">
            <!-- separator area -->
            <tr style="border:0px solid #CC88CC; background-color:black">
                <td height="2px" colspan="9"/>
            </tr>
            <!-- title row -->
            <tr>
                <td>
                    <xsl:value-of select="$v00772"/> (<xsl:value-of select="$v00260"/>)</td>
                <td>
                    <xsl:value-of select="$v00583"/>
                </td>
              
                <td>
                	<xsl:value-of select="$v02361"/><!-- Task ID -->
                </td>
                <td>
                    <xsl:value-of select="$v00913"/>
                </td>
                <td>
                    <xsl:value-of select="$v00106"/> / <xsl:value-of select="$v00493"/>
                </td>
                <td>
                    <xsl:value-of select="$v00034"/>
                </td>
            </tr>
            <!-- separator area -->
            <tr style="border:0px solid #CC88CC; background-color:black">
                <td height="2px" colspan="9"/>
            </tr>
            <xsl:for-each select="../deftask|../taskDefinition">
                <xsl:if test="prelreqs|preliminaryRqmts">
                    <tr>
                        <td colspan="5">
                            <xsl:apply-templates select="prelreqs|preliminaryRqmts"/>
                        </td>
                    </tr>
                </xsl:if>
                <tr valign="top">
                    <td>
                        <xsl:apply-templates select="refs"/>&#160;
                    </td>
                    <td>
                         <xsl:value-of select="equip//nomen|equipGroup//name"/>&#160;
                    </td>
                    <td>
	                	<xsl:value-of select="@taskIdent"/>&#160;
					</td>
                    <td>
                        <xsl:apply-templates select="task"/> <xsl:value-of select=".//relatedTask/@relatedTaskDescr"/>&#160;<xsl:value-of select=".//relatedTask/@taskIdent"/>
                    </td>
                    <td>
                        <xsl:apply-templates select="limit"/>&#160;
                    </td>
                    <td>
                        <xsl:apply-templates select="applic" mode="sched"/>
                        <xsl:call-template name="t_inlineApplicability"/>
                    </td>
                </tr>
            </xsl:for-each>
        </table>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="taskDefinition/name">
    	<xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="inspection">
        <div>
            <xsl:value-of select="$v00442"/>
            <br/>
            <xsl:apply-templates/>
        </div>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="taskitem|taskItem">
        <tr>
            <td> <xsl:apply-templates select="refs"/> </td>
            <td>
                <xsl:apply-templates select="task"/>
            </td>
            <td>
                <xsl:apply-templates select="applic" mode="sched"/>
                <xsl:call-template name="t_inlineApplicability"/>
            </td>
        </tr>
    </xsl:template>
    <!--===============================================-->
    <xsl:template
        match="taskitem/refs | taskItem/refs | taskitem/refs/refdms | deftask/refs | deftask/refs | deftask/refs/refdms | taskDefinition/refs">
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="task">
        <div>
            <xsl:apply-templates/>
        </div>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="taskTitle">
        <div style="font-weight: bold;">
            <xsl:apply-templates/>
        </div>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="taskDescr">
        <div>
            <xsl:apply-templates/>
        </div>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="cat">
        <!-- todo -->
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="refinspec">
        <!-- todo -->
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="applic" mode="sched">
        <xsl:choose>
            <xsl:when test="model">
                  <xsl:apply-templates mode="sched_applic"/>
              </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$v00022"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template match="displaytext | displayText" mode="sched_applic">
        <xsl:value-of select="."/>
    </xsl:template>
    
    <xsl:template match="model" mode="sched_applic">
        <div>
            <xsl:value-of select="$v00542"/>: <xsl:value-of select="@model"/>
            <xsl:apply-templates select="version" mode="sched_applic"/>
            <xsl:apply-templates select="serialno" mode="sched_applic"/>
        </div>
    </xsl:template>
    
    <xsl:template match="version" mode="sched_applic">
        <div>
            <xsl:value-of select="$v01047"/>: <xsl:value-of select="@version"/>
        </div>
    </xsl:template>
    
    <xsl:template match="serialno" mode="sched_applic">
        <div>
            <xsl:value-of select="$v00855"/>.: <xsl:apply-templates select="single | range" mode="sched_applic"/>
        </div>
    </xsl:template>
    
    <xsl:template match="single" mode="sched_applic">
        <div>
            <xsl:apply-templates/>
        </div>
    </xsl:template>
 
    <xsl:template match="range" mode="sched_applic">
        <div>
            [<xsl:value-of select="@from"/>-<xsl:value-of select="@to"/>]
        </div>
    </xsl:template>
 
 
    <!--===============================================-->
    <xsl:template match="limit/remarks">
        <br/>
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template
        match="deftask//reqconds[noconds] | deftask//reqpers[asrequir] | 
			deftask//supequip[nosupeq] | deftask//supplies[nosupply] |
			deftask//spares[nospares] | deftask//safety[nosafety] |
			taskDefinition//reqCondGroup[noConds] | taskDefinition//reqSupportEquips[noSupportEquips] |
			taskDefinition//reqSupplies[noSupplies] | taskDefinition//reqSpares[noSpares] |
			taskDefinition//reqSafety[noSafety]" priority="3">
        <!-- do not show any prelreqs that are empty with a deftask.-->
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="maintAllocation">
        <div style="padding-bottom: 10pt; padding-top: 10pt; font-size:14pt">
            <center>
                <xsl:value-of select="title"/>
            </center>
        </div>
        <table border="0" width="100%" style="padding-bottom: 10pt;">
            <thead>
                <tr style="border:0px solid #CC88CC; background-color:black">
                    <td height="2px" colspan="9"/>
                </tr>
                <tr>
                    <td colspan="3"/>
                </tr>
                <tr>
                    <th align="left" width="20%"> <xsl:value-of select="$v02122"/></th>
                    <th align="left" width="20%"><xsl:value-of select="$v02442"/></th>
                    <th align="left" width="20%"> <xsl:value-of select="$v02123"/></th>
                    <th align="left" width="20%"><xsl:value-of select="$v02124"/></th>
                    <th align="left" width="20%"><xsl:value-of select="$v02467"/></th>
                </tr>
                <tr style="border:0px solid #CC88CC; background-color:black">
                    <td height="2px" colspan="9"/>
                </tr>
            </thead>
            <tbody>
                <xsl:apply-templates/>
            </tbody>
        </table>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="maintAllocationGroup">
        <tr>
            <xsl:apply-templates/>
        </tr>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="groupNumber">
        <td>
            <xsl:apply-templates/>
        </td>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="componentAssy">
        <td>
            <xsl:apply-templates/>
        </td>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="componentAssy/name">
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="maintFunction">
        <td>
            <xsl:value-of select="@function"/>
        </td>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="maintLevel">
        <td>
            <xsl:apply-templates/>
        </td>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="toolsRefs">
        <td>
            <xsl:apply-templates/>
        </td>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="toolsList">
        <div style="padding-bottom: 10pt; padding-top: 10pt; font-size:14pt">
            <center>
                <xsl:value-of select="title"/>
            </center>
        </div>
        <table width="100%">
            <thead>
                <tr style="border:0px solid #CC88CC; background-color:black">
                    <td height="2px" colspan="9"/>
                </tr>
                <tr>
                    <th align="left" width="25%"><xsl:value-of select="$v02475"/></th>
                    <th align="left" width="25%"><xsl:value-of select="$v02124"/></th>
                    <th align="left" width="25%"><xsl:value-of select="$v02220"/></th>
                    <th align="left" width="25%"><xsl:value-of select="$v02460"/></th>
                </tr>
                <tr style="border:0px solid #CC88CC; background-color:black">
                    <td height="2px" colspan="9"/>
                </tr>
            </thead>
            <tbody>
                <xsl:apply-templates/>
            </tbody>
        </table>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="toolsListGroup">
        <tr>
            <xsl:apply-templates/>
        </tr>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="toolsListCode">
        <td>
            <xsl:value-of select="@id"/>
        </td>        
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="name">
        <td>
            <xsl:apply-templates/>
        </td>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="natoStockNumber">
        <td>
            <xsl:apply-templates/>
        </td>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="remarksList">
        <div style="padding-bottom: 10pt; padding-top: 10pt; font-size:14pt">
            <center>
                <xsl:value-of select="title"/>
            </center>
        </div>
        <table width="100%">
            <thead>
                <tr>
                    <tr style="border:0px solid #CC88CC; background-color:black">
                        <td height="2px" colspan="9"/>
                    </tr>
                    <th align="left" width="25%"><xsl:value-of select="$v00402"/></th>
                    <th align="left" width="75%">Remark</th>
                    <tr style="border:0px solid #CC88CC; background-color:black">
                        <td height="2px" colspan="9"/>
                    </tr>
                </tr>
            </thead>
            <tbody>
                <xsl:apply-templates mode="maintAlloc"/>
            </tbody>
        </table>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="remarkCode" mode="maintAlloc">
        <td>
            <xsl:value-of select="@id"/>
        </td>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="remarks" mode="maintAlloc">
        <td>
            <xsl:apply-templates/>
        </td>
    </xsl:template>
    <!--===============================================-->
</xsl:stylesheet>
