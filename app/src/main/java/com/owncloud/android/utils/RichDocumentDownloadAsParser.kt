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
import okhttp3.OkHttpClient
import okhttp3.Request

object RichDocumentDownloadAsParser {

    private const val TAG = "RichDocumentDownloadAsParser"
    private const val URL_UPPERCASE = "URL"
    private const val URL_LOWERCASE = "url"

    private const val FORMAT = "format"
    private const val NAME = "name"
    private const val TYPE = "Type"
    private const val FILENAME = "filename"

    private const val CONTENT_DISPOSITION_HEADER = "Content-Disposition"

    private val json = Json { ignoreUnknownKeys = true }

    private val client = OkHttpClient()

    @Suppress("TooGenericExceptionCaught")
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
        return DownloadAs(format = format, filename = createFilename(url, name, format), url = url)
    }

    private fun tryParseV1(obj: JsonObject, url: String?): DownloadAs? {
        val type = obj[TYPE]?.jsonPrimitive?.contentOrNull
        val filename = obj[FILENAME]?.jsonPrimitive?.contentOrNull
        if (type == null || url == null) return null
        return DownloadAs(format = type, filename = createFilename(url, filename, type), url = url)
    }

    private fun createFilename(url: String, filename: String?, format: String?): String {
        if (filename != null && format != null) {
            return filename + format
        }

        val request = Request.Builder().url(url).head().build()

        return try {
            client.newCall(request).execute().use { response ->
                val disposition = response.header(CONTENT_DISPOSITION_HEADER)
                extractFilenameFromDisposition(disposition) ?: randomFilename(format)
            }
        } catch (e: Exception) {
            Log_OC.e(TAG, "createFilename failed: $e")
            randomFilename(format)
        }
    }

    private fun extractFilenameFromDisposition(disposition: String?): String? {
        if (disposition.isNullOrBlank()) return null

        val extendedRegex = """filename\*\s*=\s*[^']*''([^;]+)""".toRegex(RegexOption.IGNORE_CASE)
        extendedRegex.find(disposition)?.groupValues?.get(1)?.let {
            return runCatching { java.net.URLDecoder.decode(it.trim(), "UTF-8") }.getOrNull()
        }

        val regex = """filename\s*=\s*"?([^";]+)"?""".toRegex(RegexOption.IGNORE_CASE)
        return regex.find(disposition)?.groupValues?.get(1)?.trim()
    }

    private fun randomFilename(format: String? = null): String {
        val random = java.util.UUID.randomUUID().toString().take(8)
        val extension = format?.removePrefix(".")?.let { ".$it" } ?: ""
        return "file_$random$extension"
    }
}
