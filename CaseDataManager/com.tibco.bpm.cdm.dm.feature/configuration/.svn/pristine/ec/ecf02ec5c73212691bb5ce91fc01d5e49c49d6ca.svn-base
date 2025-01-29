<?xml version='1.0' encoding='utf-8' ?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0" >

	<xsl:param name="propname"/>
	<xsl:output method="text"/>

	<!--xsl:template match="plugin">
		<xsl:text>plugin_</xsl:text><xsl:value-of select="$feature_id" /><xsl:text>-</xsl:text><xsl:value-of select="@id" />
	</xsl:template-->
	<xsl:template match="/">
		<xsl:value-of select="$propname"/>=<xsl:apply-templates select="//plugin"/>
	</xsl:template>

	<xsl:template match="plugin">
		<xsl:value-of select="@id"/>_*.jar,docs.<xsl:value-of select="@id"/>_*.jar<xsl:if test="position() != last()"><xsl:text>,</xsl:text></xsl:if>
	</xsl:template>
</xsl:stylesheet>
