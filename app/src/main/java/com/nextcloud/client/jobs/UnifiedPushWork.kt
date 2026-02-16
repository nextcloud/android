/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.utils.LinkHelper.APP_NEXTCLOUD_TALK
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.resources.notifications.ActivateWebPushRegistrationOperation
import com.owncloud.android.lib.resources.notifications.RegisterAccountDeviceForWebPushOperation
import com.owncloud.android.lib.resources.notifications.UnregisterAccountDeviceForWebPushOperation

class UnifiedPushWork(
    private val context: Context,
    params: WorkerParameters,
    private val accountManager: UserAccountManager
) : Worker(context, params)  {
    override fun doWork(): Result {
        when (val action = inputData.getString(ACTION)) {
            ACTION_ACTIVATE -> activate()
            ACTION_REGISTER -> register()
            ACTION_UNREGISTER -> unregister()
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

    companion object {
        private const val TAG = "UnifiedPushWork"
        const val ACTION = "action"
        const val ACTION_ACTIVATE = "action.activate"
        const val ACTION_REGISTER = "action.register"
        const val ACTION_UNREGISTER = "action.unregister"
        const val EXTRA_ACCOUNT = "account"
        const val EXTRA_TOKEN = "token"
        const val EXTRA_URL = "url"
        const val EXTRA_UA_PUBKEY = "uaPubkey"
        const val EXTRA_AUTH = "auth"
    }
}