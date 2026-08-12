/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.e2ee.vault

import android.app.Activity
import android.view.WindowManager
import java.util.WeakHashMap

object E2eeVaultSecureWindowManager {
    private val previousSecureStateByActivity = WeakHashMap<Activity, Boolean>()
    private val referenceCountByActivity = WeakHashMap<Activity, Int>()

    @Synchronized
    fun enable(activity: Activity) {
        val referenceCount = referenceCountByActivity[activity]
        if (referenceCount != null) {
            referenceCountByActivity[activity] = referenceCount + 1
            return
        }

        previousSecureStateByActivity[activity] = activity.isSecureFlagSet()
        referenceCountByActivity[activity] = 1
        activity.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    @Synchronized
    fun disable(activity: Activity) {
        val referenceCount = referenceCountByActivity[activity] ?: return
        if (referenceCount > 1) {
            referenceCountByActivity[activity] = referenceCount - 1
        } else {
            referenceCountByActivity.remove(activity)
            val wasSecureBefore = previousSecureStateByActivity.remove(activity) ?: false
            if (!wasSecureBefore) {
                activity.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

    private fun Activity.isSecureFlagSet(): Boolean =
        window?.attributes?.flags?.and(WindowManager.LayoutParams.FLAG_SECURE) == WindowManager.LayoutParams.FLAG_SECURE
}
