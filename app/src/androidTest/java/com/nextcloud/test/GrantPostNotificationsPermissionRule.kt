/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.test

import android.Manifest
import android.os.Build
import androidx.test.rule.GrantPermissionRule

/**
 * POST_NOTIFICATIONS is a runtime permission that only exists on API 33+. Granting it on lower API
 * levels fails with "Unknown permission", so it is added to the granted set only when supported.
 */
object GrantPostNotificationsPermissionRule {

    fun grant(vararg permissions: String): GrantPermissionRule {
        val granted = permissions.toMutableList()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            granted.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return GrantPermissionRule.grant(*granted.toTypedArray())
    }
}
