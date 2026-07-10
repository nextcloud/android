/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.test

import android.Manifest
import android.os.Build
import androidx.test.rule.GrantPermissionRule

/**
 * Grants the runtime permissions UI tests need. POST_NOTIFICATIONS only exists on API 33+, so
 * granting it on lower API levels fails with "Unknown permission" and it is therefore requested only
 * when supported.
 */
object GrantTestPermissionRule {

    fun grantStorageAndNotification(): GrantPermissionRule = if (Build.VERSION.SDK_INT >=
        Build.VERSION_CODES.TIRAMISU
    ) {
        GrantPermissionRule.grant(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
}
