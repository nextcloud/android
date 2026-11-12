/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.autoUpload

import java.io.File
import kotlin.time.Duration.Companion.hours

internal object EmptyFileUploadPolicy {
    private val minimumAgeMillis = 1.hours.inWholeMilliseconds

    fun shouldDefer(file: File, nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (!file.isFile || file.length() != 0L) return false

        val lastModified = file.lastModified()
        return lastModified <= 0L || nowMillis - lastModified < minimumAgeMillis
    }
}
