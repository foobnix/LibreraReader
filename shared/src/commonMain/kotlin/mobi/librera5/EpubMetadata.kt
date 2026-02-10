package mobi.librera5

data class EpubMetadata(var title: List<String> = emptyList(),
                        var creator: List<String> = emptyList(),
                        var genre: List<String> = emptyList(),
                        var data: List<String> = emptyList(),
                        var subject: List<String> = emptyList(),
                        var publisher: List<String> = emptyList(),
                        var date: List<String> = emptyList(),
                        var identifier: List<String> = emptyList(),
                        var language: List<String> = emptyList(),
                        var description: List<String> = emptyList(),
                        var cover: String? = null,
                        var manifest: List<EpubItem> = mutableListOf())

data class EpubTag(

                   var name: String,
                   var value: String,
                   var attrProperty: String,
                   var attrName: String,
                   var attrContent: String,
                   var attrId: String,
                   var attrHref: String,
                   var attrMediaType: String

                  )

data class EpubItem(
    var id: String = "",
    var href: String = "",
    var mediaType: String = "",
    var properties: String = "",
                   )