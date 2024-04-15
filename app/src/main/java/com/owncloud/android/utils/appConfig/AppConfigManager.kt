/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.utils.appConfig

import android.content.Context
import android.content.RestrictionsManager
import android.content.res.Resources
import com.owncloud.android.R
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.utils.enterpriseReporter.enterpriseFeedback

class AppConfigManager(private val context: Context) {

    private val restrictionsManager =
        context.getSystemService(Context.RESTRICTIONS_SERVICE) as RestrictionsManager

    private val tag = "AppConfigManager"

    fun readProxyConfig() {
        val appRestrictions = restrictionsManager.applicationRestrictions

        val host = if (appRestrictions.containsKey(AppConfigKeys.ProxyHost.key)) {
            appRestrictions.getString(AppConfigKeys.ProxyHost.key)
        } else {
            context.resources.getString(R.string.proxy_host)
        }

        val port = if (appRestrictions.containsKey(AppConfigKeys.ProxyPort.key)) {
            appRestrictions.getInt(AppConfigKeys.ProxyPort.key)
        } else {
            context.resources.getInteger(R.integer.proxy_port)
        }

        if (host == null) {
            context.enterpriseFeedback(
                AppConfigKeys.ProxyHost,
                R.string.app_config_proxy_config_cannot_be_found_message
            )
            return
        }

        try {
            OwnCloudClientManagerFactory.setProxyHost(host)
            OwnCloudClientManagerFactory.setProxyPort(port)
        } catch (e: Resources.NotFoundException) {
            context.enterpriseFeedback(AppConfigKeys.ProxyHost, R.string.app_config_proxy_config_cannot_be_set_message)
            context.enterpriseFeedback(AppConfigKeys.ProxyPort, R.string.app_config_proxy_config_cannot_be_set_message)
            Log_OC.d(tag, "Proxy config cannot able to set due to: $e")
        }
    }
}
