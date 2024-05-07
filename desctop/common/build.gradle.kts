import org.jetbrains.compose.ComposeBuildConfig

plugins {
    kotlin("jvm")
}

group = "org.spreadme.common"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies{
    implementation("org.jetbrains.compose.foundation:foundation-desktop:${ComposeBuildConfig.composeVersion}")
}