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
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.model.StreamEncoder
import com.bumptech.glide.load.resource.file.FileToStreamDecoder
import com.bumptech.glide.request.FutureTarget
import com.nextcloud.android.lib.resources.dashboard.DashboardGetWidgetItemsRemoteOperation
import com.nextcloud.android.lib.resources.dashboard.DashboardWidgetItem
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.network.ClientFactory
import com.owncloud.android.R
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.utils.BitmapUtils
import com.owncloud.android.utils.DisplayUtils.SVG_SIZE
import com.owncloud.android.utils.glide.CustomGlideStreamLoader
import com.owncloud.android.utils.glide.CustomGlideUriLoader
import com.owncloud.android.utils.svg.SVGorImage
import com.owncloud.android.utils.svg.SvgOrImageBitmapTranscoder
import com.owncloud.android.utils.svg.SvgOrImageDecoder
import dagger.android.AndroidInjection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.InputStream
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

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return StackRemoteViewsFactory(
            this.applicationContext,
            userAccountManager,
            clientFactory,
            intent,
            widgetRepository
        )
    }
}

class StackRemoteViewsFactory(
    private val context: Context,
    val userAccountManager: UserAccountManager,
    val clientFactory: ClientFactory,
    val intent: Intent,
    val widgetRepository: WidgetRepository
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
                if (widgetConfiguration.user.isPresent) {
                    val client = clientFactory.createNextcloudClient(widgetConfiguration.user.get())
                    val result = DashboardGetWidgetItemsRemoteOperation(widgetConfiguration.widgetId, LIMIT_SIZE)
                        .execute(client)
                    widgetItems = if (result.isSuccess) {
                        result.resultData[widgetConfiguration.widgetId] ?: emptyList()
                    } else {
                        emptyList()
                    }
                    hasLoadMore = widgetConfiguration.moreButton != null && widgetItems.size == LIMIT_SIZE
                } else {
                    Log_OC.w(TAG, "User not present for widget update")
                }
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

    override fun getCount(): Int {
        return if (hasLoadMore && widgetItems.isNotEmpty()) {
            widgetItems.size + 1
        } else {
            widgetItems.size
        }
    }

    override fun getViewAt(position: Int): RemoteViews {
        return if (position == widgetItems.size) {
            createLoadMoreView()
        } else {
            createItemView(position)
        }
    }

    private fun createLoadMoreView(): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_item_load_more).apply {
            val clickIntent = Intent(Intent.ACTION_VIEW, Uri.parse(widgetConfiguration.moreButton?.link))
            setTextViewText(R.id.load_more, widgetConfiguration.moreButton?.text)
            setOnClickFillInIntent(R.id.load_more_container, clickIntent)
        }
    }

    // we will switch soon to coil and then streamline all of this
    // Kotlin cannot catch multiple exception types at same time
    @Suppress("NestedBlockDepth", "TooGenericExceptionCaught")
    private fun createItemView(position: Int): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_item).apply {
            val widgetItem = widgetItems[position]

            // icon bitmap/svg
            if (widgetItem.iconUrl.isNotEmpty()) {
                val glide: FutureTarget<Bitmap>
                if (Uri.parse(widgetItem.iconUrl).encodedPath!!.endsWith(".svg")) {
                    glide = Glide.with(context)
                        .using(
                            CustomGlideUriLoader(userAccountManager.user, clientFactory),
                            InputStream::class.java
                        )
                        .from(Uri::class.java)
                        .`as`(SVGorImage::class.java)
                        .transcode(SvgOrImageBitmapTranscoder(SVG_SIZE, SVG_SIZE), Bitmap::class.java)
                        .sourceEncoder(StreamEncoder())
                        .cacheDecoder(FileToStreamDecoder(SvgOrImageDecoder()))
                        .decoder(SvgOrImageDecoder())
                        .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                        .load(Uri.parse(widgetItem.iconUrl))
                        .into(SVG_SIZE, SVG_SIZE)
                } else {
                    glide = Glide.with(context)
                        .using(CustomGlideStreamLoader(widgetConfiguration.user.get(), clientFactory))
                        .load(widgetItem.iconUrl)
                        .asBitmap()
                        .into(SVG_SIZE, SVG_SIZE)
                }

                try {
                    if (widgetConfiguration.roundIcon) {
                        setImageViewBitmap(R.id.icon, BitmapUtils.roundBitmap(glide.get()))
                    } else {
                        setImageViewBitmap(R.id.icon, glide.get())
                    }
                } catch (e: Exception) {
                    Log_OC.d(TAG, "Error setting icon", e)
                    setImageViewResource(R.id.icon, R.drawable.ic_dashboard)
                }
            }

            // text
            setTextViewText(R.id.title, widgetItem.title)

            if (widgetItem.subtitle.isNotEmpty()) {
                setViewVisibility(R.id.subtitle, View.VISIBLE)
                setTextViewText(R.id.subtitle, widgetItem.subtitle)
            } else {
                setViewVisibility(R.id.subtitle, View.GONE)
            }

            if (widgetItem.link.isNotEmpty()) {
                val clickIntent = Intent(Intent.ACTION_VIEW, Uri.parse(widgetItem.link))
                setOnClickFillInIntent(R.id.text_container, clickIntent)
            }
        }
    }

    override fun getLoadingView(): RemoteViews? {
        return null
    }

    override fun getViewTypeCount(): Int {
        return if (hasLoadMore) {
            2
        } else {
            1
        }
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    companion object {
        private val TAG = DashboardWidgetService::class.simpleName
        const val LIMIT_SIZE = 14
    }
}
