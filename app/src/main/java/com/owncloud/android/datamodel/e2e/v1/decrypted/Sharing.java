/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.datamodel.e2e.v1.decrypted;

import java.util.Map;

public class Sharing {
    private Map<String, String> recipient;
    private String signature;

    public Map<String, String> getRecipient() {
        return this.recipient;
    }

    public String getSignature() {
        return this.signature;
    }

    public void setRecipient(Map<String, String> recipient) {
        this.recipient = recipient;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}
