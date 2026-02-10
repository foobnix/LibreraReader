package mobi.librera5

import androidx.collection.floatIntMapOf
import kotlin.test.Test
import kotlin.test.assertEquals

class EpubMetadataExtractorTest {

    

    @Test
    fun testCover(){
        val opf = """
            <?xml version="1.0" encoding="UTF-8"?>
<package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="uid" xml:lang="en-US" prefix="cc: http://creativecommons.org/ns#">
    <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
        <dc:identifier id="uid">code.google.com.epub-samples.wasteland-basic</dc:identifier>
        <dc:title>The Waste Land</dc:title>
        <dc:creator>T.S. Eliot</dc:creator>
        <dc:language>en-US</dc:language>
        <dc:date>2011-09-01</dc:date>
        <meta property="dcterms:modified">2012-01-18T12:47:00Z</meta>
        <!-- rights expressions for the work as a whole -->
        <dc:rights>This work is shared with the public using the Attribution-ShareAlike 3.0 Unported (CC BY-SA 3.0) license.</dc:rights>        
        <link rel="cc:license" href="http://creativecommons.org/licenses/by-sa/3.0/"/>
        <meta property="cc:attributionURL">http://code.google.com/p/epub-samples/</meta>
        <!-- rights expression for the cover image -->       
        <link rel="cc:license" refines="#cover" href="http://creativecommons.org/licenses/by-sa/3.0/" />
        <link rel="cc:attributionURL" refines="#cover" href="http://en.wikipedia.org/wiki/Simon_Fieldhouse" />        
        <!-- cover meta element included for 2.0 reading system compatibility: -->
        <meta name="cover" content="cover"/>
    </metadata> 
    <manifest>
        <item id="t1" href="wasteland-content.xhtml" media-type="application/xhtml+xml" />
        <item id="nav" href="wasteland-nav.xhtml" properties="nav" media-type="application/xhtml+xml" />
        <item id="cover" href="wasteland-cover.jpg" media-type="image/jpeg" properties="cover-image" />
        <item id="css" href="wasteland.css" media-type="text/css" />
        <item id="css-night" href="wasteland-night.css" media-type="text/css" />
        <!-- ncx included for 2.0 reading system compatibility: -->
        <item id="ncx" href="wasteland.ncx" media-type="application/x-dtbncx+xml" />
    </manifest>
    <spine toc="ncx">
        <itemref idref="t1" />        
    </spine>    
</package>
            """.trimIndent()
        val extractor = EpubMetadataExtractor()
        val meta = extractor.epubMetaParser(opf);
        assertEquals(meta.title.firstOrNull(), "The Waste Land")
        assertEquals(meta.creator.firstOrNull(), "T.S. Eliot")
       assertEquals(meta.manifest.size, 5)
        assertEquals(meta.cover, "wasteland-cover.jpg")

    }

     @Test
    fun testParseOpf() {
        val opfContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <package xmlns="http://www.idpf.org/2007/opf" version="3.0" xml:lang="en" unique-identifier="pub-id">
                  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
                        <dc:title id="title">Hefty Water</dc:title>
                        <dc:creator id="creator">Hefty Water2</dc:creator>
                        <dc:identifier id="pub-id">code.google.com.epub-samples.hefty.water</dc:identifier>
                        <meta property="dcterms:modified">2012-03-29T12:00:00Z</meta>
                        <dc:date>2012-03-29</dc:date>
                        <dc:language>en</dc:language>
                  </metadata>
                  <manifest>
                        <item id="doc" href="heftywater.xhtml" properties="switch" media-type="application/xhtml+xml"/>
                        <item id="nav" href="nav.xhtml" properties="nav" media-type="application/xhtml+xml"/>            
                  </manifest>
                  <spine>
                        <itemref idref="doc"/>            
                  </spine>
            </package>
        """.trimIndent()

        val extractor = EpubMetadataExtractor()
        val meta = extractor.epubMetaParser(opfContent);
        assertEquals(meta.title.firstOrNull(), "Hefty Water")
        assertEquals(meta.creator.firstOrNull(), "Hefty Water2")
        assertEquals(meta.genre.firstOrNull(), null)
        assertEquals(meta.language.firstOrNull(), "en")
        assertEquals(meta.identifier.firstOrNull(), "code.google.com.epub-samples.hefty.water")

    }
}
