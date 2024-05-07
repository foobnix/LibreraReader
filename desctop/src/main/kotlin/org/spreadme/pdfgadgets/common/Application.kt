package org.spreadme.pdfgadgets.common

open class Application {

    fun startActivity(intent: ActivityIntent) {
        val activity = intent.to.java.getDeclaredConstructor().newInstance()
        activity.intent = intent
        activity.onCreate()
    }

    open fun create() {

    }
}