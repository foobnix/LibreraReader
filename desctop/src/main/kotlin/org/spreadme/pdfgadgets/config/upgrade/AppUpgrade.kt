package org.spreadme.pdfgadgets.config.upgrade

import org.spreadme.pdfgadgets.config.AppVersion

interface AppUpgrade {

    fun name(): String

    suspend fun upgrade()

    fun version(): AppVersion
}