package org.spreadme.pdfgadgets.ui.pdfview

import androidx.compose.runtime.*
import org.spreadme.pdfgadgets.common.ViewModel
import org.spreadme.pdfgadgets.model.PdfMetadata
import org.spreadme.pdfgadgets.model.Position
import org.spreadme.pdfgadgets.repository.PdfTextSearcher
import org.spreadme.pdfgadgets.ui.sidepanel.SidePanelMode
import org.spreadme.pdfgadgets.ui.sidepanel.SidePanelUIState

class PdfViewModel(
    val pdfMetadata: PdfMetadata,
    val pageViewModels: List<PageViewModel>,
    private val pdfTextSearcher: PdfTextSearcher
) : ViewModel() {

    var scale by mutableStateOf(1.0f)
    var viewType by mutableStateOf(PdfViewType.ONLY_VIEW)

    var initScrollIndex = 0
    var initScrollOffset = 0
    var horizontalInitScollIndex = 0

    var scrollable by mutableStateOf(false)
    var scrollIndex by mutableStateOf(initScrollIndex)
    var scrollOffset by mutableStateOf(initScrollOffset)

    var scrollFinish: () -> Unit = {}

    private var sidePanelModes = mutableStateListOf(SidePanelMode.STRUCTURE)
    private val sidePanelModels = mutableStateMapOf<SidePanelMode, SidePanelUIState>()
    private val excludeModesMap = mutableMapOf<SidePanelMode, ArrayList<SidePanelMode>>()

    init {
        excludeModesMap[SidePanelMode.OUTLINES] = arrayListOf(SidePanelMode.STRUCTURE, SidePanelMode.SIGNATURE)
        excludeModesMap[SidePanelMode.STRUCTURE] = arrayListOf(SidePanelMode.OUTLINES, SidePanelMode.SIGNATURE)
        excludeModesMap[SidePanelMode.SIGNATURE] = arrayListOf(SidePanelMode.OUTLINES, SidePanelMode.STRUCTURE)

        SidePanelMode.entries.forEach {
            sidePanelModels[it] = SidePanelUIState()
        }

        setTagIfAbsent(PdfMetadata::class.java.canonicalName, pdfMetadata)
    }

    fun onChangeSideViewMode(sidePanelMode: SidePanelMode) {
        if (sidePanelModes.contains(sidePanelMode)) {
            sidePanelModes.remove(sidePanelMode)
        } else if (!sidePanelModes.contains(sidePanelMode)) {
            sidePanelModes.add(sidePanelMode)
            sidePanelModes.removeAll(excludeModesMap[sidePanelMode] ?: arrayListOf())
        }
    }

    fun sideViewModel(viewMode: SidePanelMode): SidePanelUIState {
        val sideViewState = sidePanelModels[viewMode]
        if (sideViewState != null) {
            return sideViewState
        }
        return SidePanelUIState()
    }

    fun hasSideView(viewMode: SidePanelMode): Boolean = sidePanelModes.contains(viewMode)

    fun onChangeScalue(scale: Float) {
        this.scale = scale
    }

    fun onViewTypeChange(viewType: PdfViewType) {
        this.viewType = viewType
    }

    fun onScroll(position: Position, scrollFinish: () -> Unit) {
        val offset = position.calculateScrollOffset()
        if (offset == Position.DISABLE) {
            return
        }
        this.scrollable = true
        this.scrollIndex = position.index - 1
        this.scrollOffset = offset
        this.scrollFinish = scrollFinish
    }

    fun onSearch(keyword: String): List<Position> {
        val positions = pdfTextSearcher.search(pdfMetadata, keyword)
        pageViewModels.forEach { pageViewModel ->
            pageViewModel.searchPosition.clear()
            pageViewModel.searchPosition.addAll(positions.filter { p -> p.index == pageViewModel.page.index })
        }
        return positions
    }

    fun onCleanSeach() {
        pageViewModels.forEach { pageViewModel ->
            pageViewModel.searchPosition.clear()
        }
    }
}