/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.interfaces

import com.owncloud.android.lib.common.SearchResultEntry
import com.owncloud.android.ui.unifiedsearch.ProviderID

interface UnifiedSearchListInterface {
    fun onSearchResultClicked(searchResultEntry: SearchResultEntry)
    fun onLoadMoreClicked(providerID: ProviderID)
}
