package mobi.librera.appcompose.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import mobi.librera.appcompose.model.DataModel
import mobi.librera.appcompose.ui.theme.LibreraTheme

@Composable
fun BookSearchBar(dataModel: DataModel) {
    val searchQuery by dataModel.currentSearchQuery.collectAsState()
    val books by dataModel.getAllBooks.collectAsState()


    var showPopup by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .padding(top = 4.dp, bottom = 4.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {


        OutlinedTextField(
            value = searchQuery,
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .height(50.dp)
                .padding(start = 8.dp)
                .minimumInteractiveComponentSize(),
            shape = CircleShape,
            textStyle = TextStyle(fontSize = 14.sp),

            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    modifier = Modifier.padding(start = 16.dp),
                )
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            dataModel.updateSearchQuery("")
                        }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                            )
                        }
                    }

                    Text(
                        text = "${books.size}",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            },
            onValueChange = { dataModel.updateSearchQuery(it) },
            placeholder = { Text("Search...", style = MaterialTheme.typography.labelMedium) },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),

            )

        IconButton(onClick = {
            showPopup = true
        }, modifier = Modifier) {
            Icon(
                imageVector = Icons.Outlined.Settings, contentDescription = ""
            )
        }

    }


    if (showPopup) {
        Dialog(onDismissRequest = { showPopup = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    Modifier
                        .padding(10.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Books", style = MaterialTheme.typography.labelMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            dataModel.isSearchEPUB,
                            onCheckedChange = { dataModel.isSearchEPUB = it })
                        Text("EPUB")
                        Spacer(modifier = Modifier.width(20.dp))
                        Checkbox(
                            dataModel.isSearchPDF, onCheckedChange = { dataModel.isSearchPDF = it })
                        Text("PDF")
                    }
                    Button(onClick = {
                        dataModel.searchBooks()
                        showPopup = false
                    }) {
                        Text("Update search")
                    }

                    val isDark by dataModel.isDarkModeEnabled.collectAsState()

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            isDark,
                            onCheckedChange = {
                                dataModel.updateDarkMode(it)
                                showPopup = false
                            })
                        Text("Dark mode")
                    }

                }
            }
        }
    }


}

@Preview(showBackground = false)
@Composable
private fun BookSearchBarPreview() {
    LibreraTheme {
        ///BookSearchBar(bookCount = 12, onTextChanged = {}, onSearchBook = {})
    }
}