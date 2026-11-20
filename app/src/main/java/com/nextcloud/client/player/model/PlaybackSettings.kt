/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.model

import android.content.Context
import androidx.core.content.edit
import com.nextcloud.client.player.model.state.PlayMode
import com.nextcloud.utils.serialization.AppJson
import javax.inject.Inject

class PlaybackSettings @Inject constructor(context: Context) {
    companion object {
        private const val PREFERENCES_FILE_NAME = "playback_settings"
        private const val PLAY_MODE_KEY = "play_mode"
    }

    private val preferences = context.getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)

    val playMode: PlayMode
        get() = preferences.getString(PLAY_MODE_KEY, null)
            ?.let { runCatching { AppJson.instance.decodeFromString<PlayMode>(it) }.getOrNull() }
            ?: PlayMode.REPEAT_ONE

    fun setPlayMode(playMode: PlayMode) {
        preferences.edit {
            putString(PLAY_MODE_KEY, AppJson.instance.encodeToString(playMode))
        }
    }
}
