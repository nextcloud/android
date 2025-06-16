/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.database.migrations.model

enum class SQLiteColumnType(val value: String) {
    INTEGER_DEFAULT_NULL("INTEGER DEFAULT NULL"),
    TEXT_DEFAULT_NULL("TEXT DEFAULT NULL")
}
