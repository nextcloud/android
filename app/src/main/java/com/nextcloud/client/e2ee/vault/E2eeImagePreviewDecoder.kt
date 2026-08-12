/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.e2ee.vault

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import com.nextcloud.utils.rotateBitmapViaExif
import java.io.ByteArrayInputStream

class E2eeImagePreviewDecoder : E2eeImageDecoder {
    override fun decode(bytes: ByteArray, requestedWidth: Int, requestedHeight: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

        return if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            null
        } else {
            decodeSampledBitmap(bytes, bounds, requestedWidth, requestedHeight)
        }
    }

    private fun decodeSampledBitmap(
        bytes: ByteArray,
        bounds: BitmapFactory.Options,
        requestedWidth: Int,
        requestedHeight: Int
    ): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateSampleSize(bounds, requestedWidth, requestedHeight)
        }

        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.let { bitmap ->
            bitmap.rotateBitmapViaExif(readOrientation(bytes))
        }
    }

    private fun calculateSampleSize(options: BitmapFactory.Options, requestedWidth: Int, requestedHeight: Int): Int {
        var sampleSize = 1

        while (options.outWidth / sampleSize > requestedWidth * SAMPLE_FACTOR ||
            options.outHeight / sampleSize > requestedHeight * SAMPLE_FACTOR
        ) {
            sampleSize *= SAMPLE_FACTOR
        }

        return sampleSize
    }

    private fun readOrientation(bytes: ByteArray): Int = runCatching {
        ExifInterface(ByteArrayInputStream(bytes)).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

    companion object {
        private const val SAMPLE_FACTOR = 2
    }
}
