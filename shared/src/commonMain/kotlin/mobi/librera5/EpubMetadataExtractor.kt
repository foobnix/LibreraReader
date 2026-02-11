package mobi.librera5

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM
import okio.openZip
import org.kobjects.ktxml.api.EventType
import org.kobjects.ktxml.mini.MiniXmlPullParser

class EpubMetadataExtractor() {

     fun cover(zipFilePath: String): ByteArray {
        println("Extracting EPUB cover from: $zipFilePath")

        val zipPath = zipFilePath.toPath()
        val fileSystem = FileSystem.SYSTEM

        if (!zipFilePath.endsWith(".epub", ignoreCase = true)) {
            return ByteArray(0)
        }

        if (!fileSystem.exists(zipPath)) {
            return ByteArray(0)
        }

        val zipFs = fileSystem.openZip(zipPath)
        val paths = zipFs.listRecursively(".".toPath()).toList()

        // Find and parse OPF file
        var coverFilename: String? = null
        for (path in paths) {
            if (path.name.endsWith(".opf", ignoreCase = true)) {
                val opfContent = zipFs.read(path) { readUtf8() }
                val metadata = epubMetaParser(opfContent)
                coverFilename = metadata.cover
                break
            }
        }

        // Try to find cover by metadata filename
        if (coverFilename != null) {
            for (path in paths) {
                if (path.toString().endsWith(coverFilename)) {
                    val bytes = zipFs.read(path) { readByteArray() }
                    zipFs.close()
                    return bytes
                }
            }
        }

        // Fallback: find any image file
        for (path in paths) {
            val name = path.name.lowercase()
            if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")|| name
                .endsWith(".webp")) {
                val bytes = zipFs.read(path) { readByteArray() }
                zipFs.close()
                return bytes
            }
        }

        zipFs.close()
        return ByteArray(0)
    }

    private fun findCoverImage(paths: List<Path>, metadataCover: String?): Path? {
        metadataCover?.let { coverName ->
            paths.find { it.toString().endsWith(coverName) }?.let { return it }
        }

        val coverPatterns = listOf("cover", "front")
        val imageExtensions = listOf(".jpg", ".jpeg", ".png", ".webp")

        return paths.firstOrNull { path ->
            val nameLower = path.name.lowercase()

            coverPatterns.any {
                    pattern -> nameLower.contains(pattern) } &&
                    imageExtensions.any { ext ->nameLower.endsWith(ext)
            }
        } ?: paths.firstOrNull { path ->
            imageExtensions.any { ext -> path.name.lowercase().endsWith(ext) }
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


