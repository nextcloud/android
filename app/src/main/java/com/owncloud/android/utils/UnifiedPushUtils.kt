/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.utils

import android.accounts.Account
import android.app.Activity
import android.content.Context
import android.util.Log
import com.nextcloud.client.account.UserAccountManager
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.resources.notifications.GetVAPIDOperation
import com.owncloud.android.utils.theme.CapabilityUtils
import org.unifiedpush.android.connector.UnifiedPush
import java.util.concurrent.Executors

object UnifiedPushUtils {
    private val TAG: String = UnifiedPushUtils::class.java.getSimpleName()

    @JvmStatic
    fun useDefaultDistributor(activity: Activity, userAccountManager: UserAccountManager) {
        Log.d(TAG, "Using default UnifiedPush distrbutor")
        UnifiedPush.tryUseCurrentOrDefaultDistributor(activity as Context) {
            userAccountManager.accounts.forEach { account ->
                Executors.newSingleThreadExecutor().execute {
                    registerWebPushForAccount(activity, userAccountManager, account)
                }
            }
        }
    }

    private fun supportsWebPush(context: Context, userAccountManager: UserAccountManager, account: Account): Boolean =
        userAccountManager.getUser(account.name)
            .map { CapabilityUtils.getCapability(it, context).supportsWebPush.isTrue }
            .also { Log.d(TAG, "Found push capability: $it") }
            .orElse(false)

    private fun registerWebPushForAccount(context: Context, userAccountManager: UserAccountManager, account: Account) {
        Log.d(TAG, "Registering web push for ${account.name}")
        if (supportsWebPush(context, userAccountManager, account)) {
            val ocAccount = OwnCloudAccount(account, context)
            val mClient = OwnCloudClientManagerFactory.getDefaultSingleton().getNextcloudClientFor(ocAccount, context)
            val vapidRes = GetVAPIDOperation().execute(mClient)
            if (vapidRes.isSuccess) {
                val vapid = vapidRes.resultData.vapid
                UnifiedPush.register(
                    context,
                    instance = account.name,
                    messageForDistributor = account.name,
                    vapid = vapid
                )
            } else {
                Log.w(TAG, "Couldn't find VAPID for ${account.name}")
            }
        } else {
            Log.d(TAG, "${account.name}'s server doesn't support web push: aborting.")
        }
    }
}