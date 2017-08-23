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
    public Metadata metadata;
    public HashMap<String, DecryptedFile> files;

    public DecryptedFolderMetadata() {
        this.metadata = new Metadata();
        this.files = new HashMap<>();
    }

    public DecryptedFolderMetadata(Metadata metadata, HashMap<String, DecryptedFile> files) {
        this.metadata = metadata;
        this.files = files;
    }

    public static class Metadata {
        public HashMap<Integer, String> metadataKeys; // each keys is encrypted on its own, decrypt on use
        public Sharing sharing;
        public int version;

        @Override
        public String toString() {
            return String.valueOf(version);
        }
    }

    public static class Encrypted {
        public HashMap<Integer, String> metadataKeys;
    }

    public static class Sharing {
        public HashMap<String, String> recipient;
        public String signature;
    }

    public static class DecryptedFile {
        public Data encrypted;
        public String initializationVector;
        public String authenticationTag;
        public int metadataKey;
    }

    public static class Data {
        public String key;
        public String filename;
        public String mimetype;
        public int version;
    }
}
