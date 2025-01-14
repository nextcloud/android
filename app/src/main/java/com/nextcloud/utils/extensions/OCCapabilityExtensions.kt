/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.google.gson.Gson
import com.owncloud.android.lib.resources.status.OCCapability
import org.json.JSONException

private val gson = Gson()

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
    } catch (e: JSONException) {
        emptyList()
    }
}
