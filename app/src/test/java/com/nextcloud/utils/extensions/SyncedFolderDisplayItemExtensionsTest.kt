/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.utils.extensions

import com.nextcloud.client.jobs.upload.FileUploadWorker
import com.nextcloud.client.preferences.SubFolderRule
import com.owncloud.android.datamodel.MediaFolderType
import com.owncloud.android.datamodel.SyncedFolder
import com.owncloud.android.datamodel.SyncedFolderDisplayItem
import com.owncloud.android.files.services.NameCollisionPolicy
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncedFolderDisplayItemExtensionsTest {

    @Test
    fun regular() {
        assertSortedAfterShuffle(
            create("Folder1", true),
            create("Folder2", true)
        )
    }

    @Test
    fun withNull() {
        assertSortedAfterShuffle(
            null,
            null,
            create("Folder1", true),
            create("Folder2", true)
        )
    }

    @Test
    fun withNullAndEnableStatus() {
        assertSortedAfterShuffle(
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
    }

    @Test
    fun withNullFolderName() {
        assertSortedAfterShuffle(
            null,
            null,
            create("Folder1", true),
            create(null, false),
            create("Folder2", false),
            create("Folder3", false),
            create("Folder4", false),
            create("Folder5", false)
        )
    }

    @Test
    fun withNullFolderNameAllEnabled() {
        assertSortedAfterShuffle(
            null,
            null,
            create(null, true),
            create("Folder1", true),
            create("Folder2", true),
            create("Folder3", true),
            create("Folder4", true)
        )
    }

    private fun assertSortedAfterShuffle(vararg expectedOrder: SyncedFolderDisplayItem?) {
        val expected = expectedOrder.toList()
        val actual = expected.shuffled().sortAutoUploadFolderItems()

        val isSameOrder = expected.size == actual.size && expected.zip(actual).all { (e, a) -> e === a }
        assertTrue("expected:\n${expected.describe()}\nactual:\n${actual.describe()}", isSameOrder)
    }

    private fun List<SyncedFolderDisplayItem?>.describe(): String =
        joinToString("\n") { item -> item?.let { "${it.folderName} ${it.isEnabled}" } ?: "null" }

    private fun create(folderName: String?, enabled: Boolean): SyncedFolderDisplayItem = SyncedFolderDisplayItem(
        1,
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
        ArrayList(),
        folderName,
        2,
        MediaFolderType.IMAGE,
        false,
        SubFolderRule.YEAR_MONTH,
        true,
        SyncedFolder.NOT_SCANNED_YET
    )
}
