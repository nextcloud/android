/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Axel
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.widget.photo

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.widget.RemoteViews
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nextcloud.client.account.UserAccountManager
import com.owncloud.android.R
import com.owncloud.android.ui.activity.FileDisplayActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Background worker that fetches a random photo and updates all photo widgets.
 *
 * Constructed by [com.nextcloud.client.jobs.BackgroundJobFactory].
 */
class PhotoWidgetWorker(
    private val context: Context,
    params: WorkerParameters,
    private val photoWidgetRepository: PhotoWidgetRepository,
    private val userAccountManager: UserAccountManager
) : CoroutineWorker(context, params) {

    companion object {
        const val TAG = "PhotoWidgetWorker"
        private const val DATE_FORMAT = "dd MMM yyyy"
        private const val NEXT_BUTTON_REQUEST_CODE_OFFSET = 10000
    }

    override suspend fun doWork(): Result {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, PhotoWidgetProvider::class.java)
        val widgetIds = appWidgetManager.getAppWidgetIds(componentName)

        for (widgetId in widgetIds) {
            updateWidget(appWidgetManager, widgetId)
        }

        return Result.success()
    }

    private fun updateWidget(appWidgetManager: AppWidgetManager, widgetId: Int) {
        val remoteViews = RemoteViews(context.packageName, R.layout.widget_photo)

        val imageResult = photoWidgetRepository.getRandomImageResult(widgetId)
        if (imageResult != null) {
            remoteViews.setImageViewBitmap(R.id.photo_widget_image, imageResult.bitmap)

            // Show the text container
            remoteViews.setViewVisibility(R.id.photo_widget_text_container, android.view.View.VISIBLE)

            // Location line (only if geolocation is available)
            val locationText = resolveLocationName(imageResult.latitude, imageResult.longitude)
            if (locationText != null) {
                remoteViews.setTextViewText(R.id.photo_widget_location, locationText)
                remoteViews.setViewVisibility(R.id.photo_widget_location, android.view.View.VISIBLE)
            } else {
                remoteViews.setViewVisibility(R.id.photo_widget_location, android.view.View.GONE)
            }

            // Date line
            val dateText = formatDate(imageResult.modificationTimestamp)
            remoteViews.setTextViewText(R.id.photo_widget_date, dateText)
        } else {
            remoteViews.setImageViewResource(R.id.photo_widget_image, R.drawable.ic_image_outline)
            remoteViews.setViewVisibility(R.id.photo_widget_text_container, android.view.View.GONE)
        }

        // Set click on photo to open the folder in FileDisplayActivity
        val config = photoWidgetRepository.getWidgetConfig(widgetId)
        val clickIntent = createOpenFolderIntent(config)
        val openPendingIntent = PendingIntent.getActivity(
            context,
            widgetId,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        remoteViews.setOnClickPendingIntent(R.id.photo_widget_image, openPendingIntent)

        // Set click on "next" button to refresh with a new random image
        val nextIntent = Intent(context, PhotoWidgetProvider::class.java).apply {
            action = PhotoWidgetProvider.ACTION_NEXT_IMAGE
        }
        val nextPendingIntent = PendingIntent.getBroadcast(
            context,
            widgetId + NEXT_BUTTON_REQUEST_CODE_OFFSET,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        remoteViews.setOnClickPendingIntent(R.id.photo_widget_next_button, nextPendingIntent)

        appWidgetManager.updateAppWidget(widgetId, remoteViews)
    }

    /**
     * Reverse-geocodes lat/long into a human-readable location name.
     * Returns null if geocoding is unavailable or coordinates are missing.
     */
    @Suppress("DEPRECATION")
    private fun resolveLocationName(latitude: Double?, longitude: Double?): String? {
        if (latitude == null || longitude == null) return null
        if (latitude == 0.0 && longitude == 0.0) return null

        return try {
            if (!Geocoder.isPresent()) return null
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                // Build a concise location: "City, Country" or just "Country"
                val city = address.locality ?: address.subAdminArea
                val country = address.countryName
                when {
                    city != null && country != null -> "$city, $country"
                    city != null -> city
                    country != null -> country
                    else -> null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun formatDate(timestampMillis: Long): String {
        val sdf = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
        return sdf.format(Date(timestampMillis))
    }

    private fun createOpenFolderIntent(config: PhotoWidgetConfig?): Intent {
        val intent = Intent(context, FileDisplayActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return intent
    }
}
