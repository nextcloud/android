/**
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
public class OwnCloudSession {
    private String mSessionName;
    private String mSessionUrl;
    private int mEntryId;

    public OwnCloudSession(String name, String url, int entryId) {
        mSessionName = name;
        mSessionUrl = url;
        mEntryId = entryId;
    }

    public void setName(String name) {
        mSessionName = name;
    }

    public String getName() {
        return mSessionName;
    }

    public void setUrl(String url) {
        mSessionUrl = url;
    }

    public String getUrl() {
        return mSessionUrl;
    }

    public int getEntryId() {
        return mEntryId;
    }
}
