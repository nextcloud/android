/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.fragment.albums

import android.content.res.Resources
import com.owncloud.android.R
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.albums.CreateNewAlbumRemoteOperation
import com.owncloud.android.lib.resources.albums.RemoveAlbumRemoteOperation
import com.owncloud.android.lib.resources.albums.RenameAlbumRemoteOperation
import com.owncloud.android.operations.albums.CopyFileToAlbumOperation
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.ErrorMessageAdapter

class AlbumOperationListener(private val activity: FileDisplayActivity) {

    companion object {
        private const val TAG = "AlbumOperationListener"
    }

    fun onRemoveAlbumOperationFinish(operation: RemoveAlbumRemoteOperation, result: RemoteOperationResult<*>) {
        if (result.isSuccess) {
            val fragment = activity.supportFragmentManager.findFragmentByTag(AlbumItemsFragment.TAG)
            if (fragment is AlbumItemsFragment) {
                fragment.onAlbumDeleted()
            }
        } else {
            showErrorMessage(operation, result)
            showUntrustedCertDialog(result)
        }
    }

    fun onCopyAlbumFileOperationFinish(operation: CopyFileToAlbumOperation, result: RemoteOperationResult<*>) {
        if (result.isSuccess) {
            // when item added from inside of Album
            val fragment = activity.supportFragmentManager.findFragmentByTag(AlbumItemsFragment.TAG)
            if (fragment is AlbumItemsFragment) {
                fragment.refreshData()
            } else {
                // files added directly from Media tab
                DisplayUtils.showSnackMessage(
                    activity,
                    activity.getResources().getString(R.string.album_file_added_message)
                )
            }
            Log_OC.e(TAG, "Files copied successfully")
        } else {
            try {
                showErrorMessage(operation, result)
            } catch (e: Resources.NotFoundException) {
                Log_OC.e(TAG, "Error while trying to show fail message ", e)
            }
        }
    }

    fun onRenameAlbumOperationFinish(operation: RenameAlbumRemoteOperation, result: RemoteOperationResult<*>) {
        if (result.isSuccess) {
            val fragment = activity.supportFragmentManager.findFragmentByTag(AlbumItemsFragment.TAG)
            if (fragment is AlbumItemsFragment) {
                fragment.onAlbumRenamed(operation.newAlbumName)
            }
        } else {
            showErrorMessage(operation, result)
            showUntrustedCertDialog(result)
        }
    }

    fun onCreateAlbumOperationFinish(
        operation: CreateNewAlbumRemoteOperation,
        result: RemoteOperationResult<*>
    ) {
        if (result.isSuccess) {
            val fragment = activity.supportFragmentManager.findFragmentByTag(AlbumsFragment.TAG)
            if (fragment is AlbumsFragment) {
                fragment.navigateToAlbumItemsFragment(operation.newAlbumName, true)
            }
        } else {
            try {
                if (RemoteOperationResult.ResultCode.FOLDER_ALREADY_EXISTS == result.code) {
                    DisplayUtils.showSnackMessage(activity, R.string.album_already_exists)
                } else {
                    showErrorMessage(operation, result)
                }
            } catch (e: Resources.NotFoundException) {
                Log_OC.e(TAG, "Error while trying to show fail message ", e)
            }
        }
    }

    private fun showUntrustedCertDialog(result: RemoteOperationResult<*>) {
        if (result.isSslRecoverableException) {
            activity.mLastSslUntrustedServerResult = result
            activity.showUntrustedCertDialog(activity.mLastSslUntrustedServerResult)
        }
    }

    private fun showErrorMessage(operation: RemoteOperation<*>, result: RemoteOperationResult<*>) {
        DisplayUtils.showSnackMessage(
            activity,
            ErrorMessageAdapter.getErrorCauseMessage(result, operation, activity.getResources())
        )
    }
}
