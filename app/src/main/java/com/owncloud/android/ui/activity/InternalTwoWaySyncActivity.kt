/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.activity

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.nextcloud.client.di.Injectable
import com.owncloud.android.databinding.InternalTwoWaySyncLayoutBinding
import com.owncloud.android.ui.adapter.InternalTwoWaySyncAdapter

class InternalTwoWaySyncActivity : BaseActivity(), Injectable {
    lateinit var binding: InternalTwoWaySyncLayoutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = InternalTwoWaySyncLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.list.apply {
            adapter = InternalTwoWaySyncAdapter(fileDataStorageManager, user.get(), context)
            layoutManager = LinearLayoutManager(context)
        }
    }
}
