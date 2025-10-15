/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Tobias Kaminsky <tobias.kaminsky@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.client.jobs.download.FileDownloadWorker
import com.nextcloud.utils.extensions.hourPlural
import com.nextcloud.utils.extensions.minPlural
import com.nextcloud.utils.extensions.setVisibleIf
import com.owncloud.android.R
import com.owncloud.android.databinding.InternalTwoWaySyncLayoutBinding
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.adapter.InternalTwoWaySyncAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class InternalTwoWaySyncActivity :
    DrawerActivity(),
    Injectable,
    InternalTwoWaySyncAdapter.InternalTwoWaySyncAdapterOnUpdate {
    private val tag = "InternalTwoWaySyncActivity"

    @Inject
    lateinit var backgroundJobManager: BackgroundJobManager

    lateinit var binding: InternalTwoWaySyncLayoutBinding

    private lateinit var internalTwoWaySyncAdapter: InternalTwoWaySyncAdapter
    private var disableForAllFoldersMenuButton: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        internalTwoWaySyncAdapter = InternalTwoWaySyncAdapter(fileDataStorageManager, user.get(), this, this)

        binding = InternalTwoWaySyncLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupActionBar()
        setupTwoWaySyncAdapter()
        setupEmptyList()
        setupTwoWaySyncToggle()
        setupTwoWaySyncInterval()
        checkLayoutVisibilities(preferences.isTwoWaySyncEnabled)
    }

    private fun setupActionBar() {
        updateActionBarTitleAndHomeButtonByString(getString(R.string.two_way_sync_activity_title))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setupTwoWaySyncAdapter() {
        if (preferences.isTwoWaySyncEnabled) {
            binding.run {
                list.run {
                    setEmptyView(emptyList.emptyListView)
                    adapter = internalTwoWaySyncAdapter
                    layoutManager = LinearLayoutManager(this@InternalTwoWaySyncActivity)
                    adapter?.notifyDataSetChanged()
                }
            }
        }
    }

    private fun setupEmptyList() {
        binding.emptyList.run {
            emptyListViewHeadline.run {
                visibility = View.VISIBLE
                setText(R.string.two_way_sync_activity_empty_list_title)
            }

            emptyListViewText.run {
                visibility = View.VISIBLE
                setText(R.string.two_way_sync_activity_empty_list_desc)
            }

            emptyListIcon.run {
                visibility = View.VISIBLE
                setImageDrawable(
                    viewThemeUtils.platform.tintDrawable(
                        context,
                        R.drawable.ic_sync,
                        ColorRole.PRIMARY
                    )
                )
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun disableTwoWaySyncAndWorkers() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                backgroundJobManager.cancelTwoWaySyncJob()

                val currentUser = user.get()

                val folders = fileDataStorageManager.getInternalTwoWaySyncFolders(currentUser)
                folders.forEach { folder ->
                    FileDownloadWorker.cancelOperation(currentUser.accountName, folder.fileId)
                    backgroundJobManager.cancelFilesDownloadJob(currentUser, folder.fileId)

                    folder.internalFolderSyncTimestamp = -1L
                    fileDataStorageManager.saveFile(folder)
                }

                withContext(Dispatchers.Main) {
                    internalTwoWaySyncAdapter.update()
                }
            } catch (e: Exception) {
                Log_OC.d(tag, "Error caught at disableTwoWaySyncAndWorkers: $e")
            }
        }
    }

    @Suppress("MagicNumber")
    private fun setupTwoWaySyncInterval() {
        val durations = listOf(
            15.minutes to minPlural(15),
            30.minutes to minPlural(30),
            45.minutes to minPlural(45),
            1.hours to hourPlural(1),
            2.hours to hourPlural(2),
            4.hours to hourPlural(4),
            6.hours to hourPlural(6),
            8.hours to hourPlural(8),
            12.hours to hourPlural(12),
            24.hours to hourPlural(24)
        )
        val selectedDuration = durations.find { it.first.inWholeMinutes == preferences.twoWaySyncInterval }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            durations.map { it.second }
        )

        binding.twoWaySyncInterval.run {
            setAdapter(adapter)
            setText(selectedDuration?.second ?: minPlural(15), false)
            setOnItemClickListener { _, _, position, _ ->
                handleDurationSelected(durations[position].first.inWholeMinutes)
            }
        }
    }

    private fun handleDurationSelected(duration: Long) {
        preferences.twoWaySyncInterval = duration
        backgroundJobManager.scheduleInternal2WaySync(duration)
    }

    private fun setupTwoWaySyncToggle() {
        binding.twoWaySyncToggle.isChecked = preferences.isTwoWaySyncEnabled
        binding.twoWaySyncToggle.setOnCheckedChangeListener { _, isChecked ->
            preferences.setTwoWaySyncStatus(isChecked)
            setupTwoWaySyncAdapter()
            checkLayoutVisibilities(isChecked)
            checkDisableForAllFoldersMenuButtonVisibility()

            if (isChecked) {
                backgroundJobManager.scheduleInternal2WaySync(preferences.twoWaySyncInterval)
            } else {
                backgroundJobManager.cancelTwoWaySyncJob()
            }
        }
    }

    private fun checkLayoutVisibilities(condition: Boolean) {
        binding.listFrameLayout.setVisibleIf(condition)
        binding.twoWaySyncIntervalLayout.setVisibleIf(condition)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_internal_two_way_sync, menu)
        disableForAllFoldersMenuButton = menu?.findItem(R.id.action_dismiss_two_way_sync)
        checkDisableForAllFoldersMenuButtonVisibility()
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
            }
            R.id.action_dismiss_two_way_sync -> {
                disableTwoWaySyncAndWorkers()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun checkDisableForAllFoldersMenuButtonVisibility() {
        lifecycleScope.launch {
            val folderSize = withContext(Dispatchers.IO) {
                fileDataStorageManager.getInternalTwoWaySyncFolders(user.get()).size
            }

            checkDisableForAllFoldersMenuButtonVisibility(preferences.isTwoWaySyncEnabled, folderSize)
        }
    }

    private fun checkDisableForAllFoldersMenuButtonVisibility(isTwoWaySyncEnabled: Boolean, folderSize: Int) {
        val showDisableButton = isTwoWaySyncEnabled && folderSize > 0

        disableForAllFoldersMenuButton?.let {
            it.setVisible(showDisableButton)
            it.setEnabled(showDisableButton)
        }
    }

    override fun onUpdate(folderSize: Int) {
        checkDisableForAllFoldersMenuButtonVisibility(preferences.isTwoWaySyncEnabled, folderSize)
    }
}
