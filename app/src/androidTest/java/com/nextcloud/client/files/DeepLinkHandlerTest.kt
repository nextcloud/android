/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.files

import android.net.Uri
import com.nextcloud.client.account.MockUser
import com.nextcloud.client.account.Server
import com.owncloud.android.lib.resources.status.OwnCloudVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import java.net.URI

class DeepLinkHandlerTest {

    private val serverUrl = "https://nextcloud.com/ltd/e2e2"
    private val invalidServerUrl = "https://someotherserver.net"
    private val shareLinkSuffix = "FRcNeiYDQLr8f54"

    @Test
    fun matches_deep_link_patterns() {
        val url = "$serverUrl/index.php/s/$shareLinkSuffix"
        val match =
            DeepLinkHandler.DEEP_LINK_PATTERN_F.matchEntire(url) ?: DeepLinkHandler.DEEP_LINK_PATTERN_S.matchEntire(url)
        assertNotNull("Url [$url] does not match pattern", match)
        assertEquals(serverUrl, match?.groupValues?.get(DeepLinkHandler.BASE_URL_GROUP_INDEX))
        assertEquals(shareLinkSuffix, match?.groupValues?.get(DeepLinkHandler.PATH_GROUP_INDEX))
    }

    @Test
    fun no_trailing_path_allowed_after_file_id() {
        val url = "$invalidServerUrl/index.php/$shareLinkSuffix"
        val match =
            DeepLinkHandler.DEEP_LINK_PATTERN_F.matchEntire(url) ?: DeepLinkHandler.DEEP_LINK_PATTERN_S.matchEntire(url)
        assertNull(match)
    }

    @Test
    fun no_user_can_open_file() {
        val user = MockUser("name", "ACCOUNT_TYPE").apply {
            server = Server(uri = URI.create(invalidServerUrl), version = OwnCloudVersion.nextcloud_19)
        }
        val sut = DeepLinkHandler(listOf(user))
        val deepLink = Uri.parse("$serverUrl/index.php/s/$shareLinkSuffix")
        val match = sut.parseDeepLink(deepLink)
        assertNotNull(match)
        assertEquals(0, match?.users?.size)
    }

    @Test
    fun single_user_can_open_file() {
        val user = MockUser("name", "ACCOUNT_TYPE").apply {
            server = Server(uri = URI.create(invalidServerUrl), version = OwnCloudVersion.nextcloud_19)
        }
        val validUser = MockUser("name2", "ACCOUNT_TYPE_2").apply {
            server = Server(uri = URI.create(serverUrl), version = OwnCloudVersion.nextcloud_19)
        }
        val sut = DeepLinkHandler(listOf(user, validUser))
        val deepLink = Uri.parse("${validUser.server}/index.php/f/$shareLinkSuffix")
        val match = sut.parseDeepLink(deepLink)
        assertNotNull(match)
        assertSame(1, match?.users?.size)
    }

    @Test
    fun multiple_user_can_open_file() {
        val user = MockUser("name", "ACCOUNT_TYPE").apply {
            server = Server(uri = URI.create(invalidServerUrl), version = OwnCloudVersion.nextcloud_19)
        }
        val validUser = MockUser("name2", "ACCOUNT_TYPE_2").apply {
            server = Server(uri = URI.create(serverUrl), version = OwnCloudVersion.nextcloud_19)
        }
        val validUser2 = MockUser("name3", "ACCOUNT_TYPE_3").apply {
            server = Server(uri = URI.create(serverUrl), version = OwnCloudVersion.nextcloud_19)
        }
        val sut = DeepLinkHandler(listOf(user, validUser, validUser2))
        val deepLink = Uri.parse("${validUser.server}/index.php/f/$shareLinkSuffix")
        val match = sut.parseDeepLink(deepLink)
        assertNotNull(match)
        assertSame(2, match?.users?.size)
    }

    @Test
    fun match_contains_extracted_file_id() {
        val validUser = MockUser("name2", "ACCOUNT_TYPE_2").apply {
            server = Server(uri = URI.create(serverUrl), version = OwnCloudVersion.nextcloud_19)
        }
        val sut = DeepLinkHandler(listOf(validUser))
        val deepLink = Uri.parse("${validUser.server}/index.php/f/$shareLinkSuffix")
        val match = sut.parseDeepLink(deepLink)
        assertNotNull(match)
        assertEquals(shareLinkSuffix, match?.path)
    }

    @Test
    fun no_match_for_invalid_link() {
        val validUser = MockUser("name2", "ACCOUNT_TYPE_2").apply {
            server = Server(uri = URI.create(serverUrl), version = OwnCloudVersion.nextcloud_19)
        }
        val sut = DeepLinkHandler(listOf(validUser))
        val deepLink = Uri.parse("http://www.dodgylink.com/index.php")
        val match = sut.parseDeepLink(deepLink)
        assertNull(match)
    }
}
