/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.settings.storage.migration

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Activity
import android.content.ContentResolver
import androidx.annotation.StringRes
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextcloud.client.account.User
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.activity.settings.storage.migration.model.MigrationException
import com.owncloud.android.ui.activity.settings.storage.migration.model.MigrationResult
import com.owncloud.android.ui.activity.settings.storage.migration.model.StorageMigrationProgressListener
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

@Suppress("TooManyFunctions")
class StorageMigration(
    private val activity: Activity,
    user: User,
    private val sourceStoragePath: String,
    private val targetStoragePath: String,
    private val viewThemeUtils: ViewThemeUtils,
    private val scope: CoroutineScope,
    private val listener: StorageMigrationProgressListener
) {

    private val dataFolderMover = DataFolderMover(activity, user, sourceStoragePath, targetStoragePath)
    private val progressDialog by lazy { MigrationProgressDialog(activity, viewThemeUtils) }
    private val syncAuthority = activity.getString(R.string.authority)
    private val accounts: Array<Account> =
        AccountManager.get(activity).getAccountsByType(MainApp.getAccountType(activity))

    fun migrate() {
        if (dataFolderMover.dataFoldersOverlap()) {
            Log_OC.w(TAG, "Source and target data folders overlap, skipping migration")
            listener.onCancelMigration()
            return
        }

        if (dataFolderMover.targetDataFolderExists()) {
            showDataFolderExistsDialog()
            return
        }

        startMigration(moveFiles = true)
    }

    private fun showDataFolderExistsDialog() {
        val builder = MaterialAlertDialogBuilder(activity)
            .setMessage(R.string.file_migration_directory_already_exists)
            .setCancelable(true)
            .setOnCancelListener { listener.onCancelMigration() }
            .setNegativeButton(R.string.common_cancel) { _, _ -> listener.onCancelMigration() }
            .setNeutralButton(R.string.file_migration_use_data_folder) { _, _ -> startMigration(moveFiles = false) }
            .setPositiveButton(R.string.file_migration_override_data_folder) { _, _ ->
                startMigration(moveFiles = true)
            }

        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(activity, builder)
        builder.create().show()
    }

    @Suppress("TooGenericExceptionCaught")
    private fun startMigration(moveFiles: Boolean) {
        progressDialog.show()

        scope.launch(Dispatchers.Main) {
            val result = withContext(Dispatchers.IO) {
                try {
                    if (moveFiles) moveDataFolder() else useExistingDataFolder()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log_OC.e(TAG, "Unexpected migration failure", e)
                    MigrationResult.Failure(R.string.file_migration_failed_while_coping)
                }
            }

            onMigrationFinished(result)
        }
    }

    private suspend fun moveDataFolder(): MigrationResult {
        val syncStates = saveSyncStates()

        return try {
            showProgress(R.string.file_migration_preparing)
            dataFolderMover.prepare()

            showProgress(R.string.file_migration_checking_destination)
            dataFolderMover.checkDestination()

            stopSyncAndWait()

            showProgress(R.string.file_migration_migrating)
            dataFolderMover.copyFiles()

            showProgress(R.string.file_migration_updating_index)
            dataFolderMover.updateIndex()

            showProgress(R.string.file_migration_cleaning)
            dataFolderMover.deleteSource()

            showProgress(R.string.file_migration_ok_finished)
            MigrationResult.Success
        } catch (e: MigrationException) {
            dataFolderMover.rollback()
            MigrationResult.Failure(e.messageResId)
        } finally {
            restoreSync(syncStates)
        }
    }

    private suspend fun useExistingDataFolder(): MigrationResult {
        val syncStates = saveSyncStates()

        try {
            showProgress(R.string.file_migration_preparing)
            stopSyncAndWait()
        } finally {
            restoreSync(syncStates)
        }

        return MigrationResult.Success
    }

    private suspend fun saveSyncStates(): BooleanArray {
        showProgress(R.string.file_migration_saving_accounts_configuration)

        return BooleanArray(accounts.size) { ContentResolver.getSyncAutomatically(accounts[it], syncAuthority) }
    }

    private suspend fun stopSyncAndWait() {
        showProgress(R.string.file_migration_waiting_for_unfinished_sync)
        accounts.forEach { ContentResolver.setSyncAutomatically(it, syncAuthority, false) }

        accounts.forEach { account ->
            while (ContentResolver.isSyncActive(account, syncAuthority)) {
                delay(SYNC_CHECK_INTERVAL)
            }
        }
    }

    private suspend fun restoreSync(syncStates: BooleanArray) {
        showProgress(R.string.file_migration_restoring_accounts_configuration)

        accounts.forEachIndexed { index, account ->
            ContentResolver.setSyncAutomatically(account, syncAuthority, syncStates[index])
        }
    }

    private suspend fun showProgress(@StringRes messageResId: Int) {
        withContext(Dispatchers.Main) {
            if (canShowUi()) progressDialog.setMessage(messageResId)
        }
    }

    private fun onMigrationFinished(result: MigrationResult) {
        val succeed = result is MigrationResult.Success
        listener.onStorageMigrationFinished(if (succeed) targetStoragePath else sourceStoragePath, succeed)

        if (!canShowUi()) return

        when (result) {
            MigrationResult.Success -> progressDialog.dismiss()

            is MigrationResult.Failure -> if (result.messageResId == R.string.file_migration_failed_not_readable) {
                progressDialog.dismiss()
                showSourceNotReadableDialog()
            } else {
                progressDialog.showFailure(result.messageResId)
            }
        }
    }

    private fun canShowUi(): Boolean = !activity.isFinishing && !activity.isDestroyed

    private fun showSourceNotReadableDialog() {
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
        private val SYNC_CHECK_INTERVAL = 1.seconds
    }
}
