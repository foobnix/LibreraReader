package org.spreadme.pdfgadgets.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.spreadme.pdfgadgets.config.AppConfig
import org.spreadme.pdfgadgets.config.AppConfigs
import org.spreadme.pdfgadgets.config.DBHelper
import org.spreadme.pdfgadgets.model.FileMetadatas
import org.spreadme.common.copy
import org.spreadme.common.createFile
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import com.artifex.mupdf.fitz.Context

class DefaultAppConfigRepository : AppConfigRepository {

    private val logger = KotlinLogging.logger {}

    override suspend fun load(message: MutableStateFlow<String>) {
        Context.init()

        println("======= MuPDF Version: "+Context.getVersion().version)

        // open the db
        message.value = "open the app db"
        DBHelper.help("${AppConfig.appPath}/${AppConfig.appName}.db")
            .createTable(FileMetadatas)
            .createTable(AppConfigs)
            .upgrade()

    }

    override suspend fun config(configKey: String, configValue: String) {
        transaction {
            val ids = AppConfigs.select { (AppConfigs.key.eq(configKey)) }
                .map { it[AppConfigs.id] }
                .toList()
            if (ids.isNotEmpty()) {
                AppConfigs.update({ AppConfigs.id.eq(ids.first()) }) {
                    it[value] = configValue
                }
            } else {
                AppConfigs.insert {
                    it[key] = configKey
                    it[value] = configValue
                }
            }
        }
    }

    override suspend fun getConfig(configKey: String): String {
        return transaction {
            val values = AppConfigs.select { AppConfigs.key.eq(configKey) }
                .map { it[AppConfigs.value] }
                .toList()
            if (values.isNotEmpty()) {
                values.first()
            } else {
                ""
            }
        }
    }
}