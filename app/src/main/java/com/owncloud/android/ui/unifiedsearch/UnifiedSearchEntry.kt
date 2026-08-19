/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.unifiedsearch

import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.SearchResultEntry

data class UnifiedSearchEntry(val searchResult: SearchResultEntry, val localFile: OCFile?)

fun SearchResultEntry.toUnifiedSearchEntry(storageManager: FileDataStorageManager): UnifiedSearchEntry =
    UnifiedSearchEntry(this, storageManager.getFileByRemotePath(remotePath()))
