/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.e2ee.model

import androidx.annotation.StringRes
import com.owncloud.android.R

enum class E2EEDialog(@param:StringRes val titleId: Int, @param:StringRes val descriptionId: Int) {
    KeysRevokedOrChangedWhenEncrypting(
        titleId = R.string.encryption_key_handle_key_removed_title,
        descriptionId = R.string.encryption_key_handle_setup_new_key_before_encrypting_description
    ),

    KeysLocallyOutdated(
        titleId = R.string.encryption_key_handle_key_removed_title,
        descriptionId = R.string.encryption_key_handle_key_keys_only_on_device_outdated_description
    ),

    KeysOnlyExistsOnDeviceWhenCreatingFolder(
        titleId = R.string.encryption_key_handle_key_removed_title,
        descriptionId = R.string.encryption_key_handle_key_keys_only_on_device_description
    ),

    KeysUnusableWhenOpeningFolder(
        titleId = R.string.encryption_key_handle_key_removed_title,
        descriptionId = R.string.encryption_key_handle_folder_read_only_description
    )
}
