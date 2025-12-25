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
        sourceCompatibility = JavaVersion.toVersion(libs.versions.jvmTarget.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.jvmTarget.get())
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