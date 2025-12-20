/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.extensions

import android.graphics.Bitmap
import com.nextcloud.utils.decodeSampledBitmapFromFile
import com.nextcloud.utils.extensions.toFile
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.absolutePathString
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists

@Suppress("MagicNumber")
class BitmapDecodeTests {

    private lateinit var tempDir: Path

    @Before
    fun setup() {
        tempDir = Files.createTempDirectory("auto_upload_test_")
        assertTrue("Temp directory should exist", tempDir.exists())
    }

    @OptIn(ExperimentalPathApi::class)
    @After
    fun cleanup() {
        if (tempDir.exists()) {
            tempDir.deleteRecursively()
        }
    }

    private fun createTempImageFile(width: Int = 100, height: Int = 100): Path {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val imagePath = tempDir.resolve("test_${System.currentTimeMillis()}.jpg")

        Files.newOutputStream(imagePath).use { out: OutputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }

        assertTrue(imagePath.exists())
        return imagePath
    }

    @Test
    fun testToFileWhenPathIsValidShouldReturnExistingFile() {
        val path = createTempImageFile()
        val result = path.absolutePathString().toFile()
        assertNotNull(result)
        assertTrue(result!!.exists())
    }

    @Test
    fun testToFileWhenPathIsEmptyShouldReturnNull() {
        val result = "".toFile()
        assertNull(result)
    }

    @Test
    fun testToFileWhenFileDoesNotExistShouldReturnNull() {
        val nonExistentPath = tempDir.resolve("does_not_exist.jpg")
        val result = nonExistentPath.absolutePathString().toFile()
        assertNull(result)
    }

    @Test
    fun testDecodeSampledBitmapFromFileWhenValidPathShouldReturnBitmap() {
        val path = createTempImageFile(400, 400)
        val bitmap = decodeSampledBitmapFromFile(path.absolutePathString(), 100, 100)
        assertNotNull(bitmap)
        assertTrue(bitmap!!.width <= 400)
        assertTrue(bitmap.height <= 400)
    }

    @Test
    fun testDecodeSampledBitmapFromFileWhenInvalidPathShouldReturnNull() {
        val invalidPath = tempDir.resolve("invalid_path.jpg").absolutePathString()
        val bitmap = decodeSampledBitmapFromFile(invalidPath, 100, 100)
        assertNull(bitmap)
    }

    @Test
    fun testDecodeSampledBitmapFromFileWhenImageIsLargeShouldDownsampleBitmap() {
        val path = createTempImageFile(2000, 2000)
        val bitmap = decodeSampledBitmapFromFile(path.absolutePathString(), 100, 100)
        assertNotNull(bitmap)
        assertTrue("Bitmap should be smaller than original", bitmap!!.width < 2000 && bitmap.height < 2000)
    }

    @Test
    fun testDecodeSampledBitmapFromFileWhenImageIsSmallerThanRequestedShouldKeepOriginalSize() {
        val path = createTempImageFile(100, 100)
        val bitmap = decodeSampledBitmapFromFile(path.absolutePathString(), 200, 200)
        assertNotNull(bitmap)
        assertEquals(100, bitmap!!.width)
        assertEquals(100, bitmap.height)
    }
}
