/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.utils.headers

sealed interface CustomHeadersParseResult {
    data class Valid(val headers: Map<String, String>) : CustomHeadersParseResult

    data class Invalid(val lineNumber: Int) : CustomHeadersParseResult
}
