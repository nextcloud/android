/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.logger

import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

/**
 * Very simple log writer with file rotations.
 *
 * Files are rotated when writing entry causes log file to exceed it's maximum size.
 * Last entry is not truncated and final log file can exceed max file size, but
 * no further entries will be written to it.
 */
internal class FileLogHandler(private val logDir: File, private val logFilename: String, private val maxSize: Long) {

    data class RawLogs(val lines: List<String>, val logSize: Long)

    private var writer: FileOutputStream? = null
    private var size: Long = 0

    private val rotationList: List<String> =
        (ROTATED_LOGS_COUNT - 1 downTo 0).map { "$logFilename.$it" } + logFilename

    val logFile: File = File(logDir, logFilename)

    val isOpened: Boolean
        get() = writer != null

    val maxLogFilesCount: Int = rotationList.size

    fun open() {
        writer = openWriter() ?: run {
            logFile.parentFile?.mkdirs()
            openWriter()
        }
        size = logFile.length()
    }

    fun write(logEntry: String) {
        val currentWriter = writer ?: return
        val rawLogEntry = logEntry.toByteArray(Charsets.UTF_8)
        try {
            currentWriter.write(rawLogEntry)
        } catch (_: IOException) {
            return
        }

        size += rawLogEntry.size
        if (size > maxSize) {
            rotateLogs()
        }
    }

    fun close() {
        val currentWriter = writer
        writer = null
        size = 0L

        try {
            currentWriter?.close()
        } catch (_: IOException) {
        }
    }

    fun deleteAll() {
        rotationList.forEach { File(logDir, it).delete() }
    }

    fun rotateLogs() {
        val wasOpened = isOpened
        if (wasOpened) {
            close()
        }

        shiftLogFiles()

        if (wasOpened) {
            open()
        }
    }

    fun loadLogFiles(rotated: Int = ROTATED_LOGS_COUNT): RawLogs {
        require(rotated >= 0) { "Negative index" }

        val lines = mutableListOf<String>()
        var size = 0L
        rotationList
            .slice(0..minOf(rotated, rotationList.lastIndex))
            .map { File(logDir, it) }
            .filter { it.exists() }
            .forEach { file ->
                val fileLines = file.readLinesOrNull() ?: return@forEach
                lines += fileLines
                size += file.length()
            }

        return RawLogs(lines = lines, logSize = size)
    }

    private fun openWriter(): FileOutputStream? = try {
        FileOutputStream(logFile, true)
    } catch (_: FileNotFoundException) {
        null
    }

    private fun shiftLogFiles() {
        File(logDir, rotationList.first()).delete()
        rotationList.zipWithNext().forEach { (targetName, sourceName) ->
            File(logDir, sourceName).renameTo(File(logDir, targetName))
        }
    }

    private fun File.readLinesOrNull(): List<String>? = try {
        readLines(Charsets.UTF_8)
    } catch (_: IOException) {
        null
    }

    companion object {
        const val ROTATED_LOGS_COUNT = 3
    }
}
