/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import java.nio.ByteBuffer

@Suppress("MagicNumber")
fun IntArray.toByteArray(): ByteArray {
    val byteBuffer = ByteBuffer.allocate(this.size * 4)
    val intBuffer = byteBuffer.asIntBuffer()
    intBuffer.put(this)
    return byteBuffer.array()
}

@Suppress("MagicNumber")
fun ByteArray.toIntArray(): IntArray {
    val intBuffer = ByteBuffer.wrap(this).asIntBuffer()
    val intArray = IntArray(this.size / 4)
    intBuffer.get(intArray)
    return intArray
}
