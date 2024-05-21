/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.model.StreamEncoder
import com.bumptech.glide.load.resource.file.FileToStreamDecoder
import com.bumptech.glide.request.animation.GlideAnimation
import com.bumptech.glide.request.target.AppWidgetTarget
import com.nextcloud.android.lib.resources.dashboard.DashboardButton
import com.nextcloud.client.account.CurrentAccountProvider
import com.nextcloud.client.network.ClientFactory
import com.owncloud.android.R
import com.owncloud.android.utils.BitmapUtils
import com.owncloud.android.utils.DisplayUtils.SVG_SIZE
import com.owncloud.android.utils.glide.CustomGlideUriLoader
import com.owncloud.android.utils.svg.SVGorImage
import com.owncloud.android.utils.svg.SvgOrImageBitmapTranscoder
import com.owncloud.android.utils.svg.SvgOrImageDecoder
import java.io.InputStream
import javax.inject.Inject

class DashboardWidgetUpdater @Inject constructor(
    private val context: Context,
    private val clientFactory: ClientFactory,
    private val accountProvider: CurrentAccountProvider
) {

    fun updateAppWidget(
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        title: String,
        iconUrl: String,
        addButton: DashboardButton?
    ) {
        val intent = Intent(context, DashboardWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }

        val views = RemoteViews(context.packageName, R.layout.dashboard_widget).apply {
            setRemoteAdapter(R.id.list, intent)
            setEmptyView(R.id.list, R.id.empty_view)
            setTextViewText(R.id.title, title)

            setAddButton(addButton, appWidgetId, this)
            setPendingReload(this, appWidgetId)
            setPendingClick(this)
            loadIcon(appWidgetId, iconUrl, this)
        }

        appWidgetManager.run {
            updateAppWidget(appWidgetId, views)
            notifyAppWidgetViewDataChanged(appWidgetId, R.id.list)
        }
    }

    private fun setPendingReload(remoteViews: RemoteViews, appWidgetId: Int) {
        val pendingIntent = getReloadPendingIntent(appWidgetId)
        remoteViews.setOnClickPendingIntent(R.id.reload, pendingIntent)
    }

    private fun setPendingClick(remoteViews: RemoteViews) {
        val intent = Intent().apply {
            setPackage(context.packageName)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            pendingIntentFlags
        )

        remoteViews.setPendingIntentTemplate(R.id.list, pendingIntent)
    }

    private fun setAddButton(addButton: DashboardButton?, appWidgetId: Int, remoteViews: RemoteViews) {
        remoteViews.run {
            if (addButton == null) {
                setViewVisibility(R.id.create, View.GONE)
            } else {
                setViewVisibility(R.id.create, View.VISIBLE)
                setContentDescription(R.id.create, addButton.text)

                val pendingIntent = getAddPendingIntent(appWidgetId, addButton)

                setOnClickPendingIntent(
                    R.id.create,
                    pendingIntent
                )
            }
        }
    }

    // region PendingIntents
    private fun getReloadPendingIntent(appWidgetId: Int): PendingIntent {
        val intent = Intent(context, DashboardWidgetProvider::class.java).apply {
            setPackage(context.packageName)
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE

            val idArray = intArrayOf(appWidgetId)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, idArray)
        }

        return PendingIntent.getBroadcast(
            context,
            appWidgetId,
            intent,
            pendingIntentFlags
        )
    }

    private fun getAddPendingIntent(appWidgetId: Int, addButton: DashboardButton): PendingIntent {
        val intent = Intent(context, DashboardWidgetProvider::class.java).apply {
            setPackage(context.packageName)
            action = DashboardWidgetProvider.OPEN_INTENT
            data = Uri.parse(addButton.link)
        }

        return PendingIntent.getBroadcast(
            context,
            appWidgetId,
            intent,
            pendingIntentFlags
        )
    }

    @Suppress("MagicNumber")
    private val pendingIntentFlags: Int = when {
        Build.VERSION.SDK_INT >= 34 -> {
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_MUTABLE or
                PendingIntent.FLAG_ALLOW_UNSAFE_IMPLICIT_INTENT
        }

        else -> {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        }
    }
    // endregion

    private fun loadIcon(appWidgetId: Int, iconUrl: String, remoteViews: RemoteViews) {
        val iconTarget = object : AppWidgetTarget(context, remoteViews, R.id.icon, appWidgetId) {
            override fun onResourceReady(resource: Bitmap?, glideAnimation: GlideAnimation<in Bitmap>?) {
                if (resource != null) {
                    val tintedBitmap = BitmapUtils.tintImage(resource, R.color.black)
                    super.onResourceReady(tintedBitmap, glideAnimation)
                }
            }
        }

        Glide.with(context)
            .using(
                CustomGlideUriLoader(accountProvider.user, clientFactory),
                InputStream::class.java
            )
            .from(Uri::class.java)
            .`as`(SVGorImage::class.java)
            .transcode(SvgOrImageBitmapTranscoder(SVG_SIZE, SVG_SIZE), Bitmap::class.java)
            .sourceEncoder(StreamEncoder())
            .cacheDecoder(FileToStreamDecoder(SvgOrImageDecoder()))
            .decoder(SvgOrImageDecoder())
            .diskCacheStrategy(DiskCacheStrategy.SOURCE)
            .load(Uri.parse(iconUrl))
            .into(iconTarget)
    }
}
