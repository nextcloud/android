/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2021 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2021 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.jobs

import android.content.ContentResolver
import android.content.Context
import android.text.TextUtils
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.lib.common.utils.Log_OC
import third_parties.sufficientlysecure.AndroidCalendar
import third_parties.sufficientlysecure.SaveCalendar
import java.util.Calendar

class CalendarBackupWork(
    appContext: Context,
    params: WorkerParameters,
    private val contentResolver: ContentResolver,
    private val accountManager: UserAccountManager,
    private val preferences: AppPreferences
) : Worker(appContext, params) {

    companion object {
        val TAG = CalendarBackupWork::class.java.simpleName
        const val ACCOUNT = "account"
        const val FORCE = "force"
        const val JOB_INTERVAL_MS: Long = 24 * 60 * 60 * 1000
    }

    override fun doWork(): Result {
        val accountName = inputData.getString(ACCOUNT) ?: ""
        val optionalUser = accountManager.getUser(accountName)
        if (optionalUser == null || TextUtils.isEmpty(accountName)) {
            // no account provided
            Log_OC.d(TAG, "User not present")
            return Result.failure()
        }
        val lastExecution = preferences.calendarLastBackup

        val force = inputData.getBoolean(FORCE, false)
        if (force || lastExecution + JOB_INTERVAL_MS < Calendar.getInstance().timeInMillis) {
            val calendars = AndroidCalendar.loadAll(contentResolver)
            Log_OC.d(TAG, "Saving ${calendars.size} calendars")
            calendars.forEach { calendar ->
                SaveCalendar(
                    applicationContext,
                    calendar,
                    preferences,
                    optionalUser
                ).start()
            }

            // store execution date
            preferences.calendarLastBackup = Calendar.getInstance().timeInMillis
        } else {
            Log_OC.d(TAG, "last execution less than 24h ago")
        }

        return Result.success()
    }
}
