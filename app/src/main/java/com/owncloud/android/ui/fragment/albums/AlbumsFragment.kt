/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.fragment.albums

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.AppBarLayout
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.client.utils.Throttler
import com.owncloud.android.R
import com.owncloud.android.databinding.AlbumsFragmentBinding
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.albums.PhotoAlbumEntry
import com.owncloud.android.lib.resources.albums.ReadAlbumsRemoteOperation
import com.owncloud.android.ui.activity.BaseActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.adapter.albums.AlbumFragmentInterface
import com.owncloud.android.ui.adapter.albums.AlbumsAdapter
import com.owncloud.android.ui.dialog.CreateAlbumDialogFragment
import com.owncloud.android.ui.fragment.FileFragment
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AlbumsFragment :
    Fragment(),
    AlbumFragmentInterface,
    Injectable {

    private var adapter: AlbumsAdapter? = null

    private lateinit var binding: AlbumsFragmentBinding

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    @Inject
    lateinit var accountManager: UserAccountManager

    @Inject
    lateinit var preferences: AppPreferences

    @Inject
    lateinit var syncedFolderProvider: SyncedFolderProvider

    @Inject
    lateinit var throttler: Throttler

    private var containerActivity: FileFragment.ContainerActivity? = null

    private var isGridView = true
    private var maxColumnSize = 2
    private var isSelectionMode = false
    private var listState: Parcelable? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            containerActivity = context as FileFragment.ContainerActivity
        } catch (e: ClassCastException) {
            throw IllegalArgumentException(
                context.toString() + " must implement " +
                    FileFragment.ContainerActivity::class.java.simpleName,
                e
            )
        }
        arguments?.let {
            isSelectionMode = it.getBoolean(ARG_IS_SELECTION_MODE, false)
            if (isSelectionMode) {
                isGridView = false
            }
        }
    }

    override fun onDetach() {
        containerActivity = null
        super.onDetach()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        maxColumnSize = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            MAX_COLUMN_SIZE_LANDSCAPE
        } else {
            MAX_COLUMN_SIZE_PORTRAIT
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = AlbumsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showAppBar()
        setupContainingList()
        setupContent()

        viewThemeUtils.material.themeFAB(binding.createAlbum)
        binding.createAlbum.setOnClickListener {
            showCreateAlbumDialog()
        }
    }

    private fun showAppBar() {
        if (requireActivity() is FileDisplayActivity) {
            val appBarLayout = requireActivity().findViewById<AppBarLayout>(R.id.appbar)
            appBarLayout?.setExpanded(true, false)
        }
    }

    private fun showCreateAlbumDialog() {
        throttler.run("onCreateAlbumClick") {
            val fragment = requireActivity().supportFragmentManager.findFragmentByTag(CreateAlbumDialogFragment.TAG)
            if (fragment == null) {
                CreateAlbumDialogFragment.newInstance()
                    .show(
                        requireActivity().supportFragmentManager,
                        CreateAlbumDialogFragment.TAG
                    )
            }
        }
    }

    private fun setupContent() {
        binding.listRoot.setHasFixedSize(true)
        if (isGridView) {
            val layoutManager = GridLayoutManager(requireContext(), maxColumnSize)
            binding.listRoot.layoutManager = layoutManager
        } else {
            val layoutManager = LinearLayoutManager(requireContext())
            binding.listRoot.layoutManager = layoutManager
        }
        fetchAndSetData()
    }

    private fun setupContainingList() {
        viewThemeUtils.androidx.themeSwipeRefreshLayout(binding.swipeContainingList)
        binding.swipeContainingList.setOnRefreshListener {
            fetchAndSetData()
        }
    }

    fun populateList(albums: List<PhotoAlbumEntry>?) {
        Log_OC.d(TAG, "loading album list item size: " + albums?.size)
        (activity as? FileDisplayActivity)?.setMainFabVisible(false)
        initializeAdapter()
        adapter?.setAlbumItems(albums)
    }

    private fun fetchAndSetData() {
        binding.swipeContainingList.isRefreshing = true
        initializeAdapter()
        updateEmptyView(false)
        readAlbums()
    }

    private fun readAlbums() {
        val activity = activity ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            if (activity is BaseActivity) {
                val client = activity.clientRepository.getOwncloudClient()
                val operation = ReadAlbumsRemoteOperation()
                val result = operation.execute(client)

                withContext(Dispatchers.Main) {
                    if (result?.isSuccess == true && result.resultData != null) {
                        if (result.resultData.isEmpty()) {
                            updateEmptyView(true)
                        }
                        populateList(result.resultData)
                    } else {
                        Log_OC.d(TAG, "read album operation failed")
                        updateEmptyView(true)
                    }

                    hideRefreshLayoutLoader()
                }
            }
        }
    }

    private fun hideRefreshLayoutLoader() {
        binding.swipeContainingList.isRefreshing = false
    }

    private fun initializeAdapter() {
        if (adapter == null) {
            adapter = AlbumsAdapter(
                requireContext(),
                containerActivity?.storageManager,
                accountManager.user,
                this,
                syncedFolderProvider,
                preferences,
                viewThemeUtils,
                isGridView
            )
        }

        binding.listRoot.adapter = adapter

        // Restore scroll state
        listState?.let {
            binding.listRoot.layoutManager?.onRestoreInstanceState(it)
        }
    }

    private fun updateEmptyView(isEmpty: Boolean) {
        binding.emptyViewLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.listRoot.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        if (isSelectionMode) {
            binding.root.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.bg_default, null))
        }

        (activity as? FileDisplayActivity)?.run {
            setupToolbar()
            supportActionBar?.let { actionBar ->
                viewThemeUtils.files.themeActionBar(
                    requireContext(),
                    actionBar,
                    R.string.drawer_item_album,
                    isMenu = true
                )
            }
            showSortListGroup(false)
            setMainFabVisible(false)

            // clear the subtitle while navigating to any other screen from Media screen
            clearToolbarSubtitle()
        }
    }

    fun navigateToAlbumItemsFragment(albumName: String, isNewAlbum: Boolean = false) {
        requireActivity().supportFragmentManager.beginTransaction().apply {
            addToBackStack(null)
            replace(
                R.id.left_fragment_container,
                AlbumItemsFragment.newInstance(albumName, isNewAlbum = isNewAlbum),
                AlbumItemsFragment.TAG
            )
            commit()
        }
    }

    fun refreshAlbums() {
        fetchAndSetData()
    }

    override fun onPause() {
        super.onPause()
        adapter?.cancelAllPendingTasks()
        listState = binding.listRoot.layoutManager?.onSaveInstanceState()
    }

    private val isGridEnabled: Boolean
        get() {
            return binding.listRoot.layoutManager is GridLayoutManager
        }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (isGridEnabled) {
            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                maxColumnSize = MAX_COLUMN_SIZE_LANDSCAPE
            } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                maxColumnSize = MAX_COLUMN_SIZE_PORTRAIT
            }
            (binding.listRoot.layoutManager as GridLayoutManager).setSpanCount(maxColumnSize)
        }
    }

    companion object {
        val TAG: String = AlbumsFragment::class.java.simpleName
        private const val ARG_IS_SELECTION_MODE = "is_selection_mode"
        const val ARG_SELECTED_ALBUM_NAME = "selected_album_name"

        private const val MAX_COLUMN_SIZE_LANDSCAPE: Int = 4
        private const val MAX_COLUMN_SIZE_PORTRAIT: Int = 2

        fun newInstance(isSelectionMode: Boolean = false): AlbumsFragment {
            val args = Bundle()
            args.putBoolean(ARG_IS_SELECTION_MODE, isSelectionMode)
            val fragment = AlbumsFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onItemClick(album: PhotoAlbumEntry) {
        if (isSelectionMode) {
            val resultIntent = Intent().apply {
                putExtra(ARG_SELECTED_ALBUM_NAME, album.albumName)
            }
            requireActivity().setResult(Activity.RESULT_OK, resultIntent)
            requireActivity().finish()
            return
        }
        navigateToAlbumItemsFragment(album.albumName)
    }
}
