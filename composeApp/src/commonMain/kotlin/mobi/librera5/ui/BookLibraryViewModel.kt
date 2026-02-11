package mobi.librera5.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import mobi.librera5.initPDF
import mobi.librera5.model.Book
import mobi.librera5.model.BookType
import java.io.File

class BookLibraryViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow<BookLibraryUiState>(BookLibraryUiState.Empty)
    val uiState: StateFlow<BookLibraryUiState> = _uiState.asStateFlow()

    init {
        initPDF()
        scanFolder("/Users/ivanivanenko/Downloads/export/")
    }
    fun scanFolder(folderPath: String) {
        viewModelScope.launch {
            _uiState.value = BookLibraryUiState.Loading
            
            try {
                val books = withContext(Dispatchers.IO) {
                    scanBooksInFolder(folderPath)
                }
                
                _uiState.value = if (books.isEmpty()) {
                    BookLibraryUiState.Empty
                } else {
                    BookLibraryUiState.Success(books)
                }
            } catch (e: Exception) {
                _uiState.value = BookLibraryUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    private fun scanBooksInFolder(folderPath: String): List<Book> {
        val folder = File(folderPath)
        if (!folder.exists() || !folder.isDirectory) {
            return emptyList()
        }
        
        val books = mutableListOf<Book>()
        
        folder.walkTopDown().forEach { file ->
            if (file.isFile) {
                val extension = file.extension.lowercase()
                val bookType = when (extension) {
                    "pdf" -> BookType.PDF
                    "epub" -> BookType.EPUB
                    else -> null
                }
                
                if (bookType != null) {
                    books.add(
                        Book(
                            path = file.absolutePath,
                            title = file.nameWithoutExtension,
                            type = bookType
                        )
                    )
                }
            }
        }
        
        return books.sortedBy { it.title }
    }
}

sealed interface BookLibraryUiState {
    data object Empty : BookLibraryUiState
    data object Loading : BookLibraryUiState
    data class Success(val books: List<Book>) : BookLibraryUiState
    data class Error(val message: String) : BookLibraryUiState
}
