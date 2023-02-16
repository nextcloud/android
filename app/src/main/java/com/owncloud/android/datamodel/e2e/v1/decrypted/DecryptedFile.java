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

public class DecryptedFile {
    private Data encrypted;
    private String initializationVector;
    private String authenticationTag;
    private int metadataKey;

    public Data getEncrypted() {
        return this.encrypted;
    }

    public String getInitializationVector() {
        return this.initializationVector;
    }

    public String getAuthenticationTag() {
        return this.authenticationTag;
    }

    public int getMetadataKey() {
        return this.metadataKey;
    }

    public void setEncrypted(Data encrypted) {
        this.encrypted = encrypted;
    }

    public void setInitializationVector(String initializationVector) {
        this.initializationVector = initializationVector;
    }

    public void setAuthenticationTag(String authenticationTag) {
        this.authenticationTag = authenticationTag;
    }

    public void setMetadataKey(int metadataKey) {
        this.metadataKey = metadataKey;
    }
}
