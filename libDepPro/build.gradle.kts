plugins {
    id("com.android.library")
}

android {
    namespace = "mobi.librera.libdepro"
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
    api("com.github.junrar:junrar:7.5.7")
    api("com.google.android.gms:play-services-auth:21.4.0")
    api("com.google.apis:google-api-services-drive:v3-rev20220815-2.0.0")
    api("com.google.api-client:google-api-client-android:2.8.1")
    api("com.google.oauth-client:google-oauth-client-jetty:1.39.0")
    api("com.google.http-client:google-http-client-gson:2.0.3")
    implementation("com.google.android.play:review:2.0.2")
}