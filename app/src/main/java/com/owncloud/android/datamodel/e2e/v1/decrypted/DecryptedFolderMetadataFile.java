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

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.VisibleForTesting;

/**
 * Decrypted class representation of metadata json of folder metadata.
 */
public class DecryptedFolderMetadataFile {
    private DecryptedMetadata metadata;
    private Map<String, DecryptedFile> files;
    private Map<String, DecryptedFile> filedrop;

    public DecryptedFolderMetadataFile() {
        this.metadata = new DecryptedMetadata();
        this.files = new HashMap<>();
    }

    public DecryptedFolderMetadataFile(DecryptedMetadata metadata, Map<String, DecryptedFile> files) {
        this.metadata = metadata;
        this.files = files;
    }

    public DecryptedMetadata getMetadata() {
        return this.metadata;
    }

    public Map<String, DecryptedFile> getFiles() {
        return this.files;
    }

    public void setMetadata(DecryptedMetadata metadata) {
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

}
