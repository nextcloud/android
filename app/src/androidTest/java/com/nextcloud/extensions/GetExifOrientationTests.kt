/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.extensions
import android.graphics.Bitmap
import android.graphics.Color
import androidx.exifinterface.media.ExifInterface
import com.nextcloud.utils.extensions.getExifOrientation
import junit.framework.TestCase.assertEquals
import org.junit.After
import org.junit.Test
import java.io.File

class GetExifOrientationTests {

    private val tempFiles = mutableListOf<File>()

    @Suppress("MagicNumber")
    private fun createTempImageFile(): File {
        val file = File.createTempFile("test_image", ".jpg")
        tempFiles.add(file)

        val bmp = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).apply {
            setPixel(0, 0, Color.RED)
            setPixel(1, 0, Color.GREEN)
            setPixel(0, 1, Color.BLUE)
            setPixel(1, 1, Color.YELLOW)
        }

        file.outputStream().use { out ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }

        return file
    }

    @After
    fun cleanup() {
        tempFiles.forEach { it.delete() }
    }

    @Test
    fun testGetExifOrientationWhenExifIsRotate90ShouldReturnRotate90() {
        val file = createTempImageFile()

        val exif = ExifInterface(file.absolutePath)
        exif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
        exif.saveAttributes()

        val orientation = getExifOrientation(file.absolutePath)

        assertEquals(ExifInterface.ORIENTATION_ROTATE_90, orientation)
    }

    @Test
    fun testGetExifOrientationWhenExifIsRotate180ShouldReturnRotate180() {
        val file = createTempImageFile()

        val exif = ExifInterface(file.absolutePath)
        exif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_180.toString())
        exif.saveAttributes()

        val orientation = getExifOrientation(file.absolutePath)
        assertEquals(ExifInterface.ORIENTATION_ROTATE_180, orientation)
    }

    @Test
    fun testGetExifOrientationWhenExifIsUndefinedShouldReturnUndefined() {
        val file = createTempImageFile()

        val orientation = getExifOrientation(file.absolutePath)
        assertEquals(ExifInterface.ORIENTATION_UNDEFINED, orientation)
    }
}
