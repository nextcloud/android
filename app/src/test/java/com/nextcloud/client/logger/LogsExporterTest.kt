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
import kotlinx.coroutines.test.runTest
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.util.Date
import java.util.TimeZone

// Android storage APIs are mocked to exercise both paths on the host JVM.
@SuppressLint("NewApi")
class LogsExporterTest {
    @get:Rule
    val directory = TemporaryFolder()

    private val resolver: ContentResolver = mock()
    private val context: Context = mock {
        on { contentResolver }.thenReturn(resolver)
    }
    private val clock: Clock = mock {
        on { tz }.thenReturn(TimeZone.getTimeZone("UTC"))
        on { currentDate }.thenReturn(Date(0))
    }
    private val repository: LogsRepository = mock()
    private val exporter = LogsExporter(context, clock, repository)
    private val uri: Uri = mock()
    private val entries = listOf(LogEntry(Date(0), Level.ERROR, "test", "Failed to verify public key: 验证失败"))

    @Test
    fun doesNotCreateAnEmptyExport() = runTest {
        whenever(repository.load(any())).thenAnswer {
            it.getArgument<OnLogsLoaded>(0).invoke(emptyList(), 0)
        }

        assertFalse(exporter.export())
        verify(resolver, never()).insert(anyOrNull(), any())
    }

    @Test
    fun publishesUtf8LogsInNextcloudDownloadsDirectoryAfterWriting() = runTest {
        mockConstruction(ContentValues::class.java).use { values ->
            val output = ByteArrayOutputStream()
            whenever(resolver.insert(anyOrNull(), any())).thenReturn(uri)
            whenever(resolver.openOutputStream(uri, "w")).thenReturn(output)
            whenever(resolver.update(eq(uri), any(), anyOrNull(), anyOrNull())).thenReturn(1)

            exporter.writeToMediaStore("logs.txt", entries)

            assertEquals(entries.single().toString(clock.tz) + "\n", output.toString(Charsets.UTF_8))
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
    fun reportsFailureWhenMediaStoreCannotCreateAFile() = runTest {
        val result = runCatching { exporter.writeToMediaStore("logs.txt", entries) }

        assertTrue(result.exceptionOrNull() is IOException)
        verify(resolver, never()).openOutputStream(any(), any())
    }

    @Test
    fun removesPendingFileWhenOutputStreamIsUnavailable() = runTest {
        whenever(resolver.insert(anyOrNull(), any())).thenReturn(uri)

        val result = runCatching { exporter.writeToMediaStore("logs.txt", entries) }

        assertTrue(result.exceptionOrNull() is IOException)
        verify(resolver).delete(uri, null, null)
        verify(resolver, never()).update(any(), any(), anyOrNull(), anyOrNull())
    }

    @Test
    fun removesIncompleteFileWhenWritingFails() = runTest {
        val output: OutputStream = mock()
        whenever(resolver.insert(anyOrNull(), any())).thenReturn(uri)
        whenever(resolver.openOutputStream(uri, "w")).thenReturn(output)
        doThrow(IOException("Disk full")).whenever(output).write(any<ByteArray>(), any(), any())

        val result = runCatching { exporter.writeToMediaStore("logs.txt", entries) }

        assertTrue(result.exceptionOrNull() is IOException)
        verify(resolver).delete(uri, null, null)
        verify(resolver, never()).update(any(), any(), anyOrNull(), anyOrNull())
    }

    @Test
    fun removesPendingFileWhenPublishingFails() = runTest {
        whenever(resolver.insert(anyOrNull(), any())).thenReturn(uri)
        whenever(resolver.openOutputStream(uri, "w")).thenReturn(ByteArrayOutputStream())

        val result = runCatching { exporter.writeToMediaStore("logs.txt", entries) }

        assertTrue(result.exceptionOrNull() is IOException)
        verify(resolver).delete(uri, null, null)
    }

    @Test
    fun legacyExportsUseNextcloudDirectoryWithoutOverwritingExistingLogs() = runTest {
        mockStatic(Environment::class.java).use { environment ->
            environment.`when`<File> {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            }.thenReturn(directory.root)

            exporter.writeToPublicDirectory("logs.txt", entries)
            exporter.writeToPublicDirectory("logs.txt", entries)

            val files = File(directory.root, "Nextcloud").listFiles().orEmpty()
            assertEquals(2, files.size)
            files.forEach { assertEquals(entries.single().toString(clock.tz) + "\n", it.readText(Charsets.UTF_8)) }
        }
    }

    @Test
    fun legacyExportReportsFailureWhenDestinationCannotBeCreated() = runTest {
        mockStatic(Environment::class.java).use { environment ->
            environment.`when`<File> {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            }.thenReturn(directory.root)
            val existing = directory.newFile("Nextcloud").apply { writeText("existing data") }

            val result = runCatching { exporter.writeToPublicDirectory("logs.txt", entries) }

            assertTrue(result.exceptionOrNull() is IOException)
            assertEquals("existing data", existing.readText())
        }
    }
}
