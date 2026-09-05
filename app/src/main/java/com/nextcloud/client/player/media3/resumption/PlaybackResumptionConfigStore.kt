/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.media3.resumption

import android.content.Context
import androidx.core.content.edit
import com.nextcloud.client.player.model.file.PlaybackCollection
import com.nextcloud.client.player.model.file.PlaybackFileType
import javax.inject.Inject

class PlaybackResumptionConfigStore @Inject constructor(private val context: Context) {
    companion object {
        private const val PREFERENCES_FILE_NAME = "playback_resumption_config"
        private const val CURRENT_FILE_ID_KEY = "current_file_id"
        private const val FOLDER_ID_KEY = "folder_id"
        private const val FILE_TYPE_KEY = "file_type"
        private const val COLLECTION_KEY = "collection"
    }

    private val preferences by lazy {
        context.getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)
    }

    fun loadConfig(): PlaybackResumptionConfig? {
        val currentFileId = preferences.getString(CURRENT_FILE_ID_KEY, null)
        val folderId = preferences.getLong(FOLDER_ID_KEY, 0L)
        val fileType = preferences.getString(FILE_TYPE_KEY, null)?.let(::playbackFileType)
        val collection = preferences.getString(COLLECTION_KEY, null)?.let(::playbackCollection)
            ?: PlaybackCollection.FOLDER
        return if (currentFileId != null && folderId != 0L && fileType != null) {
            PlaybackResumptionConfig(currentFileId, folderId, fileType, collection)
        } else {
            null
        }
    }

    fun saveConfig(currentFileId: String, folderId: Long, fileType: PlaybackFileType, collection: PlaybackCollection) {
        preferences.edit {
            putString(CURRENT_FILE_ID_KEY, currentFileId)
            putLong(FOLDER_ID_KEY, folderId)
            putString(FILE_TYPE_KEY, fileType.value)
            putString(COLLECTION_KEY, collection.name)
        }
    }

    fun updateCurrentFileId(currentFileId: String) {
        preferences.edit {
            putString(CURRENT_FILE_ID_KEY, currentFileId)
        }
    }

    private fun playbackFileType(value: String): PlaybackFileType? = PlaybackFileType.entries.firstOrNull {
        it.value == value
    }

    private fun playbackCollection(name: String): PlaybackCollection? =
        PlaybackCollection.entries.firstOrNull { it.name == name }
}
