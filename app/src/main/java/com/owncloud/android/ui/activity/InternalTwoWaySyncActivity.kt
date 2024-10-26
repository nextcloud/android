/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Tobias Kaminsky <tobias.kaminsky@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.activity

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.model.DurationOption
import com.nextcloud.utils.extensions.setVisibleIf
import com.owncloud.android.R
import com.owncloud.android.databinding.InternalTwoWaySyncLayoutBinding
import com.owncloud.android.ui.adapter.InternalTwoWaySyncAdapter
import javax.inject.Inject

class InternalTwoWaySyncActivity : BaseActivity(), Injectable {
    lateinit var binding: InternalTwoWaySyncLayoutBinding

    @Inject
    lateinit var backgroundJobManager: BackgroundJobManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = InternalTwoWaySyncLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setVisibilities()
        setupTwoWaySyncInterval()
        setupTwoWaySyncToggle()
        setupList()
    }

    private fun setupTwoWaySyncInterval() {
        val durations = DurationOption.twoWaySyncIntervals(this)
        val selectedDuration = durations.find { it.value == preferences.twoWaySyncInterval }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            durations.map { it.displayText }
        )

        binding.twoWaySyncInterval.run {
            setAdapter(adapter)
            setText(selectedDuration?.displayText ?: getString(R.string.two_way_sync_interval_15_min), false)
            setOnItemClickListener { _, _, position, _ ->
                handleDurationSelected(durations[position].value)
            }
        }
    }

    private fun handleDurationSelected(duration: Long) {
        preferences.twoWaySyncInterval = duration
        backgroundJobManager.scheduleInternal2WaySync(duration)
    }

    private fun setupTwoWaySyncToggle() {
        binding.twoWaySyncToggle.isChecked = preferences.twoWaySyncStatus
        binding.twoWaySyncToggle.setOnCheckedChangeListener { _, isChecked ->
            preferences.twoWaySyncStatus = isChecked
            setupList()
            setVisibilities()
        }
    }

    private fun setupList() {
        if (preferences.twoWaySyncStatus) {
            binding.list.apply {
                adapter = InternalTwoWaySyncAdapter(fileDataStorageManager, user.get(), context)
                layoutManager = LinearLayoutManager(context)
            }
        }
    }

    private fun setVisibilities() {
        binding.list.setVisibleIf(preferences.twoWaySyncStatus)
        binding.twoWaySyncIntervalLayout.setVisibleIf(preferences.twoWaySyncStatus)
    }
}
