/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2017 Tobias Kaminsky
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.events;

/**
 * Event for set folder as encrypted/decrypted
 */
public class EncryptionEvent {
    public final String localId;
    public final String remotePath;
    public final String remoteId;
    public final boolean shouldBeEncrypted;

    public EncryptionEvent(String localId, String remoteId, String remotePath, boolean shouldBeEncrypted) {
        this.localId = localId;
        this.remoteId = remoteId;
        this.remotePath = remotePath;
        this.shouldBeEncrypted = shouldBeEncrypted;
    }
}
