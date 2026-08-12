/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.e2ee.vault

import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import com.owncloud.android.lib.common.utils.Log_OC
import java.io.IOException

object E2eeVideoCompatibilityLogger {
    fun isRenderable(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) {
            Log_OC.w(TAG, "E2EE video diagnostics skipped: empty plaintext")
            return false
        }

        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(E2eeByteArrayMediaDataSource(bytes))
            isRenderable(extractor)
        } catch (e: IllegalArgumentException) {
            Log_OC.w(TAG, "E2EE video diagnostics failed: ${e.javaClass.simpleName}")
            true
        } catch (e: IOException) {
            Log_OC.w(TAG, "E2EE video diagnostics failed: ${e.javaClass.simpleName}")
            true
        } catch (e: RuntimeException) {
            Log_OC.w(TAG, "E2EE video diagnostics failed: ${e.javaClass.simpleName}")
            true
        } finally {
            extractor.release()
        }
    }

    private fun isRenderable(extractor: MediaExtractor): Boolean {
        val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        var videoTrackCount = 0
        var supportedVideoTrackCount = 0

        for (trackIndex in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(trackIndex)
            val mimeType = format.getString(MediaFormat.KEY_MIME)
            val trackType = mimeType?.substringBefore('/') ?: UNKNOWN_TRACK_TYPE
            val hasDecoder = hasDecoder(codecList, format)

            if (trackType == VIDEO_TRACK_TYPE) {
                videoTrackCount++
                if (hasDecoder != false) {
                    supportedVideoTrackCount++
                }
            }

            Log_OC.i(
                TAG,
                "E2EE video track: type=$trackType mime=$mimeType " +
                    "decoder=${decoderAvailability(hasDecoder)} ${videoSize(format)}"
            )
        }

        if (videoTrackCount == 0) {
            Log_OC.w(TAG, "E2EE video diagnostics found no video track")
            return false
        }

        return supportedVideoTrackCount > 0
    }

    private fun hasDecoder(codecList: MediaCodecList, format: MediaFormat): Boolean? = try {
        codecList.findDecoderForFormat(format) != null
    } catch (e: IllegalArgumentException) {
        null
    }

    private fun decoderAvailability(hasDecoder: Boolean?): String = when (hasDecoder) {
        true -> DECODER_AVAILABLE
        false -> DECODER_MISSING
        null -> DECODER_UNKNOWN
    }

    private fun videoSize(format: MediaFormat): String {
        if (!format.containsKey(MediaFormat.KEY_WIDTH) || !format.containsKey(MediaFormat.KEY_HEIGHT)) {
            return ""
        }

        return "size=${format.getInteger(MediaFormat.KEY_WIDTH)}x${format.getInteger(MediaFormat.KEY_HEIGHT)}"
    }

    private const val TAG = "E2eeVideoCompatibility"
    private const val VIDEO_TRACK_TYPE = "video"
    private const val UNKNOWN_TRACK_TYPE = "unknown"
    private const val DECODER_AVAILABLE = "available"
    private const val DECODER_MISSING = "missing"
    private const val DECODER_UNKNOWN = "unknown"
}
