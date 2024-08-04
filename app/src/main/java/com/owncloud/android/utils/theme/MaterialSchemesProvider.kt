/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils.theme

import com.nextcloud.android.common.ui.theme.MaterialSchemes
import com.nextcloud.client.account.User
import com.owncloud.android.lib.resources.status.OCCapability

interface MaterialSchemesProvider {
    fun getMaterialSchemesForUser(user: User): MaterialSchemes
    fun getMaterialSchemesForCapability(capability: OCCapability): MaterialSchemes
    fun getMaterialSchemesForCurrentUser(): MaterialSchemes

    /**
     * Color schemes for when there's no logged in user
     */
    fun getDefaultMaterialSchemes(): MaterialSchemes

    /**
     * Color schemes for views where we want the primary color to be the background, and to be unchanged
     * (for brand purposes). For example, this includes the login screen and the first run activity
     */
    fun getMaterialSchemesForPrimaryBackground(): MaterialSchemes
}
