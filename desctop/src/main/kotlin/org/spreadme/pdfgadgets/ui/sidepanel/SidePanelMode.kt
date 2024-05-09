package org.spreadme.pdfgadgets.ui.sidepanel

import org.spreadme.pdfgadgets.resources.R

enum class SidePanelMode(
    val icon: String,
    val desc: String
) {

    INFO(R.Icons.info, "INFO"),
    OUTLINES(R.Icons.outlines, "OUTLINES"),
    STRUCTURE(R.Icons.structure, "STRUCTURE"),
    SIGNATURE(R.Icons.signature, "SIGNATURE");
}