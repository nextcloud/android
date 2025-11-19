/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Jonas Mayer <jonas.a.mayer@gmx.net>
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2019 Alice Gaudon <alice@gaudon.pro>
 * SPDX-FileCopyrightText: 2012 Bartosz Przybylski <bart.p.pl@gmail.com>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.ui.activity

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import com.nextcloud.client.account.User
import com.nextcloud.client.database.entity.OfflineOperationEntity
import com.nextcloud.client.jobs.offlineOperations.OfflineOperationsNotificationManager
import com.nextcloud.client.jobs.operation.FileOperationHelper
import com.nextcloud.client.jobs.upload.FileUploadHelper
import com.nextcloud.client.jobs.upload.FileUploadWorker
import com.nextcloud.model.HTTPStatusCodes
import com.nextcloud.utils.extensions.getDecryptedPath
import com.nextcloud.utils.extensions.logFileSize
import com.nextcloud.utils.extensions.toFile
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.db.OCUpload
import com.owncloud.android.files.services.NameCollisionPolicy
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.ui.dialog.ConflictsResolveDialog
import com.owncloud.android.ui.dialog.ConflictsResolveDialog.Decision
import com.owncloud.android.ui.dialog.ConflictsResolveDialog.OnConflictDecisionMadeListener
import com.owncloud.android.ui.notifications.NotificationUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Activity responsible for resolving file conflicts.
 *
 * - **file**: The new local file selected by the user. This represents the local
 *   version that may conflict with the remote version.
 *
 * The activity allows the user to choose between keeping the local file, keeping the server file,
 * keeping both, or applying similar logic for offline operations.
 */
@Suppress("TooManyFunctions")
class ConflictsResolveActivity :
    FileActivity(),
    OnConflictDecisionMadeListener {
    @Inject
    lateinit var uploadsStorageManager: UploadsStorageManager

    @Inject
    lateinit var fileOperationHelper: FileOperationHelper

    private var conflictUploadId: Long = 0
    private var offlineOperationPath: String? = null
    private var localBehaviour = FileUploadWorker.LOCAL_BEHAVIOUR_FORGET
    private lateinit var offlineOperationNotificationManager: OfflineOperationsNotificationManager

    /**
     * The existing file stored on the server (remote version).
     * Retrieved either from the local DB or from the server via ReadFileRemoteOperation.
     */
    private var existingFile: OCFile? = null

    @JvmField
    var listener: OnConflictDecisionMadeListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        getArguments(savedInstanceState)

        val upload = uploadsStorageManager.getUploadById(conflictUploadId)
        if (upload != null) {
            localBehaviour = upload.localAction
        }

        setupOnConflictDecisionMadeListener(upload)
        offlineOperationNotificationManager = OfflineOperationsNotificationManager(this, viewThemeUtils)
    }

    private fun getArguments(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            conflictUploadId = savedInstanceState.getLong(EXTRA_CONFLICT_UPLOAD_ID)
            localBehaviour = savedInstanceState.getInt(EXTRA_LOCAL_BEHAVIOUR)
            offlineOperationPath = savedInstanceState.getString(EXTRA_OFFLINE_OPERATION_PATH)
        } else {
            offlineOperationPath = intent.getStringExtra(EXTRA_OFFLINE_OPERATION_PATH)
            conflictUploadId = intent.getLongExtra(EXTRA_CONFLICT_UPLOAD_ID, -1)
            localBehaviour = intent.getIntExtra(EXTRA_LOCAL_BEHAVIOUR, localBehaviour)
        }
    }

    private fun setupOnConflictDecisionMadeListener(upload: OCUpload?) {
        listener = OnConflictDecisionMadeListener { decision: Decision? ->
            if (user.isEmpty) {
                Log_OC.e(TAG, "cannot resolve conflict user not exists")
                return@OnConflictDecisionMadeListener
            }
            val user = user.get()
            val offlineOperation = offlineOperationPath?.let {
                fileDataStorageManager.offlineOperationDao.getByPath(it)
            }

            when (decision) {
                Decision.KEEP_LOCAL -> uploadFileByDecision(upload, user, NameCollisionPolicy.OVERWRITE)
                Decision.KEEP_BOTH -> uploadFileByDecision(upload, user, NameCollisionPolicy.RENAME)
                Decision.KEEP_SERVER -> uploadFileByDecision(upload, user, NameCollisionPolicy.SKIP)
                Decision.KEEP_OFFLINE_FOLDER -> keepOfflineFolder(offlineOperation)
                Decision.KEEP_SERVER_FOLDER -> keepServerFile(offlineOperation)
                Decision.KEEP_BOTH_FOLDER -> keepBothFolder(offlineOperation)
                else -> Unit
            }

            updateThumbnailIfNeeded(decision)
            deleteLocalFileIfNeeded()
            dismissConflictResolveNotification()
            finish()
        }
    }

    private fun updateThumbnailIfNeeded(decision: Decision?) {
        if (decision == Decision.KEEP_BOTH) {
            file?.isUpdateThumbnailNeeded = true
            fileDataStorageManager.saveFile(file)
        }

        if (decision == Decision.KEEP_LOCAL || decision == Decision.KEEP_BOTH) {
            ThumbnailsCacheManager.removeFromCache(existingFile)
            existingFile?.isUpdateThumbnailNeeded = true
            fileDataStorageManager.saveFile(existingFile)
        }
    }

    private fun dismissConflictResolveNotification() {
        file?.let {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val tag = NotificationUtils.createUploadNotificationTag(it)
            notificationManager.cancel(tag, FileUploadWorker.NOTIFICATION_ERROR_ID)
        }
    }

    private fun keepBothFolder(offlineOperation: OfflineOperationEntity?) {
        offlineOperation ?: return
        fileDataStorageManager.keepOfflineOperationAndServerFile(offlineOperation, file)
        backgroundJobManager.startOfflineOperations()
        offlineOperationNotificationManager.dismissNotification(offlineOperation.id)
    }

    private fun keepServerFile(offlineOperation: OfflineOperationEntity?) {
        offlineOperation ?: return
        fileDataStorageManager.offlineOperationDao.delete(offlineOperation)

        val id = offlineOperation.id ?: return
        offlineOperationNotificationManager.dismissNotification(id)
    }

    private fun keepOfflineFolder(offlineOperation: OfflineOperationEntity?) {
        file ?: return
        offlineOperation ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            val client = clientRepository.getOwncloudClient() ?: return@launch
            val isSuccess = fileOperationHelper.removeFile(
                file!!,
                onlyLocalCopy = false,
                inBackground = false,
                client = client
            )

            if (isSuccess) {
                backgroundJobManager.startOfflineOperations()
                withContext(Dispatchers.Main) {
                    offlineOperationNotificationManager.dismissNotification(offlineOperation.id)
                }
            }
        }
    }

    private fun uploadFileByDecision(upload: OCUpload?, user: User, policy: NameCollisionPolicy) {
        if (upload == null) {
            Log_OC.e(TAG, "upload is null cannot upload a new file")
            return
        }

        FileUploadHelper.instance().run {
            removeFileUpload(upload.remotePath, upload.accountName)

            if (policy != NameCollisionPolicy.SKIP) {
                uploadUpdatedFile(
                    user,
                    arrayOf(file),
                    localBehaviour,
                    policy
                )
            }
        }
    }

    private fun deleteLocalFileIfNeeded() {
        if (localBehaviour != FileUploadWorker.LOCAL_BEHAVIOUR_DELETE) {
            return
        }

        try {
            val result = file?.storagePath?.toFile()?.delete()
            if (result == true) {
                Log_OC.d(TAG, "local file deleted")
            }
        } catch (e: Exception) {
            Log_OC.e(TAG, "local file deletion: ", e)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        existingFile.logFileSize(TAG)

        outState.run {
            putLong(EXTRA_CONFLICT_UPLOAD_ID, conflictUploadId)
            putInt(EXTRA_LOCAL_BEHAVIOUR, localBehaviour)
        }
    }

    override fun conflictDecisionMade(decision: Decision?) {
        listener?.conflictDecisionMade(decision)
    }

    @Suppress("ReturnCount")
    override fun onStart() {
        super.onStart()

        if (account == null) {
            showErrorAndFinish()
            return
        }

        if (file == null) {
            Log_OC.e(TAG, "newly selected local file cannot be null")
            showErrorAndFinish()
            return
        }

        offlineOperationPath?.let { path ->
            file?.let { ocFile ->
                val offlineOperation = fileDataStorageManager.offlineOperationDao.getByPath(path)

                if (offlineOperation == null) {
                    showErrorAndFinish()
                    return
                }

                val (ft, _) = prepareDialog()
                val dialog = ConflictsResolveDialog.newInstance(
                    context = this,
                    leftFile = offlineOperation,
                    rightFile = ocFile
                )
                dialog.show(ft, "conflictDialog")
                return
            }
        }

        initExistingFile()
    }

    private fun initExistingFile() {
        lifecycleScope.launch {
            val resolved = withContext(Dispatchers.IO) {
                resolveExistingFileFromDbOrServer()
            }

            withContext(Dispatchers.Main) {
                if (resolved == null) {
                    Log_OC.e(TAG, "existing file cannot be resolved from DB or server")
                    showErrorAndFinish()
                    return@withContext
                }

                existingFile = resolved

                val remotePath = fileDataStorageManager
                    .retrieveRemotePathConsideringEncryption(existingFile)

                if (remotePath == null) {
                    Log_OC.e(TAG, "failed to obtain remotePath for existing file")
                    showErrorAndFinish()
                    return@withContext
                }

                startDialog(remotePath)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun resolveExistingFileFromDbOrServer(): OCFile? {
        val candidate = file ?: return null

        val remotePath = try {
            fileDataStorageManager.retrieveRemotePathConsideringEncryption(candidate)
        } catch (e: Exception) {
            Log_OC.e(TAG, "Error calculating decrypted remote path", e)
            return null
        } ?: return null

        // check db first
        var dbFile = fileDataStorageManager.getFileByDecryptedRemotePath(remotePath)
        if (dbFile != null && dbFile.fileId != -1L) {
            return dbFile
        }

        Log_OC.w(TAG, "DB entry missing for $remotePath → fetching from server…")

        val account = account ?: return null
        val result = try {
            val op = ReadFileRemoteOperation(remotePath)
            op.execute(account, this@ConflictsResolveActivity)
        } catch (e: Exception) {
            Log_OC.e(TAG, "Error calling ReadFileRemoteOperation", e)
            return null
        }

        if (!result.isSuccess || result.data.isEmpty()) {
            Log_OC.e(TAG, "Remote file fetch failed (http ${result.httpCode})")
            return null
        }

        val remoteFile = result.data[0] as? RemoteFile ?: return null
        dbFile = fileDataStorageManager.getFileByDecryptedRemotePath(remoteFile.remotePath)

        if (dbFile != null && dbFile.fileId != -1L) {
            dbFile.lastSyncDateForProperties = System.currentTimeMillis()
            fileDataStorageManager.saveFile(dbFile)
            return dbFile
        }

        Log_OC.e(TAG, "DB still missing entry for ${remoteFile.remotePath}")
        return null
    }


    @SuppressLint("CommitTransaction")
    private fun prepareDialog(): Pair<FragmentTransaction, User> {
        val userOptional = user
        if (!userOptional.isPresent) {
            Log_OC.e(TAG, "User not present")
            showErrorAndFinish()
        }

        // Check whether the file is contained in the current Account
        val prev = supportFragmentManager.findFragmentByTag("conflictDialog")
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        if (prev != null) {
            fragmentTransaction.remove(prev)
        }

        return fragmentTransaction to user.get()
    }

    private fun startDialog(remotePath: String) {
        val (ft, user) = prepareDialog()

        if (existingFile != null && storageManager.fileExists(remotePath) && file != null) {
            val dialog = ConflictsResolveDialog.newInstance(
                title = storageManager.getDecryptedPath(existingFile!!),
                context = this,
                leftFile = file!!,
                rightFile = existingFile!!,
                user = user
            )
            dialog.show(ft, "conflictDialog")
        } else {
            // Account was changed to a different one - just finish
            Log_OC.e(TAG, "Account was changed, finishing")
            showErrorAndFinish()
        }
    }

    private fun showErrorAndFinish(code: Int? = null) {
        val message = parseErrorMessage(code)
        lifecycleScope.launch(Dispatchers.Main) {
            Toast.makeText(this@ConflictsResolveActivity, message, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun parseErrorMessage(code: Int?): String = if (code == HTTPStatusCodes.NOT_FOUND.code) {
        getString(R.string.uploader_file_not_found_on_server_message)
    } else {
        getString(R.string.conflict_dialog_error)
    }

    companion object {
        /**
         * A nullable upload entry that must be removed when and if the conflict is resolved.
         */
        const val EXTRA_CONFLICT_UPLOAD_ID = "CONFLICT_UPLOAD_ID"

        /**
         * Specify the upload local behaviour when there is no CONFLICT_UPLOAD.
         */
        const val EXTRA_LOCAL_BEHAVIOUR = "LOCAL_BEHAVIOUR"
        private const val EXTRA_OFFLINE_OPERATION_PATH = "EXTRA_OFFLINE_OPERATION_PATH"

        private val TAG = ConflictsResolveActivity::class.java.simpleName

        @JvmStatic
        fun createIntent(file: OCFile?, user: User?, conflictUploadId: Long, flag: Int?, context: Context?): Intent =
            Intent(context, ConflictsResolveActivity::class.java).apply {
                if (flag != null) {
                    flags = flags or flag
                }
                putExtra(EXTRA_FILE, file)
                putExtra(EXTRA_USER, user)
                putExtra(EXTRA_CONFLICT_UPLOAD_ID, conflictUploadId)
            }

        @JvmStatic
        fun createIntent(file: OCFile, offlineOperationPath: String, context: Context): Intent =
            Intent(context, ConflictsResolveActivity::class.java).apply {
                putExtra(EXTRA_FILE, file)
                putExtra(EXTRA_OFFLINE_OPERATION_PATH, offlineOperationPath)
            }
    }
}
