/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.datamodel.e2e.v1.decrypted;

import java.util.Map;

public class Encrypted {
    private Map<Integer, String> metadataKeys;

    public Map<Integer, String> getMetadataKeys() {
        return this.metadataKeys;
    }

    public void setMetadataKeys(Map<Integer, String> metadataKeys) {
        this.metadataKeys = metadataKeys;
    }
}
