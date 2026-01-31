/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.fragment.albums

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
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
import androidx.core.view.get
import androidx.core.view.size
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.appbar.AppBarLayout
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.jobs.upload.FileUploadWorker
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.client.network.ClientFactory.CreationException
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.client.utils.Throttler
import com.nextcloud.ui.albumItemActions.AlbumItemActionsBottomSheet
import com.nextcloud.ui.fileactions.FileActionsBottomSheet
import com.nextcloud.utils.extensions.getTypedActivity
import com.nextcloud.utils.extensions.isDialogFragmentReady
import com.owncloud.android.R
import com.owncloud.android.databinding.ListFragmentBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.datamodel.VirtualFolderType
import com.owncloud.android.db.ProviderMeta
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.albums.ReadAlbumItemsRemoteOperation
import com.owncloud.android.lib.resources.albums.RemoveAlbumFileRemoteOperation
import com.owncloud.android.lib.resources.albums.ToggleAlbumFavoriteRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.lib.resources.status.Type
import com.owncloud.android.operations.albums.ReadAlbumItemsOperation
import com.owncloud.android.ui.activity.AlbumsPickerActivity
import com.owncloud.android.ui.activity.AlbumsPickerActivity.Companion.intentForPickingMediaFiles
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.activity.FileActivity.REQUEST_CODE__LAST_SHARED
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.adapter.GalleryAdapter
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment.ConfirmationDialogFragmentListener
import com.owncloud.android.ui.dialog.CreateAlbumDialogFragment
import com.owncloud.android.ui.events.FavoriteEvent
import com.owncloud.android.ui.fragment.FileFragment
import com.owncloud.android.ui.helpers.UriUploader
import com.owncloud.android.ui.interfaces.OCFileListFragmentInterface
import com.owncloud.android.ui.preview.PreviewImageFragment
import com.owncloud.android.ui.preview.PreviewMediaActivity.Companion.canBePreviewed
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.ErrorMessageAdapter
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.Optional
import java.util.function.Supplier
import javax.inject.Inject

@Suppress("TooManyFunctions", "LargeClass")
class AlbumItemsFragment :
    Fragment(),
    OCFileListFragmentInterface,
    Injectable {

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

    private var albumRemoteFileList = listOf<RemoteFile>()

    private val refreshFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

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

    @OptIn(FlowPreview::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        optionalUser = Optional.of(accountManager.user)
        showAppBar()
        createMenu()
        setupContainingList()
        setupContent()

        // if fragment is opened when new albums is created
        // then open gallery to choose media to add
        if (isNewAlbum) {
            openGalleryToAddMedia()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                refreshFlow.onStart { emit(Unit) } // default fetch
                    .onEach { binding.swipeContainingList.isRefreshing = true } // show progress on each call
                    .debounce(DEBOUNCE_DELAY) // debounce background triggers
                    .collect {
                        fetchAndSetData()
                    }
            }
        }
    }

    private fun showAppBar() {
        if (requireActivity() is FileDisplayActivity) {
            val appBarLayout = requireActivity().findViewById<AppBarLayout>(R.id.appbar)
            appBarLayout?.setExpanded(true, false)
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

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
                    R.id.action_three_dot_icon -> {
                        openAlbumActionsMenu()
                        true
                    }

                    R.id.action_add_from_camera_roll -> {
                        // we don't want quick media access bottom sheet for Android 13+ devices
                        // to avoid that we are not using image/* and video/* mime types
                        // we are validating mime types when selection is made
                        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                            type = "*/*"
                            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                            addCategory(Intent.CATEGORY_OPENABLE)
                        }
                        startActivityForResult(
                            Intent.createChooser(intent, getString(R.string.upload_chooser_title)),
                            REQUEST_CODE__SELECT_MEDIA_FROM_APPS
                        )
                        true
                    }

                    R.id.action_add_from_account -> {
                        // open Gallery fragment as selection then add items to current album
                        openGalleryToAddMedia()
                        true
                    }

                    else -> false
                }

                override fun onPrepareMenu(menu: Menu) {
                    super.onPrepareMenu(menu)
                    for (i in 0 until menu.size) {
                        val item = menu[i]
                        item.icon?.let {
                            item.setIcon(
                                viewThemeUtils.platform.colorDrawable(
                                    it,
                                    ContextCompat.getColor(requireContext(), R.color.fontAppbar)
                                )
                            )
                        }
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

    private fun onAlbumActionChosen(@IdRes itemId: Int): Boolean = when (itemId) {
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
            showConfirmationDialog(true, null)
            true
        }

        else -> false
    }

    private fun setupContent() {
        binding.listRoot.setEmptyView(binding.emptyList.emptyListView)
        val layoutManager = GridLayoutManager(requireContext(), 1)
        binding.listRoot.layoutManager = layoutManager
    }

    private fun setupContainingList() {
        viewThemeUtils.androidx.themeSwipeRefreshLayout(binding.swipeContainingList)
        binding.swipeContainingList.setOnRefreshListener {
            binding.swipeContainingList.isRefreshing = true
            refreshData()
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
        binding.swipeContainingList.isRefreshing = true
        mMultiChoiceModeListener?.exitSelectionMode()
        initializeAdapter()
        setEmptyListLoadingMessage()
        lifecycleScope.launch(Dispatchers.IO) {
            val readAlbumItemsRemoteOperation = ReadAlbumItemsOperation(albumName, mContainerActivity?.storageManager)
            val result = client?.let { readAlbumItemsRemoteOperation.execute(it) }
            val ocFileList = mutableListOf<OCFile>()

            if (result?.isSuccess == true && result.resultData != null) {
                mContainerActivity?.storageManager?.deleteVirtuals(VirtualFolderType.ALBUM)
                val contentValues = mutableListOf<ContentValues>()
                albumRemoteFileList = result.resultData.toMutableList()

                for (remoteFile in albumRemoteFileList) {
                    val ocFile = mContainerActivity?.storageManager?.getFileByLocalId(remoteFile.localId)
                    ocFile?.let {
                        ocFileList.add(it)

                        val cv = ContentValues()
                        cv.put(ProviderMeta.ProviderTableMeta.VIRTUAL_TYPE, VirtualFolderType.ALBUM.toString())
                        cv.put(ProviderMeta.ProviderTableMeta.VIRTUAL_OCFILE_ID, it.fileId)

                        contentValues.add(cv)
                    }
                }

                mContainerActivity?.storageManager?.saveVirtuals(contentValues)
            }
            withContext(Dispatchers.Main) {
                if (result?.isSuccess == true && result.resultData != null) {
                    if (result.resultData.isEmpty() || ocFileList.isEmpty()) {
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
        lastMediaItemPosition = 0
        super.onDestroyView()
    }

    override fun getColumnsCount(): Int = columnSize

    override fun onShareIconClick(file: OCFile?) = Unit

    override fun showShareDetailView(file: OCFile?) = Unit

    override fun showActivityDetailView(file: OCFile?) = Unit

    override fun onOverflowIconClicked(file: OCFile?, view: View?) = Unit

    override fun onItemClicked(file: OCFile) {
        if (adapter?.isMultiSelect() == true) {
            toggleItemToCheckedList(file)
        } else {
            if (PreviewImageFragment.canBePreviewed(file)) {
                (mContainerActivity as FileDisplayActivity).startImagePreview(
                    file,
                    VirtualFolderType.ALBUM,
                    !file.isDown
                )
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

    override fun isLoading(): Boolean = false

    override fun onHeaderClicked() = Unit

    fun onAlbumRenamed(newAlbumName: String) {
        albumName = newAlbumName
        if (requireActivity() is FileDisplayActivity) {
            (requireActivity() as FileDisplayActivity).updateActionBarTitleAndHomeButtonByString(albumName)
        }
    }

    fun onAlbumDeleted() {
        requireActivity().supportFragmentManager.popBackStack()
    }

    @Suppress("LongMethod")
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
            val endpoints = mContainerActivity?.storageManager?.getCapability(
                optionalUser?.get()
            )?.getClientIntegrationEndpoints(
                Type.CONTEXT_MENU,
                checkedFiles.iterator().next().mimeType
            )

            val actionBottomSheet = FileActionsBottomSheet.newInstance(
                filesCount,
                checkedFiles,
                true,
                toHide,
                false,
                endpoints!!
            )
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
                showConfirmationDialog(false, checkedFiles)
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

            R.id.action_open_file_with -> {
                // use only first element as this option will only be shown for single file selection
                mContainerActivity?.fileOperationsHelper?.openFile(checkedFiles.first())
                return true
            }

            R.id.action_stream_media -> {
                // use only first element as this option will only be shown for single file selection
                mContainerActivity?.fileOperationsHelper?.streamMediaFile(checkedFiles.first())
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
                        getAlbumRemotePathForRemoval(files.first())
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
                            getAlbumRemotePathForRemoval(file)
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
                refreshData()
            }
        }
    }

    // since after files data are fetched in media the file remote path will be actual instead of Albums prefixed
    // to remove the file properly form the albums we have to provide the correct album path
    private fun getAlbumRemotePathForRemoval(ocFile: OCFile): String {
        if (!ocFile.remotePath.startsWith("/albums/$albumName")) {
            return albumRemoteFileList.find { it.etag == ocFile.etag || it.etag == ocFile.etagOnServer }?.remotePath
                ?: ocFile.remotePath
        }
        return ocFile.remotePath
    }

    private fun showConfirmationDialog(isAlbum: Boolean, files: Collection<OCFile>?) {
        val messagePair = getConfirmationDialogMessage(isAlbum, files)
        val errorDialog = ConfirmationDialogFragment.newInstance(
            messageResId = messagePair.first,
            messageArguments = arrayOf(messagePair.second),
            titleResId = -1,
            positiveButtonTextId = R.string.file_delete,
            negativeButtonTextId = R.string.file_keep,
            neutralButtonTextId = -1
        )
        errorDialog.setCancelable(false)
        errorDialog.setOnConfirmationListener(
            object : ConfirmationDialogFragmentListener {
                override fun onConfirmation(callerTag: String?) {
                    if (isAlbum) {
                        mContainerActivity?.getFileOperationsHelper()?.removeAlbum(albumName)
                    } else {
                        files?.let {
                            onRemoveFileOperation(it)
                        }
                    }
                }

                override fun onNeutral(callerTag: String?) {
                    // not used at the moment
                }

                override fun onCancel(callerTag: String?) {
                    // not used at the moment
                }
            }
        )
        errorDialog.show(requireActivity().supportFragmentManager, ConfirmationDialogFragment.FTAG_CONFIRMATION)
    }

    private fun getConfirmationDialogMessage(isAlbum: Boolean, files: Collection<OCFile>?): Pair<Int, String?> {
        if (isAlbum) {
            return Pair(R.string.confirmation_remove_folder_alert, albumName)
        }

        return if (files?.size == SINGLE_SELECTION) {
            Pair(R.string.confirmation_remove_file_alert, files.first().fileName)
        } else {
            Pair(R.string.confirmation_remove_files_alert, null)
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
        private val mSelectionWhenActionModeClosedByDrawer: MutableSet<OCFile> = HashSet()

        override fun onDrawerSlide(drawerView: View, slideOffset: Float) = Unit

        override fun onDrawerOpened(drawerView: View) = Unit

        /**
         * When the navigation drawer is closed, action mode is recovered in the same state as was when the drawer was
         * (started to be) opened.
         *
         * @param drawerView Navigation drawer just closed.
         */
        override fun onDrawerClosed(drawerView: View) {
            if (mActionModeClosedByDrawer && mSelectionWhenActionModeClosedByDrawer.isNotEmpty()) {
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
        override fun onItemCheckedStateChanged(mode: ActionMode, position: Int, id: Long, checked: Boolean) = Unit

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
        refreshFlow.tryEmit(Unit)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data != null &&
            requestCode == REQUEST_CODE__SELECT_MEDIA_FROM_APPS && resultCode == RESULT_OK
        ) {
            requestUploadOfContentFromApps(data)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    // method referenced from FileDisplayActivity#requestUploadOfContentFromApps
    private fun requestUploadOfContentFromApps(contentIntent: Intent) {
        val clipData = contentIntent.clipData
        val uris = mutableListOf<Uri>()

        if (clipData != null) {
            for (i in 0 until clipData.itemCount) {
                uris.add(clipData.getItemAt(i).uri)
            }
        } else {
            contentIntent.data?.let { uris.add(it) }
        }

        // only accept images and videos mime type
        val validUris = uris.filter { uri ->
            val type = requireActivity().contentResolver.getType(uri)
            type?.startsWith("image/") == true || type?.startsWith("video/") == true
        }

        if (validUris.isEmpty()) {
            DisplayUtils.showSnackMessage(requireActivity(), R.string.album_unsupported_file)
            return
        }

        val streamsToUpload = ArrayList<Parcelable?>()
        streamsToUpload.addAll(validUris)

        // albums remote path for uploading
        val remotePath =
            "${resources.getString(R.string.instant_upload_path)}/${resources.getString(R.string.drawer_item_album)}/"

        if (requireActivity() is FileDisplayActivity) {
            val uploader = UriUploader(
                requireActivity() as FileDisplayActivity,
                streamsToUpload,
                remotePath,
                albumName,
                (requireActivity() as FileDisplayActivity).user.orElseThrow(
                    Supplier { RuntimeException() }
                ),
                FileUploadWorker.LOCAL_BEHAVIOUR_COPY,
                false, // Not show waiting dialog while file is being copied from private storage
                null // Not needed copy temp task listener
            )

            uploader.uploadUris()
        }
    }

    companion object {
        val TAG: String = AlbumItemsFragment::class.java.simpleName

        const val REQUEST_CODE__SELECT_MEDIA_FROM_APPS: Int = REQUEST_CODE__LAST_SHARED + 10

        private const val SINGLE_SELECTION = 1

        private const val ARG_ALBUM_NAME = "album_name"
        private const val ARG_IS_NEW_ALBUM = "is_new_album"
        var lastMediaItemPosition: Int? = null

        private const val MAX_COLUMN_SIZE_LANDSCAPE: Int = 5
        private const val MAX_COLUMN_SIZE_PORTRAIT: Int = 2

        private const val SLEEP_DELAY = 100L
        private const val DEBOUNCE_DELAY = 500L

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
