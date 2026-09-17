/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.model.file

enum class PlaybackFileType(val value: String) {
    AUDIO("audio"),
    VIDEO("video");

    companion object {
        fun ofMimeType(mimeType: String): PlaybackFileType = entries
            .firstOrNull { mimeType.startsWith(it.value, ignoreCase = true) }
            ?: throw IllegalArgumentException("Unsupported file type: $mimeType")
    }
}
