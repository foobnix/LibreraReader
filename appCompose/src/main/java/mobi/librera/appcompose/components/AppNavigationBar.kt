package mobi.librera.appcompose.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun AppNavigationBar() {
    NavigationBar {
        NavigationBarItem(selected = true, onClick = {}, icon = {
            Icon(
                Icons.Filled.Home, contentDescription = ""
            )
        }, label = { Text("Library") })
        NavigationBarItem(selected = false, onClick = {}, icon = {
            Icon(
                Icons.Filled.DateRange, contentDescription = ""
            )
        }, label = { Text("Folder") })
        NavigationBarItem(selected = false, onClick = {}, icon = {
            Icon(
                Icons.Filled.Star, contentDescription = ""
            )
        }, label = { Text("Recent") })
    }
}