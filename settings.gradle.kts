pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
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
}

rootProject.name = "FOSDEM Companion"
include(":app")
