/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.strings

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.nextcloud.client.jobs.upload.FileUploadHelper.Companion.MAX_FILE_COUNT
import com.owncloud.android.R
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class StringsTests {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun testMaxFileCountText() {
        val message = context.resources.getQuantityString(
            R.plurals.file_upload_limit_message,
            MAX_FILE_COUNT,
            MAX_FILE_COUNT
        )

        assertEquals(message, "You can upload up to 500 files at once.")
    }
}
