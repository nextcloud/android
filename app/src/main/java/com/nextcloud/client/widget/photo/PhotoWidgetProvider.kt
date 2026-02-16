/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Axel
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.widget.photo

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import com.nextcloud.client.jobs.BackgroundJobManager
import dagger.android.AndroidInjection
import javax.inject.Inject

/**
 * App widget provider for the Photo Widget.
 *
 * Delegates heavy work to [PhotoWidgetWorker] via [BackgroundJobManager].
 */
class PhotoWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_NEXT_IMAGE = "com.nextcloud.client.widget.photo.ACTION_NEXT_IMAGE"
    }

    @Inject
    lateinit var backgroundJobManager: BackgroundJobManager

    @Inject
    lateinit var photoWidgetRepository: PhotoWidgetRepository

    override fun onReceive(context: Context, intent: Intent?) {
        AndroidInjection.inject(this, context)

        // Handle "next image" button tap
        if (intent?.action == ACTION_NEXT_IMAGE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = android.content.ComponentName(context, PhotoWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            for (widgetId in appWidgetIds) {
                // Show loading state immediately
                val remoteViews = android.widget.RemoteViews(context.packageName, com.owncloud.android.R.layout.widget_photo)
                remoteViews.setViewVisibility(com.owncloud.android.R.id.photo_widget_loading, android.view.View.VISIBLE)
                remoteViews.setViewVisibility(com.owncloud.android.R.id.photo_widget_next_button, android.view.View.INVISIBLE)
                appWidgetManager.partiallyUpdateAppWidget(widgetId, remoteViews)
            }

            backgroundJobManager.startImmediatePhotoWidgetUpdate()
            return
        }

        super.onReceive(context, intent)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        AndroidInjection.inject(this, context)
        backgroundJobManager.startImmediatePhotoWidgetUpdate()
    }

    override fun onEnabled(context: Context) {
        AndroidInjection.inject(this, context)
        super.onEnabled(context)
        backgroundJobManager.schedulePeriodicPhotoWidgetUpdate()
    }

    override fun onDisabled(context: Context) {
        AndroidInjection.inject(this, context)
        super.onDisabled(context)
        backgroundJobManager.cancelPeriodicPhotoWidgetUpdate()
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        AndroidInjection.inject(this, context)
        super.onDeleted(context, appWidgetIds)
        for (widgetId in appWidgetIds) {
            photoWidgetRepository.deleteWidgetConfig(widgetId)
        }
    }
}
