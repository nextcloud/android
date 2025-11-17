/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.util

import java.util.Collections

fun <T> List<T>.calculateShift(targetIndex: Int, item: T?): Int {
    val currentIndex = indexOf(item)
    return if (currentIndex >= 0 && currentIndex != targetIndex) {
        (size - currentIndex + targetIndex) % size
    } else {
        0
    }
}

fun <T> List<T>.rotate(shift: Int): List<T> {
    val copy = ArrayList(this)
    Collections.rotate(copy, shift)
    return copy
}
