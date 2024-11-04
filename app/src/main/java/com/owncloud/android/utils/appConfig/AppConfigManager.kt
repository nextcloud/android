/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.utils.appConfig

import android.content.Context
import android.content.RestrictionsManager
import android.content.res.Resources
import android.text.TextUtils
import com.nextcloud.utils.extensions.getRestriction
import com.owncloud.android.R
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.utils.Log_OC

class AppConfigManager(private val context: Context, private val restrictionsManager: RestrictionsManager) {

    private val tag = "AppConfigManager"

    fun setProxyConfig(isBrandedPlus: Boolean) {
        if (!isBrandedPlus) {
            Log_OC.d(tag, "Proxy configuration cannot be set. Client is not branded plus.")
            return
        }

        val host = restrictionsManager.getRestriction(AppConfigKeys.ProxyHost.key, context.getString(R.string.proxy_host))
        val port = restrictionsManager.getRestriction(AppConfigKeys.ProxyPort.key, context.resources.getInteger(R.integer.proxy_port))

        if (TextUtils.isEmpty(host) || port == -1) {
            Log_OC.d(tag, "Proxy configuration cannot be found")
            return
        }

        try {
            OwnCloudClientManagerFactory.setProxyHost(host)
            OwnCloudClientManagerFactory.setProxyPort(port)

            Log_OC.d(tag, "Proxy configuration successfully set")
        } catch (e: Resources.NotFoundException) {
            Log_OC.e(tag, "Proxy config cannot able to set due to: $e")
        }
    }

    fun getBaseUrl(isBrandedPlus: Boolean): String? {
        if (!isBrandedPlus) {
            Log_OC.d(tag, "Proxy configuration cannot be set. Client is not branded plus. Default url applied")
            return null
        }

        return restrictionsManager.getRestriction(AppConfigKeys.BaseUrl.key, null)
    }
}
