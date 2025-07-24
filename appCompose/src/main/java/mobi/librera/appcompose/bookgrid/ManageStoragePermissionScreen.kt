package mobi.librera.appcompose.bookgrid


import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun ManageStoragePermissionScreen() {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(false) }
    var showRationaleDialog by remember { mutableStateOf(false) }

    // Launcher for the "All files access" settings screen
    val manageStorageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        // When user returns from settings, re-check the permission
        hasPermission = isManageExternalStorageGranted(context)
        if (hasPermission) {
            // Permission granted, you can now proceed with file operations
            println("MANAGE_EXTERNAL_STORAGE permission granted.")
            // Optionally, navigate away or enable functionality here
        } else {
            // Permission still not granted
            println("MANAGE_EXTERNAL_STORAGE permission NOT granted.")
        }
    }

    // Lifecycle observer to re-check permission when the app resumes
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Re-check permission every time the app comes to foreground
                hasPermission = isManageExternalStorageGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Initial check when the composable first appears
    LaunchedEffect(Unit) {
        hasPermission = isManageExternalStorageGranted(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (hasPermission) {
            Text(
                text = "MANAGE_EXTERNAL_STORAGE Granted!",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = "You can now perform operations requiring full file system access.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            // Your app's main content or functionality that requires this permission
            Button(onClick = { /* Perform file operations */ }) {
                Text("Perform File Operations")
            }
        } else {
            Text(
                text = "MANAGE_EXTERNAL_STORAGE Required",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = "This application requires 'All files access' to function correctly. Please grant this permission in settings.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            Button(onClick = {
                // Show rationale if needed, then launch settings
                showRationaleDialog = true
            }) {
                Text("Grant Permission")
            }
        }
    }

    if (showRationaleDialog && !hasPermission) {
        AlertDialog(
            onDismissRequest = { showRationaleDialog = false },
            title = { Text("Permission Required") },
            text = { Text("To manage files on your device, please enable 'All files access' for this app in the next screen.") },
            confirmButton = {
                Button(onClick = {
                    showRationaleDialog = false
                    // Create an Intent to open the "All files access" settings screen
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.fromParts("package", context.packageName, null)
                    manageStorageLauncher.launch(intent)
                }) {
                    Text("Go to Settings")
                }
            },
            dismissButton = {
                Button(onClick = { showRationaleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Checks if the MANAGE_EXTERNAL_STORAGE permission is granted.
 * This permission is only available on Android 11 (API 30) and above.
 */
fun isManageExternalStorageGranted(context: android.content.Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        // For devices below Android 11, this permission is not applicable in the same way.
        // You would typically rely on READ_EXTERNAL_STORAGE and WRITE_EXTERNAL_STORAGE
        // which are handled differently (runtime permissions).
        // For simplicity, we'll return false if not on R or higher, assuming the app
        // specifically needs this broad access.
        false
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewManageStoragePermissionScreen() {
    MaterialTheme {
        ManageStoragePermissionScreen()
    }
}