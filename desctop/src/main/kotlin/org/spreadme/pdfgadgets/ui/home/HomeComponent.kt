package org.spreadme.pdfgadgets.ui.home

import org.spreadme.pdfgadgets.ui.frame.AppFrameLoadProgress
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import mu.KotlinLogging
import org.koin.core.component.inject
import org.spreadme.pdfgadgets.common.AppComponent
import org.spreadme.pdfgadgets.repository.FileMetadataRepository
import org.spreadme.pdfgadgets.ui.frame.ApplicationViewModel
import org.spreadme.pdfgadgets.ui.frame.LoadProgressViewModel
import org.spreadme.pdfgadgets.ui.frame.MainApplicationFrame

class HomeComponent(
    private val applicationViewModel: ApplicationViewModel
) : AppComponent("Library") {

    private val logger = KotlinLogging.logger {}

    private val fileMetadataRepository by inject<FileMetadataRepository>()

    private val loadProgressViewModel = getViewModel<LoadProgressViewModel>()
    private val recentFileViewModel = getViewModel<RecentFileViewModel>(fileMetadataRepository)
    private val searchFilesViewModel = getViewModel<SearchFilesViewModel>()

    @Composable
    override fun onRender() {
        loadProgressViewModel.onFail = {
            recentFileViewModel.reacquire()
        }

        val progressState by remember { mutableStateOf(loadProgressViewModel) }
        AppFrameLoadProgress(progressState, applicationViewModel)

        MainApplicationFrame {
            logger.info("home component【${uid}】rendered")

            Column(Modifier.fillMaxSize()) {
                searchFilesViewModel.load()

                SearchFiles(searchFilesViewModel) {
                    applicationViewModel.openFile(
                        it.path(),
                        progressState
                    )
                }
            }
        }
    }

}