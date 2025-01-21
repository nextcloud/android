/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils

import com.nextcloud.client.preferences.SubFolderRule
import com.owncloud.android.AbstractIT
import com.owncloud.android.datamodel.MediaFolder
import com.owncloud.android.datamodel.MediaFolderType
import com.owncloud.android.datamodel.SyncedFolder
import com.owncloud.android.utils.SyncedFolderUtils.hasExcludePrefix
import org.apache.commons.io.FileUtils
import org.junit.AfterClass
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.util.Arrays

class SyncedFolderUtilsTest : AbstractIT() {
    @Test
    fun assertCoverFilenameUnqualified() {
        Assert.assertFalse(SyncedFolderUtils.isFileNameQualifiedForAutoUpload(COVER))
        Assert.assertFalse(SyncedFolderUtils.isFileNameQualifiedForAutoUpload("cover.JPG"))
        Assert.assertFalse(SyncedFolderUtils.isFileNameQualifiedForAutoUpload("cover.jpeg"))
        Assert.assertFalse(SyncedFolderUtils.isFileNameQualifiedForAutoUpload("cover.JPEG"))
        Assert.assertFalse(SyncedFolderUtils.isFileNameQualifiedForAutoUpload("COVER.jpg"))
        Assert.assertFalse(SyncedFolderUtils.isFileNameQualifiedForAutoUpload(FOLDER))
        Assert.assertFalse(SyncedFolderUtils.isFileNameQualifiedForAutoUpload("Folder.jpeg"))
        Assert.assertFalse(SyncedFolderUtils.isFileNameQualifiedForAutoUpload("FOLDER.jpg"))
        Assert.assertFalse(SyncedFolderUtils.isFileNameQualifiedForAutoUpload(THUMBDATA_FILE))
    }

    @Test
    fun assertImageFilenameQualified() {
        Assert.assertTrue(SyncedFolderUtils.isFileNameQualifiedForAutoUpload("image.jpg"))
        Assert.assertTrue(SyncedFolderUtils.isFileNameQualifiedForAutoUpload("screenshot.JPG"))
        Assert.assertTrue(SyncedFolderUtils.isFileNameQualifiedForAutoUpload(IMAGE_JPEG))
        Assert.assertTrue(SyncedFolderUtils.isFileNameQualifiedForAutoUpload("image.JPEG"))
        Assert.assertTrue(SyncedFolderUtils.isFileNameQualifiedForAutoUpload("SCREENSHOT.jpg"))
        Assert.assertTrue(SyncedFolderUtils.isFileNameQualifiedForAutoUpload(SELFIE))
        Assert.assertTrue(SyncedFolderUtils.isFileNameQualifiedForAutoUpload("screenshot.PNG"))
    }

    @Test
    fun assertMediaFolderNullSafe() {
        val folder: MediaFolder? = null
        Assert.assertFalse(SyncedFolderUtils.isQualifyingMediaFolder(folder))
    }

    @Test
    fun assertMediaFolderCustomQualified() {
        val folder = MediaFolder()
        folder.type = MediaFolderType.CUSTOM
        Assert.assertTrue(SyncedFolderUtils.isQualifyingMediaFolder(folder))
    }

    @Test
    fun assertMediaFolderVideoUnqualified() {
        val folder = MediaFolder().apply {
            absolutePath = getDummyFile(THUMBDATA_FOLDER).absolutePath
            type = MediaFolderType.VIDEO
            numberOfFiles = 0L
        }
        Assert.assertFalse(SyncedFolderUtils.isQualifyingMediaFolder(folder))
    }

    @Test
    fun assertMediaFolderVideoQualified() {
        val folder = MediaFolder().apply {
            absolutePath = getDummyFile(THUMBDATA_FOLDER).absolutePath
            type = MediaFolderType.VIDEO
            numberOfFiles = 20L
        }
        Assert.assertTrue(SyncedFolderUtils.isQualifyingMediaFolder(folder))
    }

    @Test
    fun assertMediaFolderImagesQualified() {
        val folder = MediaFolder().apply {
            absolutePath = getDummyFile(THUMBDATA_FOLDER).absolutePath
            type = MediaFolderType.IMAGE
            numberOfFiles = 4L
            filePaths = Arrays.asList(
                getDummyFile(SELFIE).absolutePath,
                getDummyFile(SCREENSHOT).absolutePath,
                getDummyFile(IMAGE_JPEG).absolutePath,
                getDummyFile(IMAGE_BITMAP).absolutePath
            )
        }
        Assert.assertTrue(SyncedFolderUtils.isQualifyingMediaFolder(folder))
    }

    @Test
    fun assertMediaFolderImagesEmptyUnqualified() {
        val folder = MediaFolder().apply {
            absolutePath = getDummyFile(THUMBDATA_FOLDER).absolutePath
            type = MediaFolderType.IMAGE
            numberOfFiles = 0L
        }
        Assert.assertFalse(SyncedFolderUtils.isQualifyingMediaFolder(folder))
    }

    @Test
    fun assertMediaFolderImagesNoImagesUnqualified() {
        val folder = MediaFolder().apply {
            absolutePath = getDummyFile(THUMBDATA_FOLDER).absolutePath
            type = MediaFolderType.IMAGE
            numberOfFiles = 3L
            filePaths = Arrays.asList(
                getDummyFile(SONG_ZERO).absolutePath,
                getDummyFile(SONG_ONE).absolutePath,
                getDummyFile(SONG_TWO).absolutePath
            )
        }
        Assert.assertFalse(SyncedFolderUtils.isQualifyingMediaFolder(folder))
    }

    @Test
    fun assertMediaFolderImagesMusicAlbumWithCoverArtUnqualified() {
        val folder = MediaFolder().apply {
            absolutePath = getDummyFile(THUMBDATA_FOLDER).absolutePath
            type = MediaFolderType.IMAGE
            numberOfFiles = 3L
            filePaths = Arrays.asList(
                getDummyFile(COVER).absolutePath,
                getDummyFile(SONG_ONE).absolutePath,
                getDummyFile(SONG_TWO).absolutePath
            )
        }
        Assert.assertFalse(SyncedFolderUtils.isQualifyingMediaFolder(folder))
    }

    @Test
    fun assertMediaFolderImagesMusicAlbumWithFolderArtUnqualified() {
        val folder = MediaFolder().apply {
            absolutePath = getDummyFile(THUMBDATA_FOLDER).absolutePath
            type = MediaFolderType.IMAGE
            numberOfFiles = 3L
            filePaths = Arrays.asList(
                getDummyFile(FOLDER).absolutePath,
                getDummyFile(SONG_ONE).absolutePath,
                getDummyFile(SONG_TWO).absolutePath
            )
        }

        Assert.assertFalse(SyncedFolderUtils.isQualifyingMediaFolder(folder))
    }

    @Test
    fun assertSyncedFolderNullSafe() {
        val folder: SyncedFolder? = null
        Assert.assertFalse(SyncedFolderUtils.isQualifyingMediaFolder(folder))
    }

    @Test
    fun assertUnqualifiedContentSyncedFolder() {
        val localFolder = getDummyFile(THUMBDATA_FOLDER + File.separatorChar)
        getDummyFile(THUMBDATA_FOLDER + File.separatorChar + THUMBDATA_FILE)
        val folder = SyncedFolder(
            localFolder.absolutePath,
            "",
            true,
            false,
            false,
            true,
            account.name,
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
        Assert.assertFalse(SyncedFolderUtils.isQualifyingMediaFolder(folder))
    }

    @Test
    fun assertUnqualifiedSyncedFolder() {
        getDummyFile(THUMBNAILS_FOLDER + File.separatorChar + IMAGE_JPEG)
        getDummyFile(THUMBNAILS_FOLDER + File.separatorChar + IMAGE_BITMAP)
        val folder = SyncedFolder(
            FileStorageUtils.getTemporalPath(account.name) + File.separatorChar + THUMBNAILS_FOLDER,
            "",
            true,
            false,
            false,
            true,
            account.name,
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
        Assert.assertFalse(SyncedFolderUtils.isQualifyingMediaFolder(folder))
    }

    @Test
    fun testInstantUploadPathIgnoreExcludedPrefixes() {
        val testFiles = listOf(
            "IMG_nnn.jpg",
            "my_documents",
            "Music",
            ".trashed_IMG_nnn.jpg",
            ".pending_IMG_nnn.jpg",
            ".nomedia",
            ".thumbdata_IMG_nnn",
            ".thumbnail"
        ).filter { !hasExcludePrefix(it) }
        Assert.assertTrue(testFiles.size == 3)
    }

    companion object {
        private const val SELFIE = "selfie.png"
        private const val SCREENSHOT = "screenshot.JPG"
        private const val IMAGE_JPEG = "image.jpeg"
        private const val IMAGE_BITMAP = "image.bmp"
        private const val SONG_ZERO = "song0.mp3"
        private const val SONG_ONE = "song1.mp3"
        private const val SONG_TWO = "song2.mp3"
        private const val FOLDER = "folder.JPG"
        private const val COVER = "cover.jpg"
        private const val THUMBNAILS_FOLDER = ".thumbnails/"
        private const val THUMBDATA_FOLDER = "valid_folder/"
        private const val THUMBDATA_FILE = ".thumbdata4--1967290299"
        private const val ITERATION = 100

        @BeforeClass
        @JvmStatic
        fun setUp() {
            val tempPath =
                File(
                    FileStorageUtils.getTemporalPath(account.name) + File.separatorChar +
                        THUMBNAILS_FOLDER
                )
            if (!tempPath.exists()) {
                tempPath.mkdirs()
            }

            createFile(SELFIE, ITERATION)
            createFile(SCREENSHOT, ITERATION)
            createFile(IMAGE_JPEG, ITERATION)
            createFile(IMAGE_BITMAP, ITERATION)
            createFile(SONG_ZERO, ITERATION)
            createFile(SONG_ONE, ITERATION)
            createFile(SONG_TWO, ITERATION)
            createFile(FOLDER, ITERATION)
            createFile(COVER, ITERATION)

            createFile(THUMBDATA_FOLDER + File.separatorChar + THUMBDATA_FILE, ITERATION)
            createFile(THUMBNAILS_FOLDER + File.separatorChar + IMAGE_JPEG, ITERATION)
            createFile(THUMBNAILS_FOLDER + File.separatorChar + IMAGE_BITMAP, ITERATION)
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            FileUtils.deleteDirectory(File(FileStorageUtils.getTemporalPath(account.name)))
        }
    }
}
