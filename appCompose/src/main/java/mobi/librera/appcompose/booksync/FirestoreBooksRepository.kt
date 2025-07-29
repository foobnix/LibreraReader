package mobi.librera.appcompose.booksync

import androidx.core.net.toUri
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import mobi.librera.appcompose.App
import mobi.librera.appcompose.imageloader.AppImageLoader
import mobi.librera.appcompose.room.BookState
import java.io.File
import kotlin.coroutines.resumeWithException

val KEY_USERS = "users"
val KEY_BOOKS = "books"

object FirestoreBooksRepository {
    private val db = Firebase.firestore
    private val collection = db.collection("book_state")

    fun listenToNotes(onChange: (List<BookState>) -> Unit) {
        Firebase.auth.currentUser?.let { user ->
            db.collection(KEY_USERS).document(user.uid).collection(KEY_BOOKS)
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

    suspend fun uploadImageToFirebase(
        book: File
    ): String = suspendCancellableCoroutine { continuation ->
        val storageRef = Firebase.storage
        val userUID = Firebase.auth.currentUser?.uid

        val imageRef = storageRef.getReference("users/$userUID/${book.name}")

        println("uploadImageToFirebase ${imageRef.path} $imageRef")

        val coverFile = AppImageLoader.get().getCacheFile(book.path)




        imageRef.putFile(coverFile.toUri()).addOnSuccessListener {
            imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                continuation.resume(downloadUrl.toString(), onCancellation = { a, b, c -> {} })
                //imaUrl(downloadUrl.toString())
                println("uploadImageToFirebase onSuccess |$downloadUrl")
            }
        }.addOnFailureListener { e ->
            println("uploadImageToFirebase onError ${e.message}")
            continuation.resumeWithException(e)

        }
    }

    fun syncBook(book: BookState) = runBlocking {
        if (book.bookPaths[App.DEVICE_ID].isNullOrEmpty()) {
            println("Sync Book skip")
            return@runBlocking
        }

        //  launch {
        if (book.imageUrl.isEmpty()) {
            val bookPath = book.bookPaths[App.DEVICE_ID]
            if (!bookPath.isNullOrEmpty()) {
                val coverFile = File(bookPath)
                println("uploadImageToFirebase coverFile ${coverFile.isFile}")
                if (coverFile.isFile) {
                    println("uploadImageToFirebase 2")

                    book.imageUrl = uploadImageToFirebase(coverFile)
                    println("Sync image url $book.imageUrl")


                }
            }
        }

        println("Sync Book $book")
        Firebase.auth.currentUser?.let { user ->
            db.collection(KEY_USERS).document(user.uid).collection(KEY_BOOKS)
                .document(book.fileName).set(book).addOnSuccessListener { documentReference ->
                    println("Sync success")
                }.addOnFailureListener { e ->
                    println("Sync fail ${e.printStackTrace()}")
                }
        }
    }


}