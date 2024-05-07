package org.spreadme.pdfgadgets

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.spreadme.compose.window.DecoratedWindow
import org.spreadme.pdfgadgets.common.Activity
import org.spreadme.pdfgadgets.common.ActivityIntent
import org.spreadme.pdfgadgets.config.AppConfig
import org.spreadme.pdfgadgets.resources.R
import org.spreadme.pdfgadgets.ui.frame.AppFrameComponent
import org.spreadme.pdfgadgets.ui.frame.ApplicationBootstrap
import org.spreadme.pdfgadgets.ui.frame.TitleBarView
import org.spreadme.pdfgadgets.ui.theme.PDFGadgetsTheme
import java.awt.Taskbar
import java.awt.Toolkit

class MainActivity : Activity() {

    companion object {
        fun getStartIntent(): ActivityIntent {
            return ActivityIntent(MainActivity::class).apply {
                System.setProperty("pdfgadgets.logdir", AppConfig.appPath.toString())
            }
        }
    }

    private val appFrameComponent = AppFrameComponent()
    private val appFrameViewModel = appFrameComponent.viewModel()
    private val applicationBootstrap = ApplicationBootstrap(appFrameViewModel)

    override fun onCreate() {
        if (Taskbar.isTaskbarSupported()) {
            val taskbar = Taskbar.getTaskbar()
            if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                Taskbar.getTaskbar().iconImage = AppConfig.appIcon(R.Drawables.appIcon)
            }
        }

        val screen = Toolkit.getDefaultToolkit().screenSize

        application {
            val windowState = rememberWindowState(width = (screen.width * .80).dp, height = (screen.height * .80).dp)

            PDFGadgetsTheme(appFrameViewModel.isDark) {
                DecoratedWindow(
                    onCloseRequest = {
                        onDestory()
                        exitApplication()
                    }, icon = painterResource(R.Drawables.appIcon), title = AppConfig.appName, state = windowState
                ) {
                    // listening the state of the window
                    LaunchedEffect(windowState) {
                        snapshotFlow { windowState.size }
                            .onEach {
                                appFrameViewModel.onWindowStateChange(it)
                            }.launchIn(this)
                    }

                    TitleBarView()

                    // load the window state
                    appFrameViewModel.composeWindow = window
                    appFrameViewModel.windowState = windowState

                    applicationBootstrap.bootstrap {
                        appFrameComponent.onRender()
                    }
                }
            }
        }
    }

    override fun onDestory() {
        appFrameComponent.close()
    }
}