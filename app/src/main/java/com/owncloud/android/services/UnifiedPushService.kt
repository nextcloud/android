/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Simon Gougeon <git@sgougeon.fr>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.services

import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.jobs.BackgroundJobManager
import com.owncloud.android.datamodel.WebPushJobData
import com.owncloud.android.lib.common.utils.Log_OC
import dagger.android.AndroidInjection
import org.json.JSONException
import org.json.JSONObject
import org.unifiedpush.android.connector.FailedReason
import org.unifiedpush.android.connector.PushService
import org.unifiedpush.android.connector.data.PushEndpoint
import org.unifiedpush.android.connector.data.PushMessage
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

    override fun onNewEndpoint(endpoint: PushEndpoint, instance: String) {
        Log_OC.d(TAG, "Received new endpoint for $instance")
        // No reason to fail with the default key manager
        val key = endpoint.pubKeySet ?: return
        backgroundJobManager.startWebPushJob(
            WebPushJobData.Register(instance, endpoint.url,key.pubKey, key.auth)
        )
    }

    override fun onMessage(message: PushMessage, instance: String) {
        try {
            val token = JSONObject(message.content.toString(Charsets.UTF_8))
                .getString("activationToken")
            Log_OC.d(TAG, "Received activation push notification for $instance")
            backgroundJobManager.startWebPushJob(
                WebPushJobData.Activate(instance, token)
            )
        } catch (_: JSONException) {
            Log_OC.d(TAG, "Received push notification for $instance")
            // Messages are encrypted following RFC8291, and UnifiedPush lib handle the decryption itself:
            // message.content is the cleartext
            backgroundJobManager.startDecryptedNotificationJob(instance, message.content.toString(Charsets.UTF_8))
        }
    }

    override fun onRegistrationFailed(reason: FailedReason, instance: String) {
        Log_OC.d(TAG, "Registration failed for $instance: $reason")
    }

    override fun onUnregistered(instance: String) {
        Log_OC.d(TAG, "Unregistered: $instance")
        backgroundJobManager.startWebPushJob(WebPushJobData.Unregister(instance))
        backgroundJobManager.mayResetUnifiedPush()
    }

    companion object {
        const val TAG = "UnifiedPushService"
    }
}
