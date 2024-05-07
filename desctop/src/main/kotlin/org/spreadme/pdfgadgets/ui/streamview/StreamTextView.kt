package org.spreadme.pdfgadgets.ui.streamview

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import org.spreadme.pdfgadgets.model.PdfStreamTokenSequence
import org.spreadme.pdfgadgets.model.PdfStreamTokenType
import org.spreadme.pdfgadgets.ui.theme.FiraCode
import org.spreadme.pdfgadgets.ui.theme.LocalStreamKeywordColors

@Composable
fun StreamTextView(
    textViewState: StreamTextUIState
) {
    Box(modifier = Modifier.fillMaxSize()) {
        val horizontalScollState = rememberScrollState()
        val lazyListState = rememberLazyListState()

        LazyColumn(
            Modifier.fillMaxSize().horizontalScroll(horizontalScollState),
            state = lazyListState
        ) {
            items(textViewState.streamTokenSequences) { item ->
                val annotatedString = annotatedString(item)
                Box(Modifier.height(
                    with(LocalDensity.current) { textViewState.fontSize.toDp() } * 1.6f)
                ) {
                    Line(annotatedString, textViewState, Modifier.align(Alignment.CenterStart))
                }
            }
        }

        HorizontalScrollbar(
            modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(end = 16.dp),
            adapter = rememberScrollbarAdapter(horizontalScollState)
        )
        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(lazyListState)
        )
    }
}

@Composable
private fun annotatedString(pdfStreamTokenSequence: PdfStreamTokenSequence): AnnotatedString =
    buildAnnotatedString {
        pdfStreamTokenSequence.tokens.forEach {
            append(
                buildAnnotatedString {
                    val color = when (it.tokenType) {
                        PdfStreamTokenType.operator -> LocalStreamKeywordColors.current.operator
                        PdfStreamTokenType.number -> LocalStreamKeywordColors.current.number
                        PdfStreamTokenType.string -> LocalStreamKeywordColors.current.string
                        PdfStreamTokenType.escape -> LocalStreamKeywordColors.current.escape
                        PdfStreamTokenType.tname -> LocalStreamKeywordColors.current.name
                        PdfStreamTokenType.indent -> Color.Transparent

                    }
                    withStyle(style = SpanStyle(color)) {
                        append(it.token)
                    }
                }
            )
        }
    }

@Composable
private fun Line(content: AnnotatedString, textViewState: StreamTextUIState, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        SelectionContainer {
            StreamLineContent(
                content,
                textViewState,
                modifier = Modifier
                    .padding(start = 28.dp, end = 12.dp),
            )
        }
    }
}

@Composable
private fun StreamLineContent(
    content: AnnotatedString,
    textViewState: StreamTextUIState,
    modifier: Modifier
) = Text(
    content,
    fontSize = textViewState.fontSize,
    fontFamily = FiraCode,
    overflow = TextOverflow.Visible,
    modifier = modifier
)