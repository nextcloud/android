/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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

    override fun getMaterialSchemesForCurrentUser(): MaterialSchemes = when (val user = userAccountManager.user) {
        is AnonymousUser -> {
            logger.d(TAG, "User is anonymous, using default schemes")
            getDefaultMaterialSchemes()
        }
        else -> getMaterialSchemesForUser(user)
    }

    override fun getDefaultMaterialSchemes(): MaterialSchemes {
        val primaryColor = ContextCompat.getColor(context, R.color.primary)
        return MaterialSchemes.fromColor(primaryColor)
    }

    override fun getMaterialSchemesForPrimaryBackground(): MaterialSchemes {
        val primaryColor = ContextCompat.getColor(context, R.color.primary)
        return MaterialSchemes.withPrimaryAsBackground(primaryColor)
    }

    companion object {
        private val TAG = MaterialSchemesProviderImpl::class.java.simpleName
    }
}
