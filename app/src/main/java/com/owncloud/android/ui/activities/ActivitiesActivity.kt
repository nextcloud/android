/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2018 Edvard Holst <edvard.holst@gmail.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.core.view.size
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.common.NextcloudClient
import com.owncloud.android.R
import com.owncloud.android.databinding.ActivityListLayoutBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.activities.model.RichObject
import com.owncloud.android.lib.resources.files.FileUtils
import com.owncloud.android.ui.activities.data.activities.ActivitiesRepository
import com.owncloud.android.ui.activities.data.files.FilesRepository
import com.owncloud.android.ui.activity.DrawerActivity
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.activities.adapter.ActivityListAdapter
import com.owncloud.android.ui.interfaces.ActivityListInterface
import com.owncloud.android.ui.preview.PreviewImageActivity
import com.owncloud.android.ui.preview.PreviewImageFragment.Companion.canBePreviewed
import com.owncloud.android.utils.DisplayUtils
import java.util.function.Supplier
import javax.inject.Inject

@Suppress("MagicNumber")
class ActivitiesActivity :
    DrawerActivity(),
    ActivityListInterface,
    ActivitiesContract.View {
    var binding: ActivityListLayoutBinding? = null
    private var adapter: ActivityListAdapter? = null
    private var lastGiven: Long = 0
    private var isLoadingActivities = false
    private var actionListener: ActivitiesContract.ActionListener? = null

    @Inject
    lateinit var activitiesRepository: ActivitiesRepository

    @Inject
    lateinit var filesRepository: FilesRepository

    @Inject
    lateinit var connectivityService: ConnectivityService

    override fun onCreate(savedInstanceState: Bundle?) {
        Log_OC.v(TAG, "onCreate() start")
        super.onCreate(savedInstanceState)

        actionListener = ActivitiesPresenter(activitiesRepository, filesRepository, this)

        binding = ActivityListLayoutBinding.inflate(layoutInflater)
        setContentView(binding?.getRoot())

        setupToolbar()

        binding?.swipeContainingList?.let { viewThemeUtils.androidx.themeSwipeRefreshLayout(it) }

        setupDrawer(menuItemId)
        updateActionBarTitleAndHomeButtonByString(getString(R.string.drawer_item_activities))

        binding?.swipeContainingList?.setOnRefreshListener {
            // We set lastGiven variable to undefined here since when manually refreshing
            // activities data we want to clear the list and reset the pagination.
            lastGiven = ActivitiesContract.ActionListener.UNDEFINED.toLong()
            actionListener?.loadActivities(lastGiven)
        }
    }

    override fun getMenuItemId(): Int = R.id.nav_activity

    @Suppress("MagicNumber")
    private fun setupContent() {
        binding?.emptyList?.emptyListIcon?.setImageResource(R.drawable.ic_activity)

        adapter = ActivityListAdapter(
            this,
            userAccountManager,
            this,
            false,
            viewThemeUtils
        )
        binding?.list?.setAdapter(adapter)

        val layoutManager = LinearLayoutManager(this)

        binding?.list?.run {
            setLayoutManager(layoutManager)
            addOnScrollListener(getOnScrollListener(layoutManager))
        }

        actionListener?.loadActivities(ActivitiesContract.ActionListener.UNDEFINED.toLong())
    }

    private fun getOnScrollListener(layoutManager: LinearLayoutManager) = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            val visibleItemCount = recyclerView.size
            val totalItemCount = layoutManager.getItemCount()
            val firstVisibleItemIndex = layoutManager.findFirstVisibleItemPosition()

            // synchronize loading state when item count changes
            if (!isLoadingActivities && (totalItemCount - visibleItemCount) <= (firstVisibleItemIndex + 5) &&
                lastGiven > 0
            ) {
                // Almost reached the end, continue to load new activities
                actionListener?.loadActivities(lastGiven)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = if (item.itemId == android.R.id.home) {
        if (isDrawerOpen) {
            closeDrawer()
        } else {
            openDrawer()
        }
        true
    } else {
        Log_OC.w(TAG, "Unknown menu item triggered")
        super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        highlightNavigationViewItem(menuItemId)
        actionListener?.onResume()
        setupContent()
    }

    override fun onActivityClicked(richObject: RichObject) {
        val path = FileUtils.PATH_SEPARATOR + richObject.path
        actionListener?.openActivity(path, this)
    }

    override fun showActivities(activities: List<Any>, client: NextcloudClient, lastGiven: Long) {
        val clear = this.lastGiven == ActivitiesContract.ActionListener.UNDEFINED.toLong()
        adapter?.setActivityItems(activities, client, clear)
        this.lastGiven = lastGiven

        // Hide the recyclerView if list is empty
        if (adapter?.isEmpty() == true) {
            showEmptyContent(
                getString(R.string.activities_no_results_headline),
                getString(R.string.activities_no_results_message)
            )
            binding?.loadingContent?.visibility = View.GONE
            binding?.list?.visibility = View.GONE
        } else {
            binding?.emptyList?.emptyListView?.visibility = View.GONE
            binding?.loadingContent?.visibility = View.GONE
            binding?.list?.visibility = View.VISIBLE
        }
    }

    override fun showActivitiesLoadError(error: String) {
        connectivityService.isNetworkAndServerAvailable {
            if (it) {
                DisplayUtils.showSnackMessage(this, error)
            } else {
                showEmptyContent(
                    getString(R.string.server_not_reachable),
                    getString(R.string.server_not_reachable_content)
                )
                binding?.emptyList?.emptyListIcon?.setImageResource(R.drawable.ic_sync_off)
            }
        }
    }

    override fun showActivityDetailUI(ocFile: OCFile) {
        val intent = if (canBePreviewed(ocFile)) {
            Intent(baseContext, PreviewImageActivity::class.java)
        } else {
            Intent(baseContext, FileDisplayActivity::class.java)
        }.apply {
            putExtra(FileActivity.EXTRA_FILE, ocFile)
            putExtra(
                FileActivity.EXTRA_USER,
                user.orElseThrow(Supplier { RuntimeException() })
            )
        }

        startActivity(intent)
    }

    override fun showActivityDetailUIIsNull() {
        DisplayUtils.showSnackMessage(this, R.string.file_not_found)
    }

    override fun showActivityDetailError(error: String) {
        DisplayUtils.showSnackMessage(this, error)
    }

    override fun showLoadingMessage() {
        binding?.emptyList?.emptyListView?.visibility = View.GONE
    }

    override fun showEmptyContent(headline: String, message: String) {
        binding?.run {
            emptyList.emptyListViewHeadline.text = headline
            emptyList.emptyListViewText.text = message
            loadingContent.visibility = View.GONE
            emptyList.emptyListIcon.setVisibility(View.VISIBLE)
            emptyList.emptyListViewHeadline.visibility = View.VISIBLE
            emptyList.emptyListViewText.visibility = View.VISIBLE
            emptyList.emptyListView.visibility = View.VISIBLE
        }
    }

    override fun setProgressIndicatorState(isActive: Boolean) {
        isLoadingActivities = isActive
        if (adapter?.isEmpty() == false) {
            binding?.swipeContainingList?.post { binding?.swipeContainingList?.isRefreshing = isActive }
        }
    }

    override fun onStop() {
        super.onStop()
        actionListener?.onStop()
    }

    companion object {
        private val TAG: String = ActivitiesActivity::class.java.getSimpleName()
    }
}
