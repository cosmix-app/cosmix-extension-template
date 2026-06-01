plugins {
    id("com.android.library")
    id("app.cosmix.gradle")
}

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

cosmix {
    // when running through github workflow, GITHUB_REPOSITORY should contain current repository name
    setRepo(System.getenv("GITHUB_REPOSITORY") ?: "user/repo")
    
    // All of these properties are optional, you can safely remove any of them.
    description = "Lorem ipsum"
    authors = listOf("Cloudburst", "Luna712")
    status = 1 // Will be 3 if unspecified
    tvTypes = listOf("Movie")

    language = "en"
    iconUrl = "https://upload.wikimedia.org/wikipedia/commons/2/2f/Korduene_Logo.png"
}

android {
    namespace = "com.example"

    defaultConfig {
        minSdk = 21
        compileSdk = 35
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    tasks.withType<KotlinJvmCompile> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8) // Required
            freeCompilerArgs.addAll(
                "-Xno-call-assertions",
                "-Xno-param-assertions",
                "-Xno-receiver-assertions", "-Xskip-metadata-version-check"
            )
        }
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}

dependencies {
    val cosmix by configurations
    val implementation by configurations

    // Stubs for all cosmix classes
    cosmix("com.github.cosmix-app:cosmix:pre-release")

    // These dependencies can include any of those which are added by the app,
    // but you don't need to include any of them if you don't need them.
    implementation(kotlin("stdlib")) // Adds Standard Kotlin Features
    implementation("com.github.Blatzar:NiceHttp:0.4.11") // HTTP Lib
    implementation("org.jsoup:jsoup:1.18.3") // HTML Parser
    // IMPORTANT: Do not bump Jackson above 2.13.1, as newer versions will
    // break compatibility on older Android devices.
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.1") // JSON Parser

    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}

// Use an integer for version numbers
version = 1