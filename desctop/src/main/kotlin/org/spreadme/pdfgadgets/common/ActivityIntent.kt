package org.spreadme.pdfgadgets.common

import kotlin.reflect.KClass

class ActivityIntent (val to: KClass<out Activity>) {
    val data = mutableMapOf<String, Any>()
}