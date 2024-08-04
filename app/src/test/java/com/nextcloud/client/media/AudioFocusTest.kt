/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.media

import android.media.AudioManager
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AudioFocusTest {
    private companion object {
        const val INVALID_FOCUS = -10000
    }

    @Test
    fun `invalid values result in null`() {
        val focus = AudioFocus.fromPlatformFocus(INVALID_FOCUS)
        assertNull(focus)
    }

    @Test
    fun `audio focus values are converted`() {
        val validValues = listOf(
            AudioManager.AUDIOFOCUS_GAIN,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK
        )
        validValues.forEach {
            val focus = AudioFocus.fromPlatformFocus(-it)
            assertNotNull(focus)
        }
    }
}
