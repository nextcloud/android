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
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.client.di.Injectable
import com.owncloud.android.R
import com.owncloud.android.databinding.InternalTwoWaySyncLayoutBinding
import com.owncloud.android.ui.adapter.InternalTwoWaySyncAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class InternalTwoWaySyncActivity : DrawerActivity(), Injectable {
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
    }

    private fun setupActionBar() {
        updateActionBarTitleAndHomeButtonByString(getString(R.string.internal_two_way_sync_headline))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setupTwoWaySyncAdapter() {
        binding.run {
            list.run {
                setEmptyView(emptyList.emptyListView)
                adapter = InternalTwoWaySyncAdapter(fileDataStorageManager, user.get(), this@InternalTwoWaySyncActivity)
                layoutManager = LinearLayoutManager(this@InternalTwoWaySyncActivity)
                adapter?.notifyDataSetChanged()
            }
        }
    }

    private fun setupEmptyList() {
        binding.emptyList.run {
            emptyListViewHeadline.run {
                visibility = View.VISIBLE
                setText(R.string.internal_two_way_sync_list_empty_headline)
            }

            emptyListViewText.run {
                visibility = View.VISIBLE
                setText(R.string.internal_two_way_sync_text)
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

    private fun setupMenuProvider() {
        addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) = Unit

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
