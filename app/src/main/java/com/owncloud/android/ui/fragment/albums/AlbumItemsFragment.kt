/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.fragment.albums

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.client.network.ClientFactory.CreationException
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.client.utils.Throttler
import com.nextcloud.ui.albumItemActions.AlbumItemActionsBottomSheet
import com.nextcloud.ui.fileactions.FileActionsBottomSheet.Companion.newInstance
import com.nextcloud.utils.extensions.getTypedActivity
import com.nextcloud.utils.extensions.isDialogFragmentReady
import com.owncloud.android.R
import com.owncloud.android.databinding.ListFragmentBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.albums.ReadAlbumItemsRemoteOperation
import com.owncloud.android.lib.resources.albums.RemoveAlbumFileRemoteOperation
import com.owncloud.android.lib.resources.albums.ToggleAlbumFavoriteRemoteOperation
import com.owncloud.android.ui.activity.AlbumsPickerActivity
import com.owncloud.android.ui.activity.AlbumsPickerActivity.Companion.intentForPickingMediaFiles
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.adapter.GalleryAdapter
import com.owncloud.android.ui.dialog.CreateAlbumDialogFragment
import com.owncloud.android.ui.events.FavoriteEvent
import com.owncloud.android.ui.fragment.FileFragment
import com.owncloud.android.ui.interfaces.OCFileListFragmentInterface
import com.owncloud.android.ui.preview.PreviewImageFragment
import com.owncloud.android.ui.preview.PreviewMediaActivity.Companion.canBePreviewed
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.ErrorMessageAdapter
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.Optional
import javax.inject.Inject

@Suppress("TooManyFunctions")
class AlbumItemsFragment : Fragment(), OCFileListFragmentInterface, Injectable {

    private var adapter: GalleryAdapter? = null
    private var client: OwnCloudClient? = null
    private var optionalUser: Optional<User>? = null

    private lateinit var binding: ListFragmentBinding

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    @Inject
    lateinit var accountManager: UserAccountManager

    @Inject
    lateinit var clientFactory: ClientFactory

    @Inject
    lateinit var preferences: AppPreferences

    @Inject
    lateinit var syncedFolderProvider: SyncedFolderProvider

    @Inject
    lateinit var throttler: Throttler

    private var mContainerActivity: FileFragment.ContainerActivity? = null

    private var columnSize = 0

    private lateinit var albumName: String
    private var isNewAlbum: Boolean = false

    private var mMultiChoiceModeListener: MultiChoiceModeListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            mContainerActivity = context as FileFragment.ContainerActivity
        } catch (e: ClassCastException) {
            throw IllegalArgumentException(
                context.toString() + " must implement " +
                    FileFragment.ContainerActivity::class.java.simpleName,
                e
            )
        }
        arguments?.let {
            albumName = it.getString(ARG_ALBUM_NAME) ?: ""
            isNewAlbum = it.getBoolean(ARG_IS_NEW_ALBUM)
        }
    }

    override fun onDetach() {
        mContainerActivity = null
        super.onDetach()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        columnSize = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            MAX_COLUMN_SIZE_LANDSCAPE
        } else {
            MAX_COLUMN_SIZE_PORTRAIT
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = ListFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        optionalUser = Optional.of(accountManager.user)
        createMenu()
        setupContainingList()
        setupContent()

        // if fragment is opened when new albums is created
        // then open gallery to choose media to add
        if (isNewAlbum) {
            openGalleryToAddMedia()
        }
    }

    private fun setUpActionMode() {
        if (mMultiChoiceModeListener != null) return

        mMultiChoiceModeListener = MultiChoiceModeListener(
            requireActivity(),
            adapter,
            viewThemeUtils
        ) { filesCount, checkedFiles -> openActionsMenu(filesCount, checkedFiles) }
        (requireActivity() as FileDisplayActivity).addDrawerListener(mMultiChoiceModeListener)
    }

    private fun createMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menu.clear() // important: clears any existing activity menu
                    menuInflater.inflate(R.menu.fragment_album_items, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.action_three_dot_icon -> {
                            openAlbumActionsMenu()
                            true
                        }

                        R.id.action_add_more_photos -> {
                            // open Gallery fragment as selection then add items to current album
                            openGalleryToAddMedia()
                            true
                        }

                        else -> false
                    }
                }

                override fun onPrepareMenu(menu: Menu) {
                    super.onPrepareMenu(menu)
                    val moreMenu = menu.findItem(R.id.action_three_dot_icon)
                    moreMenu.icon?.let {
                        moreMenu.setIcon(
                            viewThemeUtils.platform.colorDrawable(
                                it,
                                ContextCompat.getColor(requireActivity(), R.color.black)
                            )
                        )
                    }
                    val add = menu.findItem(R.id.action_add_more_photos)
                    add.icon?.let {
                        add.setIcon(
                            viewThemeUtils.platform.colorDrawable(
                                it,
                                ContextCompat.getColor(requireActivity(), R.color.black)
                            )
                        )
                    }
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    private fun openAlbumActionsMenu() {
        throttler.run("overflowClick") {
            val supportFragmentManager = requireActivity().supportFragmentManager

            AlbumItemActionsBottomSheet.newInstance()
                .setResultListener(
                    supportFragmentManager,
                    this
                ) { id: Int ->
                    onAlbumActionChosen(id)
                }
                .show(supportFragmentManager, "album_actions")
        }
    }

    private fun onAlbumActionChosen(@IdRes itemId: Int): Boolean {
        return when (itemId) {
            // action to rename album
            R.id.action_rename_file -> {
                CreateAlbumDialogFragment.newInstance(albumName)
                    .show(
                        requireActivity().supportFragmentManager,
                        CreateAlbumDialogFragment.TAG
                    )
                true
            }

            // action to delete album
            R.id.action_delete -> {
                mContainerActivity?.getFileOperationsHelper()?.removeAlbum(albumName)
                true
            }

            else -> false
        }
    }

    private fun setupContent() {
        binding.listRoot.setEmptyView(binding.emptyList.emptyListView)
        val layoutManager = GridLayoutManager(requireContext(), 1)
        binding.listRoot.layoutManager = layoutManager
        fetchAndSetData()
    }

    private fun setupContainingList() {
        viewThemeUtils.androidx.themeSwipeRefreshLayout(binding.swipeContainingList)
        binding.swipeContainingList.setOnRefreshListener {
            binding.swipeContainingList.isRefreshing = true
            fetchAndSetData()
        }
    }

    @VisibleForTesting
    fun populateList(albums: List<OCFile>) {
        // exit action mode on data refresh
        mMultiChoiceModeListener?.exitSelectionMode()

        if (requireActivity() is FileDisplayActivity) {
            (requireActivity() as FileDisplayActivity).setMainFabVisible(false)
        }
        initializeAdapter()
        adapter?.showAlbumItems(albums)
    }

    private fun fetchAndSetData() {
        mMultiChoiceModeListener?.exitSelectionMode()
        initializeAdapter()
        setEmptyListLoadingMessage()
        lifecycleScope.launch(Dispatchers.IO) {
            val getRemoteNotificationOperation = ReadAlbumItemsRemoteOperation(albumName)
            val result = client?.let { getRemoteNotificationOperation.execute(it) }
            val ocFileList = mutableListOf<OCFile>()

            if (result?.isSuccess == true && result.resultData != null) {
                for (remoteFile in result.getResultData()) {
                    var ocFile = mContainerActivity?.storageManager?.getFileByLocalId(remoteFile.localId)
                    if (ocFile == null) {
                        ocFile = FileStorageUtils.fillOCFile(remoteFile)
                    } else {
                        // required: as OCFile will only contains file_name.png not with /albums/album_name/file_name
                        // to fix this we have to get the remote path from remote file and assign to OCFile
                        ocFile.remotePath = remoteFile.remotePath
                        ocFile.decryptedRemotePath = remoteFile.remotePath
                    }
                    ocFileList.add(ocFile!!)
                }
            }
            withContext(Dispatchers.Main) {
                if (result?.isSuccess == true && result.resultData != null) {
                    if (result.resultData.isEmpty()) {
                        setMessageForEmptyList(
                            R.string.file_list_empty_headline_server_search,
                            resources.getString(R.string.file_list_empty_gallery),
                            R.drawable.file_image,
                            false
                        )
                    }
                    populateList(ocFileList)
                } else {
                    Log_OC.d(TAG, result?.logMessage)
                    // show error
                    setMessageForEmptyList(
                        R.string.file_list_empty_headline_server_search,
                        resources.getString(R.string.file_list_empty_gallery),
                        R.drawable.file_image,
                        false
                    )
                }
                hideRefreshLayoutLoader()
            }
        }
    }

    private fun hideRefreshLayoutLoader() {
        binding.swipeContainingList.isRefreshing = false
    }

    private fun setEmptyListLoadingMessage() {
        val fileActivity = this.getTypedActivity(FileActivity::class.java)
        fileActivity?.connectivityService?.isNetworkAndServerAvailable { result: Boolean? ->
            if (!result!!) return@isNetworkAndServerAvailable
            binding.emptyList.emptyListViewHeadline.setText(R.string.file_list_loading)
            binding.emptyList.emptyListViewText.text = ""
            binding.emptyList.emptyListIcon.visibility = View.GONE
        }
    }

    private fun initializeClient() {
        if (client == null && optionalUser?.isPresent == true) {
            try {
                val user = optionalUser?.get()
                client = clientFactory.create(user)
            } catch (e: CreationException) {
                Log_OC.e(TAG, "Error initializing client", e)
            }
        }
    }

    private fun initializeAdapter() {
        initializeClient()
        if (adapter == null) {
            adapter = GalleryAdapter(
                requireContext(),
                accountManager.user,
                this,
                preferences,
                mContainerActivity!!,
                viewThemeUtils,
                columnSize,
                ThumbnailsCacheManager.getThumbnailDimension()
            )
            adapter?.setHasStableIds(true)
            setUpActionMode()
        }
        binding.listRoot.adapter = adapter

        lastMediaItemPosition?.let {
            binding.listRoot.layoutManager?.scrollToPosition(it)
        }
    }

    private fun setMessageForEmptyList(
        @StringRes headline: Int,
        message: String,
        @DrawableRes icon: Int,
        tintIcon: Boolean
    ) {
        binding.emptyList.emptyListViewHeadline.setText(headline)
        binding.emptyList.emptyListViewText.text = message

        if (tintIcon) {
            if (context != null) {
                binding.emptyList.emptyListIcon.setImageDrawable(
                    viewThemeUtils.platform.tintPrimaryDrawable(requireContext(), icon)
                )
            }
        } else {
            binding.emptyList.emptyListIcon.setImageResource(icon)
        }

        binding.emptyList.emptyListIcon.visibility = View.VISIBLE
        binding.emptyList.emptyListViewText.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        if (requireActivity() is FileDisplayActivity) {
            (requireActivity() as FileDisplayActivity).setupToolbar()
            (requireActivity() as FileDisplayActivity).supportActionBar?.let { actionBar ->
                viewThemeUtils.files.themeActionBar(requireContext(), actionBar, albumName)
            }
            (requireActivity() as FileDisplayActivity).showSortListGroup(false)
            (requireActivity() as FileDisplayActivity).setMainFabVisible(false)

            // clear the subtitle while navigating to any other screen from Media screen
            (requireActivity() as FileDisplayActivity).clearToolbarSubtitle()
        }
    }

    override fun onPause() {
        super.onPause()
        adapter?.cancelAllPendingTasks()
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            columnSize = MAX_COLUMN_SIZE_LANDSCAPE
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            columnSize = MAX_COLUMN_SIZE_PORTRAIT
        }
        adapter?.changeColumn(columnSize)
        adapter?.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        lastMediaItemPosition = 0
    }

    override fun getColumnsCount(): Int {
        return columnSize
    }

    override fun onShareIconClick(file: OCFile?) {
        // nothing to do here
    }

    override fun showShareDetailView(file: OCFile?) {
        // nothing to do here
    }

    override fun showActivityDetailView(file: OCFile?) {
        // nothing to do here
    }

    override fun onOverflowIconClicked(file: OCFile?, view: View?) {
        // nothing to do here
    }

    override fun onItemClicked(file: OCFile) {
        if (adapter?.isMultiSelect() == true) {
            toggleItemToCheckedList(file)
        } else {
            if (PreviewImageFragment.canBePreviewed(file)) {
                (mContainerActivity as FileDisplayActivity).startImagePreview(file, !file.isDown)
            } else if (file.isDown) {
                if (canBePreviewed(file)) {
                    (mContainerActivity as FileDisplayActivity).startMediaPreview(file, 0, true, true, false, true)
                } else {
                    mContainerActivity?.getFileOperationsHelper()?.openFile(file)
                }
            } else {
                if (canBePreviewed(file) && !file.isEncrypted) {
                    (mContainerActivity as FileDisplayActivity).startMediaPreview(file, 0, true, true, true, true)
                } else {
                    Log_OC.d(TAG, "Couldn't handle item click")
                }
            }
        }
    }

    override fun onLongItemClicked(file: OCFile): Boolean {
        // Create only once instance of action mode
        if (mMultiChoiceModeListener?.mActiveActionMode != null) {
            toggleItemToCheckedList(file)
        } else {
            requireActivity().startActionMode(mMultiChoiceModeListener)
            adapter?.addCheckedFile(file)
        }
        mMultiChoiceModeListener?.updateActionModeFile(file)
        return true
    }

    /**
     * Will toggle a file selection status from the action mode
     *
     * @param file The concerned OCFile by the selection/deselection
     */
    private fun toggleItemToCheckedList(file: OCFile) {
        adapter?.run {
            if (isCheckedFile(file)) {
                removeCheckedFile(file)
            } else {
                addCheckedFile(file)
            }
        }
        mMultiChoiceModeListener?.updateActionModeFile(file)
    }

    override fun isLoading(): Boolean {
        return false
    }

    override fun onHeaderClicked() {
        // nothing to do here
    }

    fun onAlbumRenamed(newAlbumName: String) {
        albumName = newAlbumName
        if (requireActivity() is FileDisplayActivity) {
            (requireActivity() as FileDisplayActivity).updateActionBarTitleAndHomeButtonByString(albumName)
        }
    }

    fun onAlbumDeleted() {
        requireActivity().supportFragmentManager.popBackStack()
    }

    private fun openActionsMenu(filesCount: Int, checkedFiles: Set<OCFile>) {
        throttler.run("overflowClick") {
            var toHide: MutableList<Int>? = ArrayList()
            for (file in checkedFiles) {
                if (file.isOfflineOperation) {
                    toHide = ArrayList(
                        listOf(
                            R.id.action_favorite,
                            R.id.action_move_or_copy,
                            R.id.action_sync_file,
                            R.id.action_encrypted,
                            R.id.action_unset_encrypted,
                            R.id.action_edit,
                            R.id.action_download_file,
                            R.id.action_export_file,
                            R.id.action_set_as_wallpaper
                        )
                    )
                    break
                }
            }

            toHide?.apply {
                addAll(
                    listOf(
                        R.id.action_move_or_copy,
                        R.id.action_sync_file,
                        R.id.action_encrypted,
                        R.id.action_unset_encrypted,
                        R.id.action_edit,
                        R.id.action_download_file,
                        R.id.action_export_file,
                        R.id.action_set_as_wallpaper,
                        R.id.action_send_file,
                        R.id.action_send_share_file,
                        R.id.action_see_details,
                        R.id.action_rename_file,
                        R.id.action_pin_to_homescreen
                    )
                )
            }

            val childFragmentManager = childFragmentManager
            val actionBottomSheet = newInstance(filesCount, checkedFiles, true, toHide)
                .setResultListener(
                    childFragmentManager,
                    this
                ) { id: Int -> onFileActionChosen(id, checkedFiles) }
            if (this.isDialogFragmentReady()) {
                actionBottomSheet.show(childFragmentManager, "actions")
            }
        }
    }

    @Suppress("ReturnCount")
    private fun onFileActionChosen(@IdRes itemId: Int, checkedFiles: Set<OCFile>): Boolean {
        if (checkedFiles.isEmpty()) {
            return false
        }

        when (itemId) {
            R.id.action_remove_file -> {
                onRemoveFileOperation(checkedFiles)
                return true
            }

            R.id.action_favorite -> {
                mContainerActivity?.fileOperationsHelper?.toggleFavoriteFiles(checkedFiles, true)
                return true
            }

            R.id.action_unset_favorite -> {
                mContainerActivity?.fileOperationsHelper?.toggleFavoriteFiles(checkedFiles, false)
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

            else -> return true
        }
    }

    /**
     * De-/select all elements in the current list view.
     *
     * @param select `true` to select all, `false` to deselect all
     */
    @SuppressLint("NotifyDataSetChanged")
    private fun selectAllFiles(select: Boolean) {
        adapter?.let {
            it.selectAll(select)
            it.notifyDataSetChanged()
            mMultiChoiceModeListener?.invalidateActionMode()
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onMessageEvent(event: FavoriteEvent) {
        try {
            val user = accountManager.user
            val client = clientFactory.create(user)
            val toggleFavoriteOperation = ToggleAlbumFavoriteRemoteOperation(
                event.shouldFavorite,
                event.remotePath
            )
            val remoteOperationResult = toggleFavoriteOperation.execute(client)

            if (remoteOperationResult.isSuccess) {
                Handler(Looper.getMainLooper()).post {
                    mMultiChoiceModeListener?.exitSelectionMode()
                }
                adapter?.markAsFavorite(event.remotePath, event.shouldFavorite)
            }
        } catch (e: CreationException) {
            Log_OC.e(TAG, "Error processing event", e)
        }
    }

    private fun onRemoveFileOperation(files: Collection<OCFile>) {
        lifecycleScope.launch(Dispatchers.IO) {
            val removeFailedFiles = mutableListOf<OCFile>()
            try {
                val user = accountManager.user
                val client = clientFactory.create(user)
                withContext(Dispatchers.Main) {
                    showDialog(true)
                }
                if (files.size == 1) {
                    val removeAlbumFileRemoteOperation = RemoveAlbumFileRemoteOperation(
                        files.first().remotePath
                    )
                    val remoteOperationResult = removeAlbumFileRemoteOperation.execute(client)

                    if (!remoteOperationResult.isSuccess) {
                        withContext(Dispatchers.Main) {
                            DisplayUtils.showSnackMessage(
                                requireActivity(),
                                ErrorMessageAdapter.getErrorCauseMessage(
                                    remoteOperationResult,
                                    removeAlbumFileRemoteOperation,
                                    resources
                                )
                            )
                        }
                    }
                } else {
                    for (file in files) {
                        val removeAlbumFileRemoteOperation = RemoveAlbumFileRemoteOperation(
                            file.remotePath
                        )
                        val remoteOperationResult = removeAlbumFileRemoteOperation.execute(client)

                        if (!remoteOperationResult.isSuccess) {
                            removeFailedFiles.add(file)
                        }
                    }
                }
            } catch (e: CreationException) {
                Log_OC.e(TAG, "Error processing event", e)
            }

            Log_OC.d(TAG, "Files removed: ${removeFailedFiles.size}")

            withContext(Dispatchers.Main) {
                if (removeFailedFiles.isNotEmpty()) {
                    DisplayUtils.showSnackMessage(
                        requireActivity(),
                        requireContext().resources.getString(R.string.album_delete_failed_message)
                    )
                }
                showDialog(false)

                // refresh data
                fetchAndSetData()
            }
        }
    }

    private fun showDialog(isShow: Boolean) {
        if (requireActivity() is FileDisplayActivity) {
            if (isShow) {
                (requireActivity() as FileDisplayActivity).showLoadingDialog(
                    requireContext().resources.getString(
                        R.string.wait_a_moment
                    )
                )
            } else {
                (requireActivity() as FileDisplayActivity).dismissLoadingDialog()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
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
        val activity: FragmentActivity,
        val adapter: GalleryAdapter?,
        val viewThemeUtils: ViewThemeUtils,
        val openActionsMenu: (Int, Set<OCFile>) -> Unit
    ) : AbsListView.MultiChoiceModeListener, DrawerLayout.DrawerListener {

        var mActiveActionMode: ActionMode? = null
        private var mIsActionModeNew = false

        /**
         * True when action mode is finished because the drawer was opened
         */
        private var mActionModeClosedByDrawer = false

        /**
         * Selected items in list when action mode is closed by drawer
         */
        private val mSelectionWhenActionModeClosedByDrawer: MutableSet<OCFile> = HashSet()

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
                        it.getCheckedItems()
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
            val checkedFiles: Set<OCFile> = adapter?.getCheckedItems() ?: emptySet()
            val checkedCount = checkedFiles.size
            val title: String =
                activity.resources.getQuantityString(R.plurals.items_selected_count, checkedCount, checkedCount)
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
         * @param file The concerned OCFile to refresh in adapter
         */
        fun updateActionModeFile(file: OCFile) {
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
                val checkedFiles: Set<OCFile> = it.getCheckedItems()
                if (item.itemId == R.id.custom_menu_placeholder_item) {
                    openActionsMenu(it.getFilesCount(), checkedFiles)
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

    private val activityResult: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { intentResult: ActivityResult ->
        if (Activity.RESULT_OK == intentResult.resultCode) {
            intentResult.data?.let {
                val paths = it.getStringArrayListExtra(AlbumsPickerActivity.EXTRA_MEDIA_FILES_PATH)
                paths?.let { p ->
                    addMediaToAlbum(p.toMutableList())
                }
            }
        }
    }

    private fun openGalleryToAddMedia() {
        activityResult.launch(intentForPickingMediaFiles(requireActivity()))
    }

    private fun addMediaToAlbum(filePaths: MutableList<String>) {
        viewLifecycleOwner.lifecycleScope.launch {
            // short delay to let other transactions finish
            // else showLoadingDialog will throw exception
            delay(SLEEP_DELAY)
            mContainerActivity?.fileOperationsHelper?.albumCopyFiles(filePaths, albumName)
        }
    }

    fun refreshData() {
        fetchAndSetData()
    }

    companion object {
        val TAG: String = AlbumItemsFragment::class.java.simpleName
        private const val ARG_ALBUM_NAME = "album_name"
        private const val ARG_IS_NEW_ALBUM = "is_new_album"
        var lastMediaItemPosition: Int? = null

        private const val MAX_COLUMN_SIZE_LANDSCAPE: Int = 5
        private const val MAX_COLUMN_SIZE_PORTRAIT: Int = 2

        private const val SLEEP_DELAY = 100L

        fun newInstance(albumName: String, isNewAlbum: Boolean = false): AlbumItemsFragment {
            val args = Bundle()

            val fragment = AlbumItemsFragment()
            fragment.arguments = args
            args.putString(ARG_ALBUM_NAME, albumName)
            args.putBoolean(ARG_IS_NEW_ALBUM, isNewAlbum)
            return fragment
        }
    }
}
