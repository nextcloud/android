/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.datamodel.e2e.v1.decrypted;

public class Data {
    private String filename;
    private String mimetype;
    private String key;
    private double version;

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

    public void setVersion(double version) {
        this.version = version;
    }
}
