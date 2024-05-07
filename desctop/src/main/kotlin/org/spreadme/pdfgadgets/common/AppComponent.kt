package org.spreadme.pdfgadgets.common

import org.koin.core.component.KoinComponent

abstract class AppComponent(
    open var name: String
) : AbstractComponent(), KoinComponent