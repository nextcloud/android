/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.nextcloud.client.media

import android.media.AudioFocusRequest
import android.media.AudioManager
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatcher

class AudioFocusManagerTest {

    private val audioManager = mock<AudioManager>()
    private val callback = mock<(AudioFocus)->Unit>()
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
