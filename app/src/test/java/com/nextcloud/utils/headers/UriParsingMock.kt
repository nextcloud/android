/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.utils.headers

import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import java.net.URI

/**
 * Unit tests run against the stubbed android.jar, where [Uri.parse] returns null. Backs it with [URI] instead.
 */
object UriParsingMock {
    fun install() {
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } answers { uriOf(firstArg()) }
    }

    private fun uriOf(text: String): Uri {
        val parsed = URI(text)
        return mockk {
            every { scheme } returns parsed.scheme
            every { host } returns parsed.host
            every { port } returns parsed.port
        }
    }
}
