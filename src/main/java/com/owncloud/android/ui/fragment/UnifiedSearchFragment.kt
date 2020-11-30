/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.nextcloud.client.account.CurrentAccountProvider
import com.nextcloud.client.core.AsyncRunner
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.di.ViewModelFactory
import com.nextcloud.client.network.ClientFactory
import com.owncloud.android.databinding.ListFragmentBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.lib.common.SearchResult
import com.owncloud.android.lib.common.SearchResultEntry
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.adapter.UnifiedSearchListAdapter
import com.owncloud.android.ui.asynctasks.GetRemoteFileTask
import com.owncloud.android.ui.interfaces.UnifiedSearchListInterface
import com.owncloud.android.ui.unifiedsearch.UnifiedSearchViewModel
import javax.inject.Inject

/**
 * Starts query to all capable unified search providers and displays them Opens result in our app, redirect to other
 * apps, if installed, or opens browser
 */
class UnifiedSearchFragment : Fragment(), Injectable, UnifiedSearchListInterface {
    private lateinit var adapter: UnifiedSearchListAdapter
    private var _binding: ListFragmentBinding? = null
    private val binding get() = _binding!!
    lateinit var vm: UnifiedSearchViewModel

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vm = ViewModelProvider(this, vmFactory).get(UnifiedSearchViewModel::class.java)
        vm.searchResults.observe(this, this::onSearchResultChanged)

        val query = savedInstanceState?.getString(ARG_QUERY).orEmpty()
        if (query.isNotBlank()) {
            vm.startLoading(query)
            return
        }

        val queryArgument = arguments?.getString(ARG_QUERY).orEmpty()
        if (queryArgument.isNotBlank()) {
            vm.startLoading(queryArgument)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = ListFragmentBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val gridLayoutManager = GridLayoutManager(requireContext(), 1)
        adapter = UnifiedSearchListAdapter(
            storageManager,
            this,
            currentAccountProvider.user,
            clientFactory,
            requireContext()
        )
        adapter.setLayoutManager(gridLayoutManager)
        binding.listRoot.layoutManager = gridLayoutManager
        binding.listRoot.adapter = adapter
    }

    override fun onPause() {
        super.onPause()
        //photoSearchTask?.cancel(true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showFile(result: GetRemoteFileTask.Result) {
        activity.let {
            if (activity is FileDisplayActivity) {
                val fda = activity as FileDisplayActivity
                fda.file = result.file
                fda.showFile("")
            }
        }
    }

    override fun onSearchResultClicked(searchResultEntry: SearchResultEntry) {
        openFile(searchResultEntry.remotePath())
    }

    fun openFile(fileUrl: String) {
        val user = currentAccountProvider.user
        val task = GetRemoteFileTask(
            requireContext(),
            fileUrl,
            clientFactory.create(currentAccountProvider.user),
            FileDataStorageManager(user.toPlatformAccount(), requireContext().contentResolver),
            user
        )
        runner.postQuickTask(task, onResult = this::showFile)
    }

    @VisibleForTesting
    fun onSearchResultChanged(list: MutableList<SearchResult>) {
        binding.emptyList.emptyListView.visibility = View.GONE

        adapter.setList(list)
    }

    @VisibleForTesting
    fun setViewModel(testViewModel: UnifiedSearchViewModel) {
        vm = testViewModel
        vm.searchResults.observe(this, this::onSearchResultChanged)
    }

    companion object {
        const val ARG_QUERY = "ARG_QUERY"

        /**
         * Public factory method to get fragment.
         */
        fun newInstance(query: String?): UnifiedSearchFragment {
            val fragment = UnifiedSearchFragment()
            val args = Bundle()
            args.putString(ARG_QUERY, query)
            fragment.arguments = args
            return fragment
        }
    }
}
