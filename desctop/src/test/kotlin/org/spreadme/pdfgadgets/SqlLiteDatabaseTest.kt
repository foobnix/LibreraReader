package org.spreadme.pdfgadgets

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Test

class SqlLiteDatabaseTest {

    object Cities : Table() {
        val id = integer("id").autoIncrement() // Column<Int>
        val name = varchar("name", 50) // Column<String>
        val location = varchar("location", 200)

        override val primaryKey = PrimaryKey(id, name = "PK_Cities_ID")
    }

    @Test
    fun createDb() {
        Database.connect("jdbc:sqlite:sample.db")

        transaction {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(Cities)

            Cities.insert {
                it[name] = "tom"
                it[location] = "ShangHai CN"
            }
        }
    }
}