/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.jobs

import android.accounts.AuthenticatorException
import android.accounts.OperationCanceledException
import android.app.Activity
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.text.TextUtils
import android.util.Base64
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.integrations.deck.DeckApi
import com.owncloud.android.R
import com.owncloud.android.datamodel.DecryptedPushMessage
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.OwnCloudClientFactory
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.notifications.DeleteNotificationRemoteOperation
import com.owncloud.android.lib.resources.notifications.GetNotificationRemoteOperation
import com.owncloud.android.lib.resources.notifications.models.Notification
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.activity.NotificationsActivity
import com.owncloud.android.ui.notifications.NotificationUtils
import com.owncloud.android.utils.PushUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import dagger.android.AndroidInjection
import org.apache.commons.httpclient.HttpMethod
import org.apache.commons.httpclient.HttpStatus
import org.apache.commons.httpclient.methods.DeleteMethod
import org.apache.commons.httpclient.methods.GetMethod
import org.apache.commons.httpclient.methods.PutMethod
import org.apache.commons.httpclient.methods.Utf8PostMethod
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.PrivateKey
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.inject.Inject

@Suppress("LongParameterList")
class NotificationWork constructor(
    private val context: Context,
    params: WorkerParameters,
    private val notificationManager: NotificationManager,
    private val accountManager: UserAccountManager,
    private val deckApi: DeckApi,
    private val viewThemeUtils: ViewThemeUtils
) : Worker(context, params) {

    companion object {
        const val TAG = "NotificationJob"
        const val KEY_NOTIFICATION_ACCOUNT = "KEY_NOTIFICATION_ACCOUNT"
        const val KEY_NOTIFICATION_SUBJECT = "subject"
        const val KEY_NOTIFICATION_SIGNATURE = "signature"
        const val KEY_NOTIFICATION_TYPE = "type"
        private const val KEY_NOTIFICATION_ACTION_LINK = "KEY_NOTIFICATION_ACTION_LINK"
        private const val KEY_NOTIFICATION_ACTION_TYPE = "KEY_NOTIFICATION_ACTION_TYPE"
        private const val PUSH_NOTIFICATION_ID = "PUSH_NOTIFICATION_ID"
        private const val NUMERIC_NOTIFICATION_ID = "NUMERIC_NOTIFICATION_ID"
        const val BACKEND_TYPE_FIREBASE_CLOUD_MESSAGING = 1
        const val BACKEND_TYPE_UNIFIED_PUSH = 2
    }

    @Suppress("TooGenericExceptionCaught", "NestedBlockDepth", "ComplexMethod", "LongMethod") // legacy code
    override fun doWork(): Result {
        try {
            val messageData = inputData.getString(KEY_NOTIFICATION_SUBJECT) ?: ""
            val signatureOrUser = inputData.getString(KEY_NOTIFICATION_SIGNATURE) ?: ""

            if (!TextUtils.isEmpty(messageData) && !TextUtils.isEmpty(signatureOrUser)) {
                val decryptedMessageData: StringBuilder = StringBuilder()
                val accountName: StringBuilder = StringBuilder()

                val type = inputData.getInt(KEY_NOTIFICATION_TYPE, -1)

                // if using firebase cloud messaging (via nextcloud proxy) for push notifications...
                when (type) {
                    BACKEND_TYPE_FIREBASE_CLOUD_MESSAGING -> {
                        val base64DecodedSubject = Base64.decode(messageData, Base64.DEFAULT)
                        val base64DecodedSignature = Base64.decode(signatureOrUser, Base64.DEFAULT)
                        val privateKey = PushUtils.readKeyFromFile(false) as PrivateKey
                        try {
                            val signatureVerification = PushUtils.verifySignature(
                                context,
                                accountManager,
                                base64DecodedSignature,
                                base64DecodedSubject
                            )
                            if (signatureVerification != null && signatureVerification.signatureValid) {
                                accountName.append(signatureVerification.account?.name)
                                val cipher = Cipher.getInstance("RSA/None/PKCS1Padding")
                                cipher.init(Cipher.DECRYPT_MODE, privateKey)
                                decryptedMessageData.append(String(cipher.doFinal(base64DecodedSubject)))
                            }
                        } catch (e1: GeneralSecurityException) {
                            Log_OC.d(TAG, "Error decrypting message ${e1.javaClass.name} ${e1.localizedMessage}")
                            return Result.success()
                        }
                        // else, if using unified push messaging...
                    }
                    BACKEND_TYPE_UNIFIED_PUSH -> {
                        decryptedMessageData.append(messageData)
                        accountName.append(signatureOrUser)
                    }
                    else -> return Result.success()
                }

                // transform string message to object
                val decryptedPushMessage =
                    Gson().fromJson(decryptedMessageData.toString(), DecryptedPushMessage::class.java)

                if (decryptedPushMessage.delete) {
                    notificationManager.cancel(decryptedPushMessage.nid)
                } else if (decryptedPushMessage.deleteAll) {
                    notificationManager.cancelAll()
                } else {
                    val user = accountManager.getUser(accountName)
                        .orElseThrow { RuntimeException() }
                    fetchCompleteNotification(user, decryptedPushMessage)
                }
            }
        } catch (exception: Exception) {
            Log_OC.d(TAG, "Something went very wrong" + exception.localizedMessage)
        }
        return Result.success()
    }

    @Suppress("LongMethod") // legacy code
    private fun sendNotification(notification: Notification, user: User) {
        val randomId = SecureRandom()
        val file = notification.subjectRichParameters["file"]

        val deckActionOverrideIntent = deckApi.createForwardToDeckActionIntent(notification, user)

        val pendingIntent: PendingIntent?
        if (deckActionOverrideIntent.isPresent) {
            pendingIntent = deckActionOverrideIntent.get()
        } else {
            val intent: Intent
            if (file == null) {
                intent = Intent(context, NotificationsActivity::class.java)
            } else {
                intent = Intent(context, FileDisplayActivity::class.java)
                intent.action = Intent.ACTION_VIEW
                intent.putExtra(FileDisplayActivity.KEY_FILE_ID, file.id)
            }
            intent.putExtra(KEY_NOTIFICATION_ACCOUNT, user.accountName)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            pendingIntent = PendingIntent.getActivity(
                context,
                notification.getNotificationId(),
                intent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val pushNotificationId = randomId.nextInt()
        val notificationBuilder = NotificationCompat.Builder(context, NotificationUtils.NOTIFICATION_CHANNEL_PUSH)
            .setSmallIcon(R.drawable.notification_icon)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.notification_icon))
            .setShowWhen(true)
            .setSubText(user.accountName)
            .setContentTitle(notification.getSubject())
            .setContentText(notification.getMessage())
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(pendingIntent)

        viewThemeUtils.androidx.themeNotificationCompatBuilder(context, notificationBuilder)

        // Remove
        if (notification.getActions().isEmpty()) {
            val disableDetection = Intent(context, NotificationReceiver::class.java)
            disableDetection.putExtra(NUMERIC_NOTIFICATION_ID, notification.getNotificationId())
            disableDetection.putExtra(PUSH_NOTIFICATION_ID, pushNotificationId)
            disableDetection.putExtra(KEY_NOTIFICATION_ACCOUNT, user.accountName)
            val disableIntent = PendingIntent.getBroadcast(
                context,
                pushNotificationId,
                disableDetection,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            notificationBuilder.addAction(
                NotificationCompat.Action(
                    R.drawable.ic_close,
                    context.getString(R.string.remove_push_notification),
                    disableIntent
                )
            )
        } else {
            // Actions
            for (action in notification.getActions()) {
                val actionIntent = Intent(context, NotificationReceiver::class.java)
                actionIntent.putExtra(NUMERIC_NOTIFICATION_ID, notification.getNotificationId())
                actionIntent.putExtra(PUSH_NOTIFICATION_ID, pushNotificationId)
                actionIntent.putExtra(KEY_NOTIFICATION_ACCOUNT, user.accountName)
                actionIntent.putExtra(KEY_NOTIFICATION_ACTION_LINK, action.link)
                actionIntent.putExtra(KEY_NOTIFICATION_ACTION_TYPE, action.type)
                val actionPendingIntent = PendingIntent.getBroadcast(
                    context,
                    randomId.nextInt(),
                    actionIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                var icon: Int
                icon = if (action.primary) {
                    R.drawable.ic_check_circle
                } else {
                    R.drawable.ic_check_circle_outline
                }
                notificationBuilder.addAction(NotificationCompat.Action(icon, action.label, actionPendingIntent))
            }
        }
        notificationBuilder.setPublicVersion(
            NotificationCompat.Builder(context, NotificationUtils.NOTIFICATION_CHANNEL_PUSH)
                .setSmallIcon(R.drawable.notification_icon)
                .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.notification_icon))
                .setShowWhen(true)
                .setSubText(user.accountName)
                .setContentTitle(context.getString(R.string.new_notification))
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(pendingIntent)
                .also {
                    viewThemeUtils.androidx.themeNotificationCompatBuilder(context, it)
                }
                .build()
        )
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(notification.getNotificationId(), notificationBuilder.build())
    }

    @Suppress("TooGenericExceptionCaught") // legacy code
    private fun fetchCompleteNotification(account: User, decryptedPushMessage: DecryptedPushMessage) {
        val optionalUser = accountManager.getUser(account.accountName)
        if (!optionalUser.isPresent) {
            Log_OC.e(this, "Account may not be null")
            return
        }
        val user = optionalUser.get()
        try {
            val client = OwnCloudClientFactory.createNextcloudClient(user, context)
            val result = GetNotificationRemoteOperation(decryptedPushMessage.nid)
                .execute(client)
            if (result.isSuccess) {
                val notification = result.resultData
                sendNotification(notification, account)
            }
        } catch (e: Exception) {
            Log_OC.e(this, "Error creating account", e)
        }
    }

    class NotificationReceiver : BroadcastReceiver() {
        private lateinit var accountManager: UserAccountManager

        /**
         * This is a workaround for a Dagger compiler bug - it cannot inject
         * into a nested Kotlin class for some reason, but the helper
         * works.
         */
        @Inject
        fun inject(accountManager: UserAccountManager) {
            this.accountManager = accountManager
        }

        @Suppress("ComplexMethod") // legacy code
        override fun onReceive(context: Context, intent: Intent) {
            AndroidInjection.inject(this, context)
            val numericNotificationId = intent.getIntExtra(NUMERIC_NOTIFICATION_ID, 0)
            val accountName = intent.getStringExtra(KEY_NOTIFICATION_ACCOUNT)
            if (numericNotificationId != 0) {
                Thread(
                    Runnable {
                        val notificationManager = context.getSystemService(
                            Activity.NOTIFICATION_SERVICE
                        ) as NotificationManager
                        var oldNotification: android.app.Notification? = null
                        for (statusBarNotification in notificationManager.activeNotifications) {
                            if (numericNotificationId == statusBarNotification.id) {
                                oldNotification = statusBarNotification.notification
                                break
                            }
                        }
                        cancel(context, numericNotificationId)
                        try {
                            val optionalUser = accountManager.getUser(accountName)
                            if (optionalUser.isPresent) {
                                val user = optionalUser.get()
                                val client = OwnCloudClientManagerFactory.getDefaultSingleton()
                                    .getClientFor(user.toOwnCloudAccount(), context)
                                val nextcloudClient = OwnCloudClientFactory.createNextcloudClient(user, context)
                                val actionType = intent.getStringExtra(KEY_NOTIFICATION_ACTION_TYPE)
                                val actionLink = intent.getStringExtra(KEY_NOTIFICATION_ACTION_LINK)
                                val success: Boolean = if (!actionType.isNullOrEmpty() && !actionLink.isNullOrEmpty()) {
                                    val resultCode = executeAction(actionType, actionLink, client)
                                    resultCode == HttpStatus.SC_OK || resultCode == HttpStatus.SC_ACCEPTED
                                } else {
                                    DeleteNotificationRemoteOperation(numericNotificationId)
                                        .execute(nextcloudClient).isSuccess
                                }
                                if (success) {
                                    if (oldNotification == null) {
                                        cancel(context, numericNotificationId)
                                    }
                                } else {
                                    notificationManager.notify(numericNotificationId, oldNotification)
                                }
                            }
                        } catch (e: IOException) {
                            Log_OC.e(TAG, "Error initializing client", e)
                        } catch (e: OperationCanceledException) {
                            Log_OC.e(TAG, "Error initializing client", e)
                        } catch (e: AuthenticatorException) {
                            Log_OC.e(TAG, "Error initializing client", e)
                        }
                    }
                ).start()
            }
        }

        @Suppress("ReturnCount") // legacy code
        private fun executeAction(actionType: String, actionLink: String, client: OwnCloudClient): Int {
            val method: HttpMethod
            method = when (actionType) {
                "GET" -> GetMethod(actionLink)
                "POST" -> Utf8PostMethod(actionLink)
                "DELETE" -> DeleteMethod(actionLink)
                "PUT" -> PutMethod(actionLink)
                else -> return 0 // do nothing
            }
            method.setRequestHeader(RemoteOperation.OCS_API_HEADER, RemoteOperation.OCS_API_HEADER_VALUE)
            try {
                return client.executeMethod(method)
            } catch (e: IOException) {
                Log_OC.e(TAG, "Execution of notification action failed: $e")
            }
            return 0
        }

        private fun cancel(context: Context, notificationId: Int) {
            val notificationManager = context.getSystemService(Activity.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(notificationId)
        }
    }
}
