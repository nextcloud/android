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
import com.nextcloud.client.jobs.BackgroundJobManager
import com.owncloud.android.R
import com.owncloud.android.databinding.InternalTwoWaySyncLayoutBinding
import com.owncloud.android.ui.adapter.InternalTwoWaySyncAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

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
                adapter = internalTwoWaySyncAdapter
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

    /**
     * Disable two-way sync for all folders and stop all related workers.
     */
    private fun removeAllFolders() {
        CoroutineScope(Dispatchers.Main).launch {
            // cancel main worker
            backgroundJobManager.cancelTwoWaySyncJob(user.get())

            val folders = fileDataStorageManager.getInternalTwoWaySyncFolders(user.get())
            folders.forEach { folder ->
                // update database to ignore folder
                folder.internalFolderSyncTimestamp = -1L
                fileDataStorageManager.saveFile(folder)
            }

            // update view
            internalTwoWaySyncAdapter.update()
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
                            removeAllFolders()
                            true
                        }
                        else -> false
                    }
                }
            }
        )
    }
}
