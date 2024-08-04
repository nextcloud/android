/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.media

import android.media.AudioFocusRequest
import android.media.AudioManager
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatcher
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class AudioFocusManagerTest {

    private val audioManager = mock<AudioManager>()
    private val callback = mock<(AudioFocus) -> Unit>()
    private lateinit var audioFocusManager: AudioFocusManager

    val audioRequestMatcher = object : ArgumentMatcher<AudioFocusRequest> {
        override fun matches(argument: AudioFocusRequest?): Boolean = true
    }

    @Before
    fun setUp() {
        audioFocusManager = AudioFocusManager(audioManager, callback)
        whenever(audioManager.requestAudioFocus(any(), any(), any()))
            .thenReturn(AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        whenever(audioManager.abandonAudioFocusRequest(argThat(audioRequestMatcher)))
            .thenReturn(AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        whenever(audioManager.abandonAudioFocusRequest(any()))
            .thenReturn(AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
    }

    @Test
    fun `acquiring focus triggers callback immediately`() {
        audioFocusManager.requestFocus()
        verify(callback).invoke(AudioFocus.FOCUS)
    }

    @Test
    fun `failing to acquire focus triggers callback immediately`() {
        whenever(audioManager.requestAudioFocus(any(), any(), any()))
            .thenReturn(AudioManager.AUDIOFOCUS_REQUEST_FAILED)
        audioFocusManager.requestFocus()
        verify(callback).invoke(AudioFocus.LOST)
    }

    @Test
    fun `releasing focus triggers callback immediately`() {
        audioFocusManager.releaseFocus()
        verify(callback).invoke(AudioFocus.LOST)
    }
}
