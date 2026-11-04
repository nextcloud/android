/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.network.local

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.nextcloud.android.common.core.network.LocalNetworkAddress
import com.nextcloud.android.common.core.network.LocalNetworkPermission
import com.nextcloud.android.common.core.network.LocalNetworkRoutes
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.di.ApplicationScope
import com.nextcloud.client.network.ConnectivityService
import com.owncloud.android.R
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.notifications.NotificationUtils
import com.owncloud.android.utils.FilesSyncHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LocalNetworkPermissionHandler @Inject constructor(
    private val context: Context,
    private val accountManager: UserAccountManager,
    private val uploadsStorageManager: UploadsStorageManager,
    private val connectivityService: ConnectivityService,
    private val powerManagementService: PowerManagementService,
    private val notificationManager: NotificationManager,
    private val routes: LocalNetworkRoutes,
    @param:ApplicationScope private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "LocalNetworkPermissionHandler"
        private const val NOTIFICATION_ID = 1717
        const val ACTION_REQUEST_PERMISSION = "com.nextcloud.client.network.REQUEST_LOCAL_NETWORK_PERMISSION"
    }

    val serverUrls: List<String>
        get() = accountManager.allUsers.map { it.server.uri.toString() }

    suspend fun hasLocalServer(serverUrls: List<String>): Boolean = withContext(Dispatchers.IO) {
        serverUrls.any { LocalNetworkAddress.isLocalServer(it, routes) }
    }

    fun notifyIfPermissionMissing() {
        if (!LocalNetworkPermission.isRequired(context)) {
            return
        }

        notifyIfLocalServerUnreachable()
    }

    fun onPermissionGranted() {
        Log_OC.d(TAG, "local network permission granted, restarting failed uploads")
        notificationManager.cancel(NOTIFICATION_ID)

        // only this can be retried right away
        FilesSyncHelper.restartUploadsIfNeeded(
            uploadsStorageManager,
            accountManager,
            connectivityService,
            powerManagementService
        )
    }

    @RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
    private fun notifyIfLocalServerUnreachable() {
        if (LocalNetworkPermission.isGranted(context)) {
            notificationManager.cancel(NOTIFICATION_ID)
            return
        }

        scope.launch {
            if (!hasLocalServer(serverUrls)) {
                return@launch
            }

            Log_OC.w(TAG, "local network permission missing, background operations can't reach the server")
            notificationManager.notify(NOTIFICATION_ID, buildNotification())
        }
    }

    private fun buildNotification(): Notification {
        val appName = context.getString(R.string.app_name)
        val message = context.getString(R.string.local_network_permission_notification, appName)

        return NotificationCompat.Builder(context, NotificationUtils.NOTIFICATION_CHANNEL_GENERAL)
            .setSmallIcon(R.drawable.notification_icon)
            .setContentTitle(context.getString(R.string.local_network_permission_title))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(buildContentIntent())
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun buildContentIntent(): PendingIntent {
        val intent = Intent(context, FileDisplayActivity::class.java).apply {
            action = ACTION_REQUEST_PERMISSION
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        return PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
