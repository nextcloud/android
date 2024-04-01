/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper_ozturk@proton.me>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.utils

import android.app.Notification
import android.app.Service
import android.os.Build
import androidx.core.app.ServiceCompat
import androidx.work.ForegroundInfo
import com.owncloud.android.datamodel.ForegroundServiceType

object ForegroundServiceHelper {
    fun startService(
        service: Service,
        id: Int,
        notification: Notification,
        foregroundServiceType: ForegroundServiceType
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                service,
                id,
                notification,
                foregroundServiceType.getId()
            )
        } else {
            service.startForeground(id, notification)
        }
    }

    fun createWorkerForegroundInfo(
        id: Int,
        notification: Notification,
        foregroundServiceType: ForegroundServiceType
    ): ForegroundInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(id, notification, foregroundServiceType.getId())
        } else {
            ForegroundInfo(id, notification)
        }
    }
}
