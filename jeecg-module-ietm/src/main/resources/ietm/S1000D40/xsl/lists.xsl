<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <!--===============================================-->
  <xsl:template match="randlist | randomList | attentionRandomList">
    <xsl:call-template name="t_inlineApplicability">
      <xsl:with-param name="annotation" select="$v00034"/>
    </xsl:call-template>
    <span>
      <xsl:variable name="stylingClass">spanWorkAround</xsl:variable>
      <xsl:attribute name="class">
        <xsl:value-of select="$stylingClass"/>
      </xsl:attribute>
      <xsl:call-template name="t_changeMarker">
        <!-- the existing style class for the row is passed in to the change marker template  so that change marker template can 
                    just append its own style calss, rather than overwrite it  -->
        <xsl:with-param name="existingClasses">
          <xsl:value-of select="$stylingClass"/>
        </xsl:with-param>
      </xsl:call-template>
      <ul>
        <xsl:call-template name="cssListClass"/>
        <xsl:apply-templates/>
      </ul>
    </span>
  </xsl:template>
  <!--===============================================-->
  <xsl:template match="seqlist | sequentialList | attentionSequentialList">
    <xsl:call-template name="t_changeMarker"/>
    <xsl:call-template name="t_inlineApplicability">
      <xsl:with-param name="annotation" select="$v00034"/>
    </xsl:call-template>
    <xsl:apply-templates/>
  </xsl:template>
  <!--===============================================-->
  <!--========================编号列表加上序号=======================-->
  <xsl:template match="seqlist/item | sequentialList/listItem">
    <!--去掉 | attentionSequentialList/attentionSequentialListIte 注释里的条目-->
    <xsl:variable name="stylingClass">seqItem</xsl:variable>
    <xsl:variable name="level">
      <xsl:value-of select="count(ancestor::seqlist | ancestor::sequentialList | ancestor::attentionSequentialListItem |ancestor::attentionSequentialList)"/>
    </xsl:variable>
    <p>
      <xsl:element name="span">
        <xsl:attribute name="class">
          <xsl:value-of select="$stylingClass"/>
        </xsl:attribute>
        <!--<xsl:attribute name="style">
           text-indent:<xsl:value-of select="$level*2"/> em;
        </xsl:attribute>-->
        <xsl:choose>
          <xsl:when test="$level = '1'"></xsl:when>
          <xsl:otherwise>
            <xsl:attribute name="style">
              margin-left: 2em;
            </xsl:attribute>
          </xsl:otherwise>
        </xsl:choose>
        <xsl:call-template name="t_changeMarker">
          <xsl:with-param name="existingClasses">
            <xsl:value-of select="$stylingClass"/>
          </xsl:with-param>
        </xsl:call-template>
        <xsl:call-template name="t_inlineApplicability">
          <xsl:with-param name="annotation" select="$v00034"/>
        </xsl:call-template>
        <xsl:if test="(name(.) ='title' or  not(preceding-sibling::title)) and not(preceding-sibling::para or preceding-sibling::challeng or preceding-sibling::procd or preceding-sibling::challrsp) or name(.) ='attentionSequentialListItem'">
          <xsl:choose>
            <xsl:when test="$level  = 1">
              <xsl:number format="（1）"/>&#160;
            </xsl:when>
            <xsl:when test="$level  = 2">
              <xsl:number format="a."/>&#160;
            </xsl:when>
            <xsl:when test="$level  = 3">
              <xsl:number format="（a）"/>&#160;
            </xsl:when>
            <xsl:otherwise>
              <xsl:number format="error"/>&#160;
            </xsl:otherwise>
          </xsl:choose>
        </xsl:if>
        <xsl:apply-templates/>
        <!--<xsl:number count="seqlist/item | sequentialList/listItem | attentionSequentialListItem"
              level="multiple" format="1"/>. <xsl:apply-templates/>-->
      </xsl:element>
    </p>
  </xsl:template>
  <!--===============================================-->
  <!--========================注意中的编号列表加上序号=======================-->
  <xsl:template match="attentionSequentialListItem">
    <!--去掉 | attentionSequentialListItem 注释里的条目-->
    <xsl:variable name="level">
      <xsl:value-of select="count(ancestor::attentionSequentialList)"/>
    </xsl:variable>
    <p>
      <xsl:element name="span">
        <!--<xsl:if test="name(.) ='attentionSequentialList' and not(preceding-sibling::para or preceding-sibling::challeng or preceding-sibling::procd or preceding-sibling::challrsp) ">-->
        <xsl:choose>
          <xsl:when test="$level mod 2 = 0">
            <xsl:number format="1）"/>&#160;
          </xsl:when>
          <xsl:otherwise>
            <xsl:number format="a）"/>&#160;
          </xsl:otherwise>
        </xsl:choose>
        <!--</xsl:if>-->
        <xsl:apply-templates/>
        <!--<xsl:number count="seqlist/item | sequentialList/listItem | attentionSequentialListItem"
              level="multiple" format="1"/>. <xsl:apply-templates/>-->
      </xsl:element>
    </p>
  </xsl:template>
  <xsl:template match="randlist/item | randomList/listItem | attentionRandomListItem">
    <!--去掉 | attentionSequentialListItem 注释里的条目-->
    <xsl:variable name="stylingClass">seqItem</xsl:variable>
    <xsl:element name="span">
      <xsl:attribute name="class">
        <xsl:value-of select="$stylingClass"/>
      </xsl:attribute>
      <xsl:call-template name="t_changeMarker">
        <xsl:with-param name="existingClasses">
          <xsl:value-of select="$stylingClass"/>
        </xsl:with-param>
      </xsl:call-template>
      <xsl:call-template name="t_inlineApplicability">
        <xsl:with-param name="annotation" select="$v00034"/>
      </xsl:call-template>
      <xsl:variable name="level">
        <xsl:value-of select="count(ancestor::seqlist | ancestor::sequentialList | ancestor::attentionSequentialListItem | ancestor::randomList| ancestor::radlist)"/>
      </xsl:variable>
      <xsl:if test="(name(.) ='title' or not(preceding-sibling::title)) and not(preceding-sibling::para or preceding-sibling::challeng or preceding-sibling::procd or preceding-sibling::challrsp)">
        <xsl:choose>
          <xsl:when test="$level mod 2 = 0">
           <!-- ●&#160;-->
            <p>
              <div>
                <!--<xsl:value-of select="count(ancestor::seqlist | ancestor::sequentialList| ancestor::randomList| ancestor::radlist)"/>-->
                <!--<xsl:attribute name="style">
                  margin-left: 2em;
                </xsl:attribute>-->
                <xsl:choose>
                  <xsl:when test="$level = '1'"></xsl:when>
                  <xsl:otherwise>
                    <xsl:attribute name="style">
                      margin-left: 2em;
                    </xsl:attribute>
                  </xsl:otherwise>
                </xsl:choose>
                <!--<xsl:attribute name="class">paramargin</xsl:attribute>-->
                <xsl:call-template name="t_inlineApplicability">
                  <xsl:with-param name="annotation" select="$v00034"/>
                </xsl:call-template>
                <text>●&#160;</text>
                <xsl:apply-templates/>
              </div>
            </p>
          </xsl:when>
          <xsl:otherwise>
            <!--<xsl:number format="――"/>-->
           <!-- —&#160; -->
            <p>
              <div>
                <!--<xsl:value-of select="count(ancestor::seqlist | ancestor::sequentialList| ancestor::randomList| ancestor::radlist)"/>-->
                <!--<xsl:attribute name="style">
                  margin-left: 2em;
                </xsl:attribute>-->
                <!--<xsl:attribute name="class">paramargin</xsl:attribute>-->
                <xsl:choose>
                  <xsl:when test="$level = '1'"></xsl:when>
                  <xsl:otherwise>
                    <xsl:attribute name="style">
                      margin-left: 2em;
                    </xsl:attribute>
                  </xsl:otherwise>
                </xsl:choose>
                <xsl:call-template name="t_inlineApplicability">
                  <xsl:with-param name="annotation" select="$v00034"/>
                </xsl:call-template>
                <text>—&#160;</text>
                <xsl:apply-templates/>
              </div>
            </p>
          </xsl:otherwise>
        </xsl:choose>
        <!--<xsl:apply-templates/>-->
      </xsl:if>
      <!--<xsl:number count="seqlist/item | sequentialList/listItem | attentionSequentialListItem"
              level="multiple" format="1"/>. <xsl:apply-templates/>-->
    </xsl:element>
  </xsl:template>
  <!--===============================================-->
  <xsl:template match="deflist">
    <table style="width:88%">
      <tbody>
        <tr>
          <xsl:call-template name="t_changeMarker"/>
          <xsl:call-template name="t_inlineApplicability"/>
          <td colspan="2"/>
        </tr>
        <tr>
          <td colspan="2">
            <xsl:apply-templates select="*[not(self::term)][not(self::def)]"/>
          </td>
        </tr>
        <xsl:apply-templates select="term"/>
      </tbody>
    </table>
  </xsl:template>
  <!--===============================================-->
  <xsl:template match="definitionList[not(ancestor::legend)]">
    <table cellpadding="0" cellspacing="0">
          <xsl:attribute name="style">width:88%;</xsl:attribute>
          <xsl:apply-templates select="definitionListHeader"/>
          <tbody>
            <xsl:apply-templates select="definitionListItem"/>
          </tbody>
    </table>
  </xsl:template>
  <!--===============================================-->
  <xsl:template match="definitionListHeader">
    <thead class="defListHeader">
      <tr>
        <td>
          <xsl:apply-templates select="termTitle"/>
        </td>
        <td>
          <xsl:apply-templates select="definitionTitle"/>
        </td>
      </tr>
    </thead>
  </xsl:template>
  <!--===============================================-->
  <xsl:template match="definitionListItem">
    <tr>
      <td>
        <xsl:call-template name="t_changeMarker"/>
        <xsl:call-template name="t_inlineApplicability">
          <xsl:with-param name="annotation" select="$v00034"/>
        </xsl:call-template>
      </td>
    </tr>
    <tr>
      <td colspan="2">
        <xsl:apply-templates select="*[not(self::listItemTerm)][not(self::listItemDefinition)]"/>
      </td>
    </tr>
    <xsl:apply-templates select="listItemTerm"/>
  </xsl:template>
  <!--=======================定义列表========================-->
  <xsl:template match="listItemTerm">
    <tr style="vertical-align:top;">
      <td style="font-weight: bold; width: 50mm;">
        <xsl:call-template name="t_changeMarker"/>
        <xsl:call-template name="t_inlineApplicability">
          <xsl:with-param name="annotation" select="$v00034"/>
        </xsl:call-template>
        <xsl:apply-templates/>
      </td>
      <td style="width: 100mm;">
        <xsl:apply-templates select="following-sibling::listItemDefinition[1]"/>
      </td>
    </tr>
  </xsl:template>
  <!--===============================================-->
  <xsl:template match="listItemDefinition">
    <xsl:element name="span">
      <xsl:call-template name="t_changeMarker"/>
      <xsl:call-template name="t_inlineApplicability">
        <xsl:with-param name="annotation" select="$v00034"/>
      </xsl:call-template>
      <xsl:apply-templates/>
    </xsl:element>
  </xsl:template>
  <!--===============================================-->
  <xsl:template match="term">
    <tr style="vertical-align:top;">
      <td style="font-weight: bold; width: 25%;">
        <xsl:call-template name="t_changeMarker"/>
        <xsl:call-template name="t_inlineApplicability">
          <xsl:with-param name="annotation" select="$v00034"/>
        </xsl:call-template>
        <xsl:apply-templates/>
      </td>
      <td style="width: 74%;">
        <xsl:apply-templates select="following-sibling::def[1]"/>
      </td>
    </tr>
  </xsl:template>
  <!--===============================================-->
  <xsl:template match="def">
    <xsl:call-template name="t_changeMarker"/>
    <xsl:call-template name="t_inlineApplicability">
      <xsl:with-param name="annotation" select="$v00034"/>
    </xsl:call-template>
    <xsl:apply-templates/>
  </xsl:template>
  <!--===============================================-->
  <xsl:template name="cssDivClass">
    <xsl:attribute name="class">
      <xsl:choose>
        <xsl:when test="ancestor::para0">subparaX</xsl:when>
      </xsl:choose>
    </xsl:attribute>
  </xsl:template>
  <!--===============================================-->
  <xsl:template name="cssListClass">
    <xsl:attribute name="class">
      <xsl:choose>
        <xsl:when test="@prefix='SIMPLE'or @prefix='simple' or @prefix='pf01' or @listItemPrefix='pf01' ">RandListNone</xsl:when>
        <xsl:when test="@prefix='UNORDER' or @prefix='unorder' or @prefix='pf02' or @listItemPrefix='pf02' ">
          <!-- match up with a randlist with a prefix of pf02-->
          <!-- make a <ul> tag-->
          <xsl:variable name="vCountOfRandlistAncestors">
            <!-- the value of this var is evaluated by the result of a call-template call-->
            <xsl:choose>
              <xsl:when test="ancestor::randlist or ancestor::randomList">
                <!-- recursive template call-->
                <xsl:call-template name="T_PF02RandListCount">
                  <xsl:with-param name="counter">0</xsl:with-param>
                </xsl:call-template>
              </xsl:when>
              <xsl:otherwise>0</xsl:otherwise>
            </xsl:choose>
          </xsl:variable>
          <xsl:choose>
            <!-- if the count is odd or even show a different bullit type on the ul-->
            <xsl:when test="$vCountOfRandlistAncestors mod 2 = 1">RandListBullet</xsl:when>
            <xsl:otherwise>RandListDash</xsl:otherwise>
          </xsl:choose>
        </xsl:when>
        <xsl:when test="@prefix='pf03' or @listItemPrefix='pf03' ">RandListDash</xsl:when>
        <xsl:when test="@prefix='pf04' or @listItemPrefix='pf04' ">RandListCircleDot</xsl:when>
        <xsl:when test="@prefix='pf05' or @listItemPrefix='pf05' ">RandListCircle</xsl:when>
        <xsl:when test="@prefix='pf06' or @listItemPrefix='pf06' ">RandListSquare</xsl:when>
      </xsl:choose>
    </xsl:attribute>
  </xsl:template>
  <!--===============================================-->
  <xsl:template name="T_PF02RandListCount">
    <!--
			this template needs to count all the randlists with the prefix of
			pf02, UNORDER & unorder that are directly above the current node
		-->
    <xsl:param name="counter"/>
    <xsl:choose>
      <xsl:when test="parent::*">
        <xsl:choose>
          <xsl:when test="parent::randlist[@prefix='pf02'] or
                                    parent::randlist[@prefix='UNORDER'] or
		       parent::randlist[@prefix='unorder'] or
		       parent::randomList[@listItemPrefix='pf02'] ">
            <!-- increments counter by call itself with counter +1 -->
            <xsl:for-each select="parent::*">
              <xsl:call-template name="T_PF02RandListCount">
                <xsl:with-param name="counter" select="$counter + 1"/>
              </xsl:call-template>
            </xsl:for-each>
          </xsl:when>
          <xsl:when test="not(parent::randlist or parent::randomList)">
            <xsl:for-each select="parent::*">
              <xsl:call-template name="T_PF02RandListCount">
                <xsl:with-param name="counter" select="$counter"/>
              </xsl:call-template>
            </xsl:for-each>
          </xsl:when>
          <xsl:when test="parent::randlist[not(@prefix='pf02')] or
                		parent::randlist[not(@prefix='UNORDER')] or
                		parent::randlist[not(@prefix='unorder')] or
                		parent::randomList[not(@listItemPrefix='pf02')] ">
            <xsl:value-of select="$counter"/>
          </xsl:when>
        </xsl:choose>
      </xsl:when>
      <xsl:otherwise>
        <!-- if no parents return counter-->
        <xsl:value-of select="$counter"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <!--===============================================-->
</xsl:stylesheet>
