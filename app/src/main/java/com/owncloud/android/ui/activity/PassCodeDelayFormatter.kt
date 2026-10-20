/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity

import android.content.res.Resources
import com.owncloud.android.R
import com.nextcloud.utils.AppConstants.SECONDS_PER_MINUTE

class PassCodeDelayFormatter(private val resources: Resources) {

    fun getExplanationText(timeInSeconds: Int): String {
        val minutes = timeInSeconds / SECONDS_PER_MINUTE
        val remainingSeconds = timeInSeconds % SECONDS_PER_MINUTE

        return when {
            timeInSeconds < SECONDS_PER_MINUTE -> resources.getQuantityString(
                R.plurals.passcode_delay_seconds,
                timeInSeconds,
                timeInSeconds
            )

            remainingSeconds == 0 -> resources.getQuantityString(
                R.plurals.passcode_delay_minutes,
                minutes,
                minutes
            )

            else -> resources.getQuantityString(
                R.plurals.passcode_delay_minutes_seconds,
                minutes,
                minutes,
                remainingSeconds
            )
        }
    }
}
