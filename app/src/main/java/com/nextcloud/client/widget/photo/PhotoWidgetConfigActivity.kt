/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Axel
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.widget.photo

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.google.android.material.chip.ChipGroup
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
 * Uses a modern bottom-sheet style UI (ConstraintLayout) for configuration.
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

    // UI Elements
    private lateinit var folderPathText: TextView
    private lateinit var folderChangeBtn: Button
    private lateinit var intervalChipGroup: ChipGroup
    private lateinit var addWidgetBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_widget_config)

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

        bindViews()
        setupListeners()
        restoreState()
    }

    private fun bindViews() {
        folderPathText = findViewById(R.id.folder_path)
        folderChangeBtn = findViewById(R.id.folder_change_btn)
        intervalChipGroup = findViewById(R.id.interval_chip_group)
        addWidgetBtn = findViewById(R.id.add_widget_btn)
    }

    private fun setupListeners() {
        folderChangeBtn.setOnClickListener {
            val intent = Intent(this, FolderPickerActivity::class.java).apply {
                putExtra(FolderPickerActivity.EXTRA_ACTION, FolderPickerActivity.CHOOSE_LOCATION)
            }
            @Suppress("DEPRECATION")
            startActivityForResult(intent, REQUEST_FOLDER_PICKER)
        }

        intervalChipGroup.setOnCheckedChangeListener { _, _ ->
            checkValidation()
        }

        addWidgetBtn.setOnClickListener {
            finishConfiguration()
        }
    }

    private fun restoreState() {
        val existingConfig = viewModel.getExistingConfig()
        
        // Restore Folder
        if (existingConfig != null) {
            // We only describe the path here since we don't have the full OCFile object yet without querying.
            // But we can simulate selection if needed, or just set the text.
            // For robust editing, ideally we would fetch the OCFile, but standard flow usually picks new.
            // For now, let's just trigger picker if it's a new widget.
            folderPathText.text = existingConfig.folderPath
            addWidgetBtn.text = getString(R.string.common_save) // "Save" vs "Add"
            
            // Restore Interval
            val interval = existingConfig.intervalMinutes
            val chipId = when (interval) {
                15L -> R.id.chip_15m
                30L -> R.id.chip_30m
                60L -> R.id.chip_1h
                0L -> R.id.chip_manual
                else -> R.id.chip_15m // Default fallback
            }
            intervalChipGroup.check(chipId)
        } else {
            // New Widget Default state
            folderPathText.text = getString(R.string.photo_widget_select_folder)
            intervalChipGroup.check(R.id.chip_15m) // Default 15m
            
            // Immediately launch picker for better UX on fresh add
            folderChangeBtn.performClick()
        }
        
        checkValidation()
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_FOLDER_PICKER && resultCode == RESULT_OK && data != null) {
            val folder: OCFile? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                data.getParcelableExtra(FolderPickerActivity.EXTRA_FOLDER, OCFile::class.java)
            } else {
                data.getParcelableExtra(FolderPickerActivity.EXTRA_FOLDER)
            }

            if (folder != null) {
                viewModel.setSelectedFolder(folder)
                folderPathText.text = folder.remotePath
                checkValidation()
            }
        } else if (requestCode == REQUEST_FOLDER_PICKER && viewModel.getSelectedFolder() == null && viewModel.getExistingConfig() == null) {
            // If user cancelled picker on first launch AND no existing config, finish activity
            finish()
        }
    }

    private fun checkValidation() {
        val hasFolder = viewModel.getSelectedFolder() != null || viewModel.getExistingConfig() != null
        val hasInterval = intervalChipGroup.checkedChipId != -1
        addWidgetBtn.isEnabled = hasFolder && hasInterval
    }

    private fun finishConfiguration() {
        // Resolve interval
        val intervalMinutes = when (intervalChipGroup.checkedChipId) {
            R.id.chip_15m -> 15L
            R.id.chip_30m -> 30L
            R.id.chip_1h -> 60L
            R.id.chip_manual -> 0L
            else -> 15L
        }

        // Resolve folder path (prefer new selection, fallback to existing config)
        val selectedFolder = viewModel.getSelectedFolder()
        val folderPath = selectedFolder?.remotePath ?: viewModel.getExistingConfig()?.folderPath
        
        if (folderPath == null) return

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
