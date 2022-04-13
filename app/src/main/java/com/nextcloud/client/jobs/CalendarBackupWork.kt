/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2021 Tobias Kaminsky
 * Copyright (C) 2021 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
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
        if (!optionalUser.isPresent || TextUtils.isEmpty(accountName)) { // no account provided
            return Result.failure()
        }
        val lastExecution = preferences.calendarLastBackup

        val force = inputData.getBoolean(FORCE, false)
        if (force || lastExecution + JOB_INTERVAL_MS < Calendar.getInstance().timeInMillis) {

            AndroidCalendar.loadAll(contentResolver).forEach { calendar ->
                SaveCalendar(
                    applicationContext,
                    calendar,
                    preferences,
                    accountManager.user
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
