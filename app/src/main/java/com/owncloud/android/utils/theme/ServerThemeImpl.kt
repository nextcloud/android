/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils.theme

import com.nextcloud.android.common.ui.color.ColorUtil
import com.nextcloud.android.common.ui.theme.ServerTheme
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
        //TODO: Right now primaryColor fallback resource is referenced from qrcodescanner dependency. However, I think it should be
        // referenced from within app (such as R.color.primary) or common-ui resource.
        primaryColor =
            colorUtil.getNullSafeColorWithFallbackRes(capability.serverColor, com.blikoon.qrcodescanner.R.color.colorPrimary)
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
