import org.jetbrains.compose.ComposeBuildConfig.composeVersion

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.jetbrains.compose")
}

group = "org.spreadme.decorated-window"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":common"))
    implementation("org.jetbrains.compose.foundation:foundation-desktop:$composeVersion")
    implementation("net.java.dev.jna:jna:5.14.0")
}