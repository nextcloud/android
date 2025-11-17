/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.model

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.test.platform.app.InstrumentationRegistry
import com.nextcloud.client.player.model.state.RepeatMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PlaybackSettingsTest {

    private lateinit var context: Context
    private lateinit var preferences: SharedPreferences

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        preferences = context.getSharedPreferences("playback_settings", Context.MODE_PRIVATE)
    }

    @Test
    fun returns_default_values_when_empty() {
        val settings = PlaybackSettings(context)
        assertEquals(RepeatMode.ALL, settings.repeatMode)
        assertFalse(settings.isShuffle)
        settings.reset()
    }

    @Test
    fun setRepeatMode_persists_value() {
        val settings = PlaybackSettings(context)
        settings.setRepeatMode(RepeatMode.SINGLE)
        val reloaded = PlaybackSettings(context)
        assertEquals(RepeatMode.SINGLE, reloaded.repeatMode)
        settings.reset()
    }

    @Test
    fun setShuffle_persists_value() {
        val settings = PlaybackSettings(context)
        settings.setShuffle(true)
        val reloaded = PlaybackSettings(context)
        assertTrue(reloaded.isShuffle)
        settings.reset()
    }

    @Test
    fun falls_back_to_default_when_invalid_RepeatMode_is_stored() {
        preferences.edit { putInt("repeat_mode_id", 999) }
        val settings = PlaybackSettings(context)
        assertEquals(RepeatMode.ALL, settings.repeatMode)
        settings.reset()
    }
}
