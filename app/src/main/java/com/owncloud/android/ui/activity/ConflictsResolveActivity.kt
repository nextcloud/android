/*
 * ownCloud Android client application
 *
 * @author Bartek Przybylski
 * @author David A. Velasco Copyright (C) 2012 Bartek Przybylski Copyright (C) 2016 ownCloud Inc.
 * <p>
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License version 2, as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 * <p/>
 * You should have received a copy of the GNU General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.nextcloud.client.account.User
import com.nextcloud.utils.extensions.getParcelableArgument
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.db.OCUpload
import com.owncloud.android.files.services.FileDownloader
import com.owncloud.android.files.services.FileUploader
import com.owncloud.android.files.services.NameCollisionPolicy
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.ui.dialog.ConflictsResolveDialog
import com.owncloud.android.ui.dialog.ConflictsResolveDialog.Decision
import com.owncloud.android.ui.dialog.ConflictsResolveDialog.OnConflictDecisionMadeListener
import com.owncloud.android.utils.FileStorageUtils
import javax.inject.Inject

/**
 * Wrapper activity which will be launched if keep-in-sync file will be modified by external application.
 */
class ConflictsResolveActivity : FileActivity(), OnConflictDecisionMadeListener {

    @JvmField
    @Inject
    var uploadsStorageManager: UploadsStorageManager? = null

    private var conflictUploadId: Long = 0
    private var existingFile: OCFile? = null
    private var newFile: OCFile? = null
    private var localBehaviour = FileUploader.LOCAL_BEHAVIOUR_FORGET

    @JvmField
    var listener: OnConflictDecisionMadeListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        getArguments(savedInstanceState)

        val upload = uploadsStorageManager?.getUploadById(conflictUploadId)
        if (upload != null) {
            localBehaviour = upload.localAction
        }

        // new file was modified locally in file system
        newFile = file
        setupOnConflictDecisionMadeListener(upload)
    }

    private fun getArguments(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            conflictUploadId = savedInstanceState.getLong(EXTRA_CONFLICT_UPLOAD_ID)
            existingFile = savedInstanceState.getParcelableArgument(EXTRA_EXISTING_FILE, OCFile::class.java)
            localBehaviour = savedInstanceState.getInt(EXTRA_LOCAL_BEHAVIOUR)
        } else {
            conflictUploadId = intent.getLongExtra(EXTRA_CONFLICT_UPLOAD_ID, -1)
            existingFile = intent.getParcelableExtra(EXTRA_EXISTING_FILE)
            localBehaviour = intent.getIntExtra(EXTRA_LOCAL_BEHAVIOUR, localBehaviour)
        }
    }

    private fun setupOnConflictDecisionMadeListener(upload: OCUpload?) {
        listener = OnConflictDecisionMadeListener { decision: Decision? ->
            val file = newFile // local file got changed, so either upload it or replace it again by server
            // version
            val user = user.orElseThrow { RuntimeException() }
            when (decision) {
                Decision.CANCEL -> {}
                Decision.KEEP_LOCAL -> {
                    FileUploader.uploadUpdateFile(
                        baseContext,
                        user,
                        file,
                        localBehaviour,
                        NameCollisionPolicy.OVERWRITE
                    )
                    uploadsStorageManager!!.removeUpload(upload)
                }

                Decision.KEEP_BOTH -> {
                    FileUploader.uploadUpdateFile(
                        baseContext,
                        user,
                        file,
                        localBehaviour,
                        NameCollisionPolicy.RENAME
                    )
                    uploadsStorageManager!!.removeUpload(upload)
                }

                Decision.KEEP_SERVER -> if (!shouldDeleteLocal()) {
                    // Overwrite local file
                    val intent = Intent(baseContext, FileDownloader::class.java)
                    intent.putExtra(FileDownloader.EXTRA_USER, getUser().orElseThrow { RuntimeException() })
                    intent.putExtra(FileDownloader.EXTRA_FILE, file)
                    intent.putExtra(EXTRA_CONFLICT_UPLOAD_ID, conflictUploadId)
                    startService(intent)
                } else {
                    uploadsStorageManager!!.removeUpload(upload)
                }

                else -> {}
            }
            finish()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(EXTRA_CONFLICT_UPLOAD_ID, conflictUploadId)
        outState.putParcelable(EXTRA_EXISTING_FILE, existingFile)
        outState.putInt(EXTRA_LOCAL_BEHAVIOUR, localBehaviour)
    }

    override fun conflictDecisionMade(decision: Decision) {
        listener?.conflictDecisionMade(decision)
    }

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
        if (existingFile == null) {
            // fetch info of existing file from server
            val operation = ReadFileRemoteOperation(newFile!!.remotePath)

            @Suppress("TooGenericExceptionCaught")
            Thread {
                try {
                    val result = operation.execute(account, this)
                    if (result.isSuccess) {
                        existingFile = FileStorageUtils.fillOCFile(result.data[0] as RemoteFile)
                        existingFile?.lastSyncDateForProperties = System.currentTimeMillis()
                        startDialog()
                    } else {
                        Log_OC.e(TAG, "ReadFileRemoteOp returned failure with code: " + result.httpCode)
                        showErrorAndFinish()
                    }
                } catch (e: Exception) {
                    Log_OC.e(TAG, "Error when trying to fetch remote file", e)
                    showErrorAndFinish()
                }
            }.start()
        } else {
            startDialog()
        }
    }

    private fun startDialog() {
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
        if (existingFile != null && storageManager.fileExists(newFile!!.remotePath)) {
            val dialog = ConflictsResolveDialog.newInstance(
                existingFile,
                newFile,
                userOptional.get()
            )
            dialog.show(fragmentTransaction, "conflictDialog")
        } else {
            // Account was changed to a different one - just finish
            Log_OC.e(TAG, "Account was changed, finishing")
            showErrorAndFinish()
        }
    }

    private fun showErrorAndFinish() {
        runOnUiThread { Toast.makeText(this, R.string.conflict_dialog_error, Toast.LENGTH_LONG).show() }
        finish()
    }

    /**
     * @return whether the local version of the files is to be deleted.
     */
    private fun shouldDeleteLocal(): Boolean {
        return localBehaviour == FileUploader.LOCAL_BEHAVIOUR_DELETE
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
        const val EXTRA_EXISTING_FILE = "EXISTING_FILE"
        private val TAG = ConflictsResolveActivity::class.java.simpleName

        @JvmStatic
        fun createIntent(
            file: OCFile?,
            user: User?,
            conflictUploadId: Long,
            flag: Int?,
            context: Context?
        ): Intent {
            val intent = Intent(context, ConflictsResolveActivity::class.java)
            if (flag != null) {
                intent.flags = intent.flags or flag
            }
            intent.putExtra(EXTRA_FILE, file)
            intent.putExtra(EXTRA_USER, user)
            intent.putExtra(EXTRA_CONFLICT_UPLOAD_ID, conflictUploadId)
            return intent
        }
    }
}
