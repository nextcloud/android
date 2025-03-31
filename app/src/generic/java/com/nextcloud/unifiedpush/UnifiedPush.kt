/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.unifiedpush

import android.content.Context
import android.os.PowerManager
import androidx.work.WorkManager
import com.google.gson.Gson
import com.nextcloud.client.core.ClockImpl
import com.nextcloud.client.jobs.BackgroundJobManagerImpl
import com.nextcloud.client.jobs.NotificationWork
import com.nextcloud.client.preferences.AppPreferencesImpl
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.datamodel.PushConfigurationState
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.activity.DrawerActivity
import com.owncloud.android.utils.PushUtils
import org.unifiedpush.android.connector.FailedReason
import org.unifiedpush.android.connector.PushService
import org.unifiedpush.android.connector.UnifiedPush
import org.unifiedpush.android.connector.data.PushEndpoint
import org.unifiedpush.android.connector.data.PushMessage
import org.unifiedpush.android.connector.ui.SelectDistributorDialogsBuilder
import org.unifiedpush.android.connector.ui.UnifiedPushFunctions

class UnifiedPush : PushService() {
    companion object {
        private val TAG: String? = UnifiedPush::class.java.simpleName
        private const val WAKELOCK_TIMEOUT = 5000L

        fun registerForPushMessaging(activity: DrawerActivity?, accountName: String, forceChoose: Boolean) {
            if ((activity === null) || (activity.mHandler === null) || activity.isFinishing)
                return

            // run on ui thread
            activity.mHandler.post {
                SelectDistributorDialogsBuilder(
                    activity,
                    object : UnifiedPushFunctions {
                        override fun tryUseDefaultDistributor(callback: (Boolean) -> Unit) =
                            UnifiedPush.tryUseDefaultDistributor(activity, callback).also {
                                Log_OC.d(TAG, "tryUseDefaultDistributor()")
                            }

                        override fun getAckDistributor(): String? =
                            UnifiedPush.getAckDistributor(activity).also {
                                Log_OC.d(TAG, "getAckDistributor() = $it")
                            }

                        override fun getDistributors(): List<String> =
                            UnifiedPush.getDistributors(activity).also {
                                Log_OC.d(TAG, "getDistributors() = $it")
                            }

                        override fun register(instance: String) =
                            UnifiedPush.register(activity, instance).also {
                                Log_OC.d(TAG, "register($instance)")
                            }

                        override fun saveDistributor(distributor: String) =
                            UnifiedPush.saveDistributor(activity, distributor).also {
                                Log_OC.d(TAG, "saveDistributor($distributor)")
                            }
                    }
                ).apply {
                    instances = listOf(accountName)
                    mayUseCurrent = !forceChoose
                    mayUseDefault = !forceChoose
                }.run()
            }
        }

        fun unregisterForPushMessaging(context: Context, accountName: String) {
            // unregister with distributor
            UnifiedPush.unregister(context, accountName)

            // delete locally saved endpoint value
            ArbitraryDataProviderImpl(context).deleteKeyForAccount(accountName, PushUtils.KEY_PUSH)
        }
    }

    override fun onMessage(message: PushMessage, instance: String) {
        // get a wake lock to 'help' background job run more promptly since it can take minutes to run if phone is
        // sleeping/dozing - 5 secs should be well long enough to get the notification displayed
        val wakeLock = (getSystemService(POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "nc:timedPartialwakelock")
        wakeLock?.acquire(WAKELOCK_TIMEOUT)

        // called when a new message is received. The message contains the full POST body of the push message
        Log_OC.d(TAG, "unified push message received")

        BackgroundJobManagerImpl(
            WorkManager.getInstance(this), ClockImpl(), AppPreferencesImpl.fromContext(this)
        ).startNotificationJob(
            message.content.toString(Charsets.UTF_8),
            instance,
            NotificationWork.BACKEND_TYPE_UNIFIED_PUSH
        )
    }

    override fun onNewEndpoint(endpoint: PushEndpoint, instance: String) {
        Log_OC.d(TAG, "onNewEndpoint(${endpoint.url}, $instance)")

        val newAccountPushData = PushConfigurationState()
        newAccountPushData.setPushToken(endpoint.url)
        ArbitraryDataProviderImpl(this).storeOrUpdateKeyValue(
            instance,
            PushUtils.KEY_PUSH,
            Gson().toJson(newAccountPushData)
        )
    }

    override fun onRegistrationFailed(reason: FailedReason, instance: String) =
        // the registration is not possible, eg. no network
        // force unregister to make sure cleaned up. re-register will be re-attempted next time
        UnifiedPush.unregister(this, instance).also {
            Log_OC.d(TAG, "onRegistrationFailed(${reason.name}, $instance)")
        }

    override fun onUnregistered(instance: String) =
        // this application is unregistered by the distributor from receiving push messages
        // force unregister to make sure cleaned up. re-register will be re-attempted next time
        UnifiedPush.unregister(this, instance).also {
            Log_OC.d(TAG, "onUnregistered($instance)")
        }

}
