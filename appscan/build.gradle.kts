/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.nextcloud.appscan"

    defaultConfig {
        minSdk = 27
        compileSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint.targetSdk = 35
    testOptions.targetSdk = 35
}

kotlin.compilerOptions {
    jvmTarget.set(JvmTarget.JVM_17)
    freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.document.scanning.android.sdk)
    implementation(libs.ui)
}
