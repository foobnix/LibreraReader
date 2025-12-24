plugins {
    id("com.android.library")
}

android {
    namespace = "mobi.librera.libgoogleFree"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = rootProject.extra["minSdk"] as Int
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

    api("com.google.android.gms:play-services-ads:24.9.0")
    api("com.google.android.ump:user-messaging-platform:4.0.0")
    //implementation("com.android.billingclient:billing:8.1.0")
}