plugins {
    id("com.android.application") version "8.13.0" apply false
    id("com.android.library") version "8.13.0" apply false
    id("org.jetbrains.kotlin.android") version "2.2.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21" apply false
    id("androidx.room") version "2.7.2" apply false
    id("com.google.devtools.ksp") version "2.3.0" apply false
    id("com.google.gms.google-services") version "4.4.4" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.21" apply false
    id("org.jetbrains.kotlin.jvm") version "2.2.21" apply false
}

project.extra.apply {
    set("targetSdk", 36)
    set("minSdk", 24)
    set("minFDroidSdk", 16)
    set("compileSdk", 36)
}
