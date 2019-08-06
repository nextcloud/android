/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.nextcloud.client.logger

import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TestFileLogHandler {

    private lateinit var logDir: File

    private fun readLogFile(name: String): String {
        val logFile = File(logDir, name)
        val raw = Files.readAllBytes(logFile.toPath())
        return String(raw, Charset.forName("UTF-8"))
    }

    private fun writeLogFile(name: String, content: String) {
        val logFile = File(logDir, name)
        Files.write(logFile.toPath(), content.toByteArray(Charsets.UTF_8))
    }

    @Before
    fun setUp() {
        logDir = Files.createTempDirectory("logger-test-").toFile()
    }

    @Test
    fun `logs dir is created on open`() {
        // GIVEN
        //      logs directory does not exist
        val nonexistingLogsDir = File(logDir, "subdir")
        assertFalse(nonexistingLogsDir.exists())

        // WHEN
        //      file is opened
        val handler = FileLogHandler(nonexistingLogsDir, "log.txt", 1000)
        handler.open()

        // THEN
        //      directory is created
        assertTrue(nonexistingLogsDir.exists())
    }

    @Test
    fun `log test helpers`() {
        val filename = "test.txt"
        val expected = "Hello, world!"
        writeLogFile(filename, expected)
        val readBack = readLogFile(filename)
        assertEquals(expected, readBack)
    }

    @Test
    fun `rotate files`() {
        // GIVEN
        //      log contains files
        writeLogFile("log.txt", "0")
        writeLogFile("log.txt.0", "1")
        writeLogFile("log.txt.1", "2")
        writeLogFile("log.txt.2", "3")

        val writer = FileLogHandler(logDir, "log.txt", 1024)

        // WHEN
        //      files are rotated
        writer.rotateLogs()

        // THEN
        //      last file is removed
        //      all remaining files are advanced by 1 step
        assertFalse(File(logDir, "log.txt").exists())
        assertEquals("0", readLogFile("log.txt.0"))
        assertEquals("1", readLogFile("log.txt.1"))
        assertEquals("2", readLogFile("log.txt.2"))
    }

    @Test
    fun `log file is rotated when crossed max size`() {
        // GIVEN
        //      log file contains 10 bytes
        //      log file limit is 20 bytes
        //      log writer is opened
        writeLogFile("log.txt", "0123456789")
        val writer = FileLogHandler(logDir, "log.txt", 20)
        writer.open()

        // WHEN
        //      writing 2nd log entry of 11 bytes
        writer.write("0123456789!") // 11 bytes

        // THEN
        //      log file is closed and rotated
        val rotatedContent = readLogFile("log.txt.0")
        assertEquals("01234567890123456789!", rotatedContent)
    }

    @Test
    fun `log file is reopened after rotation`() {
        // GIVEN
        //      log file contains 10 bytes
        //      log file limit is 20 bytes
        //      log writer is opened
        writeLogFile("log.txt", "0123456789")
        val writer = FileLogHandler(logDir, "log.txt", 20)
        writer.open()

        // WHEN
        //      writing 2nd log entry of 11 bytes
        //      writing another log entry
        //      closing log
        writer.write("0123456789!") // 11 bytes
        writer.write("Hello!")
        writer.close()

        // THEN
        //      current log contains last entry
        val lastEntry = readLogFile("log.txt")
        assertEquals("Hello!", lastEntry)
    }

    @Test
    fun `load log lines from files`() {
        // GIVEN
        //      multiple log files exist
        //      log files have lines
        writeLogFile("log.txt.2", "line1\nline2\nline3")
        writeLogFile("log.txt.1", "line4\nline5\nline6")
        writeLogFile("log.txt.0", "line7\nline8\nline9")
        writeLogFile("log.txt", "line10\nline11\nline12")

        // WHEN
        //      log file is read including rotated content
        val writer = FileLogHandler(logDir, "log.txt", 1000)
        val lines = writer.loadLogFiles(3)

        // THEN
        //      all files are loaded
        //      lines are loaded in correct order
        assertEquals(12, lines.size)
        assertEquals(
            listOf(
                "line1", "line2", "line3",
                "line4", "line5", "line6",
                "line7", "line8", "line9",
                "line10", "line11", "line12"
            ),
            lines
        )
    }

    @Test
    fun `load log lines from files with gaps between rotated files`() {
        // GIVEN
        //      multiple log files exist
        //      log files have lines
        //      some rotated files are deleted
        writeLogFile("log.txt", "line1\nline2\nline3")
        writeLogFile("log.txt.2", "line4\nline5\nline6")

        // WHEN
        //      log file is read including rotated content
        val writer = FileLogHandler(logDir, "log.txt", 1000)
        val lines = writer.loadLogFiles(3)

        // THEN
        //      all files are loaded
        assertEquals(6, lines.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `load log lines - negative count is illegal`() {
        // WHEN
        //      requesting negative number of rotated files
        val writer = FileLogHandler(logDir, "log.txt", 1000)
        val lines = writer.loadLogFiles(-1)

        // THEN
        //      illegal argument exception
    }

    @Test
    fun `all log files are deleted`() {
        // GIVEN
        //      log files exist
        val handler = FileLogHandler(logDir, "log.txt", 100)
        for (i in 0 until handler.maxLogFilesCount) {
            handler.rotateLogs()
            handler.open()
            handler.write("new log entry")
            handler.close()
        }
        assertEquals(handler.maxLogFilesCount, logDir.listFiles().size)

        // WHEN
        //      files are deleted
        handler.deleteAll()

        // THEN
        //      all files are deleted
        assertEquals(0, logDir.listFiles().size)
    }
}
