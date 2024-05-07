package org.spreadme.pdfgadgets.model

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

data class FileMetadata(
    val uid: String,
    val path: String,
    val name: String,
    val thumbnail: String? = null,
    val length: Long,
    val openTime: Date
) {

    var openProperties: OpenProperties = OpenProperties()

    fun path(): Path = Paths.get(path)
    fun file(): File = Paths.get(path).toFile()
}

object FileMetadatas : Table("FILE_METADATAS") {
    val id = integer("id").autoIncrement()
    val uid = varchar("uid", 50)
    val path = varchar("path", 500)
    val name = varchar("name", 500)
    val thumbnail = varchar("thumbnail", 500).nullable()
    val length = long("length")
    val openTime = datetime("openTime")

    override val primaryKey = PrimaryKey(id, name = "PK_FILE_METADATAS_ID")
}
