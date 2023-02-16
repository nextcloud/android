/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2023 Tobias Kaminsky
 * Copyright (C) 2023 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.datamodel.e2e.v1.decrypted;

public class Data {
    private String filename;
    private String mimetype;
    private String key;
    private int version;

    public String getKey() {
        return this.key;
    }

    public String getFilename() {
        return this.filename;
    }

    public String getMimetype() {
        return this.mimetype;
    }

    public int getVersion() {
        return this.version;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
