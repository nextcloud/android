/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.media3.resumption

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nextcloud.client.player.model.file.PlaybackFileType
import com.owncloud.android.ui.fragment.SearchType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class PlaybackResumptionConfigStoreTest {

    private lateinit var store: PlaybackResumptionConfigStore

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        store = PlaybackResumptionConfigStore(context)
    }

    @Test
    fun save_and_load_returns_expected_config() {
        store.saveConfig("123", 42L, PlaybackFileType.AUDIO, SearchType.NO_SEARCH)
        val loaded = store.loadConfig()
        requireNotNull(loaded)
        assertEquals("123", loaded.currentFileId)
        assertEquals(42L, loaded.folderId)
        assertEquals(PlaybackFileType.AUDIO, loaded.fileType)
        assertEquals(SearchType.NO_SEARCH, loaded.searchType)
    }

    @Test
    fun clear_and_load_returns_null() {
        store.saveConfig("123", 42L, PlaybackFileType.AUDIO, SearchType.NO_SEARCH)
        store.clear()
        assertNull(store.loadConfig())
    }

    @Test
    fun updateCurrentFileId_only_changes_that() {
        store.saveConfig("123", 42L, PlaybackFileType.AUDIO, SearchType.NO_SEARCH)
        store.updateCurrentFileId("999")
        val loaded = store.loadConfig()
        requireNotNull(loaded)
        assertEquals("999", loaded.currentFileId)
        assertEquals(42L, loaded.folderId)
    }
}
