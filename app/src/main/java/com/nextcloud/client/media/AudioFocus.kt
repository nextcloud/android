/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.media

import android.media.AudioManager

/**
 * Simplified audio focus values, relevant to application's media player experience.
 */
internal enum class AudioFocus {

    LOST,
    DUCK,
    FOCUS;

    companion object {
        fun fromPlatformFocus(audioFocus: Int): AudioFocus? = when (audioFocus) {
            AudioManager.AUDIOFOCUS_GAIN -> FOCUS
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT -> FOCUS
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK -> FOCUS
            AudioManager.AUDIOFOCUS_LOSS -> LOST
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> LOST
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> DUCK
            else -> null
        }
    }
}
