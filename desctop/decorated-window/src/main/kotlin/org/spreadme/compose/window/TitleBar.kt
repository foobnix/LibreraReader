package org.spreadme.compose.window

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.*
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ParentDataModifierNode
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.*
import org.spreadme.common.OS
import org.spreadme.common.Platform
import org.spreadme.compose.window.styling.TitleBarStyle
import org.spreadme.compose.window.utils.macos.MacUtil
import java.awt.Window
import kotlin.math.max

internal const val TITLE_BAR_COMPONENT_LAYOUT_ID_PREFIX = "__TITLE_BAR_"

internal const val TITLE_BAR_LAYOUT_ID = "__TITLE_BAR_CONTENT__"

internal const val TITLE_BAR_BORDER_LAYOUT_ID = "__TITLE_BAR_BORDER__"

@Composable
fun DecoratedWindowScope.TitleBar(
    modifier: Modifier = Modifier,
    gradientStartColor: Color = Color.Unspecified,
    style: TitleBarStyle = defaultTitleBarStyle,
    content: @Composable TitleBarScope.(DecoratedWindowState) -> Unit,
) {
    when (Platform.os) {
        OS.Windows -> TitleBarOnWindows(modifier, gradientStartColor, style, window, state, content)
        OS.MacOs -> TitleBarOnMacOs(modifier, gradientStartColor, style, window, state, content)
        OS.Unknown -> error("TitleBar is not supported on this platform(${System.getProperty("os.name")})")
    }
}

@Composable
fun DecoratedDialogWindowScope.TitleBar(
    modifier: Modifier = Modifier,
    gradientStartColor: Color = Color.Unspecified,
    style: TitleBarStyle = defaultTitleBarStyle,
    content: @Composable TitleBarScope.(DecoratedWindowState) -> Unit,
) {
    when (Platform.os) {
        OS.Windows -> TitleBarOnWindows(modifier, gradientStartColor, style, window, state, content)
        OS.MacOs -> TitleBarOnMacOs(modifier, gradientStartColor, style, window, state, content)
        OS.Unknown -> error("TitleBar is not supported on this platform(${System.getProperty("os.name")})")
    }
}

@Composable
internal fun TitleBarImpl(
    modifier: Modifier = Modifier,
    gradientStartColor: Color = Color.Unspecified,
    style: TitleBarStyle = defaultTitleBarStyle,
    window: Window,
    state: DecoratedWindowState,
    applyTitleBar: (Dp, DecoratedWindowState) -> PaddingValues,
    content: @Composable TitleBarScope.(DecoratedWindowState) -> Unit,
) {
    val titleBarInfo = LocalTitleBarInfo.current

    val background by style.colors.backgroundFor(state)

    val density = LocalDensity.current

    val backgroundBrush = remember(background, gradientStartColor) {
        if (gradientStartColor.isUnspecified) {
            SolidColor(background)
        } else {
            with(density) {
                Brush.horizontalGradient(
                    0.0f to background,
                    0.5f to gradientStartColor,
                    1.0f to background,
                    startX = style.metrics.gradientStartX.toPx(),
                    endX = style.metrics.gradientEndX.toPx(),
                )
            }
        }
    }

    Layout(
        content = {
            val scope = TitleBarScopeImpl(titleBarInfo.title, titleBarInfo.icon)
            scope.content(state)
        },
        modifier = modifier.background(backgroundBrush)
            .focusProperties { canFocus = false }
            .layoutId(TITLE_BAR_LAYOUT_ID)
            .height(style.metrics.height)
            .onSizeChanged { with(density) { applyTitleBar(it.height.toDp(), state) } }
            .fillMaxWidth(),
        measurePolicy = rememberTitleBarMeasurePolicy(
            window,
            state,
            applyTitleBar,
        ),
    )

    Spacer(
        Modifier.layoutId(TITLE_BAR_BORDER_LAYOUT_ID)
            .height(1.dp)
            .fillMaxWidth()
            .background(style.colors.border),
    )
}

internal class TitleBarMeasurePolicy(
    private val window: Window,
    private val state: DecoratedWindowState,
    private val applyTitleBar: (Dp, DecoratedWindowState) -> PaddingValues,
) : MeasurePolicy {

    override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
        if (measurables.isEmpty()) {
            return layout(width = constraints.minWidth, height = constraints.minHeight) {}
        }

        var occupiedSpaceHorizontally = 0

        var maxSpaceVertically = constraints.minHeight
        val contentConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val measuredPlaceable = mutableListOf<Pair<Measurable, Placeable>>()

        for (it in measurables) {
            val placeable = it.measure(contentConstraints.offset(horizontal = -occupiedSpaceHorizontally))
            if (constraints.maxWidth < occupiedSpaceHorizontally + placeable.width) {
                break
            }
            occupiedSpaceHorizontally += placeable.width
            maxSpaceVertically = max(maxSpaceVertically, placeable.height)
            measuredPlaceable += it to placeable
        }

        val boxHeight = maxSpaceVertically

        val contentPadding = applyTitleBar(boxHeight.toDp(), state)

        val leftInset = contentPadding.calculateLeftPadding(layoutDirection).roundToPx()
        val rightInset = contentPadding.calculateRightPadding(layoutDirection).roundToPx()

        occupiedSpaceHorizontally += leftInset
        occupiedSpaceHorizontally += rightInset

        val boxWidth = maxOf(constraints.minWidth, occupiedSpaceHorizontally)

        return layout(boxWidth, boxHeight) {
            if (state.isFullscreen) {
                MacUtil.updateFullScreenButtons(window)
            }
            val placeableGroups =
                measuredPlaceable.groupBy { (measurable, _) ->
                    (measurable.parentData as? TitleBarChildDataNode)?.horizontalAlignment
                        ?: Alignment.CenterHorizontally
                }

            var headUsedSpace = leftInset
            var trailerUsedSpace = rightInset

            placeableGroups[Alignment.Start]?.forEach { (_, placeable) ->
                val x = headUsedSpace
                val y = Alignment.CenterVertically.align(placeable.height, boxHeight)
                placeable.placeRelative(x, y)
                headUsedSpace += placeable.width
            }
            placeableGroups[Alignment.End]?.forEach { (_, placeable) ->
                val x = boxWidth - placeable.width - trailerUsedSpace
                val y = Alignment.CenterVertically.align(placeable.height, boxHeight)
                placeable.placeRelative(x, y)
                trailerUsedSpace += placeable.width
            }

            val centerPlaceable = placeableGroups[Alignment.CenterHorizontally].orEmpty()

            val requiredCenterSpace = centerPlaceable.sumOf { it.second.width }
            val minX = headUsedSpace
            val maxX = boxWidth - trailerUsedSpace - requiredCenterSpace
            var centerX = (boxWidth - requiredCenterSpace) / 2

            if (minX <= maxX) {
                if (centerX > maxX) {
                    centerX = maxX
                }
                if (centerX < minX) {
                    centerX = minX
                }

                centerPlaceable.forEach { (_, placeable) ->
                    val x = centerX
                    val y = Alignment.CenterVertically.align(placeable.height, boxHeight)
                    placeable.placeRelative(x, y)
                    centerX += placeable.width
                }
            }
        }
    }
}

@Composable
internal fun rememberTitleBarMeasurePolicy(
    window: Window,
    state: DecoratedWindowState,
    applyTitleBar: (Dp, DecoratedWindowState) -> PaddingValues,
): MeasurePolicy =
    remember(window, state, applyTitleBar) {
        TitleBarMeasurePolicy(window, state, applyTitleBar)
    }

interface TitleBarScope {

    val title: String

    val icon: Painter?

    @Stable
    fun Modifier.align(alignment: Alignment.Horizontal): Modifier
}

private class TitleBarScopeImpl(
    override val title: String,
    override val icon: Painter?,
) : TitleBarScope {

    override fun Modifier.align(alignment: Alignment.Horizontal): Modifier =
        this then TitleBarChildDataElement(
            alignment,
            debugInspectorInfo {
                name = "align"
                value = alignment
            },
        )
}

private class TitleBarChildDataElement(
    val horizontalAlignment: Alignment.Horizontal,
    val inspectorInfo: InspectorInfo.() -> Unit = NoInspectorInfo,
) : ModifierNodeElement<TitleBarChildDataNode>(), InspectableValue {

    override fun create(): TitleBarChildDataNode = TitleBarChildDataNode(horizontalAlignment)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherModifier = other as? TitleBarChildDataElement ?: return false
        return horizontalAlignment == otherModifier.horizontalAlignment
    }

    override fun hashCode(): Int = horizontalAlignment.hashCode()

    override fun update(node: TitleBarChildDataNode) {
        node.horizontalAlignment = horizontalAlignment
    }

    override fun InspectorInfo.inspectableProperties() {
        inspectorInfo()
    }
}

private class TitleBarChildDataNode(
    var horizontalAlignment: Alignment.Horizontal,
) : ParentDataModifierNode, Modifier.Node() {

    override fun Density.modifyParentData(parentData: Any?) =
        this@TitleBarChildDataNode
}
