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

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Encrypted class representation of metadata json of folder metadata
 */
@Getter
@Setter
@AllArgsConstructor
public class EncryptedFolderMetadata {
    private DecryptedFolderMetadata.Metadata metadata;
    private Map<String, EncryptedFile> files;

    @Getter
    @Setter
    public static class EncryptedFile {
        private String encrypted;
        private String initializationVector;
        private String authenticationTag;
        private int metadataKey;
    }
}
