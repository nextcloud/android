/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.utils

import android.app.Notification
import android.app.Service
import android.os.Build
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.work.ForegroundInfo
import com.owncloud.android.datamodel.ForegroundServiceType

object ForegroundServiceHelper {
    private const val TAG = "ForegroundServiceHelper"
    private val isAboveOrEqualAndroid10 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    @Suppress("TooGenericExceptionCaught")
    fun startService(
        service: Service,
        id: Int,
        notification: Notification,
        foregroundServiceType: ForegroundServiceType
    ) {
        if (isAboveOrEqualAndroid10) {
            try {
                ServiceCompat.startForeground(
                    service,
                    id,
                    notification,
                    foregroundServiceType.getId()
                )
            } catch (e: Exception) {
                Log.d(TAG, "Exception caught at ForegroundServiceHelper.startService: $e")
            }
        } else {
            service.startForeground(id, notification)
        }
    }

    fun createWorkerForegroundInfo(
        id: Int,
        notification: Notification,
        foregroundServiceType: ForegroundServiceType
    ): ForegroundInfo {
        return if (isAboveOrEqualAndroid10) {
            ForegroundInfo(id, notification, foregroundServiceType.getId())
        } else {
            ForegroundInfo(id, notification)
        }
    }
}
