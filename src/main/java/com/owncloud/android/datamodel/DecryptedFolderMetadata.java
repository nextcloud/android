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
 * Decrypted class representation of metadata json of folder metadata
 */

public class DecryptedFolderMetadata {
    private Metadata metadata;
    private HashMap<String, DecryptedFile> files;

    public DecryptedFolderMetadata() {
        this.metadata = new Metadata();
        this.files = new HashMap<>();
    }

    public DecryptedFolderMetadata(Metadata metadata, HashMap<String, DecryptedFile> files) {
        this.metadata = metadata;
        this.files = files;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public HashMap<String, DecryptedFile> getFiles() {
        return files;
    }

    public void setFiles(HashMap<String, DecryptedFile> files) {
        this.files = files;
    }

    public static class Metadata {
        private HashMap<Integer, String> metadataKeys; // each keys is encrypted on its own, decrypt on use
        private Sharing sharing;
        private int version;

        public HashMap<Integer, String> getMetadataKeys() {
            return metadataKeys;
        }

        public void setMetadataKeys(HashMap<Integer, String> metadataKeys) {
            this.metadataKeys = metadataKeys;
        }

        public Sharing getSharing() {
            return sharing;
        }

        public void setSharing(Sharing sharing) {
            this.sharing = sharing;
        }

        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }

        @Override
        public String toString() {
            return String.valueOf(version);
        }
    }

    public static class Encrypted {
        private HashMap<Integer, String> metadataKeys;

        public HashMap<Integer, String> getMetadataKeys() {
            return metadataKeys;
        }

        public void setMetadataKeys(HashMap<Integer, String> metadataKeys) {
            this.metadataKeys = metadataKeys;
        }
    }

    public static class Sharing {
        private HashMap<String, String> recipient;
        private String signature;

        public HashMap<String, String> getRecipient() {
            return recipient;
        }

        public void setRecipient(HashMap<String, String> recipient) {
            this.recipient = recipient;
        }

        public String getSignature() {
            return signature;
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
            return encrypted;
        }

        public void setEncrypted(Data encrypted) {
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

    public static class Data {
        private String key;
        private String filename;
        private String mimetype;
        private int version;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public String getMimetype() {
            return mimetype;
        }

        public void setMimetype(String mimetype) {
            this.mimetype = mimetype;
        }

        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }
    }
}
