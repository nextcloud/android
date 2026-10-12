/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.utils.serialization

import kotlinx.serialization.json.Json

object AppJson {
    val instance: Json = Json { ignoreUnknownKeys = true }
}
