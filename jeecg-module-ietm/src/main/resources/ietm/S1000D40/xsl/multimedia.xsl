<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<!--===================控制显示多媒体的标题============================-->
	<xsl:template match="multimedia">
		<xsl:call-template name="t_inlineApplicability">
			<xsl:with-param name="annotation">
				<xsl:value-of select="$v00034"/>:&#160;</xsl:with-param>
		</xsl:call-template>
		<xsl:variable name="multimediaNumb">
		<xsl:number count="multimedia" from="content" format="1"/>
		</xsl:variable>
		<xsl:variable name="multimediaTitle">
			<!--$v00548多媒体-->
			<!--<xsl:value-of select="$v00355"/>-->
			<xsl:text>多媒体</xsl:text>
			<xsl:number level="any"/>&#160;<xsl:value-of select="title"/>
		</xsl:variable>
		<center>
		<xsl:apply-templates select="multimediaobject|multimediaObject">
					<xsl:with-param name="multimediaNumber" select="$multimediaNumb"/>
		</xsl:apply-templates>
		<p>
		   <xsl:value-of select="substring-before(./multimediaObject/@boardno|./multimediaobject/@infoEntityIdent|./multimediaObject/@infoEntityIdent|./multimediaobject/@boardno,'.')"/>
	    </p>
		</center>
		<span class="figure">
			<center>
				<xsl:value-of select="$multimediaTitle"/>
			</center>
		</span>
		<script>
			multimediaTitle.add('<xsl:value-of select="$multimediaNumb"/>','<xsl:text>1</xsl:text>','<xsl:value-of select="$multimediaTitle"/>');
		</script>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="multimediaobject">
		<xsl:call-template name="t_inlineApplicability">
			<xsl:with-param name="annotation">
				<xsl:value-of select="$v00034"/>:&#160;</xsl:with-param>
		</xsl:call-template>
		<xsl:apply-templates/>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="multimediaobject[@class='audio'] | multimediaobject[@multimediaclass='audio'] | multimediaObject[@multimediaType='audio']">
		<span>
			<xsl:attribute name="id"><xsl:value-of select="../@id"/></xsl:attribute>
		</span>
		<center>
			<xsl:call-template name="t_inlineApplicability">
				<xsl:with-param name="annotation">
					<xsl:value-of select="$v00034"/>:&#160;</xsl:with-param>
			</xsl:call-template>
			<xsl:variable name="audioPos">
				<xsl:number count="//multimediaobject[@class='audio'] | multimediaobject[@multimediaclass='audio'] | multimediaObject[@multimediaType='audio']" level="any"/>
			</xsl:variable>
			<!-- Create a single Windows Media Player object to handle the playing of all audio files -->
			<xsl:if test="$audioPos = '1'">
				<div style="display:none">
					<object classid="CLSID:6BF52A52-394A-11D3-B153-00C04F79FAA6" id="WindowsMediaPlayer">
						<param name="autoStart" value="0"/>
					</object>
				</div>
			</xsl:if>
			<xsl:variable name="boardNum">
				<xsl:value-of select="translate(@boardno|@infoEntityIdent,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')"/>
			</xsl:variable>
			<img>
				<xsl:attribute name="id"><xsl:value-of select="@id"/></xsl:attribute>
				<xsl:attribute name="onclick">playSound(this)</xsl:attribute>
				<!--<xsl:attribute name="url">BinaryResourceStreamer?pub=<xsl:value-of select="$Publication"/>&amp;string=<xsl:value-of select="@boardno|@infoEntityIdent"/></xsl:attribute>-->
				<xsl:attribute name="style">cursor:pointer</xsl:attribute>
				<!--<xsl:attribute name="src">images/speakers.gif</xsl:attribute>-->
				<xsl:attribute name="src">avicit/ietm/viewer/images/audio.gif</xsl:attribute>
				<xsl:attribute name="title"><xsl:value-of select="$v00661"/></xsl:attribute>
				<xsl:attribute name="hspace">10</xsl:attribute>
				<xsl:attribute name="vspace">10</xsl:attribute>
				<!--<xsl:attribute name="file"><xsl:value-of select="@boardno|@infoEntityIdent"/></xsl:attribute>
				<xsl:attribute name="pub"><xsl:value-of select="$Publication"/></xsl:attribute>-->
			</img>
			<script>
				document.getElementById("<xsl:value-of select="@boardno|@infoEntityIdent"/>").src = lessonPath+"images/audio.gif";
			</script>
		</center>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="multimediaobject[@class='video'] | multimediaobject[@multimediaclass='video'] | multimediaObject[@multimediaType='video']">
		<xsl:param name="multimediaNumber"/>
		<span>
			<xsl:attribute name="id"><xsl:value-of select="../@id"/></xsl:attribute>
		</span>
		<script> addFigure('<xsl:value-of select="@boardno|@infoEntityIdent"/>'); </script>
		<center>
			<xsl:call-template name="t_inlineApplicability">
				<xsl:with-param name="annotation">
					<xsl:value-of select="$v00034"/>:&#160;</xsl:with-param>
			</xsl:call-template>
			<img>
				<xsl:attribute name="id"><xsl:value-of select="@boardno|@infoEntityIdent"/></xsl:attribute>
				<xsl:attribute name="onclick"><xsl:text>showMultimediaInfo('</xsl:text><xsl:value-of select="@boardno|@infoEntityIdent"/><xsl:text>');</xsl:text></xsl:attribute>
				<!--<xsl:attribute name="url">BinaryResourceStreamer?pub=<xsl:value-of select="$Publication"/>&amp;string=<xsl:value-of select="@boardno|@infoEntityIdent"/></xsl:attribute>-->
				<xsl:attribute name="style">cursor:pointer</xsl:attribute>
				<!--<xsl:attribute name="src" select="'images/filmreel.gif'"/>-->
				<xsl:attribute name="src">avicit/ietm/viewer/images/video.gif</xsl:attribute>
				<xsl:attribute name="title"><xsl:value-of select="$v02464"/></xsl:attribute>
				<xsl:attribute name="hspace">10</xsl:attribute>
				<xsl:attribute name="vspace">10</xsl:attribute>
				<!--<xsl:attribute name="file"><xsl:value-of select="@boardno|@infoEntityIdent"/></xsl:attribute>
				<xsl:attribute name="pub"><xsl:value-of select="$Publication"/></xsl:attribute>-->
			</img>
			<script>
				document.getElementById("<xsl:value-of select="@boardno|@infoEntityIdent"/>").src = lessonPath+"images/video.gif";
			</script>
		</center>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="multimediaobject[@class='other'] | multimediaobject[@multimediaclass='other'] | multimediaObject[@multimediaType='other']">
		<xsl:param name="multimediaNumber"/>
		<span>
			<xsl:attribute name="id"><xsl:value-of select="../@id"/></xsl:attribute>
		</span>
		<script> addFigure('<xsl:value-of select="@boardno|@infoEntityIdent"/>'); </script>
		<center>
			<xsl:call-template name="t_inlineApplicability">
				<xsl:with-param name="annotation">
					<xsl:value-of select="$v00034"/>:&#160;</xsl:with-param>
			</xsl:call-template>
			<img>
				<xsl:attribute name="id"><xsl:value-of select="@boardno|@infoEntityIdent"/></xsl:attribute>
				<xsl:attribute name="onclick"><xsl:text>showMultimediaInfo('</xsl:text><xsl:value-of select="@boardno|@infoEntityIdent"/><xsl:text>');</xsl:text></xsl:attribute>
				<!--<xsl:attribute name="url">BinaryResourceStreamer?pub=<xsl:value-of select="$Publication"/>&amp;string=<xsl:value-of select="@boardno|@infoEntityIdent"/></xsl:attribute>-->
				<xsl:attribute name="style">cursor:pointer</xsl:attribute>
				<!--<xsl:attribute name="src">images/dmodule/document.gif</xsl:attribute>-->
				<xsl:attribute name="src">avicit/ietm/viewer/images/flash.gif</xsl:attribute>
				<!--<xsl:attribute name="file"><xsl:value-of select="@boardno|@infoEntityIdent"/></xsl:attribute>
				<xsl:attribute name="pub"><xsl:value-of select="$Publication"/></xsl:attribute>-->
				<xsl:attribute name="title"><xsl:value-of select="$v01112"/></xsl:attribute>
			</img>
			<script>
				document.getElementById("<xsl:value-of select="@boardno|@infoEntityIdent"/>").src = lessonPath+"images/flash.gif";
			</script>
		</center>
	</xsl:template>
	<!--===============================================-->
	<xsl:template match="multimediaobject[@class='3D'] | multimediaobject[@multimediaclass='3D'] | multimediaObject[@multimediaType='3D']">
		<xsl:param name="multimediaNumber"/>
		<xsl:apply-templates select="param |parameter"/>
		<span>
			<xsl:attribute name="id"><xsl:value-of select="../@id"/></xsl:attribute>
		</span>
		<script> addFigure('<xsl:value-of select="@boardno|@infoEntityIdent"/>'); </script>
		<center>
			<xsl:call-template name="t_inlineApplicability">
				<xsl:with-param name="annotation">
					<xsl:value-of select="$v00034"/>:&#160;</xsl:with-param>
			</xsl:call-template>
			<img>
				<xsl:attribute name="id"><xsl:value-of select="@boardno|@infoEntityIdent"/></xsl:attribute>
				<xsl:attribute name="onclick"><xsl:text>showMultimediaInfo('</xsl:text><xsl:value-of select="@boardno|@infoEntityIdent"/><xsl:text>');</xsl:text></xsl:attribute>
				<!--<xsl:attribute name="url">BinaryResourceStreamer?pub=<xsl:value-of select="$Publication"/>&amp;string=<xsl:value-of select="@boardno|@infoEntityIdent"/></xsl:attribute>-->
				<xsl:attribute name="style">cursor:pointer</xsl:attribute>
				<xsl:attribute name="src">avicit/ietm/viewer/images/3d.gif</xsl:attribute>
				<!--<xsl:attribute name="file"><xsl:value-of select="@boardno|@infoEntityIdent"/></xsl:attribute>
				<xsl:attribute name="pub"><xsl:value-of select="$Publication"/></xsl:attribute>-->
				<xsl:attribute name="title"><!-- <xsl:value-of select="$v00661"/> -->Open 3D</xsl:attribute>
			</img>
			<div>
				<xsl:attribute name="id">isoLegendDiv<xsl:value-of select="generate-id(.)"/></xsl:attribute>
				<xsl:attribute name="style">display:none</xsl:attribute>
				<xsl:call-template name="ParamLegendContent">
					<xsl:with-param name="boardno" select="@boardno|@infoEntityIdent"/>
				</xsl:call-template>
			</div>
			<!-- <script>
				document.getElementById("<xsl:value-of select="@boardno|@infoEntityIdent"/>").src = lessonPath+"images/3d.gif";
			</script> -->
		</center>
	</xsl:template>
	<!--===============================================-->
</xsl:stylesheet>
