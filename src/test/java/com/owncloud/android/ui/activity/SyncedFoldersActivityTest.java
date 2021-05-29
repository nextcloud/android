/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2019 Tobias Kaminsky
 * Copyright (C) 2019 Nextcloud GmbH
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

package com.owncloud.android.ui.activity;

import com.owncloud.android.datamodel.MediaFolderType;
import com.owncloud.android.datamodel.SyncedFolderDisplayItem;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.files.services.NameCollisionPolicy;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class SyncedFoldersActivityTest {

    @Test
    public void regular() {
        SyncedFolderDisplayItem[] sortedArray = {
            create("Folder1", true),
            create("Folder2", true),
        };

        assertTrue(sortAndTest(Arrays.asList(sortedArray)));
    }

    @Test
    public void withNull() {
        SyncedFolderDisplayItem[] sortedArray = {
            null,
            null,
            create("Folder1", true),
            create("Folder2", true),
        };

        assertTrue(sortAndTest(Arrays.asList(sortedArray)));
    }

    @Test
    public void withNullAndEnableStatus() {
        SyncedFolderDisplayItem[] sortedArray = {
            null,
            null,
            create("Folder1", true),
            create("Folder2", true),
            create("Folder3", true),
            create("Folder4", true),
            create("Folder5", false),
            create("Folder6", false),
            create("Folder7", false),
            create("Folder8", false),
        };

        assertTrue(sortAndTest(Arrays.asList(sortedArray)));
    }

    @Test
    public void withNullFolderName() {
        SyncedFolderDisplayItem[] sortedArray = {
            null,
            null,
            create("Folder1", true),
            create(null, false),
            create("Folder2", false),
            create("Folder3", false),
            create("Folder4", false),
            create("Folder5", false),
        };

        assertTrue(sortAndTest(Arrays.asList(sortedArray)));
    }

    @Test
    public void withNullFolderNameAllEnabled() {
        SyncedFolderDisplayItem[] sortedArray = {
            null,
            null,
            create(null, true),
            create("Folder1", true),
            create("Folder2", true),
            create("Folder3", true),
            create("Folder4", true),
        };

        assertTrue(sortAndTest(Arrays.asList(sortedArray)));
    }

    private List<SyncedFolderDisplayItem> shuffle(List<SyncedFolderDisplayItem> list) {
        List<SyncedFolderDisplayItem> shuffled = new ArrayList<>(list);

        Collections.shuffle(shuffled);

        return shuffled;
    }

    private boolean sortAndTest(List<SyncedFolderDisplayItem> sortedList) {
        List<SyncedFolderDisplayItem> unsortedList = shuffle(sortedList);

        return test(sortedList, SyncedFoldersActivity.sortSyncedFolderItems(unsortedList));
    }

    private boolean test(List<SyncedFolderDisplayItem> target, List<SyncedFolderDisplayItem> actual) {

        for (int i = 0; i < target.size(); i++) {
            boolean compare;

            compare = target.get(i) == (actual.get(i));

            if (!compare) {

                System.out.println("target:");

                for (SyncedFolderDisplayItem item : target) {
                    if (item == null) {
                        System.out.println("null");
                    } else {
                        System.out.println(item.getFolderName() + " " + item.isEnabled());
                    }
                }

                System.out.println();
                System.out.println("actual:");
                for (SyncedFolderDisplayItem item : actual) {
                    if (item == null) {
                        System.out.println("null");
                    } else {
                        System.out.println(item.getFolderName() + " " + item.isEnabled());
                    }
                }

                return false;
            }
        }

        return true;
    }

    private SyncedFolderDisplayItem create(String folderName, boolean enabled) {
        return new SyncedFolderDisplayItem(1,
                                           "localPath",
                                           "remotePath",
                                           true,
                                           true,
                                           true,
                                           true,
                                           "test@nextcloud.com",
                                           FileUploader.LOCAL_BEHAVIOUR_MOVE,
                                           NameCollisionPolicy.ASK_USER.serialize(),
                                           enabled,
                                           System.currentTimeMillis(),
                                           new ArrayList<String>(),
                                           folderName,
                                           2,
                                           MediaFolderType.IMAGE,
                                           false);
    }
}
