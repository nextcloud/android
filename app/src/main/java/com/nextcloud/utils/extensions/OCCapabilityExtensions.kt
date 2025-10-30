/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.google.gson.Gson
import com.owncloud.android.lib.resources.status.NextcloudVersion
import com.owncloud.android.lib.resources.status.OCCapability
import org.json.JSONException

private val gson = Gson()

/**
 * Determines whether **Windows-compatible file (WCF)** restrictions should be applied
 * for the current server version and configuration.
 *
 * Behavior:
 * - For **Nextcloud 32 and newer**, WCF enforcement depends on the [`isWCFEnabled`] flag
 *   provided by the server capabilities.
 * - For **Nextcloud 30 and 31**, WCF restrictions are always applied (feature considered enabled).
 * - For **versions older than 30**, WCF is not supported, and no restrictions are applied.
 *
 * @return `true` if WCF restrictions should be enforced based on the server version and configuration;
 * `false` otherwise.
 */
fun OCCapability.checkWCFRestrictions(): Boolean = if (version.isNewerOrEqual(NextcloudVersion.nextcloud_32)) {
    isWCFEnabled.isTrue
} else {
    version.isNewerOrEqual(NextcloudVersion.nextcloud_30)
}

fun OCCapability.forbiddenFilenames(): List<String> = jsonToList(forbiddenFilenamesJson)

fun OCCapability.forbiddenFilenameCharacters(): List<String> = jsonToList(forbiddenFilenameCharactersJson)

fun OCCapability.forbiddenFilenameExtensions(): List<String> = jsonToList(forbiddenFilenameExtensionJson)

fun OCCapability.forbiddenFilenameBaseNames(): List<String> = jsonToList(forbiddenFilenameBaseNamesJson)

fun OCCapability.shouldRemoveNonPrintableUnicodeCharactersAndConvertToUTF8(): Boolean =
    forbiddenFilenames().isNotEmpty() ||
        forbiddenFilenameCharacters().isNotEmpty() ||
        forbiddenFilenameExtensions().isNotEmpty() ||
        forbiddenFilenameBaseNames().isNotEmpty()

@Suppress("ReturnCount")
private fun jsonToList(json: String?): List<String> {
    if (json == null) return emptyList()

    return try {
        return gson.fromJson(json, Array<String>::class.java).toList()
    } catch (_: JSONException) {
        emptyList()
    }
}
