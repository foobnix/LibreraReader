package org.spreadme.pdfgadgets.config

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.spreadme.pdfgadgets.config.upgrade.AppUpgrade
import org.spreadme.pdfgadgets.config.upgrade.AppUpgradeRepository

object DBHelper : KoinComponent {

    private val appUpgradeRepository by inject<AppUpgradeRepository>()

    suspend fun help(name: String): DBHelper {
        withContext(Dispatchers.IO) {
            Database.connect("jdbc:sqlite:$name")
        }
        return this
    }

    suspend fun createTable(table: Table): DBHelper {
        withContext(Dispatchers.IO) {
            transaction {
                addLogger(StdOutSqlLogger)
                SchemaUtils.create(table)
            }
        }
        return this
    }

    suspend fun upgrade() {
        withContext(Dispatchers.IO) {
            val upgrades = getKoin().getAll<AppUpgrade>()
            upgrades.forEach {
                if (it.version() >= AppConfig.version && !appUpgradeRepository.isUpgraded(it)) {
                    it.upgrade()
                    appUpgradeRepository.record(it)
                }
            }
        }
    }
}