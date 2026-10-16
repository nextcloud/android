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
        // FairScan isn't published to JitPack yet. Until then, it's consumed from mavenLocal,
        // published there manually and explicitly, same as any other binary dependency you're
        // developing against locally before it has a real release:
        //   ./gradlew publishToMavenLocal
        // Re-run that whenever the FairScan checkout is updated to a different commit/tag. Once
        // FairScan publishes to JitPack for real, this mavenLocal entry goes away and the
        // coordinate in gradle/libs.versions.toml switches to the JitPack one - nothing else
        // about how :app depends on it changes.
        mavenLocal {
            content { includeGroup("org.fairscan") }
        }
    }
}

/*
Needed for local android library
includeBuild("../android-library") {
    dependencySubstitution {
        substitute(module("com.github.nextcloud:android-library"))
            .using(project(":library"))
    }
}
*/


/*
Needed for local android common library

includeBuild("../android-common") {
    dependencySubstitution {
        substitute(module("com.github.nextcloud.android-common:core"))
            .using(project(":core"))

        substitute(module("com.github.nextcloud.android-common:ui"))
            .using(project(":ui"))
    }
}
*/

include(":app")
