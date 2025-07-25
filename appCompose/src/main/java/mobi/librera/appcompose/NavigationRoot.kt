package mobi.librera.appcompose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import mobi.librera.appcompose.bookgrid.BookGridScreen
import mobi.librera.appcompose.bookgrid.BookGridViewModel
import mobi.librera.appcompose.bookread.ReadBookScreen
import org.koin.androidx.compose.koinViewModel


sealed class Route {
    @Serializable
    data object BookGridRoot

    @Serializable
    data class BookReadRoot(val bookPath: String)
}


@Composable
fun NavigationRoot(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Route.BookGridRoot
    ) {
        composable<Route.BookGridRoot> {
            val viewModel = koinViewModel<BookGridViewModel>()
            BookGridScreen(
                viewModel, onOpenBook = {
                    navController.navigate(Route.BookReadRoot(bookPath = it)) {
                        popUpTo(Route.BookGridRoot) {
                            inclusive = true
                        }
                    }

                },
                onHomeClick = {

                })
        }
        composable<Route.BookReadRoot> {
            val bookPath = it.toRoute<Route.BookReadRoot>().bookPath

            val viewModel = koinViewModel<BookGridViewModel>()
            viewModel.onSelectBook(bookPath)
            val selectedBook by viewModel.selectedBook.collectAsState()

            ReadBookScreen(
                viewModel,
                selectedBook,
                onBookClose = {
                    navController.navigate(Route.BookGridRoot)
                },
                onOpenBook = { book ->
                    viewModel.onSelectBook(book)
                }
            )
        }
    }
}