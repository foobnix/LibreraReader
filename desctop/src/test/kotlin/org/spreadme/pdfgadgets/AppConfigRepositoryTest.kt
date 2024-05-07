package org.spreadme.pdfgadgets

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.test.KoinTest
import org.koin.test.inject
import org.spreadme.pdfgadgets.di.appConfigLoadModule
import org.spreadme.pdfgadgets.repository.AppConfigRepository

class AppConfigRepositoryTest : KoinTest {

    private val appConfigRepository by inject<AppConfigRepository>()

    @Test
    fun testConfigLoad() {
        startKoin {
            modules(
                appConfigLoadModule
            )
        }

        runBlocking {
            appConfigRepository.load(MutableStateFlow(""))
        }
    }
}