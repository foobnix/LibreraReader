package mobi.librera.lib.gdrive

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream

class GoogleDriveHelper(private val context: Context) {

    companion object {
        private const val TAG = "GoogleDriveHelper"
        private val SCOPES = listOf(DriveScopes.DRIVE_FILE)
    }

    private var driveService: Drive? = null

    /**
     * Get Google Sign-In options
     */
    fun getGoogleSignInOptions(): GoogleSignInOptions {
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
    }

    /**
     * Initialize Drive service with signed-in account
     */
    fun initializeDriveService(account: GoogleSignInAccount?) {
        if (account == null) {
            Log.e(TAG, "Google account is null")
            return
        }

        try {
            val credential = GoogleAccountCredential.usingOAuth2(context, SCOPES)
            credential.selectedAccount = account.account

            driveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName("FileViewerApp")
                .build()

            Log.d(TAG, "Drive service initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Drive service", e)
        }
    }

    /**
     * Check if user is signed in to Google
     */
    @Suppress("DEPRECATION")
    fun isSignedIn(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return account != null && driveService != null
    }

    /**
     * Sign out from Google account
     */
    @Suppress("DEPRECATION")
    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val signInClient = GoogleSignIn.getClient(context, getGoogleSignInOptions())
            signInClient.signOut()
            driveService = null
            Log.d(TAG, "Successfully signed out from Google account")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sign out", e)
            Result.failure(e)
        }
    }

    /**
     * Upload file to Google Drive
     */
    suspend fun uploadFile(
        fileUri: Uri,
        fileName: String,
        mimeType: String
    ): Result<String> = uploadFileToFolder(fileUri, fileName, mimeType, "root")

    /**
     * Upload file to specific folder in Google Drive
     */
    suspend fun uploadFileToFolder(
        fileUri: Uri,
        fileName: String,
        mimeType: String,
        parentFolderId: String = "root"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: return@withContext Result.failure(
                Exception("Drive service not initialized. Please sign in first.")
            )

            Log.d(TAG, "Starting upload for file: $fileName")

            // Read file content
            val inputStream = context.contentResolver.openInputStream(fileUri)
                ?: return@withContext Result.failure(Exception("Could not open file"))

            val fileContent = inputStream.readBytes()
            inputStream.close()

            // Create file metadata
            val fileMetadata = File().apply {
                name = fileName
                parents = listOf(parentFolderId)
            }

            // Create media content
            val mediaContent = com.google.api.client.http.InputStreamContent(
                mimeType,
                java.io.ByteArrayInputStream(fileContent)
            )
            mediaContent.length = fileContent.size.toLong()

            // Upload the file
            val uploadedFile = service.files()
                .create(fileMetadata, mediaContent)
                .setFields("id, name, size, mimeType")
                .execute()

            Log.d(TAG, "File uploaded successfully: ${uploadedFile.name} (ID: ${uploadedFile.id})")
            Result.success(uploadedFile.id)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload file", e)
            Result.failure(e)
        }
    }

    /**
     * Get upload progress (placeholder for future implementation)
     */
    suspend fun uploadFileWithProgress(
        fileUri: Uri,
        fileName: String,
        mimeType: String,
        onProgress: (Int) -> Unit
    ): Result<String> {
        // For now, just call regular upload
        // In a real implementation, you would track upload progress
        onProgress(0)
        val result = uploadFile(fileUri, fileName, mimeType)
        onProgress(100)
        return result
    }

    /**
     * List files and folders in Google Drive
     */
    suspend fun listFiles(
        folderId: String = "root",
        pageSize: Int = 50
    ): Result<List<DriveFileItem>> = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: return@withContext Result.failure(
                Exception("Drive service not initialized. Please sign in first.")
            )

            Log.d(TAG, "Listing files in folder: $folderId")

            val query = "'$folderId' in parents and trashed=false"
            val result = service.files()
                .list()
                .setQ(query)
                .setPageSize(pageSize)
                .setFields("files(id,name,mimeType,size,modifiedTime,parents,webViewLink,thumbnailLink)")
                .execute()

            val driveFiles = result.files?.map { file ->
                DriveFileItem(
                    id = file.id,
                    name = file.name,
                    mimeType = file.mimeType,
                    size = file.size.toLong() ?: 0L,
                    modifiedTime = file.modifiedTime?.value ?: 0L,
                    isFolder = file.mimeType == "application/vnd.google-apps.folder",
                    webViewLink = file.webViewLink,
                    parentId = folderId,
                    coverLink = file.thumbnailLink // Use Google Drive's built-in thumbnail
                )
            } ?: emptyList()

            Log.d(TAG, "Found ${driveFiles.size} files in folder $folderId")
            Result.success(driveFiles)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to list files", e)
            Result.failure(e)
        }
    }

    /**
     * Find or create Books folder in Google Drive
     */
    suspend fun findOrCreateBooksFolder(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: return@withContext Result.failure(
                Exception("Drive service not initialized. Please sign in first.")
            )

            Log.d(TAG, "Looking for Books folder...")

            // Search for existing Books folder
            val query =
                "name='Books' and mimeType='application/vnd.google-apps.folder' and trashed=false"
            val searchResult = service.files()
                .list()
                .setQ(query)
                .setFields("files(id,name)")
                .execute()

            val existingFolder = searchResult.files?.firstOrNull()
            if (existingFolder != null) {
                Log.d(TAG, "Found existing Books folder: ${existingFolder.id}")
                return@withContext Result.success(existingFolder.id)
            }

            // Create Books folder if it doesn't exist
            Log.d(TAG, "Creating new Books folder...")
            val folderMetadata = File().apply {
                name = "Books"
                mimeType = "application/vnd.google-apps.folder"
                parents = listOf("root")
            }

            val createdFolder = service.files()
                .create(folderMetadata)
                .setFields("id,name")
                .execute()

            Log.d(TAG, "Created Books folder: ${createdFolder.id}")
            Result.success(createdFolder.id)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to find or create Books folder", e)
            Result.failure(e)
        }
    }

    /**
     * Upload file to Books folder in Google Drive with progress tracking
     */
    suspend fun uploadFileToBooks(
        fileUri: Uri,
        fileName: String,
        mimeType: String,
        onProgress: ((Int) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            onProgress?.invoke(10) // Finding/creating Books folder

            val booksFolderResult = findOrCreateBooksFolder()
            if (booksFolderResult.isFailure) {
                return@withContext Result.failure(
                    booksFolderResult.exceptionOrNull()
                        ?: Exception("Failed to access Books folder")
                )
            }

            onProgress?.invoke(30) // Starting book upload
            val booksFolderId = booksFolderResult.getOrThrow()
            val uploadResult = uploadFileToFolderWithProgress(
                fileUri,
                fileName,
                mimeType,
                booksFolderId
            ) { progress ->
                // Map upload progress to 30-80% range
                onProgress?.invoke(30 + (progress * 50 / 100))
            }

            if (uploadResult.isSuccess) {
                onProgress?.invoke(100) // Upload complete
            }

            return@withContext uploadResult

        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload file to Books folder", e)
            Result.failure(e)
        }
    }

    /**
     * Upload file to specific folder in Google Drive with progress tracking
     */
    suspend fun uploadFileToFolderWithProgress(
        fileUri: Uri,
        fileName: String,
        mimeType: String,
        parentFolderId: String = "root",
        onProgress: ((Int) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            onProgress?.invoke(0)
            val result = uploadFileToFolder(fileUri, fileName, mimeType, parentFolderId)
            onProgress?.invoke(100)
            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload file with progress", e)
            Result.failure(e)
        }
    }

    /**
     * Upload cover to Covers folder in Google Drive
     */
    suspend fun uploadCoverToCovers(
        coverUri: Uri,
        coverName: String
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val coversFolderResult = findOrCreateCoversFolder()
                if (coversFolderResult.isFailure) {
                    return@withContext Result.failure(
                        coversFolderResult.exceptionOrNull()
                            ?: Exception("Failed to access Covers folder")
                    )
                }
                val coversFolderId = coversFolderResult.getOrThrow()
                uploadFileToFolder(coverUri, coverName, "image/png", coversFolderId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload cover to Covers folder", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Find or create Covers folder in Google Drive
     */
    suspend fun findOrCreateCoversFolder(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: return@withContext Result.failure(
                Exception("Drive service not initialized. Please sign in first.")
            )

            Log.d(TAG, "Looking for Covers folder...")

            // Search for existing Covers folder
            val query =
                "name='Covers' and mimeType='application/vnd.google-apps.folder' and trashed=false"
            val searchResult = service.files()
                .list()
                .setQ(query)
                .setFields("files(id,name)")
                .execute()

            val existingFolder = searchResult.files?.firstOrNull()
            if (existingFolder != null) {
                Log.d(TAG, "Found existing Covers folder: ${existingFolder.id}")
                return@withContext Result.success(existingFolder.id)
            }

            // Create Covers folder if it doesn't exist
            Log.d(TAG, "Creating new Covers folder...")
            val folderMetadata = File().apply {
                name = "Covers"
                mimeType = "application/vnd.google-apps.folder"
                parents = listOf("root")
            }

            val createdFolder = service.files()
                .create(folderMetadata)
                .setFields("id,name")
                .execute()

            Log.d(TAG, "Created Covers folder: ${createdFolder.id}")
            Result.success(createdFolder.id)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to find or create Covers folder", e)
            Result.failure(e)
        }
    }

    /**
     * Get the cover URL for a given file (typically a book)
     */
    suspend fun getCoverUrlForFile(fileName: String): String? = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: return@withContext null

            // Find the Covers folder
            val coversFolderResult = findOrCreateCoversFolder()
            if (coversFolderResult.isFailure) {
                return@withContext null
            }
            val coversFolderId = coversFolderResult.getOrThrow()

            // Generate the cover name from the file name
            val coverName = CoverGenerator.generateCoverName(fileName) + ".png"

            // Search for the cover file in Covers folder
            val query = "name='$coverName' and '$coversFolderId' in parents and trashed=false"
            val result = service.files()
                .list()
                .setQ(query)
                .setFields("files(id,thumbnailLink)")
                .execute()

            val coverFile = result.files?.firstOrNull()
            if (coverFile != null) {
                Log.d(TAG, "Found cover for $fileName: ${coverFile.id}")
                // Use thumbnailLink for image files - it's a direct URL that works well with image loaders
                val thumbnailUrl = coverFile.thumbnailLink
                if (thumbnailUrl != null) {
                    Log.d(TAG, "Cover thumbnail URL: $thumbnailUrl")
                    return@withContext thumbnailUrl
                }

            }

            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get cover URL for file: $fileName", e)
            null
        }
    }

    /**
     * List files with covers (for Books folder)
     */
    suspend fun listFilesWithCovers(
        folderId: String = "root",
        pageSize: Int = 50
    ): Result<List<DriveFileItem>> = withContext(Dispatchers.IO) {
        try {
            // First, get the regular file list
            val filesResult = listFiles(folderId, pageSize)
            if (filesResult.isFailure) {
                return@withContext filesResult
            }

            val files = filesResult.getOrThrow()

            // Check if we're in the Books folder
            val isInBooksFolder = if (folderId != "root") {
                try {
                    val service = driveService ?: return@withContext Result.success(files)
                    val folderResult = service.files().get(folderId).setFields("name").execute()
                    folderResult.name == "Books"
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to check if in Books folder", e)
                    false
                }
            } else {
                false
            }

            if (!isInBooksFolder) {
                // If not in Books folder, return files as-is
                return@withContext Result.success(files)
            }

            // For each PDF file in the Books folder, try to find its cover
            val filesWithCovers = files.map { file ->
                if (!file.isFolder && file.mimeType == "application/pdf") {
                    val coverUrl = getCoverUrlForFile(file.name)
                    file.copy(coverLink = coverUrl)
                } else {
                    file
                }
            }

            Result.success(filesWithCovers)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list files with covers", e)
            Result.failure(e)
        }
    }

    suspend fun downloadFile(
        fileId: String,
        fileName: String
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: return@withContext Result.failure(
                Exception("Drive service not initialized. Please sign in first.")
            )

            Log.d(TAG, "Downloading file: $fileName (ID: $fileId)")

            val outputStream = ByteArrayOutputStream()
            service.files().get(fileId).executeMediaAndDownloadTo(outputStream)

            val fileContent = outputStream.toByteArray()
            Log.d(TAG, "Downloaded file: $fileName, size: ${fileContent.size} bytes")

            Result.success(fileContent)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to download file: $fileName", e)
            Result.failure(e)
        }
    }
}

// Extension function to read bytes from InputStream
private fun InputStream.readBytes(): ByteArray {
    val buffer = ByteArrayOutputStream()
    val data = ByteArray(1024)
    var nRead: Int
    while (read(data, 0, data.size).also { nRead = it } != -1) {
        buffer.write(data, 0, nRead)
    }
    return buffer.toByteArray()
}
