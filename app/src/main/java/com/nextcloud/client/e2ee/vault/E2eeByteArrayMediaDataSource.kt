/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.e2ee.vault

import android.media.MediaDataSource
import kotlin.math.min

class E2eeByteArrayMediaDataSource(private val bytes: ByteArray) : MediaDataSource() {
    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
        if (position < 0 || position > Int.MAX_VALUE || position >= bytes.size) {
            return END_OF_STREAM
        }

        if (offset < 0 || offset > buffer.size || size < 0) {
            return END_OF_STREAM
        }

        val requestedLength = min(size, buffer.size - offset)
        if (requestedLength == 0) {
            return 0
        }

        val start = position.toInt()
        val length = min(requestedLength, bytes.size - start)
        bytes.copyInto(buffer, offset, start, start + length)
        return length
    }

    override fun getSize(): Long = bytes.size.toLong()

    override fun close() = Unit

    companion object {
        private const val END_OF_STREAM = -1
    }
}
