/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.fragment.autoUpload.data

import com.nextcloud.client.core.Clock
import com.nextcloud.client.jobs.upload.FileUploadWorker
import com.nextcloud.client.preferences.SubFolderRule
import com.owncloud.android.datamodel.MediaFolder
import com.owncloud.android.datamodel.MediaFolderType
import com.owncloud.android.datamodel.SyncedFolder
import com.owncloud.android.datamodel.SyncedFolderDisplayItem
import com.owncloud.android.files.services.NameCollisionPolicy
import com.owncloud.android.ui.dialog.parcel.SyncedFolderParcelable
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@Suppress("MagicNumber")
class AutoUploadDisplayItemFactoryTest {

    private val enabledAt = 1_000_000_000_000L
    private val now = 2_000_000_000_000L

    private val clock: Clock = mockk()
    private lateinit var factory: AutoUploadDisplayItemFactory

    @Before
    fun setup() {
        every { clock.currentTime } returns now
        factory = AutoUploadDisplayItemFactory(clock, 4, "/InstantUpload")
    }

    private fun createItem(enabled: Boolean, timestampMs: Long = enabledAt) = SyncedFolderDisplayItem(
        1L,
        "/storage/emulated/0/DCIM/Camera",
        "/InstantUpload/Camera",
        true,
        false,
        false,
        false,
        "account",
        FileUploadWorker.LOCAL_BEHAVIOUR_FORGET,
        NameCollisionPolicy.ASK_USER.serialize(),
        enabled,
        timestampMs,
        "Camera",
        MediaFolderType.CUSTOM,
        false,
        SubFolderRule.YEAR_MONTH,
        false,
        SyncedFolder.NOT_SCANNED_YET
    )

    @Test
    fun `saving settings of an enabled folder keeps its enabled timestamp`() {
        val item = createItem(enabled = true)
        val parcelable = SyncedFolderParcelable(item, 0).apply { isWifiOnly = false }

        factory.updateAutoUploadFolderItem(item, parcelable)

        assertEquals(enabledAt, item.enabledTimestampMs)
    }

    @Test
    fun `enabling a folder through its settings starts a new enabled period`() {
        val item = createItem(enabled = false)
        val parcelable = SyncedFolderParcelable(item, 0).apply { isEnabled = true }

        factory.updateAutoUploadFolderItem(item, parcelable)

        assertEquals(now, item.enabledTimestampMs)
    }

    @Test
    fun `disabling a folder through its settings clears its enabled timestamp`() {
        val item = createItem(enabled = true)
        val parcelable = SyncedFolderParcelable(item, 0).apply { isEnabled = false }

        factory.updateAutoUploadFolderItem(item, parcelable)

        assertEquals(SyncedFolder.EMPTY_ENABLED_TIMESTAMP_MS, item.enabledTimestampMs)
    }

    @Test
    fun `display item of a stored folder keeps the stored enabled timestamp`() {
        val stored = createItem(enabled = true)
        val mediaFolder = MediaFolder().apply {
            folderName = "Camera"
            absolutePath = stored.localPath
            filePaths = emptyList()
            type = MediaFolderType.IMAGE
        }

        val item = factory.createAutoUploadFolder(stored, mediaFolder)

        assertEquals(enabledAt, item.enabledTimestampMs)
    }
}
