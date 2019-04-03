package com.owncloud.android.utils;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class OwnCloudSessionTest {

    private final String SESSION_NAME = "testSession";
    private final String SESSION_URL = "https://test.cloud.local";
    private final int SESSION_ID = 1;

    private OwnCloudSession ownCloudSession = new OwnCloudSession(SESSION_NAME, SESSION_URL,SESSION_ID);

    @Before
    public void setUp() {
        ownCloudSession.setName(SESSION_NAME);
        ownCloudSession.setUrl(SESSION_URL);
    }

    @Test
    public void setName_assertCorrectNameUpdated() {
        String newName = "newTestSession";
        ownCloudSession.setName(newName);
        assertEquals("OwnCloud session name updated",
                     newName, ownCloudSession.getName());
    }

    @Test
    public void getName_assertCorrectNamedRetrieved() {
        assertEquals("OwnCloudSession name correct",
                     SESSION_NAME, ownCloudSession.getName());
    }

    @Test
    public void setUrl_assertCorrectUrlUpdated() {
        String newUrl = "https://new.cloud.local";
        ownCloudSession.setUrl(newUrl);
        assertEquals("OwnCloud session URL updated",
                     newUrl, ownCloudSession.getUrl());
    }

    @Test
    public void getUrl_assertCorrectUrlRetrieved() {
        assertEquals("OwnCloudSession URL correct.",
                     SESSION_URL, ownCloudSession.getUrl());
    }

    @Test
    public void getEntryId_assertCorrectIdRetrieved() {
        assertEquals("OwnCloudSession ID is correct",
                     SESSION_ID, ownCloudSession.getEntryId());
    }
}
