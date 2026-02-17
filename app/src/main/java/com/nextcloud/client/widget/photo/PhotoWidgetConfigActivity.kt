/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Axel
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.widget.photo

import android.app.Activity
import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.nextcloud.client.account.UserAccountManager
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.activity.FolderPickerActivity
import dagger.android.AndroidInjection
import javax.inject.Inject

/**
 * Configuration activity launched when the user places a Photo Widget
 * or reconfigures an existing one (Android 12+).
 *
 * Opens [FolderPickerActivity] for folder selection, then shows an interval
 * picker dialog, saves the config, and triggers an immediate widget update.
 */
@Suppress("TooManyFunctions")
class PhotoWidgetConfigActivity : Activity() {

    companion object {
        private const val REQUEST_FOLDER_PICKER = 1001
    }

    @Inject
    lateinit var viewModel: PhotoWidgetConfigViewModel

    @Inject
    lateinit var userAccountManager: UserAccountManager

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        // Set result to CANCELED in case the user backs out
        setResult(RESULT_CANCELED)

        // Extract the widget ID from the intent
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        viewModel.setWidgetId(appWidgetId)

        // Launch FolderPickerActivity for folder selection
        val folderPickerIntent = Intent(this, FolderPickerActivity::class.java).apply {
            putExtra(FolderPickerActivity.EXTRA_ACTION, FolderPickerActivity.CHOOSE_LOCATION)
        }
        @Suppress("DEPRECATION")
        startActivityForResult(folderPickerIntent, REQUEST_FOLDER_PICKER)
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_FOLDER_PICKER && resultCode == RESULT_OK && data != null) {
            val folder: OCFile? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                data.getParcelableExtra(FolderPickerActivity.EXTRA_FOLDER, OCFile::class.java)
            } else {
                @Suppress("DEPRECATION")
                data.getParcelableExtra(FolderPickerActivity.EXTRA_FOLDER)
            }

            if (folder != null) {
                viewModel.setSelectedFolder(folder)
                showIntervalPicker(folder)
            } else {
                finish()
            }
        } else {
            finish()
        }
    }

    /**
     * Shows a dialog for the user to pick a refresh interval.
     * Presets: 5 / 15 / 30 / 60 minutes / Manual only.
     */
    private fun showIntervalPicker(selectedFolder: OCFile) {
        val labels = arrayOf(
            getString(R.string.photo_widget_interval_5),
            getString(R.string.photo_widget_interval_15),
            getString(R.string.photo_widget_interval_30),
            getString(R.string.photo_widget_interval_60),
            getString(R.string.photo_widget_interval_manual)
        )
        val values = PhotoWidgetConfig.INTERVAL_OPTIONS // [5, 15, 30, 60, 0]

        // Default selection: 15 minutes (index 1)
        var selectedIndex = 1

        // If reconfiguring, use existing interval as default
        val existingConfig = viewModel.getExistingConfig()
        if (existingConfig != null) {
            val existingIndex = values.indexOf(existingConfig.intervalMinutes)
            if (existingIndex >= 0) {
                selectedIndex = existingIndex
            }
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.photo_widget_interval_title))
            .setSingleChoiceItems(labels, selectedIndex) { _, which ->
                selectedIndex = which
            }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val intervalMinutes = values[selectedIndex]
                finishConfiguration(selectedFolder, intervalMinutes)
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                finish()
            }
            .setOnCancelListener {
                finish()
            }
            .show()
    }

    private fun finishConfiguration(folder: OCFile, intervalMinutes: Long) {
        val folderPath = folder.remotePath
        val accountName = userAccountManager.user.accountName

        // Delegate all business logic to ViewModel
        viewModel.saveConfiguration(folderPath, accountName, intervalMinutes)

        // Return success
        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(RESULT_OK, resultValue)
        finish()
    }
}
