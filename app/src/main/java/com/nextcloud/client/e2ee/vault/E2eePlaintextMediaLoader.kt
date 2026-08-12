/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.e2ee.vault

import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.OwnCloudClient

interface E2eePlaintextMediaLoader {
    fun <T> withPlaintext(file: OCFile, parent: OCFile, client: OwnCloudClient, block: (ByteArray) -> T?): T?
}
