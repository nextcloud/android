/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.logger

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.nextcloud.client.core.Clock
import com.nextcloud.client.core.ManualAsyncRunner
import com.nextcloud.client.logger.ui.LogsEmailSender
import com.owncloud.android.R
import com.owncloud.android.utils.FileExportUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito.mockConstruction
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.util.Date
import java.util.TimeZone
import java.util.function.IntConsumer

// Android storage APIs are mocked to exercise both paths on the host JVM.
@SuppressLint("NewApi")
class LogsExportTest {
    @get:Rule
    val directory = TemporaryFolder()

    private val resolver: ContentResolver = mock()
    private val context: Context = mock {
        on { contentResolver }.thenReturn(resolver)
        on { cacheDir }.thenAnswer { File(directory.root, "cache").apply { mkdirs() } }
    }
    private val clock: Clock = mock {
        on { tz }.thenReturn(TimeZone.getTimeZone("UTC"))
    }
    private val runner = ManualAsyncRunner()
    private val sender = LogsEmailSender(context, clock, runner)
    private val onComplete: IntConsumer = mock()
    private val exporter = FileExportUtils()
    private val uri: Uri = mock()
    private val entries = listOf(LogEntry(Date(0), Level.ERROR, "test", "Failed to verify public key: 验证失败"))
    private val content get() = entries.single().toString(clock.tz) + System.lineSeparator()
    private val source by lazy { directory.newFile("source.txt").apply { writeText(content, Charsets.UTF_8) } }

    @Test
    fun doesNotCreateAnEmptyExport() {
        sender.export(emptyList(), onComplete)

        assertTrue(runner.isEmpty)
        verify(onComplete).accept(R.string.logs_export_to_downloads_empty)
    }

    @Test
    fun publishesUtf8LogsInNextcloudDownloadsDirectoryAfterWriting() {
        mockConstruction(ContentValues::class.java).use { values ->
            val output = ByteArrayOutputStream()
            whenever(resolver.insert(anyOrNull(), any())).thenReturn(uri)
            whenever(resolver.openOutputStream(uri, "w")).thenReturn(output)
            whenever(resolver.update(eq(uri), any(), anyOrNull(), anyOrNull())).thenReturn(1)

            exportToMediaStore()

            assertEquals(content, output.toString(Charsets.UTF_8))
            verify(values.constructed()[0]).put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                "${Environment.DIRECTORY_DOWNLOADS}/Nextcloud"
            )
            verify(values.constructed()[0]).put(MediaStore.MediaColumns.IS_PENDING, 1)
            verify(values.constructed()[1]).put(MediaStore.MediaColumns.IS_PENDING, 0)
            verify(resolver, never()).delete(any(), anyOrNull(), anyOrNull())
        }
    }

    @Test
    fun reportsFailureWhenMediaStoreCannotCreateAFile() {
        assertTrue(runCatching { exportToMediaStore() }.exceptionOrNull() is IOException)
        verify(resolver, never()).openOutputStream(any(), any())
    }

    @Test
    fun removesPendingFileWhenOutputStreamIsUnavailable() {
        whenever(resolver.insert(anyOrNull(), any())).thenReturn(uri)

        assertTrue(runCatching { exportToMediaStore() }.exceptionOrNull() is IOException)
        verify(resolver).delete(uri, null, null)
        verify(resolver, never()).update(any(), any(), anyOrNull(), anyOrNull())
    }

    @Test
    fun removesIncompleteFileWhenWritingFails() {
        val output: OutputStream = mock()
        whenever(resolver.insert(anyOrNull(), any())).thenReturn(uri)
        whenever(resolver.openOutputStream(uri, "w")).thenReturn(output)
        doThrow(IOException("Disk full")).whenever(output).write(any<ByteArray>(), any(), any())

        assertTrue(runCatching { exportToMediaStore() }.exceptionOrNull() is IOException)
        verify(resolver).delete(uri, null, null)
        verify(resolver, never()).update(any(), any(), anyOrNull(), anyOrNull())
        verify(output).close()
    }

    @Test
    fun removesPendingFileWhenPublishingFails() {
        whenever(resolver.insert(anyOrNull(), any())).thenReturn(uri)
        whenever(resolver.openOutputStream(uri, "w")).thenReturn(ByteArrayOutputStream())

        assertTrue(runCatching { exportToMediaStore() }.exceptionOrNull() is IOException)
        verify(resolver).delete(uri, null, null)
    }

    @Test
    fun settingsAndLogViewerShareBackgroundExportWithoutOverwritingExistingLogs() = withDownloads {
        sender.export(entries, onComplete)
        sender.export(entries, onComplete)
        assertEquals(1, runner.size)
        assertFalse(File(directory.root, "Nextcloud").exists())
        runner.runAll()
        sender.export(entries, onComplete)
        runner.runAll()

        val files = File(directory.root, "Nextcloud").listFiles().orEmpty()
        assertEquals(2, files.size)
        files.forEach { assertEquals(content, it.readText(Charsets.UTF_8)) }
        verify(onComplete, times(2)).accept(R.string.logs_export_to_downloads_success)
        assertTrue(context.cacheDir.listFiles().isNullOrEmpty())
    }

    @Test
    fun reportsFailureWithoutOverwritingAnExistingDestination() = withDownloads {
        val existing = directory.newFile("Nextcloud").apply { writeText("existing data") }

        sender.export(entries, onComplete)
        runner.runAll()

        verify(onComplete).accept(R.string.logs_export_to_downloads_failed)
        verify(onComplete, never()).accept(R.string.logs_export_to_downloads_success)
        assertEquals("existing data", existing.readText())
    }

    @Test
    fun reportsSerializationFailureAndAllowsRetry() {
        directory.newFile("cache")
        sender.export(entries, onComplete)
        runner.runAll()
        verify(onComplete).accept(R.string.logs_export_to_downloads_failed)

        sender.export(entries, onComplete)
        assertEquals(1, runner.size)
    }

    @Test
    fun stoppingCancelsQueuedExport() {
        sender.export(entries, onComplete)
        sender.stop()

        assertTrue(runner.isEmpty)
        verify(onComplete, never()).accept(any())
    }

    @Test
    fun otherExportsKeepTheDownloadsRootAndCleanUpFailedWrites() = withDownloads {
        assertTrue(exporter.exportFile("other.txt", "text/plain", resolver, null, source))
        assertEquals(content, File(directory.root, "other.txt").readText(Charsets.UTF_8))
        assertFalse(exporter.exportFile("missing.txt", "text/plain", resolver, null, File(directory.root, "no-source")))
        assertFalse(File(directory.root, "missing.txt").exists())
    }

    private fun exportToMediaStore() =
        exporter.exportFileAndroid10AndAbove("logs.txt", "text/plain", resolver, null, source, "Nextcloud")

    private fun withDownloads(block: () -> Unit) {
        mockStatic(Environment::class.java).use { environment ->
            environment.`when`<File> {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            }.thenReturn(directory.root)
            block()
        }
    }
}
