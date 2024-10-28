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
import androidx.recyclerview.widget.LinearLayoutManager
import com.nextcloud.client.di.Injectable
import com.owncloud.android.R
import com.owncloud.android.databinding.InternalTwoWaySyncLayoutBinding
import com.owncloud.android.ui.adapter.InternalTwoWaySyncAdapter

class InternalTwoWaySyncActivity : DrawerActivity(), Injectable {
    lateinit var binding: InternalTwoWaySyncLayoutBinding

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = InternalTwoWaySyncLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.run {
            list.run {
                setEmptyView(binding.emptyList.emptyListView)

                emptyList.run {
                    emptyListViewHeadline.apply {
                        visibility = View.VISIBLE
                        setText(R.string.internal_two_way_sync_list_empty_headline)
                    }
                    emptyList.emptyListViewText.apply {
                        visibility = View.VISIBLE
                        setText(R.string.internal_two_way_sync_text)
                    }
                    emptyList.emptyListIcon.apply {
                        visibility = View.VISIBLE
                        setImageDrawable(
                            viewThemeUtils.platform.tintPrimaryDrawable(
                                context,
                                R.drawable.ic_sync
                            )
                        )
                    }
                }

                adapter = InternalTwoWaySyncAdapter(fileDataStorageManager, user.get(), context).apply {
                    notifyDataSetChanged()
                }
                layoutManager = LinearLayoutManager(context)
            }
        }

        setupToolbar()
        updateActionBarTitleAndHomeButtonByString(getString(R.string.internal_two_way_sync_headline))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        android.R.id.home -> {
                            onBackPressed()
                            true
                        }

                        else -> false
                    }
                }
            }
        )
    }
}
