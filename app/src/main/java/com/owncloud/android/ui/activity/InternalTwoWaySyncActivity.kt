/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Tobias Kaminsky <tobias.kaminsky@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.activity

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.nextcloud.client.di.Injectable
import com.nextcloud.utils.extensions.setVisibleIf
import com.owncloud.android.databinding.InternalTwoWaySyncLayoutBinding
import com.owncloud.android.ui.adapter.InternalTwoWaySyncAdapter

class InternalTwoWaySyncActivity : BaseActivity(), Injectable {
    lateinit var binding: InternalTwoWaySyncLayoutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = InternalTwoWaySyncLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTwoWaySyncToggle()
        setupList()
    }

    private fun setupTwoWaySyncToggle() {
        binding.twoWaySyncToggle.isChecked = preferences.twoWayInternalSyncStatus
        binding.twoWaySyncToggle.setOnCheckedChangeListener { _, isChecked ->
            preferences.twoWayInternalSyncStatus = isChecked
            setupList()
        }
    }

    private fun setupList() {
        binding.list.setVisibleIf(preferences.twoWayInternalSyncStatus)

        if (preferences.twoWayInternalSyncStatus) {
            binding.list.apply {
                adapter = InternalTwoWaySyncAdapter(fileDataStorageManager, user.get(), context)
                layoutManager = LinearLayoutManager(context)
            }
        }
    }
}
