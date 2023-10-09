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
package com.owncloud.android.ui.activity

import com.nextcloud.client.preferences.SubFolderRule
import com.owncloud.android.datamodel.MediaFolderType
import com.owncloud.android.datamodel.SyncedFolderDisplayItem
import com.owncloud.android.files.services.FileUploader
import com.owncloud.android.files.services.NameCollisionPolicy
import com.owncloud.android.ui.activity.SyncedFoldersActivity.Companion.sortSyncedFolderItems
import org.junit.Assert
import org.junit.Test
import java.util.Arrays
import java.util.Collections

class SyncedFoldersActivityTest {
    @Test
    fun regular() {
        val sortedArray = arrayOf(
            create("Folder1", true),
            create("Folder2", true)
        )
        Assert.assertTrue(sortAndTest(Arrays.asList(*sortedArray)))
    }

    @Test
    fun withNull() {
        val sortedArray = arrayOf(
            null,
            null,
            create("Folder1", true),
            create("Folder2", true)
        )
        Assert.assertTrue(sortAndTest(Arrays.asList(*sortedArray)))
    }

    @Test
    fun withNullAndEnableStatus() {
        val sortedArray = arrayOf(
            null,
            null,
            create("Folder1", true),
            create("Folder2", true),
            create("Folder3", true),
            create("Folder4", true),
            create("Folder5", false),
            create("Folder6", false),
            create("Folder7", false),
            create("Folder8", false)
        )
        Assert.assertTrue(sortAndTest(Arrays.asList(*sortedArray)))
    }

    @Test
    fun withNullFolderName() {
        val sortedArray = arrayOf(
            null,
            null,
            create("Folder1", true),
            create(null, false),
            create("Folder2", false),
            create("Folder3", false),
            create("Folder4", false),
            create("Folder5", false)
        )
        Assert.assertTrue(sortAndTest(Arrays.asList(*sortedArray)))
    }

    @Test
    fun withNullFolderNameAllEnabled() {
        val sortedArray = arrayOf(
            null,
            null,
            create(null, true),
            create("Folder1", true),
            create("Folder2", true),
            create("Folder3", true),
            create("Folder4", true)
        )
        Assert.assertTrue(sortAndTest(Arrays.asList(*sortedArray)))
    }

    private fun shuffle(list: List<SyncedFolderDisplayItem?>): List<SyncedFolderDisplayItem?> {
        val shuffled: List<SyncedFolderDisplayItem?> = ArrayList(list)
        Collections.shuffle(shuffled)
        return shuffled
    }

    private fun sortAndTest(sortedList: List<SyncedFolderDisplayItem?>): Boolean {
        val unsortedList = shuffle(sortedList)
        return test(sortedList, sortSyncedFolderItems(unsortedList))
    }

    private fun test(target: List<SyncedFolderDisplayItem?>, actual: List<SyncedFolderDisplayItem?>): Boolean {
        for (i in target.indices) {
            var compare: Boolean
            compare = target[i] === actual[i]
            if (!compare) {
                println("target:")
                for (item in target) {
                    if (item == null) {
                        println("null")
                    } else {
                        println(item.folderName + " " + item.isEnabled)
                    }
                }
                println()
                println("actual:")
                for (item in actual) {
                    if (item == null) {
                        println("null")
                    } else {
                        println(item.folderName + " " + item.isEnabled)
                    }
                }
                return false
            }
        }
        return true
    }

    private fun create(folderName: String?, enabled: Boolean): SyncedFolderDisplayItem {
        return SyncedFolderDisplayItem(
            1,
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
            ArrayList(),
            folderName,
            2,
            MediaFolderType.IMAGE,
            false,
            SubFolderRule.YEAR_MONTH
        )
    }
}