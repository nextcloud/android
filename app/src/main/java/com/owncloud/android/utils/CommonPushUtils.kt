/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.utils

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.BuildConfig
import com.owncloud.android.R
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
import org.unifiedpush.android.connector.data.ResolvedDistributor

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
     *
     * Push notifications are set up for the first time with this function
     */
    @JvmStatic
    fun registerCurrentPushConfiguration(activity: Activity, accountManager: UserAccountManager, preferences: AppPreferences) {
        if (
            (!preferences.isPushInitialized && BuildConfig.DEFAULT_PUSH_UNIFIEDPUSH)
            || preferences.isUnifiedPushEnabled
            ){
            tryUseUnifiedPush(activity, accountManager, preferences) {}
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                // The generic flavor has an embedded distrbutor to work with the
                // Play Services, other flavors work with Proxy-Push
                if (activity.packageName in UnifiedPush.getDistributors(activity)) {
                    UnifiedPush.saveDistributor(activity, activity.packageName)
                    registerUnifiedPushForAllAccounts(activity, accountManager, null)
                } else {
                    PushUtils.pushRegistrationToServer(accountManager, preferences.pushToken)
                }
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
     * Count external push services
     */
    @JvmStatic
    fun countExternalServices(context: Context): Int = UnifiedPush.getDistributors(context)
        .filter { s -> s != context.packageName }
        .size

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
    fun tryUseUnifiedPush(
        activity: Activity,
        accountManager: UserAccountManager,
        preferences: AppPreferences,
        callback: (String?) -> Unit
    ) {
        UnifiedPush.getAckDistributor(activity)?.let {
            Log_OC.d(TAG, "Found ack distributor")
            registerUnifiedPushForAllAccounts(activity, accountManager, preferences.pushToken)
            callback(it)
            return
        }
        when (val res = UnifiedPush.resolveDefaultDistributor(activity)) {
            is ResolvedDistributor.Found ->  {
                Log_OC.d(TAG, "Found default distributor")
                preferences.isUnifiedPushEnabled = true
                UnifiedPush.saveDistributor(activity, res.packageName)
                registerUnifiedPushForAllAccounts(activity, accountManager, preferences.pushToken)
                callback(res.packageName)
            }
            ResolvedDistributor.NoneAvailable -> {
                Log_OC.d(TAG, "No default distributor")
                // Do not change preference
                disableExternalUnifiedPush(activity, accountManager, preferences.pushToken)
                callback(null)
            }
            ResolvedDistributor.ToSelect -> {
                Log_OC.d(TAG, "Default distributor to select")
                activity.runOnUiThread {
                    showDistributorSelectionDialog(activity) { confirmed ->
                        if (confirmed) {
                            UnifiedPush.tryUseDefaultDistributor(activity) { res ->
                                if (res) {
                                    preferences.isUnifiedPushEnabled = true
                                    registerUnifiedPushForAllAccounts(activity, accountManager, preferences.pushToken)
                                    callback(UnifiedPush.getSavedDistributor(activity))
                                } else {
                                    preferences.isUnifiedPushEnabled = false
                                    disableExternalUnifiedPush(activity, accountManager, preferences.pushToken)
                                    callback(null)
                                }
                            }
                        } else {
                            Log_OC.d(TAG, "Default distributor dismissed")
                            preferences.isUnifiedPushEnabled = false
                            disableExternalUnifiedPush(activity, accountManager, preferences.pushToken)
                            callback(null)
                        }
                    }
                }
            }
        }
    }

    /**
     * Inform the user they will have to select a distributor
     *
     * **Should nearly never happen**
     *
     * It is shown only if the user has many distributors, they haven't set a default yet, nor selected a distributor
     */
    private fun showDistributorSelectionDialog(context: Context, onResult: (Boolean) -> Unit) {
        MaterialAlertDialogBuilder(context, R.style.Theme_ownCloud_Dialog)
            .setTitle(context.getString(R.string.unifiedpush))
            .setMessage(context.getString(R.string.select_unifiedpush_service_dialog))
            .setPositiveButton(
                android.R.string.ok
            ) { dialog: DialogInterface?, _: Int ->
                dialog?.dismiss()
                onResult(true)
            }
            .setNegativeButton(
                android.R.string.cancel
            ) { dialog: DialogInterface?, _: Int ->
                dialog?.dismiss()
                onResult(false)
            }
            .create()
            .show()
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
    fun disableExternalUnifiedPush(
        context: Context,
        accountManager: UserAccountManager,
        proxyPushToken: String?
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val hasEmbeddedDistrib = context.packageName in UnifiedPush.getDistributors(context)

            for (account in accountManager.getAccounts()) {
                PushUtils.setRegistrationForAccountEnabled(account, true)
                if (!hasEmbeddedDistrib) unregisterUnifiedPushForAccount(context, accountManager, OwnCloudAccount(account, context))
            }
            // If the app has an embedded distributor, then we try to use it as a fallback
            // This embedded distributor is available only on non-gplay variants
            // where [PushUtils.pushRegistrationToServer] does nothing.
            if (hasEmbeddedDistrib) {
                UnifiedPush.saveDistributor(context, context.packageName)
                registerUnifiedPushForAllAccounts(context, accountManager, null)
            } else {
                PushUtils.pushRegistrationToServer(accountManager, proxyPushToken)
            }
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
    fun registerUnifiedPushForAllAccounts(
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
