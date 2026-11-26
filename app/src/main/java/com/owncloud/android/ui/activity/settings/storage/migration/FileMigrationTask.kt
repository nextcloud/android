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
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.activity.settings.storage.migration.StorageMigration.Companion.dataFolderOf
import com.owncloud.android.ui.activity.settings.storage.migration.StorageMigration.Companion.dataFolderPrefix
import com.owncloud.android.ui.activity.settings.storage.migration.model.MigrationException
import com.owncloud.android.ui.activity.settings.storage.migration.model.MigrationResult
import com.owncloud.android.utils.FileStorageUtils
import java.io.File
import java.io.IOException
import java.nio.file.Files

@Suppress("TooManyFunctions")
class FileMigrationTask(
    private val context: Context,
    private val user: User,
    private val sourceStoragePath: String,
    private val targetStoragePath: String,
    private val accountSyncPauser: AccountSyncPauser,
    private val onProgress: suspend (Int) -> Unit
) {

    private val sourceDataFolder = dataFolderOf(sourceStoragePath)
    private val targetDataFolder = dataFolderOf(targetStoragePath)
    private var indexUpdateStarted = false

    suspend fun migrate(): MigrationResult {
        onProgress(R.string.file_migration_preparing)

        return try {
            deleteRecursive(targetDataFolder)
            createSourceDataFolder()

            onProgress(R.string.file_migration_checking_destination)
            checkDestinationAvailability()

            accountSyncPauser.runPaused(onProgress) {
                onProgress(R.string.file_migration_migrating)
                copyDirs(sourceDataFolder, targetDataFolder)

                onProgress(R.string.file_migration_updating_index)
                updateIndex()

                onProgress(R.string.file_migration_cleaning)
                cleanup()
            }

            onProgress(R.string.file_migration_ok_finished)
            MigrationResult.Success
        } catch (e: MigrationException) {
            rollback()
            MigrationResult.Failure(e.messageResId)
        }
    }

    private fun createSourceDataFolder() {
        try {
            Files.createDirectories(sourceDataFolder.toPath())
        } catch (e: IOException) {
            Log_OC.e(TAG, "Could not create directory: " + sourceDataFolder.absolutePath, e)
        }
    }

    private fun checkDestinationAvailability() {
        val sourceRoot = File(sourceStoragePath)
        val targetRoot = File(targetStoragePath)

        if (!targetRoot.canRead() || !targetRoot.canWrite()) failWith(R.string.file_migration_failed_not_writable)
        if (!sourceRoot.canRead()) failWith(R.string.file_migration_failed_not_readable)
        if (targetDataFolder.exists()) failWith(R.string.file_migration_failed_dir_already_exists)
        if (targetRoot.freeSpace < FileStorageUtils.getFolderSize(sourceDataFolder)) {
            failWith(R.string.file_migration_failed_not_enough_space)
        }
    }

    private fun copyDirs(source: File, target: File) {
        if (!target.mkdirs()) failWith(R.string.file_migration_failed_while_coping)

        val files = source.listFiles() ?: failWith(R.string.file_migration_failed_while_coping)

        files.forEach { file ->
            val targetFile = File(target, file.name)
            if (file.isDirectory) {
                copyDirs(file, targetFile)
                return@forEach
            }

            if (!FileStorageUtils.copyFile(file, targetFile)) failWith(R.string.file_migration_failed_while_coping)

            if (!targetFile.setLastModified(file.lastModified())) {
                Log_OC.w(TAG, "Could not preserve modification time of " + targetFile.absolutePath)
            }
        }
    }

    private fun failWith(@StringRes messageResId: Int): Nothing = throw MigrationException(messageResId)

    @Suppress("TooGenericExceptionCaught")
    private fun updateIndex() {
        indexUpdateStarted = true
        val fileStorageManager = FileDataStorageManager(user, context.contentResolver)
        val uploadsStorageManager =
            UploadsStorageManager(UserAccountManagerImpl.fromContext(context), context.contentResolver)
        val targetPrefix = dataFolderPrefix(targetStoragePath)

        try {
            sourceDataFolderPrefixes().forEach { sourcePrefix ->
                fileStorageManager.migrateStoredFiles(sourcePrefix, targetPrefix)
                migrateUploadPaths(uploadsStorageManager, sourcePrefix, targetPrefix)
            }
        } catch (e: Exception) {
            Log_OC.e(TAG, e.message, e)
            failWith(R.string.file_migration_failed_while_updating_index)
        }
    }

    private fun sourceDataFolderPrefixes(): Set<String> = linkedSetOf(
        dataFolderPrefix(sourceStoragePath),
        dataFolderPrefix(File(sourceStoragePath).canonicalPathOrAbsolute)
    )

    private fun migrateUploadPaths(
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

    private fun cleanup() {
        if (!deleteRecursive(sourceDataFolder) && sourceDataFolder.exists()) {
            Log_OC.w(TAG, "Migration cleanup step failed: " + sourceDataFolder.absolutePath)
        }
    }

    private fun rollback() {
        if (indexUpdateStarted) {
            Log_OC.w(TAG, "Index update failed, keeping copied data: " + targetDataFolder.absolutePath)
            return
        }

        if (!deleteRecursive(targetDataFolder) && targetDataFolder.exists()) {
            Log_OC.w(TAG, "Rollback step failed: " + targetDataFolder.absolutePath)
        }
    }

    private fun deleteRecursive(file: File): Boolean {
        val children = if (file.isDirectory) file.listFiles().orEmpty() else emptyArray()
        val childrenDeleted = children.fold(true) { deleted, child -> deleteRecursive(child) && deleted }

        return file.delete() && childrenDeleted
    }

    companion object {
        private const val TAG = "FileMigrationTask"
    }
}
