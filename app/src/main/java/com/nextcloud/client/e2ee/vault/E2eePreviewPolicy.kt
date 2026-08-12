/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.e2ee.vault

import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.files.model.ServerFileInterface

object E2eePreviewPolicy {
    @JvmStatic
    fun isServerPreviewAllowed(file: ServerFileInterface): Boolean = file !is OCFile || !file.isEncrypted
}
