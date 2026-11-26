/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.settings.storage.migration

import android.content.Context
import androidx.annotation.StringRes
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManagerImpl
import com.nextcloud.utils.extensions.canonicalPathOrAbsolute
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.activity.settings.storage.migration.model.MigrationException
import com.owncloud.android.utils.FileStorageUtils
import java.io.File
import java.io.IOException
import java.nio.file.Files

@Suppress("TooManyFunctions")
class DataFolderMover(
    private val context: Context,
    private val user: User,
    private val sourceStoragePath: String,
    private val targetStoragePath: String
) {

    private val sourceDataFolder = File(sourceStoragePath, MainApp.dataFolder)
    private val targetDataFolder = File(targetStoragePath, MainApp.dataFolder)
    private var indexUpdateStarted = false

    fun targetDataFolderExists(): Boolean = targetDataFolder.isDirectory

    fun dataFoldersOverlap(): Boolean {
        val source = sourceDataFolder.canonicalPathOrAbsolute
        val target = targetDataFolder.canonicalPathOrAbsolute

        return source == target ||
            source.startsWith(target + File.separator) ||
            target.startsWith(source + File.separator)
    }

    fun prepare() {
        deleteRecursive(targetDataFolder)

        try {
            Files.createDirectories(sourceDataFolder.toPath())
        } catch (e: IOException) {
            Log_OC.e(TAG, "Could not create directory: " + sourceDataFolder.absolutePath, e)
        }
    }

    fun checkDestination() {
        val sourceRoot = File(sourceStoragePath)
        val targetRoot = File(targetStoragePath)

        if (!targetRoot.canRead() || !targetRoot.canWrite()) failWith(R.string.file_migration_failed_not_writable)
        if (!sourceRoot.canRead()) failWith(R.string.file_migration_failed_not_readable)
        if (targetDataFolder.exists()) failWith(R.string.file_migration_failed_dir_already_exists)
        if (targetRoot.freeSpace < FileStorageUtils.getFolderSize(sourceDataFolder)) {
            failWith(R.string.file_migration_failed_not_enough_space)
        }
    }

    fun copyFiles() {
        copyFolder(sourceDataFolder, targetDataFolder)
    }

    @Suppress("TooGenericExceptionCaught")
    fun updateIndex() {
        indexUpdateStarted = true
        val fileStorageManager = FileDataStorageManager(user, context.contentResolver)
        val uploadsStorageManager =
            UploadsStorageManager(UserAccountManagerImpl.fromContext(context), context.contentResolver)
        val targetPrefix = dataFolderPrefix(targetStoragePath)
        val sourcePrefixes = linkedSetOf(
            dataFolderPrefix(sourceStoragePath),
            dataFolderPrefix(File(sourceStoragePath).canonicalPathOrAbsolute)
        )

        try {
            sourcePrefixes.forEach { sourcePrefix ->
                fileStorageManager.migrateStoredFiles(sourcePrefix, targetPrefix)
                updateUploadPaths(uploadsStorageManager, sourcePrefix, targetPrefix)
            }
        } catch (e: Exception) {
            Log_OC.e(TAG, e.message, e)
            failWith(R.string.file_migration_failed_while_updating_index)
        }
    }

    fun deleteSource() {
        if (!deleteRecursive(sourceDataFolder) && sourceDataFolder.exists()) {
            Log_OC.w(TAG, "Could not delete old data folder: " + sourceDataFolder.absolutePath)
        }
    }

    fun rollback() {
        if (indexUpdateStarted) {
            Log_OC.w(TAG, "Index update failed, keeping copied data: " + targetDataFolder.absolutePath)
            return
        }

        if (!deleteRecursive(targetDataFolder) && targetDataFolder.exists()) {
            Log_OC.w(TAG, "Rollback failed: " + targetDataFolder.absolutePath)
        }
    }

    private fun copyFolder(source: File, target: File) {
        if (!target.mkdirs()) failWith(R.string.file_migration_failed_while_coping)

        val files = source.listFiles() ?: failWith(R.string.file_migration_failed_while_coping)

        files.forEach { file ->
            val targetFile = File(target, file.name)
            if (file.isDirectory) {
                copyFolder(file, targetFile)
                return@forEach
            }

            if (!FileStorageUtils.copyFile(file, targetFile)) failWith(R.string.file_migration_failed_while_coping)

            if (!targetFile.setLastModified(file.lastModified())) {
                Log_OC.w(TAG, "Could not keep modification time of " + targetFile.absolutePath)
            }
        }
    }

    private fun updateUploadPaths(
        uploadsStorageManager: UploadsStorageManager,
        sourcePrefix: String,
        targetPrefix: String
    ) {
        uploadsStorageManager.getAllStoredUploads().forEach { upload ->
            val localPath = upload.localPath.orEmpty()
            if (!localPath.startsWith(sourcePrefix)) return@forEach

            upload.localPath = targetPrefix + localPath.substring(sourcePrefix.length)
            uploadsStorageManager.updateUpload(upload)
        }
    }

    private fun deleteRecursive(file: File): Boolean {
        val children = if (file.isDirectory) file.listFiles().orEmpty() else emptyArray()
        val childrenDeleted = children.fold(true) { deleted, child -> deleteRecursive(child) && deleted }

        return file.delete() && childrenDeleted
    }

    private fun dataFolderPrefix(storagePath: String): String =
        storagePath + File.separator + MainApp.dataFolder + File.separator

    private fun failWith(@StringRes messageResId: Int): Nothing = throw MigrationException(messageResId)

    companion object {
        private const val TAG = "DataFolderMover"
    }
}
