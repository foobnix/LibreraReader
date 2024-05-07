package org.spreadme.pdfgadgets.config.upgrade

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.spreadme.pdfgadgets.config.AppConfigs
import org.spreadme.pdfgadgets.repository.AppConfigRepository

class AppUpgradeRepository : KoinComponent {

    private val appConfigRepository by inject<AppConfigRepository>()

    suspend fun record(appUpgrade: AppUpgrade) {
        val names = this.getUpgradeNames()
        names.add(appUpgrade.name())
        appConfigRepository.config(AppConfigs.APP_UPGRADED_NAMES, names.joinToString(","))
    }

    suspend fun isUpgraded(appUpgrade: AppUpgrade): Boolean {
        val names = this.getUpgradeNames()
        return names.contains(appUpgrade.name())
    }

    private suspend fun getUpgradeNames(): MutableList<String> =
        appConfigRepository.getConfig(AppConfigs.APP_UPGRADED_NAMES)
            .split(",")
            .filter { it.isNotBlank() }
            .toMutableList()
}