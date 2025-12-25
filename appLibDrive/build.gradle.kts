import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.foobnix.googledrive"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    lint {
        targetSdk = libs.versions.targetSdk.get().toInt()
    }

    testOptions {
        targetSdk = libs.versions.targetSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/NOTICE",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE.txt",
                "META-INF/INDEX.LIST",
                "META-INF/io.netty.versions.properties"
            )
            pickFirsts += setOf(
                "google/protobuf/*.proto",
                "META-INF/services/io.grpc.LoadBalancerProvider",
                "META-INF/services/io.grpc.ManagedChannelProvider",
                "META-INF/services/io.grpc.NameResolverProvider"
            )
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

dependencies {

    implementation(platform("androidx.compose:compose-bom:2025.07.00"))
    api("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.2")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.fragment:fragment-ktx:1.8.8")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.3.0")
    implementation("com.squareup.okhttp3:okhttp:5.1.0")

    api(platform("com.google.firebase:firebase-bom:34.1.0"))
    api("com.google.firebase:firebase-auth")
    api("com.google.firebase:firebase-firestore")
    api("com.google.firebase:firebase-storage")//optional to remove


    //gRPC dependencies for Firestore
    api("io.grpc:grpc-okhttp:1.74.0")
    api("io.grpc:grpc-android:1.74.0")
    api("io.grpc:grpc-protobuf-lite:1.74.0")
    api("io.grpc:grpc-stub:1.74.0")


    api("com.google.android.gms:play-services-auth:21.4.0")
    api("com.google.apis:google-api-services-drive:v3-rev20220815-2.0.0")
    api("com.google.api-client:google-api-client-android:2.8.0")
    api("com.google.oauth-client:google-oauth-client-jetty:1.39.0")
    api("com.google.http-client:google-http-client-gson:1.47.1")


    api("androidx.credentials:credentials:1.5.0")
    api("androidx.credentials:credentials-play-services-auth:1.5.0")
    api("com.google.android.libraries.identity.googleid:googleid:1.1.1")

}
