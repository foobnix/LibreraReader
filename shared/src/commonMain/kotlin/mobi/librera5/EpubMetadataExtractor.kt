package mobi.librera5

import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import okio.openZip
import okio.use
import org.kobjects.ktxml.api.EventType
import org.kobjects.ktxml.api.XmlPullParser
import org.kobjects.ktxml.mini.MiniXmlPullParser

class EpubMetadataExtractor() {

    fun cover(zipFilePath: String): ByteArray {
        println("EPUB cover for $zipFilePath")

        val zipPath = zipFilePath.toPath()
        val fileSystem = FileSystem.SYSTEM

        if (!zipFilePath.endsWith(".epub") || !fileSystem.exists(zipPath)) {
            return ByteArray(0)
        }

        var meta = EpubMetadata()

        return fileSystem.openZip(zipPath).use { zipFs ->
            val paths = zipFs.listRecursively(".".toPath()).toList()
            paths.firstOrNull { it.name.endsWith(".opf") }?.let { opfPath ->
                val opf = zipFs.read(opfPath) { readUtf8() }
                meta = epubMetaParser(opf)
            }
            val coverPath = paths.firstOrNull { it.name == meta.cover } ?: paths.firstOrNull {
                val name = it.name.lowercase()
                name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")
            }

            coverPath?.let { path ->
                zipFs.read(path) { readByteArray() }
            } ?: ByteArray(0)

        }

    }

    fun epubMetaParser(xmlContent: String): EpubMetadata = EpubMetadata().apply {
        var coverTemp = ""
        metaParser(xmlContent) { tag ->
            val element = tag.name.replace("dc:", "").replace("dcns:", "")

            when (element) {
                "title" -> title += tag.value
                "creator" -> creator += tag.value
                "date" -> date += tag.value
                "subject" -> subject += tag.value
                "publisher" -> publisher += tag.value
                "identifier" -> identifier += tag.value
                "language" -> language += tag.value
                "description" -> language += tag.value
                "meta" -> {
                    if (tag.attrName == "cover") {
                        coverTemp = tag.attrContent
                    }
                }

                "item" -> {
                    manifest += EpubItem(tag.attrId,
                        tag.attrHref,
                        tag.attrMediaType,
                        tag.attrProperty)
                }
            }
        }
        cover = manifest.find { it.id == coverTemp }?.href
    }

    fun metaParser(xmlContent: String, resolver: (EpubTag) -> Unit) {
        val reader = MiniXmlPullParser(xmlContent, relaxed = true)
        while (reader.next() != EventType.END_DOCUMENT) {
            val event = reader.eventType
            when (event) {

                EventType.START_TAG -> {
                    val attrProperty = reader.getAttributeValue("", "property") ?: ""
                    val attrName = reader.getAttributeValue("", "name") ?: ""
                    val attrContent = reader.getAttributeValue("", "content") ?: ""
                    val attrId = reader.getAttributeValue("", "id") ?: ""
                    val attrHref = reader.getAttributeValue("", "href") ?: ""
                    val attrMediaType = reader.getAttributeValue("", "media-type") ?: ""
                    val name = reader.name
                    val text = reader.nextText()
                    resolver(EpubTag(
                        name = name,
                        value = text,
                        attrProperty = attrProperty,
                        attrName = attrName,
                        attrContent = attrContent,
                        attrId = attrId,
                        attrHref = attrHref,
                        attrMediaType = attrMediaType,
                                    ))
                }

                else -> Unit
            }
        }

    }
}


