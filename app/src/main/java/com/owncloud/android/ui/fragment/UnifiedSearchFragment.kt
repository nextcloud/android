/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.fragment
import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.appcompat.widget.SearchView
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.nextcloud.client.account.CurrentAccountProvider
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.core.AsyncRunner
import com.nextcloud.client.core.Clock
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.di.ViewModelFactory
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.utils.extensions.getTypedActivity
import com.nextcloud.utils.extensions.searchFilesByName
import com.nextcloud.utils.extensions.typedActivity
import com.owncloud.android.R
import com.owncloud.android.databinding.ListFragmentBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.lib.common.SearchResultEntry
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.status.NextcloudVersion
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.adapter.UnifiedSearchItemViewHolder
import com.owncloud.android.ui.adapter.UnifiedSearchListAdapter
import com.owncloud.android.ui.fragment.util.PairMediatorLiveData
import com.owncloud.android.ui.interfaces.UnifiedSearchCurrentDirItemAction
import com.owncloud.android.ui.interfaces.UnifiedSearchListInterface
import com.owncloud.android.ui.unifiedsearch.IUnifiedSearchViewModel
import com.owncloud.android.ui.unifiedsearch.ProviderID
import com.owncloud.android.ui.unifiedsearch.UnifiedSearchSection
import com.owncloud.android.ui.unifiedsearch.UnifiedSearchViewModel
import com.owncloud.android.ui.unifiedsearch.filterOutHiddenFiles
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.PermissionUtil
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Starts query to all capable unified search providers and displays them Opens result in our app, redirect to other
 * apps, if installed, or opens browser
 */
@Suppress("TooManyFunctions")
class UnifiedSearchFragment :
    Fragment(),
    Injectable,
    UnifiedSearchListInterface,
    SearchView.OnQueryTextListener,
    UnifiedSearchItemViewHolder.FilesAction,
    UnifiedSearchCurrentDirItemAction {
    private lateinit var adapter: UnifiedSearchListAdapter
    private var _binding: ListFragmentBinding? = null
    val binding get() = _binding!!
    private var searchView: SearchView? = null
    lateinit var vm: IUnifiedSearchViewModel

    companion object {
        private const val TAG = "UnifiedSearchFragment"

        private const val ARG_QUERY = "ARG_QUERY"
        private const val ARG_HIDDEN_FILES = "ARG_HIDDEN_FILES"
        private const val CURRENT_DIR_PATH = "CURRENT_DIR"

        fun newInstance(
            query: String?,
            listOfHiddenFiles: ArrayList<String>?,
            currentDirPath: String
        ): UnifiedSearchFragment = UnifiedSearchFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_QUERY, query)
                putString(CURRENT_DIR_PATH, currentDirPath)
                putStringArrayList(ARG_HIDDEN_FILES, listOfHiddenFiles)
            }
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

    @Inject
    lateinit var accountManager: UserAccountManager

    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var clock: Clock

    private var listOfHiddenFiles = ArrayList<String>()
    private var showMoreActions = false
    private var currentDir: OCFile? = null
    private var initialQuery: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vm = ViewModelProvider(this, vmFactory)[UnifiedSearchViewModel::class.java]
        initialQuery = savedInstanceState?.getString(ARG_QUERY) ?: arguments?.getString(ARG_QUERY)
        val currentDirPath = savedInstanceState?.getString(CURRENT_DIR_PATH) ?: arguments?.getString(CURRENT_DIR_PATH)
        currentDir = storageManager.getFileByDecryptedRemotePath(currentDirPath)
        listOfHiddenFiles =
            savedInstanceState?.getStringArrayList(ARG_HIDDEN_FILES) ?: arguments?.getStringArrayList(ARG_HIDDEN_FILES)
                ?: ArrayList()
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

        setupAdapter()
        if (supportsOpeningCalendarContactsLocally()) {
            checkPermissions()
        }
    }

    override fun onResume() {
        super.onResume()
        typedActivity<FileDisplayActivity>()?.run {
            setupToolbar()
            setMainFabVisible(false)
            updateActionBarTitleAndHomeButtonByString(null)
        }
    }

    private fun supportsOpeningCalendarContactsLocally(): Boolean = storageManager
        .getCapability(accountManager.user)
        .version
        .isNewerOrEqual(NextcloudVersion.nextcloud_30)

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        val item = menu.findItem(R.id.action_search)
        setupSearchView(item)
    }

    private fun checkPermissions() {
        val permissions = arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.READ_CALENDAR)
        if (!PermissionUtil.checkPermissions(requireContext(), permissions)) {
            permissionLauncher.launch(permissions)
        }
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions.entries.all { it.value }
            if (!granted) {
                DisplayUtils.showSnackMessage(binding.root, R.string.unified_search_fragment_permission_needed)
            }
        }

    private fun setupSearchView(item: MenuItem) {
        (item.actionView as? SearchView?)?.run {
            // Required to align with TextView width.
            // Because this fragment is opened with TextView onClick on the previous screen
            maxWidth = Integer.MAX_VALUE
            viewThemeUtils.androidx.themeToolbarSearchView(this)
            setQuery(vm.query.value ?: initialQuery, false)
            setOnQueryTextListener(this@UnifiedSearchFragment)
            isIconified = false
            clearFocus()
            setSearchAction(this)
            setCloseAction(this)
        }
    }

    private fun setCloseAction(searchView: SearchView) {
        val closeButton = searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
        closeButton?.setOnClickListener {
            searchView.run {
                setQuery("", false)
                clearFocus()
            }

            vm.setQuery("")
            adapter.setData(emptyList())
            adapter.setDataCurrentDirItems(listOf())

            showStartYourSearch()
            showKeyboard(searchView)
        }
    }

    private fun makeEmptyListVisible() {
        binding.emptyList.run {
            root.visibility = View.VISIBLE
            emptyListIcon.visibility = View.VISIBLE
            emptyListViewHeadline.visibility = View.VISIBLE
            emptyListViewText.visibility = View.VISIBLE
            emptyListIcon.visibility = View.VISIBLE
        }
    }

    private fun showStartYourSearch() {
        makeEmptyListVisible()

        binding.emptyList.run {
            emptyListViewHeadline.text = getString(R.string.file_list_empty_unified_search_start_search)
            emptyListViewText.text = getString(R.string.file_list_empty_unified_search_start_search_description)
            emptyListIcon.setImageDrawable(
                viewThemeUtils.platform.tintDrawable(
                    requireContext(),
                    R.drawable.ic_search_grey
                )
            )
        }
    }

    private fun showNoResult() {
        makeEmptyListVisible()

        binding.emptyList.run {
            emptyListViewHeadline.text =
                requireContext().getString(R.string.file_list_empty_headline_server_search)
            emptyListViewText.text =
                requireContext().getString(R.string.file_list_empty_unified_search_no_results)
            emptyListIcon.setImageDrawable(
                viewThemeUtils.platform.tintDrawable(requireContext(), R.drawable.ic_search_grey)
            )
        }
    }

    private fun setSearchAction(searchView: SearchView) {
        val searchEditText = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        searchEditText.setOnEditorActionListener { v, actionId, _ ->
            val isActionSearch = (actionId == EditorInfo.IME_ACTION_SEARCH)
            if (isActionSearch) {
                // Hide keyboard
                (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).apply {
                    hideSoftInputFromWindow(v.windowToken, 0)
                }

                // Disable cursor
                searchEditText.run {
                    isCursorVisible = false
                    clearFocus()
                    onQueryTextSubmit(text.toString())
                }
            }

            isActionSearch
        }

        searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            searchEditText.isCursorVisible = hasFocus
        }
    }

    private fun showKeyboard(searchView: SearchView) {
        val searchEditText = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        searchEditText?.apply {
            requestFocus()
            post {
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    @Suppress("ComplexCondition")
    private fun setUpViewModel() {
        vm.searchResults.observe(viewLifecycleOwner, this::onSearchResultChanged)
        vm.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.swipeContainingList.isRefreshing = loading
        }

        PairMediatorLiveData(vm.searchResults, vm.isLoading).observe(viewLifecycleOwner) { pair ->
            if (pair.second == false) {
                var count = 0

                pair.first?.forEach {
                    count += it.entries.size
                }

                if (count == 0 &&
                    pair.first?.isNotEmpty() == true &&
                    context != null &&
                    !adapter.isCurrentDirItemsEmpty()
                ) {
                    showNoResult()
                }
            }
        }

        vm.error.observe(viewLifecycleOwner) { error ->
            if (!error.isNullOrEmpty()) {
                DisplayUtils.showSnackMessage(binding.root, error)
            }
        }
        vm.browserUri.observe(viewLifecycleOwner) { uri ->
            val browserIntent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(browserIntent)
        }
        vm.file.observe(viewLifecycleOwner) {
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

    private fun setupAdapter() {
        val syncedFolderProvider = SyncedFolderProvider(requireContext().contentResolver, appPreferences, clock)
        val gridLayoutManager = GridLayoutManager(requireContext(), 1)

        lifecycleScope.launch(Dispatchers.IO) {
            val client =
                getTypedActivity(FileActivity::class.java)?.clientRepository?.getNextcloudClient() ?: return@launch

            withContext(Dispatchers.Main) {
                adapter = UnifiedSearchListAdapter(
                    supportsOpeningCalendarContactsLocally(),
                    storageManager,
                    this@UnifiedSearchFragment,
                    this@UnifiedSearchFragment,
                    currentAccountProvider.user,
                    requireContext(),
                    viewThemeUtils,
                    appPreferences,
                    syncedFolderProvider,
                    client,
                    this@UnifiedSearchFragment
                )

                adapter.shouldShowFooters(true)
                adapter.setLayoutManager(gridLayoutManager)
                binding.listRoot.layoutManager = gridLayoutManager
                binding.listRoot.adapter = adapter
                searchInCurrentDirectory(initialQuery ?: "")

                setUpViewModel()
                if (!initialQuery.isNullOrEmpty()) {
                    vm.setQuery(initialQuery!!)
                    vm.initialQuery()
                }
            }
        }
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
        searchInCurrentDirectory(newText ?: "")
        return true
    }

    private fun searchInCurrentDirectory(query: String) {
        currentDir?.run {
            val files = storageManager.searchFilesByName(this, accountManager.user.accountName, query)
            adapter.setDataCurrentDirItems(files)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun showFilesAction(searchResultEntry: SearchResultEntry) {
        showMoreActions = true
        vm.openResult(searchResultEntry)
    }

    override fun openFile(remotePath: String, showMoreActions: Boolean) {
        this.showMoreActions = showMoreActions
        vm.openFile(remotePath)
    }
}
