/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.VisibleForTesting
import androidx.appcompat.widget.SearchView
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.nextcloud.client.account.CurrentAccountProvider
import com.nextcloud.client.core.AsyncRunner
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.di.ViewModelFactory
import com.nextcloud.client.network.ClientFactory
import com.owncloud.android.R
import com.owncloud.android.databinding.ListFragmentBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.SearchResultEntry
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.adapter.UnifiedSearchItemViewHolder
import com.owncloud.android.ui.adapter.UnifiedSearchListAdapter
import com.owncloud.android.ui.fragment.util.PairMediatorLiveData
import com.owncloud.android.ui.interfaces.UnifiedSearchListInterface
import com.owncloud.android.ui.unifiedsearch.IUnifiedSearchViewModel
import com.owncloud.android.ui.unifiedsearch.ProviderID
import com.owncloud.android.ui.unifiedsearch.UnifiedSearchSection
import com.owncloud.android.ui.unifiedsearch.UnifiedSearchViewModel
import com.owncloud.android.ui.unifiedsearch.filterOutHiddenFiles
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

/**
 * Starts query to all capable unified search providers and displays them Opens result in our app, redirect to other
 * apps, if installed, or opens browser
 */
class UnifiedSearchFragment :
    Fragment(),
    Injectable,
    UnifiedSearchListInterface,
    SearchView.OnQueryTextListener,
    UnifiedSearchItemViewHolder.FilesAction {
    private lateinit var adapter: UnifiedSearchListAdapter
    private var _binding: ListFragmentBinding? = null
    val binding get() = _binding!!
    private var searchView: SearchView? = null
    lateinit var vm: IUnifiedSearchViewModel

    companion object {
        private const val TAG = "UnifiedSearchFragment"

        const val ARG_QUERY = "ARG_QUERY"
        const val ARG_HIDDEN_FILES = "ARG_HIDDEN_FILES"

        fun newInstance(query: String?, listOfHiddenFiles: ArrayList<String>?): UnifiedSearchFragment {
            val fragment = UnifiedSearchFragment()
            val args = Bundle()
            args.putString(ARG_QUERY, query)
            args.putStringArrayList(ARG_HIDDEN_FILES, listOfHiddenFiles)
            fragment.arguments = args
            return fragment
        }
    }

    @Inject
    lateinit var vmFactory: ViewModelFactory

    @Inject
    lateinit var storageManager: FileDataStorageManager

    @Inject
    lateinit var runner: AsyncRunner

    @Inject
    lateinit var currentAccountProvider: CurrentAccountProvider

    @Inject
    lateinit var clientFactory: ClientFactory

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    private var listOfHiddenFiles = ArrayList<String>()

    private var showMoreActions = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vm = ViewModelProvider(this, vmFactory)[UnifiedSearchViewModel::class.java]
        setUpViewModel()

        val query = savedInstanceState?.getString(ARG_QUERY) ?: arguments?.getString(ARG_QUERY)
        listOfHiddenFiles =
            savedInstanceState?.getStringArrayList(ARG_HIDDEN_FILES) ?: arguments?.getStringArrayList(ARG_HIDDEN_FILES)
                ?: ArrayList()

        if (!query.isNullOrEmpty()) {
            vm.setQuery(query)
            vm.initialQuery()
        }
    }

    @Suppress("DEPRECATION")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ListFragmentBinding.inflate(inflater, container, false)
        binding.listRoot.updatePadding(top = resources.getDimension(R.dimen.standard_half_padding).toInt())
        setUpBinding()

        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupFileDisplayActivity()
        setupAdapter()
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        val item = menu.findItem(R.id.action_search)
        setupSearchView(item)
    }

    private fun setupSearchView(item: MenuItem) {
        (item.actionView as? SearchView?)?.run {
            // Required to align with TextView width.
            // Because this fragment is opened with TextView onClick on the previous screen
            maxWidth = Integer.MAX_VALUE
            viewThemeUtils.androidx.themeToolbarSearchView(this)
            setQuery(vm.query.value, false)
            setOnQueryTextListener(this@UnifiedSearchFragment)
            isIconified = false
            clearFocus()
        }
    }

    private fun setUpViewModel() {
        vm.searchResults.observe(this, this::onSearchResultChanged)
        vm.isLoading.observe(this) { loading ->
            binding.swipeContainingList.isRefreshing = loading
        }

        PairMediatorLiveData(vm.searchResults, vm.isLoading).observe(this) { pair ->
            if (pair.second == false) {
                var count = 0

                pair.first?.forEach {
                    count += it.entries.size
                }

                if (count == 0 && pair.first?.isNotEmpty() == true && context != null) {
                    binding.emptyList.root.visibility = View.VISIBLE
                    binding.emptyList.emptyListIcon.visibility = View.VISIBLE
                    binding.emptyList.emptyListViewHeadline.visibility = View.VISIBLE
                    binding.emptyList.emptyListViewText.visibility = View.VISIBLE
                    binding.emptyList.emptyListIcon.visibility = View.VISIBLE

                    binding.emptyList.emptyListViewHeadline.text =
                        requireContext().getString(R.string.file_list_empty_headline_server_search)
                    binding.emptyList.emptyListViewText.text =
                        requireContext().getString(R.string.file_list_empty_unified_search_no_results)
                    binding.emptyList.emptyListIcon.setImageDrawable(
                        viewThemeUtils.platform.tintDrawable(requireContext(), R.drawable.ic_search_grey)
                    )
                }
            }
        }

        vm.error.observe(this) { error ->
            if (!error.isNullOrEmpty()) {
                DisplayUtils.showSnackMessage(binding.root, error)
            }
        }
        vm.browserUri.observe(this) { uri ->
            val browserIntent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(browserIntent)
        }
        vm.file.observe(this) {
            showFile(it, showMoreActions)
        }
    }

    private fun setUpBinding() {
        binding.swipeContainingList.setOnRefreshListener {
            vm.initialQuery()
        }
    }

    private fun showFile(file: OCFile, showFileActions: Boolean) {
        activity.let {
            if (activity is FileDisplayActivity) {
                val fda = activity as FileDisplayActivity
                fda.file = file

                if (showFileActions) {
                    fda.showFileActions(file)
                } else {
                    fda.showFile(file, "")
                }
            }
        }
    }

    private fun setupFileDisplayActivity() {
        (activity as? FileDisplayActivity)?.run {
            setMainFabVisible(false)
            updateActionBarTitleAndHomeButtonByString(null)
        }
    }

    private fun setupAdapter() {
        val gridLayoutManager = GridLayoutManager(requireContext(), 1)
        adapter = UnifiedSearchListAdapter(
            storageManager,
            this,
            this,
            currentAccountProvider.user,
            clientFactory,
            requireContext(),
            viewThemeUtils
        )
        adapter.shouldShowFooters(true)
        adapter.setLayoutManager(gridLayoutManager)
        binding.listRoot.layoutManager = gridLayoutManager
        binding.listRoot.adapter = adapter
    }

    override fun onSearchResultClicked(searchResultEntry: SearchResultEntry) {
        showMoreActions = false
        vm.openResult(searchResultEntry)
    }

    override fun onLoadMoreClicked(providerID: ProviderID) {
        vm.loadMore(providerID)
    }

    @VisibleForTesting
    fun onSearchResultChanged(result: List<UnifiedSearchSection>) {
        Log_OC.d(TAG, "result")
        binding.emptyList.emptyListView.visibility = View.GONE
        adapter.setData(result.filterOutHiddenFiles(listOfHiddenFiles))
    }

    @VisibleForTesting
    fun setViewModel(testViewModel: IUnifiedSearchViewModel) {
        vm = testViewModel
        setUpViewModel()
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        vm.setQuery(query)
        vm.initialQuery()
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        val closeButton = searchView?.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
        closeButton?.visibility = if (newText?.isEmpty() == true) View.INVISIBLE else View.VISIBLE
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun showFilesAction(searchResultEntry: SearchResultEntry) {
        showMoreActions = true
        vm.openResult(searchResultEntry)
    }
}
