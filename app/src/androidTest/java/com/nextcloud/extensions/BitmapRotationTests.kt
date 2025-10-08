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
import com.nextcloud.utils.rotateBitmapViaExif
import junit.framework.TestCase.assertEquals
import org.junit.Test

class BitmapRotationTests {

    private fun createTestBitmap(): Bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).apply {
        setPixel(0, 0, Color.RED)
        setPixel(1, 0, Color.GREEN)
        setPixel(0, 1, Color.BLUE)
        setPixel(1, 1, Color.YELLOW)
    }

    @Test
    fun testRotateBitmapViaExifWhenGivenNullBitmapShouldReturnNull() {
        val rotated = null.rotateBitmapViaExif(ExifInterface.ORIENTATION_ROTATE_90)
        assertEquals(null, rotated)
    }

    @Test
    fun testRotateBitmapViaExifWhenGivenNormalOrientationShouldReturnSameBitmap() {
        val bmp = createTestBitmap()
        val rotated = bmp.rotateBitmapViaExif(ExifInterface.ORIENTATION_NORMAL)
        assertEquals(bmp, rotated)
    }

    @Test
    fun testRotateBitmapViaExifWhenGivenRotate90ShouldReturnRotatedBitmap() {
        val bmp = createTestBitmap()
        val rotated = bmp.rotateBitmapViaExif(ExifInterface.ORIENTATION_ROTATE_90)!!
        assertEquals(bmp.width, rotated.height)
        assertEquals(bmp.height, rotated.width)

        assertEquals(Color.BLUE, rotated.getPixel(0, 0))
        assertEquals(Color.RED, rotated.getPixel(1, 0))
        assertEquals(Color.YELLOW, rotated.getPixel(0, 1))
        assertEquals(Color.GREEN, rotated.getPixel(1, 1))
    }

    @Test
    fun testRotateBitmapViaExifWhenGivenRotate180ShouldReturnRotatedBitmap() {
        val bmp = createTestBitmap()
        val rotated = bmp.rotateBitmapViaExif(ExifInterface.ORIENTATION_ROTATE_180)!!
        assertEquals(bmp.width, rotated.width)
        assertEquals(bmp.height, rotated.height)

        assertEquals(Color.YELLOW, rotated.getPixel(0, 0))
        assertEquals(Color.BLUE, rotated.getPixel(1, 0))
        assertEquals(Color.GREEN, rotated.getPixel(0, 1))
        assertEquals(Color.RED, rotated.getPixel(1, 1))
    }

    @Test
    fun testRotateBitmapViaExifWhenGivenFlipHorizontalShouldReturnFlippedBitmap() {
        val bmp = createTestBitmap()
        val rotated = bmp.rotateBitmapViaExif(ExifInterface.ORIENTATION_FLIP_HORIZONTAL)!!
        assertEquals(bmp.width, rotated.width)
        assertEquals(bmp.height, rotated.height)

        assertEquals(Color.GREEN, rotated.getPixel(0, 0))
        assertEquals(Color.RED, rotated.getPixel(1, 0))
        assertEquals(Color.YELLOW, rotated.getPixel(0, 1))
        assertEquals(Color.BLUE, rotated.getPixel(1, 1))
    }

    @Test
    fun testRotateBitmapViaExifWhenGivenFlipVerticalShouldReturnFlippedBitmap() {
        val bmp = createTestBitmap()
        val rotated = bmp.rotateBitmapViaExif(ExifInterface.ORIENTATION_FLIP_VERTICAL)!!
        assertEquals(bmp.width, rotated.width)
        assertEquals(bmp.height, rotated.height)

        assertEquals(Color.BLUE, rotated.getPixel(0, 0))
        assertEquals(Color.YELLOW, rotated.getPixel(1, 0))
        assertEquals(Color.RED, rotated.getPixel(0, 1))
        assertEquals(Color.GREEN, rotated.getPixel(1, 1))
    }
}
