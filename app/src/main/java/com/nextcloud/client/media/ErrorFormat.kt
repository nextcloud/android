/**
 * Nextcloud Android client application
 *
 * @author David A. Velasco
 * @author masensio
 * @author Chris Narkiewicz
 * Copyright (C) 2013 David A. Velasco
 * Copyright (C) 2016 masensio
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.nextcloud.client.media

import android.content.Context
import android.media.MediaPlayer
import androidx.media3.common.PlaybackException
import com.owncloud.android.R

/**
 * This code has been moved from legacy media player service.
 */
@Deprecated("This legacy helper should be refactored")
@Suppress("ComplexMethod") // it's legacy code
object ErrorFormat {

    /** Error code for specific messages - see regular error codes at [MediaPlayer]  */
    const val OC_MEDIA_ERROR = 0

    @JvmStatic
    fun toString(context: Context?, what: Int, extra: Int): String {
        val messageId: Int

        if (what == OC_MEDIA_ERROR) {
            messageId = extra
        } else if (extra == MediaPlayer.MEDIA_ERROR_UNSUPPORTED) {
            /*  Added in API level 17
                Bitstream is conforming to the related coding standard or file spec,
                but the media framework does not support the feature.
                Constant Value: -1010 (0xfffffc0e)
             */
            messageId = R.string.media_err_unsupported
        } else if (extra == MediaPlayer.MEDIA_ERROR_IO) {
            /*  Added in API level 17
                File or network related operation errors.
                Constant Value: -1004 (0xfffffc14)
             */
            messageId = R.string.media_err_io
        } else if (extra == MediaPlayer.MEDIA_ERROR_MALFORMED) {
            /*  Added in API level 17
                Bitstream is not conforming to the related coding standard or file spec.
                Constant Value: -1007 (0xfffffc11)
             */
            messageId = R.string.media_err_malformed
        } else if (extra == MediaPlayer.MEDIA_ERROR_TIMED_OUT) {
            /*  Added in API level 17
                Some operation takes too long to complete, usually more than 3-5 seconds.
                Constant Value: -110 (0xffffff92)
             */
            messageId = R.string.media_err_timeout
        } else if (what == MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK) {
            /*  Added in API level 3
                The video is streamed and its container is not valid for progressive playback i.e the video's index
                (e.g moov atom) is not at the start of the file.
                Constant Value: 200 (0x000000c8)
             */
            messageId = R.string.media_err_invalid_progressive_playback
        } else {
            /*  MediaPlayer.MEDIA_ERROR_UNKNOWN
                Added in API level 1
                Unspecified media player error.
                Constant Value: 1 (0x00000001)
             */
            /*  MediaPlayer.MEDIA_ERROR_SERVER_DIED)
                Added in API level 1
                Media server died. In this case, the application must release the MediaPlayer
                object and instantiate a new one.
                Constant Value: 100 (0x00000064)
             */
            messageId = R.string.media_err_unknown
        }
        return context?.getString(messageId) ?: "Media error"
    }

    fun toString(context: Context, exception: PlaybackException): String {
        val messageId = when (exception.errorCode) {
            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES -> {
                R.string.media_err_unsupported
            }
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
            PlaybackException.ERROR_CODE_IO_NO_PERMISSION,
            PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED,
            PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE -> {
                R.string.media_err_io
            }
            PlaybackException.ERROR_CODE_TIMEOUT -> {
                R.string.media_err_timeout
            }
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED -> {
                R.string.media_err_malformed
            }
            else -> {
                R.string.media_err_invalid_progressive_playback
            }
        }
        return context.getString(messageId)
    }
}
