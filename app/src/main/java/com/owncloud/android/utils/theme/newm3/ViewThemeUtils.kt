/*
 * Nextcloud Talk application
 *
 * @author Álvaro Brey
 * Copyright (C) 2022 Álvaro Brey
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

package com.owncloud.android.utils.theme.newm3

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
    val files = FilesSpecificViewThemeUtils(schemes, platform, androidx)
}
