/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.operations

import com.owncloud.android.lib.common.utils.Log_OC
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.nio.channels.FileLock

object FileLockManager {

    private const val TAG = "FileLockManager"

    @Throws(Exception::class)
    fun lockFile(path: String): Pair<FileChannel?, FileLock?> {

        val file = File(path)
        val channel = RandomAccessFile(file, "rw").channel
        val lock = channel.lock()
        Log_OC.d(TAG, "Locked file: $path")

        return Pair(channel, lock)
    }

    fun unlockFile(channel: FileChannel?, lock: FileLock?) {
        if (lock != null && lock.isValid) {
            lock.release()
            Log_OC.d(TAG, "Released file lock.")
        }

        if (channel != null && channel.isOpen) {
            channel.close()
            Log_OC.d(TAG, "Closed file channel.")
        }
    }
}
