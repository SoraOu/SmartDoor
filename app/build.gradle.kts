plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android) version "2.1.0"      // Kotlin plugin version will be updated here
    alias(libs.plugins.compose.compiler)  // Apply Compose Compiler plugin
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("kotlin-kapt")
}
android {
    namespace = "com.example.smartdoor"
    compileSdk = 35  // ✅ Update this from 34 to 35

    defaultConfig {
        applicationId = "com.example.smartdoor"
        minSdk = 24
        targetSdk = 35  // Optional: update targetSdk to 35 as well
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildFeatures {
        viewBinding = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.0" // Set the latest Compose Compiler version
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }


}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.2")

    implementation("androidx.compose.ui:ui:1.5.0")  // Jetpack Compose dependencies
    implementation("androidx.compose.material3:material3:1.0.0")  // Material3 for Compose
    implementation("androidx.compose.ui:ui-tooling-preview:1.5.0")  // Tooling for preview

    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // UI
    implementation(libs.androidx.appcompat) // Corrected to androidx.appcompat (AndroidX version)
    implementation(libs.material)

    // Firebase dependencies (latest versions)
    implementation("com.google.firebase:firebase-analytics:22.5.0")
    implementation("com.google.firebase:firebase-crashlytics-ktx:19.4.4")

    // Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation(libs.firebase.inappmessaging)
    implementation(libs.androidx.camera.view)
    implementation(libs.firebase.database.ktx)
    kapt("com.github.bumptech.glide:compiler:4.16.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // CameraX dependencies (latest versions)
    implementation("androidx.camera:camera-core:1.4.2")
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")

    // Supabase
    //implementation("io.github.jan-tenner.supabase:core:3.2.0")
    //implementation("io.github.jan-tenner.supabase:postgrest-kt:3.2.0-ksp-b1")
    //implementation("io.github.jan-tenner.supabase:storage-kt:3.2.0-ksp-b1")
    //implementation("io.github.jan-tenner.supabase:supabase-kt:3.2.0")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")






}
