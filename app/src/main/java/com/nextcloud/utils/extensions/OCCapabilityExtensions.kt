/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.google.gson.Gson
import com.owncloud.android.lib.resources.status.OCCapability
import org.json.JSONException

private val gson = Gson()

fun OCCapability.forbiddenFilenames(): List<String> = jsonToList(forbiddenFilenamesJson)

fun OCCapability.forbiddenFilenameCharacters(): List<String> = jsonToList(forbiddenFilenameCharactersJson)

fun OCCapability.forbiddenFilenameExtension(): List<String> = jsonToList(forbiddenFilenameExtensionJson)

fun OCCapability.forbiddenFilenameBaseNames(): List<String> = jsonToList(forbiddenFilenameBaseNames)

private fun jsonToList(json: String?): List<String> {
    if (json == null) return emptyList()

    return try {
        return gson.fromJson(json, Array<String>::class.java).toList()
    } catch (e: JSONException) {
        emptyList()
    }
}
