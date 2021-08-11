/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * @author Tobias Kaminsky
 *
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * Copyright (C) 2019 Tobias Kaminsky
 * Copyright (C) 2019 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.nextcloud.client.device

import android.content.Context
import android.os.PowerManager
import com.nextcloud.client.preferences.AppPreferences
import dagger.Module
import dagger.Provides

@Module
class DeviceModule {

    @Provides
    fun powerManagementService(context: Context, preferences: AppPreferences): PowerManagementService {
        val platformPowerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return PowerManagementServiceImpl(
            context = context,
            platformPowerManager = platformPowerManager,
            deviceInfo = DeviceInfo(),
            preferences = preferences
        )
    }
}
