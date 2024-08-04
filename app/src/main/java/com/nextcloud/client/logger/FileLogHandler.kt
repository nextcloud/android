/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
