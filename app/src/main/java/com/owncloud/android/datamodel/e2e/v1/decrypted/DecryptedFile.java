/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.datamodel.e2e.v1.decrypted;

public class DecryptedFile {
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
