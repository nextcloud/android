/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.drawer.externallinks

import androidx.annotation.WorkerThread
import com.nextcloud.client.account.User
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.ExternalLinksProvider
import com.owncloud.android.lib.common.ExternalLink
import com.owncloud.android.lib.common.accounts.ExternalLinksOperation
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.GetCapabilitiesOperation
import com.owncloud.android.ui.activity.BaseActivity
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.utils.FilesSyncHelper
import com.owncloud.android.utils.theme.CapabilityUtils
import kotlin.jvm.optionals.getOrNull

class ExternalLinksSynchronizer(
    private val activity: BaseActivity,
    private val externalLinksProvider: ExternalLinksProvider,
    private val appOpenedCountProvider: ArbitraryDataProvider
) {

    @WorkerThread
    fun sync(user: User, force: Boolean) {
        refreshCapabilities()
        syncExternalLinks(user, force)
    }

    private fun refreshCapabilities() {
        val capability = activity.capabilities.getOrNull() ?: return
        if (capability.accountName?.isEmpty() == true) return
        val storageManager = activity.storageManager ?: return
        GetCapabilitiesOperation(storageManager).execute(activity.baseContext)
    }

    private fun syncExternalLinks(user: User, force: Boolean) {
        val isExternalLinksEnabled =
            activity.storageManager != null && CapabilityUtils.getCapability(user, activity).externalLinks.isTrue
        if (!isExternalLinksEnabled) {
            externalLinksProvider.deleteAllExternalLinks()
            Log_OC.d(TAG, "links disabled")
            return
        }

        val appOpenedCount =
            appOpenedCountProvider.getIntegerValue(FilesSyncHelper.GLOBAL, FileActivity.APP_OPENED_COUNT)
        val shouldUpdate = force ||
            appOpenedCount == APP_OPENED_COUNT_NOT_SET ||
            appOpenedCount > APP_OPENED_COUNT_UPDATE_THRESHOLD
        if (!shouldUpdate) {
            storeAppOpenedCount(appOpenedCount + 1)
            return
        }

        if (force) {
            Log_OC.d(TAG, "force update")
        }
        storeAppOpenedCount(0)

        Log_OC.d(TAG, "update via api")
        val result = ExternalLinksOperation().execute(user, activity)
        if (!result.isSuccess) return
        val externalLinks = result.data ?: return

        externalLinksProvider.deleteAllExternalLinks()
        externalLinks.filterIsInstance<ExternalLink>().forEach(externalLinksProvider::storeExternalLink)
    }

    private fun storeAppOpenedCount(count: Int) {
        appOpenedCountProvider.storeOrUpdateKeyValue(FilesSyncHelper.GLOBAL, FileActivity.APP_OPENED_COUNT, "$count")
    }

    companion object {
        private const val TAG = "ExternalLinks"
        private const val APP_OPENED_COUNT_NOT_SET = -1
        private const val APP_OPENED_COUNT_UPDATE_THRESHOLD = 10
    }
}
