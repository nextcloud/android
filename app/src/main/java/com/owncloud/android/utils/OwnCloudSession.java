/*
 * ownCloud Android client application
 *
 * @author Bartek Przybylski
 * Copyright (C) 2011  Bartek Przybylski
 * Copyright (C) 2015 ownCloud Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.utils;

/**
 * Represents a session to an ownCloud instance
 */
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
