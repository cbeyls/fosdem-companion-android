plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.parcelize")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "be.digitalia.fosdem"
    compileSdk = 34

    defaultConfig {
        applicationId = "be.digitalia.fosdem"
        minSdk = 21
        targetSdk = 33
        multiDexEnabled = true
        versionCode = 2100219
        versionName = "2.1.9"
        // Supported languages
        resourceConfigurations += "en"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    sourceSets {
        getByName("main") {
            res.srcDir("src/main/res-override")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles("proguard-defaults.txt", "proguard-rules.pro")

            kotlinOptions {
                freeCompilerArgs = listOf(
                        "-Xno-param-assertions",
                        "-Xno-call-assertions",
                        "-Xno-receiver-assertions"
                )
            }

            packaging {
                resources {
                    excludes += listOf(
                        "DebugProbesKt.bin",
                        "kotlin-tooling-metadata.json",
                        "kotlin/**",
                        "META-INF/*.kotlin_module",
                        "META-INF/*.version"
                    )
                }
            }
        }
    }
    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

fun patchDesugarConfig(config: Property<String>) {
    val defaultConfig = config as org.gradle.api.internal.provider.DefaultProperty<String>
    val patchedDesugarConfig = defaultConfig.provider.map {
        it.replace(
            "\"support_all_callbacks_from_library\":true",
            "\"support_all_callbacks_from_library\":false"
        )
    }
    config.set(patchedDesugarConfig)
}

afterEvaluate {
    tasks.withType(com.android.build.gradle.internal.tasks.R8Task::class).configureEach {
        patchDesugarConfig(coreLibDesugarConfig)
    }
    tasks.withType(com.android.build.gradle.internal.tasks.L8DexDesugarLibTask::class).configureEach {
        patchDesugarConfig(libConfiguration)
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.viewpager2:viewpager2:1.1.0-beta02")
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.browser:browser:1.7.0")
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.viewmodel)
    implementation("androidx.paging:paging-runtime-ktx:3.2.1")
    implementation(libs.room.runtime)
    implementation(libs.room.paging)
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    ksp(libs.room.compiler)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okio:okio:3.7.0")
    implementation("com.squareup.moshi:moshi:1.15.0")
    implementation("com.github.chrisbanes:PhotoView:2.3.0")
}
