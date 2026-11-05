/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.dialog.parcel

import com.nextcloud.client.jobs.upload.FileUploadWorker
import com.owncloud.android.datamodel.SyncedFolder
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncedFolderParcelableTest {

    @Test
    fun deleteAfterDelayKeepsFilesAndSetsDefaultRetention() {
        val parcelable = SyncedFolderParcelable()

        parcelable.setUploadAction(DELETE_AFTER_DELAY)

        assertEquals(FileUploadWorker.LOCAL_BEHAVIOUR_FORGET, parcelable.uploadAction)
        assertEquals(SyncedFolder.DEFAULT_AUTO_DELETE_LOCAL_AFTER_MS, parcelable.autoDeleteLocalAfterMs)
        assertEquals(DELETE_AFTER_DELAY_INDEX, parcelable.uploadActionInteger)
    }

    @Test
    fun otherBehavioursResetRetention() {
        val parcelable = SyncedFolderParcelable()
        parcelable.setUploadAction(DELETE_AFTER_DELAY)

        parcelable.setUploadAction("LOCAL_BEHAVIOUR_MOVE")

        assertEquals(SyncedFolder.AUTO_DELETE_LOCAL_DISABLED, parcelable.autoDeleteLocalAfterMs)
        assertEquals(FileUploadWorker.LOCAL_BEHAVIOUR_MOVE, parcelable.uploadAction)
    }

    private companion object {
        const val DELETE_AFTER_DELAY = "LOCAL_BEHAVIOUR_DELETE_AFTER_DELAY"
        const val DELETE_AFTER_DELAY_INDEX = 3
    }
}
