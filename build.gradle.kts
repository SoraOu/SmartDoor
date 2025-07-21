// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.5.1" apply false  // Apply the Android plugin
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false  // Apply Kotlin plugin
    alias(libs.plugins.compose.compiler) apply false  // Apply Compose Compiler plugin from version catalog
}

buildscript {
    dependencies {
        classpath("com.google.gms:google-services:4.3.15")
        classpath("com.google.firebase:firebase-crashlytics-gradle:2.9.0")
    }
}
