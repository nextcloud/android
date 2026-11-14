/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations.upload

import com.owncloud.android.lib.common.operations.RemoteOperationResult

class UploadFolderException(val result: RemoteOperationResult<*>) : Exception(result.logMessage)
