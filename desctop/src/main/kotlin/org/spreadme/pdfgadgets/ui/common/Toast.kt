package org.spreadme.pdfgadgets.ui.common

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.spreadme.pdfgadgets.ui.theme.PDFGadgetsTheme


@Composable
fun Toast(
    message: String,
    type: ToastType,
    timeout: Long = 2000,
    onFinished: () -> Unit = {}
) {
    val toastUI = when (type) {
        ToastType.SUCCESS -> ToastUI(
            PDFGadgetsTheme.extraColors.successBackground,
            PDFGadgetsTheme.extraColors.successBorder,
            PDFGadgetsTheme.extraColors.onSuccess,
            Icons.Default.Check
        )
        ToastType.WARNING -> ToastUI(
            PDFGadgetsTheme.extraColors.warningBackground,
            PDFGadgetsTheme.extraColors.warningBorder,
            PDFGadgetsTheme.extraColors.onWarning,
            Icons.Default.Warning
        )
        ToastType.ERROR -> ToastUI(
            PDFGadgetsTheme.extraColors.errorBackground,
            PDFGadgetsTheme.extraColors.errorBorder,
            PDFGadgetsTheme.extraColors.onError,
            Icons.Default.Error
        )
        ToastType.INFO -> ToastUI(
            MaterialTheme.colors.primary,
            MaterialTheme.colors.primaryVariant,
            MaterialTheme.colors.onPrimary,
            Icons.Default.Info
        )
    }
    Toast(
        modifier = Modifier.clip(RoundedCornerShape(8.dp))
            .background(toastUI.background)
            .border(1.dp, toastUI.border, RoundedCornerShape(8.dp))
            .padding(16.dp),
        mutableStateOf(true),
        timeout,
        onFinished = onFinished
    ) {
        Icon(
            toastUI.icon,
            contentDescription = "warning",
            tint = toastUI.onBackground,
            modifier = Modifier.padding(end = 8.dp).size(16.dp)
        )
        Text(
            message,
            color = toastUI.onBackground,
            style = MaterialTheme.typography.caption
        )
    }
}

@Composable
fun Toast(
    modifier: Modifier = Modifier,
    enabledState: MutableState<Boolean> = mutableStateOf(true),
    timeout: Long = 5000,
    onFinished: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    var enabled by remember { enabledState }
    AnimatedVisibility(
        enabled,
        enter = fadeIn() + expandIn(),
        exit = shrinkOut() + fadeOut()
    ) {
        Row(
            Modifier.wrapContentSize().then(modifier),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()
        }
    }

    if(timeout > 0) {
        MainScope().launch {
            delay(timeout)
            enabled = false
            onFinished()
        }
    }
}

enum class ToastType {
    INFO,
    SUCCESS,
    WARNING,
    ERROR
}

data class ToastUI(
    val background: Color,
    val border: Color,
    val onBackground: Color,
    val icon: ImageVector
)