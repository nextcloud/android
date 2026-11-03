/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.providers.documentstorage.modify

import android.content.Context
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.utils.fileNameValidator.FileNameValidator
import com.owncloud.android.lib.resources.status.OCCapability
import com.owncloud.android.utils.theme.CapabilityUtils

internal class DocumentNameValidator(private val context: Context, private val accountManager: UserAccountManager) {

    private val capabilities: OCCapability
        get() = CapabilityUtils.getCapability(accountManager.user, context)

    fun fileNameError(filename: String): String? = FileNameValidator.checkFileName(filename, capabilities, context)

    fun isValidFolderPath(folderPath: String): Boolean =
        FileNameValidator.checkFolderPath(folderPath, capabilities, context)
}
