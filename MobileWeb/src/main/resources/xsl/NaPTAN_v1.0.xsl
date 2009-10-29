<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<xsl:output method="text"/>

	<xsl:template match="/">
var markers = [
	<xsl:apply-templates select="//Stop"/>
]
	</xsl:template>

    <xsl:template match="Stop">
    	<xsl:if test="Lon &lt; -2.59 and Lon &gt; -2.61 and Lat &lt; 51.465 and Lat &gt; 51.455">
    {
    	"id": "<xsl:value-of select="ATCOCode"></xsl:value-of>",
    	"lat": "<xsl:value-of select="Lat"></xsl:value-of>",
    	"lng": "<xsl:value-of select="Lon"></xsl:value-of>"
    },
    	</xsl:if>
    </xsl:template>

</xsl:stylesheet>