package mobi.librera.appcompose.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NumberPickerDialog(
    showDialog: Boolean,
    onDismissRequest: () -> Unit,
    onNumberSelected: (Int) -> Unit,
    initialNumber: Int,
    range: IntRange = 0..100,
    itemHeight: Dp = 48.dp, // Height of each number row
    visibleItems: Int = 5 // Must be odd for proper centering (e.g., 5 means 2 above, current, 2 below)
) {
    if (!showDialog) return

    val numbers = remember { range.toList() }
    val listState = rememberLazyListState()

    // Calculate the index of the initial number to scroll to
    val initialIndex = remember(initialNumber, numbers) {
        numbers.indexOf(initialNumber).coerceAtLeast(0)
    }

    // State to hold the currently selected number (based on scroll position)
    var selectedNumber by remember { mutableIntStateOf(initialNumber) }

    // Calculate padding needed to center the selected item
    val halfVisibleItems = visibleItems / 2
    val contentPadding = PaddingValues(vertical = itemHeight * halfVisibleItems)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(listState, numbers) {
        // Scroll to the initial number, compensating for padding
        val targetScrollIndex = initialIndex - halfVisibleItems
        if (targetScrollIndex >= 0 && targetScrollIndex < numbers.size) {
            listState.scrollToItem(targetScrollIndex)
        }

        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .debounce(100) // Debounce to prevent rapid updates during scrolling
            .collect { (firstVisibleItemIndex, firstVisibleItemScrollOffset) ->
                // Calculate the true center item index
                val centerIndex = firstVisibleItemIndex + halfVisibleItems
                if (centerIndex >= 0 && centerIndex < numbers.size) {
                    selectedNumber = numbers[centerIndex]
                }
            }
    }


    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Select a Number",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Box(
                    modifier = Modifier
                        .height(itemHeight * visibleItems) // Set height based on visible items
                        .width(100.dp) // Fixed width for the picker
                        .background(Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                ) {
                    // Highlight for the selected item
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(itemHeight)
                            .align(Alignment.Center)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                RoundedCornerShape(4.dp)
                            )
                    )


                    LazyColumn(
                        state = listState,
                        contentPadding = contentPadding, // Apply padding to center content
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(numbers) { index, number ->
                            val isSelected = remember {
                                derivedStateOf { number == selectedNumber }
                            }.value

                            Text(
                                text = number.toString(),
                                style = if (isSelected) MaterialTheme.typography.headlineMedium.copy(
                                    color = MaterialTheme.colorScheme.primary
                                )
                                else MaterialTheme.typography.bodyLarge.copy(color = Color.Black),
                                modifier = Modifier
                                    .height(itemHeight)
                                    .fillMaxWidth()
                                    .clickable {
                                        // Scroll to clicked item, centering it
                                        val targetScrollIndex = index - halfVisibleItems
                                        if (targetScrollIndex >= 0 && targetScrollIndex < numbers.size) {
                                            coroutineScope.launch {
                                                listState.animateScrollToItem(targetScrollIndex)
                                            }
                                        }
                                    }
                                    .wrapContentHeight(align = Alignment.CenterVertically)
                                    .wrapContentWidth(align = Alignment.CenterHorizontally)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onDismissRequest,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onNumberSelected(selectedNumber) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}

// Example usage in your main Composable
@Composable
fun AppContent() {
    var showPicker by remember { mutableStateOf(false) }
    var selectedValue by remember { mutableIntStateOf(25) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(50.dp))
        Text("Selected Number: $selectedValue", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = { showPicker = true }) {
            Text("Open Number Picker")
        }

        NumberPickerDialog(
            showDialog = showPicker,
            onDismissRequest = { showPicker = false },
            onNumberSelected = { number ->
                selectedValue = number
                showPicker = false
            },
            initialNumber = selectedValue,
            range = 0..99
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewNumberPickerDialog() {
    AppContent()
}