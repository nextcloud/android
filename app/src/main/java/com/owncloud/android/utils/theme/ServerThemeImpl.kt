/*
 * Nextcloud Android client application
 *
 *  @author Álvaro Brey
 *  Copyright (C) 2022 Álvaro Brey
 *  Copyright (C) 2022 Nextcloud GmbH
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.utils.theme

import com.nextcloud.android.common.ui.color.ColorUtil
import com.nextcloud.android.common.ui.theme.ServerTheme
import com.owncloud.android.R
import com.owncloud.android.lib.resources.status.OCCapability
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class ServerThemeImpl @AssistedInject constructor(colorUtil: ColorUtil, @Assisted capability: OCCapability) :
    ServerTheme {
    override val colorElement: Int
    override val colorElementBright: Int
    override val colorElementDark: Int
    override val colorText: Int
    override val primaryColor: Int

    init {
        primaryColor =
            colorUtil.getNullSafeColorWithFallbackRes(capability.serverColor, R.color.colorPrimary)
        colorElement = colorUtil.getNullSafeColor(capability.serverElementColor, primaryColor)
        colorElementBright =
            colorUtil.getNullSafeColor(capability.serverElementColorBright, primaryColor)
        colorElementDark = colorUtil.getNullSafeColor(capability.serverElementColorDark, primaryColor)
        colorText = colorUtil.getTextColor(capability.serverTextColor, primaryColor)
    }

    @AssistedFactory
    interface Factory {
        fun create(capability: OCCapability): ServerThemeImpl
    }
}
