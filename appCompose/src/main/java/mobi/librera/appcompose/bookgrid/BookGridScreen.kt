package mobi.librera.appcompose.bookgrid

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat
import mobi.librera.appcompose.components.BookSearchBar
import mobi.librera.appcompose.media.fetchAllDownloadFiles

@Composable
fun BookGridScreen(
    dataModel: BookGridViewModel,
    onOpenBook: (String) -> Unit,
    onHomeClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        BookSearchBar(dataModel)

        var isManageStorageGranted by remember { mutableStateOf(Environment.isExternalStorageManager()) }

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) {
            isManageStorageGranted = Environment.isExternalStorageManager()
            if (isManageStorageGranted) {
                dataModel.loadInitialBooks()
            }
        }

        if (!isManageStorageGranted) {
            ManageStoragePermissionScreen(launcher)
        } else {
            //GoogleSignInScreen(stringResource(R.string.default_web_client_id))
            //GoogleDriveBrowserScreen()

//            SelectedBooksBar(
//                dataModel, false,
//                onOpenBook = onOpenBook,
//                onHomeClick = onHomeClick
//            )

            val context = LocalContext.current
            var list by remember { mutableStateOf(fetchAllDownloadFiles(context)) }

            val requestPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    list = fetchAllDownloadFiles(context)
                } else {

                }
            }

            fun openDirectory(pickerInitialUri: Uri) {
                // Choose a directory using the system's file picker.
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                    // Optionally, specify a URI for the directory that should be opened in
                    // the system file picker when it loads.
                    putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
                }

                
            }


            Button(onClick = {


                //requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    // Permission already granted
                    Toast.makeText(context, "Permission Already Granted", Toast.LENGTH_SHORT).show()
                } else {
                    // Request permission
                    requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }) {
                Text("add permission")
            }


            LazyColumn {
                items(list.size) { id ->
                    val item = list[id]
                    Card { Text(item.name) }
                }
            }


//            val books by dataModel.getAllBooks.collectAsState()
//
//            BookGrid(
//                dataModel,
//                books, dataModel.listGridStates,
//                onStarClicked = { dataModel.updateStar(it, !it.isSelected) },
//                onBookClicked = { onOpenBook(it.path) })
        }
    }
}