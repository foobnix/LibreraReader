package org.spreadme.pdfgadgets.model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.itextpdf.kernel.geom.Rectangle

data class Position(
    var index: Int,
    var pageSize: Rectangle,
    var rectangle: Rectangle,
    val enabled: MutableState<Boolean> = mutableStateOf(true),
    val selected: MutableState<Boolean> = mutableStateOf(false)
) {

    companion object {
        const val DISABLE: Int = Int.MIN_VALUE
    }

    fun calculateScrollOffset(): Int {
        if (!enabled.value) {
            return DISABLE
        }
        val ratioY = rectangle.y / pageSize.height
        val scrollOffset = pageSize.height - pageSize.height * ratioY
        return scrollOffset.toInt()
    }

    fun contain(rectangle: Rectangle) {
        this.rectangle.contains(rectangle)
    }

    override fun toString(): String {
        return "Position(index=$index, offsetX=$rectangle.x, offsetY=$rectangle.y)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Position

        if (index != other.index) return false
        if (!rectangle.equalsWithEpsilon(other.rectangle)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = index
        result = 31 * result + rectangle.hashCode()
        return result
    }

}