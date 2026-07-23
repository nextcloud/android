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
 * Grants the runtime permissions UI tests need, matching what the manifest actually declares:
 *
 * - WRITE_EXTERNAL_STORAGE is capped at maxSdkVersion="29", so on API 30+ the app never requests it
 *   and granting it throws "has not requested permission WRITE_EXTERNAL_STORAGE". It is therefore
 *   granted only when SDK_INT <= 29.
 * - POST_NOTIFICATIONS only exists on API 33+, so granting it on lower API levels fails with
 *   "Unknown permission" and it is granted only from TIRAMISU onwards.
 */
object GrantTestPermissionRule {

    fun grantStorageAndNotification(): GrantPermissionRule {
        val permissions = buildList {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        return GrantPermissionRule.grant(*permissions.toTypedArray())
    }
}
