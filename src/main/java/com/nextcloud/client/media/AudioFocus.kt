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
