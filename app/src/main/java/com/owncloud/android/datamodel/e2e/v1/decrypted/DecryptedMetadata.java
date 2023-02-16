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

public class DecryptedMetadata {
    private Map<Integer, String> metadataKeys; // each keys is encrypted on its own, decrypt on use
    private Sharing sharing;
    private int version;

    @Override
    public String toString() {
        return String.valueOf(version);
    }

    public Map<Integer, String> getMetadataKeys() {
        return this.metadataKeys;
    }

    public Sharing getSharing() {
        return this.sharing;
    }

    public int getVersion() {
        return this.version;
    }

    public void setMetadataKeys(Map<Integer, String> metadataKeys) {
        this.metadataKeys = metadataKeys;
    }

    public void setSharing(Sharing sharing) {
        this.sharing = sharing;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
