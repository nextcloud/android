/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.utils

import com.nextcloud.client.preferences.SubFolderRule
import com.nextcloud.utils.extensions.filterEnabledOrWithoutEnabledParent
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
                "/root/my_folder",
                "/InstantUpload/",
                true,
                false,
                false,
                true,
                "test@https://nextcloud.localhost",
                0,
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
                "/root/my_folder/B",
                "/InstantUpload/",
                true,
                false,
                false,
                true,
                "test@https://nextcloud.localhost",
                0,
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
                "/root/my_folder_2",
                "/InstantUpload/",
                true,
                false,
                false,
                true,
                "test@https://nextcloud.localhost",
                0,
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

        val filteredList = list.filterEnabledOrWithoutEnabledParent()

        val firstItem = filteredList.find { it.id == 1L }
        val secondItem = filteredList.find { it.id == 3L }

        assert(firstItem != null)
        assert(secondItem != null)
    }
}
