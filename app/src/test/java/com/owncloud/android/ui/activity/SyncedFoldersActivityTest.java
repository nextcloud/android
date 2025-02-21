/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activity;

import com.nextcloud.client.jobs.upload.FileUploadWorker;
import com.nextcloud.client.preferences.SubFolderRule;
import com.owncloud.android.datamodel.MediaFolderType;
import com.owncloud.android.datamodel.SyncedFolder;
import com.owncloud.android.datamodel.SyncedFolderDisplayItem;
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
                                           FileUploadWorker.LOCAL_BEHAVIOUR_MOVE,
                                           NameCollisionPolicy.ASK_USER.serialize(),
                                           enabled,
                                           System.currentTimeMillis(),
                                           new ArrayList<String>(),
                                           folderName,
                                           2,
                                           MediaFolderType.IMAGE,
                                           false,
                                           SubFolderRule.YEAR_MONTH,
                                           true,
                                           SyncedFolder.NOT_SCANNED_YET,
                                           true);
    }
}
