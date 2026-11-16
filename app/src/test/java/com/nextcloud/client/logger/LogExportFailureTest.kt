/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.logger

import android.content.ContentResolver
import android.os.Environment
import com.owncloud.android.utils.FileExportUtils
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito.mockConstruction
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class LogExportFailureTest {
    @get:Rule
    val directory = TemporaryFolder()

    private val resolver: ContentResolver = mock()
    private val exporter = FileExportUtils("Nextcloud")

    @Test
    fun exportedBytesAndSizeMatchTheSerializedLog() = withDownloads {
        val source = directory.newFile("source.txt").apply { writeText("ERROR: key verification failed 验证失败\n") }

        val fileName = exporter.exportFile("logs.txt", "text/plain", resolver, null, source)

        val exported = File(directory.root, "Nextcloud/logs.txt")
        assertEquals(exported.name, fileName)
        assertArrayEquals(source.readBytes(), exported.readBytes())
        assertEquals(source.length(), exported.length())
    }

    @Test
    fun duplicateNameReturnsActualSavedNameAndPreservesBothExports() = withDownloads {
        val source = directory.newFile("source.txt").apply { writeText("first export\n") }
        exporter.exportFile("logs.txt", "text/plain", resolver, null, source)
        source.writeText("second export\n")

        val fileName = exporter.exportFile("logs.txt", "text/plain", resolver, null, source)

        assertEquals("logs (2).txt", fileName)
        assertEquals("first export\n", File(directory.root, "Nextcloud/logs.txt").readText())
        assertEquals("second export\n", File(directory.root, "Nextcloud/$fileName").readText())
    }

    @Test
    fun missingSourceIsReportedAsFailure() = withDownloads {
        assertThrows(IOException::class.java) {
            exporter.exportFile("logs.txt", "text/plain", resolver, null, File(directory.root, "missing"))
        }
    }

    @Test
    fun diskWriteErrorIsReportedAndOutputIsClosed() = withDownloads {
        val source = directory.newFile("source.txt").apply { writeText("ERROR: verification failed\n") }
        mockConstruction(FileOutputStream::class.java) { output, _ ->
            doThrow(IOException("Disk full")).whenever(output).write(any<ByteArray>(), any(), any())
        }.use { outputs ->
            val error = assertThrows(IOException::class.java) {
                exporter.exportFile("logs.txt", "text/plain", resolver, null, source)
            }
            assertEquals("Disk full", error.message)
            verify(outputs.constructed().single()).close()
        }
    }

    private fun withDownloads(block: () -> Unit) {
        mockStatic(Environment::class.java).use { environment ->
            environment.`when`<File> {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            }.thenReturn(directory.root)
            block()
        }
    }
}
