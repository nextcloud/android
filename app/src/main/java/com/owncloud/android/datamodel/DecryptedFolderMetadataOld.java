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

import androidx.annotation.VisibleForTesting;

/**
 * Decrypted class representation of metadata json of folder metadata.
 */
public class DecryptedFolderMetadataOld {
    private Metadata metadata;
    private Map<String, DecryptedFile> files;

    private Map<String, DecryptedFile> filedrop;

    public DecryptedFolderMetadataOld() {
        this.metadata = new Metadata();
        this.files = new HashMap<>();
    }

    public DecryptedFolderMetadataOld(Metadata metadata, Map<String, DecryptedFile> files) {
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

    @VisibleForTesting
    public void setFiledrop(Map<String, DecryptedFile> filedrop) {
        this.filedrop = filedrop;
    }

    public Map<String, DecryptedFile> getFiledrop() {
        return filedrop;
    }

    public static class Metadata {
        transient
        private Map<Integer, String> metadataKeys; // outdated with v1.1
        private String metadataKey;
        private String checksum;
        private double version = 1.2;

        @Override
        public String toString() {
            return String.valueOf(version);
        }

        public Map<Integer, String> getMetadataKeys() {
            return this.metadataKeys;
        }

        public double getVersion() {
            return this.version;
        }

        public void setMetadataKeys(Map<Integer, String> metadataKeys) {
            this.metadataKeys = metadataKeys;
        }

        public void setVersion(double version) {
            this.version = version;
        }

        public String getMetadataKey() {
            if (metadataKey == null) {
                // fallback to old keys array
                return metadataKeys.get(0);
            }
            return metadataKey;
        }

        public void setMetadataKey(String metadataKey) {
            this.metadataKey = metadataKey;
        }

        public String getChecksum() {
            return checksum;
        }

        public void setChecksum(String checksum) {
            this.checksum = checksum;
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

    public static class DecryptedFile {
        private Data encrypted;
        private String initializationVector;
        private String authenticationTag;
        transient private int metadataKey;

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
        transient private double version;

        public String getKey() {
            return this.key;
        }

        public String getFilename() {
            return this.filename;
        }

        public String getMimetype() {
            return this.mimetype;
        }

        public double getVersion() {
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
