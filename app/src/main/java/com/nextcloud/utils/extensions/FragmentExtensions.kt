/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

inline fun <reified T : Any> Fragment.typedActivity(): T? {
    return if (isAdded && activity != null && activity is T) {
        activity as T
    } else {
        null
    }
}

/**
 * Extension for Java Classes
 */
fun <T : Any> Fragment.getTypedActivity(type: Class<T>): T? {
    return if (isAdded && activity != null && type.isInstance(activity)) {
        type.cast(activity)
    } else {
        null
    }
}

fun Fragment.launchOnMainThread(action: () -> Unit) {
    lifecycleScope.launch(Dispatchers.Main) {
        action()
    }
}
