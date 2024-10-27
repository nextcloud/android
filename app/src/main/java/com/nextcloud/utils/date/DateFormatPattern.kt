/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.date

enum class DateFormatPattern(val pattern: String) {
    /**
     * e.g. 10.11.2024 - 12:44
     */
    FullDateWithHours("dd.MM.yyyy - HH:mm")
}
