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
import com.owncloud.android.ui.activities.data.activities.ActivitiesRepository
import com.owncloud.android.ui.activities.data.activities.ActivitiesRepository.LoadActivitiesCallback
import com.owncloud.android.ui.activities.data.files.FilesRepository
import com.owncloud.android.ui.activities.data.files.FilesRepository.ReadRemoteFileCallback
import com.owncloud.android.ui.activity.BaseActivity

class ActivitiesPresenter internal constructor(
    private val activitiesRepository: ActivitiesRepository,
    private val filesRepository: FilesRepository,
    private val activitiesView: ActivitiesContract.View
) : ActivitiesContract.ActionListener {
    private var activityStopped = false

    override fun loadActivities(lastGiven: Long) {
        if (ActivitiesContract.ActionListener.UNDEFINED.toLong() == lastGiven) {
            activitiesView.showLoadingMessage()
        } else {
            activitiesView.setProgressIndicatorState(true)
        }
        activitiesRepository.getActivities(
            lastGiven,
            object : LoadActivitiesCallback {
                override fun onActivitiesLoaded(activities: List<Any>, client: NextcloudClient, lastGiven: Long) {
                    if (!activityStopped) {
                        activitiesView.setProgressIndicatorState(false)
                        activitiesView.showActivities(activities, client, lastGiven)
                    }
                }

                override fun onActivitiesLoadedError(error: String) {
                    if (!activityStopped) {
                        activitiesView.setProgressIndicatorState(false)
                        activitiesView.showActivitiesLoadError(error)
                    }
                }
            }
        )
    }

    override fun openActivity(fileUrl: String, baseActivity: BaseActivity) {
        activitiesView.setProgressIndicatorState(true)
        filesRepository.readRemoteFile(
            fileUrl,
            baseActivity,
            object : ReadRemoteFileCallback {
                override fun onFileLoaded(ocFile: OCFile?) {
                    activitiesView.setProgressIndicatorState(false)
                    if (ocFile != null) {
                        activitiesView.showActivityDetailUI(ocFile)
                    } else {
                        activitiesView.showActivityDetailUIIsNull()
                    }
                }

                override fun onFileLoadError(error: String) {
                    activitiesView.setProgressIndicatorState(false)
                    activitiesView.showActivityDetailError(error)
                }
            }
        )
    }

    override fun onStop() {
        activityStopped = true
    }

    override fun onResume() {
        activityStopped = false
    }
}
