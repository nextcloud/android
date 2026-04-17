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
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import com.nextcloud.client.account.User
import com.nextcloud.client.database.entity.OfflineOperationEntity
import com.nextcloud.client.jobs.download.FileDownloadHelper
import com.nextcloud.client.jobs.offlineOperations.OfflineOperationsNotificationManager
import com.nextcloud.client.jobs.operation.FileOperationHelper
import com.nextcloud.client.jobs.upload.FileUploadHelper
import com.nextcloud.client.jobs.upload.FileUploadWorker
import com.nextcloud.client.jobs.upload.UploadNotificationManager
import com.nextcloud.client.jobs.utils.UploadErrorNotificationManager
import com.nextcloud.model.HTTPStatusCodes
import com.nextcloud.utils.extensions.getDecryptedPath
import com.nextcloud.utils.extensions.getParcelableArgument
import com.nextcloud.utils.extensions.logFileSize
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
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.FileStorageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Suppress("TooManyFunctions", "ReturnCount")
class ConflictsResolveActivity :
    FileActivity(),
    OnConflictDecisionMadeListener {

    @Inject
    lateinit var uploadsStorageManager: UploadsStorageManager

    @Inject
    lateinit var fileOperationHelper: FileOperationHelper

    private var conflictUploadId: Long = 0
    private var offlineOperationPath: String? = null
    private var existingFile: OCFile? = null
    private var newFile: OCFile? = null
    private var localBehaviour = FileUploadWorker.LOCAL_BEHAVIOUR_FORGET
    private lateinit var offlineOperationNotificationManager: OfflineOperationsNotificationManager
    private val uploadHelper = FileUploadHelper.instance()
    private val downloadHelper = FileDownloadHelper.instance()

    @JvmField
    var listener: OnConflictDecisionMadeListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        restoreState(savedInstanceState)

        val upload = uploadsStorageManager.getUploadById(conflictUploadId)
        if (upload != null) {
            localBehaviour = upload.localAction
        }

        newFile = file
        setupDecisionListener(upload)
        offlineOperationNotificationManager = OfflineOperationsNotificationManager(this, viewThemeUtils)
    }

    private fun restoreState(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            conflictUploadId = savedInstanceState.getLong(EXTRA_CONFLICT_UPLOAD_ID)
            existingFile = savedInstanceState.getParcelableArgument(EXTRA_EXISTING_FILE, OCFile::class.java)
            localBehaviour = savedInstanceState.getInt(EXTRA_LOCAL_BEHAVIOUR)
            offlineOperationPath = savedInstanceState.getString(EXTRA_OFFLINE_OPERATION_PATH)
        } else {
            conflictUploadId = intent.getLongExtra(EXTRA_CONFLICT_UPLOAD_ID, -1)
            existingFile = intent.getParcelableArgument(EXTRA_EXISTING_FILE, OCFile::class.java)
            localBehaviour = intent.getIntExtra(EXTRA_LOCAL_BEHAVIOUR, localBehaviour)
            offlineOperationPath = intent.getStringExtra(EXTRA_OFFLINE_OPERATION_PATH)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        existingFile.logFileSize(TAG)
        outState.putLong(EXTRA_CONFLICT_UPLOAD_ID, conflictUploadId)
        outState.putParcelable(EXTRA_EXISTING_FILE, existingFile)
        outState.putInt(EXTRA_LOCAL_BEHAVIOUR, localBehaviour)
        outState.putString(
            EXTRA_OFFLINE_OPERATION_PATH,
            offlineOperationPath
        )
    }

    private fun setupDecisionListener(upload: OCUpload?) {
        listener = OnConflictDecisionMadeListener { decision ->
            val file = newFile
            val optionalUser = user
            if (optionalUser.isEmpty) {
                Log_OC.e(TAG, "user not available")
                finish()
                return@OnConflictDecisionMadeListener
            }

            lifecycleScope.launch(Dispatchers.IO) {
                val offlineOperation = offlineOperationPath?.let {
                    fileDataStorageManager.offlineOperationDao.getByPath(it)
                }

                when (decision) {
                    Decision.KEEP_LOCAL -> handleFile(file, upload, optionalUser.get(), NameCollisionPolicy.OVERWRITE)
                    Decision.KEEP_BOTH -> handleFile(file, upload, optionalUser.get(), NameCollisionPolicy.RENAME)
                    Decision.KEEP_SERVER -> keepServer(file, upload, optionalUser.get())
                    Decision.KEEP_OFFLINE_FOLDER -> keepOfflineFolder(file, offlineOperation)
                    Decision.KEEP_SERVER_FOLDER -> keepServerFile(offlineOperation)
                    Decision.KEEP_BOTH_FOLDER -> keepBothFolder(offlineOperation, file)
                    else -> Unit
                }

                upload?.remotePath?.let { path ->
                    val oldFile = storageManager.getFileByDecryptedRemotePath(path)
                    updateThumbnailIfNeeded(decision, file, oldFile)
                }

                withContext(Dispatchers.Main) {
                    UploadErrorNotificationManager.dismissConflictResolveNotification(
                        this@ConflictsResolveActivity,
                        conflictUploadId
                    )
                    finish()
                }
            }
        }
    }

    private fun handleFile(file: OCFile?, upload: OCUpload?, user: User, policy: NameCollisionPolicy) {
        upload?.let { uploadHelper.removeFileUpload(it.remotePath, it.accountName) }
        uploadHelper.uploadUpdatedFile(
            user,
            arrayOf(file),
            localBehaviour,
            policy,
            skipAutoUploadCheck = true
        )
    }

    private suspend fun keepServer(file: OCFile?, upload: OCUpload?, user: User) {
        if (!shouldDeleteLocal()) {
            file?.let {
                downloadHelper.downloadFile(
                    user,
                    file,
                    conflictUploadId = conflictUploadId
                )
            }
        }

        upload?.let {
            uploadHelper.removeFileUpload(it.remotePath, it.accountName)
            val id = it.uploadId.toInt()

            withContext(Dispatchers.Main) {
                UploadNotificationManager(applicationContext, viewThemeUtils, id).dismissNotification(id)
            }
        }
    }

    private suspend fun keepBothFolder(offlineOperation: OfflineOperationEntity?, serverFile: OCFile?) {
        offlineOperation ?: return
        fileDataStorageManager.keepOfflineOperationAndServerFile(offlineOperation, serverFile)
        backgroundJobManager.startOfflineOperations()
        withContext(Dispatchers.Main) {
            offlineOperationNotificationManager.dismissNotification(offlineOperation.id)
        }
    }

    private suspend fun keepServerFile(offlineOperation: OfflineOperationEntity?) {
        offlineOperation ?: return
        fileDataStorageManager.offlineOperationDao.delete(offlineOperation)
        offlineOperation.id?.let {
            withContext(Dispatchers.Main) {
                offlineOperationNotificationManager.dismissNotification(it)
            }
        }
    }

    private suspend fun keepOfflineFolder(serverFile: OCFile?, offlineOperation: OfflineOperationEntity?) {
        serverFile ?: return
        offlineOperation ?: return

        val client = clientRepository.getOwncloudClient() ?: return
        val isSuccess = fileOperationHelper.removeFile(
            serverFile,
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

    @Suppress("ReturnCount")
    override fun onStart() {
        super.onStart()

        if (account == null) {
            finish()
            return
        }
        if (newFile == null) {
            Log_OC.e(TAG, "No file received")
            finish()
            return
        }

        offlineOperationPath?.let { path ->
            showOfflineOperationConflictDialog(path)
            return
        }

        if (existingFile == null) {
            fetchRemoteFileAndShowDialog()
        } else {
            val remotePath = fileDataStorageManager.retrieveRemotePathConsideringEncryption(existingFile) ?: return
            showFileConflictDialog(remotePath)
        }
    }

    private fun showOfflineOperationConflictDialog(path: String) {
        val offlineOperation = fileDataStorageManager.offlineOperationDao.getByPath(path)
        if (offlineOperation == null) {
            showErrorAndFinish()
            return
        }

        val (ft, _) = prepareDialogTransaction()
        ConflictsResolveDialog.newInstance(
            context = this,
            leftFile = offlineOperation,
            rightFile = newFile!!
        ).show(ft, "conflictDialog")
    }

    @Suppress("TooGenericExceptionCaught", "DEPRECATION")
    private fun fetchRemoteFileAndShowDialog() {
        val remotePath = fileDataStorageManager.retrieveRemotePathConsideringEncryption(newFile) ?: return
        val operation = ReadFileRemoteOperation(remotePath)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val result = operation.execute(account, this@ConflictsResolveActivity)
                if (result.isSuccess) {
                    existingFile = FileStorageUtils.fillOCFile(result.data[0] as RemoteFile).also {
                        it.lastSyncDateForProperties = System.currentTimeMillis()
                    }
                    showFileConflictDialog(remotePath)
                } else {
                    Log_OC.e(TAG, "ReadFileRemoteOp returned failure with code: ${result.httpCode}")
                    showErrorAndFinish(result.httpCode)
                }
            } catch (e: Exception) {
                Log_OC.e(TAG, "Error when trying to fetch remote file", e)
                showErrorAndFinish()
            }
        }
    }

    private fun showFileConflictDialog(remotePath: String) {
        val (ft, user) = prepareDialogTransaction()
        if (existingFile != null && storageManager.fileExists(remotePath) && newFile != null) {
            ConflictsResolveDialog.newInstance(
                title = storageManager.getDecryptedPath(existingFile!!),
                context = this,
                leftFile = newFile!!,
                rightFile = existingFile!!,
                user = user
            ).show(ft, "conflictDialog")
        } else {
            Log_OC.e(TAG, "Account was changed, finishing")
            showErrorAndFinish()
        }
    }

    @SuppressLint("CommitTransaction")
    private fun prepareDialogTransaction(): Pair<FragmentTransaction, User> {
        val userOptional = user
        if (!userOptional.isPresent) {
            Log_OC.e(TAG, "User not present")
            showErrorAndFinish()
        }

        val fragmentTransaction = supportFragmentManager.beginTransaction()
        supportFragmentManager.findFragmentByTag("conflictDialog")?.let {
            fragmentTransaction.remove(it)
        }

        return fragmentTransaction to user.get()
    }

    override fun conflictDecisionMade(decision: Decision?) {
        listener?.conflictDecisionMade(decision)
    }

    private fun updateThumbnailIfNeeded(decision: Decision?, file: OCFile?, oldFile: OCFile?) {
        if (decision != Decision.KEEP_BOTH && decision != Decision.KEEP_LOCAL) return

        if (decision == Decision.KEEP_LOCAL) {
            ThumbnailsCacheManager.removeFromCache(oldFile)
        }

        file?.isUpdateThumbnailNeeded = true
        fileDataStorageManager.saveFile(file)
    }

    private fun showErrorAndFinish(code: Int? = null) {
        val message = if (code == HTTPStatusCodes.NOT_FOUND.code) {
            getString(R.string.uploader_file_not_found_on_server_message)
        } else {
            getString(R.string.conflict_dialog_error)
        }

        lifecycleScope.launch(Dispatchers.Main) {
            DisplayUtils.showSnackMessage(this@ConflictsResolveActivity, message)
            finish()
        }
    }

    private fun shouldDeleteLocal(): Boolean = localBehaviour == FileUploadWorker.LOCAL_BEHAVIOUR_DELETE

    companion object {
        const val EXTRA_CONFLICT_UPLOAD_ID = "CONFLICT_UPLOAD_ID"
        const val EXTRA_LOCAL_BEHAVIOUR = "LOCAL_BEHAVIOUR"
        const val EXTRA_EXISTING_FILE = "EXISTING_FILE"
        private const val EXTRA_OFFLINE_OPERATION_PATH = "EXTRA_OFFLINE_OPERATION_PATH"
        private val TAG = ConflictsResolveActivity::class.java.simpleName

        @JvmStatic
        fun createIntent(file: OCFile?, user: User?, conflictUploadId: Long, flag: Int?, context: Context?): Intent =
            Intent(context, ConflictsResolveActivity::class.java).apply {
                if (flag != null) flags = flags or flag
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
