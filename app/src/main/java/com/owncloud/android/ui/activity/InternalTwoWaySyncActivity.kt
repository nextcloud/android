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
import com.nextcloud.utils.extensions.hourPlural
import com.nextcloud.utils.extensions.minPlural
import com.nextcloud.utils.extensions.setVisibleIf
import com.owncloud.android.databinding.InternalTwoWaySyncLayoutBinding
import com.owncloud.android.ui.adapter.InternalTwoWaySyncAdapter
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

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
            setupList()
            setVisibilities()

            if (isChecked) {
                backgroundJobManager.scheduleInternal2WaySync(preferences.twoWaySyncInterval)
            } else {
                backgroundJobManager.cancelInternal2WaySyncJob()
            }
        }
    }

    private fun setupList() {
        if (preferences.isTwoWaySyncEnabled) {
            binding.list.apply {
                adapter = InternalTwoWaySyncAdapter(fileDataStorageManager, user.get(), context)
                layoutManager = LinearLayoutManager(context)
            }
        }
    }

    private fun setVisibilities() {
        binding.list.setVisibleIf(preferences.isTwoWaySyncEnabled)
        binding.twoWaySyncIntervalLayout.setVisibleIf(preferences.isTwoWaySyncEnabled)
    }
}
