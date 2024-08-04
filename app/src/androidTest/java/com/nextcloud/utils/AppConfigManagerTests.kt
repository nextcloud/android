/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils

import android.os.Bundle
import com.owncloud.android.AbstractIT
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.utils.appConfig.AppConfigKeys
import com.owncloud.android.utils.appConfig.AppConfigManager
import org.junit.AfterClass
import org.junit.Test

class AppConfigManagerTests : AbstractIT() {

    private val testBaseUrl = "nextcloud.cloud.cloud"
    private val testProxyHost = "nextcloud.cloud.cloud.com"

    @Suppress("MagicNumber")
    private val testProxyPort = 8800

    @Test
    fun testSetProxyConfigWhenGivenClientBrandedPlusAndCorrectBundleDataProxyConfigurationShouldSet() {
        val proxySetting = Bundle().apply {
            putString(AppConfigKeys.ProxyHost.key, testProxyHost)
            putInt(AppConfigKeys.ProxyPort.key, testProxyPort)
        }

        AppConfigManager(targetContext, proxySetting).run {
            setProxyConfig(true)
        }

        val proxyHost = OwnCloudClientManagerFactory.getProxyHost()
        val proxyPort = OwnCloudClientManagerFactory.getProxyPort()

        assert(proxyHost.equals(testProxyHost))
        assert(proxyPort == testProxyPort)
    }

    @Test
    fun testSetProxyConfigWhenGivenClientNotBrandedPlusAndCorrectBundleDataProxyConfigurationShouldNotSet() {
        val proxySetting = Bundle().apply {
            putString(AppConfigKeys.ProxyHost.key, testProxyHost)
            putInt(AppConfigKeys.ProxyPort.key, testProxyPort)
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
    fun testGetBaseUrlConfigWhenGivenClientBrandedPlusAndCorrectBundleDataBaseUrlConfigurationShouldSet() {
        val baseUrlConfig = Bundle().apply {
            putString(AppConfigKeys.BaseUrl.key, testBaseUrl)
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

    companion object {
        @JvmStatic
        @AfterClass
        fun tearDown() {
            OwnCloudClientManagerFactory.setProxyHost("")
            OwnCloudClientManagerFactory.setProxyPort(-1)
        }
    }
}
