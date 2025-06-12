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
        focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).run {
            setWillPauseWhenDucked(true)
            setOnAudioFocusChangeListener(focusListener)
        }.build()
    }

    /**
     * Request audio focus. Focus is reported via callback.
     * If focus cannot be gained, lost of focus is reported.
     */
    fun requestFocus() {
        val requestResult =
            focusRequest?.let { audioManger.requestAudioFocus(it) }
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
        focusRequest?.let {
            audioManger.abandonAudioFocusRequest(it)
        } ?: AudioManager.AUDIOFOCUS_REQUEST_FAILED
        focusListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS)
    }
}
