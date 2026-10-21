/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Daniele Verducci <daniele.verducci@nextcloud.com>
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.extensions

import android.graphics.Bitmap
import com.nextcloud.utils.extensions.areImageDimensionsSame
import com.owncloud.android.lib.resources.files.model.ImageDimension
import com.owncloud.android.lib.resources.files.model.RemoteFile
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists

class RemoteFileExtensionsImageTest {

    private lateinit var tempDir: Path
    private val remoteFileWithDimensions = RemoteFile().apply {
        mimeType = "image/jpeg"
        imageDimension = ImageDimension(IMAGE_WIDTH.toFloat(), IMAGE_HEIGHT.toFloat())
    }
    private val remoteFileWithoutDimensions = RemoteFile().apply {
        mimeType = "image/jpeg"
        imageDimension = null
    }

    @Test
    fun testAreImageDimensionsSameWhenLocalFileIsNotImage_ShouldReturnTrue() {
        // local not an image
        assertTrue(
            "comparing with non-image",
            remoteFileWithDimensions.areImageDimensionsSame(getPath(TEXT_FILENAME).toString())
        )
    }

    @Test
    fun testAreImageDimensionsSame_ShouldReturnTrue() {
        // same dimensions
        assertTrue(
            "comparing with same-size image",
            remoteFileWithDimensions.areImageDimensionsSame(getPath(IMAGE_FILENAME).toString())
        )
    }

    @Test
    fun testAreImageDimensionsSame_ShouldReturnFalse() {
        // different dimensions
        val biggerRemoteImage = RemoteFile().apply {
            mimeType = "image/jpeg"
            imageDimension = ImageDimension((IMAGE_WIDTH * 2).toFloat(), IMAGE_HEIGHT.toFloat())
        }
        assertFalse(
            "comparing with different-size image",
            biggerRemoteImage.areImageDimensionsSame(getPath(IMAGE_FILENAME).toString())
        )
    }

    @Test
    fun testAreImageDimensionsSameWhenLocalFileHasNoDimensions_ShouldReturnTrue() {
        // local file with null dimensions
        assertTrue(
            "comparing null-dimensions local file with valid remote file",
            remoteFileWithoutDimensions.areImageDimensionsSame(getPath(TEXT_FILENAME).toString())
        )
    }

    @Test
    fun testAreImageDimensionsSameWhenRemoteFileHasNoDimensions_ShouldReturnTrue() {
        // remote file with null dimensions
        assertTrue(
            "comparing with null-dimensions remote file",
            remoteFileWithoutDimensions.areImageDimensionsSame(getPath(IMAGE_FILENAME).toString())
        )
    }

    @Test
    fun testAreImageDimensionsSameWhenBothFilesHaveNoDimensions_ShouldReturnTrue() {
        // both files with null dimensions
        assertTrue(
            "comparing null-dimensions local file with null-dimensions remote file",
            remoteFileWithoutDimensions.areImageDimensionsSame(getPath(TEXT_FILENAME).toString())
        )
    }

    @After
    fun cleanup() {
        val tf = File(getPath(TEXT_FILENAME).toUri())
        val imgf = File(getPath(IMAGE_FILENAME).toUri())
        tf.delete()
        imgf.delete()
    }

    @Before
    fun setup() {
        tempDir = Files.createTempDirectory(TMP_DIR)
        assertTrue("Temp directory should exist", tempDir.exists())

        createImageFile()
        createTextFile()
    }

    private fun getPath(filename: String): Path = tempDir.resolve("test_$filename")

    private fun createImageFile() {
        val bitmap = Bitmap.createBitmap(IMAGE_WIDTH, IMAGE_HEIGHT, Bitmap.Config.ARGB_8888)
        val imagePath = getPath(IMAGE_FILENAME)

        Files.newOutputStream(imagePath).use { out: OutputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }

        assertTrue(imagePath.exists())
    }

    private fun createTextFile() {
        val f = File(getPath(TEXT_FILENAME).toUri())
        f.writeText("Lorem ipsum")
    }

    companion object {
        val TMP_DIR = "remoteFileExtensionsTest"
        val TEXT_FILENAME = "text.txt"
        val IMAGE_FILENAME = "image.jpg"

        val IMAGE_WIDTH = 123
        val IMAGE_HEIGHT = 45
    }
}
