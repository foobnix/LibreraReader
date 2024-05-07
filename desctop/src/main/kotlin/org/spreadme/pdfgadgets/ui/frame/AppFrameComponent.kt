package org.spreadme.pdfgadgets.ui.frame

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.core.component.inject
import org.spreadme.pdfgadgets.common.AppComponent
import org.spreadme.pdfgadgets.repository.AppConfigRepository
import org.spreadme.pdfgadgets.repository.FileMetadataParser
import org.spreadme.pdfgadgets.repository.FileMetadataRepository
import org.spreadme.pdfgadgets.repository.PdfMetadataParser
import org.spreadme.pdfgadgets.ui.tabbars.Tabbars
import org.spreadme.pdfgadgets.ui.theme.PDFGadgetsTheme
import org.spreadme.pdfgadgets.ui.toolbars.ActionBar

class AppFrameComponent : AppComponent("Application Frame") {

    private val appConfigRepository by inject<AppConfigRepository>()
    private val fileMetadataRepository by inject<FileMetadataRepository>()
    private val fileMetadataParser by inject<FileMetadataParser>()
    private val pdfMetadataParser by inject<PdfMetadataParser>()

    private val applicationViewModel = getViewModel<ApplicationViewModel>(
        appConfigRepository, fileMetadataRepository,
        fileMetadataParser, pdfMetadataParser
    )

    private val loadProgressViewModel = getViewModel<LoadProgressViewModel>()

    @Composable
    override fun onRender() {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colors.surface)) {
            Row {
                val progressState by remember { mutableStateOf(loadProgressViewModel) }
                AppFrameLoadProgress(progressState, applicationViewModel)

                ActionBar(applicationViewModel, progressState)
                Column {
                    //Tabs Bar
                    DefaultTabBars(applicationViewModel)
                    Divider(color = PDFGadgetsTheme.extraColors.border, thickness = 1.dp)
                    Box(
                        Modifier.fillMaxSize()
                    ) {
                        //Tabs View
                        TabView(applicationViewModel)
                    }
                }
            }
        }
    }

    fun viewModel(): ApplicationViewModel = applicationViewModel
}

@Composable
fun DefaultTabBars(
    applicationViewModel: ApplicationViewModel
) {
    Tabbars(
        applicationViewModel.components,
        applicationViewModel.currentComponent,
        Modifier.fillMaxWidth().height(30.dp).background(MaterialTheme.colors.background)
            .padding(
                start = applicationViewModel.tabbarPaddingStart.dp,
                end = applicationViewModel.tabbarPaddingEnd.dp
            ),
        applicationViewModel::calculateTabWidth,
        addIconSize = applicationViewModel.addIconSize,
        onSelected = applicationViewModel::onSelectTab,
        onClose = applicationViewModel::onCloseTab,
    ) {
        applicationViewModel.newBlankTab()
        applicationViewModel.calculateTabWidth()
    }
}

@Composable
fun TabView(frameViewModel: ApplicationViewModel) {
    frameViewModel.currentComponent?.render()
}