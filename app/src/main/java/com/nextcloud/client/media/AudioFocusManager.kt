/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.media

import android.media.AudioFocusRequest
import android.media.AudioManager

/**
 * Wrapper around audio manager exposing simplified audio focus API and
 * hiding platform API level differences.
 *
 * @param audioManger Platform audio manager
 * @param onFocusChange Called when audio focus changes, including acquired and released focus states
 */
internal class AudioFocusManager(
    private val audioManger: AudioManager,
    private val onFocusChange: (AudioFocus) -> Unit,
    requestBuilder: AudioFocusRequest.Builder = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
) {
    private val focusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        val focus = when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK -> AudioFocus.FOCUS
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> AudioFocus.LOST
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> AudioFocus.DUCK
            else -> null
        }
        focus?.let { onFocusChange(it) }
    }

    private val focusRequest = requestBuilder
        .setWillPauseWhenDucked(true)
        .setOnAudioFocusChangeListener(focusListener)
        .build()

    fun requestFocus() {
        val requestResult = audioManger.requestAudioFocus(focusRequest)
        if (requestResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            focusListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
        } else {
            focusListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS)
        }
    }

    fun releaseFocus() {
        audioManger.abandonAudioFocusRequest(focusRequest)
        focusListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS)
    }
}
