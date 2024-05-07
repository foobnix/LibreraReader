package org.spreadme.pdfgadgets.ui.frame

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import org.spreadme.pdfgadgets.model.OpenProperties
import org.spreadme.pdfgadgets.ui.common.*
import org.spreadme.pdfgadgets.ui.theme.PDFGadgetsTheme

@Composable
fun AppFrameLoadProgress(loadProgressViewModel: LoadProgressViewModel, applicationViewModel: ApplicationViewModel) {
    when (loadProgressViewModel.status) {
        LoadProgressStatus.LOADING -> {
            LoadingModal()
        }

        LoadProgressStatus.FAILURE -> {
            FailureToast(loadProgressViewModel.message) {
                loadProgressViewModel.status = LoadProgressStatus.FINISHED
            }
        }

        LoadProgressStatus.NEED_PASSWORD -> {
            EnterPasswordDialog("", loadProgressViewModel.message) { password ->
                if (password.isNotBlank()) {
                    loadProgressViewModel.loadPath?.let {
                        val openProperties = OpenProperties()
                        openProperties.password = password.toByteArray()
                        applicationViewModel.openFile(it, loadProgressViewModel, openProperties)
                    }
                }
            }
        }

        else -> {}
    }
}

@Composable
fun LoadingModal() {
    Box(
        Modifier.fillMaxSize()
            .background(PDFGadgetsTheme.globalColors.background.copy(alpha = 0.8f))
            .zIndex(999f),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun FailureToast(message: String, onFinished: () -> Unit) {
    Box(
        Modifier.fillMaxSize().zIndex(999f).padding(top = 56.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Toast(
            message,
            ToastType.WARNING,
            onFinished = onFinished
        )
    }
}

@Composable
fun EnterPasswordDialog(
    title: String,
    message: String,
    onConfirm: (String) -> Unit
) {
    var enabled by remember { mutableStateOf(true) }
    if (enabled) {
        Dialog(
            onClose = {
                enabled = false
                onConfirm("")
            },
            title = title,
            resizable = false
        ) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "",
                    tint = PDFGadgetsTheme.extraColors.iconDisable,
                    modifier = Modifier.size(80.dp)
                )
            }
            Row(
                Modifier.fillMaxWidth().height(32.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    message,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onBackground
                )
            }
            Row(
                Modifier.fillMaxWidth().height(42.dp).padding(8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                var text by remember { mutableStateOf("") }
                TextInputField(
                    text,
                    modifier = Modifier.fillMaxWidth().height(32.dp)
                        .onBindKeyEvent(Key.Enter, onKeyDown = {
                            onConfirm(text)
                        })
                        .onBindKeyEvent(Key.Escape, onKeyDown = {
                            enabled = false
                        }),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.caption.copy(color = MaterialTheme.colors.onSurface),
                    visualTransformation = PasswordVisualTransformation(),
                    onValueChange = { text = it },
                    trailingIcon = {
                        AnimatedVisibility(text.isNotBlank()) {
                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = "",
                                tint = MaterialTheme.colors.primary,
                                modifier = Modifier.padding(horizontal = 8.dp).size(16.dp).clickable {
                                    onConfirm(text)
                                }
                            )
                        }
                    }
                )
            }
        }
    }
}