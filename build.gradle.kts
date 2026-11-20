/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Jimly Asshiddiqy <jimly.asshiddiqy@accenture.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.spotbugs) apply false
    alias(libs.plugins.detekt) apply false
    // needed to make renovate run without shot, as shot requires Android SDK
    // https://github.com/pedrovgs/Shot/issues/300
    alias(libs.plugins.shot) apply false
}

tasks.register<Delete>("clean") {
    description = "Deletes the root project build directory."
    delete(layout.buildDirectory)
}

tasks.register<Copy>("installGitHooks") {
    description = "Install git hooks"

    val sourceFolder = layout.projectDirectory.dir("scripts/hooks")
    val destFolder = layout.projectDirectory.dir(".git/hooks")

    from(sourceFolder) { include("*") }
    into(destFolder)
    eachFile { println("$sourceFolder/$path -> $destFolder/$path") }
}
