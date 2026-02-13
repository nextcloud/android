/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.services

import android.util.Log
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.utils.LinkHelper.APP_NEXTCLOUD_TALK
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.resources.notifications.ActivateWebPushRegistrationOperation
import com.owncloud.android.lib.resources.notifications.RegisterAccountDeviceForWebPushOperation
import com.owncloud.android.lib.resources.notifications.UnregisterAccountDeviceForWebPushOperation
import dagger.android.AndroidInjection
import org.json.JSONException
import org.json.JSONObject
import org.unifiedpush.android.connector.FailedReason
import org.unifiedpush.android.connector.PushService
import org.unifiedpush.android.connector.data.PushEndpoint
import org.unifiedpush.android.connector.data.PushMessage
import java.util.concurrent.Executors
import javax.inject.Inject

class UnifiedPushService: PushService() {
    @Inject
    lateinit var accountManager: UserAccountManager
    @Inject
    lateinit var backgroundJobManager: BackgroundJobManager

    override fun onCreate() {
        super.onCreate()
        AndroidInjection.inject(this)
    }

    private fun apptypes(): List<String> = packageManager
        .getLaunchIntentForPackage(APP_NEXTCLOUD_TALK)?.let {
            listOf("all", "-talk")
        } ?: listOf("all")

    override fun onNewEndpoint(endpoint: PushEndpoint, instance: String) {
        Log.d(TAG, "Received new endpoint for $instance")
        // No reason to fail with the default key manager
        val key = endpoint.pubKeySet ?: return
        Executors.newSingleThreadExecutor().execute {
            val ocAccount = OwnCloudAccount(accountManager.getAccountByName(instance), this)
            val mClient = OwnCloudClientManagerFactory.getDefaultSingleton().getNextcloudClientFor(ocAccount, this)
            RegisterAccountDeviceForWebPushOperation(
                endpoint = endpoint.url,
                auth = key.auth,
                uaPublicKey = key.pubKey,
                appTypes = apptypes()
            ).execute(mClient)
        }
    }

    override fun onMessage(message: PushMessage, instance: String) {
        Log.d(TAG, "Received new message for $instance")
        try {
            val mObj = JSONObject(message.content.toString(Charsets.UTF_8))
            val token = mObj.getString("activationToken")
            Executors.newSingleThreadExecutor().execute {
                val ocAccount = OwnCloudAccount(accountManager.getAccountByName(instance), this)
                val mClient = OwnCloudClientManagerFactory.getDefaultSingleton().getNextcloudClientFor(ocAccount, this)
                ActivateWebPushRegistrationOperation(token)
                    .execute(mClient)
            }
        } catch (_: JSONException) {
            // Messages are encrypted following RFC8291, and UnifiedPush lib handle the decryption itself:
            // message.content is the cleartext
            backgroundJobManager.startDecryptedNotificationJob(instance, message.content.toString(Charsets.UTF_8))
        }
    }

    override fun onRegistrationFailed(reason: FailedReason, instance: String) {
        Log.d(TAG, "Registration failed for $instance: $reason")
    }

    override fun onUnregistered(instance: String) {
        Log.d(TAG, "Unregistered: $instance")
        Executors.newSingleThreadExecutor().execute {
            val ocAccount = OwnCloudAccount(accountManager.getAccountByName(instance), this)
            val mClient = OwnCloudClientManagerFactory.getDefaultSingleton().getNextcloudClientFor(ocAccount, this)
            UnregisterAccountDeviceForWebPushOperation()
                .execute(mClient)
        }
    }

    companion object {
        const val TAG = "UnifiedPushService"
    }
}