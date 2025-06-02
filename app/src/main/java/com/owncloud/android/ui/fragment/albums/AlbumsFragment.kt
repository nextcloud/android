/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.fragment.albums

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.client.network.ClientFactory.CreationException
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.R
import com.owncloud.android.databinding.AlbumsFragmentBinding
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.albums.PhotoAlbumEntry
import com.owncloud.android.lib.resources.albums.ReadAlbumsRemoteOperation
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.adapter.albums.AlbumFragmentInterface
import com.owncloud.android.ui.adapter.albums.AlbumsAdapter
import com.owncloud.android.ui.dialog.CreateAlbumDialogFragment
import com.owncloud.android.ui.fragment.FileFragment
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Optional
import javax.inject.Inject

class AlbumsFragment : Fragment(), AlbumFragmentInterface, Injectable {

    private var adapter: AlbumsAdapter? = null
    private var client: OwnCloudClient? = null
    private var optionalUser: Optional<User>? = null

    private lateinit var binding: AlbumsFragmentBinding

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

    private var mContainerActivity: FileFragment.ContainerActivity? = null

    private var isGridView = true
    private var maxColumnSize = 2
    private var isSelectionMode = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            mContainerActivity = context as FileFragment.ContainerActivity
        } catch (e: ClassCastException) {
            throw IllegalArgumentException(
                context.toString() + " must implement " +
                    FileFragment.ContainerActivity::class.java.simpleName, e
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
        mContainerActivity = null
        super.onDetach()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        maxColumnSize = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            4
        } else {
            2
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = AlbumsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        optionalUser = Optional.of(accountManager.user)
        createMenu()
        setupContainingList()
        setupContent()
        binding.createAlbum.setOnClickListener {
            showCreateAlbumDialog()
        }
    }

    private fun createMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.clear() // important: clears any existing activity menu
                menuInflater.inflate(R.menu.fragment_create_album, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_create_new_album -> {
                        showCreateAlbumDialog()
                        true
                    }

                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun showCreateAlbumDialog() {
        CreateAlbumDialogFragment.newInstance()
            .show(
                requireActivity().supportFragmentManager,
                CreateAlbumDialogFragment.TAG
            )
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

    @VisibleForTesting
    fun populateList(albums: List<PhotoAlbumEntry>?) {
        if (requireActivity() is FileDisplayActivity) {
            (requireActivity() as FileDisplayActivity).setMainFabVisible(false)
        }
        initializeAdapter()
        adapter?.setAlbumItems(albums)
    }

    private fun fetchAndSetData() {
        binding.swipeContainingList.isRefreshing = true
        initializeAdapter()
        updateEmptyView(false)
        lifecycleScope.launch(Dispatchers.IO) {
            val getRemoteNotificationOperation = ReadAlbumsRemoteOperation()
            val result = client?.let { getRemoteNotificationOperation.execute(it) }
            withContext(Dispatchers.Main) {
                if (result?.isSuccess == true && result.resultData != null) {
                    if (result.resultData.isEmpty()) {
                        updateEmptyView(true)
                    }
                    populateList(result.resultData)
                } else {
                    Log_OC.d(TAG, result?.logMessage)
                    // show error
                    updateEmptyView(true)
                }
                hideRefreshLayoutLoader()
            }
        }
    }

    private fun hideRefreshLayoutLoader() {
        binding.swipeContainingList.isRefreshing = false
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
            adapter = AlbumsAdapter(
                requireContext(),
                mContainerActivity?.storageManager,
                accountManager.user,
                this,
                syncedFolderProvider,
                preferences,
                viewThemeUtils,
                isGridView
            )
        }
        binding.listRoot.adapter = adapter
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
        if (requireActivity() is FileDisplayActivity) {
            (requireActivity() as FileDisplayActivity).setupToolbar()
            (requireActivity() as FileDisplayActivity).supportActionBar?.let { actionBar ->
                viewThemeUtils.files.themeActionBar(
                    requireContext(),
                    actionBar,
                    R.string.drawer_item_album,
                    isMenu = true
                )
            }
            (requireActivity() as FileDisplayActivity).showSortListGroup(false)
            (requireActivity() as FileDisplayActivity).setMainFabVisible(false)

            // clear the subtitle while navigating to any other screen from Media screen
            (requireActivity() as FileDisplayActivity).clearToolbarSubtitle()
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
    }

    private val isGridEnabled: Boolean
        get() {
            return binding.listRoot.layoutManager is GridLayoutManager
        }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (isGridEnabled) {
            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                maxColumnSize = 4
            } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                maxColumnSize = 2
            }
            (binding.listRoot.layoutManager as GridLayoutManager).setSpanCount(maxColumnSize)
        }
    }

    companion object {
        val TAG: String = AlbumsFragment::class.java.simpleName
        private const val ARG_IS_SELECTION_MODE = "is_selection_mode"
        const val ARG_SELECTED_ALBUM_NAME = "selected_album_name"

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