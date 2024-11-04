/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import android.content.RestrictionsManager

@Suppress("UNCHECKED_CAST")
fun <T> RestrictionsManager.getRestriction(key: String, defaultValue: T): T {
    val appRestrictions = getApplicationRestrictions()
    return when (defaultValue) {
        is String -> appRestrictions.getString(key) as T? ?: defaultValue
        is Int -> appRestrictions.getInt(key) as T? ?: defaultValue
        is Boolean -> appRestrictions.getBoolean(key) as T? ?: defaultValue
        else -> defaultValue
    }
}
