/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.jobs

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.db.UploadResult
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.status.Problem
import com.owncloud.android.lib.resources.status.SendClientDiagnosticRemoteOperation
import com.owncloud.android.utils.EncryptionUtils
import com.owncloud.android.utils.theme.CapabilityUtils

class HealthStatusWork(
    private val context: Context,
    params: WorkerParameters,
    private val userAccountManager: UserAccountManager,
    private val arbitraryDataProvider: ArbitraryDataProvider,
    private val backgroundJobManager: BackgroundJobManager
) : Worker(context, params) {
    override fun doWork(): Result {
        backgroundJobManager.logStartOfWorker(BackgroundJobManagerImpl.formatClassTag(this::class))

        for (user in userAccountManager.allUsers) {
            // only if security guard is enabled
            if (!CapabilityUtils.getCapability(user, context).securityGuard.isTrue) {
                continue
            }

            val syncConflicts = collectSyncConflicts(user)

            val problems = mutableListOf<Problem>().apply {
                addAll(
                    collectUploadProblems(
                        user,
                        listOf(
                            UploadResult.CREDENTIAL_ERROR,
                            UploadResult.CANNOT_CREATE_FILE,
                            UploadResult.FOLDER_ERROR,
                            UploadResult.SERVICE_INTERRUPTED
                        )
                    )
                )
            }

            val virusDetected = collectUploadProblems(user, listOf(UploadResult.VIRUS_DETECTED)).firstOrNull()

            val e2eErrors = EncryptionUtils.readE2eError(arbitraryDataProvider, user)

            val nextcloudClient = OwnCloudClientManagerFactory.getDefaultSingleton()
                .getNextcloudClientFor(user.toOwnCloudAccount(), context)
            val result =
                SendClientDiagnosticRemoteOperation(
                    syncConflicts,
                    problems,
                    virusDetected,
                    e2eErrors
                ).execute(
                    nextcloudClient
                )

            if (!result.isSuccess) {
                if (result.exception == null) {
                    Log_OC.e(TAG, "Update client health NOT successful!")
                } else {
                    Log_OC.e(TAG, "Update client health NOT successful!", result.exception)
                }
            }
        }

        val result = Result.success()
        backgroundJobManager.logEndOfWorker(BackgroundJobManagerImpl.formatClassTag(this::class), result)
        return result
    }

    private fun collectSyncConflicts(user: User): Problem? {
        val fileDataStorageManager = FileDataStorageManager(user, context.contentResolver)

        val conflicts = fileDataStorageManager.getFilesWithSyncConflict(user)

        return if (conflicts.isEmpty()) {
            null
        } else {
            Problem("sync_conflicts", conflicts.size, conflicts.minOf { it.lastSyncDateForData })
        }
    }

    private fun collectUploadProblems(user: User, errorCodes: List<UploadResult>): List<Problem> {
        val uploadsStorageManager = UploadsStorageManager(userAccountManager, context.contentResolver)

        val problems = uploadsStorageManager
            .getUploadsForAccount(user.accountName)
            .filter {
                errorCodes.contains(it.lastResult)
            }.groupBy { it.lastResult }

        return if (problems.isEmpty()) {
            emptyList()
        } else {
            return problems.map { problem ->
                Problem(problem.key.toString(), problem.value.size, problem.value.minOf { it.uploadEndTimestamp })
            }
        }
    }

    companion object {
        private const val TAG = "Health Status"
    }
}
