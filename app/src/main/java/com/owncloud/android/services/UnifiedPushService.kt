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
        Log.d(TAG, "Received new endpoint for $instance")
        // No reason to fail with the default key manager
        val key = endpoint.pubKeySet ?: return
        backgroundJobManager.registerWebPush(instance, endpoint.url,key.pubKey, key.auth)
    }

    override fun onMessage(message: PushMessage, instance: String) {
        Log.d(TAG, "Received new message for $instance")
        try {
            val mObj = JSONObject(message.content.toString(Charsets.UTF_8))
            val token = mObj.getString("activationToken")
            backgroundJobManager.activateWebPush(instance, token)
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
        backgroundJobManager.unregisterWebPush(instance)
        backgroundJobManager.mayResetUnifiedPush()
    }

    companion object {
        const val TAG = "UnifiedPushService"
    }
}