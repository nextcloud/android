/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019-2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils;

class OwnCloudSession {
    private String sessionName;
    private String sessionUrl;
    private int entryId;

    public OwnCloudSession(String sessionName, String sessionUrl, int entryId) {
        this.sessionName = sessionName;
        this.sessionUrl = sessionUrl;
        this.entryId = entryId;
    }

    public String getSessionName() {
        return this.sessionName;
    }

    public String getSessionUrl() {
        return this.sessionUrl;
    }

    public int getEntryId() {
        return this.entryId;
    }

    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }

    public void setSessionUrl(String sessionUrl) {
        this.sessionUrl = sessionUrl;
    }
}
