/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.preferences

import androidx.annotation.StringRes
import com.owncloud.android.R

enum class AppLock(val value: String, @StringRes val summaryResId: Int) {
    NONE("none", R.string.prefs_lock_none),
    PASSCODE("passcode", R.string.prefs_lock_using_passcode),
    DEVICE_CREDENTIALS("device_credentials", R.string.prefs_lock_using_device_credentials);

    companion object {
        @JvmStatic
        fun fromValue(value: String?): AppLock = entries.firstOrNull { it.value == value } ?: NONE
    }
}
