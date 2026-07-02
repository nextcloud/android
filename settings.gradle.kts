/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2025 Jimly Asshiddiqy <jimly.asshiddiqy@accenture.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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

/*
Needed for local android library

includeBuild("../android_library") {
    dependencySubstitution {
        substitute(module("com.github.nextcloud:android-library"))
            .using(project(":library"))
    }
}
*/


/*
Needed for local android common library


*/

includeBuild("../android_common") {
    dependencySubstitution {
        substitute(module("com.github.nextcloud.android-common:core"))
            .using(project(":core"))

        substitute(module("com.github.nextcloud.android-common:ui"))
            .using(project(":ui"))
    }
}

include(":app", ":appscan")
