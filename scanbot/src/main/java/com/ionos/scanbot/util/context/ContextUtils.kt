/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.util.context

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

internal fun Context.getActivity(): Activity? {
    return getActivity<Activity>(Activity::class.java)
}

internal fun <T> Context.getActivity(activityType: Class<T>): T? = when {
    activityType.isInstance(this) -> activityType.cast(this)
    this is ContextWrapper -> baseContext.getActivity<T>(activityType)
    else -> null
}

internal fun Context.isActivityFinishing(): Boolean {
    return this is Activity && this.isFinishing
}
