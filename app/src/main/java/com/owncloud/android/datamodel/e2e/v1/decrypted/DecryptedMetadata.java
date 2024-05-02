/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.datamodel.e2e.v1.decrypted;

import java.util.Map;

public class DecryptedMetadata {
    transient
    private Map<Integer, String> metadataKeys; // outdated with v1.1
    private String metadataKey;
    private String checksum;
    private double version;

    @Override
    public String toString() {
        return String.valueOf(version);
    }

    public Map<Integer, String> getMetadataKeys() {
        return this.metadataKeys;
    }

    public String getMetadataKey() {
        if (metadataKey == null) {
            // fallback to old keys array
            return metadataKeys.get(0);
        }
        return metadataKey;
    }

    public double getVersion() {
        return this.version;
    }

    public void setMetadataKeys(Map<Integer, String> metadataKeys) {
        this.metadataKeys = metadataKeys;
    }

    public void setMetadataKey(String metadataKey) {
        this.metadataKey = metadataKey;
    }

    public void setVersion(double version) {
        this.version = version;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }
}
