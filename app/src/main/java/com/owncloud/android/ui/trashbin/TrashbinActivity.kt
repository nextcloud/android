/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * @author Chris Narkiewicz
 *
 * Copyright (C) 2018 Tobias Kaminsky
 * Copyright (C) 2018 Nextcloud GmbH.
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.trashbin

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.VisibleForTesting
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.nextcloud.client.account.CurrentAccountProvider
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.R
import com.owncloud.android.databinding.TrashbinActivityBinding
import com.owncloud.android.lib.resources.trashbin.model.TrashbinFile
import com.owncloud.android.ui.activity.DrawerActivity
import com.owncloud.android.ui.adapter.TrashbinListAdapter
import com.owncloud.android.ui.dialog.SortingOrderDialogFragment.OnSortingOrderListener
import com.owncloud.android.ui.interfaces.TrashbinActivityInterface
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.FileSortOrder
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

/**
 * Presenting trashbin data, received from presenter
 */
class TrashbinActivity :
    DrawerActivity(),
    TrashbinActivityInterface,
    OnSortingOrderListener,
    TrashbinContract.View,
    Injectable {

    @JvmField
    @Inject
    var preferences: AppPreferences? = null

    @JvmField
    @Inject
    var accountProvider: CurrentAccountProvider? = null

    @JvmField
    @Inject
    var clientFactory: ClientFactory? = null

    @JvmField
    @Inject
    var viewThemeUtils: ViewThemeUtils? = null

    private var trashbinListAdapter: TrashbinListAdapter? = null

    @VisibleForTesting
    var trashbinPresenter: TrashbinPresenter? = null

    private var active = false
    lateinit var binding: TrashbinActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentUser = user.orElse(accountProvider!!.user)
        val targetAccount = intent.getStringExtra(Intent.EXTRA_USER)

        if (targetAccount != null && !currentUser.nameEquals(targetAccount)) {
            val targetUser = userAccountManager.getUser(targetAccount)
            if (targetUser.isPresent) {
                setUser(targetUser.get())
            } else {
                Toast.makeText(this, R.string.associated_account_not_found, Toast.LENGTH_LONG).show()
                finish()
                return
            }
        }

        clientFactory?.let {
            val trashRepository = RemoteTrashbinRepository(user.orElse(accountProvider!!.user), it)
            trashbinPresenter = TrashbinPresenter(trashRepository, this)
        }

        binding = TrashbinActivityBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setupToolbar()

        findViewById<View>(R.id.sort_list_button_group).visibility = View.VISIBLE
        findViewById<View>(R.id.switch_grid_view_button).visibility =
            View.GONE

        updateActionBarTitleAndHomeButtonByString(getString(R.string.trashbin_activity_title))
        setupDrawer(R.id.nav_trashbin)
    }

    override fun onStart() {
        super.onStart()

        active = true
        setupContent()
    }

    override fun onResume() {
        super.onResume()

        setDrawerMenuItemChecked(R.id.nav_trashbin)
    }

    private fun setupContent() {
        val recyclerView = binding.list
        recyclerView.setEmptyView(binding.emptyList.emptyListView)

        binding.emptyList.emptyListView.visibility = View.GONE
        binding.emptyList.emptyListIcon.setImageResource(R.drawable.ic_delete)
        binding.emptyList.emptyListIcon.visibility = View.VISIBLE
        binding.emptyList.emptyListViewHeadline.text = getString(R.string.trashbin_empty_headline)
        binding.emptyList.emptyListViewText.text = getString(R.string.trashbin_empty_message)
        binding.emptyList.emptyListViewText.visibility = View.VISIBLE

        trashbinListAdapter = TrashbinListAdapter(
            this,
            storageManager,
            preferences,
            this,
            user.orElse(accountProvider!!.user),
            viewThemeUtils
        )

        recyclerView.adapter = trashbinListAdapter
        recyclerView.setHasFixedSize(true)
        recyclerView.setHasFooter(true)
        recyclerView.layoutManager = LinearLayoutManager(this)

        viewThemeUtils.androidx.themeSwipeRefreshLayout(binding.swipeContainingList)
        binding.swipeContainingList.setOnRefreshListener { loadFolder() }
        viewThemeUtils.material.colorMaterialTextButton(findViewById(R.id.sort_button))

        findViewById<View>(R.id.sort_button).setOnClickListener {
            DisplayUtils.openSortingOrderDialogFragment(
                supportFragmentManager,
                preferences?.getSortOrderByType(
                    FileSortOrder.Type.trashBinView,
                    FileSortOrder.sort_new_to_old
                )
            )
        }

        loadFolder()

        handleOnBackPressed()
    }

    private fun handleOnBackPressed() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    trashbinPresenter?.navigateUp()
                }
            }
        )
    }

    fun loadFolder() {
        trashbinListAdapter?.let {
            if (it.itemCount > EMPTY_LIST_COUNT) {
                binding.swipeContainingList.isRefreshing = true
            } else {
                showInitialLoading()
            }

            trashbinPresenter?.loadFolder()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var retval = true
        val itemId = item.itemId
        if (itemId == android.R.id.home) {
            if (isDrawerOpen) {
                closeDrawer()
            } else if (trashbinPresenter?.isRoot == true) {
                trashbinPresenter?.navigateUp()
            } else {
                openDrawer()
            }
        } else if (itemId == R.id.action_empty_trashbin) {
            trashbinPresenter?.emptyTrashbin()
        } else {
            retval = super.onOptionsItemSelected(item)
        }
        return retval
    }

    override fun onOverflowIconClicked(file: TrashbinFile, view: View) {
        val popup = PopupMenu(this, view)
        popup.inflate(R.menu.item_trashbin)
        popup.setOnMenuItemClickListener {
            trashbinPresenter?.removeTrashbinFile(file)
            true
        }
        popup.show()
    }

    override fun onItemClicked(file: TrashbinFile) {
        if (file.isFolder) {
            trashbinPresenter?.enterFolder(file.remotePath)
            mDrawerToggle.isDrawerIndicatorEnabled = false
        }
    }

    override fun onRestoreIconClicked(file: TrashbinFile, view: View) {
        trashbinPresenter?.restoreTrashbinFile(file)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_trashbin, menu)
        return true
    }

    override fun onPause() {
        super.onPause()
        active = false
        trashbinListAdapter?.cancelAllPendingTasks()
    }

    override fun close() {
        trashbinPresenter?.navigateUp()
    }

    override fun setDrawerIndicatorEnabled(bool: Boolean) {
        mDrawerToggle.isDrawerIndicatorEnabled = bool
    }

    override fun onSortingOrderChosen(sortOrder: FileSortOrder?) {
        val sortButton = findViewById<TextView>(R.id.sort_button)
        sortButton.setText(DisplayUtils.getSortOrderStringId(sortOrder))
        trashbinListAdapter?.setSortOrder(sortOrder)
    }

    override fun showTrashbinFolder(trashbinFiles: List<TrashbinFile?>?) {
        if (active) {
            trashbinListAdapter?.setTrashbinFiles(trashbinFiles, true)
            binding.swipeContainingList.isRefreshing = false
            binding.loadingContent.visibility = View.GONE
            binding.emptyList.emptyListIcon.setImageResource(R.drawable.ic_delete)
            binding.emptyList.emptyListViewHeadline.text = getString(R.string.trashbin_empty_headline)
            binding.emptyList.emptyListViewText.text = getString(R.string.trashbin_empty_message)
            binding.list.visibility = View.VISIBLE
        }
    }

    override fun removeFile(file: TrashbinFile?) {
        if (active) {
            trashbinListAdapter?.removeFile(file)
        }
    }

    override fun removeAllFiles() {
        trashbinListAdapter?.removeAllFiles()
    }

    override fun showSnackbarError(message: Int, file: TrashbinFile?) {
        if (active) {
            binding.swipeContainingList.isRefreshing = false
            Snackbar.make(binding.list, String.format(getString(message), file?.fileName), Snackbar.LENGTH_LONG)
                .show()
        }
    }

    @VisibleForTesting
    fun showInitialLoading() {
        binding.emptyList.emptyListView.visibility = View.GONE
        binding.list.visibility = View.GONE
        binding.loadingContent.visibility = View.VISIBLE
    }

    @VisibleForTesting
    fun showUser() {
        binding.loadingContent.visibility = View.GONE
        binding.list.visibility = View.VISIBLE
        binding.swipeContainingList.isRefreshing = false
        binding.emptyList.emptyListViewText.text = user.get().accountName
        binding.emptyList.emptyListViewText.visibility = View.VISIBLE
        binding.emptyList.emptyListView.visibility = View.VISIBLE
    }

    override fun showError(message: Int) {
        if (active) {
            trashbinListAdapter?.removeAllFiles()
            binding.loadingContent.visibility = View.GONE
            binding.list.visibility = View.VISIBLE
            binding.swipeContainingList.isRefreshing = false
            binding.emptyList.emptyListViewHeadline.setText(R.string.common_error)
            binding.emptyList.emptyListIcon.setImageDrawable(
                ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.ic_list_empty_error,
                    null
                )
            )
            binding.emptyList.emptyListViewText.setText(message)
            binding.emptyList.emptyListViewText.visibility = View.VISIBLE
            binding.emptyList.emptyListIcon.visibility = View.VISIBLE
            binding.emptyList.emptyListView.visibility = View.VISIBLE
        }
    }

    companion object {
        const val EMPTY_LIST_COUNT = 1
    }
}
