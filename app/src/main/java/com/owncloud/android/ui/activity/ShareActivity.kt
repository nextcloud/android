/*
 * ownCloud Android client application
 *
 * @author masensio
 * @author David A. Velasco
 * @author Juan Carlos González Cabrero
 * Copyright (C) 2015 ownCloud Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.activity

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.owncloud.android.R
import com.owncloud.android.databinding.ShareActivityBinding
import com.owncloud.android.datamodel.SyncedFolderProvider
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
import com.owncloud.android.ui.fragment.FileDetailsSharingProcessFragment.Companion.newInstance
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.MimeTypeUtil
import javax.inject.Inject

/**
 * Activity for sharing files.
 */
class ShareActivity : FileActivity() {

    @JvmField
    @Inject
    var syncedFolderProvider: SyncedFolderProvider? = null

    private lateinit var binding: ShareActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ShareActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val optionalUser = user
        if (!optionalUser.isPresent) {
            finish()
            return
        }

        setupIcon()
        setupFileInfo()
        readFile()

        if (savedInstanceState == null) {
            addShareFragment()
        }
    }

    private fun setupIcon() {
        if (file.isFolder) {
            val isAutoUploadFolder =
                SyncedFolderProvider.isAutoUploadFolder(syncedFolderProvider, file, user.get())
            val overlayIconId = file.getFileOverlayIconId(isAutoUploadFolder)
            val drawable = MimeTypeUtil.getFileIcon(preferences.isDarkModeEnabled, overlayIconId, this, viewThemeUtils)
            binding.shareFileIcon.setImageDrawable(drawable)
        } else {
            binding.shareFileIcon.setImageDrawable(
                MimeTypeUtil.getFileTypeIcon(
                    file.mimeType,
                    file.fileName,
                    this,
                    viewThemeUtils
                )
            )
            if (MimeTypeUtil.isImage(file)) {
                val remoteId = ThumbnailsCacheManager.PREFIX_THUMBNAIL + file.remoteId
                val thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(remoteId)
                if (thumbnail != null) {
                    binding.shareFileIcon.setImageBitmap(thumbnail)
                }
            }
        }
    }

    private fun setupFileInfo() {
        binding.shareFileName.text = resources.getString(R.string.share_file, file.fileName)
        viewThemeUtils.platform.colorViewBackground(binding.shareHeaderDivider)
        binding.shareFileSize.text = DisplayUtils.bytesToHumanReadable(file.fileLength)
    }

    private fun readFile() {
        Thread {
            val result = ReadFileRemoteOperation(file.remotePath).execute(user.get(), this)
            if (result.isSuccess) {
                val remoteFile = result.data[0] as RemoteFile
                val length = remoteFile.length
                file.fileLength = length
                runOnUiThread { binding.shareFileSize.text = DisplayUtils.bytesToHumanReadable(length) }
            }
        }.start()
    }

    private fun addShareFragment() {
        val ft = supportFragmentManager.beginTransaction()
        val fragment: Fragment = FileDetailSharingFragment.newInstance(file, user.get())
        ft.replace(R.id.share_fragment_container, fragment, TAG_SHARE_FRAGMENT)
        ft.commit()
    }

    override fun onStart() {
        super.onStart()

        Log_OC.d(TAG, "Refreshing lists on account set")
        refreshSharesFromStorageManager()
    }

    override fun doShareWith(shareeName: String, shareType: ShareType) {
        supportFragmentManager.beginTransaction().replace(
            R.id.share_fragment_container,
            newInstance(
                file,
                shareeName,
                shareType
            ),
            FileDetailsSharingProcessFragment.TAG
        ).commit()
    }

    /**
     * Updates the view associated to the activity after the finish of some operation over files in the current
     * account.
     *
     * @param operation Removal operation performed.
     * @param result    Result of the removal.
     */
    override fun onRemoteOperationFinish(operation: RemoteOperation<*>?, result: RemoteOperationResult<*>) {
        super.onRemoteOperationFinish(operation, result)

        if (result.isSuccess || operation is GetSharesForFileOperation &&
            result.code == RemoteOperationResult.ResultCode.SHARE_NOT_FOUND
        ) {
            Log_OC.d(TAG, "Refreshing view on successful operation or finished refresh")
            refreshSharesFromStorageManager()
        }
    }

    /**
     * Updates the view, reading data from [com.owncloud.android.datamodel.FileDataStorageManager].
     */
    private fun refreshSharesFromStorageManager() {
        shareFileFragment?.let {
            if (it.isAdded) {
                it.refreshCapabilitiesFromDB()
                it.refreshSharesFromDB()
            }
        }
    }

    private val shareFileFragment: FileDetailSharingFragment?
        get() = supportFragmentManager.findFragmentByTag(TAG_SHARE_FRAGMENT) as FileDetailSharingFragment?

    override fun onShareProcessClosed() {
        finish()
    }

    companion object {
        private val TAG = ShareActivity::class.java.simpleName
        const val TAG_SHARE_FRAGMENT = "SHARE_FRAGMENT"
    }
}
