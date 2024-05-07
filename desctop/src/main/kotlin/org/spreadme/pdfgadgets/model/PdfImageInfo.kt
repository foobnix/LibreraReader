package org.spreadme.pdfgadgets.model

import java.awt.image.BufferedImage

data class PdfImageInfo(
    val bufferedImage: BufferedImage?,
    val imageBytes: ByteArray? = null,
    val imageType: String?
)
