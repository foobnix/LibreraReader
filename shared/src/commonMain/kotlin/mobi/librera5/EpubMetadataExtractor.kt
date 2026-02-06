package mobi.librera5


import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.core.impl.multiplatform.StringReader

import nl.adaptivity.xmlutil.xmlStreaming

@OptIn(XmlUtilInternal::class)
internal class EpubMetadataExtractor {


    fun metaParser(xmlContent: String): EpubMetadata {
        val reader = xmlStreaming.newReader(StringReader(xmlContent))

        val meta = EpubMetadata()

        var currentElement = ""
        while (reader.hasNext()) {
            when (reader.next()) {
                EventType.START_ELEMENT -> {
                    currentElement = reader.localName
                    println("currentElement: $currentElement")
                }

                EventType.TEXT -> {
                    val text = reader.text.trim()

                    if (text.isNotEmpty()) {
                        when (currentElement) {
                            "title" -> meta.title = text
                            "creator" -> meta.author = text
                            "subject" -> meta.genre = text
                        }
                        println(text)
                    }
                }

                EventType.END_ELEMENT -> {

                }

                EventType.START_DOCUMENT -> {

                }

                EventType.END_DOCUMENT -> {

                }

                else -> {

                }
            }
        }

        reader.close()
        return meta
    }
}


