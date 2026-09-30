/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.fragment.albums.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.owncloud.android.R
import com.owncloud.android.lib.common.operations.RemoteOperationResult

enum class AlbumItemsEmptyState(@StringRes val headline: Int, @StringRes val message: Int, @DrawableRes val icon: Int) {
    NO_ITEMS(
        R.string.file_list_empty_headline_server_search,
        R.string.file_list_empty_gallery,
        R.drawable.file_image
    ),
    NO_CONNECTION(
        R.string.file_list_error_headline,
        R.string.file_list_error_description,
        R.drawable.ic_no_internet
    ),
    MAINTENANCE(
        R.string.maintenance_mode,
        R.string.file_list_error_description,
        R.drawable.ic_alert
    ),
    LOAD_FAILED(
        R.string.common_error,
        R.string.unexpected_error_occurred,
        R.drawable.ic_alert
    );

    companion object {
        fun fromFailure(result: RemoteOperationResult<*>?): AlbumItemsEmptyState = when (result?.code) {
            RemoteOperationResult.ResultCode.NO_NETWORK_CONNECTION,
            RemoteOperationResult.ResultCode.WRONG_CONNECTION,
            RemoteOperationResult.ResultCode.HOST_NOT_AVAILABLE,
            RemoteOperationResult.ResultCode.TIMEOUT -> NO_CONNECTION

            RemoteOperationResult.ResultCode.MAINTENANCE_MODE -> MAINTENANCE

            else -> LOAD_FAILED
        }
    }
}
