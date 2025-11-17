/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.model

import android.content.Context
import androidx.core.content.edit
import com.nextcloud.client.player.model.state.RepeatMode
import javax.inject.Inject

class PlaybackSettings @Inject constructor(context: Context) {
    companion object {
        private const val PREFERENCES_FILE_NAME = "playback_settings"
        private const val REPEAT_MODE_ID_KEY = "repeat_mode_id"
        private const val SHUFFLE_KEY = "shuffle"
        private val DEFAULT_REPEAT_MODE = RepeatMode.ALL
    }

    private val preferences = context.getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)

    val repeatMode: RepeatMode
        get() = preferences.getInt(REPEAT_MODE_ID_KEY, -1)
            .let { id -> RepeatMode.entries.firstOrNull { it.id == id } }
            ?: DEFAULT_REPEAT_MODE

    val isShuffle: Boolean
        get() = preferences.getBoolean(SHUFFLE_KEY, false)

    fun setRepeatMode(repeatMode: RepeatMode) {
        preferences.edit {
            putInt(REPEAT_MODE_ID_KEY, repeatMode.id)
        }
    }

    fun setShuffle(shuffle: Boolean) {
        preferences.edit {
            putBoolean(SHUFFLE_KEY, shuffle)
        }
    }

    fun reset() {
        preferences.edit {
            clear()
        }
    }
}
