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
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuProvider
import androidx.lifecycle.lifecycleScope
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.client.jobs.download.FileDownloadWorker
import com.owncloud.android.R
import com.nextcloud.utils.extensions.hourPlural
import com.nextcloud.utils.extensions.minPlural
import com.nextcloud.utils.extensions.setVisibleIf
import com.owncloud.android.databinding.InternalTwoWaySyncLayoutBinding
import com.owncloud.android.ui.adapter.InternalTwoWaySyncAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class InternalTwoWaySyncActivity : DrawerActivity(), Injectable {
    @Inject
    lateinit var backgroundJobManager: BackgroundJobManager

    lateinit var binding: InternalTwoWaySyncLayoutBinding

    private lateinit var internalTwoWaySyncAdapter: InternalTwoWaySyncAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        internalTwoWaySyncAdapter = InternalTwoWaySyncAdapter(fileDataStorageManager, user.get(), this)

        binding = InternalTwoWaySyncLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupActionBar()
        setupMenuProvider()
        setupTwoWaySyncAdapter()
        setupEmptyList()
        setupTwoWaySyncToggle()
        setupTwoWaySyncInterval()
        setVisibilities(preferences.isTwoWaySyncEnabled)
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

    private fun disableTwoWaySyncAndWorkers() {
        lifecycleScope.launch(Dispatchers.IO) {
            backgroundJobManager.cancelTwoWaySyncJob()

            val folders = fileDataStorageManager.getInternalTwoWaySyncFolders(user.get())
            folders.forEach { folder ->
                FileDownloadWorker.cancelOperation(user.get().accountName, folder.fileId)
                backgroundJobManager.cancelFilesDownloadJob(user.get(), folder.fileId)

                folder.internalFolderSyncTimestamp = -1L
                fileDataStorageManager.saveFile(folder)
            }

            launch(Dispatchers.Main) {
                internalTwoWaySyncAdapter.update()
            }
        }
    }

    private fun setupMenuProvider() {
        addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.activity_internal_two_way_sync, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        android.R.id.home -> {
                            onBackPressed()
                            true
                        }
                        R.id.action_dismiss_two_way_sync -> {
                            disableTwoWaySyncAndWorkers()
                            true
                        }
                        else -> false
                    }
                }
            }
        )
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
            setVisibilities(isChecked)

            if (isChecked) {
                backgroundJobManager.scheduleInternal2WaySync(preferences.twoWaySyncInterval)
            } else {
                backgroundJobManager.cancelInternal2WaySyncJob()
            }
        }
    }

    private fun setVisibilities(condition: Boolean) {
        binding.listFrameLayout.setVisibleIf(condition)
        binding.twoWaySyncIntervalLayout.setVisibleIf(condition)
    }
}
