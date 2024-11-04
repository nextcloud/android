/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import android.content.RestrictionsManager

fun RestrictionsManager.getRestriction(key: String, defaultValue: String?): String? {
    val appRestrictions = getApplicationRestrictions()

    return if (appRestrictions.containsKey(key)) {
        appRestrictions.getString(key)
    } else {
        defaultValue
    }
}

fun RestrictionsManager.getRestriction(key: String, defaultValue: Int): Int {
    val appRestrictions = getApplicationRestrictions()

    return if (appRestrictions.containsKey(key)) {
        appRestrictions.getInt(key)
    } else {
        defaultValue
    }
}

fun RestrictionsManager.getRestriction(key: String, defaultValue: Boolean): Boolean {
    val appRestrictions = getApplicationRestrictions()

    return if (appRestrictions.containsKey(key)) {
        appRestrictions.getBoolean(key)
    } else {
        defaultValue
    }
}
