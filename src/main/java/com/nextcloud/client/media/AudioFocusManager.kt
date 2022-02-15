/**
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 *
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
import android.os.Build

/**
 * Wrapper around audio manager exposing simplified audio focus API and
 * hiding platform API level differences.
 *
 * @param audioManger Platform audio manager
 * @param onFocusChange Called when audio focus changes, including acquired and released focus states
 */
internal class AudioFocusManager(
    private val audioManger: AudioManager,
    private val onFocusChange: (AudioFocus) -> Unit
) {

    private val focusListener = object : AudioManager.OnAudioFocusChangeListener {
        override fun onAudioFocusChange(focusChange: Int) {
            val focus = when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> AudioFocus.FOCUS
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT -> AudioFocus.FOCUS
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK -> AudioFocus.FOCUS
                AudioManager.AUDIOFOCUS_LOSS -> AudioFocus.LOST
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> AudioFocus.LOST
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> AudioFocus.DUCK
                else -> null
            }
            focus?.let { onFocusChange(it) }
        }
    }
    private var focusRequest: AudioFocusRequest? = null

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).run {
                setWillPauseWhenDucked(true)
                setOnAudioFocusChangeListener(focusListener)
            }.build()
        }
    }

    /**
     * Request audio focus. Focus is reported via callback.
     * If focus cannot be gained, lost of focus is reported.
     */
    fun requestFocus() {
        val requestResult = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManger.requestAudioFocus(it) }
        } else {
            audioManger.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }

        if (requestResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            focusListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
        } else {
            focusListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS)
        }
    }

    /**
     * Release audio focus. Loss of focus is reported via callback.
     */
    fun releaseFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let {
                audioManger.abandonAudioFocusRequest(it)
            } ?: AudioManager.AUDIOFOCUS_REQUEST_FAILED
        } else {
            audioManger.abandonAudioFocus(focusListener)
        }
        focusListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS)
    }
}
