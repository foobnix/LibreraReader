package org.spreadme.pdfgadgets.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.spreadme.pdfgadgets.model.FileMetadata
import org.spreadme.pdfgadgets.model.FileMetadatas
import java.nio.file.Path
import java.time.ZoneId
import java.util.*

class FileMetadataRepository {

    suspend fun save(fileMetadata: FileMetadata) {
        withContext(Dispatchers.IO) {
            transaction {
//                val ids = FileMetadatas.select { (FileMetadatas.path.eq(fileMetadata.path)) }
//                    .map { it[FileMetadatas.id] }
//                    .toList()
//                if (ids.isNotEmpty()) {
//                    FileMetadatas.deleteWhere { (FileMetadatas.id.inList(ids)) }
//                }
                FileMetadatas.insertOrUpdate(fileMetadata)

//                FileMetadatas.insert {
//                    it[uid] = fileMetadata.uid
//                    it[path] = fileMetadata.path
//                    it[name] = fileMetadata.name
//                    it[thumbnail] = fileMetadata.thumbnail
//                    it[length] = fileMetadata.length
//                    it[openTime] = fileMetadata.openTime.toInstant().atZone(ZoneId.systemDefault())
//                        .toLocalDateTime()
//                }
            }
        }
    }

    suspend fun query(): List<FileMetadata> = withContext(Dispatchers.IO) {
        transaction {
            FileMetadatas.selectAll()
                .orderBy(FileMetadatas.openTime to SortOrder.DESC)
                .map {
                    FileMetadata(
                        it[FileMetadatas.uid],
                        it[FileMetadatas.path],
                        it[FileMetadatas.name],
                        it[FileMetadatas.thumbnail],
                        it[FileMetadatas.length],
                        Date.from(it[FileMetadatas.openTime].atZone(ZoneId.systemDefault()).toInstant())
                    )
                }.toList()
        }
    }

    suspend fun deleteByPath(path: Path) = withContext(Dispatchers.IO){
        transaction {
            FileMetadatas.deleteWhere { (FileMetadatas.path.eq(path.toString())) }
        }
    }
}