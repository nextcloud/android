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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Decrypted class representation of metadata json of folder metadata.
 */
@Getter
@Setter
@AllArgsConstructor
public class DecryptedFolderMetadata {
    private Metadata metadata;
    private Map<String, DecryptedFile> files;

    public DecryptedFolderMetadata() {
        this.metadata = new Metadata();
        this.files = new HashMap<>();
    }

    @Getter
    @Setter
    public static class Metadata {
        private Map<Integer, String> metadataKeys; // each keys is encrypted on its own, decrypt on use
        private Sharing sharing;
        private int version;

        @Override
        public String toString() {
            return String.valueOf(version);
        }
    }

    @Getter
    @Setter
    public static class Encrypted {
        private Map<Integer, String> metadataKeys;
    }

    @Getter
    @Setter
    public static class Sharing {
        private Map<String, String> recipient;
        private String signature;
    }

    @Getter
    @Setter
    public static class DecryptedFile {
        private Data encrypted;
        private String initializationVector;
        private String authenticationTag;
        private int metadataKey;
    }

    @Getter
    @Setter
    public static class Data {
        private String key;
        private String filename;
        private String mimetype;
        private int version;
    }
}
