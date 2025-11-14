/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro.brey@nextcloud.com>
 * SPDX-FileCopyrightText: 2018-2021 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2017 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2017 Mario Danic <mario@lovelyhq.com>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2015 María Asensio Valverde <masensio@solidgear.es>
 * SPDX-FileCopyrightText: 2014 Luke Owncloud <owncloud@ohrt.org
 * SPDX-FileCopyrightText: 2014 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-FileCopyrightText: 2012 Bartosz Przybylski <bart.p.pl@gmail.com>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.ui.fragment

import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.google.android.material.button.MaterialButton
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.client.preferences.AppPreferencesImpl
import com.nextcloud.utils.extensions.getTypedActivity
import com.nextcloud.utils.extensions.mainThread
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.databinding.ListFragmentBinding
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.SearchRemoteOperation
import com.owncloud.android.lib.resources.status.OwnCloudVersion
import com.owncloud.android.ui.EmptyRecyclerView
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.activity.FolderPickerActivity
import com.owncloud.android.ui.activity.OnEnforceableRefreshListener
import com.owncloud.android.ui.activity.UploadFilesActivity
import com.owncloud.android.ui.adapter.LocalFileListAdapter
import com.owncloud.android.ui.adapter.OCFileListAdapter
import com.owncloud.android.ui.events.SearchEvent
import com.owncloud.android.utils.theme.ViewThemeUtils
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Suppress("MagicNumber", "TooManyFunctions")
open class ExtendedListFragment :
    Fragment(),
    AdapterView.OnItemClickListener,
    OnEnforceableRefreshListener,
    SearchView.OnQueryTextListener,
    SearchView.OnCloseListener,
    Injectable {
    private var maxColumnSize = 5

    @Inject
    lateinit var preferences: AppPreferences

    @Inject
    lateinit var accountManager: UserAccountManager

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    private var mScaleGestureDetector: ScaleGestureDetector? = null

    @JvmField
    protected var mRefreshListLayout: SwipeRefreshLayout? = null

    @JvmField
    protected var mSortButton: MaterialButton? = null

    @JvmField
    protected var mSwitchGridViewButton: MaterialButton? = null

    var mEmptyListContainer: ViewGroup? = null
    private var mEmptyListMessage: TextView? = null
    private var mEmptyListHeadline: TextView? = null
    private var mEmptyListIcon: ImageView? = null

    // Save the state of the scroll in browsing
    private var mIndexes = arrayListOf<Int?>()
    private var mFirstPositions = arrayListOf<Int?>()
    private var mTops = arrayListOf<Int?>()
    private var mHeightCell = 0

    private var mOnRefreshListener: OnRefreshListener? = null

    private var mRecyclerView: EmptyRecyclerView? = null

    private var searchView: SearchView? = null
    private var closeButton: ImageView? = null

    private var mScale = AppPreferencesImpl.DEFAULT_GRID_COLUMN

    var binding: ListFragmentBinding? = null
        private set

    private var previousEmptyListData: EmptyListData? = null

    protected fun setRecyclerViewAdapter(recyclerViewAdapter: RecyclerView.Adapter<*>?) {
        mRecyclerView?.setAdapter(recyclerViewAdapter)
    }

    protected val recyclerView: RecyclerView?
        get() = mRecyclerView

    open fun setLoading(enabled: Boolean) {
        mRefreshListLayout?.isRefreshing = enabled
    }

    open fun switchToGridView() {
        if (!isGridEnabled) {
            recyclerView?.layoutManager = GridLayoutManager(context, columnsCount)
        }
    }

    open fun switchToListView() {
        if (isGridEnabled) {
            recyclerView?.layoutManager = LinearLayoutManager(context)
        }
    }

    val isGridEnabled: Boolean
        get() {
            return recyclerView?.layoutManager is GridLayoutManager
        }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        val item = menu.findItem(R.id.action_search)
        searchView = item.actionView as SearchView?
        viewThemeUtils.androidx.themeToolbarSearchView(searchView!!)
        closeButton = searchView?.findViewById(androidx.appcompat.R.id.search_close_btn)
        searchView?.setOnQueryTextListener(this)
        searchView?.setOnCloseListener(this)

        val displayMetrics = DisplayMetrics()
        activity?.let { activity ->
            activity.windowManager?.defaultDisplay?.getMetrics(displayMetrics)
            val width = displayMetrics.widthPixels
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                searchView?.setMaxWidth((width * 0.4).toInt())
            } else {
                if (activity is FolderPickerActivity) {
                    searchView?.setMaxWidth((width * 0.8).toInt())
                } else {
                    searchView?.setMaxWidth(width)
                }
            }
        }

        searchView?.setOnQueryTextFocusChangeListener { _: View?, hasFocus: Boolean ->
            Handler(Looper.getMainLooper()).post {
                val activity = activity

                if (activity == null || (activity is FolderPickerActivity) || (activity is UploadFilesActivity)) {
                    return@post
                }

                if (activity is FileDisplayActivity) {
                    val fragment = activity.leftFragment
                    if (fragment is OCFileListFragment) {
                        fragment.setFabVisible(!hasFocus)
                    }
                }

                if (searchView?.query.isNullOrEmpty()) {
                    closeButton?.visibility = View.INVISIBLE
                }
            }
        }

        // On close -> empty field, show keyboard and
        closeButton?.setOnClickListener {
            searchView?.setQuery("", true)
            searchView?.requestFocus()
            searchView?.onActionViewExpanded()

            val inputMethodManager =
                activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            inputMethodManager?.showSoftInput(searchView, InputMethodManager.SHOW_IMPLICIT)
        }

        val searchBar = searchView?.findViewById<LinearLayout>(androidx.appcompat.R.id.search_bar)
        searchBar?.setLayoutTransition(LayoutTransition())
    }

    override fun onQueryTextChange(query: String): Boolean {
        // After 300 ms, set the query

        closeButton?.visibility = View.VISIBLE
        if (query.isEmpty()) {
            closeButton?.visibility = View.INVISIBLE
        }
        return false
    }

    @Suppress("ReturnCount")
    override fun onQueryTextSubmit(query: String): Boolean {
        val adapter = recyclerView?.adapter
        if (adapter is OCFileListAdapter) {
            val listOfHiddenFiles = adapter.listOfHiddenFiles
            performSearch(query, listOfHiddenFiles, false)
            return true
        }
        if (adapter is LocalFileListAdapter) {
            performSearch(query, ArrayList(), false)
            return true
        }
        return false
    }

    fun performSearch(query: String, listOfHiddenFiles: ArrayList<String>?, isBackPressed: Boolean) {
        val adapter = recyclerView?.adapter
        val activity = activity ?: return

        when (activity) {
            is FileDisplayActivity -> {
                if (isBackPressed && query.isEmpty()) {
                    activity.resetSearchView()
                    activity.updateListOfFilesFragment(true)
                } else {
                    Handler(Looper.getMainLooper()).post {
                        if (adapter is OCFileListAdapter) {
                            if (accountManager
                                    .user
                                    .server
                                    .version
                                    .isNewerOrEqual(OwnCloudVersion.nextcloud_20)
                            ) {
                                activity.performUnifiedSearch(query, listOfHiddenFiles)
                            } else {
                                EventBus.getDefault().post(
                                    SearchEvent(query, SearchRemoteOperation.SearchType.FILE_SEARCH)
                                )
                            }
                        } else if (adapter is LocalFileListAdapter) {
                            adapter.filter(query)
                        }
                    }
                    searchView?.clearFocus()
                }
            }

            is UploadFilesActivity -> {
                val localFileListAdapter = adapter
                if (adapter is LocalFileListAdapter) {
                    localFileListAdapter.filter(query)
                }
            }

            is FolderPickerActivity -> {
                activity.search(query)
            }
        }
    }

    override fun onClose(): Boolean {
        val adapter = recyclerView?.adapter
        if (adapter is OCFileListAdapter) {
            val listOfHiddenFiles = adapter.listOfHiddenFiles
            performSearch("", listOfHiddenFiles, true)
            return false
        }
        return true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log_OC.d(TAG, "onCreateView")

        binding = ListFragmentBinding.inflate(inflater, container, false)
        val v = binding!!.getRoot()

        setupEmptyList()

        mRecyclerView = binding?.listRoot
        mRecyclerView?.setHasFooter(true)
        mRecyclerView?.setEmptyView(binding?.emptyList?.emptyListView)
        mRecyclerView?.setHasFixedSize(true)
        mRecyclerView?.layoutManager = LinearLayoutManager(context)

        mScale = preferences.getGridColumns()
        setGridViewColumns(1f)

        mScaleGestureDetector = ScaleGestureDetector(MainApp.getAppContext(), ScaleListener())

        recyclerView?.setOnTouchListener { view: View, motionEvent: MotionEvent ->
            mScaleGestureDetector?.onTouchEvent(motionEvent)
            if (motionEvent.action == MotionEvent.ACTION_UP) {
                view.performClick()
            }
            false
        }

        mRefreshListLayout = binding?.swipeContainingList
        mRefreshListLayout?.let {
            viewThemeUtils.androidx.themeSwipeRefreshLayout(it)
            it.setOnRefreshListener(this)
        }

        mSortButton = requireActivity().findViewById(R.id.sort_button)
        mSortButton?.let {
            viewThemeUtils.material.colorMaterialTextButton(it)
        }

        mSwitchGridViewButton = requireActivity().findViewById(R.id.switch_grid_view_button)
        mSwitchGridViewButton?.let {
            viewThemeUtils.material.colorMaterialTextButton(it)
        }

        return v
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private inner class ScaleListener : SimpleOnScaleGestureListener() {
        @SuppressLint("NotifyDataSetChanged")
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            setGridViewColumns(detector.getScaleFactor())

            preferences.setGridColumns(mScale)

            recyclerView?.adapter?.notifyDataSetChanged()

            return true
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    protected open fun setGridViewColumns(scaleFactor: Float) {
        val gridLayoutManager = mRecyclerView?.layoutManager
        if (gridLayoutManager is GridLayoutManager) {
            if (mScale == -1f) {
                gridLayoutManager.setSpanCount(GridView.AUTO_FIT)
                mScale = gridLayoutManager.spanCount.toFloat()
            }
            mScale *= 2f - scaleFactor
            mScale = max(MIN_COLUMN_SIZE, min(mScale, maxColumnSize.toFloat()))
            val scaleInt = mScale.roundToInt()
            gridLayoutManager.setSpanCount(scaleInt)
            mRecyclerView?.adapter?.notifyDataSetChanged()
        }
    }

    protected fun setupEmptyList() {
        binding?.emptyList?.run {
            mEmptyListContainer = emptyListView
            mEmptyListMessage = emptyListViewText
            mEmptyListHeadline = emptyListViewHeadline
            mEmptyListIcon = emptyListIcon
        }
    }

    private fun getEmptyListViewTextId(): Int {
        val currentDir = getTypedActivity(FileDisplayActivity::class.java)?.currentDir
        return if (currentDir?.canCreateFileAndFolder() == false) {
            R.string.extented_list_fragment_empty_list_message_no_write_permission
        } else {
            R.string.file_list_empty
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) {
            return
        }

        savedInstanceState.getIntegerArrayList(KEY_INDEXES)?.let { mIndexes = it }
        savedInstanceState.getIntegerArrayList(KEY_FIRST_POSITIONS)?.let { mFirstPositions = it }
        savedInstanceState.getIntegerArrayList(KEY_TOPS)?.let { mTops = it }
        mHeightCell = savedInstanceState.getInt(KEY_HEIGHT_CELL)
        setMessageForEmptyList(savedInstanceState.getString(KEY_EMPTY_LIST_MESSAGE))

        if (savedInstanceState.getBoolean(KEY_IS_GRID_VISIBLE, false) && recyclerView?.adapter != null) {
            switchToGridView()
        }

        val referencePosition = savedInstanceState.getInt(KEY_SAVED_LIST_POSITION)
        Log_OC.v(TAG, "Setting grid position $referencePosition")
        scrollToPosition(referencePosition)
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        Log_OC.d(TAG, "onSaveInstanceState()")

        savedInstanceState.run {
            putBoolean(KEY_IS_GRID_VISIBLE, isGridEnabled)
            putIntegerArrayList(KEY_INDEXES, mIndexes)
            putIntegerArrayList(KEY_FIRST_POSITIONS, mFirstPositions)
            putIntegerArrayList(KEY_TOPS, mTops)
            putInt(KEY_HEIGHT_CELL, mHeightCell)
            putString(KEY_EMPTY_LIST_MESSAGE, emptyViewText)
        }

        preferences.setGridColumns(mScale)
    }

    open val columnsCount: Int
        get() {
            if (mScale == -1f) {
                return AppPreferencesImpl.DEFAULT_GRID_COLUMN.roundToInt()
            }
            return mScale.roundToInt()
        }

    /*
     * Restore index and position
     */
    protected fun restoreIndexAndTopPosition() {
        if (mIndexes.isEmpty()) {
            Log_OC.d(TAG, "Indexes is null or empty")
            return
        }

        // needs to be checked; not every browse-up had a browse-down before
        val index = mIndexes.removeAt(mIndexes.size - 1)
        val firstPosition = mFirstPositions.removeAt(mFirstPositions.size - 1)
        val top = mTops.removeAt(mTops.size - 1)

        Log_OC.v(
            TAG,
            (
                "Setting selection to position: " + firstPosition + "; top: " +
                    top + "; index: " + index
                )
        )

        firstPosition?.let { scrollToPosition(it) }
    }

    private fun scrollToPosition(position: Int) {
        val layoutManager = mRecyclerView?.layoutManager

        if (layoutManager is LinearLayoutManager) {
            val visibleItemCount = layoutManager.findLastCompletelyVisibleItemPosition() -
                layoutManager.findFirstCompletelyVisibleItemPosition()
            layoutManager.scrollToPositionWithOffset(position, (visibleItemCount / 2) * mHeightCell)
        }
    }

    /*
     * Save index and top position
     */
    protected fun saveIndexAndTopPosition(index: Int) {
        mIndexes.add(index)

        val layoutManager = mRecyclerView?.layoutManager
        val firstPosition: Int = if (layoutManager is GridLayoutManager) {
            layoutManager.findFirstCompletelyVisibleItemPosition()
        } else {
            (layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
        }

        mFirstPositions.add(firstPosition)

        val view = mRecyclerView?.getChildAt(0)
        val top = view?.top ?: 0

        mTops.add(top)

        // Save the height of a cell
        mHeightCell = if (view == null || mHeightCell != 0) mHeightCell else view.height
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        // to be @overridden
    }

    override fun onRefresh() {
        if (searchView != null) {
            searchView?.onActionViewCollapsed()

            val activity = activity
            if (activity is FileDisplayActivity) {
                activity.setDrawerIndicatorEnabled(activity.isDrawerIndicatorAvailable)
                activity.hideSearchView(activity.getCurrentDir())
            }
        }
        mOnRefreshListener?.onRefresh()
    }

    fun setOnRefreshListener(listener: OnEnforceableRefreshListener?) {
        mOnRefreshListener = listener
    }

    /**
     * Disables swipe gesture.
     *
     *
     * Sets the 'enabled' state of the refresh layouts contained in the fragment.
     *
     *
     * When 'false' is set, prevents user gestures but keeps the option to refresh programmatically,
     *
     * @param enabled Desired state for capturing swipe gesture.
     */
    fun setSwipeEnabled(enabled: Boolean) {
        mRefreshListLayout?.setEnabled(enabled)
    }

    /**
     * / ** Set message for empty list view.
     */
    fun setMessageForEmptyList(message: String?) {
        if (mEmptyListContainer != null && mEmptyListMessage != null) {
            mEmptyListMessage?.text = message
        }
    }

    /**
     * displays an empty list information with a headline, a message and an icon.
     *
     * @param headline the headline
     * @param message  the message
     * @param icon     the icon to be shown
     * @param tintIcon flag if the given icon should be tinted with primary color
     */
    private fun setMessageForEmptyList(
        @StringRes headline: Int,
        @StringRes message: Int,
        @DrawableRes icon: Int?,
        tintIcon: Boolean = false
    ) {
        if (mEmptyListContainer == null || mEmptyListMessage == null) {
            return
        }

        val newData = EmptyListData(headline, message, icon, tintIcon)
        if (previousEmptyListData == newData) {
            Log_OC.d(TAG, "Skipping setMessageForEmptyList – same data as before")
            return
        }
        previousEmptyListData = newData
        Log_OC.d(TAG, "Updating setMessageForEmptyList")

        Handler(Looper.getMainLooper()).post {
            mEmptyListHeadline?.setText(headline)
            mEmptyListMessage?.setText(message)

            if (icon != null) {
                if (tintIcon) {
                    context?.let { context ->
                        val drawable = viewThemeUtils.platform.tintDrawable(context, icon, ColorRole.PRIMARY)
                        mEmptyListIcon?.setImageDrawable(drawable)
                    }
                } else {
                    mEmptyListIcon?.setImageResource(icon)
                }

                mEmptyListIcon?.visibility = View.VISIBLE
            } else {
                mEmptyListIcon?.visibility = View.GONE
            }

            mEmptyListMessage?.visibility = View.VISIBLE
        }
    }

    @Suppress("LongMethod")
    fun setEmptyListMessage(state: Parcelable?) {
        when (state) {
            SearchType.NO_SEARCH -> {
                setMessageForEmptyList(
                    R.string.file_list_empty_headline,
                    getEmptyListViewTextId(),
                    R.drawable.ic_list_empty_folder,
                    true
                )
            }
            SearchType.FILE_SEARCH -> {
                setMessageForEmptyList(
                    R.string.file_list_empty_headline_server_search,
                    R.string.file_list_empty,
                    R.drawable.ic_search_light_grey
                )
            }
            SearchType.FAVORITE_SEARCH -> {
                setMessageForEmptyList(
                    R.string.file_list_empty_favorite_headline,
                    R.string.file_list_empty_favorites_filter_list,
                    R.drawable.ic_star_light_yellow
                )
            }
            SearchType.RECENTLY_MODIFIED_SEARCH -> {
                setMessageForEmptyList(
                    R.string.file_list_empty_headline_server_search,
                    R.string.file_list_empty_recently_modified,
                    R.drawable.ic_list_empty_recent
                )
            }
            SearchType.REGULAR_FILTER -> {
                setMessageForEmptyList(
                    R.string.file_list_empty_headline_search,
                    R.string.file_list_empty_search,
                    R.drawable.ic_search_light_grey
                )
            }
            SearchType.SHARED_FILTER -> {
                setMessageForEmptyList(
                    R.string.file_list_empty_shared_headline,
                    R.string.file_list_empty_shared,
                    R.drawable.ic_list_empty_shared
                )
            }
            SearchType.GALLERY_SEARCH -> {
                setMessageForEmptyList(
                    R.string.file_list_empty_headline_server_search,
                    R.string.file_list_empty_gallery,
                    R.drawable.file_image
                )
            }
            SearchType.LOCAL_SEARCH -> {
                setMessageForEmptyList(
                    R.string.file_list_empty_headline_server_search,
                    R.string.file_list_empty_local_search,
                    R.drawable.ic_search_light_grey
                )
            }
            EmptyListState.OFFLINE_MODE -> {
                setMessageForEmptyList(
                    R.string.offline_mode_info_title,
                    R.string.offline_mode_info_description,
                    R.drawable.ic_cloud_sync,
                    true
                )
            }
            EmptyListState.LOADING -> {
                setMessageForEmptyList(
                    R.string.file_list_loading,
                    R.string.empty,
                    null
                )
            }
            EmptyListState.ADD_FOLDER -> {
                setMessageForEmptyList(
                    R.string.folder_list_empty_headline,
                    R.string.file_list_empty_moving,
                    R.drawable.ic_list_empty_create_folder,
                    true
                )
            }
            EmptyListState.ONLY_ON_DEVICE -> {
                setMessageForEmptyList(
                    R.string.file_list_empty_headline,
                    R.string.file_list_empty_on_device,
                    R.drawable.ic_list_empty_folder,
                    true
                )
            }
            EmptyListState.LOCAL_FILE_LIST_EMPTY_FILE -> {
                setMessageForEmptyList(
                    R.string.file_list_empty_headline,
                    R.string.local_file_list_empty,
                    R.drawable.ic_list_empty_folder,
                    true
                )
            }
            EmptyListState.LOCAL_FILE_LIST_EMPTY_FOLDER -> {
                setMessageForEmptyList(
                    R.string.folder_list_empty_headline,
                    R.string.local_folder_list_empty,
                    R.drawable.ic_list_empty_folder,
                    true
                )
            }
            EmptyListState.ERROR -> {
                setMessageForEmptyList(
                    R.string.file_list_error_headline,
                    R.string.file_list_error_description,
                    R.drawable.ic_no_internet,
                    false
                )
            }
            else -> {
                setMessageForEmptyList(
                    R.string.file_list_empty_headline,
                    getEmptyListViewTextId(),
                    R.drawable.ic_list_empty_folder,
                    true
                )
            }
        }.also {
            mainThread {
                mRefreshListLayout?.isRefreshing = false
            }
        }
    }

    val emptyViewText: String
        /**
         * Get the text of EmptyListMessage TextView.
         *
         * @return String empty text view text-value
         */
        get() = if (mEmptyListContainer != null && mEmptyListMessage != null) {
            mEmptyListMessage?.getText()
                .toString()
        } else {
            ""
        }

    override fun onRefresh(ignoreETag: Boolean) {
        mOnRefreshListener?.let { listener ->
            if (listener is FileDisplayActivity) {
                listener.onRefresh(ignoreETag)
            } else {
                listener.onRefresh()
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            maxColumnSize = 10
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            maxColumnSize = 5
        }

        if (isGridEnabled && columnsCount > maxColumnSize) {
            (recyclerView?.layoutManager as GridLayoutManager).spanCount = maxColumnSize
        }
    }

    protected fun setGridSwitchButton() {
        mSwitchGridViewButton?.let {
            if (isGridEnabled) {
                it.setContentDescription(getString(R.string.action_switch_list_view))
                it.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_view_list)
            } else {
                it.setContentDescription(getString(R.string.action_switch_grid_view))
                it.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_view_module)
            }
        }
    }

    protected fun isAccountManagerInitialized(): Boolean = ::accountManager.isInitialized

    private data class EmptyListData(val headline: Int, val message: Int, val icon: Int?, val tintIcon: Boolean)

    companion object {
        protected val TAG: String = ExtendedListFragment::class.java.getSimpleName()

        protected const val KEY_SAVED_LIST_POSITION: String = "SAVED_LIST_POSITION"

        private const val KEY_INDEXES = "INDEXES"
        private const val KEY_FIRST_POSITIONS = "FIRST_POSITIONS"
        private const val KEY_TOPS = "TOPS"
        private const val KEY_HEIGHT_CELL = "HEIGHT_CELL"
        private const val KEY_EMPTY_LIST_MESSAGE = "EMPTY_LIST_MESSAGE"
        private const val KEY_IS_GRID_VISIBLE = "IS_GRID_VISIBLE"
        private const val MIN_COLUMN_SIZE: Float = 2.0f
    }
}
