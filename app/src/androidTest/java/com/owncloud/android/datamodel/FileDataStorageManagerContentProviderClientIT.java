/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
