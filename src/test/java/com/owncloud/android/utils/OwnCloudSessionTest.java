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
package com.owncloud.android.utils;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class OwnCloudSessionTest {

    private final String SESSION_NAME = "testSession";
    private final String SESSION_URL = "https://test.cloud.local";
    private final int SESSION_ID = 1;

    private OwnCloudSession ownCloudSession = new OwnCloudSession(SESSION_NAME, SESSION_URL, SESSION_ID);

    @Before
    public void setUp() {
        ownCloudSession.setSessionName(SESSION_NAME);
        ownCloudSession.setSessionUrl(SESSION_URL);
    }

    @Test
    public void setName_assertCorrectNameUpdated() {
        String newName = "newTestSession";
        ownCloudSession.setSessionName(newName);
        assertEquals("OwnCloud session name not updated", newName, ownCloudSession.getSessionName());
    }

    @Test
    public void getName_assertCorrectNamedRetrieved() {
        assertEquals("OwnCloudSession name not correct", SESSION_NAME, ownCloudSession.getSessionName());
    }

    @Test
    public void setUrl_assertCorrectUrlUpdated() {
        String newUrl = "https://new.cloud.local";
        ownCloudSession.setSessionUrl(newUrl);
        assertEquals("OwnCloud session URL not updated", newUrl, ownCloudSession.getSessionUrl());
    }

    @Test
    public void getUrl_assertCorrectUrlRetrieved() {
        assertEquals("OwnCloudSession URL not correct", SESSION_URL, ownCloudSession.getSessionUrl());
    }

    @Test
    public void getEntryId_assertCorrectIdRetrieved() {
        assertEquals("OwnCloudSession ID is incorrect", SESSION_ID, ownCloudSession.getEntryId());
    }
}
