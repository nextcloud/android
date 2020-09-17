/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.unifiedsearch

import com.nextcloud.client.account.CurrentAccountProvider
import com.nextcloud.client.core.AsyncRunner
import com.nextcloud.client.network.ClientFactory
import com.owncloud.android.lib.common.utils.Log_OC


class UnifiedSearchRemoteRepository(private val clientFactory: ClientFactory,
                                    private val currentAccountProvider: CurrentAccountProvider,
                                    private val asyncRunner: AsyncRunner) : IUnifiedSearchRepository {
    override fun refresh() {
        TODO("Not yet implemented")
    }

    override fun startLoading() {
        TODO("Not yet implemented")
    }

    override fun loadMore(query: String, vm: UnifiedSearchViewModel) {
        Log_OC.d(this, "loadMore")
        val client = clientFactory.createNextcloudClient(currentAccountProvider.user)
        val task = SearchOnProviderTask(query, "files", client)
        asyncRunner.postQuickTask(task, onResult = vm::onSearchResult, onError = vm::onError)
    }
}
