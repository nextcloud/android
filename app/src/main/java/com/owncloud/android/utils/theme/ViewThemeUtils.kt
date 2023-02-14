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
import com.nextcloud.android.common.ui.theme.MaterialSchemes
import com.nextcloud.android.common.ui.theme.ViewThemeUtilsBase
import com.nextcloud.android.common.ui.theme.utils.AndroidViewThemeUtils
import com.nextcloud.android.common.ui.theme.utils.AndroidXViewThemeUtils
import com.nextcloud.android.common.ui.theme.utils.DialogViewThemeUtils
import com.nextcloud.android.common.ui.theme.utils.MaterialViewThemeUtils
import javax.inject.Inject

/**
 * Child fields intentionally constructed instead of injected in order to reuse schemes for performance
 */
class ViewThemeUtils @Inject constructor(
    schemes: MaterialSchemes,
    colorUtil: ColorUtil
) : ViewThemeUtilsBase(schemes) {
    @JvmField
    val platform = AndroidViewThemeUtils(schemes, colorUtil)

    @JvmField
    val material = MaterialViewThemeUtils(schemes, colorUtil)

    @JvmField
    val androidx = AndroidXViewThemeUtils(schemes, platform)

    @JvmField
    val dialog = DialogViewThemeUtils(schemes)

    @JvmField
    val files = FilesSpecificViewThemeUtils(schemes, colorUtil, platform, androidx)

    class Factory @Inject constructor(
        private val schemesProvider: MaterialSchemesProvider,
        private val colorUtil: ColorUtil
    ) {
        fun withSchemes(schemes: MaterialSchemes): ViewThemeUtils {
            return ViewThemeUtils(schemes, colorUtil)
        }

        fun withDefaultSchemes(): ViewThemeUtils {
            return withSchemes(schemesProvider.getDefaultMaterialSchemes())
        }

        fun withPrimaryAsBackground(): ViewThemeUtils {
            return withSchemes(schemesProvider.getMaterialSchemesForPrimaryBackground())
        }
    }
}
