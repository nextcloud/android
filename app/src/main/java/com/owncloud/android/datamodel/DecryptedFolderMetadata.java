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
import java.util.Map;

/**
 * Decrypted class representation of metadata json of folder metadata.
 */
public class DecryptedFolderMetadata {
    private Metadata metadata;
    private Map<String, DecryptedFile> files;

    public DecryptedFolderMetadata() {
        this.metadata = new Metadata();
        this.files = new HashMap<>();
    }

    public DecryptedFolderMetadata(Metadata metadata, Map<String, DecryptedFile> files) {
        this.metadata = metadata;
        this.files = files;
    }

    public Metadata getMetadata() {
        return this.metadata;
    }

    public Map<String, DecryptedFile> getFiles() {
        return this.files;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public void setFiles(Map<String, DecryptedFile> files) {
        this.files = files;
    }

    public static class Metadata {
        private Map<Integer, String> metadataKeys; // each keys is encrypted on its own, decrypt on use
        private Sharing sharing;
        private int version;

        @Override
        public String toString() {
            return String.valueOf(version);
        }

        public Map<Integer, String> getMetadataKeys() {
            return this.metadataKeys;
        }

        public Sharing getSharing() {
            return this.sharing;
        }

        public int getVersion() {
            return this.version;
        }

        public void setMetadataKeys(Map<Integer, String> metadataKeys) {
            this.metadataKeys = metadataKeys;
        }

        public void setSharing(Sharing sharing) {
            this.sharing = sharing;
        }

        public void setVersion(int version) {
            this.version = version;
        }
    }

    public static class Encrypted {
        private Map<Integer, String> metadataKeys;

        public Map<Integer, String> getMetadataKeys() {
            return this.metadataKeys;
        }

        public void setMetadataKeys(Map<Integer, String> metadataKeys) {
            this.metadataKeys = metadataKeys;
        }
    }

    public static class Sharing {
        private Map<String, String> recipient;
        private String signature;

        public Map<String, String> getRecipient() {
            return this.recipient;
        }

        public String getSignature() {
            return this.signature;
        }

        public void setRecipient(Map<String, String> recipient) {
            this.recipient = recipient;
        }

        public void setSignature(String signature) {
            this.signature = signature;
        }
    }

    public static class DecryptedFile {
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

    public static class Data {
        private String key;
        private String filename;
        private String mimetype;
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
}
