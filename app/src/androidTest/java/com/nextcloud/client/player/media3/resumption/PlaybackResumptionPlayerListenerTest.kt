/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.media3.resumption

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.test.core.app.ApplicationProvider
import com.nextcloud.client.player.model.file.PlaybackFileType
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class PlaybackResumptionPlayerListenerTest {

    private lateinit var store: PlaybackResumptionConfigStore
    private lateinit var listener: PlaybackResumptionPlayerListener

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        store = PlaybackResumptionConfigStore(context)
        store.saveConfig("oldId", 1L, PlaybackFileType.AUDIO, null)
        listener = PlaybackResumptionPlayerListener(store)
    }

    @Test
    fun onMediaItemTransition_updates_id() {
        val item = MediaItem.Builder().setMediaId("newId").build()
        listener.onMediaItemTransition(item, 0)
        val loaded = store.loadConfig()
        requireNotNull(loaded)
        assertEquals("newId", loaded.currentFileId)
    }
}
