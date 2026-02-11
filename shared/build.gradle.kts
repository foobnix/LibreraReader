import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
        macosArm64(),
          ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    jvm()
    jvmToolchain(21)

    sourceSets {
        //jvmMain by getting
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.okio)
            implementation(compose.desktop.currentOs)
            implementation("org.kobjects.ktxml:core:1.0.0")

        }
        jvmMain.dependencies {
            implementation("com.dshatz.pdfmp:pdfmp-compose:1.0.9")
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation("com.squareup.okio:okio-fakefilesystem:3.16.4")
        }
    }
}

android {
    namespace = "mobi.librera.shared"
    compileSdk = libs.versions.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.jvmTarget.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.jvmTarget.get())
    }
    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }
}
