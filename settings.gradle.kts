/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2014-2024 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
rootProject.name = "Nextcloud"

pluginManagement {
    resolutionStrategy.eachPlugin {
        if (requested.id.id == "shot") useModule("com.karumi:shot:${requested.version}")
    }

    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        maven("https://jitpack.io")
    }
}
//includeBuild("../android-common") {
//    dependencySubstitution {
//        substitute module("com.github.nextcloud.android-common:ui") using project(":ui")
//    }
//}

//includeBuild("../android-library") {
//    dependencySubstitution {
//        substitute module('com.github.nextcloud:android-library') using project(':library') // broken on gradle 8.14.2, so use 8.13 if needed
//    }
//}

include(":app", ":appscan")