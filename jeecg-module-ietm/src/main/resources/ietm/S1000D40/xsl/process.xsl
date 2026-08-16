<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <!--===============================================-->
    <xsl:include href="fig_tab.xsl"/>
    <!--===============================================-->
    <xsl:template match="/">
        <xsl:call-template name="importCSS"/>
        <xsl:call-template name="t_root"/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="assertion">
        <!-- Element not output -->
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="dm-seq">
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="dm-node">
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="dm-node-alt">
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="dm-if">
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="dm-then-seq">
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="dm-else-seq">
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="dm-loop">
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="error-message">
        <p class="pdmrText">
            <xsl:apply-templates/>
        </p>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="error-node">
        <p class="pdmrText">
            <xsl:apply-templates/>
        </p>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="extapp|externalApplication">
    	<script>
    		Richfaces.showModalPanel('extappReceivePanel');
    	</script>
    	<p class="pdmrText">
            <xsl:value-of select="text|paraBasic"/>
        </p>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="fillin[not(child::validate)]">
        <script>disableOkBtn = true;</script>
        <div class="pdmrDialog">
            <div class="pdmrDialogContent">
                <xsl:apply-templates/>
                <div class="pdmrButtonBlock">
                    <p/>
                    <input class="pdmrFillin" id="tbFillin" type="text"
                        onKeyUp="onFillInKeyUp(this)">
                        <xsl:attribute name="value">
                            <xsl:value-of select="text"/>
                        </xsl:attribute>
                    </input>
                    <p/>
                    <input class="button, pdmrOkBtn" type="submit" value="OK" id="btnOk" title="OK"
                        onclick="updateResponse()"/>
                    <input class="button, pdmrCancelBtn" type="submit" value="Cancel" id="btnCancel"
                        onclick="hiddenReturnValue.value='cancel'" title="Cancel"/>
                   <script>onFillInLoad();</script>
                </div>
                <p/>
                <div id="errorMessage" class="fillinErrorMsg">
                    <xsl:apply-templates select="script/message"/>
                </div>
            </div>
        </div>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="fillin[child::validate]|userEntry">
        <script>disableOkBtn = true;</script>
        <div class="pdmrDialog">
            <div class="pdmrDialogContent">
                <xsl:apply-templates/>
                <div class="pdmrButtonBlock">
                    <p/>
                    <input class="pdmrFillin" id="tbFillin" type="text"
                        onKeyUp="onUserEntryKeyUp(this)">
                        <xsl:attribute name="value">
                            <xsl:value-of select="text"/>
                        </xsl:attribute>
                    </input>
                    <p/>
                    <input class="button, pdmrOkBtn" type="submit" value="OK" id="btnOk" title="OK"
                        onclick="updateResponse()"/>
                    <input class="button, pdmrCancelBtn" type="submit" value="Cancel" id="btnCancel"
                        onclick="hiddenReturnValue.value='cancel'" title="Cancel"/>
                   <script>OnUserEntryLoad();</script>
                </div>
                <p/>
                <div id="errorMessage" class="fillinErrorMsg">
                    <xsl:apply-templates select="script/message"/>
                </div>
            </div>
        </div>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="menu">
        <script>var disableOkBtn = true;</script>
        <div class="pdmrDialog">
            <div class="pdmrDialogContent">
                <xsl:apply-templates select="text"/>
                <div class="pdmrButtonBlock">
                    <xsl:apply-templates/>
                    <p/>
                    <input type="submit" value="OK" id="btnOk" title="OK" class="button"
                        onclick="updateResponse()">
                        <xsl:attribute name="title">
                            <xsl:value-of select="$v00603"/>
                        </xsl:attribute>
                    </input>
                    <input type="submit" value="Cancel" id="btnCancel"
                        onclick="returnValue='cancel';updateResponse();" class="button">
                        <xsl:attribute name="title">
                            <xsl:value-of select="$v00079"/>
                        </xsl:attribute>
                    </input>
                </div>
            </div>
        </div>
        <script>enableOkButton();</script>
    </xsl:template>
    <!--===============================================-->
    <!-- 
		Handles a menu choice that allows multiple responses by creating checkboxes and enabling the default choices specified by the process data module.
	-->
    <xsl:template
        match="menuchoice[../@select='multiple']|menuChoice[../@choiceSelection='multiple']">
        <script>disableOkBtn = false;</script>
        <p/>
        <input type="checkbox" name="cb">
            <xsl:attribute name="value">
                <xsl:value-of select="count(preceding-sibling::menuchoice|preceding-sibling::menuChoice)"/>
            </xsl:attribute>
            <xsl:attribute name="id">cb<xsl:value-of select="count(preceding-sibling::menuchoice|preceding-sibling::menuChoice)"/></xsl:attribute>
            <xsl:attribute name="onclick">onMultipleMenuChoiceClick(this)</xsl:attribute>
            <xsl:apply-templates/>
        </input>
        <xsl:choose>
            <xsl:when test="@default = '1.0'">
                <script>checkSingleMenuChoice('cb<xsl:value-of
                        select="count(preceding-sibling::menuchoice|preceding-sibling::menuChoice)"/>');</script>
            </xsl:when>
        </xsl:choose>
    </xsl:template>
    <!--===============================================-->
    <!-- 
		Handles a menu choice by creating a radiobutton and attaching javascript for enabling and disabling the button as well as 
		reacting to click selections.
	 -->
    <xsl:template match="menuchoice|menuChoice">
        <p/>
        <input type="radio">
            <xsl:attribute name="name">radio</xsl:attribute>
            <xsl:attribute name="id">radio<xsl:value-of
                    select="count(preceding-sibling::menuchoice|preceding-sibling::menuChoice)"/></xsl:attribute>
            <xsl:attribute name="value">
                <xsl:value-of select="count(preceding-sibling::menuchoice|preceding-sibling::menuChoice)"/>
            </xsl:attribute>
            <xsl:attribute name="onclick">onSingleMenuChoiceClick(this)</xsl:attribute>
            <xsl:apply-templates/>
        </input>
        <!-- Select this menuchoice dialog if it is a default choice -->
        <xsl:choose>
            <xsl:when test="@default='1.0' or @default='1'">
                <xsl:variable name="radioId">radio<xsl:value-of
                        select="count(preceding-sibling::menuchoice|preceding-sibling::menuChoice)"/></xsl:variable>
                <script> checkSingleMenuChoice("<xsl:value-of select="$radioId"/>"); disableOkBtun =
                    false; enableOkButton(); </script>
            </xsl:when>
        </xsl:choose>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="message">
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="num-range">
        <!-- Element not output -->
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="prompt/text | menuchoice/text | prompt/paraBasic">
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="para">
        <p class="pdmrText">
            <xsl:apply-templates/>
        </p>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="process">
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <!--
		Template for hadling the xml tag returned when the runtime hits the end of the process DM runtime.
	-->
    <xsl:template match="process-end">
        <p class="pdmrText">
            <xsl:value-of select="$v00298"/>
        </p>
    </xsl:template>
    <!--===============================================-->
    <!-- 
		Displays a fatal error that terminates the process data module
	-->
    <xsl:template match="process-fatal">
        <p class="pdmrText">
            <xsl:value-of select="$v00336"/>
        </p>
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <!-- 
		Template for handling the xml tag returned when the runtime hits the start of the process DM runtime.  
	-->
    <xsl:template match="process-start">
        <p class="pdmrText">
            <xsl:value-of select="$v00875"/>
        </p>
    </xsl:template>
    <!--===============================================-->
    <!-- 
		Displays a warning as a javascript alert if one is contained within this XML fragment. 
	-->
    <xsl:template match="process-warning">
        <!--<script> alert('<xsl:value-of select="$v01062"/> : <xsl:apply-templates/>'); </script>-->
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="processFragment">
        <p class="prelreqTitle">
            <xsl:value-of select="$v00719"/>
        </p>
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="prompt">
        <xsl:apply-templates/>
    </xsl:template>
    <!--===============================================-->
    <!-- 
		Matches on a refdm fragment that auto-forwards to a RefDM. 
		RefDM elements that are part of steps, or other similar fragments, are dealt with by the refdm template
		in content.xsl
		===
		27.03.09
		This should now be depracated as the refdm construct is now handled purely server side. 
	 -->
    <xsl:template match="processFragment/refdm">
        <xsl:choose>
            <xsl:when test="$SingleDMView='false'">
                <script type="text/javascript">
                    <xsl:apply-templates/>
                </script>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$v01117"/>
                <xsl:call-template name="t_formatDMC"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <!--===============================================-->
    <!-- 
		Inserts the validation code generated by the PDMR engine.
	-->
    <xsl:template match="script">
        <script>
            function validate(control) { <xsl:value-of select="text()"/> } </script>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="text">
        <!-- Don't output raw text -->
    </xsl:template>
    <!--===============================================-->
    <!-- 
		Resolves a string specified in the output
	-->
    <xsl:template match="translation">
        <xsl:choose>
            <xsl:when test="@id = 'v00429'">
                <xsl:value-of select="$v00429"/>
            </xsl:when>
            <xsl:when test="@id = 'v00433'">
                <xsl:value-of select="$v00433"/>
            </xsl:when>
            <xsl:when test="@id = 'v00432'">
                <xsl:value-of select="v00432"/>
            </xsl:when>
            <xsl:when test="@id = 'v00440'">
                <xsl:value-of select="$v00440"/>
            </xsl:when>
            <xsl:when test="@id = 'v00436'">
                <xsl:value-of select="$v00436"/>
            </xsl:when>
            <xsl:when test="@id = 'v00438'">
                <xsl:value-of select="$v00438"/>
            </xsl:when>

            <xsl:when test="@id = 'v00987'">
                <xsl:value-of select="$v00987"/>
            </xsl:when>
            <xsl:when test="@id = 'v00414'">
                <xsl:value-of select="$v00414"/>
            </xsl:when>
            <xsl:when test="@id = 'v00089'">
                <xsl:value-of select="$v00089"/>
            </xsl:when>
            <xsl:when test="@id = 'v01036'">
                <xsl:value-of select="$v01036"/>
            </xsl:when>
            <xsl:when test="@id = 'v01031'">
                <xsl:value-of select="$v01031"/>
            </xsl:when>
            <xsl:when test="@id = 'v01029'">
                <xsl:value-of select="$v01029"/>
            </xsl:when>
            <xsl:when test="@id = 'v01027'">
                <xsl:value-of select="$v01027"/>
            </xsl:when>
            <xsl:when test="@id = 'v01025'">
                <xsl:value-of select="$v01025"/>
            </xsl:when>
            <xsl:when test="@id = 'v01038'">
                <xsl:value-of select="$v01038"/>
            </xsl:when>
            <xsl:when test="@id = 'v01033'">
                <xsl:value-of select="$v01033"/>
            </xsl:when>
            <xsl:when test="@id = 'v00605'">
                <xsl:value-of select="$v00605"/>
            </xsl:when>
            <xsl:when test="@id = 'v00326'">
                <xsl:value-of select="$v00326"/>
            </xsl:when>
            <xsl:when test="@id = 'v00607'">
                <xsl:value-of select="$v00607"/>
            </xsl:when>
            <xsl:when test="@id = 'v00328'">
                <xsl:value-of select="$v00328"/>
            </xsl:when>
            <xsl:when test="@id = 'v00933'">
                <xsl:value-of select="$v00933"/>
            </xsl:when>
            <xsl:when test="@id = 'v00993'">
                <xsl:value-of select="$v00993"/>
            </xsl:when>
            
            <xsl:when test="@id = 'v02405'">
                <xsl:value-of select="$v02405"/>
            </xsl:when>
            <xsl:when test="@id = 'v02406'">
                <xsl:value-of select="$v02406"/>
            </xsl:when>
            <xsl:when test="@id = 'v02407'">
                <xsl:value-of select="$v02407"/>
            </xsl:when>
            <xsl:when test="@id = 'v02409'">
                <xsl:value-of select="$v02409"/>
            </xsl:when>
            <xsl:when test="@id = 'v02410'">
                <xsl:value-of select="$v02410"/>
            </xsl:when>
            <xsl:when test="@id = 'v02411'">
                <xsl:value-of select="$v02411"/>
            </xsl:when>
            <xsl:when test="@id = 'v02412'">
                <xsl:value-of select="$v02412"/>
            </xsl:when>
            
        </xsl:choose>
    </xsl:template>
    <!--===============================================-->
    <xsl:template match="validate">
        <!-- Suppress output -->
    </xsl:template>
    <!--===============================================-->
    <!--
		Outputs the value of a variable-ref element but only when it is a child of a para. 
		The value is inserted into the fragment processed by the engine.
	-->
    <xsl:template match="para/variable-ref|para/variableRef">
        <xsl:apply-templates/>
    </xsl:template>    
    <!--===============================================-->
</xsl:stylesheet>
