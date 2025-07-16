package mobi.librera.appcompose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mobi.librera.appcompose.ui.theme.LibreraTheme

@Composable
fun BookSearchBar(bookCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Row(
            modifier = Modifier
                .weight(1f)
                .padding(8.dp)
                .background(color = MaterialTheme.colorScheme.background, shape = CircleShape),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                modifier = Modifier.padding(start = 16.dp),
            )

            Text(
                text = "Search ...",
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
            )

            Text(
                text = "$bookCount",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
            )


        }

        IconButton(onClick = {}, modifier = Modifier) {
            Icon(
                imageVector = Icons.Outlined.Settings, contentDescription = ""
            )
        }
    }

}

@Preview
@Composable
private fun BookSearchBarPreview() {
    LibreraTheme {
        BookSearchBar(bookCount = 12)
    }
}