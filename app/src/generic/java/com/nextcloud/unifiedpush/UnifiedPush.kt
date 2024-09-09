/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.unifiedpush

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue
import androidx.appcompat.view.ContextThemeWrapper
import androidx.work.WorkManager
import com.google.gson.Gson
import com.nextcloud.client.core.ClockImpl
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.client.jobs.BackgroundJobManagerImpl
import com.nextcloud.client.jobs.NotificationWork
import com.nextcloud.client.preferences.AppPreferencesImpl
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.datamodel.PushConfigurationState
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.activity.DrawerActivity
import com.owncloud.android.utils.PushUtils
import org.unifiedpush.android.connector.ChooseDialog
import org.unifiedpush.android.connector.MessagingReceiver
import org.unifiedpush.android.connector.NoDistributorDialog
import org.unifiedpush.android.connector.RegistrationDialogContent
import org.unifiedpush.android.connector.UnifiedPush

class UnifiedPush : MessagingReceiver() {
    private val TAG: String? = UnifiedPush::class.java.simpleName

    companion object {
        fun registerForPushMessaging(activity: DrawerActivity, accountName: String) {
            if ((activity === null) || (activity.mHandler === null) || (activity.isFinishing === true))
                return

            // if a distributor is registered and available, re-register to ensure in sync
            if (UnifiedPush.getSavedDistributor(activity) !== null) {
                UnifiedPush.registerApp(activity, accountName)
            } else {
                // else, previous distributor has gone away (uninstalled maybe) or there never was one,
                // register now if possible
                activity.mHandler.post {
                    UnifiedPush.registerAppWithDialog(
                        ContextThemeWrapper(activity, R.style.Theme_ownCloud_Dialog),
                        accountName,
                        RegistrationDialogContent(
                            NoDistributorDialog(
                                message = activity.getString(R.string.unified_push_no_distributors_dialog_text),
                                title = activity.getString(R.string.unified_push_choose_distributor_title)
                            ),
                            ChooseDialog(activity.getString(R.string.unified_push_choose_distributor_title))
                        )
                    )
                }
            }
        }

        fun unregisterForPushMessaging(accountName: String) {
            val context = MainApp.getAppContext()

            // unregister with distributor
            UnifiedPush.unregisterApp(context, accountName)

            // delete locally saved endpoint value
            ArbitraryDataProviderImpl(context).deleteKeyForAccount(accountName, PushUtils.KEY_PUSH)
        }
    }

    override fun onMessage(context: Context, message: ByteArray, instance: String) {
        // called when a new message is received. The message contains the full POST body of the push message
        Log_OC.d(TAG, "unified push message received")

        val workManager = WorkManager.getInstance(context)
        val preferences = AppPreferencesImpl.fromContext(context)
        val backgroundJobManager: BackgroundJobManager = BackgroundJobManagerImpl(workManager, ClockImpl(), preferences)
        backgroundJobManager.startNotificationJob(message.toString(Charsets.UTF_8), instance, NotificationWork.BACKEND_TYPE_UNIFIED_PUSH)
    }

    override fun onNewEndpoint(context: Context, endpoint: String, instance: String) {
        // called when a new endpoint is to be used for sending push messages
        val newAccountPushData = PushConfigurationState()
        newAccountPushData.setPushToken(endpoint)
        ArbitraryDataProviderImpl(context).storeOrUpdateKeyValue(instance, PushUtils.KEY_PUSH, Gson().toJson(newAccountPushData))
    }

    override fun onRegistrationFailed(context: Context, instance: String) {
        // called when the registration is not possible, eg. no network
        // just dump the registration to make sure it's cleaned up. re-register will be auto-reattempted
        unregisterForPushMessaging(instance)
    }

    override fun onUnregistered(context: Context, instance: String) {
        // called when this application is remotely unregistered from receiving push messages
        unregisterForPushMessaging(instance)
    }
}