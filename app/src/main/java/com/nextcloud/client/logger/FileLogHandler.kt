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
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.Charset

/**
 * Very simple log writer with file rotations.
 *
 * Files are rotated when writing entry causes log file to exceed it's maximum size.
 * Last entry is not truncated and final log file can exceed max file size, but
 * no further entries will be written to it.
 */
internal class FileLogHandler(private val logDir: File, private val logFilename: String, private val maxSize: Long) {

    data class RawLogs(val lines: List<String>, val logSize: Long)

    companion object {
        const val ROTATED_LOGS_COUNT = 3
    }

    private var writer: FileOutputStream? = null
    private var size: Long = 0
    private val rotationList = listOf(
        "$logFilename.2",
        "$logFilename.1",
        "$logFilename.0",
        logFilename
    )

    val logFile: File
        get() {
            return File(logDir, logFilename)
        }

    val isOpened: Boolean
        get() {
            return writer != null
        }

    val maxLogFilesCount get() = rotationList.size

    fun open() {
        try {
            writer = FileOutputStream(logFile, true)
            size = logFile.length()
        } catch (ex: FileNotFoundException) {
            logFile.parentFile.mkdirs()
            writer = FileOutputStream(logFile, true)
            size = logFile.length()
        }
    }

    fun write(logEntry: String) {
        val rawLogEntry = logEntry.toByteArray(Charset.forName("UTF-8"))
        writer?.write(rawLogEntry)
        size += rawLogEntry.size
        if (size > maxSize) {
            rotateLogs()
        }
    }

    fun close() {
        writer?.close()
        writer = null
        size = 0L
    }

    fun deleteAll() {
        rotationList
            .map { File(logDir, it) }
            .forEach { it.delete() }
    }

    fun rotateLogs() {
        val rotatatingOpenedLog = isOpened
        if (rotatatingOpenedLog) {
            close()
        }

        val existingLogFiles = logDir.listFiles().associate { it.name to it }
        existingLogFiles[rotationList.first()]?.delete()

        for (i in 0 until rotationList.size - 1) {
            val nextFile = File(logDir, rotationList[i])
            val previousFile = existingLogFiles[rotationList[i + 1]]
            previousFile?.renameTo(nextFile)
        }

        if (rotatatingOpenedLog) {
            open()
        }
    }

    fun loadLogFiles(rotated: Int = ROTATED_LOGS_COUNT): RawLogs {
        if (rotated < 0) {
            throw IllegalArgumentException("Negative index")
        }
        val allLines = mutableListOf<String>()
        var size = 0L
        for (i in 0..Math.min(rotated, rotationList.size - 1)) {
            val file = File(logDir, rotationList[i])
            if (!file.exists()) continue
            try {
                val lines = file.readLines(Charsets.UTF_8)
                allLines.addAll(lines)
                size += file.length()
            } catch (ex: IOException) {
                // ignore failing file
            }
        }
        return RawLogs(lines = allLines, logSize = size)
    }
}
