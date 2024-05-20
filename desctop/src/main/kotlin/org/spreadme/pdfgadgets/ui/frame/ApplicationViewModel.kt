package org.spreadme.pdfgadgets.ui.frame

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.WindowState
import com.itextpdf.kernel.exceptions.BadPasswordException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.spreadme.pdfgadgets.common.AppComponent
import org.spreadme.pdfgadgets.common.ViewModel
import org.spreadme.pdfgadgets.common.viewModelScope
import org.spreadme.pdfgadgets.config.AppConfig
import org.spreadme.pdfgadgets.model.FileMetadata
import org.spreadme.pdfgadgets.model.OpenProperties
import org.spreadme.pdfgadgets.model.PdfMetadata
import org.spreadme.pdfgadgets.repository.AppConfigRepository
import org.spreadme.pdfgadgets.repository.FileMetadataParser
import org.spreadme.pdfgadgets.repository.FileMetadataRepository
import org.spreadme.pdfgadgets.repository.PdfMetadataParser
import org.spreadme.pdfgadgets.ui.home.HomeComponent
import org.spreadme.pdfgadgets.ui.pdfview.PdfViewAppComponent
import org.spreadme.pdfgadgets.ui.tool.ASN1ParseViewComponent
import java.nio.file.Path
import kotlin.system.exitProcess

class ApplicationViewModel(
    private val appConfigRepository: AppConfigRepository,
    private val fileMetadataRepository: FileMetadataRepository,
    private val fileMetadataParser: FileMetadataParser,
    private val pdfMetadataParser: PdfMetadataParser
) : ViewModel() {

    private val logger = KotlinLogging.logger {}

    var composeWindow = ComposeWindow()

    val components = mutableStateListOf<AppComponent>()
    var currentComponent by mutableStateOf<AppComponent?>(null)

    //application load status
    var bootFinished by mutableStateOf(false)
    var bootMessage = MutableStateFlow("")

    //UI State
    var windowState = WindowState()
    var isDark by AppConfig.isDark

    var tabbarPaddingStart = 0
    var tabbarPaddingEnd = 16
    val addIconSize = 32

    // TODO re calculate the tab width
    fun onWindowStateChange(size: DpSize) {
    }

    fun onSelectTab(selectedComponent: AppComponent) {
        currentComponent = selectedComponent
    }

    fun onCloseTab(closeComponent: AppComponent) {
        closeComponent.close()
        val index = components.indexOf(closeComponent)
        currentComponent = if (index == 0 && components.size == 1) {
            components.remove(closeComponent)
            exitProcess(0)
        } else if (index == 0) {
            components[1]
        } else {
            components[index - 1]
        }
        components.remove(closeComponent)
    }

    fun newBlankTab() {
        val homeComponent = HomeComponent(this)
        components.add(homeComponent)
        currentComponent = homeComponent
    }

    private fun openCurrentTab(component: AppComponent) {
        components.add(component)
        if (currentComponent is HomeComponent) {
            components.remove(currentComponent)
        }
        currentComponent = component
    }

    fun calculateTabWidth(): Float {
        var tabWidth = 168f
        val windowWidth =
            windowState.size.width.value - (tabbarPaddingStart + tabbarPaddingEnd + addIconSize)
        if ((components.size + 1) * tabWidth > windowWidth) {
            tabWidth = (windowWidth / (components.size + 1))
        }
        return tabWidth
    }

    /**
     * @param path the file path
     * @param progressViewModel progrss view model
     * @param openProperties open properties,
     * first parse [PdfMetadata] by [PdfMetadataParser], when load finished then render the [PdfViewAppComponent]
     */
    fun openFile(
        path: Path,
        progressViewModel: LoadProgressViewModel,
        openProperties: OpenProperties = OpenProperties()
    ) {
        progressViewModel.start()
        viewModelScope.launch {
            try {
                val fileMetadata = fileMetadataParser.parse(path)
                fileMetadata.openProperties = openProperties
                val pdfMetadata = pdfMetadataParser.parse(fileMetadata)
                fileMetadataRepository.save(fileMetadata)
                val appComponent = PdfViewAppComponent(pdfMetadata, this@ApplicationViewModel)
                openCurrentTab(appComponent)
                progressViewModel.status = LoadProgressStatus.FINISHED

            } catch (bpe: BadPasswordException) {
                val message = if (openProperties.password == null) {
                    "文档已被保护，请输入文档保护口令"
                } else {
                    "文档保护口令不正确"
                }
                progressViewModel.needPassword(path, message)
            } catch (e: Exception) {
                logger.error(e.message, e)
                fileMetadataRepository.deleteByPath(path)
                val message = when (e) {
                    is java.nio.file.NoSuchFileException -> "文件已被删除或被转移"
                    else -> e.message ?: "Pdf文件解析失败"
                }
                progressViewModel.fail(message)
            }
        }
    }

    fun openASN1Parser() {
        val asn1ParseViewComponent = ASN1ParseViewComponent()
        openCurrentTab(asn1ParseViewComponent)
    }


    fun config(configKey: String, configValue: String) {
        viewModelScope.launch {
            appConfigRepository.config(configKey, configValue)
        }
    }

    fun bootstrap() {
        viewModelScope.launch {
            appConfigRepository.load(this@ApplicationViewModel.bootMessage)
            newBlankTab()
            this@ApplicationViewModel.bootFinished = true
        }
    }

    override fun onCleared() {
        components.forEach {
            it.close()
        }
    }
}