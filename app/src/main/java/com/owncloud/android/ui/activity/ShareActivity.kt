/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2018-2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2017 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2016 ownCloud Inc.
 * SPDX-FileCopyrightText: 2016 Juan Carlos González Cabrero <malkomich@gmail.com>
 * SPDX-FileCopyrightText: 2015 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-FileCopyrightText: 2015 María Asensio Valverde <masensio@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.nextcloud.client.account.User
import com.owncloud.android.R
import com.owncloud.android.databinding.ShareActivityBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.SyncedFolderObserver
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.operations.GetSharesForFileOperation
import com.owncloud.android.ui.fragment.FileDetailSharingFragment
import com.owncloud.android.ui.fragment.FileDetailsSharingProcessFragment
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.MimeTypeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShareActivity : FileActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ShareActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val currentUser = user.orElse(null) ?: run {
            finish()
            return
        }
        val file = file ?: run {
            finish()
            return
        }

        setupHeader(binding, file, currentUser)
        fetchRemoteFileSize(file, currentUser, binding)

        if (savedInstanceState == null) {
            showSharingFragment(file, currentUser)
        }
    }

    override fun onStart() {
        super.onStart()
        Log_OC.d(TAG, "Refreshing lists on account set")
        refreshSharesFromStorageManager()
    }

    override fun doShareWith(shareeName: String, shareType: ShareType) {
        val file = file ?: return
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.share_fragment_container,
                FileDetailsSharingProcessFragment.newInstance(file, shareeName, shareType, false),
                FileDetailsSharingProcessFragment.TAG
            )
            .commit()
    }

    override fun onRemoteOperationFinish(operation: RemoteOperation<*>?, result: RemoteOperationResult<*>) {
        super.onRemoteOperationFinish(operation, result)

        val isShareNotFound = operation is GetSharesForFileOperation &&
            result.code == RemoteOperationResult.ResultCode.SHARE_NOT_FOUND

        if (result.isSuccess || isShareNotFound) {
            Log_OC.d(TAG, "Refreshing view on successful operation or finished refresh")
            refreshSharesFromStorageManager()
        }
    }

    override fun onShareProcessClosed() {
        finish()
    }

    private fun setupHeader(binding: ShareActivityBinding, file: OCFile, user: User) {
        binding.shareBackButton.setOnClickListener { navigateToParentFolder(user) }
        setupFileIcon(binding, file, user)
        with(binding) {
            shareFileName.text = getString(R.string.share_file, file.fileName)
            viewThemeUtils.platform.colorViewBackground(shareHeaderDivider)
            shareFileSize.text = DisplayUtils.bytesToHumanReadable(file.fileLength)
        }
    }

    private fun setupFileIcon(binding: ShareActivityBinding, file: OCFile, user: User) {
        if (file.isFolder) {
            val isAutoUploadFolder = SyncedFolderObserver.isAutoUploadFolder(file, user)
            val overlayIconId = file.getFileOverlayIconId(isAutoUploadFolder)
            binding.shareFileIcon.setImageDrawable(
                MimeTypeUtil.getFolderIcon(preferences.isDarkModeEnabled(), overlayIconId, this, viewThemeUtils)
            )
        } else {
            binding.shareFileIcon.setImageDrawable(
                MimeTypeUtil.getFileTypeIcon(file.mimeType, file.fileName, this, viewThemeUtils)
            )
            if (MimeTypeUtil.isImage(file)) {
                ThumbnailsCacheManager.getBitmapFromDiskCache(file.remoteId.toString())?.let {
                    binding.shareFileIcon.setImageBitmap(it)
                }
            }
        }
    }

    private fun fetchRemoteFileSize(file: OCFile, user: User, binding: ShareActivityBinding) {
        lifecycleScope.launch(Dispatchers.IO) {
            val result = ReadFileRemoteOperation(file.remotePath).execute(user, this@ShareActivity)
            if (result.isSuccess) {
                val length = (result.data.first() as RemoteFile).length
                file.fileLength = length
                withContext(Dispatchers.Main) {
                    binding.shareFileSize.text = DisplayUtils.bytesToHumanReadable(length)
                }
            }
        }
    }

    private fun showSharingFragment(file: OCFile, user: User) {
        val fragment = FileDetailSharingFragment.newInstance(file, user)
        supportFragmentManager.beginTransaction()
            .replace(R.id.share_fragment_container, fragment, TAG_SHARE_FRAGMENT)
            .commit()
    }

    private fun refreshSharesFromStorageManager() {
        shareFileFragment?.takeIf { it.isAdded }?.run {
            refreshCapabilitiesFromDB()
            refreshSharesFromDB()
        }
    }

    private val shareFileFragment: FileDetailSharingFragment?
        get() = supportFragmentManager.findFragmentByTag(TAG_SHARE_FRAGMENT) as? FileDetailSharingFragment

    private fun navigateToParentFolder(user: User) {
        val file = file ?: run {
            finish()
            return
        }
        val parentFolder = if (file.isFolder) {
            file
        } else {
            storageManager.getFileByDecryptedRemotePath(file.parentRemotePath)
        }

        if (parentFolder == null) {
            finish()
            return
        }

        Intent(this, FileDisplayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(EXTRA_FILE, parentFolder)
            putExtra(EXTRA_USER, user)
        }.also {
            startActivity(it)
            finish()
        }
    }

    companion object {
        private val TAG: String = ShareActivity::class.java.simpleName
        const val TAG_SHARE_FRAGMENT: String = "SHARE_FRAGMENT"
    }
}
