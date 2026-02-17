/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.R
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.notifications.ActivateWebPushRegistrationOperation
import com.owncloud.android.lib.resources.notifications.RegisterAccountDeviceForWebPushOperation
import com.owncloud.android.lib.resources.notifications.UnregisterAccountDeviceForWebPushOperation
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.notifications.NotificationUtils
import com.owncloud.android.utils.CommonPushUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import org.unifiedpush.android.connector.UnifiedPush
import org.unifiedpush.android.connector.data.ResolvedDistributor
import java.security.SecureRandom

class UnifiedPushWork(
    private val context: Context,
    params: WorkerParameters,
    private val accountManager: UserAccountManager,
    private val preferences: AppPreferences,
    private val viewThemeUtils: ViewThemeUtils
) : Worker(context, params)  {
    override fun doWork(): Result {
        when (val action = inputData.getString(ACTION)) {
            ACTION_ACTIVATE -> activate()
            ACTION_REGISTER -> register()
            ACTION_UNREGISTER -> unregister()
            ACTION_MAY_RESET -> mayResetUnifiedPush()
            else -> Log.w(TAG, "Unknown action $action")
        }
        return Result.success()
    }

    private fun register() {
        val url = inputData.getString(EXTRA_URL) ?: run {
            Log.w(TAG, "No url supplied")
            return
        }
        val accountName = inputData.getString(EXTRA_ACCOUNT) ?: run {
            Log.w(TAG, "No account supplied")
            return
        }
        val uaPublicKey = inputData.getString(EXTRA_UA_PUBKEY) ?: run {
            Log.w(TAG, "No uaPubkey supplied")
            return
        }
        val auth = inputData.getString(EXTRA_AUTH) ?: run {
            Log.w(TAG, "No auth supplied")
            return
        }
        val ocAccount = OwnCloudAccount(accountManager.getAccountByName(accountName), context)
        val mClient = OwnCloudClientManagerFactory.getDefaultSingleton().getNextcloudClientFor(ocAccount, context)
        RegisterAccountDeviceForWebPushOperation(
            endpoint = url,
            auth = auth,
            uaPublicKey = uaPublicKey,
            appTypes = appTypes()
        ).execute(mClient)
    }


    private fun appTypes(): List<String> = context.packageManager
        .getLaunchIntentForPackage(APP_NEXTCLOUD_TALK)?.let {
            listOf("all", "-talk")
        } ?: listOf("all")

    private fun activate() {
        val accountName = inputData.getString(EXTRA_ACCOUNT) ?: run {
            Log.w(TAG, "No account supplied")
            return
        }
        val token = inputData.getString(EXTRA_TOKEN) ?: run {
            Log.w(TAG, "No account supplied")
            return
        }
        val ocAccount = OwnCloudAccount(accountManager.getAccountByName(accountName), context)
        val mClient = OwnCloudClientManagerFactory.getDefaultSingleton().getNextcloudClientFor(ocAccount, context)
        ActivateWebPushRegistrationOperation(token)
            .execute(mClient)
    }

    private fun unregister() {
        val accountName = inputData.getString(EXTRA_ACCOUNT) ?: run {
            Log.w(TAG, "No account supplied")
            return
        }
        val ocAccount = OwnCloudAccount(accountManager.getAccountByName(accountName), context)
        val mClient = OwnCloudClientManagerFactory.getDefaultSingleton().getNextcloudClientFor(ocAccount, context)
        UnregisterAccountDeviceForWebPushOperation()
            .execute(mClient)
    }

    /**
     * We received one or many unregistration 10 seconds ago:
     * - If we are still registered to the distributor, we re-register to it
     * - If we still have a default distributor, we register to it
     * - Else we show a notification to ask the user to open the application
     * so notifications can be reset
     */
    fun mayResetUnifiedPush() {
        UnifiedPush.getAckDistributor(context)?.let {
            Log.d(TAG, "Ack distributor still available")
            CommonPushUtils.registerUnifiedPushForAllAccounts(context, accountManager, preferences.pushToken)
        }
        when (val res = UnifiedPush.resolveDefaultDistributor(context)) {
            is ResolvedDistributor.Found -> {
                Log.d(TAG, "Found new distributor default")
                UnifiedPush.saveDistributor(context, res.packageName)
                CommonPushUtils.registerUnifiedPushForAllAccounts(context, accountManager, preferences.pushToken)
            }
            ResolvedDistributor.NoneAvailable,
            ResolvedDistributor.ToSelect -> {
                Log.d(TAG, "No default distributor: $res")
                showNotificationToResetPush()
            }
        }
    }

    private fun showNotificationToResetPush() {
        val pushNotificationId = SecureRandom().nextInt()
        val intent = Intent(context, FileDisplayActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags =  Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            pushNotificationId,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val notificationBuilder = NotificationCompat.Builder(context, NotificationUtils.NOTIFICATION_CHANNEL_PUSH)
            .setSmallIcon(R.drawable.notification_icon)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.notification_icon))
            .setShowWhen(true)
            .setAutoCancel(true)
            .setContentTitle(context.getString(R.string.push_notifications))
            .setContentText(context.getString(R.string.notif_push_unregistered))
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.notif_push_unregistered)))
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(pendingIntent)

        viewThemeUtils.androidx.themeNotificationCompatBuilder(context, notificationBuilder)
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log_OC.w(this, "Missing permission to post notifications")
        } else {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(pushNotificationId, notificationBuilder.build())
        }
    }

    companion object {
        private const val TAG = "UnifiedPushWork"
        const val APP_NEXTCLOUD_TALK = "com.nextcloud.talk2"
        const val ACTION = "action"
        const val ACTION_ACTIVATE = "action.activate"
        const val ACTION_REGISTER = "action.register"
        const val ACTION_UNREGISTER = "action.unregister"
        const val ACTION_MAY_RESET = "action.mayReset"
        const val EXTRA_ACCOUNT = "account"
        const val EXTRA_TOKEN = "token"
        const val EXTRA_URL = "url"
        const val EXTRA_UA_PUBKEY = "uaPubkey"
        const val EXTRA_AUTH = "auth"
    }
}