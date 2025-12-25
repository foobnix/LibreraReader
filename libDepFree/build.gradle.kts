import org.gradle.kotlin.dsl.implementation

plugins {
    alias(libs.plugins.androidLibrary)
}

android {
    namespace = "mobi.librera.libgoogleFree"
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

    //implementation(platform(libs.firebase.bom))
    //api(libs.firebase.analytics)
    //api("com.google.ads.mediation:facebook:6.21.0.0")

    api(libs.play.services.ads)
    api(libs.user.messaging.platform)

    //implementation(libs.billing)




}