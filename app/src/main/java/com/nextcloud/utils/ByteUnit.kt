/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils

enum class ByteUnit(val suffix: String, val decimals: Int) {
    BYTES("B", 0),
    KILOBYTES("KB", 0),
    MEGABYTES("MB", 1),
    GIGABYTES("GB", 1),
    TERABYTES("TB", 1),
    PETABYTES("PB", 2),
    EXABYTES("EB", 2)
}
