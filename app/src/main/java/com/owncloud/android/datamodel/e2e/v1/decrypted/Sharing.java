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
