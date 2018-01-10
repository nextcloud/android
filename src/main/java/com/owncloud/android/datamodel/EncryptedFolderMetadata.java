/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2017 Tobias Kaminsky
 * Copyright (C) 2017 Nextcloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.datamodel;

import java.util.HashMap;

/**
 * Encrypted class representation of metadata json of folder metadata
 */

public class EncryptedFolderMetadata {
    private DecryptedFolderMetadata.Metadata metadata;
    private HashMap<String, EncryptedFile> files;
    
    public EncryptedFolderMetadata(DecryptedFolderMetadata.Metadata metadata, HashMap<String, EncryptedFile> files) {
        this.metadata = metadata;
        this.files = files;
    }

    public DecryptedFolderMetadata.Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(DecryptedFolderMetadata.Metadata metadata) {
        this.metadata = metadata;
    }

    public HashMap<String, EncryptedFile> getFiles() {
        return files;
    }

    public void setFiles(HashMap<String, EncryptedFile> files) {
        this.files = files;
    }

    public static class EncryptedFile {
        private String encrypted;
        private String initializationVector;
        private String authenticationTag;
        private int metadataKey;

        public String getEncrypted() {
            return encrypted;
        }

        public void setEncrypted(String encrypted) {
            this.encrypted = encrypted;
        }

        public String getInitializationVector() {
            return initializationVector;
        }

        public void setInitializationVector(String initializationVector) {
            this.initializationVector = initializationVector;
        }

        public String getAuthenticationTag() {
            return authenticationTag;
        }

        public void setAuthenticationTag(String authenticationTag) {
            this.authenticationTag = authenticationTag;
        }

        public int getMetadataKey() {
            return metadataKey;
        }

        public void setMetadataKey(int metadataKey) {
            this.metadataKey = metadataKey;
        }
    }
}

