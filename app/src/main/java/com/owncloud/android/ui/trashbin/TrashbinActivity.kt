/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2018 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2018 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.trashbin

import android.content.Intent
import android.os.Bundle
import android.view.ActionMode
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.AbsListView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.IdRes
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.nextcloud.client.account.CurrentAccountProvider
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.client.utils.Throttler
import com.nextcloud.ui.trashbinFileActions.TrashbinFileActionsBottomSheet
import com.owncloud.android.R
import com.owncloud.android.databinding.TrashbinActivityBinding
import com.owncloud.android.datamodel.SyncedFolderProvider
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

    @Inject
    lateinit var syncedFolderProvider: SyncedFolderProvider

    @JvmField
    @Inject
    var accountProvider: CurrentAccountProvider? = null

    @JvmField
    @Inject
    var clientFactory: ClientFactory? = null

    @Inject
    lateinit var throttler: Throttler

    @Inject
    lateinit var connectivityService: ConnectivityService

    private var trashbinListAdapter: TrashbinListAdapter? = null

    @VisibleForTesting
    var trashbinPresenter: TrashbinPresenter? = null

    private var active = false
    lateinit var binding: TrashbinActivityBinding

    private var mMultiChoiceModeListener: MultiChoiceModeListener? = null

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            trashbinPresenter?.navigateUp()
        }
    }

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
        setupDrawer()
        handleBackPress()
    }

    override fun onStart() {
        super.onStart()

        active = true
        setupContent()
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
            syncedFolderProvider,
            this,
            user.orElse(accountProvider!!.user),
            viewThemeUtils
        )

        recyclerView.adapter = trashbinListAdapter
        recyclerView.setHasFixedSize(true)
        recyclerView.setHasFooter(true)
        recyclerView.layoutManager = LinearLayoutManager(this)

        viewThemeUtils?.androidx?.themeSwipeRefreshLayout(binding.swipeContainingList)
        binding.swipeContainingList.setOnRefreshListener { loadFolder() }
        viewThemeUtils?.material?.colorMaterialTextButton(findViewById(R.id.sort_button))

        val sortOrder = preferences?.getSortOrderByType(
            FileSortOrder.Type.trashBinView,
            FileSortOrder.SORT_NEW_TO_OLD
        )

        findViewById<TextView>(R.id.sort_button).apply {
            setOnClickListener {
                DisplayUtils.openSortingOrderDialogFragment(
                    supportFragmentManager,
                    preferences?.getSortOrderByType(
                        FileSortOrder.Type.trashBinView,
                        FileSortOrder.SORT_NEW_TO_OLD
                    )
                )
            }

            setText(DisplayUtils.getSortOrderStringId(sortOrder))
        }

        loadFolder()

        mMultiChoiceModeListener = MultiChoiceModeListener(
            this,
            trashbinListAdapter,
            viewThemeUtils
        ) { filesCount, checkedFiles -> openActionsMenu(filesCount, checkedFiles) }
        addDrawerListener(mMultiChoiceModeListener)
    }

    private fun handleBackPress() {
        onBackPressedDispatcher.addCallback(
            this,
            onBackPressedCallback
        )
    }

    fun loadFolder(onComplete: () -> Unit = {}, onError: () -> Unit = {}) {
        // exit action mode on data refresh
        mMultiChoiceModeListener?.exitSelectionMode()
        trashbinListAdapter?.let {
            if (it.itemCount > EMPTY_LIST_COUNT) {
                binding.swipeContainingList.isRefreshing = true
            } else {
                showInitialLoading()
            }

            trashbinPresenter?.loadFolder(onComplete, onError)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var retval = true
        val itemId = item.itemId
        if (itemId == android.R.id.home) {
            if (isDrawerOpen) {
                closeDrawer()
            } else if (trashbinPresenter?.isRoot == false) {
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
            onFileActionChosen(it.itemId, setOf(file))
            true
        }
        popup.show()
    }

    override fun onItemClicked(file: TrashbinFile) {
        if (trashbinListAdapter?.isMultiSelect == true) {
            toggleItemToCheckedList(file)
        } else if (file.isFolder) {
            trashbinPresenter?.enterFolder(file.remotePath)
        }
    }

    override fun onRestoreIconClicked(file: TrashbinFile) {
        trashbinPresenter?.restoreTrashbinFile(listOf(file))
    }

    override fun onLongItemClicked(file: TrashbinFile): Boolean {
        // Create only once instance of action mode
        if (mMultiChoiceModeListener?.mActiveActionMode != null) {
            toggleItemToCheckedList(file)
        } else {
            startActionMode(mMultiChoiceModeListener)
            trashbinListAdapter?.addCheckedFile(file)
        }
        mMultiChoiceModeListener?.updateActionModeFile(file)
        return true
    }

    /**
     * Will toggle a file selection status from the action mode
     *
     * @param file The concerned TrashbinFile by the selection/deselection
     */
    private fun toggleItemToCheckedList(file: TrashbinFile) {
        trashbinListAdapter?.run {
            if (isCheckedFile(file)) {
                removeCheckedFile(file)
            } else {
                addCheckedFile(file)
            }
        }
        mMultiChoiceModeListener?.updateActionModeFile(file)
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

    override fun atRoot(isRoot: Boolean) {
        mDrawerToggle.isDrawerIndicatorEnabled = isRoot
        onBackPressedCallback.isEnabled = !isRoot
    }

    override fun onSortingOrderChosen(selection: FileSortOrder?) {
        val sortButton = findViewById<TextView>(R.id.sort_button)
        sortButton.setText(DisplayUtils.getSortOrderStringId(selection))
        trashbinListAdapter?.setSortOrder(selection)
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
            connectivityService.isNetworkAndServerAvailable { result: Boolean? ->
                if (result == true) {
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
                } else {
                    showEmptyContent(
                        getString(R.string.server_not_reachable),
                        getString(R.string.server_not_reachable_content)
                    )
                }
            }
        }
    }

    private fun showEmptyContent(headline: String, message: String) {
        binding.emptyList.emptyListViewHeadline.text = headline
        binding.emptyList.emptyListViewText.text = message
        binding.loadingContent.visibility = View.GONE
        binding.emptyList.emptyListIcon.visibility = View.VISIBLE
        binding.emptyList.emptyListViewHeadline.visibility = View.VISIBLE
        binding.emptyList.emptyListViewText.visibility = View.VISIBLE
        binding.emptyList.emptyListView.visibility = View.VISIBLE
        binding.emptyList.emptyListIcon.setImageResource(R.drawable.ic_sync_off)
    }

    private fun openActionsMenu(filesCount: Int, checkedFiles: Set<TrashbinFile>) {
        throttler.run("overflowClick") {
            val supportFragmentManager = supportFragmentManager

            TrashbinFileActionsBottomSheet.newInstance(filesCount, checkedFiles)
                .setResultListener(
                    supportFragmentManager,
                    this
                ) { id: Int ->
                    onFileActionChosen(
                        id,
                        checkedFiles
                    )
                }
                .show(supportFragmentManager, "actions")
        }
    }

    @Suppress("ReturnCount")
    private fun onFileActionChosen(@IdRes itemId: Int, checkedFiles: Set<TrashbinFile>): Boolean {
        if (checkedFiles.isEmpty()) {
            return false
        }

        when (itemId) {
            R.id.action_delete -> {
                trashbinPresenter?.removeTrashbinFile(checkedFiles)
                mMultiChoiceModeListener?.exitSelectionMode()
                return true
            }

            R.id.restore -> {
                trashbinPresenter?.restoreTrashbinFile(checkedFiles)
                mMultiChoiceModeListener?.exitSelectionMode()
                return true
            }

            R.id.action_select_all_action_menu -> {
                selectAllFiles(true)
                return true
            }

            R.id.action_deselect_all_action_menu -> {
                selectAllFiles(false)
                return true
            }

            else -> return false
        }
    }

    /**
     * De-/select all elements in the current list view.
     *
     * @param select `true` to select all, `false` to deselect all
     */
    private fun selectAllFiles(select: Boolean) {
        trashbinListAdapter?.let {
            if (select) {
                it.addAllFilesToCheckedFiles()
            } else {
                it.clearCheckedItems()
            }
            for (i in 0 until it.itemCount) {
                it.notifyItemChanged(i)
            }

            mMultiChoiceModeListener?.invalidateActionMode()
        }
    }

    companion object {
        const val EMPTY_LIST_COUNT = 1
    }

    /**
     * Handler for multiple selection mode.
     *
     *
     * Manages input from the user when one or more files or folders are selected in the list.
     *
     *
     * Also listens to changes in navigation drawer to hide and recover multiple selection when it's opened and closed.
     */
    internal class MultiChoiceModeListener(
        val activity: TrashbinActivity,
        val adapter: TrashbinListAdapter?,
        val viewThemeUtils: ViewThemeUtils,
        val openActionsMenu: (Int, Set<TrashbinFile>) -> Unit
    ) : AbsListView.MultiChoiceModeListener,
        DrawerLayout.DrawerListener {

        var mActiveActionMode: ActionMode? = null
        private var mIsActionModeNew = false

        /**
         * True when action mode is finished because the drawer was opened
         */
        private var mActionModeClosedByDrawer = false

        /**
         * Selected items in list when action mode is closed by drawer
         */
        private val mSelectionWhenActionModeClosedByDrawer: MutableSet<TrashbinFile> = HashSet()

        override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
            // nothing to do
        }

        override fun onDrawerOpened(drawerView: View) {
            // nothing to do
        }

        /**
         * When the navigation drawer is closed, action mode is recovered in the same state as was when the drawer was
         * (started to be) opened.
         *
         * @param drawerView Navigation drawer just closed.
         */
        override fun onDrawerClosed(drawerView: View) {
            if (mActionModeClosedByDrawer && mSelectionWhenActionModeClosedByDrawer.size > 0) {
                activity.startActionMode(this)

                adapter?.setCheckedItem(mSelectionWhenActionModeClosedByDrawer)

                mActiveActionMode?.invalidate()

                mSelectionWhenActionModeClosedByDrawer.clear()
            }
        }

        /**
         * If the action mode is active when the navigation drawer starts to move, the action mode is closed and the
         * selection stored to be recovered when the drawer is closed.
         *
         * @param newState One of STATE_IDLE, STATE_DRAGGING or STATE_SETTLING.
         */
        override fun onDrawerStateChanged(newState: Int) {
            if (DrawerLayout.STATE_DRAGGING == newState && mActiveActionMode != null) {
                adapter?.let {
                    mSelectionWhenActionModeClosedByDrawer.addAll(
                        it.checkedItems
                    )
                }

                mActiveActionMode?.finish()
                mActionModeClosedByDrawer = true
            }
        }

        /**
         * Update action mode bar when an item is selected / unselected in the list
         */
        override fun onItemCheckedStateChanged(mode: ActionMode, position: Int, id: Long, checked: Boolean) {
            // nothing to do here
        }

        /**
         * Load menu and customize UI when action mode is started.
         */
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mActiveActionMode = mode
            // Determine if actionMode is "new" or not (already affected by item-selection)
            mIsActionModeNew = true

            // fake menu to be able to use bottom sheet instead
            val inflater: MenuInflater = activity.menuInflater
            inflater.inflate(R.menu.custom_menu_placeholder, menu)
            val item = menu.findItem(R.id.custom_menu_placeholder_item)
            item.icon?.let {
                item.setIcon(
                    viewThemeUtils.platform.colorDrawable(
                        it,
                        ContextCompat.getColor(activity, R.color.white)
                    )
                )
            }

            mode.invalidate()

            // set actionMode color
            viewThemeUtils.platform.colorStatusBar(
                activity,
                ContextCompat.getColor(activity, R.color.action_mode_background)
            )

            adapter?.setMultiSelect(true)
            return true
        }

        /**
         * Updates available action in menu depending on current selection.
         */
        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            val checkedFiles: Set<TrashbinFile> = adapter?.checkedItems ?: emptySet()
            val checkedCount = checkedFiles.size
            val title: String =
                activity.getResources().getQuantityString(R.plurals.items_selected_count, checkedCount, checkedCount)
            mode.title = title

            // Determine if we need to finish the action mode because there are no items selected
            if (checkedCount == 0 && !mIsActionModeNew) {
                exitSelectionMode()
            }

            return true
        }

        /**
         * Exits the multi file selection mode.
         */
        fun exitSelectionMode() {
            mActiveActionMode?.run {
                finish()
            }
        }

        /**
         * Will update (invalidate) the action mode adapter/mode to refresh an item selection change
         *
         * @param file The concerned TrashbinFile to refresh in adapter
         */
        fun updateActionModeFile(file: TrashbinFile) {
            mIsActionModeNew = false
            mActiveActionMode?.let {
                it.invalidate()
                adapter?.notifyItemChanged(file)
            }
        }

        fun invalidateActionMode() {
            mActiveActionMode?.invalidate()
        }

        /**
         * Starts the corresponding action when a menu item is tapped by the user.
         */
        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            adapter?.let {
                val checkedFiles: Set<TrashbinFile> = it.checkedItems
                if (item.itemId == R.id.custom_menu_placeholder_item) {
                    openActionsMenu(it.filesCount, checkedFiles)
                }
                return true
            }
            return false
        }

        /**
         * Restores UI.
         */
        override fun onDestroyActionMode(mode: ActionMode) {
            mActiveActionMode = null

            viewThemeUtils.platform.resetStatusBar(activity)

            adapter?.setMultiSelect(false)
            adapter?.clearCheckedItems()
        }
    }
}
