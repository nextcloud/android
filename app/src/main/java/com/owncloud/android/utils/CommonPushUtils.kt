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
import com.owncloud.android.lib.resources.notifications.UnregisterAccountDeviceForWebPushOperation
import com.owncloud.android.utils.theme.CapabilityUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.unifiedpush.android.connector.UnifiedPush

/**
 * Handle UnifiedPush (web push server side) and proxy push ([PushUtils]) registrations
 */
object CommonPushUtils {
    private val TAG: String = CommonPushUtils::class.java.getSimpleName()

    /**
     * Register UnifiedPush, or FCM with the current config
     *
     * This is run when the application starts, and this is also where push notifications
     * are set up for the first time
     */
    @JvmStatic
    fun registerCurrentPushConfiguration(context: Context, accountManager: UserAccountManager, preferences: AppPreferences) {
        if (preferences.isUnifiedPushEnabled) {
            UnifiedPush.getAckDistributor(context)?.let {
                registerUnifiedPushForAllAccounts(context, accountManager, preferences.pushToken)
            } ?: run {
                // The user has uninstalled the distributor, fallback to play services with the proxy push if available
                preferences.isUnifiedPushEnabled = false
                disableUnifiedPush(context, accountManager, preferences.pushToken)
            }
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                PushUtils.pushRegistrationToServer(accountManager, preferences.pushToken)
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
    fun useDefaultUnifiedPushDistributor(
        activity: Activity,
        accountManager: UserAccountManager,
        proxyPushToken: String?,
        callback: (String?) -> Unit
    ) {
        Log_OC.d(TAG, "Using default UnifiedPush distributor")
        UnifiedPush.tryUseCurrentOrDefaultDistributor(activity as Context) { res ->
            if (res) {
                registerUnifiedPushForAllAccounts(activity, accountManager, proxyPushToken)
                callback(UnifiedPush.getSavedDistributor(activity))
            } else {
                callback(null)
            }
        }
    }

    /**
     * Pick another distributor, register all accounts that support webpush
     *
     * Unregister proxy push for account if succeed
     * Re-register proxy push for the others
     *
     * @param activity: Context needs to be an activity, to get a result
     * @param accountManager: Used to register all accounts
     * @param callback: run with the push service name if available
     */
    @JvmStatic
    fun pickUnifiedPushDistributor(
        activity: Activity,
        accountManager: UserAccountManager,
        proxyPushToken: String?,
        callback: (String?) -> Unit
    ) {
        Log_OC.d(TAG, "Picking another UnifiedPush distributor")
        UnifiedPush.tryPickDistributor(activity as Context) { res ->
            if (res) {
                registerUnifiedPushForAllAccounts(activity, accountManager, proxyPushToken)
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
        context: Context,
        accountManager: UserAccountManager,
        proxyPushToken: String?
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            for (account in accountManager.getAccounts()) {
                PushUtils.setRegistrationForAccountEnabled(account, true)
                unregisterUnifiedPushForAccount(context, accountManager, OwnCloudAccount(account, context))
            }
            PushUtils.pushRegistrationToServer(accountManager, proxyPushToken)
        }
    }

    @JvmStatic
    fun unregisterUnifiedPushForAccount(
        context: Context,
        accountManager: UserAccountManager,
        account: OwnCloudAccount
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            if (supportsWebPush(context, accountManager, account.name)) {
                val mClient = OwnCloudClientManagerFactory.getDefaultSingleton().getNextcloudClientFor(account, context)
                UnregisterAccountDeviceForWebPushOperation()
                    .execute(mClient)
                UnifiedPush.unregister(context, account.name)
            }
        }
    }

    /**
     * Register UnifiedPush for all accounts with the server VAPID key if the server supports web push
     *
     * Web push is registered on the nc server when the push endpoint is received
     *
     * Proxy push is unregistered for accounts on server with web push support, if a server doesn't support web push, proxy push is re-registered
     */
    private fun registerUnifiedPushForAllAccounts(
        context: Context,
        accountManager: UserAccountManager,
        proxyPushToken: String?
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val jobs = accountManager.accounts.map { account ->
                CoroutineScope(Dispatchers.IO).launch {
                    val ocAccount = OwnCloudAccount(account, context)
                    val res = registerUnifiedPushForAccount(context, accountManager, ocAccount)
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
     * Register UnifiedPush with the server VAPID key if the server supports web push
     *
     * Web push is registered on the nc server when the push endpoint is received
     *
     * @return true if registration succeed
     */
    private fun registerUnifiedPushForAccount(
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