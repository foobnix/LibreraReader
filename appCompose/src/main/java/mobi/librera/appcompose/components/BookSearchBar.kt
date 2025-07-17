package mobi.librera.appcompose.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import mobi.librera.appcompose.ui.theme.LibreraTheme

@Composable
fun BookSearchBar(bookCount: Int, onTextChanged: (String) -> Unit, onSearchBook: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(top = 4.dp, bottom = 4.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {


        var text by remember { mutableStateOf("") }
        LaunchedEffect(text) {
            onTextChanged(text)
        }

        OutlinedTextField(
            value = text,
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
                    if (text.isNotEmpty()) {
                        IconButton(onClick = {
                            text = ""
                        }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                            )
                        }
                    }

                    Text(
                        text = "$bookCount",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            },
            onValueChange = { text = it },
            placeholder = { Text("Search...", style = MaterialTheme.typography.labelMedium) },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),

            )

        IconButton(onClick = onSearchBook, modifier = Modifier) {
            Icon(
                imageVector = Icons.Outlined.Settings, contentDescription = ""
            )

        }

    }


}

@Preview(showBackground = false)
@Composable
private fun BookSearchBarPreview() {
    LibreraTheme {
        BookSearchBar(bookCount = 12, onTextChanged = {}, onSearchBook = {})
    }
}