/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.common.NextcloudClient
import com.owncloud.android.R
import com.owncloud.android.databinding.FragmentActivitiesBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.activities.model.RichObject
import com.owncloud.android.lib.resources.files.FileUtils
import com.owncloud.android.ui.activities.ActivitiesContract
import com.owncloud.android.ui.activities.ActivitiesPresenter
import com.owncloud.android.ui.activities.adapter.ActivityListAdapter
import com.owncloud.android.ui.activities.data.activities.ActivitiesRepository
import com.owncloud.android.ui.activities.data.files.FilesRepository
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.interfaces.ActivityListInterface
import com.owncloud.android.ui.navigation.NavigatorActivity
import com.owncloud.android.ui.preview.PreviewImageActivity
import com.owncloud.android.ui.preview.PreviewImageFragment.Companion.canBePreviewed
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

@Suppress("MagicNumber")
class ActivitiesFragment :
    Fragment(),
    ActivityListInterface,
    ActivitiesContract.View {

    @Inject
    lateinit var activitiesRepository: ActivitiesRepository

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    @Inject
    lateinit var filesRepository: FilesRepository

    @Inject
    lateinit var connectivityService: ConnectivityService

    @Inject
    lateinit var userAccountManager: UserAccountManager

    var binding: FragmentActivitiesBinding? = null

    private var adapter: ActivityListAdapter? = null
    private var lastGiven: Long = 0
    private var isLoadingActivities = false
    private var actionListener: ActivitiesContract.ActionListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentActivitiesBinding.inflate(inflater, container, false)
        val binding = binding!!
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        actionListener = ActivitiesPresenter(activitiesRepository, filesRepository, this)
        binding?.swipeContainingList?.let { viewThemeUtils.androidx.themeSwipeRefreshLayout(it) }
        binding?.swipeContainingList?.setOnRefreshListener {
            lastGiven = ActivitiesContract.ActionListener.UNDEFINED.toLong()
            actionListener?.loadActivities(lifecycleScope, lastGiven)
        }
        setupContent()
    }

    private fun setupContent() {
        binding?.emptyList?.emptyListIcon?.setImageResource(R.drawable.ic_activity)
        adapter = ActivityListAdapter(
            requireActivity(),
            userAccountManager,
            this,
            false,
            viewThemeUtils
        )
        binding?.list?.adapter = adapter
        val layoutManager = LinearLayoutManager(requireContext())
        binding?.list?.run {
            setLayoutManager(layoutManager)
            addOnScrollListener(getOnScrollListener(layoutManager))
        }
        actionListener?.loadActivities(lifecycleScope, ActivitiesContract.ActionListener.UNDEFINED.toLong())
    }

    private fun getOnScrollListener(layoutManager: LinearLayoutManager) = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            val visibleItemCount = recyclerView.size
            val totalItemCount = layoutManager.itemCount
            val firstVisibleItemIndex = layoutManager.findFirstVisibleItemPosition()

            if (!isLoadingActivities &&
                (totalItemCount - visibleItemCount) <= (firstVisibleItemIndex + 5) &&
                lastGiven > 0
            ) {
                actionListener?.loadActivities(lifecycleScope, lastGiven)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        actionListener?.onResume()
    }

    override fun onStop() {
        super.onStop()
        actionListener?.onStop()
    }

    override fun onActivityClicked(richObject: RichObject) {
        val path = FileUtils.PATH_SEPARATOR + richObject.path
        val baseActivity = requireActivity() as NavigatorActivity
        actionListener?.openActivity(path, baseActivity)
    }

    override fun showActivities(activities: List<Any>, client: NextcloudClient, lastGiven: Long) {
        val binding = binding ?: return
        val clear = this.lastGiven == ActivitiesContract.ActionListener.UNDEFINED.toLong()
        adapter?.setActivityItems(activities, client, clear)
        this.lastGiven = lastGiven

        if (adapter?.isEmpty() == true) {
            showEmptyContent(
                getString(R.string.activities_no_results_headline),
                getString(R.string.activities_no_results_message)
            )
            binding.loadingContent.visibility = View.GONE
            binding.list.visibility = View.GONE
        } else {
            binding.emptyList.emptyListView.visibility = View.GONE
            binding.loadingContent.visibility = View.GONE
            binding.list.visibility = View.VISIBLE
        }
    }

    override fun showActivitiesLoadError(error: String) {
        val binding = binding ?: return
        connectivityService.isNetworkAndServerAvailable {
            if (it) {
                DisplayUtils.showSnackMessage(requireView(), error)
            } else {
                showEmptyContent(
                    getString(R.string.server_not_reachable),
                    getString(R.string.server_not_reachable_content)
                )
                binding.emptyList.emptyListIcon.setImageResource(R.drawable.ic_sync_off)
            }
        }
    }

    override fun showActivityDetailUI(ocFile: OCFile) {
        val user = userAccountManager.user
        val intent = if (canBePreviewed(ocFile)) {
            Intent(requireContext(), PreviewImageActivity::class.java)
        } else {
            Intent(requireContext(), FileDisplayActivity::class.java)
        }.apply {
            putExtra(FileActivity.EXTRA_FILE, ocFile)
            putExtra(FileActivity.EXTRA_USER, user)
        }
        startActivity(intent)
    }

    override fun showActivityDetailUIIsNull() {
        DisplayUtils.showSnackMessage(requireView(), R.string.file_not_found)
    }

    override fun showActivityDetailError(error: String) {
        DisplayUtils.showSnackMessage(requireView(), error)
    }

    override fun showLoadingMessage() {
        binding?.emptyList?.emptyListView?.visibility = View.GONE
    }

    override fun showEmptyContent(headline: String, message: String) {
        binding?.run {
            emptyList.emptyListViewHeadline.text = headline
            emptyList.emptyListViewText.text = message
            loadingContent.visibility = View.GONE
            emptyList.emptyListIcon.visibility = View.VISIBLE
            emptyList.emptyListViewHeadline.visibility = View.VISIBLE
            emptyList.emptyListViewText.visibility = View.VISIBLE
            emptyList.emptyListView.visibility = View.VISIBLE
        }
    }

    override fun setProgressIndicatorState(isActive: Boolean) {
        val binding = binding ?: return
        isLoadingActivities = isActive
        if (adapter?.isEmpty() == false) {
            binding.swipeContainingList.post { binding.swipeContainingList.isRefreshing = isActive }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}
