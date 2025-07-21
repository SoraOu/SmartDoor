pluginManagement {
    repositories {
        google()  // Required for Android dependencies
        gradlePluginPortal()  // For Gradle plugins
        mavenCentral()  // Central repository for many libraries
        maven("https://jitpack.io")  // If you need to use GitHub-hosted libraries
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)  // Ensure repositories are not added in project-level build.gradle
    repositories {
        google()  // Required for Android dependencies
        gradlePluginPortal()  // For Gradle plugins
        mavenCentral()
        maven("https://jitpack.io") // If the library is hosted on JitPack
    }
}

rootProject.name = "Smart Door"
include(":app")
