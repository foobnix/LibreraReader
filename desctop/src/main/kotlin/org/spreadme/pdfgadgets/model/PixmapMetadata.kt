package org.spreadme.pdfgadgets.model

import java.awt.image.BufferedImage

data class PixmapMetadata(
    val width: Int,
    val height: Int,
) {

    fun toBufferedImage(pixels: IntArray): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_USHORT_555_RGB)
        image.setRGB(0, 0, width, height, pixels, 0, width)
        return image
    }

}
