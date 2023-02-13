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
