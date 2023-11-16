pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    val hiltVersion: String by settings
    plugins {
        id("com.google.dagger.hilt.android") version hiltVersion
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven {
            url = uri("https://jitpack.io")
            content {
                includeGroup("com.github.chrisbanes")
            }
        }
        google()
        mavenCentral()
    }

    versionCatalogs {
        val hiltVersion: String by settings
        create("libs") {
            version("hilt", hiltVersion)
            version("room", "2.6.0")
            version("lifecycle", "2.6.2")
            library("hilt-android", "com.google.dagger", "hilt-android").versionRef("hilt")
            library("hilt-compiler", "com.google.dagger", "hilt-compiler").versionRef("hilt")
            library("room-runtime", "androidx.room", "room-ktx").versionRef("room")
            library("room-paging", "androidx.room", "room-paging").versionRef("room")
            library("room-compiler", "androidx.room", "room-compiler").versionRef("room")
            library("lifecycle-runtime", "androidx.lifecycle", "lifecycle-runtime-ktx").versionRef("lifecycle")
            library("lifecycle-viewmodel", "androidx.lifecycle", "lifecycle-viewmodel-ktx").versionRef("lifecycle")
        }
    }
}

rootProject.name = "FOSDEM Companion"
include(":app")
