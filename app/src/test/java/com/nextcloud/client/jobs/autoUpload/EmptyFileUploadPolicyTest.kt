/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.autoUpload

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.io.File
import kotlin.time.Duration.Companion.hours

class EmptyFileUploadPolicyTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val nowMillis = 2.hours.inWholeMilliseconds
    private val oneHourMillis = 1.hours.inWholeMilliseconds

    @Test
    fun recentEmptyFileIsDeferred() {
        val file = emptyFile(nowMillis - oneHourMillis + 1)

        assertTrue(EmptyFileUploadPolicy.shouldDefer(file, nowMillis))
    }

    @Test
    fun emptyFileIsAllowedAtExactlyOneHour() {
        val file = emptyFile(nowMillis - oneHourMillis)

        assertFalse(EmptyFileUploadPolicy.shouldDefer(file, nowMillis))
    }

    @Test
    fun olderEmptyFileIsAllowed() {
        val file = emptyFile(nowMillis - oneHourMillis - 1)

        assertFalse(EmptyFileUploadPolicy.shouldDefer(file, nowMillis))
    }

    @Test
    fun modifyingAnOldEmptyFileRestartsTheGracePeriod() {
        val file = emptyFile(nowMillis - oneHourMillis)
        assertFalse(EmptyFileUploadPolicy.shouldDefer(file, nowMillis))

        assertTrue(file.setLastModified(nowMillis))

        assertTrue(EmptyFileUploadPolicy.shouldDefer(file, nowMillis))
    }

    @Test
    fun recentNonEmptyFileIsAllowed() {
        val file = temporaryFolder.newFile().apply {
            writeText("content")
            assertTrue(setLastModified(nowMillis))
        }

        assertFalse(EmptyFileUploadPolicy.shouldDefer(file, nowMillis))
    }

    @Test
    fun deferredFileIsAllowedAsSoonAsItHasContent() {
        val file = emptyFile(nowMillis)
        assertTrue(EmptyFileUploadPolicy.shouldDefer(file, nowMillis))

        file.writeText("content")
        assertTrue(file.setLastModified(nowMillis))

        assertFalse(EmptyFileUploadPolicy.shouldDefer(file, nowMillis))
    }

    @Test
    fun emptyFileWithFutureModificationTimeIsDeferred() {
        val file = emptyFile(nowMillis + oneHourMillis)

        assertTrue(EmptyFileUploadPolicy.shouldDefer(file, nowMillis))
    }

    @Test
    fun emptyFileWithUnknownModificationTimeIsDeferred() {
        val file = mock<File> {
            on { isFile } doReturn true
            on { length() } doReturn 0L
            on { lastModified() } doReturn 0L
        }

        assertTrue(EmptyFileUploadPolicy.shouldDefer(file, nowMillis))
    }

    @Test
    fun missingFileIsLeftToExistingErrorHandling() {
        val file = File(temporaryFolder.root, "missing.txt")

        assertFalse(EmptyFileUploadPolicy.shouldDefer(file, nowMillis))
    }

    @Test
    fun directoryIsNotTreatedAsAnEmptyFile() {
        val directory = temporaryFolder.newFolder()

        assertFalse(EmptyFileUploadPolicy.shouldDefer(directory, nowMillis))
    }

    private fun emptyFile(lastModified: Long): File = temporaryFolder.newFile().apply {
        assertTrue(setLastModified(lastModified))
    }
}
