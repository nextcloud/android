/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.utils.appConfig

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.text.TextUtils
import com.owncloud.android.R
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.utils.Log_OC

class AppConfigManager(private val context: Context, private val appRestrictions: Bundle) {

    private val tag = "AppConfigManager"

    fun setProxyConfig(isBrandedPlus: Boolean) {
        if (!isBrandedPlus) {
            Log_OC.d(tag, "Proxy configuration cannot be set. Client is not branded plus.")
            return
        }

        val host = if (appRestrictions.containsKey(AppConfigKeys.ProxyHost.key)) {
            appRestrictions.getString(AppConfigKeys.ProxyHost.key)
        } else {
            context.getString(R.string.proxy_host)
        }

        val port = if (appRestrictions.containsKey(AppConfigKeys.ProxyPort.key)) {
            appRestrictions.getInt(AppConfigKeys.ProxyPort.key)
        } else {
            context.resources.getInteger(R.integer.proxy_port)
        }

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

        return if (appRestrictions.containsKey(AppConfigKeys.BaseUrl.key)) {
            appRestrictions.getString(AppConfigKeys.BaseUrl.key)
        } else {
            Log_OC.d(tag, "BaseUrl configuration cannot be found, default url applied")
            null
        }
    }
}
