/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2022 Tobias Kaminsky
 * Copyright (C) 2022 Nextcloud GmbH
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

package com.nextcloud.client.widget

import com.nextcloud.android.lib.resources.dashboard.DashboardButton
import com.nextcloud.client.account.User
import com.nextcloud.java.util.Optional

data class WidgetConfiguration(
    val widgetId: String,
    val title: String,
    val iconUrl: String,
    val roundIcon: Boolean,
    val user: Optional<User>,
    val addButton: DashboardButton?,
    val moreButton: DashboardButton?
)
