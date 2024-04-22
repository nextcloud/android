/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils

import android.os.Bundle
import com.owncloud.android.AbstractIT
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.utils.appConfig.AppConfigKeys
import com.owncloud.android.utils.appConfig.AppConfigManager
import org.junit.Test

class AppConfigManagerTests : AbstractIT() {

    @Suppress("MagicNumber")
    @Test
    fun testSetProxyConfigWhenGivenClientBrandedPlusAndCorrectBundleDataProxyConfigurationShouldSet() {
        val proxySetting = Bundle().apply {
            putString(AppConfigKeys.ProxyHost.key, "nextcloud.cloud.cloud.com")
            putInt(AppConfigKeys.ProxyPort.key, 441212)
        }

        AppConfigManager(targetContext, proxySetting).run {
            setProxyConfig(true)
        }

        val proxyHost = OwnCloudClientManagerFactory.getProxyHost()
        val proxyPort = OwnCloudClientManagerFactory.getProxyPort()

        assert(proxyHost.equals("nextcloud.cloud.cloud.com"))
        assert(proxyPort == 441212)
    }

    @Suppress("MagicNumber")
    @Test
    fun testSetProxyConfigWhenGivenClientNotBrandedPlusAndCorrectBundleDataProxyConfigurationShouldNotSet() {
        val proxySetting = Bundle().apply {
            putString(AppConfigKeys.ProxyHost.key, "nextcloud.cloud.cloud.com")
            putInt(AppConfigKeys.ProxyPort.key, 441212)
        }

        AppConfigManager(targetContext, proxySetting).run {
            setProxyConfig(false)
        }

        val proxyHost = OwnCloudClientManagerFactory.getProxyHost()
        val proxyPort = OwnCloudClientManagerFactory.getProxyPort()

        assert(proxyHost.equals(""))
        assert(proxyPort == -1)
    }

    @Test
    fun testSetProxyConfigWhenGivenClientBrandedPlusAndBrokenBundleDataProxyConfigurationShouldSetDefaultValues() {
        val proxySetting = Bundle()

        AppConfigManager(targetContext, proxySetting).run {
            setProxyConfig(true)
        }

        val proxyHost = OwnCloudClientManagerFactory.getProxyHost()
        val proxyPort = OwnCloudClientManagerFactory.getProxyPort()

        assert(proxyHost.equals(""))
        assert(proxyPort == -1)
    }

    @Test
    fun testGetBaseUrlConfigWhenGivenClientBrandedPlusAndCorrectBundleDataBaseUrlConfigurationShouldSet() {
        val baseUrlConfig = Bundle().apply {
            putString(AppConfigKeys.BaseUrl.key, "nextcloud.cloud.cloud")
        }
        val sut = AppConfigManager(targetContext, baseUrlConfig)
        assert(!sut.getBaseUrl(true).isNullOrEmpty())
    }

    @Test
    fun testGetBaseUrlConfigWhenGivenClientBrandedPlusAndBrokenBundleDataBaseUrlConfigurationShouldNotSet() {
        val baseUrlConfig = Bundle()
        val sut = AppConfigManager(targetContext, baseUrlConfig)
        assert(sut.getBaseUrl(true).isNullOrEmpty())
    }
}
