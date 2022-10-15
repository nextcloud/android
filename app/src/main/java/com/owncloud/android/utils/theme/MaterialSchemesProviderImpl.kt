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

import android.content.Context
import androidx.core.content.ContextCompat
import com.nextcloud.android.common.ui.theme.MaterialSchemes
import com.nextcloud.client.account.AnonymousUser
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.logger.Logger
import com.owncloud.android.R
import com.owncloud.android.lib.resources.status.OCCapability
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

internal class MaterialSchemesProviderImpl @Inject constructor(
    private val logger: Logger,
    private val context: Context,
    private val userAccountManager: UserAccountManager,
    private val themeFactory: ServerThemeImpl.Factory
) : MaterialSchemesProvider {

    private val themeCache: MutableMap<String, MaterialSchemes> = ConcurrentHashMap()

    override fun getMaterialSchemesForUser(user: User): MaterialSchemes {
        val url: String = user.server.uri.toString()

        if (!themeCache.containsKey(url)) {
            val capability = CapabilityUtils.getCapability(user, context)
            themeCache[url] = getMaterialSchemesForCapability(capability)
        }

        return themeCache[url]!!
    }

    override fun getMaterialSchemesForCapability(capability: OCCapability): MaterialSchemes {
        val serverTheme = themeFactory.create(capability)
        return MaterialSchemes.fromServerTheme(serverTheme)
    }

    override fun getMaterialSchemesForCurrentUser(): MaterialSchemes {
        return when (val user = userAccountManager.user) {
            is AnonymousUser -> {
                logger.d(TAG, "User is anonymous, using default schemes")
                getDefaultMaterialSchemes()
            }
            else -> getMaterialSchemesForUser(user)
        }
    }

    override fun getDefaultMaterialSchemes(): MaterialSchemes {
        val primaryColor = ContextCompat.getColor(context, R.color.primary)
        return MaterialSchemes.fromColor(primaryColor)
    }

    companion object {
        private val TAG = MaterialSchemesProviderImpl::class.java.simpleName
    }
}
