/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 TSI-mc
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Nextcloud GmbH
 * SPDX-License-Identifier: GPL-3.0-or-later AND AGPL-3.0-or-later
 */
package com.owncloud.android.ui.fragment

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nextcloud.utils.extensions.getGalleryItemsPageSuspended
import com.nextcloud.utils.extensions.getParcelableArgument
import com.nextcloud.utils.extensions.getTypedActivity
import com.nextcloud.utils.extensions.isLandscape
import com.nextcloud.utils.extensions.toGalleryItems
import com.owncloud.android.BuildConfig
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.EmptyRecyclerView
import com.owncloud.android.ui.activity.AlbumsPickerActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.activity.FolderPickerActivity
import com.owncloud.android.ui.activity.ToolbarActivity
import com.owncloud.android.ui.adapter.CommonOCFileListAdapterInterface
import com.owncloud.android.ui.adapter.GalleryAdapter
import com.owncloud.android.ui.asynctasks.GallerySearchTask
import com.owncloud.android.ui.events.ChangeMenuEvent
import com.owncloud.android.ui.fragment.GalleryFragmentBottomSheetDialog.MediaState
import com.owncloud.android.ui.fragment.helper.ColumnCount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("ForbiddenComment", "ReturnCount", "MagicNumber", "MaxLineLength")
class GalleryFragment :
    OCFileListFragment(),
    GalleryFragmentBottomSheetActions {
    var isPhotoSearchQueryRunning: Boolean = false
    private var photoSearchTask: Job? = null
    private var showGalleryJob: Job? = null
    private var endDate: Long = 0
    private val limit = 150
    private var loadedItemCount = INITIAL_GALLERY_WINDOW
    private var restoreScrollPending = false
    private var adapter: GalleryAdapter? = null

    private var bottomSheet: GalleryFragmentBottomSheetDialog? = null

    override var columnsCount: Int = 0
        private set

    private var isFromAlbum = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        searchFragment = true
        arguments?.let {
            isFromAlbum = it.getBoolean(AlbumsPickerActivity.EXTRA_FROM_ALBUM, false)
        }
        bottomSheet = GalleryFragmentBottomSheetDialog()
        columnsCount = ColumnCount.Wide.get(resources.isLandscape())
        registerRefreshSearchEventReceiver()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!isFromAlbum) {
            addMenuProvider()
        }
    }

    private fun addMenuProvider() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.fragment_gallery_three_dots, menu)
                    val menuItem = menu.findItem(R.id.action_three_dot_icon)
                    viewThemeUtils.platform.colorMenuItemText(requireContext(), menuItem)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    val sheet = bottomSheet ?: return false
                    if (menuItem.itemId != R.id.action_three_dot_icon) {
                        return false
                    }

                    if (!sheet.isVisible) {
                        sheet.show(childFragmentManager, FRAGMENT_TAG_BOTTOM_SHEET)
                    }

                    return true
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    private fun registerRefreshSearchEventReceiver() {
        val filter = IntentFilter(REFRESH_SEARCH_EVENT_RECEIVER)
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(refreshSearchEventReceiver, filter)
    }

    private val refreshSearchEventReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            getTypedActivity(FileDisplayActivity::class.java)?.startPhotoSearch(R.id.nav_gallery)
        }
    }

    override fun onDestroyView() {
        showGalleryJob?.cancel()
        showGalleryJob = null

        photoSearchTask?.cancel()
        photoSearchTask = null

        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(refreshSearchEventReceiver)

        adapter?.cleanup()

        super.onDestroyView()
    }

    override fun onPause() {
        super.onPause()
        photoSearchTask?.cancel()
        savedScrollState = recyclerView?.layoutManager?.onSaveInstanceState()
        savedLoadedItemCount = loadedItemCount
        savedMediaState = bottomSheet?.currMediaState
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = super.onCreateView(inflater, container, savedInstanceState)

        recyclerView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                loadMoreWhenEndReached(recyclerView, dy)
            }
        })

        Log_OC.i(this, "onCreateView() in GalleryFragment end")
        return v
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        currentSearchType = SearchType.GALLERY_SEARCH

        menuItemAddRemoveValue = MenuItemAddRemove.REMOVE_GRID_AND_SORT
        requireActivity().invalidateOptionsMenu()

        if (savedMediaState != null) {
            bottomSheet?.currMediaState = savedMediaState!!
        }
        updateSubtitle(bottomSheet?.currMediaState)

        // Restore the previously loaded window so a saved scroll position still resolves to a valid item.
        loadedItemCount = savedLoadedItemCount ?: INITIAL_GALLERY_WINDOW
        restoreScrollPending = savedScrollState != null

        handleSearchEvent()
    }

    override fun setAdapter(args: Bundle?) {
        adapter = GalleryAdapter(
            requireContext(),
            accountManager.user,
            this,
            mContainerActivity,
            viewThemeUtils,
            this.columnsCount,
            ThumbnailsCacheManager.getThumbnailDimension(),
            thumbnailGenerator
        )
        adapter?.setHasStableIds(true)
        setRecyclerViewAdapter(adapter)

        // update the footer as there is no footer shown in media view
        if (recyclerView is EmptyRecyclerView) {
            (recyclerView as EmptyRecyclerView).setHasFooter(false)
        }

        val layoutManager = GridLayoutManager(context, 1)
        adapter?.setLayoutManager(layoutManager)
        recyclerView?.setLayoutManager(layoutManager)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        columnsCount = ColumnCount.Wide.get(newConfig.isLandscape())
        adapter?.changeColumn(columnsCount)
        showAllGalleryItems()
    }

    override fun onRefresh() {
        super.onRefresh()
        handleSearchEvent()
    }

    override fun getCommonAdapter(): CommonOCFileListAdapterInterface? = adapter

    val currentMediaState: MediaState?
        get() = bottomSheet?.currMediaState

    override fun onResume() {
        super.onResume()

        val fda = getTypedActivity(FileDisplayActivity::class.java)
        fda?.updateActionBarTitleAndHomeButtonByString(getString(R.string.drawer_item_gallery))
        fda?.setMainFabVisible(false)
    }

    override fun onMessageEvent(changeMenuEvent: ChangeMenuEvent) {
        super.onMessageEvent(changeMenuEvent)
    }

    private fun handleSearchEvent() {
        prepareCurrentSearch(searchEvent)
        setEmptyListMessage(EmptyListState.LOADING)

        // always show first stored items
        showAllGalleryItems()

        setFabVisible(false)

        searchAndDisplay()
    }

    private fun searchAndDisplay() {
        if (!isPhotoSearchQueryRunning && endDate <= 0) {
            // fix an issue when the method is called after loading the gallery and pressing play on a movie
            // to avoid reloading, check if endDate has already a value which is not -1 or 0
            endDate = System.currentTimeMillis() / 1000
            isPhotoSearchQueryRunning = true
            runGallerySearchTask()
        }
    }

    fun searchCompleted(result: GallerySearchTask.Result) {
        if (!isAdded) return

        this.isPhotoSearchQueryRunning = false

        if (result.resultCode == RemoteOperationResult.ResultCode.OUT_OF_MEMORY) {
            setEmptyListMessage(EmptyListState.OUT_OF_MEMORY)
            return
        }

        if (result.lastTimestamp > -1) {
            endDate = result.lastTimestamp
        }

        if (adapter?.isEmpty() == true) {
            setEmptyListMessage(SearchType.GALLERY_SEARCH)
        }

        if (!result.emptySearch) {
            showAllGalleryItems()
        }

        Log_OC.d(this, "End gallery search")
    }

    override fun selectMediaFolder() {
        val intent = Intent(requireActivity(), FolderPickerActivity::class.java).apply {
            putExtra(FolderPickerActivity.EXTRA_ACTION, FolderPickerActivity.CHOOSE_LOCATION)
        }
        folderPickerLauncher.launch(intent)
    }

    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val chosenFolder = data?.getParcelableArgument(FolderPickerActivity.EXTRA_FOLDER, OCFile::class.java)

            if (chosenFolder != null) {
                preferences.setLastSelectedMediaFolder(chosenFolder.remotePath)
                searchAndDisplayAfterChangingFolder()
            }
        }
    }

    private fun searchAndDisplayAfterChangingFolder() {
        // TODO: Fix folder change, it seems it doesn't work at all
        loadedItemCount = INITIAL_GALLERY_WINDOW
        restoreScrollPending = false
        clearSavedViewState()
        endDate = System.currentTimeMillis() / 1000
        isPhotoSearchQueryRunning = true
        runGallerySearchTask()
    }

    private fun runGallerySearchTask() {
        if (mContainerActivity == null) {
            Log_OC.w(TAG, "container activity is null, can't run search task")
            return
        }

        photoSearchTask = GallerySearchTask(
            this,
            accountManager.user,
            mContainerActivity.getStorageManager(),
            endDate,
            limit
        ).execute()
    }

    private fun loadMoreWhenEndReached(recyclerView: RecyclerView, dy: Int) {
        if (dy <= 0 || isPhotoSearchQueryRunning) {
// scrolling up or search query already active, do not search gallery
            return
        }

        if (recyclerView.layoutManager !is GridLayoutManager) {
            Log_OC.e(TAG, "can't load more; layout manager is not LinearLayoutManager or GridLayoutManager")
            return
        }

        val gridLayoutManager = recyclerView.layoutManager as GridLayoutManager
        val totalItemCount: Int = gridLayoutManager.getItemCount()
        val lastVisibleItem: Int = gridLayoutManager.findLastCompletelyVisibleItemPosition()
        val visibleItemCount: Int = gridLayoutManager.childCount

        if (lastVisibleItem == RecyclerView.NO_POSITION) {
            return
        }

        val lastFile = adapter?.getItem(lastVisibleItem - 1) ?: return
        val lastItemTimestamp = lastFile.modificationTimestamp / 1000

        // if we have already older media in the gallery then retrieve file in chronological order to fill the gap
        if (lastItemTimestamp < this.endDate) {
            if (BuildConfig.DEBUG) {
                Log_OC.d(this, "Gallery swipe: retrieve items to check the chronology")
            }

            this.isPhotoSearchQueryRunning = true
            runGallerySearchTask()
            // no more files in the gallery, retrieve the next ones
        } else if ((totalItemCount - visibleItemCount) <= (lastVisibleItem + MAX_ITEMS_PER_ROW) &&
            (totalItemCount - visibleItemCount) > 0
        ) {
            if (BuildConfig.DEBUG) {
                Log_OC.d(this, "Gallery swipe: retrieve items because end of gallery display")
            }

            // Almost reached the end, widen the display window and continue to load new photos
            endDate = lastItemTimestamp
            loadedItemCount += GALLERY_WINDOW_INCREMENT
            showAllGalleryItems()
            isPhotoSearchQueryRunning = true
            runGallerySearchTask()
        }
    }

    override fun updateMediaContent(mediaState: MediaState) {
        loadedItemCount = INITIAL_GALLERY_WINDOW
        restoreScrollPending = false
        clearSavedViewState()
        showAllGalleryItems()
    }

    fun showAllGalleryItems() {
        val mediaState = bottomSheet?.currMediaState ?: return

        val mimeFilter = when (mediaState) {
            MediaState.MEDIA_STATE_PHOTOS_ONLY -> IMAGE_MIME_FILTER
            MediaState.MEDIA_STATE_VIDEOS_ONLY -> VIDEO_MIME_FILTER
            else -> null
        }

        showGalleryJob?.cancel()
        showGalleryJob = lifecycleScope.launch(Dispatchers.Default) {
            val remotePath = preferences.getLastSelectedMediaFolder()
            val items = mContainerActivity.storageManager.getGalleryItemsPageSuspended(
                remotePath,
                mimeFilter,
                loadedItemCount
            )

            val galleryItems = items.toGalleryItems(columnsCount, ThumbnailsCacheManager.getThumbnailDimension())

            withContext(Dispatchers.Main) {
                if (galleryItems.isEmpty()) {
                    setEmptyListMessage(SearchType.GALLERY_SEARCH)
                }
                adapter?.updateList(galleryItems)
                updateSubtitle(mediaState)

                if (restoreScrollPending && galleryItems.isNotEmpty()) {
                    restoreScrollPending = false
                    savedScrollState?.let { state ->
                        savedScrollState = null
                        recyclerView?.layoutManager?.onRestoreInstanceState(state)
                    }
                }
            }
        }
    }

    private fun updateSubtitle(mediaState: MediaState?) {
        val toolbarActivity = getTypedActivity(ToolbarActivity::class.java)
        if (!isAdded || toolbarActivity == null || isFromAlbum) {
            return
        }

        toolbarActivity.runOnUiThread {
            if (!isAdded) {
                return@runOnUiThread
            }

            val subTitle = when (mediaState) {
                MediaState.MEDIA_STATE_PHOTOS_ONLY -> {
                    resources.getString(R.string.subtitle_photos_only)
                }

                MediaState.MEDIA_STATE_VIDEOS_ONLY -> {
                    resources.getString(R.string.subtitle_videos_only)
                }

                else -> {
                    resources.getString(R.string.subtitle_photos_videos)
                }
            }

            toolbarActivity.updateToolbarSubtitle(subTitle)
        }
    }

    fun addImagesToAlbum(checkedFiles: Set<OCFile>) {
        if (!isFromAlbum) {
            return
        }

        getTypedActivity(AlbumsPickerActivity::class.java)?.addFilesToAlbum(checkedFiles)
        exitSelectionMode()
        requireActivity().finish()
    }

    override fun setGridViewColumns(scaleFactor: Float) = Unit

    fun markAsFavorite(remotePath: String, favorite: Boolean) {
        adapter?.markAsFavorite(remotePath, favorite)
    }

    companion object {
        private const val MAX_ITEMS_PER_ROW = 10
        private const val FRAGMENT_TAG_BOTTOM_SHEET = "data"

        private const val INITIAL_GALLERY_WINDOW = 500
        private const val GALLERY_WINDOW_INCREMENT = 500
        private const val IMAGE_MIME_FILTER = "image/%"
        private const val VIDEO_MIME_FILTER = "video/%"

        const val REFRESH_SEARCH_EVENT_RECEIVER: String = "refreshSearchEventReceiver"

        // Kept across the activity recreation that happens when returning from the media preview,
        // so the grid reopens at the same scroll position and with the same loaded window.
        private var savedScrollState: Parcelable? = null
        private var savedLoadedItemCount: Int? = null
        private var savedMediaState: MediaState? = null

        fun clearSavedViewState() {
            savedScrollState = null
            savedLoadedItemCount = null
            savedMediaState = null
        }
    }
}
