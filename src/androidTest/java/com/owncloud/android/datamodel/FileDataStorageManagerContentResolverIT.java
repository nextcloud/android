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

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FileDataStorageManagerContentResolverIT extends FileDataStorageManagerIT {
    @Override
    public void before() {
        sut = new FileDataStorageManager(user, targetContext.getContentResolver());

        super.before();
    }

    @Test
    /*
    only on FileDataStorageManager
     */
    public void testMoveManyFiles() {
        // create folder
        OCFile folderA = new OCFile("/folderA/", "00001"); // remote Id must never be null
        folderA.setFolder()
            .setParentId(sut.getFileByDecryptedRemotePath("/").getFileId());

        sut.saveFile(folderA);
        assertTrue(sut.fileExists("/folderA/"));
        assertEquals(0, sut.getFolderContent(folderA, false).size());

        long folderAId = sut.getFileByDecryptedRemotePath("/folderA/").getFileId();

        ArrayList<OCFile> newFiles = new ArrayList<>();
        for (int i = 0; i < 5000; i++) {
            OCFile file = new OCFile("/folderA/file" + i, String.valueOf(i));
            file.setParentId(folderAId);
            sut.saveFile(file);

            OCFile storedFile = sut.getFileByDecryptedRemotePath("/folderA/file" + i);
            assertNotNull(storedFile);

            newFiles.add(storedFile);
        }

        sut.saveFolder(folderA,
                       newFiles,
                       new ArrayList<>());

        assertEquals(5000, sut.getFolderContent(folderA, false).size());
    }
}
