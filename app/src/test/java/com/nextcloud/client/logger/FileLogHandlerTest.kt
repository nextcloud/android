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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files

@Suppress("TooManyFunctions")
class FileLogHandlerTest {

    private companion object {
        const val FILE_SIZE = 1024L
        const val MAX_FILE_SIZE = 20L
        const val THREE_LOG_FILES = 3
        const val EXPECTED_LINE_COUNT_6 = 6
        const val EXPECTED_LINE_COUNT_12 = 12
    }

    private lateinit var logDir: File

    private fun readLogFile(name: String): String {
        val logFile = File(logDir, name)
        val raw = Files.readAllBytes(logFile.toPath())
        return String(raw, Charset.forName("UTF-8"))
    }

    /**
     * Write raw content to file in log dir.
     *
     * @return size of written data in bytes
     */
    private fun writeLogFile(name: String, content: String): Int {
        val logFile = File(logDir, name)
        val rawContent = content.toByteArray(Charsets.UTF_8)
        Files.write(logFile.toPath(), rawContent)
        return rawContent.size
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
        val handler = FileLogHandler(nonexistingLogsDir, "log.txt", FILE_SIZE)
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

        val writer = FileLogHandler(logDir, "log.txt", FILE_SIZE)

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
        val writer = FileLogHandler(logDir, "log.txt", MAX_FILE_SIZE)
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
        val writer = FileLogHandler(logDir, "log.txt", MAX_FILE_SIZE)
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
        var totalLogsSize = 0L
        totalLogsSize += writeLogFile("log.txt.2", "line1\nline2\nline3")
        totalLogsSize += writeLogFile("log.txt.1", "line4\nline5\nline6")
        totalLogsSize += writeLogFile("log.txt.0", "line7\nline8\nline9")
        totalLogsSize += writeLogFile("log.txt", "line10\nline11\nline12")

        // WHEN
        //      log file is read including rotated content
        val writer = FileLogHandler(logDir, "log.txt", FILE_SIZE)
        val rawLogs = writer.loadLogFiles(THREE_LOG_FILES)

        // THEN
        //      all files are loaded
        //      lines are loaded in correct order
        //      log files size is correctly reported
        assertEquals(EXPECTED_LINE_COUNT_12, rawLogs.lines.size)
        assertEquals(
            listOf(
                "line1", "line2", "line3",
                "line4", "line5", "line6",
                "line7", "line8", "line9",
                "line10", "line11", "line12"
            ),
            rawLogs.lines
        )
        assertEquals(totalLogsSize, rawLogs.logSize)
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
        val writer = FileLogHandler(logDir, "log.txt", FILE_SIZE)
        val lines = writer.loadLogFiles(THREE_LOG_FILES)

        // THEN
        //      all files are loaded
        //      log file size is non-zero
        assertEquals(EXPECTED_LINE_COUNT_6, lines.lines.size)
        assertTrue(lines.logSize > 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `load log lines - negative count is illegal`() {
        // WHEN
        //      requesting negative number of rotated files
        val writer = FileLogHandler(logDir, "log.txt", FILE_SIZE)
        val lines = writer.loadLogFiles(-1)

        // THEN
        //      illegal argument exception
    }

    @Test
    fun `all log files are deleted`() {
        // GIVEN
        //      log files exist
        val handler = FileLogHandler(logDir, "log.txt", MAX_FILE_SIZE)
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
