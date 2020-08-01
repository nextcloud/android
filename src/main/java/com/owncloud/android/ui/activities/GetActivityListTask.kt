/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.activities

import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.resources.activities.GetActivitiesRemoteOperation
import com.owncloud.android.lib.resources.activities.model.Activity

class GetActivityListTask(
    private val last: Int,
    private val client: OwnCloudClient
) : () -> GetActivityListTask.Result {

    data class Result(val success: Boolean = false, val activities: List<Activity> = emptyList(), val last: Int = -1)

    override fun invoke(): Result {
        val op = when {
            last <= 0 -> GetActivitiesRemoteOperation()
            else -> GetActivitiesRemoteOperation(last)
        }
        val result = op.execute(client)
        return if (result.isSuccess && result.data != null) {
            Result(
                success = true,
                activities = result.data[0] as List<Activity>,
                last = result.data[1] as Int
            )
        } else {
            Result()
        }
    }
}
