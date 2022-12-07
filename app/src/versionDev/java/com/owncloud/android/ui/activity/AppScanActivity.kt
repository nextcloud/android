/*
 * Nextcloud Android client application
 *
 * @author Álvaro Brey Vilas
 * Copyright (C) 2022 Álvaro Brey Vilas
 * Copyright (C) 2022 Nextcloud GmbH
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

package com.owncloud.android.ui.activity

import android.app.Activity
import com.owncloud.android.lib.common.utils.Log_OC

class AppScanActivity {
    // stub
    companion object {
        private val TAG = AppScanActivity::class.simpleName

        @JvmStatic
        val enabled: Boolean = false

        @JvmStatic
        fun scanFromCamera(activity: Activity, requestcode: Int) {
            // stub
            Log_OC.w(TAG, "scanFromCamera called in stub implementation")
        }
    }
}
