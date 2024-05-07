package org.spreadme.pdfgadgets.ui.pdfview

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.spreadme.pdfgadgets.common.ViewModel
import org.spreadme.pdfgadgets.common.viewModelScope
import org.spreadme.pdfgadgets.model.PageMetadata
import org.spreadme.pdfgadgets.model.PageRenderInfo
import org.spreadme.pdfgadgets.model.Position

class PageViewModel(
    val page: PageMetadata
) : ViewModel() {

    private val logger = KotlinLogging.logger { }

    val enabled by mutableStateOf(true)
    var pageRenderInfo by mutableStateOf<PageRenderInfo?>(null)
    val searchPosition = mutableStateListOf<Position>()

    fun onRender(scale: Float) {
        viewModelScope.launch {
            pageRenderInfo = page.render(2f * scale)
        }
    }

    fun clearPage() {
        logger.debug("clean page[{}]", page.index)
        pageRenderInfo = null
    }
}

