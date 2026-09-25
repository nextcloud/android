/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils

import android.content.res.Resources
import androidx.annotation.RawRes
import com.owncloud.android.lib.common.utils.Log_OC
import java.io.IOException

object RawResourceReader {

    private const val TAG = "RawResourceReader"

    @JvmStatic
    fun readText(resources: Resources, @RawRes id: Int): String = try {
        resources.openRawResource(id).bufferedReader().use { it.readText() }
    } catch (e: IOException) {
        Log_OC.e(TAG, "Failed to read raw resource", e)
        ""
    }
}
