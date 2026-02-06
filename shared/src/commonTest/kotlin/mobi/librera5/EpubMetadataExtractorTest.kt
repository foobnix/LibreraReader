package mobi.librera5

import kotlin.test.Test
import kotlin.test.assertEquals

class EpubMetadataExtractorTest {

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
        val meta = extractor.metaParser(opfContent);
        assertEquals(meta.title, "Hefty Water")
        assertEquals(meta.author, "Hefty Water2")
        assertEquals(meta.genre, "")

    }
}
