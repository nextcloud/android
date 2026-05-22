/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.utils

import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.model.DownloadAs
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object RichDocumentDownloadAsParser {

    private const val TAG = "RichDocumentDownloadAsParser"
    private const val URL_UPPERCASE = "URL"
    private const val URL_LOWERCASE = "url"

    private const val FORMAT = "format"
    private const val NAME = "name"
    private const val TYPE = "Type"
    private const val FILENAME = "filename"

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(jsonString: String?): DownloadAs? {
        if (jsonString.isNullOrBlank()) return null

        return try {
            val obj = json.parseToJsonElement(jsonString).jsonObject
            val url = obj[URL_LOWERCASE]?.jsonPrimitive?.contentOrNull
                ?: obj[URL_UPPERCASE]?.jsonPrimitive?.contentOrNull
            tryParseV2(obj, url) ?: tryParseV1(obj, url)
        } catch (e: Exception) {
            Log_OC.e(TAG, "parse failed: $e")
            null
        }
    }

    private fun tryParseV2(obj: JsonObject, url: String?): DownloadAs? {
        val format = obj[FORMAT]?.jsonPrimitive?.contentOrNull
        val name = obj[NAME]?.jsonPrimitive?.contentOrNull
        if (format == null || url == null) return null
        return DownloadAs(format = format, fileName = name ?: "", url = url)
    }

    private fun tryParseV1(obj: JsonObject, url: String?): DownloadAs? {
        val type = obj[TYPE]?.jsonPrimitive?.contentOrNull
        val filename = obj[FILENAME]?.jsonPrimitive?.contentOrNull
        if (type == null || url == null) return null
        return DownloadAs(format = type, fileName = filename ?: "", url = url)
    }
}
