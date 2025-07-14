/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
class ViewThemeUtils @Inject constructor(schemes: MaterialSchemes, colorUtil: ColorUtil) : ViewThemeUtilsBase(schemes) {
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
        fun withSchemes(schemes: MaterialSchemes): ViewThemeUtils = ViewThemeUtils(schemes, colorUtil)

        fun withDefaultSchemes(): ViewThemeUtils = withSchemes(schemesProvider.getDefaultMaterialSchemes())

        fun withPrimaryAsBackground(): ViewThemeUtils =
            withSchemes(schemesProvider.getMaterialSchemesForPrimaryBackground())
    }
}
