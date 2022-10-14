/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2022 Tobias Kaminsky
 * Copyright (C) 2022 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.nextcloud.client.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.LinearLayoutManager
import com.nextcloud.android.lib.resources.dashboard.DashBoardButtonType
import com.nextcloud.android.lib.resources.dashboard.DashboardListWidgetsRemoteOperation
import com.nextcloud.android.lib.resources.dashboard.DashboardWidget
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.client.network.ClientFactory.CreationException
import com.owncloud.android.R
import com.owncloud.android.databinding.DashboardWidgetConfigurationLayoutBinding
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.adapter.DashboardWidgetListAdapter
import com.owncloud.android.ui.dialog.AccountChooserInterface
import com.owncloud.android.ui.dialog.MultipleAccountsDialog
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DashboardWidgetConfigurationActivity :
    AppCompatActivity(),
    DashboardWidgetConfigurationInterface,
    Injectable,
    AccountChooserInterface {
    private lateinit var mAdapter: DashboardWidgetListAdapter
    private lateinit var binding: DashboardWidgetConfigurationLayoutBinding
    private lateinit var currentUser: User

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    @Inject
    lateinit var accountManager: UserAccountManager

    @Inject
    lateinit var clientFactory: ClientFactory

    @Inject
    lateinit var widgetRepository: WidgetRepository

    @Inject
    lateinit var widgetUpdater: DashboardWidgetUpdater

    var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    public override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED)

        binding = DashboardWidgetConfigurationLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewThemeUtils.platform.colorDrawable(binding.icon.drawable, getColor(R.color.dark))

        val layoutManager = LinearLayoutManager(this)
        // TODO follow our new architecture
        mAdapter = DashboardWidgetListAdapter(accountManager, clientFactory, this, this)
        binding.list.apply {
            setHasFooter(false)
            setAdapter(mAdapter)
            setLayoutManager(layoutManager)
            setEmptyView(binding.emptyView.emptyListView)
        }

        currentUser = accountManager.user
        if (accountManager.allUsers.size > 1) {
            binding.chooseWidget.visibility = View.GONE

            binding.accountName.apply {
                setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    viewThemeUtils.platform.colorDrawable(
                        AppCompatResources.getDrawable(
                            context,
                            R.drawable.ic_baseline_arrow_drop_down_24
                        )!!,
                        getColor(R.color.black)
                    ),
                    null
                )
                visibility = View.VISIBLE
                text = currentUser.accountName
                setOnClickListener {
                    val dialog = MultipleAccountsDialog()
                    dialog.highlightCurrentlyActiveAccount = false
                    dialog.show(supportFragmentManager, null)
                }
            }
        }
        loadWidgets(currentUser)

        binding.close.setOnClickListener { finish() }

        // Find the widget id from the intent.
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // If this activity was started with an intent without an app widget ID, finish with an error.
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
    }

    private fun loadWidgets(user: User) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                binding.emptyView.root.visibility = View.GONE
                if (accountManager.allUsers.size > 1) {
                    binding.accountName.text = user.accountName
                }
            }

            try {
                val client = clientFactory.createNextcloudClient(user)
                val result = DashboardListWidgetsRemoteOperation().execute(client)

                withContext(Dispatchers.Main) {
                    if (result.code == RemoteOperationResult.ResultCode.FILE_NOT_FOUND) {
                        withContext(Dispatchers.Main) {
                            mAdapter.setWidgetList(null)
                            binding.emptyView.root.visibility = View.VISIBLE
                            binding.emptyView.emptyListViewHeadline.setText(R.string.widgets_not_available_title)

                            binding.emptyView.emptyListIcon.apply {
                                setImageResource(R.drawable.ic_list_empty_error)
                                visibility = View.VISIBLE
                            }
                            binding.emptyView.emptyListViewText.apply {
                                setText(
                                    String.format(
                                        getString(R.string.widgets_not_available),
                                        getString(R.string.app_name)
                                    )
                                )
                                visibility = View.VISIBLE
                            }
                        }
                    } else {
                        mAdapter.setWidgetList(result.resultData)
                    }
                }
            } catch (e: CreationException) {
                Log_OC.e(this, "Error loading widgets for user $user", e)

                withContext(Dispatchers.Main) {
                    mAdapter.setWidgetList(null)
                    binding.emptyView.root.visibility = View.VISIBLE

                    binding.emptyView.emptyListIcon.apply {
                        setImageResource(R.drawable.ic_list_empty_error)
                        visibility = View.VISIBLE
                    }
                    binding.emptyView.emptyListViewText.apply {
                        setText(R.string.common_error)
                        visibility = View.VISIBLE
                    }
                    binding.emptyView.emptyListViewAction.apply {
                        visibility = View.VISIBLE
                        setText(R.string.reload)
                        setOnClickListener {
                            loadWidgets(user)
                        }
                    }
                }
            }
        }
    }

    override fun onItemClicked(dashboardWidget: DashboardWidget) {
        widgetRepository.saveWidget(appWidgetId, dashboardWidget, currentUser)

        // update widget
        val appWidgetManager = AppWidgetManager.getInstance(this)

        widgetUpdater.updateAppWidget(
            appWidgetManager,
            appWidgetId,
            dashboardWidget.title,
            dashboardWidget.iconUrl,
            dashboardWidget.buttons?.find { it.type == DashBoardButtonType.NEW }
        )

        val resultValue = Intent().apply {
            putExtra(EXTRA_APPWIDGET_ID, appWidgetId)
        }

        setResult(RESULT_OK, resultValue)
        finish()
    }

    override fun onAccountChosen(user: User) {
        currentUser = user
        loadWidgets(user)
    }
}
