/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.datamodel;

import android.util.Pair;

import com.nextcloud.utils.extensions.FileDataStorageManagerExtensionsKt;
import com.owncloud.android.db.ProviderMeta;

import org.junit.Test;

import java.util.Arrays;

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

    @Test
    public void testGenerateFileNameForConflictResolution() {
        final var names = Arrays.asList(
            // Files
            new Pair<>("hello", "hello (1)"),
            new Pair<>("hello.txt", "hello (1).txt"),
            new Pair<>("hello (1).txt", "hello (2).txt"),
            new Pair<>("hello (18y5).txt", "hello (18y5) (1).txt"),
            new Pair<>("hello (hey)", "hello (hey) (1)"),
            new Pair<>("hello (hey).txt", "hello (hey) (1).txt"),
            new Pair<>(".hello", ".hello (1)"),

            // Folders
            new Pair<>("hello/", "hello (1)/"),
            new Pair<>("hello (1)/", "hello (2)/"),
            new Pair<>("hello.hello/", "hello.hello (1)/"),
            new Pair<>("hello.hello (y)/", "hello.hello (y) (1)/"),
            new Pair<>(".hello/", ".hello (1)/")
         );

        for (final var name: names) {
            String gen = FileDataStorageManagerExtensionsKt.generateFileNameForConflictResolution(name.first);
            assertEquals(gen, name.second);
        }
    }
}
