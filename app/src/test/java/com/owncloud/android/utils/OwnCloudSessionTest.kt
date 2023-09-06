/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2019 Tobias Kaminsky
 * Copyright (C) 2019 Edvard Holst
 * Copyright (C) 2019 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.owncloud.android.utils

import org.junit.Assert
import org.junit.Before
import org.junit.Test

class OwnCloudSessionTest {
    private val SESSION_NAME = "testSession"
    private val SESSION_URL = "https://test.cloud.local"
    private val SESSION_ID = 1
    private val ownCloudSession = OwnCloudSession(SESSION_NAME, SESSION_URL, SESSION_ID)
    @Before
    fun setUp() {
        ownCloudSession.sessionName = SESSION_NAME
        ownCloudSession.sessionUrl = SESSION_URL
    }

    @Test
    fun setName_assertCorrectNameUpdated() {
        val newName = "newTestSession"
        ownCloudSession.sessionName = newName
        Assert.assertEquals("OwnCloud session name not updated", newName, ownCloudSession.sessionName)
    }

    @get:Test
    val name_assertCorrectNamedRetrieved: Unit
        get() {
            Assert.assertEquals("OwnCloudSession name not correct", SESSION_NAME, ownCloudSession.sessionName)
        }

    @Test
    fun setUrl_assertCorrectUrlUpdated() {
        val newUrl = "https://new.cloud.local"
        ownCloudSession.sessionUrl = newUrl
        Assert.assertEquals("OwnCloud session URL not updated", newUrl, ownCloudSession.sessionUrl)
    }

    @get:Test
    val url_assertCorrectUrlRetrieved: Unit
        get() {
            Assert.assertEquals("OwnCloudSession URL not correct", SESSION_URL, ownCloudSession.sessionUrl)
        }

    @get:Test
    val entryId_assertCorrectIdRetrieved: Unit
        get() {
            Assert.assertEquals(
                "OwnCloudSession ID is incorrect",
                SESSION_ID.toLong(),
                ownCloudSession.entryId.toLong()
            )
        }
}