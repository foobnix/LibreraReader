package org.spreadme.pdfgadgets.ui.pdfview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.itextpdf.kernel.pdf.PdfName
import com.itextpdf.kernel.pdf.PdfStream
import com.itextpdf.kernel.pdf.PdfString
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.koin.core.component.inject
import org.spreadme.pdfgadgets.common.AppComponent
import org.spreadme.pdfgadgets.model.PdfMetadata
import org.spreadme.pdfgadgets.model.StructureNode
import org.spreadme.pdfgadgets.repository.ASN1Parser
import org.spreadme.pdfgadgets.repository.PdfStreamParser
import org.spreadme.pdfgadgets.repository.PdfTextSearcher
import org.spreadme.pdfgadgets.ui.frame.ApplicationViewModel
import org.spreadme.pdfgadgets.ui.frame.MainApplicationFrame
import org.spreadme.pdfgadgets.ui.sidepanel.SidePanel
import org.spreadme.pdfgadgets.ui.sidepanel.SidePanelMode
import org.spreadme.pdfgadgets.ui.streamview.*
import org.spreadme.pdfgadgets.ui.toolbars.Toolbars
import org.spreadme.pdfgadgets.ui.toolbars.ToolbarsViewModel

class PdfViewAppComponent(
    pdfMetadata: PdfMetadata,
    private val applicationViewModel: ApplicationViewModel
) : AppComponent(pdfMetadata.fileMetadata.name) {

    private val logger = KotlinLogging.logger { }

    private val streamParser by inject<PdfStreamParser>()
    private val asN1Parser by inject<ASN1Parser>()
    private val pdfTextSearcher by inject<PdfTextSearcher>()

    private val toolbarsViewModel = getViewModel<ToolbarsViewModel>(true)
    private val streamPanelViewModel = getViewModel<StreamPanelViewModel>()

    private val pdfViewModel: PdfViewModel

    init {
        val pageViewModels = pdfMetadata.pages.map { getViewModel<PageViewModel>(it, single = false) }.toList()
        pdfViewModel = getViewModel(pdfMetadata, pageViewModels, pdfTextSearcher)
    }

    @Composable
    override fun onRender() {
        MainApplicationFrame {
            logger.debug("pdf view component【${name}】rendered")
            Column {
                Toolbars(
                    modifier = Modifier.fillMaxWidth().height(48.dp).background(MaterialTheme.colors.background),
                    toolbarsViewModel = toolbarsViewModel
                )
                Row {
                    val pdfpdfViewModel = remember { pdfViewModel }
                    toolbarsViewModel.onChangeSideViewMode = pdfpdfViewModel::onChangeSideViewMode
                    toolbarsViewModel.onChangeScale = pdfpdfViewModel::onChangeScalue
                    toolbarsViewModel.onViewTypeChange = pdfpdfViewModel::onViewTypeChange
                    toolbarsViewModel.onSearch = pdfpdfViewModel::onSearch
                    toolbarsViewModel.onCleanSearch = pdfpdfViewModel::onCleanSeach
                    toolbarsViewModel.onScroll = pdfpdfViewModel::onScroll
                    SidePanelGroup(pdfpdfViewModel)
                    PageDetailGroup(pdfpdfViewModel)
                    StructureDetailPanel()
                }
            }
        }
    }

    @Composable
    fun RowScope.SidePanelGroup(pdfViewModel: PdfViewModel) {
        // PDF Info View Component
        if (pdfViewModel.hasSideView(SidePanelMode.INFO)) {
            DocumentAttrDetail(
                pdfViewModel.pdfMetadata.fileMetadata.name,
                pdfViewModel.pdfMetadata.documentInfo
            ) {
                pdfViewModel.onChangeSideViewMode(SidePanelMode.INFO)
            }
        }

        // PDF Bookmarks View Component
        AnimatedVisibility(pdfViewModel.hasSideView(SidePanelMode.OUTLINES)) {
            SidePanel(pdfViewModel.sideViewModel(SidePanelMode.OUTLINES)) {
                OutlinesTree(
                    it,
                    pdfViewModel.pdfMetadata.outlines,
                    pdfViewModel.pdfMetadata.pages,
                    pdfViewModel::onScroll
                )
            }
        }

        // PDF Structure View Component
        AnimatedVisibility(pdfViewModel.hasSideView(SidePanelMode.STRUCTURE)) {
            SidePanel(pdfViewModel.sideViewModel(SidePanelMode.STRUCTURE)) { sidePanelUIState ->
                StructureTree(pdfViewModel.pdfMetadata.structureRoot, sidePanelUIState) { node ->
                    if (node.isParseable()) {
                        val streamUIState = this@PdfViewAppComponent.getStreamViewUIState(node)
                        streamPanelViewModel.swicth(streamUIState)
                    }
                }
            }
        }

        // PDF Signature View Component
        AnimatedVisibility(pdfViewModel.hasSideView(SidePanelMode.SIGNATURE)) {
            SidePanel(pdfViewModel.sideViewModel(SidePanelMode.SIGNATURE)) {
                SignatureList(
                    pdfViewModel.pdfMetadata.signatures,
                    it,
                    { signatureContent ->
                        streamPanelViewModel.swicth(StreamASN1UIState(asN1Parser, signatureContent, StreamPanelViewType.SIGCONTENT))
                    },
                    pdfViewModel::onScroll
                )
            }
        }
    }

    @Composable
    fun RowScope.PageDetailGroup(
        pdfViewModel: PdfViewModel
    ) {
        val lazyListState = rememberLazyListState(pdfViewModel.initScrollIndex, pdfViewModel.initScrollOffset)
        val horizontalScollState = rememberScrollState(pdfViewModel.horizontalInitScollIndex)

        val coroutineScope = rememberCoroutineScope()
        if (pdfViewModel.scrollable) {
            coroutineScope.launch {
                lazyListState.scrollToItem(pdfViewModel.scrollIndex, pdfViewModel.scrollOffset)
                pdfViewModel.scrollFinish()
                pdfViewModel.scrollable = false
            }
        }

        Box(modifier = Modifier.weight(1f).background(MaterialTheme.colors.surface)) {
            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
                    .padding(end = 16.dp, bottom = 16.dp)
                    .horizontalScroll(horizontalScollState),
                state = lazyListState
            ) {
                items(pdfViewModel.pageViewModels) { item ->
                    PageDetail(item, pdfViewModel)
                }
            }

            pdfViewModel.initScrollIndex = lazyListState.firstVisibleItemIndex
            pdfViewModel.initScrollOffset = lazyListState.firstVisibleItemScrollOffset
            pdfViewModel.horizontalInitScollIndex = horizontalScollState.value

            Box(
                modifier = Modifier.padding(end = 16.dp, bottom = 16.dp)
                    .size(80.dp, 32.dp)
                    .background(MaterialTheme.colors.primary.copy(0.85f), RoundedCornerShape(4.dp))
                    .align(Alignment.BottomEnd)
                    .zIndex(2f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "${lazyListState.firstVisibleItemIndex + 1} / ${pdfViewModel.pdfMetadata.numberOfPages}",
                    style = MaterialTheme.typography.subtitle2,
                    color = MaterialTheme.colors.onPrimary
                )
            }

            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                adapter = rememberScrollbarAdapter(lazyListState)
            )
            HorizontalScrollbar(
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(end = 16.dp),
                adapter = rememberScrollbarAdapter(horizontalScollState)
            )
        }
    }

    @Composable
    fun RowScope.StructureDetailPanel() {
        AnimatedVisibility(streamPanelViewModel.enabled) {
            SidePanel(streamPanelViewModel.sidePanelUIState, true) {
                StreamPanel(streamPanelViewModel, mutableStateOf(applicationViewModel.isDark))
            }
        }
    }

    private fun getStreamViewUIState(node: StructureNode): StreamUIState? {
        return if (node.isPdfStream()) {
            val pdfStream = node.pdfObject as PdfStream
            if (pdfStream[PdfName.Subtype] == PdfName.Image) {
                StreamImageUIState(streamParser, node.pdfObject, StreamPanelViewType.IMAGE)
            } else if (pdfStream[PdfName.Subtype] == PdfName.XML) {
                StreamTextUIState(streamParser, node.pdfObject, StreamPanelViewType.XML)
            } else {
                StreamTextUIState(streamParser, node.pdfObject, StreamPanelViewType.DEFAULT)
            }
        } else if (node.isSignatureContent()) {
            StreamASN1UIState(asN1Parser, (node.pdfObject as PdfString).valueBytes, StreamPanelViewType.SIGCONTENT)
        } else {
            null
        }
    }
}