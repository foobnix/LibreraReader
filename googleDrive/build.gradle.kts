import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.foobnix.googledrive"
    compileSdk = rootProject.extra["compileSdk"] as Int

    defaultConfig {
        minSdk = rootProject.extra["minSdk"] as Int
    }

    lint {
        targetSdk = rootProject.extra["targetSdk"] as Int
    }

    testOptions {
        targetSdk = rootProject.extra["targetSdk"] as Int
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

dependencies {
    api("com.google.android.gms:play-services-auth:21.4.0")
    api("com.google.apis:google-api-services-drive:v3-rev20220815-2.0.0")
    api("com.google.api-client:google-api-client-android:2.8.0")
    api("com.google.oauth-client:google-oauth-client-jetty:1.39.0")
    api("com.google.http-client:google-http-client-gson:1.47.1")


    api("androidx.credentials:credentials:1.5.0")
    api("androidx.credentials:credentials-play-services-auth:1.5.0")
    api("com.google.android.libraries.identity.googleid:googleid:1.1.1")
}
