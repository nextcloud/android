/*
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * Copyright (C) 2020 Andy Scherzinger
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

package com.owncloud.android.utils

import com.owncloud.android.AbstractIT
import com.owncloud.android.datamodel.MediaFolder
import com.owncloud.android.datamodel.MediaFolderType
import com.owncloud.android.datamodel.SyncedFolder
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
        Assert.assertFalse(SyncedFolderUtils.isFileNameQualifiedForMediaDetection(COVER))
        Assert.assertFalse(SyncedFolderUtils.isFileNameQualifiedForMediaDetection("cover.JPG"))
        Assert.assertFalse(SyncedFolderUtils.isFileNameQualifiedForMediaDetection("cover.jpeg"))
        Assert.assertFalse(SyncedFolderUtils.isFileNameQualifiedForMediaDetection("cover.JPEG"))
        Assert.assertFalse(SyncedFolderUtils.isFileNameQualifiedForMediaDetection("COVER.jpg"))
        Assert.assertFalse(SyncedFolderUtils.isFileNameQualifiedForMediaDetection(FOLDER))
        Assert.assertFalse(SyncedFolderUtils.isFileNameQualifiedForMediaDetection("Folder.jpeg"))
        Assert.assertFalse(SyncedFolderUtils.isFileNameQualifiedForMediaDetection("FOLDER.jpg"))
        Assert.assertFalse(SyncedFolderUtils.isFileNameQualifiedForMediaDetection(".thumbdata4--1967290299"))
    }

    @Test
    fun assertImageFilenameQualified() {
        Assert.assertTrue(SyncedFolderUtils.isFileNameQualifiedForMediaDetection("image.jpg"))
        Assert.assertTrue(SyncedFolderUtils.isFileNameQualifiedForMediaDetection("screenshot.JPG"))
        Assert.assertTrue(SyncedFolderUtils.isFileNameQualifiedForMediaDetection(IMAGE_JPEG))
        Assert.assertTrue(SyncedFolderUtils.isFileNameQualifiedForMediaDetection("image.JPEG"))
        Assert.assertTrue(SyncedFolderUtils.isFileNameQualifiedForMediaDetection("SCREENSHOT.jpg"))
        Assert.assertTrue(SyncedFolderUtils.isFileNameQualifiedForMediaDetection(SELFIE))
        Assert.assertTrue(SyncedFolderUtils.isFileNameQualifiedForMediaDetection("screenshot.PNG"))
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
        val folder = MediaFolder()
        folder.type = MediaFolderType.VIDEO
        folder.numberOfFiles = 0L
        Assert.assertFalse(SyncedFolderUtils.isQualifyingMediaFolder(folder))
    }

    @Test
    fun assertMediaFolderVideoQualified() {
        val folder = MediaFolder()
        folder.type = MediaFolderType.VIDEO
        folder.numberOfFiles = 20L
        Assert.assertTrue(SyncedFolderUtils.isQualifyingMediaFolder(folder))
    }

    @Test
    fun assertMediaFolderImagesQualified() {
        val folder = MediaFolder()
        folder.type = MediaFolderType.IMAGE
        folder.numberOfFiles = 4L
        folder.filePaths = Arrays.asList(
            getDummyFile(SELFIE).absolutePath,
            getDummyFile(SCREENSHOT).absolutePath,
            getDummyFile(IMAGE_JPEG).absolutePath,
            getDummyFile(IMAGE_BITMAP).absolutePath
        )
        Assert.assertTrue(SyncedFolderUtils.isQualifyingMediaFolder(folder))
    }

    @Test
    fun assertMediaFolderImagesEmptyUnqualified() {
        val folder = MediaFolder()
        folder.type = MediaFolderType.IMAGE
        folder.numberOfFiles = 0L
        Assert.assertFalse(SyncedFolderUtils.isQualifyingMediaFolder(folder))
    }

    @Test
    fun assertMediaFolderImagesNoImagesUnqualified() {
        val folder = MediaFolder()
        folder.type = MediaFolderType.IMAGE
        folder.numberOfFiles = 3L
        folder.filePaths = Arrays.asList(
            getDummyFile(SONG_ZERO).absolutePath,
            getDummyFile(SONG_ONE).absolutePath,
            getDummyFile(SONG_TWO).absolutePath
        )
        Assert.assertFalse(SyncedFolderUtils.isQualifyingMediaFolder(folder))
    }

    @Test
    fun assertMediaFolderImagesMusicAlbumWithCoverArtUnqualified() {
        val folder = MediaFolder()
        folder.type = MediaFolderType.IMAGE
        folder.numberOfFiles = 3L
        folder.filePaths = Arrays.asList(
            getDummyFile(COVER).absolutePath,
            getDummyFile(SONG_ONE).absolutePath,
            getDummyFile(SONG_TWO).absolutePath
        )
        Assert.assertFalse(SyncedFolderUtils.isQualifyingMediaFolder(folder))
    }

    @Test
    fun assertMediaFolderImagesMusicAlbumWithFolderArtUnqualified() {
        val folder = MediaFolder()
        folder.type = MediaFolderType.IMAGE
        folder.numberOfFiles = 3L
        folder.filePaths = Arrays.asList(
            getDummyFile(FOLDER).absolutePath,
            getDummyFile(SONG_ONE).absolutePath,
            getDummyFile(SONG_TWO).absolutePath
        )
        Assert.assertFalse(SyncedFolderUtils.isQualifyingMediaFolder(folder))
    }

    @Test
    fun assertSyncedFolderNullSafe() {
        val folder: SyncedFolder? = null
        Assert.assertFalse(SyncedFolderUtils.isQualifyingMediaFolder(folder))
    }

    @Test
    fun assertUnqualifiedSyncedFolder() {
        val tempPath = File(FileStorageUtils.getTemporalPath(account.name) + File.pathSeparator + ".thumbnail")
        if (!tempPath.exists()) {
            Assert.assertTrue(tempPath.mkdirs())
        }
        getDummyFile(".thumbnail/image.jpg")
        val folder = SyncedFolder(
            tempPath.absolutePath,
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
            false)
        Assert.assertFalse(SyncedFolderUtils.isQualifyingMediaFolder(folder))
    }

    @Test
    fun assertUnqualifiedContentSyncedFolder() {
        val image = getDummyFile(THUMBDATA_FOLDER + File.pathSeparator + IMAGE_JPEG)
        val folder = SyncedFolder(
            FileStorageUtils.getTemporalPath(account.name) + File.pathSeparator + THUMBDATA_FOLDER,
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
            false)
        Assert.assertFalse(SyncedFolderUtils.isQualifyingMediaFolder(folder))
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
        private const val THUMBDATA_FOLDER = "thumbdata_test";
        private const val THUMBDATA_FILE = "thumbdata_test";
        private const val ITERATION = 100

        @BeforeClass
        fun setUp() {
            val tempPath = File(FileStorageUtils.getTemporalPath(account.name) + File.pathSeparator + THUMBDATA_FOLDER)
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

            createFile(THUMBDATA_FOLDER + File.pathSeparator + THUMBDATA_FILE, ITERATION)
        }

        @AfterClass
        fun tearDown() {
            FileUtils.deleteDirectory(File(FileStorageUtils.getTemporalPath(account.name)))
        }
    }
}
