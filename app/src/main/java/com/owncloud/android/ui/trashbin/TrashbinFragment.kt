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
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.IdRes
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.nextcloud.client.account.CurrentAccountProvider
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.client.utils.Throttler
import com.nextcloud.ui.trashbinFileActions.TrashbinFileActionsBottomSheet
import com.nextcloud.utils.extensions.getTypedActivity
import com.owncloud.android.R
import com.owncloud.android.databinding.FragmentTrashbinBinding
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.lib.resources.trashbin.model.TrashbinFile
import com.owncloud.android.ui.adapter.TrashbinListAdapter
import com.owncloud.android.ui.dialog.SortingOrderDialogFragment.OnSortingOrderListener
import com.owncloud.android.ui.interfaces.TrashbinActivityInterface
import com.owncloud.android.ui.navigation.NavigatorActivity
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.FileSortOrder
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

class TrashbinFragment :
    Fragment(),
    TrashbinActivityInterface,
    OnSortingOrderListener,
    TrashbinContract.View,
    Injectable {

    @Inject
    lateinit var preferences: AppPreferences

    @Inject
    lateinit var syncedFolderProvider: SyncedFolderProvider

    @Inject
    lateinit var accountProvider: CurrentAccountProvider

    @Inject
    lateinit var userAccountManager: UserAccountManager

    @Inject
    lateinit var clientFactory: ClientFactory

    @Inject
    lateinit var throttler: Throttler

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    @Inject
    lateinit var connectivityService: ConnectivityService

    private var trashbinListAdapter: TrashbinListAdapter? = null

    @VisibleForTesting
    var trashbinPresenter: TrashbinPresenter? = null

    private var active = false

    private var binding: FragmentTrashbinBinding? = null

    private var multiChoiceModeListener: MultiChoiceModeListener? = null

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            trashbinPresenter?.navigateUp()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentTrashbinBinding.inflate(inflater, container, false)
        val binding = binding!!
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navigatorActivity = getTypedActivity(NavigatorActivity::class.java)

        val currentUser = navigatorActivity?.user?.orElse(accountProvider.user)
        val targetAccount = activity?.intent?.getStringExtra(Intent.EXTRA_USER)

        if (targetAccount != null && currentUser?.nameEquals(targetAccount) == false) {
            val targetUser = userAccountManager.getUser(targetAccount)
            if (targetUser.isPresent) {
                navigatorActivity.setUser(targetUser.get())
            } else {
                DisplayUtils.showSnackMessage(this, R.string.associated_account_not_found)
                activity?.finish()
                return
            }
        }

        val trashRepository = RemoteTrashbinRepository(accountProvider.user, clientFactory)
        trashbinPresenter = TrashbinPresenter(trashRepository, this)
        handleBackPress()
        active = true
        setupContent()
        addMenuProvider()
    }

    private fun setupContent() {
        val binding = binding ?: return

        val navigatorActivity = getTypedActivity(NavigatorActivity::class.java)

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
            navigatorActivity?.storageManager,
            preferences,
            syncedFolderProvider,
            requireContext(),
            navigatorActivity?.user?.orElse(accountProvider.user),
            viewThemeUtils
        )

        recyclerView.adapter = trashbinListAdapter
        recyclerView.setHasFixedSize(true)
        recyclerView.setHasFooter(true)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        viewThemeUtils.androidx.themeSwipeRefreshLayout(binding.swipeContainingList)
        binding.swipeContainingList.setOnRefreshListener { loadFolder() }

        val sortOrder = preferences.getSortOrderByType(
            FileSortOrder.Type.trashBinView,
            FileSortOrder.SORT_NEW_TO_OLD
        )

        activity?.findViewById<View>(R.id.sort_list_button_group)?.visibility = View.VISIBLE
        activity?.findViewById<MaterialButton>(R.id.sort_button)?.run {
            setOnClickListener {
                DisplayUtils.openSortingOrderDialogFragment(
                    activity?.supportFragmentManager,
                    preferences.getSortOrderByType(
                        FileSortOrder.Type.trashBinView,
                        FileSortOrder.SORT_NEW_TO_OLD
                    )
                )
            }

            setText(DisplayUtils.getSortOrderStringId(sortOrder))
            visibility = View.VISIBLE
        }

        activity?.findViewById<MaterialButton>(R.id.switch_grid_view_button)?.run {
            visibility = View.GONE
        }

        loadFolder()

        multiChoiceModeListener = MultiChoiceModeListener(
            navigatorActivity,
            trashbinListAdapter,
            viewThemeUtils
        ) { filesCount, checkedFiles -> openActionsMenu(filesCount, checkedFiles) }

        multiChoiceModeListener?.let { listener ->
            activity?.findViewById<DrawerLayout>(R.id.drawer_layout)?.addDrawerListener(listener)
        }
    }

    private fun handleBackPress() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            onBackPressedCallback
        )
    }

    fun loadFolder(onComplete: () -> Unit = {}, onError: () -> Unit = {}) {
        // exit action mode on data refresh
        multiChoiceModeListener?.exitSelectionMode()
        trashbinListAdapter?.let {
            if (it.itemCount > EMPTY_LIST_COUNT) {
                binding?.swipeContainingList?.isRefreshing = true
            } else {
                showInitialLoading()
            }

            trashbinPresenter?.loadFolder(onComplete, onError)
        }
    }

    private fun addMenuProvider() {
        val menuHost: MenuHost = requireActivity()
        val navigatorActivity = getTypedActivity(NavigatorActivity::class.java)

        menuHost.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.activity_trashbin, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    val itemId = menuItem.itemId

                    if (itemId == android.R.id.home) {
                        if (navigatorActivity?.isDrawerOpen == true) {
                            navigatorActivity.closeDrawer()
                        } else if (trashbinPresenter?.isRoot == false) {
                            trashbinPresenter?.navigateUp()
                        } else {
                            navigatorActivity?.openDrawer()
                        }
                    } else if (itemId == R.id.action_empty_trashbin) {
                        trashbinPresenter?.emptyTrashbin()
                    }

                    return false
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    override fun onOverflowIconClicked(file: TrashbinFile, view: View) {
        PopupMenu(requireContext(), view).apply {
            inflate(R.menu.item_trashbin)
            setOnMenuItemClickListener {
                onFileActionChosen(it.itemId, setOf(file))
                true
            }
            show()
        }
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
        val navigatorActivity = getTypedActivity(NavigatorActivity::class.java)

        // Create only once instance of action mode
        if (multiChoiceModeListener?.activeActionMode != null) {
            toggleItemToCheckedList(file)
        } else {
            navigatorActivity?.startActionMode(multiChoiceModeListener)
            trashbinListAdapter?.addCheckedFile(file)
        }
        multiChoiceModeListener?.updateActionModeFile(file)
        return true
    }

    private fun toggleItemToCheckedList(file: TrashbinFile) {
        trashbinListAdapter?.run {
            if (isCheckedFile(file)) {
                removeCheckedFile(file)
            } else {
                addCheckedFile(file)
            }
        }
        multiChoiceModeListener?.updateActionModeFile(file)
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
        val navigatorActivity = getTypedActivity(NavigatorActivity::class.java)
        navigatorActivity?.setDrawerIndicatorEnabled(isRoot)
        onBackPressedCallback.isEnabled = !isRoot
    }

    override fun onSortingOrderChosen(selection: FileSortOrder?) {
        val navigatorActivity = getTypedActivity(NavigatorActivity::class.java)
        val sortButton = navigatorActivity?.findViewById<TextView>(R.id.sort_button)
        sortButton?.setText(DisplayUtils.getSortOrderStringId(selection))
        trashbinListAdapter?.setSortOrder(selection)
    }

    override fun showTrashbinFolder(trashbinFiles: List<TrashbinFile?>?) {
        if (active) {
            val binding = binding ?: return
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
            val binding = binding ?: return
            binding.swipeContainingList.isRefreshing = false
            Snackbar.make(binding.list, String.format(getString(message), file?.fileName), Snackbar.LENGTH_LONG)
                .show()
        }
    }

    @VisibleForTesting
    fun showInitialLoading() {
        val binding = binding ?: return
        binding.emptyList.emptyListView.visibility = View.GONE
        binding.list.visibility = View.GONE
        binding.loadingContent.visibility = View.VISIBLE
    }

    @VisibleForTesting
    fun showUser() {
        val navigatorActivity = getTypedActivity(NavigatorActivity::class.java)
        val binding = binding ?: return
        binding.loadingContent.visibility = View.GONE
        binding.list.visibility = View.VISIBLE
        binding.swipeContainingList.isRefreshing = false
        binding.emptyList.emptyListViewText.text = navigatorActivity?.user?.get()?.accountName
        binding.emptyList.emptyListViewText.visibility = View.VISIBLE
        binding.emptyList.emptyListView.visibility = View.VISIBLE
    }

    override fun showError(message: Int) {
        if (active) {
            val binding = binding ?: return
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
        val binding = binding ?: return
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
            val navigatorActivity = getTypedActivity(NavigatorActivity::class.java)
            val supportFragmentManager = navigatorActivity?.supportFragmentManager ?: return@run

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
                multiChoiceModeListener?.exitSelectionMode()
                return true
            }

            R.id.restore -> {
                trashbinPresenter?.restoreTrashbinFile(checkedFiles)
                multiChoiceModeListener?.exitSelectionMode()
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

            multiChoiceModeListener?.invalidateActionMode()
        }
    }

    companion object {
        const val EMPTY_LIST_COUNT = 1
    }

    internal class MultiChoiceModeListener(
        val activity: NavigatorActivity?,
        val adapter: TrashbinListAdapter?,
        val viewThemeUtils: ViewThemeUtils,
        val openActionsMenu: (Int, Set<TrashbinFile>) -> Unit
    ) : AbsListView.MultiChoiceModeListener,
        DrawerLayout.DrawerListener {

        var activeActionMode: ActionMode? = null
        private var mIsActionModeNew = false

        private var actionModeClosedByDrawer = false
        private val selectionWhenActionModeClosedByDrawer: MutableSet<TrashbinFile> = HashSet()

        override fun onItemCheckedStateChanged(mode: ActionMode, position: Int, id: Long, checked: Boolean) = Unit
        override fun onDrawerSlide(drawerView: View, slideOffset: Float) = Unit
        override fun onDrawerOpened(drawerView: View) = Unit

        override fun onDrawerClosed(drawerView: View) {
            if (actionModeClosedByDrawer && selectionWhenActionModeClosedByDrawer.isNotEmpty()) {
                activity?.startActionMode(this)

                adapter?.setCheckedItem(selectionWhenActionModeClosedByDrawer)

                activeActionMode?.invalidate()

                selectionWhenActionModeClosedByDrawer.clear()
            }
        }

        override fun onDrawerStateChanged(newState: Int) {
            if (DrawerLayout.STATE_DRAGGING == newState && activeActionMode != null) {
                adapter?.let {
                    selectionWhenActionModeClosedByDrawer.addAll(
                        it.checkedItems
                    )
                }

                activeActionMode?.finish()
                actionModeClosedByDrawer = true
            }
        }

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            activeActionMode = mode
            // Determine if actionMode is "new" or not (already affected by item-selection)
            mIsActionModeNew = true

            // fake menu to be able to use bottom sheet instead
            val inflater: MenuInflater? = activity?.menuInflater
            inflater?.inflate(R.menu.custom_menu_placeholder, menu)
            val item = menu.findItem(R.id.custom_menu_placeholder_item)
            item.icon?.let {
                item.setIcon(
                    activity?.let { it1 ->
                        viewThemeUtils.platform.colorDrawable(
                            it,
                            ContextCompat.getColor(it1, R.color.white)
                        )
                    }
                )
            }

            mode.invalidate()

            if (activity != null) {
                viewThemeUtils.platform.colorStatusBar(
                    activity,
                    ContextCompat.getColor(activity, R.color.action_mode_background)
                )
            }

            adapter?.setMultiSelect(true)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            val checkedFiles: Set<TrashbinFile> = adapter?.checkedItems ?: emptySet()
            val checkedCount = checkedFiles.size
            val title: String? =
                activity?.getResources()?.getQuantityString(R.plurals.items_selected_count, checkedCount, checkedCount)
            mode.title = title

            // Determine if we need to finish the action mode because there are no items selected
            if (checkedCount == 0 && !mIsActionModeNew) {
                exitSelectionMode()
            }

            return true
        }

        fun exitSelectionMode() {
            activeActionMode?.run {
                finish()
            }
        }

        fun updateActionModeFile(file: TrashbinFile) {
            mIsActionModeNew = false
            activeActionMode?.let {
                it.invalidate()
                adapter?.notifyItemChanged(file)
            }
        }

        fun invalidateActionMode() {
            activeActionMode?.invalidate()
        }

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

        override fun onDestroyActionMode(mode: ActionMode) {
            activeActionMode = null

            if (activity != null) {
                viewThemeUtils.platform.resetStatusBar(activity)
            }

            adapter?.setMultiSelect(false)
            adapter?.clearCheckedItems()
        }
    }
}
