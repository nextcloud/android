/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import androidx.fragment.app.Fragment

inline fun <reified T : Any> Fragment.typedActivity(): T? = if (isAdded && activity != null && activity is T) {
    activity as T
} else {
    null
}

/**
 * Extension for Java Classes
 */
fun <T : Any> Fragment.getTypedActivity(type: Class<T>): T? =
    if (isAdded && activity != null && type.isInstance(activity)) {
        type.cast(activity)
    } else {
        null
    }
