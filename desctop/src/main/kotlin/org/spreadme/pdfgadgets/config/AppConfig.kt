package org.spreadme.pdfgadgets.config

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import org.jetbrains.exposed.sql.Table
import java.awt.image.BufferedImage
import java.nio.file.Path
import java.nio.file.Paths
import javax.imageio.ImageIO

object AppConfig {

    val appName: String = "PDFGadgets"
    val version: AppVersion = AppVersion.toVersion("1.0.0")
    val appPath: Path = Paths.get(System.getProperty("user.home"), ".pdfgadgets")

    var isDark: MutableState<Boolean> = mutableStateOf(false)

    fun appIcon(resourceId: String): BufferedImage {
        // Retrieving image
        val resourceFile = AppConfig::class.java.classLoader.getResourceAsStream(resourceId)
        val imageInput = ImageIO.read(resourceFile)

        val newImage = BufferedImage(
            imageInput.width,
            imageInput.height,
            BufferedImage.TYPE_INT_ARGB
        )

        // Drawing
        val canvas = newImage.createGraphics()
        canvas.drawImage(imageInput, 0, 0, null)
        canvas.dispose()

        return newImage
    }
}

object AppConfigs : Table("APP_CONFIGS") {

    const val DARK_CONFIG = "isDark"
    const val APP_UPGRADED_NAMES = "appUpgradedNames"

    val id = integer("id").autoIncrement()
    val key = varchar("configKey", 500)
    val value = text("configVey")

    override val primaryKey = PrimaryKey(AppConfigs.id, name = "PK_APP_CONFIGS_ID")

}