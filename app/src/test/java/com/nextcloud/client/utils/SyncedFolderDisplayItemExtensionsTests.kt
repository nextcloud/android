/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.utils

import com.nextcloud.client.preferences.SubFolderRule
import com.nextcloud.utils.extensions.filterEnabledOrWithoutParentInEnabledSet
import com.nextcloud.utils.extensions.filterEnabledSubfoldersWithEnabledParent
import com.owncloud.android.datamodel.MediaFolderType
import com.owncloud.android.datamodel.SyncedFolder
import com.owncloud.android.datamodel.SyncedFolderDisplayItem
import org.junit.Test

class SyncedFolderDisplayItemExtensionsTests {

    @Suppress("MagicNumber", "LongMethod")
    @Test
    fun testFilterEnabledOrWithoutParentInEnabledSet() {
        val list = listOf(
            SyncedFolderDisplayItem(
                1,
                "/storage/emulated/0/DCIM/my_folder",
                "/InstantUpload/",
                true,
                false,
                false,
                true,
                "test@https://nextcloud.localhost",
                0,
                0,
                true,
                1000,
                "my_folder",
                MediaFolderType.IMAGE,
                false,
                SubFolderRule.YEAR_MONTH,
                false,
                SyncedFolder.NOT_SCANNED_YET
            ),
            SyncedFolderDisplayItem(
                2,
                "/storage/emulated/0/DCIM/my_folder/B",
                "/InstantUpload/",
                true,
                false,
                false,
                true,
                "test@https://nextcloud.localhost",
                0,
                0,
                false,
                1000,
                "B",
                MediaFolderType.IMAGE,
                false,
                SubFolderRule.YEAR_MONTH,
                false,
                SyncedFolder.NOT_SCANNED_YET
            ),
            SyncedFolderDisplayItem(
                3,
                "/storage/emulated/0/DCIM/my_folder_2",
                "/InstantUpload/",
                true,
                false,
                false,
                true,
                "test@https://nextcloud.localhost",
                0,
                0,
                true,
                1000,
                "my_folder_2",
                MediaFolderType.IMAGE,
                false,
                SubFolderRule.YEAR_MONTH,
                false,
                SyncedFolder.NOT_SCANNED_YET
            )
        )

        val filteredList = list.filterEnabledOrWithoutParentInEnabledSet()

        val firstItem = filteredList.find { it.id == 1L }
        val secondItem = filteredList.find { it.id == 3L }

        assert(firstItem != null)
        assert(secondItem != null)
    }

    @Suppress("MagicNumber", "LongMethod")
    @Test
    fun testFilterEnabledSubfoldersWithEnabledParent() {
        val list = listOf(
            SyncedFolder(
                "/storage/emulated/0/DCIM/my_folder",
                "",
                true,
                false,
                false,
                true,
                "account",
                1,
                1,
                true,
                0L,
                MediaFolderType.IMAGE,
                false,
                SubFolderRule.YEAR_MONTH,
                false,
                SyncedFolder.NOT_SCANNED_YET
            ),
            SyncedFolder(
                "/storage/emulated/0/DCIM/my_folder/B",
                "",
                true,
                false,
                false,
                true,
                "account",
                1,
                1,
                true,
                0L,
                MediaFolderType.IMAGE,
                false,
                SubFolderRule.YEAR_MONTH,
                false,
                SyncedFolder.NOT_SCANNED_YET
            ),
            SyncedFolder(
                "/storage/emulated/0/DCIM/my_folder_2",
                "",
                true,
                false,
                false,
                true,
                "account",
                1,
                1,
                true,
                0L,
                MediaFolderType.IMAGE,
                false,
                SubFolderRule.YEAR_MONTH,
                false,
                SyncedFolder.NOT_SCANNED_YET
            )
        )

        val filteredList = list.filterEnabledSubfoldersWithEnabledParent()

        val firstItem = filteredList.find { it.localPath == "/storage/emulated/0/DCIM/my_folder/B" }

        assert(firstItem != null && filteredList.size == 1)
    }
}
