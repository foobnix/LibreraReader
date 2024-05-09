package org.spreadme.pdfgadgets.ui.toolbars

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.spreadme.pdfgadgets.config.AppConfigs
import org.spreadme.pdfgadgets.resources.R
import org.spreadme.pdfgadgets.ui.common.FileDialog
import org.spreadme.pdfgadgets.ui.frame.ApplicationViewModel
import org.spreadme.pdfgadgets.ui.frame.LoadProgressViewModel
import org.spreadme.common.choose

@Composable
fun ActionBar(
    applicationViewModel: ApplicationViewModel,
    progressViewModel: LoadProgressViewModel,
) {
    Column(
        Modifier.fillMaxHeight().width(56.dp)
            .background(MaterialTheme.colors.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ActionIcon(Modifier.padding(top = 16.dp), R.Icons.open) {
            FileDialog(
                parent = applicationViewModel.composeWindow,
                title = "title",
                exts = arrayListOf("pdf"),
                onFileOpen = {
                    applicationViewModel.openFile(
                        it,
                        progressViewModel
                    )
                }
            )
        }

        ActionIcon(Modifier.padding(top = 24.dp), R.Icons.decode) {
            applicationViewModel.openASN1Parser()
        }

        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ActionIcon(Modifier.padding(bottom = 16.dp), applicationViewModel.isDark.choose(R.Icons.dark, R.Icons.lignt)) {
                applicationViewModel.isDark = !applicationViewModel.isDark
                applicationViewModel.config(AppConfigs.DARK_CONFIG, applicationViewModel.isDark.toString())
            }
        }
    }
}

@Composable
fun ActionIcon(
    modifier: Modifier = Modifier,
    resource: String,
    onAction: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    Box(
        modifier.size(32.dp).clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colors.primary)
            .selectable(true) {
                onAction()
                focusManager.clearFocus()
            }
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(resource),
            contentDescription = "",
            tint = MaterialTheme.colors.onPrimary
        )
    }
}