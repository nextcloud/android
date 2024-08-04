/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
