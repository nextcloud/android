/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.account

import junit.framework.TestCase.assertEquals
import org.junit.Test

class UserTests {

    @Test
    fun testGetUserId() {
        val testCases = listOf(
            "admin@10.0.2.2:55001/" to "admin",
            "admin@@10.0.2.2:55001/" to "admin@",
            "admin___@10.0.2.2:55001/" to "admin___",
            "admin10101010@10.0.2.2:55001/" to "admin10101010",
            "admin10101010@@10.0.2.2:55001/" to "admin10101010@",
            "admin10101010@@@10.0.2.2:55001/" to "admin10101010@@",
            "user@nextcloud.com" to "user",
            "user_2001@next_cloud_next.com.de" to "user_2001",
            "@_@@user_2001_!%@@4_!next_cloud_next.com.de" to "@_@@user_2001_!%@",
            "@10.0.2.2:55001/" to "",
            "user@domain.com" to "user",
            "user123@another-domain.org" to "user123",
            "simpleuser@" to "simpleuser",
            "no-at-sign" to "no-at-sign"
        )

        for ((account, expectedUserId) in testCases) {
            val user = MockUser(account, "Nextcloud")
            assertEquals(expectedUserId, user.getUserId())
        }
    }
}
