<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format">
    <xsl:output method="text" indent="no" omit-xml-declaration="yes" encoding="UTF-16"/>

    <!-- =================== -->
    <xsl:template match="/">
        <xsl:apply-templates/>
    </xsl:template>
	
    <!-- =================== -->
	<xsl:template match="expression">
	<xsl:if test="following-sibling::idivide">
		<xsl:text>Integer.parseInt(</xsl:text>
	</xsl:if>
		<xsl:text>(</xsl:text>
		<xsl:if test="preceding-sibling::defined">
			<xsl:text>"</xsl:text>
		</xsl:if>
		<xsl:choose>
			<xsl:when test="concat | substring | add | remove | union | intersect | member | subset | disjoint">
				<xsl:call-template name="complex_binary_exp">
					<xsl:with-param name="func_type"><xsl:value-of select="name(child::*[position() = 2])"/></xsl:with-param>
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:choose>
					<xsl:when test="set-diff">
						<xsl:call-template name="complex_binary_exp">
							<xsl:with-param name="func_type">SetDifference</xsl:with-param>
						</xsl:call-template>
					</xsl:when>
					<xsl:otherwise>
						<xsl:choose>
							<xsl:when test="exponent">
								<xsl:call-template name="exponent_binary_exp">
									<xsl:with-param name="func_type">java.lang.Math.pow</xsl:with-param>
								</xsl:call-template>
							</xsl:when>
							<xsl:otherwise>
								<xsl:apply-templates/>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:otherwise>
		</xsl:choose>
		<xsl:if test="preceding-sibling::index | preceding-sibling::idivide">
			<xsl:text>)</xsl:text>
		</xsl:if>
		<xsl:if test="preceding-sibling::defined">
			<xsl:text>"</xsl:text>
		</xsl:if>
		<xsl:text>)</xsl:text>
	</xsl:template>

<!-- ====Simple Binary Expression=====-->

	<xsl:template match="eq">
		<xsl:text>==</xsl:text>
	</xsl:template>
	
	<xsl:template match="ne">
		<xsl:text>!=</xsl:text>
	</xsl:template>
	
	<xsl:template match="lt">
		<xsl:text>&lt;</xsl:text>
	</xsl:template>
	
	<xsl:template match="gt">
		<xsl:text>&gt;</xsl:text>
	</xsl:template>
	
	<xsl:template match="le">
		<xsl:text>&lt;=</xsl:text>
	</xsl:template>
	
	<xsl:template match="ge">
		<xsl:text>&gt;=</xsl:text>
	</xsl:template>
	
	<xsl:template match="and">
		<xsl:text>&amp;&amp;</xsl:text>
	</xsl:template>
	
	<xsl:template match="or">
		<xsl:text>||</xsl:text>
	</xsl:template>
	
	<xsl:template match="xor">
		<xsl:text>^</xsl:text>
	</xsl:template>
	
	<xsl:template match="plus">
		<xsl:text>+</xsl:text>
	</xsl:template>
	
	<xsl:template match="minus">
		<xsl:text>-</xsl:text>
	</xsl:template>
	
	<xsl:template match="times">
		<xsl:text>*</xsl:text>
	</xsl:template>
	
	<xsl:template match="divide">
		<xsl:text>/</xsl:text>
	</xsl:template>
	
	<xsl:template match="idivide">
		<xsl:text>/</xsl:text>
	</xsl:template>
	
	<xsl:template match="modulus">
		<xsl:text>%</xsl:text>
	</xsl:template>

	<!-- ====Complex Binary Expression=====-->	
	<xsl:template name="complex_binary_exp">
		<xsl:param name="func_type"></xsl:param>
		<xsl:value-of select="$func_type"/>(
		<xsl:apply-templates select="expression[1]"/>, <xsl:apply-templates select="expression[2]"/>)
	</xsl:template>
	
	<xsl:template name="exponent_binary_exp">
		<xsl:param name="func_type"></xsl:param>
		<xsl:value-of select="$func_type"/>((double)
		<xsl:apply-templates select="expression[1]"/>, (double)<xsl:apply-templates select="expression[2]"/>)
	</xsl:template>

	<!-- ====Unary Expression=====-->	

	<xsl:template match="not">
		<xsl:text>!</xsl:text>
	</xsl:template>
	
	<xsl:template match="empty">
		<xsl:text>IsEmpty</xsl:text>
	</xsl:template>
	
	<xsl:template match="neg">
		<xsl:text>-</xsl:text>
	</xsl:template>
	
	<xsl:template match="trunc">
		<xsl:text>Integer.parseInt</xsl:text>
	<xsl:apply-templates/>
	
	</xsl:template>
	
	<xsl:template match="float">
		<xsl:text>Double.parseDouble</xsl:text>
		<xsl:apply-templates/>
	</xsl:template>
	
	<xsl:template match="index">
		<xsl:text>Index</xsl:text>
		<xsl:apply-templates/>
	</xsl:template>
	
	<xsl:template match="index-value">
		<xsl:if test="name(preceding-sibling::*[1]) = ''">
			<xsl:text>(</xsl:text>
		</xsl:if>
		<xsl:apply-templates/>
		<xsl:text>,</xsl:text>
	</xsl:template>
	
	<xsl:template match="defined">
		<xsl:text>IsDefined</xsl:text>
	</xsl:template>
	
	<xsl:template match="sizeof">
		<xsl:text>SizeOf</xsl:text>
	</xsl:template>

	<!-- ====Expression Values=====-->	
	<xsl:template match="variable-ref">
		<!-- <xsl:value-of select="@name"/> -->
		<xsl:apply-templates/>
	</xsl:template>
	
	<xsl:template match="boolean">
		<xsl:apply-templates/>
	</xsl:template>
	
	<xsl:template match="true">
		<xsl:text>true</xsl:text>
	</xsl:template>
	
	<xsl:template match="false">
		<xsl:text>false</xsl:text>
	</xsl:template>
	
	<xsl:template match="novalue">
		<xsl:text>null</xsl:text>
	</xsl:template>
	
	<xsl:template match='set'> 
		<xsl:if test="name(child::*[1]) = 'integer'">
			 <xsl:text>new Integer</xsl:text>
		</xsl:if>
		<xsl:if test="name(child::*[1]) = 'real'">
			 <xsl:text>new Double</xsl:text>
		</xsl:if>
		<xsl:if test="name(child::*[1]) = 'string'">
			 <xsl:text>new String</xsl:text>
		</xsl:if>
		<xsl:text>[]</xsl:text>
		<xsl:text>{</xsl:text>
		<xsl:apply-templates/>
		<xsl:text>}</xsl:text>
	</xsl:template>
	
	<xsl:template match='integer'>
		<xsl:apply-templates/>
		<xsl:if test="name(parent::*) = 'set' and position() != last()">
			 <xsl:text>,</xsl:text>
		</xsl:if>
	</xsl:template>
	
	<xsl:template match='real'>
		<xsl:apply-templates/>
		<xsl:if test="name(parent::*) = 'set' and position() != last()">
		 <xsl:text>,</xsl:text>
		</xsl:if>
	</xsl:template>
	
	<xsl:template match='string'>
		<xsl:text>"</xsl:text><xsl:apply-templates/><xsl:text>"</xsl:text>
		<xsl:if test="name(parent::*) = 'set' and position() != last()">
			 <xsl:text>,</xsl:text>
		</xsl:if>
	</xsl:template>

</xsl:stylesheet>
