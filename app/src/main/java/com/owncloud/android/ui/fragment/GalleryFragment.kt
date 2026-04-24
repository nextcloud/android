/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 TSI-mc
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Nextcloud GmbH
 * SPDX-License-Identifier: GPL-3.0-or-later AND AGPL-3.0-or-later
 */
package com.owncloud.android.ui.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nextcloud.utils.extensions.getParcelableArgument
import kotlinx.coroutines.Job
import com.nextcloud.utils.extensions.getTypedActivity
import com.owncloud.android.BuildConfig
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.EmptyRecyclerView
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.activity.FolderPickerActivity
import com.owncloud.android.ui.activity.ToolbarActivity
import com.owncloud.android.ui.adapter.CommonOCFileListAdapterInterface
import com.owncloud.android.ui.adapter.GalleryAdapter
import com.owncloud.android.ui.asynctasks.GallerySearchTask
import com.owncloud.android.ui.events.ChangeMenuEvent
import com.owncloud.android.ui.fragment.GalleryFragmentBottomSheetDialog.MediaState

@Suppress("ForbiddenComment", "ReturnCount", "MagicNumber", "MaxLineLength")
class GalleryFragment :
    OCFileListFragment(),
    GalleryFragmentBottomSheetActions {
    var isPhotoSearchQueryRunning: Boolean = false
    private var photoSearchTask: Job? = null
    private var endDate: Long = 0
    private val limit = 150
    private var adapter: GalleryAdapter? = null

    private var bottomSheet: GalleryFragmentBottomSheetDialog? = null

    override var columnsCount: Int = 0
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        searchFragment = true

        setupBottomSheet()
        setupColumnCount()
        registerRefreshSearchEventReceiver()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addMenuProvider()
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
                    if (menuItem.itemId == R.id.action_three_dot_icon && bottomSheet != null) {
                        showBottomSheet()
                        return true
                    }

                    return false
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    private fun setupBottomSheet() {
        if (bottomSheet == null) {
            bottomSheet = GalleryFragmentBottomSheetDialog(this)
        }
    }

    private fun setupColumnCount() {
        columnsCount = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            MAX_LANDSCAPE_COLUMN_SIZE
        } else {
            MAX_PORTRAIT_COLUMN_SIZE
        }
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
        if (photoSearchTask != null) {
            photoSearchTask?.cancel()
            photoSearchTask = null
        }

        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(refreshSearchEventReceiver)

        setLastMediaItemPosition(null)

        adapter?.cleanup()

        super.onDestroyView()
    }

    override fun onPause() {
        super.onPause()
        photoSearchTask?.cancel()
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

        updateSubtitle(bottomSheet?.currMediaState)

        handleSearchEvent()
    }

    override fun setAdapter(args: Bundle?) {
        adapter = GalleryAdapter(
            requireContext(),
            accountManager.user,
            this,
            preferences,
            mContainerActivity,
            viewThemeUtils,
            this.columnsCount,
            ThumbnailsCacheManager.getThumbnailDimension()
        )
        adapter?.setHasStableIds(true)
        setRecyclerViewAdapter(adapter)

        // update the footer as there is no footer shown in media view
        if (recyclerView is EmptyRecyclerView) {
            (recyclerView as EmptyRecyclerView).setHasFooter(false)
        }

        if (recyclerView != null) {
            val layoutManager = GridLayoutManager(context, 1)
            adapter?.setLayoutManager(layoutManager)
            recyclerView?.setLayoutManager(layoutManager)

            if (lastMediaItemPosition != null) {
                layoutManager.scrollToPosition(lastMediaItemPosition!!)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            columnsCount = MAX_LANDSCAPE_COLUMN_SIZE
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            columnsCount = MAX_PORTRAIT_COLUMN_SIZE
        }

        adapter?.changeColumn(columnsCount)
        showAllGalleryItems()
    }

    override fun onRefresh() {
        super.onRefresh()
        handleSearchEvent()
    }

    override fun getCommonAdapter(): CommonOCFileListAdapterInterface? = adapter

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

    fun searchCompleted(emptySearch: Boolean, lastTimeStamp: Long) {
        if (!isAdded) return

        this.isPhotoSearchQueryRunning = false

        if (lastTimeStamp > -1) {
            endDate = lastTimeStamp
        }

        if (adapter?.isEmpty() == true) {
            setEmptyListMessage(SearchType.GALLERY_SEARCH)
        }

        if (!emptySearch) {
            showAllGalleryItems()
        }

        Log_OC.d(this, "End gallery search")
    }

    private fun showBottomSheet() {
        if (bottomSheet?.isVisible == false) {
            bottomSheet?.show(getChildFragmentManager(), FRAGMENT_TAG_BOTTOM_SHEET)
        }
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
        if (result.resultCode == android.app.Activity.RESULT_OK) {
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
        if (dy > 0 && !isPhotoSearchQueryRunning) {
            // scrolled vertical space not bigger than 0 or still searching
            return
        }

        if (recyclerView.layoutManager !is GridLayoutManager) {
            Log_OC.e(TAG, "can't load more layout manager is not grid")
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

            // Almost reached the end, continue to load new photos
            endDate = lastItemTimestamp
            isPhotoSearchQueryRunning = true
            runGallerySearchTask()
        }
    }

    override fun updateMediaContent(mediaState: MediaState) {
        showAllGalleryItems()
    }

    fun showAllGalleryItems() {
        val mediaState = bottomSheet?.currMediaState ?: return

        adapter?.showAllGalleryItems(
            preferences.getLastSelectedMediaFolder(),
            mediaState,
            this
        )
        updateSubtitle(mediaState)
    }

    private fun updateSubtitle(mediaState: MediaState?) {
        val toolbarActivity = getTypedActivity(ToolbarActivity::class.java)
        if (!isAdded || toolbarActivity == null) {
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

    override fun setGridViewColumns(scaleFactor: Float) = Unit

    fun markAsFavorite(remotePath: String, favorite: Boolean) {
        adapter?.markAsFavorite(remotePath, favorite)
    }

    companion object {
        private const val MAX_ITEMS_PER_ROW = 10
        private const val FRAGMENT_TAG_BOTTOM_SHEET = "data"

        private var lastMediaItemPosition: Int? = null
        const val REFRESH_SEARCH_EVENT_RECEIVER: String = "refreshSearchEventReceiver"

        private const val MAX_LANDSCAPE_COLUMN_SIZE = 5
        private const val MAX_PORTRAIT_COLUMN_SIZE = 2

        fun setLastMediaItemPosition(position: Int?) {
            lastMediaItemPosition = position
        }
    }
}
