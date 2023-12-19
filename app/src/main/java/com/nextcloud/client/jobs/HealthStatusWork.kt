/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2023 Tobias Kaminsky
 * Copyright (C) 2023 Nextcloud GmbH
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
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
