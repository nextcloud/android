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
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object RichDocumentDownloadAsParser {

    private const val TAG = "RichDocumentDownloadAsParser"
    private const val URL_UPPERCASE = "URL"
    private const val URL_LOWERCASE = "url"

    private const val FORMAT = "format"
    private const val NAME = "name"
    private const val TYPE = "Type"
    private const val FILENAME = "filename"

    private const val EXTENSION_SEPARATOR = "."
    private const val DEFAULT_FILENAME_PREFIX = "document_"

    private val CONTROL_FORMATS = setOf("print", "slideshow", "export")
    private val EXTENSION_PATTERN = Regex("^[A-Za-z0-9]{1,10}$")

    private val CONTENT_DISPOSITION_FILENAME_STAR =
        Regex("""filename\*\s*=\s*[^']*''([^;]+)""", RegexOption.IGNORE_CASE)
    private val CONTENT_DISPOSITION_FILENAME =
        Regex("""filename\s*=\s*"?([^";]+)"?""", RegexOption.IGNORE_CASE)

    private val json = Json { ignoreUnknownKeys = true }

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

    fun hasExtension(name: String): Boolean {
        val dotIndex = name.lastIndexOf(EXTENSION_SEPARATOR)
        return dotIndex in 1 until name.length - 1
    }

    fun filenameFromContentDisposition(contentDisposition: String?): String? {
        if (contentDisposition.isNullOrBlank()) return null
        return encodedFilename(contentDisposition) ?: plainFilename(contentDisposition)
    }

    @Suppress("TooGenericExceptionCaught")
    private fun encodedFilename(contentDisposition: String): String? {
        val encoded = CONTENT_DISPOSITION_FILENAME_STAR.find(contentDisposition)
            ?.groupValues?.get(1) ?: return null

        val decoded = try {
            URLDecoder.decode(encoded, StandardCharsets.UTF_8.name())
        } catch (e: Exception) {
            Log_OC.e(TAG, "filename* decode failed: $e")
            null
        }

        return decoded?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun plainFilename(contentDisposition: String): String? =
        CONTENT_DISPOSITION_FILENAME.find(contentDisposition)
            ?.groupValues?.get(1)
            ?.trim()
            ?.trim('"')
            ?.takeIf { it.isNotBlank() }

    private fun tryParseV2(obj: JsonObject, url: String?): DownloadAs? {
        val format = obj[FORMAT]?.jsonPrimitive?.contentOrNull
        val name = obj[NAME]?.jsonPrimitive?.contentOrNull
        if (format == null || url == null) return null
        return DownloadAs(format = format, filename = createFilename(name, format), url = url)
    }

    private fun tryParseV1(obj: JsonObject, url: String?): DownloadAs? {
        val type = obj[TYPE]?.jsonPrimitive?.contentOrNull
        val filename = obj[FILENAME]?.jsonPrimitive?.contentOrNull
        if (type == null || url == null) return null
        return DownloadAs(format = type, filename = createFilename(filename, type), url = url)
    }

    private fun createFilename(filename: String?, format: String?): String {
        val name = filename?.takeIf { it.isNotBlank() }
            ?: "$DEFAULT_FILENAME_PREFIX${System.currentTimeMillis()}"

        val extension = extensionFromFormat(format)
        return when {
            hasExtension(name) || extension == null -> name
            else -> "$name$EXTENSION_SEPARATOR$extension"
        }
    }

    private fun extensionFromFormat(format: String?): String? = format?.lowercase()
        ?.takeUnless { it in CONTROL_FORMATS }
        ?.takeIf { EXTENSION_PATTERN.matches(it) }
}
