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
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.activity.FileDisplayActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    private suspend fun updateWidget(appWidgetManager: AppWidgetManager, widgetId: Int) {
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
        // Hide loading and show next button
        remoteViews.setViewVisibility(R.id.photo_widget_loading, android.view.View.GONE)
        remoteViews.setViewVisibility(R.id.photo_widget_next_button, android.view.View.VISIBLE)
        remoteViews.setOnClickPendingIntent(R.id.photo_widget_next_button, nextPendingIntent)

        appWidgetManager.updateAppWidget(widgetId, remoteViews)
    }

    /**
     * Reverse-geocodes lat/long into a human-readable location name.
     * Returns null if geocoding is unavailable or coordinates are missing.
     */
    @Suppress("DEPRECATION")
    private suspend fun resolveLocationName(latitude: Double?, longitude: Double?): String? = withContext(Dispatchers.IO) {
        if (latitude == null || longitude == null) {
            Log_OC.d(TAG, "Location resolution skipped: latitude=$latitude, longitude=$longitude")
            return@withContext null
        }
        if (latitude == 0.0 && longitude == 0.0) {
            Log_OC.d(TAG, "Location resolution skipped: coordinates are 0.0, 0.0")
            return@withContext null
        }

        try {
            if (!Geocoder.isPresent()) {
                Log_OC.e(TAG, "Location resolution failed: Geocoder not present")
                return@withContext null
            }
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                // Build a concise location: "City, Country" or just "Country"
                val city = address.locality ?: address.subAdminArea
                val country = address.countryName
                Log_OC.d(TAG, "Location resolved: city=$city, country=$country")
                when {
                    city != null && country != null -> "$city, $country"
                    city != null -> city
                    country != null -> country
                    else -> null
                }
            } else {
                Log_OC.d(TAG, "Location resolution: No address found for $latitude, $longitude")
                null
            }
        } catch (e: Exception) {
            Log_OC.e(TAG, "Location resolution failed", e)
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
        if (config != null) {
            intent.putExtra("folderPath", config.folderPath)
            intent.putExtra("accountName", config.accountName)
        }
        return intent
    }
}
