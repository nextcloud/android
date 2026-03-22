/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.media3.resumption

import android.content.Context
import androidx.core.content.edit
import com.nextcloud.client.player.model.file.PlaybackFileType
import com.owncloud.android.ui.fragment.SearchType
import javax.inject.Inject

class PlaybackResumptionConfigStore @Inject constructor(private val context: Context) {
    companion object {
        private const val PREFERENCES_FILE_NAME = "playback_resumption_config"
        private const val CURRENT_FILE_ID_KEY = "current_file_id"
        private const val FOLDER_ID_KEY = "folder_id"
        private const val FILE_TYPE_KEY = "file_type"
        private const val SEARCH_TYPE_KEY = "search_type"
    }

    private val preferences by lazy {
        context.getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)
    }

    fun loadConfig(): PlaybackResumptionConfig? {
        val currentFileId = preferences.getString(CURRENT_FILE_ID_KEY, null)
        val folderId = preferences.getLong(FOLDER_ID_KEY, 0L)
        val fileType = preferences.getString(FILE_TYPE_KEY, null)?.let(::playbackFileType)
        val searchType = preferences.getString(SEARCH_TYPE_KEY, null)?.let(::searchType)
        return if (currentFileId != null && folderId != 0L && fileType != null) {
            PlaybackResumptionConfig(currentFileId, folderId, fileType, searchType)
        } else {
            null
        }
    }

    fun saveConfig(currentFileId: String, folderId: Long, fileType: PlaybackFileType, searchType: SearchType?) {
        preferences.edit {
            putString(CURRENT_FILE_ID_KEY, currentFileId)
            putLong(FOLDER_ID_KEY, folderId)
            putString(FILE_TYPE_KEY, fileType.value)
            putString(SEARCH_TYPE_KEY, searchType?.name)
        }
    }

    fun updateCurrentFileId(currentFileId: String) {
        preferences.edit {
            putString(CURRENT_FILE_ID_KEY, currentFileId)
        }
    }

    fun clear() {
        preferences.edit {
            clear()
        }
    }

    private fun playbackFileType(value: String): PlaybackFileType? = PlaybackFileType.entries.firstOrNull {
        it.value == value
    }

    private fun searchType(name: String): SearchType? = SearchType.entries.firstOrNull { it.name == name }
}
