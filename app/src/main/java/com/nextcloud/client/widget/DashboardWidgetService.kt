/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import com.nextcloud.android.lib.resources.dashboard.DashboardGetWidgetItemsRemoteOperation
import com.nextcloud.android.lib.resources.dashboard.DashboardWidgetItem
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.utils.GlideHelper
import com.owncloud.android.R
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.utils.BitmapUtils
import dagger.android.AndroidInjection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DashboardWidgetService : RemoteViewsService() {
    @Inject
    lateinit var userAccountManager: UserAccountManager

    @Inject
    lateinit var clientFactory: ClientFactory

    @Inject
    lateinit var widgetRepository: WidgetRepository

    override fun onCreate() {
        super.onCreate()
        AndroidInjection.inject(this)
    }

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = StackRemoteViewsFactory(
        this.applicationContext,
        userAccountManager,
        clientFactory,
        intent,
        widgetRepository
    )
}

class StackRemoteViewsFactory(
    private val context: Context,
    val userAccountManager: UserAccountManager,
    val clientFactory: ClientFactory,
    val intent: Intent,
    private val widgetRepository: WidgetRepository
) : RemoteViewsService.RemoteViewsFactory {

    private lateinit var widgetConfiguration: WidgetConfiguration
    private var widgetItems: List<DashboardWidgetItem> = emptyList()
    private var hasLoadMore = false

    override fun onCreate() {
        Log_OC.d(TAG, "onCreate")
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)

        widgetConfiguration = widgetRepository.getWidget(appWidgetId)

        if (!widgetConfiguration.user.isPresent) {
            // TODO show error
            Log_OC.e(this, "No user found!")
        }

        onDataSetChanged()
    }

    override fun onDataSetChanged() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!widgetConfiguration.user.isPresent) {
                    Log_OC.w(TAG, "User not present for widget update")
                    return@launch
                }

                val client = clientFactory.createNextcloudClient(widgetConfiguration.user.get())
                val result = DashboardGetWidgetItemsRemoteOperation(widgetConfiguration.widgetId, LIMIT_SIZE)
                    .execute(client)
                widgetItems = if (result.isSuccess) {
                    result.resultData[widgetConfiguration.widgetId] ?: emptyList()
                } else {
                    emptyList()
                }
                hasLoadMore = widgetConfiguration.moreButton != null && widgetItems.size == LIMIT_SIZE
            } catch (e: ClientFactory.CreationException) {
                Log_OC.e(TAG, "Error updating widget", e)
            }
        }

        Log_OC.d(TAG, "onDataSetChanged")
    }

    override fun onDestroy() {
        Log_OC.d(TAG, "onDestroy")

        widgetItems = emptyList()
    }

    override fun getCount(): Int = if (hasLoadMore && widgetItems.isNotEmpty()) {
        widgetItems.size + 1
    } else {
        widgetItems.size
    }

    override fun getViewAt(position: Int): RemoteViews = if (position == widgetItems.size) {
        createLoadMoreView()
    } else {
        createItemView(position)
    }

    private fun createLoadMoreView(): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_item_load_more).apply {
            val clickIntent = Intent(Intent.ACTION_VIEW, widgetConfiguration.moreButton?.link?.toUri())
            setTextViewText(R.id.load_more, widgetConfiguration.moreButton?.text)
            setOnClickFillInIntent(R.id.load_more_container, clickIntent)
        }

    // we will switch soon to coil and then streamline all of this
    // Kotlin cannot catch multiple exception types at same time
    @Suppress("NestedBlockDepth")
    private fun createItemView(position: Int): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_item).apply {
            if (widgetItems.isEmpty()) {
                return@apply
            }

            val widgetItem = widgetItems[position]

            if (widgetItem.iconUrl.isNotEmpty()) {
                loadIcon(widgetItem, this)
            }

            updateTexts(widgetItem, this)

            if (widgetItem.link.isNotEmpty()) {
                val clickIntent = Intent(Intent.ACTION_VIEW, widgetItem.link.toUri())
                setOnClickFillInIntent(R.id.text_container, clickIntent)
            }
        }
    }

    private fun loadIcon(widgetItem: DashboardWidgetItem, remoteViews: RemoteViews) {
        CoroutineScope(Dispatchers.IO).launch {
            val client = OwnCloudClientManagerFactory.getDefaultSingleton()
                .getNextcloudClientFor(userAccountManager.user.toOwnCloudAccount(), context)
            val pictureDrawable = GlideHelper.getDrawable(context, client, widgetItem.iconUrl)
            val bitmap = pictureDrawable?.toBitmap() ?: return@launch

            withContext(Dispatchers.Main) {
                remoteViews.setRemoteImageView(bitmap)
                return@withContext
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun RemoteViews.setRemoteImageView(source: Bitmap) {
        try {
            val bitmap: Bitmap = if (widgetConfiguration.roundIcon) {
                BitmapUtils.roundBitmap(source)
            } else {
                source
            }

            setImageViewBitmap(R.id.icon, bitmap)
        } catch (e: Exception) {
            Log_OC.d(TAG, "Error setting icon", e)
            setImageViewResource(R.id.icon, R.drawable.ic_dashboard)
        }
    }

    private fun updateTexts(widgetItem: DashboardWidgetItem, remoteViews: RemoteViews) {
        remoteViews.setTextViewText(R.id.title, widgetItem.title)

        if (widgetItem.subtitle.isNotEmpty()) {
            remoteViews.setViewVisibility(R.id.subtitle, View.VISIBLE)
            remoteViews.setTextViewText(R.id.subtitle, widgetItem.subtitle)
        } else {
            remoteViews.setViewVisibility(R.id.subtitle, View.GONE)
        }
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = if (hasLoadMore) {
        2
    } else {
        1
    }

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true

    companion object {
        private val TAG = DashboardWidgetService::class.simpleName
        const val LIMIT_SIZE = 14
    }
}
