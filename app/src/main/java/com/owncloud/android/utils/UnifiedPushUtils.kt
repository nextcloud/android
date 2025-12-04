/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.utils

import android.app.Activity
import android.content.Context
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.notifications.GetVAPIDOperation
import com.owncloud.android.utils.theme.CapabilityUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.unifiedpush.android.connector.UnifiedPush

object UnifiedPushUtils {
    private val TAG: String = UnifiedPushUtils::class.java.getSimpleName()

    /**
     * Use default distributor, register all accounts that support webpush
     *
     * Unregister proxy push for account if succeed
     * Re-register proxy push for the others
     *
     * @param activity: Context needs to be an activity, to get a result
     * @param accountManager: Used to register all accounts
     * @param callback: run with the push service name if available
     */
    @JvmStatic
    fun useDefaultDistributor(
        activity: Activity,
        accountManager: UserAccountManager,
        proxyPushToken: String?,
        callback: (String?) -> Unit
    ) {
        Log_OC.d(TAG, "Using default UnifiedPush distrbutor")
        UnifiedPush.tryUseCurrentOrDefaultDistributor(activity as Context) { res ->
            if (res) {
                registerAllAccounts(activity, accountManager, proxyPushToken)
                callback(UnifiedPush.getSavedDistributor(activity))
            } else {
                callback(null)
            }
        }
    }

    /**
     * Disable UnifiedPush and try to register with proxy push again
     */
    @JvmStatic
    fun disableUnifiedPush(
        accountManager: UserAccountManager,
        proxyPushToken: String?
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            for (account in accountManager.getAccounts()) {
                PushUtils.setRegistrationForAccountEnabled(account, true)
            }
            PushUtils.pushRegistrationToServer(accountManager, proxyPushToken)
        }
    }

    /**
     * Register UnifiedPush, or FCM with the current config
     */
    @JvmStatic
    fun registerCurrentPushConfiguration(context: Context, accountManager: UserAccountManager, preferences: AppPreferences) {
        if (preferences.isUnifiedPushEnabled) {
            UnifiedPush.getAckDistributor(context)?.let {
                registerAllAccounts(context, accountManager, preferences.pushToken)
            } ?: run {
                // The user has uninstalled the distributor, fallback to play services with the proxy push if available
                preferences.isUnifiedPushEnabled = false
                disableUnifiedPush(accountManager, preferences.pushToken)
            }
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                PushUtils.pushRegistrationToServer(accountManager, preferences.pushToken)
            }
        }
    }

    private fun registerAllAccounts(
        context: Context,
        accountManager: UserAccountManager,
        proxyPushToken: String?
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val jobs = accountManager.accounts.map { account ->
                CoroutineScope(Dispatchers.IO).launch {
                    val ocAccount = OwnCloudAccount(account, context)
                    val res = registerWebPushForAccount(context, accountManager, ocAccount)
                    if (res) {
                        PushUtils.setRegistrationForAccountEnabled(account, false)
                    }
                }
            }
            jobs.joinAll()
            proxyPushToken?.let {
                PushUtils.pushRegistrationToServer(accountManager, it)
            }
        }
    }

    /**
     * Check if server supports web push
     */
    private fun supportsWebPush(context: Context, accountManager: UserAccountManager, accountName: String): Boolean =
        accountManager.getUser(accountName)
            .map { CapabilityUtils.getCapability(it, context).supportsWebPush.isTrue }
            .also { Log_OC.d(TAG, "Found push capability: $it") }
            .orElse(false)

    /**
     * Register web push on the server if supported
     *
     * @return true if registration succeed
     */
    private fun registerWebPushForAccount(
        context: Context,
        accountManager: UserAccountManager,
        account: OwnCloudAccount
    ): Boolean {
        Log_OC.d(TAG, "Registering web push for ${account.name}")
        if (supportsWebPush(context, accountManager, account.name)) {
            val mClient = OwnCloudClientManagerFactory.getDefaultSingleton().getNextcloudClientFor(account, context)
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
                Log_OC.w(TAG, "Couldn't find VAPID for ${account.name}")
            }
            return vapidRes.isSuccess
        } else {
            Log_OC.d(TAG, "${account.name}'s server doesn't support web push: aborting.")
            return false
        }
    }
}