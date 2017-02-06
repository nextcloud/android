/**
 * Nextcloud Android client application
 *
 * @author Mario Danic
 * Copyright (C) 2017 Mario Danic
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
package com.owncloud.android.datamodel;

import android.support.v4.util.Pair;

import org.apache.commons.io.monitor.FileEntry;

import java.io.IOException;
import java.io.Serializable;

/**
 * Pair that we can serialize
 */

public class SerializablePair<S, F> implements Serializable {
    private static final long serialVersionUID = -1710182118966395715L;
    private transient Pair pair = null;

    public SerializablePair(SyncedFolder key, FileEntry value) {
        this.pair = new Pair(key, value);
    }

    private void writeObject(java.io.ObjectOutputStream stream)
            throws IOException {
        stream.writeObject(pair.first);
        stream.writeObject(pair.second);
    }

    private void readObject(java.io.ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
        SyncedFolder syncedFolder = (SyncedFolder) stream.readObject();
        FileEntry fileEntry = (FileEntry) stream.readObject();
        pair = new Pair(syncedFolder, fileEntry);
    }

    @Override
    public String toString() {
        return pair.toString();
    }

    public SyncedFolder getKey() {
        return (SyncedFolder)this.pair.first;
    }

    public FileEntry getValue() {
        return (FileEntry)this.pair.second;
    }
}
