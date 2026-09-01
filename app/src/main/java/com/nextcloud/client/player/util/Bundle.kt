/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.util

import android.os.Bundle
import com.nextcloud.client.player.model.file.PlaybackFile
import kotlinx.serialization.json.Json

private val playbackJson = Json { ignoreUnknownKeys = true }

fun Bundle.putPlaybackFile(key: String, playbackFile: PlaybackFile) {
    putString(key, playbackJson.encodeToString(playbackFile))
}

fun Bundle?.getPlaybackFile(key: String): PlaybackFile? {
    val encoded = this?.getString(key) ?: return null
    return runCatching { playbackJson.decodeFromString<PlaybackFile>(encoded) }.getOrNull()
}
