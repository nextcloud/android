/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2018 Edvard Holst <edvard.holst@gmail.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activities

import com.nextcloud.common.NextcloudClient
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.activity.BaseActivity
import kotlinx.coroutines.CoroutineScope

interface ActivitiesContract {
    interface View {
        fun showActivities(activities: List<Any>, client: NextcloudClient, lastGiven: Long)
        fun showActivitiesLoadError(error: String)
        fun showActivityDetailUI(ocFile: OCFile)
        fun showActivityDetailUIIsNull()
        fun showActivityDetailError(error: String)
        fun showLoadingMessage()
        fun showEmptyContent(headline: String, message: String)
        fun setProgressIndicatorState(isActive: Boolean)
    }

    interface ActionListener {
        fun loadActivities(lifecycleScope: CoroutineScope, lastGiven: Long)

        fun openActivity(fileUrl: String, baseActivity: BaseActivity)

        fun onStop()

        fun onResume()

        companion object {
            @JvmField
            val UNDEFINED: Int = -1
        }
    }
}
