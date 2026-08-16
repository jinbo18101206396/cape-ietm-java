<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<!--===============================================-->
	<xsl:template match="table">
		<xsl:call-template name="t_inlineApplicability"/>
		<!-- create a unique ID for each table, so it can be used to display the
			table in the tear off window. -->
		<!-- make a div for the dmtoc link -->
		<xsl:call-template name="T_makeDMTocLink"/>
		<div id="{generate-id(.)}">
			<xsl:call-template name="t_changeMarker"/>
			<!-- xref anchor -->
			<xsl:call-template name="createLinkAnchor"/>
			<!-- <xsl:apply-templates /> -->
			<!--定义了表格的抬头-->
			<div class="TableTitle">
				<xsl:apply-templates select="title"/>
			</div>
			<div style="text-align: center;">
			<!--<span id="tableHeader">-->
			<table class="tableBorders">
				<xsl:apply-templates select="tgroup/thead"/>
				<xsl:apply-templates select="tgroup/tbody"/>
				<xsl:apply-templates select="tgroup/tfoot"/>
			</table>

			<!--</span>

			<span id="tableFooter">

			</span>--></div>
		</div>
	</xsl:template>
	<!--=============================================== -->
	<xsl:template match="tgroup | colspec | spanspec">
		<xsl:apply-templates/>
	</xsl:template>
	<!--=============================================== -->
	<xsl:template match="thead | thead">
			<xsl:call-template name="constructColgroup"/>
			<xsl:apply-templates/>
	</xsl:template>
	<!--=============================================== -->
	<xsl:template match="tbody">
		<xsl:variable name="tbodyID">tableBody<xsl:number count="table" level="any"/>
		</xsl:variable>
		<!--tableBody定义了表格的字体大小
		<div class="tbodydiv" id="tableBody">
			<xsl:attribute name="id"><xsl:value-of select="$tbodyID"/></xsl:attribute>
			<table>
				<xsl:attribute name="class"><xsl:value-of select="name(.)"/><xsl:call-template name="T_TableBorderAttributes"/></xsl:attribute>
				<xsl:call-template name="constructColgroup"/>-->
				<xsl:apply-templates/>
			<!--</table>
		</div>
		<script type="text/javascript">
			registerTable("<xsl:value-of select="$tbodyID"/>")
		</script>-->
	</xsl:template>
	<!--=============================================== -->
	<xsl:template match="tbody/row">
		<tr>
			<xsl:call-template name="t_changeMarker"/>
			<xsl:apply-templates/>
		</tr>
	</xsl:template>
	<!--=============================================== -->
	<xsl:template match="thead/row | tfoot/row">
		<tr>
			<xsl:apply-templates/>
			<!-- Add a scroll bar shim to the header and footers. 
			<td class="scrollshim">&#160;</td>-->
		</tr>
	</xsl:template>
	<!--=============================================== -->
	<xsl:template match="entry">
		<xsl:element name="td">
			<!-- Check to see if the entry tag or it's parent elements contain rowsep's 
				or colsep's. <xsl:if test="@rowsep='1' or  ../@rowsep='1' or ancestor::tgroup/@rowsep='1' ">-->
			<xsl:attribute name="class">
					bottomBorders
				<!--</xsl:if><xsl:if test="@colsep='1' or ancestor::tgroup/@rowsep='1' ">
					rightBorder
				</xsl:if>--></xsl:attribute>
			<!-- Check to see if the cell has alignment attribute -->
			<xsl:if test="@align">
				<xsl:attribute name="align"><xsl:value-of select="@align"/></xsl:attribute>
			</xsl:if>
			<xsl:if test="@valign">
				<xsl:attribute name="valign"><xsl:value-of select="@valign"/></xsl:attribute>
			</xsl:if>
			<!-- == -->
			<xsl:call-template name="T_Colspan"/>
			<xsl:call-template name="T_Rowspan"/>
			<xsl:apply-templates/>
		</xsl:element>
	</xsl:template>
	<!--=============================================== -->
	<xsl:template name="constructColgroup">
		<!-- this can get called from thead, tbody or tfoot -->
		<!-- the idea here it to make a colgroup element for a table using the 
			colspecs depending on where they are located -->
		<xsl:variable name="totalwidth">
			<xsl:choose>
				<xsl:when test="colspec">
					<xsl:for-each select="colspec[1]">
						<xsl:call-template name="calculateTotalColspecWidthValue">
							<xsl:with-param name="runningTotal">0</xsl:with-param>
						</xsl:call-template>
					</xsl:for-each>
				</xsl:when>
				<xsl:otherwise>
					<xsl:for-each select="../colspec[1]">
						<xsl:call-template name="calculateTotalColspecWidthValue">
							<xsl:with-param name="runningTotal">0</xsl:with-param>
						</xsl:call-template>
					</xsl:for-each>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<!-- colgroup element -->
		<colgroup>
			<!-- 
				set 'global' tgroup settings in the colgroup
				any of the col setting can override them 
			-->
			<xsl:if test="@cols">
				<xsl:attribute name="cols"><xsl:value-of select="../@cols"/></xsl:attribute>
			</xsl:if>
			<xsl:if test="@align">
				<xsl:attribute name="cols"><xsl:value-of select="../@align"/></xsl:attribute>
			</xsl:if>
			<xsl:choose>
				<xsl:when test="colspec">
					<xsl:variable name="colspecCount">
						<xsl:value-of select="count(colspec)"/>
					</xsl:variable>
					<xsl:for-each select="colspec">
						<xsl:call-template name="createColElement">
							<xsl:with-param name="totalwidth" select="$totalwidth"/>
						</xsl:call-template>
					</xsl:for-each>
				</xsl:when>
				<xsl:otherwise>
					<xsl:variable name="colspecCount">
						<xsl:value-of select="count(../colspec)"/>
					</xsl:variable>
					<xsl:for-each select="../colspec">
						<xsl:call-template name="createColElement">
							<xsl:with-param name="totalwidth" select="$totalwidth"/>
						</xsl:call-template>
					</xsl:for-each>
				</xsl:otherwise>
			</xsl:choose>
		</colgroup>
	</xsl:template>
	<!--===============================================-->
	<xsl:template name="calculateTotalColspecWidthValue">
		<xsl:param name="runningTotal"/>
		<xsl:variable name="currentColWidthValue">
			<xsl:choose>
				<xsl:when test="@colwidth">
					<xsl:value-of select="translate(@colwidth, '*%inmcpxtINMCPXPT', '')"/>
				</xsl:when>
				<xsl:otherwise>1</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<xsl:choose>
			<xsl:when test="following-sibling::colspec">
				<xsl:for-each select="(following-sibling::colspec)[1]">
					<xsl:call-template name="calculateTotalColspecWidthValue">
						<xsl:with-param name="runningTotal" select="$runningTotal + $currentColWidthValue"/>
					</xsl:call-template>
				</xsl:for-each>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$runningTotal + $currentColWidthValue"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<!--===============================================-->
	<xsl:template name="createColElement">
		<xsl:param name="totalwidth"/>
		<col>
			<xsl:choose>
				<xsl:when test="@colwidth">
					<xsl:attribute name="width"><xsl:value-of select="round((translate(@colwidth, '*%inmcpxtINMCPXPT', '') div $totalwidth)*100)"/>%</xsl:attribute>
				</xsl:when>
				<xsl:otherwise>
					<xsl:attribute name="width"><xsl:value-of select="round((1 div $totalwidth) * 100)"/>%</xsl:attribute>
				</xsl:otherwise>
			</xsl:choose>
			<xsl:if test="@align">
				<xsl:attribute name="align"><xsl:value-of select="@align"/></xsl:attribute>
			</xsl:if>
		</col>
	</xsl:template>
	<!--===============================================-->
	<xsl:template name="T_Rowspan">
		<xsl:if test="@morerows">		
			<!--<xsl:attribute name="rowspan"><xsl:value-of select="@morerows + 1"/></xsl:attribute>-->	<!-- xiaowd comment -->
		</xsl:if>
	</xsl:template>
	<!--===============================================-->
	<xsl:template name="T_Colspan">
		<xsl:if test="@namest">
			<xsl:variable name="namest">
				<xsl:value-of select="@namest"/>
			</xsl:variable>
			<xsl:variable name="nameend">
				<xsl:value-of select="@nameend"/>
			</xsl:variable>
			<xsl:variable name="nameStartNumber">
				<xsl:for-each select="(ancestor::table//colspec[@colname = $namest])[1]">
					<xsl:value-of select="count(preceding-sibling::colspec)+1"/>
				</xsl:for-each>
			</xsl:variable>
			<xsl:variable name="nameEndNumber">
				<xsl:for-each select="(ancestor::table//colspec[@colname = $nameend])[1]">
					<!--<xsl:value-of select="count(preceding-sibling::colspec)+1"/>-->	<!-- xiaowd comment -->
				</xsl:for-each>
			</xsl:variable>
			<xsl:attribute name="colspan"><xsl:value-of select="($nameEndNumber - $nameStartNumber) +1"/></xsl:attribute>
		</xsl:if>
	</xsl:template>
	<!--===============================================-->
	<xsl:template name="T_TableBorderAttributes">
		<xsl:param name="MoreRowsCount"/>
		<!-- this handles the borders for tables and their entrys-->
		<!-- remember borders are not supported for table-columns and rows-->
		<!-- this is called from table and entry level-->
		<!-- table level borders-->
		<xsl:choose>
			<xsl:when test="name(.)='thead' or name(.)='tbody' or name(.)='tfoot'">
				<!--top border only-->
				<xsl:if test="ancestor::table/@frame='TOP' or 
				ancestor::table/@frame='top' or 
				ancestor::table/@frame='ALL' or 
				ancestor::table/@frame='all' or 
				ancestor::table/@frame='TOPBOT' or 
				ancestor::table/@frame='topbot'"> topBorders </xsl:if>
				<!--bottom border only-->
				<xsl:if test="ancestor::table/@frame='BOTTOM' or ancestor::table/@frame='bottom' or ancestor::table/@frame='ALL' or ancestor::table/@frame='all' or ancestor::table/@frame='TOPBOT' or ancestor::table/@frame='topbot'"> bottomBorders </xsl:if>
				<!--side borders only-->
				<xsl:if test="ancestor::table/@frame='SIDES' or 
				ancestor::table/@frame='sides' or 
				ancestor::table/@frame='ALL' or 
				ancestor::table/@frame='all'"> 
					sideBorders 
				</xsl:if>
				<!-- 为中国用户定制，只要是表格中没有设置frame属性，则给整个表格边框 -->
				<xsl:if test="not(ancestor::table/@frame)">
					tableBorders
				</xsl:if>
			</xsl:when>
			<!-- entry level borders-->
			<xsl:otherwise>
				<!-- according to the spec you should always have a bottom border, so ignore the last row-->
				<!-- in addition you don't want a bottom border if the cell is spread accross the number of available rows anyway-->
				<xsl:if test="not(ancestor::tbody and not(ancestor::row[following-sibling::row]) or (ancestor::tbody and @morerows = count(../following-sibling::row)))">
					<xsl:call-template name="T_doTableContentBorders">
						<xsl:with-param name="pBorderElement">rowsep</xsl:with-param>
					</xsl:call-template>
				</xsl:if>
				<!-- only do if the entry has a sibling and is not part of a table with frames of sides and all-->
				<xsl:if test="(name(.) = 'entry' and not(not(following-sibling::entry) and (ancestor::table/@frame='SIDES' or ancestor::table/@frame='sides' or ancestor::table/@frame='ALL' or ancestor::table/@frame='all'))) or (name(.) = 'entry' and $MoreRowsCount > 0)">
					<xsl:call-template name="T_doTableContentBorders">
						<xsl:with-param name="pBorderElement">colsep</xsl:with-param>
					</xsl:call-template>
				</xsl:if>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<!-- ===================================================================== -->
	<xsl:template name="T_doTableContentBorders">
		<!-- this template will put a bottom or right border on an entry-->
		<!-- pBorderElement tells you if its a colspec or a rowsec-->
		<xsl:param name="pBorderElement"/>
		<xsl:if test="@*[name() = $pBorderElement] = '1' or (ancestor::row/@*[name() = $pBorderElement] = '1' and not(@*[name() = $pBorderElement]))
	or (ancestor::tgroup/@*[name() = $pBorderElement] = '1' and not(@*[name() = $pBorderElement] or ancestor::row/@*[name() = $pBorderElement])) 
	or (ancestor::table/@*[name() = $pBorderElement] = '1' and not(@*[name() = $pBorderElement] or ancestor::row/@*[name() = $pBorderElement] or ancestor::tgroup/@*[name() = $pBorderElement]))">
			<xsl:choose>
				<xsl:when test="$pBorderElement ='rowsep'">
					bottomBorders
				</xsl:when>
				<xsl:otherwise>
					rightBorder
				</xsl:otherwise>
			</xsl:choose>
		</xsl:if>
	</xsl:template>
	<!-- ===================================================================== -->
	<xsl:template name="T_ReturnNumberofColumnsSpanned">
		<!-- this is will return the number of columns the author wants to span 
			based on the column names -->
		<!-- the colname does not have to be a number so cant be done by the value 
			of namest and nameend -->
		<!-- remember the current context attributes of namest that defines where 
			the column is at currenlty -->
		<xsl:param name="ColspecLocation"/>
		<xsl:variable name="namest">
			<xsl:value-of select="@namest"/>
		</xsl:variable>
		<!-- remember what column you want the spanning to end at from the nameend 
			attribut -->
		<xsl:variable name="nameend">
			<xsl:value-of select="@nameend"/>
		</xsl:variable>
		<!-- count the number of colspecs BEFORE the start column -->
		<xsl:variable name="start">
			<xsl:for-each select="ancestor::table//colspec[parent::*[name()=$ColspecLocation]][@colname=$namest]">
				<xsl:value-of select="count(preceding-sibling::colspec)"/>
			</xsl:for-each>
		</xsl:variable>
		<!-- count the number of colspecs AFTER the ending column -->
		<xsl:variable name="end">
			<xsl:for-each select="ancestor::table//colspec[parent::*[name()=$ColspecLocation]][@colname=$nameend]">
				<xsl:value-of select="count(following-sibling::colspec)"/>
			</xsl:for-each>
		</xsl:variable>
		<!-- to find the number of columns spanned subract the total of start and 
			end off the number of colmns -->
		<xsl:value-of select="count(ancestor::table//colspec[parent::*[name()=$ColspecLocation]]) - ($start + $end)"/>
	</xsl:template>
	<!--=============================================== -->
</xsl:stylesheet>
