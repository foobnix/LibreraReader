import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusGroup
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.modifier.*
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.platform.debugInspectorInfo
import java.awt.Component
import java.awt.Window
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.trackWindowActivation(window: Window): Modifier =
    composed(
        debugInspectorInfo {
            name = "activateRoot"
            properties["window"] = window
        },
    ) {
        var parentActivated by remember { mutableStateOf(false) }

        DisposableEffect(window) {
            val listener = object : WindowAdapter() {
                override fun windowActivated(e: WindowEvent?) {
                    parentActivated = true
                }

                override fun windowDeactivated(e: WindowEvent?) {
                    parentActivated = false
                }
            }
            window.addWindowListener(listener)
            onDispose { window.removeWindowListener(listener) }
        }
        Modifier.modifierLocalProvider(ModifierLocalActivated) { parentActivated }
    }

@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.trackComponentActivation(awtParent: Component): Modifier =
    composed(
        debugInspectorInfo {
            name = "activateRoot"
            properties["parent"] = awtParent
        },
    ) {
        var parentActivated by remember { mutableStateOf(false) }

        DisposableEffect(awtParent) {
            val listener = object : FocusListener {
                override fun focusGained(e: FocusEvent?) {
                    parentActivated = true
                }

                override fun focusLost(e: FocusEvent?) {
                    parentActivated = false
                }
            }
            awtParent.addFocusListener(listener)
            onDispose { awtParent.removeFocusListener(listener) }
        }

        Modifier.modifierLocalProvider(ModifierLocalActivated) { parentActivated }
    }

@OptIn(ExperimentalFoundationApi::class)
@Stable
fun Modifier.trackActivation(): Modifier =
    composed(
        debugInspectorInfo { name = "trackActivation" },
    ) {
        val activatedModifierLocal = remember { ActivatedModifierLocal() }
        Modifier.focusGroup()
            .onFocusChanged {
                if (it.hasFocus) {
                    activatedModifierLocal.childGainedFocus()
                } else {
                    activatedModifierLocal.childLostFocus()
                }
            }
            .then(activatedModifierLocal)
    }

private class ActivatedModifierLocal : ModifierLocalProvider<Boolean>, ModifierLocalConsumer {

    private var parentActivated: Boolean by mutableStateOf(false)

    private var hasFocus: Boolean by mutableStateOf(false)

    override fun onModifierLocalsUpdated(scope: ModifierLocalReadScope) {
        with(scope) { parentActivated = ModifierLocalActivated.current }
    }

    override val key: ProvidableModifierLocal<Boolean> = ModifierLocalActivated
    override val value: Boolean by derivedStateOf(structuralEqualityPolicy()) {
        parentActivated && hasFocus
    }

    fun childLostFocus() {
        hasFocus = false
    }

    fun childGainedFocus() {
        hasFocus = true
    }
}

val ModifierLocalActivated: ProvidableModifierLocal<Boolean> = modifierLocalOf { false }

fun Modifier.onActivated(
    enabled: Boolean = true,
    onChanged: (Boolean) -> Unit,
): Modifier =
    this then if (enabled) {
        ActivateChangedModifierElement(
            onChanged,
            debugInspectorInfo {
                name = "onActivated"
                properties["onChanged"] = onChanged
            },
        )
    } else {
        Modifier
    }

private class ActivateChangedModifierElement(
    private val onChanged: (Boolean) -> Unit,
    inspectorInfo: InspectorInfo.() -> Unit,
) : ModifierLocalConsumer, InspectorValueInfo(inspectorInfo) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ActivateChangedModifierElement) return false

        if (onChanged != other.onChanged) return false

        return true
    }

    override fun hashCode(): Int = onChanged.hashCode()

    private var currentActivated = false

    override fun onModifierLocalsUpdated(scope: ModifierLocalReadScope) {
        with(scope) {
            val activated = ModifierLocalActivated.current
            if (activated != currentActivated) {
                currentActivated = activated
                onChanged(activated)
            }
        }
    }
}
