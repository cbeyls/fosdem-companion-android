import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "be.digitalia.fosdem"
    compileSdk = 35

    defaultConfig {
        applicationId = "be.digitalia.fosdem"
        minSdk = 21
        targetSdk = 34
        versionCode = 2100229
        versionName = "2.2.9"
        // Supported languages
        androidResources.localeFilters += "en"
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

            packaging {
                resources {
                    excludes += listOf(
                        "DebugProbesKt.bin",
                        "kotlin-tooling-metadata.json",
                        "kotlin/**",
                        "META-INF/*.version",
                        "META-INF/versions/**",
                        "META-INF/NOTICE.md"
                    )
                }
                jniLibs {
                    excludes += "**/libdatastore_shared_counter.so"
                }
            }
            vcsInfo.include = false
        }
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
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
    coreLibraryDesugaring(libs.desugarJdkLibs)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.coordinatorlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.drawerlayout)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.paging)
    implementation(libs.androidx.datastore.preferences)
    ksp(libs.androidx.room.compiler)
    implementation(libs.okhttp)
    implementation(libs.okhttp.tls)
    implementation(libs.okio)
    implementation(libs.moshi)
    implementation(libs.photoview)
}
