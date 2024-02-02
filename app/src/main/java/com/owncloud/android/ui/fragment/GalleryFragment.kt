/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * @author TSI-mc
 * Copyright (C) 2019 Tobias Kaminsky
 * Copyright (C) 2019 Nextcloud GmbH
 * Copyright (C) 2023 TSI-mc
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
package com.owncloud.android.ui.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nextcloud.client.network.ClientFactory
import com.owncloud.android.BuildConfig
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.EmptyRecyclerView
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.activity.FolderPickerActivity
import com.owncloud.android.ui.activity.ToolbarActivity
import com.owncloud.android.ui.adapter.CommonOCFileListAdapterInterface
import com.owncloud.android.ui.adapter.GalleryAdapter
import com.owncloud.android.ui.adapter.GalleryRowHolder
import com.owncloud.android.ui.adapter.OCFileListDelegate
import com.owncloud.android.ui.asynctasks.GallerySearchTask
import com.owncloud.android.ui.events.ChangeMenuEvent
import com.owncloud.android.ui.fragment.GalleryFragmentBottomSheetDialog.MediaState
import javax.inject.Inject

/**
 * A Fragment that lists all files and folders in a given path
 */
class GalleryFragment : OCFileListFragment(), GalleryFragmentBottomSheetActions, GalleryRowHolder.GalleryRowItemClick {
    private var isPhotoSearchQueryRunning = false
        set(value) {
            field = value
            this.isLoading = value // link the photoSearchQueryRunning variable with UI progress loading
        }

    private var photoSearchTask: AsyncTask<Void, Void, GallerySearchTask.Result>? = null
    private var endDate: Long = 0
    private val limit = 150
    private var mAdapter: GalleryAdapter? = null
    private var galleryFragmentBottomSheetDialog: GalleryFragmentBottomSheetDialog? = null

    @JvmField
    @Inject
    var fileDataStorageManager: FileDataStorageManager? = null

    @JvmField
    @Inject
    var clientFactory: ClientFactory? = null

    private val maxColumnSizeLandscape = 5
    private val maxColumnSizePortrait = 2
    private var columnSize = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        searchFragment = true
        setHasOptionsMenu(true)

        if (galleryFragmentBottomSheetDialog == null) {
            galleryFragmentBottomSheetDialog = GalleryFragmentBottomSheetDialog(this)
        }

        columnSize = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            maxColumnSizeLandscape
        } else {
            maxColumnSizePortrait
        }

        registerRefreshSearchEventReceiver()
    }

    private fun registerRefreshSearchEventReceiver() {
        val filter = IntentFilter(REFRESH_SEARCH_EVENT_RECEIVER)
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(refreshSearchEventReceiver, filter)
    }

    private val refreshSearchEventReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (activity is FileDisplayActivity) {
                (activity as FileDisplayActivity).startPhotoSearch(R.id.nav_gallery)
            }
        }
    }

    override fun onDestroyView() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(refreshSearchEventReceiver)
        super.onDestroyView()
    }

    override fun onPause() {
        super.onPause()
        photoSearchTask?.cancel(true)
    }

    /**
     * {@inheritDoc}
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = super.onCreateView(inflater, container, savedInstanceState)
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
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
        updateSubtitle(galleryFragmentBottomSheetDialog!!.currMediaState)
        handleSearchEvent()
    }

    private lateinit var ocFileListDelegate: OCFileListDelegate

    private fun initOCFileListDelegate() {
        val storageManager: FileDataStorageManager = mContainerActivity.storageManager

        ocFileListDelegate = OCFileListDelegate(
            requireContext(),
            this,
            accountManager.user,
            storageManager,
            false,
            preferences,
            true,
            mContainerActivity,
            showMetadata = false,
            showShareAvatar = false,
            viewThemeUtils
        )
    }

    override fun setAdapter(args: Bundle) {
        initOCFileListDelegate()

        mAdapter = GalleryAdapter(
            requireContext(),
            accountManager.user,
            mContainerActivity,
            columnSize,
            ThumbnailsCacheManager.getThumbnailDimension(),
            clientFactory,
            ocFileListDelegate,
            this
        )

        setRecyclerViewAdapter(mAdapter)

        // update the footer as there is no footer shown in media view
        if (recyclerView is EmptyRecyclerView) {
            (recyclerView as EmptyRecyclerView).setHasFooter(false)
        }

        val layoutManager = GridLayoutManager(context, 1)
        mAdapter?.setLayoutManager(layoutManager)

        recyclerView.layoutManager = layoutManager
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            columnSize = maxColumnSizeLandscape
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            columnSize = maxColumnSizePortrait
        }

        mAdapter?.changeColumn(columnSize)
        showAllGalleryItems()
    }

    override fun getColumnsCount(): Int {
        return columnSize
    }

    override fun onRefresh() {
        super.onRefresh()
        handleSearchEvent()
    }

    override fun getCommonAdapter(): CommonOCFileListAdapterInterface {
        return mAdapter!!
    }

    override fun onResume() {
        super.onResume()

        isLoading = isPhotoSearchQueryRunning
        val activity = activity

        if (activity is FileDisplayActivity) {
            activity.updateActionBarTitleAndHomeButtonByString(getString(R.string.drawer_item_gallery))
            activity.setMainFabVisible(false)
        }
    }

    override fun onMessageEvent(changeMenuEvent: ChangeMenuEvent) {
        super.onMessageEvent(changeMenuEvent)
    }

    private fun handleSearchEvent() {
        prepareCurrentSearch(searchEvent)
        setEmptyListLoadingMessage()

        // always show first stored items
        showAllGalleryItems()
        setFabVisible(false)
        searchAndDisplay()
    }

    private fun searchAndDisplay() {
        if (!isPhotoSearchQueryRunning && endDate <= 0) {
            // fix an issue when the method is called after loading the gallery and pressing play on a movie (--> endDate <= 0)
            // to avoid reloading the gallery, check if endDate has already a value which is not -1 or 0 (which generally means some kind of reset/init)
            endDate = System.currentTimeMillis() / 1000
            isPhotoSearchQueryRunning = true
            runGallerySearchTask()
        }
    }

    fun searchCompleted(emptySearch: Boolean, lastTimeStamp: Long) {
        isPhotoSearchQueryRunning = false
        if (lastTimeStamp > -1) {
            endDate = lastTimeStamp
        }
        if (mAdapter?.isEmpty() == true) {
            setEmptyListMessage(SearchType.GALLERY_SEARCH)
        }
        if (!emptySearch) {
            showAllGalleryItems()
        }
        Log_OC.d(this, "End gallery search")
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.fragment_gallery_three_dots, menu)
        val menuItem = menu.findItem(R.id.action_three_dot_icon)
        menuItem?.let {
            viewThemeUtils.platform.colorMenuItemText(requireContext(), it)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        if (item.itemId == R.id.action_three_dot_icon &&
            !isPhotoSearchQueryRunning && galleryFragmentBottomSheetDialog != null
        ) {
            showBottomSheet()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun showBottomSheet() {
        if (galleryFragmentBottomSheetDialog?.isVisible == false) {
            galleryFragmentBottomSheetDialog?.show(childFragmentManager, FRAGMENT_TAG_BOTTOM_SHEET)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SELECT_LOCATION_REQUEST_CODE && data != null) {
            val chosenFolder = data.getParcelableExtra<OCFile>(FolderPickerActivity.EXTRA_FOLDER)
            chosenFolder?.let {
                preferences.lastSelectedMediaFolder = it.remotePath
                searchAndDisplayAfterChangingFolder()
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun searchAndDisplayAfterChangingFolder() {
        // TODO: Fix folder change, it seems it doesn't work at all
        endDate = System.currentTimeMillis() / 1000
        isPhotoSearchQueryRunning = true
        runGallerySearchTask()
    }

    private fun runGallerySearchTask() {
        if (mContainerActivity != null) {
            photoSearchTask = GallerySearchTask(
                this,
                accountManager.user,
                mContainerActivity.storageManager,
                endDate,
                limit
            ).execute()
        }
    }

    private fun loadMoreWhenEndReached(recyclerView: RecyclerView, dy: Int) {
        if (recyclerView.layoutManager is GridLayoutManager) {
            val gridLayoutManager = recyclerView.layoutManager as GridLayoutManager?

            // scroll down
            if (dy > 0 && !isPhotoSearchQueryRunning) {
                val totalItemCount = gridLayoutManager!!.itemCount
                val lastVisibleItem = gridLayoutManager.findLastCompletelyVisibleItemPosition()
                val visibleItemCount = gridLayoutManager.childCount
                if (lastVisibleItem == RecyclerView.NO_POSITION) {
                    return
                }
                val lastFile = mAdapter!!.getItem(lastVisibleItem - 1) ?: return
                val lastItemTimestamp = lastFile.modificationTimestamp / 1000

                // if we have already older media in the gallery then retrieve file in chronological order to fill the gap
                if (lastItemTimestamp < endDate) {
                    if (BuildConfig.DEBUG) {
                        Log_OC.d(this, "Gallery swipe: retrieve items to check the chronology")
                    }
                    isPhotoSearchQueryRunning = true
                    runGallerySearchTask()
                } else if (totalItemCount - visibleItemCount <= lastVisibleItem +
                    MAX_ITEMS_PER_ROW && // no more files in the gallery, retrieve the next ones
                    totalItemCount - visibleItemCount > 0
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
        }
    }

    override fun updateMediaContent(mediaState: MediaState) {
        showAllGalleryItems()
    }

    override fun selectMediaFolder() {
        val action = Intent(requireActivity(), FolderPickerActivity::class.java)
        action.putExtra(FolderPickerActivity.EXTRA_ACTION, FolderPickerActivity.CHOOSE_LOCATION)
        startActivityForResult(action, SELECT_LOCATION_REQUEST_CODE)
    }

    private fun showAllGalleryItems() {
        mAdapter?.showAllGalleryItems(
            preferences.lastSelectedMediaFolder,
            galleryFragmentBottomSheetDialog!!.currMediaState,
            this
        )
        updateSubtitle(galleryFragmentBottomSheetDialog!!.currMediaState)
    }

    private fun updateSubtitle(mediaState: MediaState) {
        requireActivity().runOnUiThread {
            var subTitle = requireContext().resources.getString(R.string.subtitle_photos_videos)
            if (mediaState === MediaState.MEDIA_STATE_PHOTOS_ONLY) {
                subTitle = requireContext().resources.getString(R.string.subtitle_photos_only)
            } else if (mediaState === MediaState.MEDIA_STATE_VIDEOS_ONLY) {
                subTitle = requireContext().resources.getString(R.string.subtitle_videos_only)
            }
            if (requireActivity() is ToolbarActivity) {
                (requireActivity() as ToolbarActivity).updateToolbarSubtitle(subTitle)
            }
        }
    }

    override fun setGridViewColumns(scaleFactor: Float) {
        // do nothing
    }

    companion object {
        private const val MAX_ITEMS_PER_ROW = 10
        private const val FRAGMENT_TAG_BOTTOM_SHEET = "data"
        private const val SELECT_LOCATION_REQUEST_CODE = 212
        const val REFRESH_SEARCH_EVENT_RECEIVER = "refreshSearchEventReceiver"
    }

    override fun openMedia(file: OCFile) {
        ocFileListDelegate.ocFileListFragmentInterface.onItemClicked(file)
    }
}
