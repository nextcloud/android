/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Axel
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.widget.photo

import androidx.lifecycle.ViewModel
import com.nextcloud.client.jobs.BackgroundJobManager
import com.owncloud.android.datamodel.OCFile
import javax.inject.Inject

/**
 * ViewModel for [PhotoWidgetConfigActivity].
 *
 * Manages folder selection state, interval selection, and config saving.
 * Separates business logic from the Activity lifecycle.
 */
class PhotoWidgetConfigViewModel @Inject constructor(
    private val photoWidgetRepository: PhotoWidgetRepository,
    private val backgroundJobManager: BackgroundJobManager
) : ViewModel() {

    private var selectedFolder: OCFile? = null
    private var widgetId: Int = -1

    fun setWidgetId(id: Int) {
        widgetId = id
    }

    fun getWidgetId(): Int = widgetId

    fun setSelectedFolder(folder: OCFile) {
        selectedFolder = folder
    }

    fun getSelectedFolder(): OCFile? = selectedFolder

    /**
     * Saves the widget configuration and schedules update jobs.
     *
     * @param folderPath The remote path of the selected folder
     * @param accountName The account name of the user
     * @param intervalMinutes The refresh interval in minutes (0 = manual only)
     */
    fun saveConfiguration(folderPath: String, accountName: String, intervalMinutes: Long) {
        val config = PhotoWidgetConfig(widgetId, folderPath, accountName, intervalMinutes)
        photoWidgetRepository.saveWidgetConfig(config)
        backgroundJobManager.schedulePeriodicPhotoWidgetUpdate(intervalMinutes)
        backgroundJobManager.startImmediatePhotoWidgetUpdate()
    }

    /**
     * Returns the existing config for this widget, if any.
     * Useful for reconfigure flow (Android 12+).
     */
    fun getExistingConfig(): PhotoWidgetConfig? {
        return photoWidgetRepository.getWidgetConfig(widgetId)
    }
}
