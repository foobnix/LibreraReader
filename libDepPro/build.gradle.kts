plugins {
    alias(libs.plugins.androidLibrary)
}

android {
    namespace = "mobi.librera.libdepro"
    compileSdk {
        version = release(libs.versions.compileSdk.get().toInt())
    }

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    api(libs.junrar)
    api(libs.play.services.auth)
    api(libs.google.api.services.drive)
    api(libs.google.api.client.android)
    api(libs.google.oauth.client.jetty)
    api(libs.google.http.client.gson)
    implementation(libs.review)
}