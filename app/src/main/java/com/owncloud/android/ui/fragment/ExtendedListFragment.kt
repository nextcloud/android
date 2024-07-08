/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
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
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
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
import androidx.core.view.MenuItemCompat
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

@Suppress("MagicNumber", "TooManyFunctions")
open class ExtendedListFragment :
    Fragment(),
    AdapterView.OnItemClickListener,
    OnEnforceableRefreshListener,
    SearchView.OnQueryTextListener,
    SearchView.OnCloseListener,
    Injectable {
    private var maxColumnSize = 5
    private val maxColumnSizePortrait = 5
    private val maxColumnSizeLandscape = 10

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

    protected var mEmptyListContainer: ViewGroup? = null
    private var mEmptyListMessage: TextView? = null
    private var mEmptyListHeadline: TextView? = null
    private var mEmptyListIcon: ImageView? = null

    // Save the state of the scroll in browsing
    private var mIndexes = ArrayList<Int>()

    private var mFirstPositions = ArrayList<Int>()

    private var mTops = ArrayList<Int>()

    private var mHeightCell = 0

    private var mOnRefreshListener: OnRefreshListener? = null

    private var mRecyclerView: EmptyRecyclerView? = null

    protected var searchView: SearchView? = null
    private var closeButton: ImageView? = null
    private val handler = Handler(Looper.getMainLooper())

    private var mScale = AppPreferencesImpl.DEFAULT_GRID_COLUMN

    private lateinit var binding: ListFragmentBinding

    protected fun setRecyclerViewAdapter(recyclerViewAdapter: RecyclerView.Adapter<*>?) {
        mRecyclerView?.adapter = recyclerViewAdapter
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
        get() = if (recyclerView != null) {
            recyclerView?.layoutManager is GridLayoutManager
        } else {
            false
        }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        val item = menu.findItem(R.id.action_search)
        searchView = MenuItemCompat.getActionView(item) as SearchView
        searchView?.let {
            viewThemeUtils.androidx.themeToolbarSearchView(it)
        }

        closeButton = searchView?.findViewById(androidx.appcompat.R.id.search_close_btn)
        searchView?.setOnQueryTextListener(this)
        searchView?.setOnCloseListener(this)

        val displayMetrics = DisplayMetrics()
        if (activity != null) {
            requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
            val width = displayMetrics.widthPixels
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                searchView?.maxWidth = (width * 0.4).toInt()
            } else {
                if (activity is FolderPickerActivity) {
                    searchView?.maxWidth = (width * 0.8).toInt()
                } else {
                    searchView?.maxWidth = width
                }
            }
        }

        searchView?.setOnQueryTextFocusChangeListener { _: View?, hasFocus: Boolean ->
            Handler(Looper.getMainLooper()).post {
                if (activity != null && activity !is FolderPickerActivity &&
                    activity !is UploadFilesActivity
                ) {
                    if (activity is FileDisplayActivity) {
                        val fragment =
                            (activity as FileDisplayActivity?)?.leftFragment
                        if (fragment is OCFileListFragment) {
                            fragment.setFabVisible(!hasFocus)
                        }
                    }
                    if (TextUtils.isEmpty(searchView?.query)) {
                        closeButton?.setVisibility(View.INVISIBLE)
                    }
                }
            }
        }

        // On close -> empty field, show keyboard and
        closeButton?.setOnClickListener {
            searchView?.setQuery("", true)
            searchView?.requestFocus()
            searchView?.onActionViewExpanded()

            val inputMethodManager =
                requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.showSoftInput(searchView, InputMethodManager.SHOW_IMPLICIT)
        }

        val searchBar = searchView?.findViewById<LinearLayout>(R.id.search_bar)
        searchBar?.layoutTransition = LayoutTransition()
    }

    override fun onQueryTextChange(query: String): Boolean {
        // After 300 ms, set the query
        closeButton?.visibility = View.VISIBLE
        if (query.isEmpty()) {
            closeButton?.visibility = View.INVISIBLE
        }
        return false
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        val adapter = recyclerView?.adapter
        if (adapter is OCFileListAdapter) {
            val listOfHiddenFiles = adapter.listOfHiddenFiles
            performSearch(query, listOfHiddenFiles, false)
            return true
        }
        return false
    }

    fun performSearch(query: String?, listOfHiddenFiles: ArrayList<String?>?, isBackPressed: Boolean) {
        handler.removeCallbacksAndMessages(null)
        val adapter = recyclerView?.adapter
        if (activity == null) {
            return
        }

        if (activity is FileDisplayActivity) {
            val activity = activity as FileDisplayActivity
            if (isBackPressed && TextUtils.isEmpty(query)) {
                activity.resetSearchView()
                activity.updateListOfFilesFragment(true)
            } else {
                handler.post {
                    if (adapter is OCFileListAdapter) {
                        if (accountManager
                                .user
                                .server
                                .version
                                .isNewerOrEqual(OwnCloudVersion.nextcloud_20)
                        ) {
                            activity.performUnifiedSearch(query, listOfHiddenFiles)
                        } else {
                            query?.let {
                                EventBus.getDefault().post(
                                    SearchEvent(query, SearchRemoteOperation.SearchType.FILE_SEARCH)
                                )
                            }
                        }
                    } else if (adapter is LocalFileListAdapter) {
                        adapter.filter(query)
                    }
                }

                if (searchView != null) {
                    searchView?.clearFocus()
                }
            }
        } else if (activity is UploadFilesActivity) {
            val activity = activity as UploadFilesActivity
            val localFileListAdapter = adapter as LocalFileListAdapter?
            localFileListAdapter?.filter(query)
            activity.showToolbarSpinner()
        } else if (activity is FolderPickerActivity) {
            val activity = activity as FolderPickerActivity
            activity.search(query)
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
        setupEmptyList()

        mRecyclerView = binding.listRoot
        mRecyclerView?.setHasFooter(true)
        mRecyclerView?.setEmptyView(binding.emptyList.emptyListView)
        mRecyclerView?.setHasFixedSize(true)
        mRecyclerView?.layoutManager = LinearLayoutManager(context)

        mScale = preferences.gridColumns
        setGridViewColumns(1f)

        mScaleGestureDetector = ScaleGestureDetector(MainApp.getAppContext(), ScaleListener())

        recyclerView?.setOnTouchListener { view: View, motionEvent: MotionEvent ->
            mScaleGestureDetector?.onTouchEvent(motionEvent)
            if (motionEvent.action == MotionEvent.ACTION_UP) {
                view.performClick()
            }
            false
        }

        // Pull-down to refresh layout
        mRefreshListLayout = binding.swipeContainingList

        mRefreshListLayout?.let {
            viewThemeUtils.androidx.themeSwipeRefreshLayout(it)
        }

        mRefreshListLayout?.setOnRefreshListener(this)

        mSortButton = requireActivity().findViewById(R.id.sort_button)
        mSortButton?.let {
            viewThemeUtils.material.colorMaterialTextButton(it)
        }

        mSwitchGridViewButton = requireActivity().findViewById(R.id.switch_grid_view_button)
        mSwitchGridViewButton?.let {
            viewThemeUtils.material.colorMaterialTextButton(it)
        }

        return binding.root
    }

    private inner class ScaleListener : SimpleOnScaleGestureListener() {
        @SuppressLint("NotifyDataSetChanged")
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            setGridViewColumns(detector.scaleFactor)

            preferences.gridColumns = mScale

            recyclerView?.adapter?.notifyDataSetChanged()

            return true
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    protected open fun setGridViewColumns(scaleFactor: Float) {
        if (mRecyclerView?.layoutManager is GridLayoutManager) {
            val gridLayoutManager = mRecyclerView?.layoutManager as GridLayoutManager?
            if (mScale == -1f) {
                gridLayoutManager?.spanCount = GridView.AUTO_FIT
                mScale = gridLayoutManager?.spanCount?.toFloat() ?: 1f
            }
            mScale *= 1f - (scaleFactor - 1f)
            mScale = max(
                MIN_COLUMN_SIZE.toDouble(),
                min(mScale.toDouble(), maxColumnSize.toDouble())
            ).toFloat()
            val scaleInt = Math.round(mScale)
            gridLayoutManager?.spanCount = scaleInt
            mRecyclerView?.adapter?.notifyDataSetChanged()
        }
    }

    private fun setupEmptyList() {
        mEmptyListContainer = binding.emptyList.emptyListView
        mEmptyListMessage = binding.emptyList.emptyListViewText
        mEmptyListHeadline = binding.emptyList.emptyListViewHeadline
        mEmptyListIcon = binding.emptyList.emptyListIcon
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState == null) {
            return
        }

        mIndexes = savedInstanceState.getIntegerArrayList(KEY_INDEXES) ?: ArrayList()
        mFirstPositions = savedInstanceState.getIntegerArrayList(KEY_FIRST_POSITIONS) ?: ArrayList()
        mTops = savedInstanceState.getIntegerArrayList(KEY_TOPS) ?: ArrayList()
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

        preferences.gridColumns = mScale
    }

    open val columnsCount: Int
        get() {
            if (mScale == -1f) {
                return Math.round(AppPreferencesImpl.DEFAULT_GRID_COLUMN)
            }
            return Math.round(mScale)
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
            "Setting selection to position: " + firstPosition + "; top: " +
                top + "; index: " + index
        )

        scrollToPosition(firstPosition)
    }

    private fun scrollToPosition(position: Int) {
        val layoutManager = mRecyclerView?.layoutManager as LinearLayoutManager? ?: return

        val visibleItemCount = layoutManager.findLastCompletelyVisibleItemPosition() -
            layoutManager.findFirstCompletelyVisibleItemPosition()
        layoutManager.scrollToPositionWithOffset(position, (visibleItemCount / 2) * mHeightCell)
    }

    /*
     * Save index and top position
     */
    protected fun saveIndexAndTopPosition(index: Int) {
        mIndexes.add(index)

        val layoutManager = mRecyclerView?.layoutManager
        val firstPosition = if (layoutManager is GridLayoutManager) {
            layoutManager.findFirstCompletelyVisibleItemPosition()
        } else {
            (layoutManager as LinearLayoutManager?)?.findFirstCompletelyVisibleItemPosition()
        }

        firstPosition?.let {
            mFirstPositions.add(firstPosition)
        }

        val view = mRecyclerView?.getChildAt(0)
        val top = if ((view == null)) 0 else view.top

        mTops.add(top)

        // Save the height of a cell
        mHeightCell = if ((view == null || mHeightCell != 0)) mHeightCell else view.height
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
        // to be @overridden
    }

    override fun onRefresh() {
        searchView?.let { searchView ->
            searchView.onActionViewCollapsed()

            var activity: Activity?
            if ((getActivity().also { activity = it }) != null && activity is FileDisplayActivity) {
                val fileDisplayActivity = activity as FileDisplayActivity
                fileDisplayActivity.setDrawerIndicatorEnabled(fileDisplayActivity.isDrawerIndicatorAvailable)
                fileDisplayActivity.hideSearchView(fileDisplayActivity.currentDir)
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
        mRefreshListLayout?.isEnabled = enabled
    }

    /**
     * / ** Set message for empty list view.
     */
    private fun setMessageForEmptyList(message: String?) {
        if (mEmptyListContainer != null) {
            mEmptyListMessage?.text = message
        }
    }

    /**
     * displays an empty list information with a headline, a message and a not to be tinted icon.
     *
     * @param headline the headline
     * @param message  the message
     * @param icon     the icon to be shown
     */
    private fun setMessageForEmptyList(@StringRes headline: Int, @StringRes message: Int, @DrawableRes icon: Int) {
        setMessageForEmptyList(headline, message, icon, false)
    }

    /**
     * displays an empty list information with a headline, a message and an icon.
     *
     * @param headline the headline
     * @param message  the message
     * @param icon     the icon to be shown
     * @param tintIcon flag if the given icon should be tinted with primary color
     */
    fun setMessageForEmptyList(
        @StringRes headline: Int,
        @StringRes message: Int,
        @DrawableRes icon: Int,
        tintIcon: Boolean
    ) {
        Handler(Looper.getMainLooper()).post {
            if (mEmptyListContainer == null) {
                return@post
            }

            mEmptyListHeadline?.setText(headline)
            mEmptyListMessage?.setText(message)

            if (tintIcon && context != null) {
                mEmptyListIcon?.setImageDrawable(
                    viewThemeUtils.platform.tintDrawable(requireContext(), icon, ColorRole.PRIMARY)
                )
            } else {
                mEmptyListIcon?.setImageResource(icon)
            }

            mEmptyListIcon?.visibility = View.VISIBLE
            mEmptyListMessage?.visibility = View.VISIBLE
        }
    }

    fun setEmptyListMessage(searchType: SearchType) {
        Handler(Looper.getMainLooper()).post {
            when (searchType) {
                SearchType.NO_SEARCH -> {
                    setMessageForEmptyList(
                        R.string.file_list_empty_headline,
                        R.string.file_list_empty,
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

                SearchType.GROUPFOLDER -> {
                }
            }
        }
    }

    /**
     * Set message for empty list view.
     */
    @SuppressLint("SetTextI18n")
    fun setEmptyListLoadingMessage() {
        Handler(Looper.getMainLooper()).post {
            if (mEmptyListContainer != null) {
                mEmptyListHeadline?.setText(R.string.file_list_loading)
                mEmptyListMessage?.text = ""

                mEmptyListIcon?.visibility = View.GONE
            }
        }
    }

    private val emptyViewText: String
        get() = if ((mEmptyListContainer != null && mEmptyListMessage != null)) {
            mEmptyListMessage?.text.toString()
        } else {
            ""
        }

    override fun onRefresh(ignoreETag: Boolean) {
        if (mOnRefreshListener == null) {
            return
        }

        if (mOnRefreshListener is FileDisplayActivity) {
            (mOnRefreshListener as FileDisplayActivity).onRefresh(ignoreETag)
        } else {
            mOnRefreshListener?.onRefresh()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            maxColumnSize = maxColumnSizeLandscape
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            maxColumnSize = maxColumnSizePortrait
        }

        if (isGridEnabled && columnsCount > maxColumnSize) {
            (recyclerView?.layoutManager as GridLayoutManager?)?.spanCount = maxColumnSize
        }
    }

    protected fun setGridSwitchButton() {
        mSwitchGridViewButton?.run {
            contentDescription = getString(
                if (isGridEnabled) R.string.action_switch_list_view else R.string.action_switch_grid_view
            )
            icon = ContextCompat.getDrawable(
                requireContext(),
                if (isGridEnabled) R.drawable.ic_view_list else R.drawable.ic_view_module
            )
        }
    }

    companion object {
        protected val TAG: String = ExtendedListFragment::class.java.simpleName

        protected const val KEY_SAVED_LIST_POSITION: String = "SAVED_LIST_POSITION"

        private const val KEY_INDEXES = "INDEXES"
        private const val KEY_FIRST_POSITIONS = "FIRST_POSITIONS"
        private const val KEY_TOPS = "TOPS"
        private const val KEY_HEIGHT_CELL = "HEIGHT_CELL"
        private const val KEY_EMPTY_LIST_MESSAGE = "EMPTY_LIST_MESSAGE"
        private const val KEY_IS_GRID_VISIBLE = "IS_GRID_VISIBLE"
        const val MIN_COLUMN_SIZE: Float = 2.0f
    }
}
