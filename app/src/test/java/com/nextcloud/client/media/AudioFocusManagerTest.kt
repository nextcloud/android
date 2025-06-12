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
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class AudioFocusManagerTest {

    private val audioManager: AudioManager = mock()
    private val callback: (AudioFocus) -> Unit = mock()
    private lateinit var audioFocusManager: AudioFocusManager

    private val builder: AudioFocusRequest.Builder = mock()
    private val focusRequest: AudioFocusRequest = mock()

    @Before
    fun setUp() {
        // Chain mock methods for the builder
        whenever(builder.setWillPauseWhenDucked(true)).thenReturn(builder)
        whenever(builder.setOnAudioFocusChangeListener(any())).thenReturn(builder)
        whenever(builder.build()).thenReturn(focusRequest)

        audioFocusManager = AudioFocusManager(audioManager, callback, builder)

        whenever(audioManager.requestAudioFocus(focusRequest))
            .thenReturn(AudioManager.AUDIOFOCUS_REQUEST_GRANTED)

        whenever(audioManager.abandonAudioFocusRequest(focusRequest))
            .thenReturn(AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
    }

    @Test
    fun `requestFocus should invoke FOCUS callback when granted`() {
        audioFocusManager.requestFocus()
        verify(callback).invoke(AudioFocus.FOCUS)
    }

    @Test
    fun `requestFocus should invoke LOST callback when denied`() {
        whenever(audioManager.requestAudioFocus(focusRequest))
            .thenReturn(AudioManager.AUDIOFOCUS_REQUEST_FAILED)

        audioFocusManager.requestFocus()
        verify(callback).invoke(AudioFocus.LOST)
    }

    @Test
    fun `releaseFocus should invoke LOST callback`() {
        audioFocusManager.releaseFocus()
        verify(callback).invoke(AudioFocus.LOST)
    }
}
