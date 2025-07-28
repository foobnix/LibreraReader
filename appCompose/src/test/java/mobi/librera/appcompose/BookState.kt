package mobi.librera.appcompose

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Assuming these are your data classes and repositories
data class BookState(
    val fileName: String,
    var time: Long, // Use Long for timestamps
    var bookPaths: Set<String> // Using a Set to avoid duplicate paths
)

object FirestoreBooksRepository {
    // This should ideally return a Flow<List<BookState>> for continuous updates
    // For simplicity, I'm assuming listenToNotes provides updates
    fun listenToNotes(onUpdate: (List<BookState>) -> Unit) {
        // Mock implementation for demonstration
        // In a real app, this would be a Firestore listener
        // that calls onUpdate with the latest data.
        // For example:
        /*
        FirebaseFirestore.getInstance().collection("books")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    println("Listen failed: $e")
                    return@addSnapshotListener
                }
                val remoteBooks = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(BookState::class.java)
                } ?: emptyList()
                onUpdate(remoteBooks)
            }
        */

        // Simulating some updates for testing
        // This is not how a real Firestore listener works,
        // but it allows the code to compile and run for demonstration.
        CoroutineScope(Dispatchers.IO).launch {
            kotlinx.coroutines.delay(1000)
            onUpdate(
                listOf(
                    BookState(
                        "book1.pdf",
                        System.currentTimeMillis() - 5000,
                        setOf("/path/to/book1")
                    ),
                    BookState(
                        "book2.epub",
                        System.currentTimeMillis() - 2000,
                        setOf("/path/to/book2")
                    )
                )
            )
            kotlinx.coroutines.delay(3000)
            onUpdate(
                listOf(
                    BookState(
                        "book1.pdf",
                        System.currentTimeMillis(),
                        setOf("/path/to/book1", "/another/path/to/book1")
                    ),
                    BookState("book3.mobi", System.currentTimeMillis(), setOf("/path/to/book3"))
                )
            )
        }
    }

    suspend fun syncBook(book: BookState) {
        // In a real app, this would upload the book to Firestore
        println("Firestore Sync: Uploading ${book.fileName}")
        // Simulate network delay
        kotlinx.coroutines.delay(500)
    }
}

class BookRepository(private val database: Any) { // Replace Any with your actual Room DAO/Database type
    private val localBooks = mutableListOf<BookState>() // Mock for Room operations

    init {
        // Initialize with some mock data
        localBooks.add(
            BookState(
                "book1.pdf",
                System.currentTimeMillis() - 10000,
                setOf("/old/path/book1")
            )
        )
        localBooks.add(
            BookState(
                "book4.txt",
                System.currentTimeMillis() - 1000,
                setOf("/path/to/book4")
            )
        )
    }

    suspend fun getAllBookState(): List<BookState> {
        // Simulate Room database access
        kotlinx.coroutines.delay(100)
        return localBooks.toList()
    }

    suspend fun insertBookState(book: BookState) {
        // Simulate Room database insertion/update
        val existingIndex = localBooks.indexOfFirst { it.fileName == book.fileName }
        if (existingIndex != -1) {
            localBooks[existingIndex] = book
            println("Room DB: Updated ${book.fileName}")
        } else {
            localBooks.add(book)
            println("Room DB: Inserted ${book.fileName}")
        }
        kotlinx.coroutines.delay(50)
    }
}

// Assume App.DEVICE_ID exists
object App {
    const val DEVICE_ID = "my_device_123"
}


class SyncManager(
    private val bookRepository: BookRepository,
    private val viewModelScope: CoroutineScope // Pass a CoroutineScope (e.g., viewModelScope, lifecycleScope)
) {

    private fun observeFirestoreAndSyncToRoom() {
        println("Sync: ${App.DEVICE_ID}")

        FirestoreBooksRepository.listenToNotes { remoteBooks ->
            viewModelScope.launch(Dispatchers.IO) {
                val localBooks = bookRepository.getAllBookState()

                println("Sync remoteBooks count: ${remoteBooks.size}")
                println("Sync localBooks count: ${localBooks.size}")

                val allFileNames =
                    (localBooks.map { it.fileName } + remoteBooks.map { it.fileName }).toSet()

                allFileNames.forEach { fileName ->
                    val remoteBook = remoteBooks.find { it.fileName == fileName }
                    val localBook = localBooks.find { it.fileName == fileName }

                    when {
                        // Case 1: Book only exists remotely (new book from Firestore)
                        localBook == null && remoteBook != null -> {
                            bookRepository.insertBookState(remoteBook)
                            println("Sync: Local insert book from remote: ${remoteBook.fileName}")
                        }
                        // Case 2: Book only exists locally (new book from local device)
                        remoteBook == null && localBook != null -> {
                            FirestoreBooksRepository.syncBook(localBook)
                            println("Sync: Remote sync book from local: ${localBook.fileName}")
                        }
                        // Case 3: Book exists in both, resolve conflicts
                        remoteBook != null && localBook != null -> {
                            when {
                                // Timestamps are identical, no conflict, skip
                                remoteBook.time == localBook.time -> {
                                    println("Sync: Skip by time for ${fileName} (timestamps are equal)")
                                }
                                // Remote is newer, update local and merge bookPaths
                                remoteBook.time > localBook.time -> {
                                    val mergedBookPaths = remoteBook.bookPaths + localBook.bookPaths
                                    val updatedRemoteBook =
                                        remoteBook.copy(bookPaths = mergedBookPaths.toSet())
                                    bookRepository.insertBookState(updatedRemoteBook)
                                    println("Sync: Local update from newer remote: ${remoteBook.fileName}. Merged paths.")
                                }
                                // Local is newer, update remote and merge bookPaths
                                else -> { // localBook.time > remoteBook.time
                                    val mergedBookPaths = localBook.bookPaths + remoteBook.bookPaths
                                    val updatedLocalBook =
                                        localBook.copy(bookPaths = mergedBookPaths.toSet())
                                    FirestoreBooksRepository.syncBook(updatedLocalBook)
                                    println("Sync: Remote sync from newer local: ${localBook.fileName}. Merged paths.")
                                }
                            }
                        }
                        // This case should ideally not happen if allFileNames is derived correctly
                        else -> println("Sync: Should not reach here for $fileName (both null)")
                    }
                }
            }
        }
    }

    // You might want to expose a way to start observing
    fun startSyncing() {
        observeFirestoreAndSyncToRoom()
    }
}

// Example usage (e.g., in a ViewModel or Application class)
fun main() {
    val bookRepository =
        BookRepository(Any()) // Replace Any with your actual Room database instance
    val syncManager = SyncManager(
        bookRepository,
        CoroutineScope(Dispatchers.Default)
    ) // Using Dispatchers.Default for main function example

    syncManager.startSyncing()

    // Keep the main thread alive for a bit to see the async operations
    Thread.sleep(10000)
}