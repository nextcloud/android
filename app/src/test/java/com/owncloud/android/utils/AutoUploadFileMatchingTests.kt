/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.utils

import com.owncloud.android.datamodel.MediaFolderType
import com.owncloud.android.datamodel.SyncedFolder
import com.owncloud.android.files.services.NameCollisionPolicy
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@Suppress("TooManyFunctions")
class AutoUploadFileMatchingTests {

    companion object {
        private const val LOCAL_PATH = "/storage/emulated/0/DCIM/Camera"
        private const val LOCAL_PATH_WITH_SEPARATOR = "/storage/emulated/0/DCIM/Camera/"

        private const val IMAGE_FILE_PATH = "/storage/emulated/0/DCIM/Camera/photo.jpg"
        private const val VIDEO_FILE_PATH = "/storage/emulated/0/DCIM/Camera/video.mp4"
        private const val CUSTOM_FILE_PATH = "/storage/emulated/0/DCIM/Camera/document.pdf"

        private const val VIDEO_FILE_OUTSIDE_PATH = "/storage/emulated/0/Downloads/archive.mp4"
        private const val VIDEO_FILE_SIMILAR_PATH = "/storage/emulated/0/DCIM/Camera_backup/archive.mp4"

        private const val IMAGE_FILE_OUTSIDE_PATH = "/storage/emulated/0/Downloads/photo.jpg"
        private const val IMAGE_FILE_SIMILAR_PATH = "/storage/emulated/0/DCIM/Camera_backup/photo.jpg"
    }

    private lateinit var imageFile: File
    private lateinit var videoFile: File
    private lateinit var customFile: File

    @Before
    fun setUp() {
        mockkStatic(MimeTypeUtil::class)

        imageFile = File(IMAGE_FILE_PATH)
        videoFile = File(VIDEO_FILE_PATH)
        customFile = File(CUSTOM_FILE_PATH)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // region Helper
    private fun createSyncedFolder(localPath: String?, type: MediaFolderType): SyncedFolder = SyncedFolder(
        localPath,
        "/remote/path",
        false,
        false,
        false,
        false,
        "account@server",
        0,
        NameCollisionPolicy.SKIP.serialize(),
        true,
        System.currentTimeMillis(),
        type,
        false,
        com.nextcloud.client.preferences.SubFolderRule.YEAR_MONTH,
        false,
        System.currentTimeMillis()
    )
    // endregion

    // region IMAGE type tests
    @Test
    fun `given image folder when file is image and under local path then returns true`() {
        every { MimeTypeUtil.isImage(imageFile) } returns true
        every { MimeTypeUtil.isVideo(imageFile) } returns false

        val folder = createSyncedFolder(LOCAL_PATH, MediaFolderType.IMAGE)
        assertTrue(folder.isFileInFolderWithCorrectMediaType(imageFile, IMAGE_FILE_PATH))
    }

    @Test
    fun `given image folder when file is video and under local path then returns false`() {
        every { MimeTypeUtil.isImage(videoFile) } returns false
        every { MimeTypeUtil.isVideo(videoFile) } returns true

        val folder = createSyncedFolder(LOCAL_PATH, MediaFolderType.IMAGE)
        assertFalse(folder.isFileInFolderWithCorrectMediaType(videoFile, VIDEO_FILE_PATH))
    }

    @Test
    fun `given image folder when file is image but outside local path then returns false`() {
        val outsideFile = File(IMAGE_FILE_OUTSIDE_PATH)
        every { MimeTypeUtil.isImage(outsideFile) } returns true
        every { MimeTypeUtil.isVideo(outsideFile) } returns false

        val folder = createSyncedFolder(LOCAL_PATH, MediaFolderType.IMAGE)
        assertFalse(folder.isFileInFolderWithCorrectMediaType(outsideFile, IMAGE_FILE_OUTSIDE_PATH))
    }

    @Test
    fun `given image folder when file is image and local path has trailing separator then returns true`() {
        every { MimeTypeUtil.isImage(imageFile) } returns true
        every { MimeTypeUtil.isVideo(imageFile) } returns false

        val folder = createSyncedFolder(LOCAL_PATH_WITH_SEPARATOR, MediaFolderType.IMAGE)
        assertTrue(folder.isFileInFolderWithCorrectMediaType(imageFile, IMAGE_FILE_PATH))
    }

    @Test
    fun `given image folder when file path is in similar named folder then returns false`() {
        val similarFile = File(IMAGE_FILE_SIMILAR_PATH)
        every { MimeTypeUtil.isImage(similarFile) } returns true
        every { MimeTypeUtil.isVideo(similarFile) } returns false

        val folder = createSyncedFolder(LOCAL_PATH, MediaFolderType.IMAGE)
        assertFalse(
            "Should not match path in a folder that starts with the same prefix but is a different folder",
            folder.isFileInFolderWithCorrectMediaType(similarFile, IMAGE_FILE_SIMILAR_PATH)
        )
    }

    @Test
    fun `given image folder when file path is null then returns false`() {
        val folder = createSyncedFolder(LOCAL_PATH, MediaFolderType.IMAGE)
        assertFalse(folder.isFileInFolderWithCorrectMediaType(imageFile, null))
    }

    @Test
    fun `given image folder when local path is null then returns false`() {
        every { MimeTypeUtil.isImage(imageFile) } returns true
        val folder = createSyncedFolder(null, MediaFolderType.IMAGE)
        assertFalse(folder.isFileInFolderWithCorrectMediaType(imageFile, IMAGE_FILE_PATH))
    }

    // endregion

    // region VIDEO type tests
    @Test
    fun `given video folder when file is video and under local path then returns true`() {
        every { MimeTypeUtil.isImage(videoFile) } returns false
        every { MimeTypeUtil.isVideo(videoFile) } returns true

        val folder = createSyncedFolder(LOCAL_PATH, MediaFolderType.VIDEO)
        assertTrue(folder.isFileInFolderWithCorrectMediaType(videoFile, VIDEO_FILE_PATH))
    }

    @Test
    fun `given video folder when file is image and under local path then returns false`() {
        every { MimeTypeUtil.isImage(imageFile) } returns true
        every { MimeTypeUtil.isVideo(imageFile) } returns false

        val folder = createSyncedFolder(LOCAL_PATH, MediaFolderType.VIDEO)
        assertFalse(folder.isFileInFolderWithCorrectMediaType(imageFile, IMAGE_FILE_PATH))
    }

    @Test
    fun `given video folder when file is video but outside local path then returns false`() {
        val outsideVideo = File(VIDEO_FILE_OUTSIDE_PATH)
        every { MimeTypeUtil.isImage(outsideVideo) } returns false
        every { MimeTypeUtil.isVideo(outsideVideo) } returns true

        val folder = createSyncedFolder(LOCAL_PATH, MediaFolderType.VIDEO)
        assertFalse(folder.isFileInFolderWithCorrectMediaType(outsideVideo, VIDEO_FILE_OUTSIDE_PATH))
    }

    @Test
    fun `given video folder when file is video and local path has trailing separator then returns true`() {
        every { MimeTypeUtil.isImage(videoFile) } returns false
        every { MimeTypeUtil.isVideo(videoFile) } returns true

        val folder = createSyncedFolder(LOCAL_PATH_WITH_SEPARATOR, MediaFolderType.VIDEO)
        assertTrue(folder.isFileInFolderWithCorrectMediaType(videoFile, VIDEO_FILE_PATH))
    }

    @Test
    fun `given video folder when file path is in similar named folder then returns false`() {
        val similarVideo = File(VIDEO_FILE_OUTSIDE_PATH)
        every { MimeTypeUtil.isImage(similarVideo) } returns false
        every { MimeTypeUtil.isVideo(similarVideo) } returns true

        val folder = createSyncedFolder(LOCAL_PATH, MediaFolderType.VIDEO)
        assertFalse(
            "Should not match path in a folder that starts with the same prefix but is a different folder",
            folder.isFileInFolderWithCorrectMediaType(similarVideo, VIDEO_FILE_SIMILAR_PATH)
        )
    }

    @Test
    fun `given video folder when file path is null then returns false`() {
        val folder = createSyncedFolder(LOCAL_PATH, MediaFolderType.VIDEO)
        assertFalse(folder.isFileInFolderWithCorrectMediaType(videoFile, null))
    }

    // endregion

    // region CUSTOM type tests
    @Test
    fun `given custom folder when any file type is under local path then returns true`() {
        every { MimeTypeUtil.isImage(customFile) } returns false
        every { MimeTypeUtil.isVideo(customFile) } returns false

        val folder = createSyncedFolder(LOCAL_PATH, MediaFolderType.CUSTOM)
        assertTrue(folder.isFileInFolderWithCorrectMediaType(customFile, CUSTOM_FILE_PATH))
    }

    @Test
    fun `given custom folder when image file is under local path then returns true`() {
        every { MimeTypeUtil.isImage(imageFile) } returns true
        every { MimeTypeUtil.isVideo(imageFile) } returns false

        val folder = createSyncedFolder(LOCAL_PATH, MediaFolderType.CUSTOM)
        assertTrue(folder.isFileInFolderWithCorrectMediaType(imageFile, IMAGE_FILE_PATH))
    }

    @Test
    fun `given custom folder when video file is under local path then returns true`() {
        every { MimeTypeUtil.isImage(videoFile) } returns false
        every { MimeTypeUtil.isVideo(videoFile) } returns true

        val folder = createSyncedFolder(LOCAL_PATH, MediaFolderType.CUSTOM)
        assertTrue(folder.isFileInFolderWithCorrectMediaType(videoFile, VIDEO_FILE_PATH))
    }

    @Test
    fun `given custom folder when file is outside local path then returns false`() {
        val outsideFile = File(IMAGE_FILE_OUTSIDE_PATH)
        every { MimeTypeUtil.isImage(outsideFile) } returns true
        every { MimeTypeUtil.isVideo(outsideFile) } returns false

        val folder = createSyncedFolder(LOCAL_PATH, MediaFolderType.CUSTOM)
        assertFalse(folder.isFileInFolderWithCorrectMediaType(outsideFile, IMAGE_FILE_OUTSIDE_PATH))
    }

    @Test
    fun `given custom folder when local path has trailing separator then returns true`() {
        every { MimeTypeUtil.isImage(customFile) } returns false
        every { MimeTypeUtil.isVideo(customFile) } returns false

        val folder = createSyncedFolder(LOCAL_PATH_WITH_SEPARATOR, MediaFolderType.CUSTOM)
        assertTrue(folder.isFileInFolderWithCorrectMediaType(customFile, CUSTOM_FILE_PATH))
    }

    @Test
    fun `given custom folder when file path is in similar named folder then returns false`() {
        val similarFile = File(IMAGE_FILE_SIMILAR_PATH)
        every { MimeTypeUtil.isImage(similarFile) } returns true
        every { MimeTypeUtil.isVideo(similarFile) } returns false

        val folder = createSyncedFolder(LOCAL_PATH, MediaFolderType.CUSTOM)
        assertFalse(
            "Should not match path in a folder that starts with the same prefix but is a different folder",
            folder.isFileInFolderWithCorrectMediaType(similarFile, IMAGE_FILE_SIMILAR_PATH)
        )
    }

    @Test
    fun `given custom folder when file path is null then returns false`() {
        val folder = createSyncedFolder(LOCAL_PATH, MediaFolderType.CUSTOM)
        assertFalse(folder.isFileInFolderWithCorrectMediaType(customFile, null))
    }
    // endregion

    // region Path edge case tests
    @Test
    fun `given image folder when file is directly inside local path root then returns true`() {
        every { MimeTypeUtil.isImage(imageFile) } returns true
        every { MimeTypeUtil.isVideo(imageFile) } returns false

        val folder = createSyncedFolder(LOCAL_PATH, MediaFolderType.IMAGE)
        assertTrue(folder.isFileInFolderWithCorrectMediaType(imageFile, IMAGE_FILE_PATH))
    }

    @Test
    fun `given any folder type when file is in nested subdirectory then returns true`() {
        val nestedPath = "/storage/emulated/0/DCIM/Camera/2024/March/photo.jpg"
        val nestedFile = File(nestedPath)
        every { MimeTypeUtil.isImage(nestedFile) } returns true
        every { MimeTypeUtil.isVideo(nestedFile) } returns false

        val folder = createSyncedFolder(LOCAL_PATH, MediaFolderType.IMAGE)
        assertTrue(folder.isFileInFolderWithCorrectMediaType(nestedFile, nestedPath))
    }

    @Test
    fun `given any folder type when local path is empty string then returns false`() {
        every { MimeTypeUtil.isImage(imageFile) } returns true

        val folder = createSyncedFolder("", MediaFolderType.IMAGE)
        val result = folder.isFileInFolderWithCorrectMediaType(imageFile, IMAGE_FILE_PATH)
        assertFalse("Empty local path should not match arbitrary paths", result)
    }
    // endregion
}
