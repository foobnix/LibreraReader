plugins {
    id("com.android.library")
}

android {
    namespace = "mobi.librera.libReflow"

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
}