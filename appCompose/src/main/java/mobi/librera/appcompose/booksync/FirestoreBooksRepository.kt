package mobi.librera.appcompose.booksync

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import mobi.librera.appcompose.App
import mobi.librera.appcompose.room.BookState

val KEY_USERS = "users"
val KEY_BOOKS = "books"

object FirestoreBooksRepository {
    private val db = Firebase.firestore
    private val collection = db.collection("book_state")

    fun listenToNotes(onChange: (List<BookState>) -> Unit) {
        Firebase.auth.currentUser?.let { user ->
            db.collection(KEY_USERS)
                .document(user.uid)
                .collection(KEY_BOOKS)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && !snapshot.isEmpty) {
                        val books = snapshot.toObjects(BookState::class.java)
                        onChange(books)
                    } else {
                        onChange(emptyList())
                    }
                }
        }
    }

    fun syncBook(book: BookState) {
        if (book.bookPaths[App.DEVICE_ID].isNullOrEmpty()) {
            println("Sync Book skip")
            return
        }
        println("Sync Book $book")

        Firebase.auth.currentUser?.let { user ->
            db.collection(KEY_USERS)
                .document(user.uid)
                .collection(KEY_BOOKS)
                .document(book.fileName)
                .set(book)
                .addOnSuccessListener { documentReference ->
                    println("Sync success")
                }
                .addOnFailureListener { e ->
                    println("Sync fail ${e.printStackTrace()}")
                }
        }
    }
}