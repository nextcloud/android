/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.datamodel.e2e.v1.decrypted;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.VisibleForTesting;

/**
 * Decrypted class representation of metadata json of folder metadata.
 */
public class DecryptedFolderMetadataFileV1 {
    private DecryptedMetadata metadata;
    private Map<String, DecryptedFile> files;
    private Map<String, DecryptedFile> filedrop;

    public DecryptedFolderMetadataFileV1() {
        this.metadata = new DecryptedMetadata();
        this.files = new HashMap<>();
    }

    public DecryptedFolderMetadataFileV1(DecryptedMetadata metadata, Map<String, DecryptedFile> files) {
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
