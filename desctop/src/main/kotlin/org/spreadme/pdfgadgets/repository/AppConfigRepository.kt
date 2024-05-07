package org.spreadme.pdfgadgets.repository

import kotlinx.coroutines.flow.MutableStateFlow

interface AppConfigRepository {

    suspend fun load(message: MutableStateFlow<String>)

    suspend fun config(configKey: String, configValue: String)

    suspend fun getConfig(configKey: String): String
}