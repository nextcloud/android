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

package com.owncloud.android.datamodel.e2e.v1.encrypted;

import com.owncloud.android.datamodel.EncryptedFiledrop;
import com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedMetadata;

import java.util.Map;

/**
 * Encrypted class representation of metadata json of folder metadata
 */
public class EncryptedFolderMetadataFileV1 {
    private DecryptedMetadata metadata;
    private Map<String, EncryptedFile> files;

    private Map<String, EncryptedFiledrop> filedrop;

    public EncryptedFolderMetadataFileV1(DecryptedMetadata metadata,
                                         Map<String, EncryptedFile> files,
                                         Map<String, EncryptedFiledrop> filesdrop) {
        this.metadata = metadata;
        this.files = files;
        this.filedrop = filesdrop;
    }

    public DecryptedMetadata getMetadata() {
        return this.metadata;
    }

    public Map<String, EncryptedFile> getFiles() {
        return files;
    }

    public Map<String, EncryptedFiledrop> getFiledrop() {
        return filedrop;
    }

    public void setMetadata(DecryptedMetadata metadata) {
        this.metadata = metadata;
    }

    public void setFiles(Map<String, EncryptedFile> files) {
        this.files = files;
    }

    public static class EncryptedFile {
        private String encrypted;
        private String initializationVector;
        private String authenticationTag;
        transient private int metadataKey;

        public String getEncrypted() {
            return encrypted;
        }

        public String getInitializationVector() {
            return initializationVector;
        }

        public String getAuthenticationTag() {
            return authenticationTag;
        }

        public int getMetadataKey() {
            return metadataKey;
        }

        public void setEncrypted(String encrypted) {
            this.encrypted = encrypted;
        }

        public void setInitializationVector(String initializationVector) {
            this.initializationVector = initializationVector;
        }

        public void setAuthenticationTag(String authenticationTag) {
            this.authenticationTag = authenticationTag;
        }
    }
}
