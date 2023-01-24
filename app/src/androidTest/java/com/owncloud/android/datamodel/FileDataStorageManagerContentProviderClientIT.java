/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
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

package com.owncloud.android.datamodel;

import com.owncloud.android.db.ProviderMeta;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class FileDataStorageManagerContentProviderClientIT extends FileDataStorageManagerIT {
    public void before() {
        sut = new FileDataStorageManager(user,
                                         targetContext
                                             .getContentResolver()
                                             .acquireContentProviderClient(ProviderMeta.ProviderTableMeta.CONTENT_URI)
        );

        super.before();
    }

    @Test
    public void saveFile() {

        String path = "/1.txt";
        OCFile file = new OCFile(path);
        file.setRemoteId("00000008ocjycgrudn78");

        // TODO check via reflection that every parameter is set

        file.setFileLength(1024000);
        file.setModificationTimestamp(1582019340);
        sut.saveNewFile(file);


        OCFile read = sut.getFileByPath(path);
        assertNotNull(read);

        assertEquals(file.getRemotePath(), read.getRemotePath());
    }
}
