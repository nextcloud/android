/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.model

import com.owncloud.android.lib.common.utils.Log_OC
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

@Serializable
data class DownloadAsV1(
    @SerialName("Type") val type: String,
    @SerialName("URL") val url: String,
    @SerialName("filename") val fileName: String
) {
    companion object {
        fun tryDeserialize(json: String): DownloadAsResult? {
            return try {
                val v1 = jsonImpl.decodeFromString<DownloadAsV1>(json)
                return DownloadAsResult(
                    format = v1.type,
                    fileName = v1.fileName,
                    url = v1.url
                )
            } catch (e: SerializationException) {
                Log_OC.e("DownloadAsV1", "tryDeserialize: $e")
                null
            }
        }
    }
}

@Serializable
data class DownloadAsV2(
    @SerialName("format") val format: String,
    @SerialName("name") val fileName: String,
    @SerialName("url") val url: String
) {
    companion object {
        fun tryDeserialize(json: String): DownloadAsResult? {
            return try {
                val v2 = jsonImpl.decodeFromString<DownloadAsV2>(json)
                return DownloadAsResult(
                    format = v2.format,
                    fileName = v2.fileName,
                    url = v2.url
                )
            } catch (e: SerializationException) {
                Log_OC.e("DownloadAsV2", "tryDeserialize: $e")
                null
            }
        }
    }
}

data class DownloadAsResult(
    val format: String,
    val fileName: String,
    val url: String
)

private val jsonImpl = Json { ignoreUnknownKeys = true }
