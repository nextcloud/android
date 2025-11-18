/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Jimly Asshiddiqy <jimly.asshiddiqy@accenture.com>
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
        sourceCompatibility = JavaVersion.VERSION_24
        targetCompatibility = JavaVersion.VERSION_24
    }

    lint.targetSdk = 36
    testOptions.targetSdk = 36
}

kotlin.compilerOptions {
    jvmTarget.set(JvmTarget.JVM_24)
    freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.document.scanning.android.sdk)
    implementation(libs.ui)
}
