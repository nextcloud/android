/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.metadata

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nextcloud.client.account.User
import com.nextcloud.utils.extensions.getNonEncryptedSubfolders
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.RefreshFolderOperation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.LinkedList
import java.util.Queue

@Suppress("DEPRECATION", "ReturnCount", "TooGenericExceptionCaught")
class MetadataWorker(private val context: Context, params: WorkerParameters, private val user: User) :
    CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "MetadataWorker"
        const val FILE_PATH = "file_path"
        const val FORCE_REFRESH = "force_refresh"  // When true, ignore eTag and always fetch content
    }

    override suspend fun doWork(): Result {
        val filePath = inputData.getString(FILE_PATH)
        if (filePath == null) {
            Log_OC.e(TAG, "❌ Invalid folder path. Aborting metadata sync. $filePath")
            return Result.failure()
        }

        // Check if we should force a full refresh (ignore eTag)
        val forceRefresh = inputData.getBoolean(FORCE_REFRESH, false)
        Log_OC.d(TAG, "📥 Force refresh mode: $forceRefresh for path: $filePath")

        if (user.isAnonymous) {
            Log_OC.w(TAG, "user is anonymous cannot start metadata worker")
            return Result.failure()
        }

        val storageManager = FileDataStorageManager(user, context.contentResolver)
        val currentDir = storageManager.getFileByDecryptedRemotePath(filePath)
        if (currentDir == null) {
            Log_OC.e(TAG, "❌ Current directory is null. Aborting metadata sync. $filePath")
            return Result.failure()
        }
        if (!currentDir.hasValidParentId()) {
            Log_OC.e(TAG, "❌ Current directory has invalid ID: ${currentDir.fileId}. Path: $filePath")
            return Result.failure()
        }

        Log_OC.d(TAG, "🕒 Starting metadata sync for folder: $filePath, id: ${currentDir.fileId}")

        // First check current dir
        val currentRefreshResult = refreshFolder(currentDir, storageManager, forceRefresh)
        if (!currentRefreshResult) {
            Log_OC.e(TAG, "❌ Failed to refresh current directory: $filePath")
            return Result.failure()
        }

        // Re-fetch the folder after refresh to get updated data
        val refreshedDir = storageManager.getFileByPath(filePath)
        if (refreshedDir == null || !refreshedDir.hasValidParentId()) {
            Log_OC.e(TAG, "❌ Directory invalid after refresh. Path: $filePath")
            return Result.failure()
        }

        // IMPORTANT: Also fetch immediate files in the current folder, not just subfolders
        // This ensures FolderDownloadWorker has access to files when it runs after MetadataWorker
        val currentFolderFiles = storageManager.getFolderContent(refreshedDir.fileId, false)
            .filter { !it.isFolder }
        Log_OC.d(TAG, "Found ${currentFolderFiles.size} files in current folder: $filePath")

        // Use BFS to recursively process ALL nested subfolders
        // This ensures we fetch metadata for folders at all depth levels
        // (e.g., Artists/ABBA, Artists/Beatles, etc.)
        val folderQueue: Queue<OCFile> = LinkedList()
        
        // Get the first level of subfolders
        val initialSubfolders = storageManager.getNonEncryptedSubfolders(refreshedDir.fileId, user.accountName)
        Log_OC.d(TAG, "Found ${initialSubfolders.size} top-level subfolders to sync")
        folderQueue.addAll(initialSubfolders)

        var processedCount = 0
        var failedCount = 0

        // BFS: Process all folders level by level
        while (folderQueue.isNotEmpty()) {
            val subFolder = folderQueue.poll() ?: continue
            processedCount++

            if (!subFolder.hasValidParentId()) {
                Log_OC.e(TAG, "❌ Skipping subfolder with invalid ID: ${subFolder.remotePath}")
                failedCount++
                continue
            }

            Log_OC.d(TAG, "📂 Processing folder (${processedCount}): ${subFolder.remotePath}")

            // Refresh this folder
            val success = refreshFolder(subFolder, storageManager, forceRefresh)
            if (!success) {
                Log_OC.e(TAG, "❌ Failed to refresh folder: ${subFolder.remotePath}")
                failedCount++
            }

            // After refreshing, get this folder's subfolders and add them to the queue
            // This enables recursive processing of ALL nested folders
            val reloadedFolder = storageManager.getFileByPath(subFolder.remotePath)
            if (reloadedFolder != null && reloadedFolder.hasValidParentId()) {
                val nestedSubfolders = storageManager.getNonEncryptedSubfolders(reloadedFolder.fileId, user.accountName)
                if (nestedSubfolders.isNotEmpty()) {
                    Log_OC.d(TAG, "  └── Found ${nestedSubfolders.size} nested subfolders in: ${subFolder.remotePath}")
                    folderQueue.addAll(nestedSubfolders)
                }
                
                // Also fetch files in this subfolder (not just sub-subfolders)
                // This ensures FolderDownloadWorker has access to all files at every level
                val subfolderFiles = storageManager.getFolderContent(reloadedFolder.fileId, false)
                    .filter { !it.isFolder }
                if (subfolderFiles.isNotEmpty()) {
                    Log_OC.d(TAG, "  └── Found ${subfolderFiles.size} files in: ${subFolder.remotePath}")
                }
            }
        }

        Log_OC.d(TAG, "🏁 Metadata sync completed for folder: $filePath. Processed: $processedCount, Failed: $failedCount")

        return Result.success()
    }

    @Suppress("DEPRECATION")
    private suspend fun refreshFolder(folder: OCFile, storageManager: FileDataStorageManager, forceRefresh: Boolean = false): Boolean =
        withContext(Dispatchers.IO) {
            Log_OC.d(
                TAG,
                "📂 eTag check\n" +
                    "  Path:         " + folder.remotePath + "\n" +
                    "  eTag:         " + folder.etag + "\n" +
                    "  eTagOnServer: " + folder.etagOnServer
            )
            if (!folder.hasValidParentId()) {
                Log_OC.e(TAG, "❌ Folder has invalid ID: ${folder.remotePath}")
                return@withContext false
            }

            // Skip eTag check if forceRefresh is true
            if (!forceRefresh && !folder.isEtagChanged) {
                Log_OC.d(TAG, "Skipping ${folder.remotePath}, eTag didn't change")
                return@withContext true
            }

            // If forceRefresh is true, log that we're doing a forced refresh
            if (forceRefresh) {
                Log_OC.d(TAG, "🔄 Forcing refresh for: ${folder.remotePath}, ignoring eTag")
            }

            Log_OC.d(TAG, "⏳ Fetching metadata for: ${folder.remotePath}, id: ${folder.fileId}")

            val operation = RefreshFolderOperation(folder, storageManager, user, context)
            return@withContext try {
                val result = operation.execute(user, context)
                if (result.isSuccess) {
                    Log_OC.d(TAG, "✅ Successfully fetched metadata for: ${folder.remotePath}")
                    true
                } else {
                    Log_OC.e(TAG, "❌ Failed to fetch metadata for: ${folder.remotePath}")
                    false
                }
            } catch (e: Exception) {
                Log_OC.e(TAG, "❌ Exception refreshing folder ${folder.remotePath}: ${e.message}", e)
                false
            }
        }
}
