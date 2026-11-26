/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2016 Nextcloud
 * SPDX-FileCopyrightText: 2016 Bartosz Przybylski <bart.p.pl@gmail.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activity.settings.storage.migration

import android.app.Activity
import androidx.annotation.StringRes
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextcloud.client.account.User
import com.nextcloud.utils.extensions.canonicalPathOrAbsolute
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.activity.settings.storage.migration.model.MigrationResult
import com.owncloud.android.ui.activity.settings.storage.migration.model.StorageMigrationProgressListener
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class StorageMigration(
    private val activity: Activity,
    private val user: User,
    private val sourceStoragePath: String,
    private val targetStoragePath: String,
    private val viewThemeUtils: ViewThemeUtils,
    private val scope: CoroutineScope,
    private val listener: StorageMigrationProgressListener
) {

    private val accountSyncPauser = AccountSyncPauser(activity)
    private val progressDialog by lazy { MigrationProgressDialog(activity, viewThemeUtils) }

    fun migrate() {
        if (dataFoldersOverlap()) {
            Log_OC.w(TAG, "Source and target data folders overlap, skipping migration")
            listener.onCancelMigration()
            return
        }

        if (dataFolderOf(targetStoragePath).isDirectory) {
            askToOverride()
            return
        }

        startFileMigration()
    }

    private fun dataFoldersOverlap(): Boolean {
        val sourceDataFolder = dataFolderOf(sourceStoragePath).canonicalPathOrAbsolute
        val targetDataFolder = dataFolderOf(targetStoragePath).canonicalPathOrAbsolute

        return sourceDataFolder == targetDataFolder ||
            sourceDataFolder.startsWith(targetDataFolder + File.separator) ||
            targetDataFolder.startsWith(sourceDataFolder + File.separator)
    }

    private fun askToOverride() {
        val builder = MaterialAlertDialogBuilder(activity)
            .setMessage(R.string.file_migration_directory_already_exists)
            .setCancelable(true)
            .setOnCancelListener { listener.onCancelMigration() }
            .setNegativeButton(R.string.common_cancel) { _, _ -> listener.onCancelMigration() }
            .setNeutralButton(R.string.file_migration_use_data_folder) { _, _ -> startStoragePathSwitch() }
            .setPositiveButton(R.string.file_migration_override_data_folder) { _, _ -> startFileMigration() }

        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(activity, builder)
        builder.create().show()
    }

    private fun startFileMigration() {
        runInBackground {
            FileMigrationTask(activity, user, sourceStoragePath, targetStoragePath, accountSyncPauser, ::reportProgress)
                .migrate()
        }
    }

    private fun startStoragePathSwitch() {
        runInBackground {
            reportProgress(R.string.file_migration_preparing)
            accountSyncPauser.runPaused(::reportProgress) { MigrationResult.Success }
        }
    }

    private fun runInBackground(work: suspend () -> MigrationResult) {
        progressDialog.show()

        scope.launch(Dispatchers.Main) {
            val result = withContext(Dispatchers.IO) { runSafely(work) }
            onFinished(result)
        }
    }

    private suspend fun reportProgress(@StringRes messageResId: Int) {
        withContext(Dispatchers.Main) {
            if (canShowUi()) progressDialog.setMessage(messageResId)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun runSafely(work: suspend () -> MigrationResult): MigrationResult = try {
        work()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log_OC.e(TAG, "Unexpected migration failure", e)
        MigrationResult.Failure(R.string.file_migration_failed_while_coping)
    }

    private fun onFinished(result: MigrationResult) {
        val succeed = result is MigrationResult.Success
        listener.onStorageMigrationFinished(if (succeed) targetStoragePath else sourceStoragePath, succeed)

        if (!canShowUi()) return

        when (result) {
            MigrationResult.Success -> progressDialog.dismiss()

            is MigrationResult.Failure -> if (result.messageResId == R.string.file_migration_failed_not_readable) {
                progressDialog.dismiss()
                askToStillMove()
            } else {
                progressDialog.showFailure(result.messageResId)
            }
        }
    }

    private fun canShowUi(): Boolean = !activity.isFinishing && !activity.isDestroyed

    private fun askToStillMove() {
        val builder = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.file_migration_source_not_readable_title)
            .setMessage(activity.getString(R.string.file_migration_source_not_readable, targetStoragePath))
            .setNegativeButton(R.string.common_no) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton(R.string.common_yes) { _, _ ->
                listener.onStorageMigrationFinished(targetStoragePath, true)
            }

        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(activity, builder)
        builder.create().show()
    }

    companion object {
        private const val TAG = "StorageMigration"

        fun dataFolderOf(storagePath: String): File = File(storagePath, MainApp.dataFolder)

        fun dataFolderPrefix(storagePath: String): String =
            storagePath + File.separator + MainApp.dataFolder + File.separator
    }
}
