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
import androidx.palette.graphics.Palette
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nextcloud.client.account.UserAccountManager
import com.owncloud.android.R
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.activity.FileDisplayActivity
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Background worker that fetches a random photo and updates all photo widgets.
 *
 * Constructed by [com.nextcloud.client.jobs.BackgroundJobFactory].
 */
@Suppress("TooManyFunctions")
class PhotoWidgetWorker(
    private val context: Context,
    params: WorkerParameters,
    private val photoWidgetRepository: PhotoWidgetRepository,
    private val userAccountManager: UserAccountManager
) : CoroutineWorker(context, params) {

    companion object {
        const val TAG = "PhotoWidgetWorker"
        private const val NEXT_BUTTON_REQUEST_CODE_OFFSET = 10000
        private const val BRIGHTNESS_THRESHOLD = 128
    }

    override suspend fun doWork(): Result {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, PhotoWidgetProvider::class.java)
        val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
        val isImmediate = tags.contains("immediate_photo_widget")

        for (widgetId in widgetIds) {
            // Get config for this widget
            val config = photoWidgetRepository.getWidgetConfig(widgetId) ?: continue
            
            // Determine if we should update based on interval
            val shouldUpdate = if (isImmediate) {
                true
            } else {
                val intervalMinutes = config.intervalMinutes
                if (intervalMinutes <= 0) {
                    false // Manual mode, don't auto-update
                } else {
                    val lastUpdate = photoWidgetRepository.getWidgetLastUpdateTimestamp(widgetId)
                    val intervalMillis = intervalMinutes * 60 * 1000
                    (System.currentTimeMillis() - lastUpdate) >= intervalMillis
                }
            }

            if (shouldUpdate) {
                updateWidget(appWidgetManager, widgetId)
                // Save timestamp
                photoWidgetRepository.setWidgetLastUpdateTimestamp(widgetId, System.currentTimeMillis())
            }
        }

        return Result.success()
    }

    @Suppress("LongMethod")
    private suspend fun updateWidget(appWidgetManager: AppWidgetManager, widgetId: Int) {
        val remoteViews = RemoteViews(context.packageName, R.layout.widget_photo)

        val imageResult = photoWidgetRepository.getRandomImageResult(widgetId)
        if (imageResult != null) {
            // Show photo, hide empty state
            remoteViews.setViewVisibility(R.id.photo_widget_image, android.view.View.VISIBLE)
            remoteViews.setViewVisibility(R.id.photo_widget_empty_state, android.view.View.GONE)
            remoteViews.setImageViewBitmap(R.id.photo_widget_image, imageResult.bitmap)

            // Show gradient scrim and text container
            remoteViews.setViewVisibility(R.id.photo_widget_scrim, android.view.View.VISIBLE)
            remoteViews.setViewVisibility(R.id.photo_widget_text_container, android.view.View.VISIBLE)

            // Location line (only if geolocation is available)
            val locationText = resolveLocationName(imageResult.latitude, imageResult.longitude)
            if (locationText != null) {
                remoteViews.setTextViewText(R.id.photo_widget_location, locationText)
                remoteViews.setViewVisibility(R.id.photo_widget_location, android.view.View.VISIBLE)
            } else {
                remoteViews.setViewVisibility(R.id.photo_widget_location, android.view.View.GONE)
                // Hide scrim and text container if there's no text to show
                remoteViews.setViewVisibility(R.id.photo_widget_scrim, android.view.View.GONE)
                remoteViews.setViewVisibility(R.id.photo_widget_text_container, android.view.View.GONE)
            }

            // Adaptive button tint: light icon on dark images, dark icon on light images
            applyAdaptiveButtonTint(remoteViews, imageResult)
        } else {
            // Show empty state, hide photo
            remoteViews.setViewVisibility(R.id.photo_widget_image, android.view.View.GONE)
            remoteViews.setViewVisibility(R.id.photo_widget_empty_state, android.view.View.VISIBLE)
            remoteViews.setViewVisibility(R.id.photo_widget_scrim, android.view.View.GONE)
            remoteViews.setViewVisibility(R.id.photo_widget_text_container, android.view.View.GONE)

            // Apply static tints programmatically (XML android:tint crashes on some RemoteViews)
            remoteViews.setInt(R.id.photo_widget_empty_icon, "setColorFilter", android.graphics.Color.parseColor("#666680"))
            remoteViews.setInt(R.id.photo_widget_retry_button, "setColorFilter", android.graphics.Color.parseColor("#AAAACC"))
        }

        // Set click on photo to open the specific file in FileDisplayActivity
        val config = photoWidgetRepository.getWidgetConfig(widgetId)
        val clickIntent = createOpenFileIntent(config, imageResult)
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

        // Wire retry button in empty state to same refresh action
        remoteViews.setOnClickPendingIntent(R.id.photo_widget_retry_button, nextPendingIntent)

        appWidgetManager.updateAppWidget(widgetId, remoteViews)
    }

    /**
     * Applies adaptive tint to the refresh button based on image brightness.
     * Uses [Palette] to detect the dominant color in the top-right corner
     * (where the button sits) and sets the button tint accordingly.
     */
    @Suppress("MagicNumber")
    private fun applyAdaptiveButtonTint(remoteViews: RemoteViews, imageResult: PhotoWidgetImageResult) {
        try {
            val bitmap = imageResult.bitmap
            // Sample the top-right quadrant where the button lives
            val sampleWidth = bitmap.width / 4
            val sampleHeight = bitmap.height / 4
            if (sampleWidth <= 0 || sampleHeight <= 0) return

            val cornerBitmap = android.graphics.Bitmap.createBitmap(
                bitmap,
                bitmap.width - sampleWidth,
                0,
                sampleWidth,
                sampleHeight
            )

            val palette = Palette.from(cornerBitmap).generate()
            val dominantSwatch = palette.dominantSwatch

            if (dominantSwatch != null) {
                val r = android.graphics.Color.red(dominantSwatch.rgb)
                val g = android.graphics.Color.green(dominantSwatch.rgb)
                val b = android.graphics.Color.blue(dominantSwatch.rgb)
                // Perceived brightness formula
                val brightness = (r * 0.299 + g * 0.587 + b * 0.114).toInt()

                val tintColor = if (brightness > BRIGHTNESS_THRESHOLD) {
                    // Light background → dark button
                    android.graphics.Color.parseColor("#CC333333")
                } else {
                    // Dark background → light button
                    android.graphics.Color.WHITE
                }
                remoteViews.setInt(R.id.photo_widget_next_button, "setColorFilter", tintColor)
            }

            if (cornerBitmap != bitmap) {
                cornerBitmap.recycle()
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Log_OC.d(TAG, "Could not apply adaptive tint: ${e.message}")
        }
    }

    /**
     * Reverse-geocodes lat/long into a human-readable location name.
     * Returns null if geocoding is unavailable or coordinates are missing.
     */
    @Suppress("DEPRECATION", "TooGenericExceptionCaught", "ReturnCount", "CyclomaticComplexMethod")
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

    /**
     * Creates an intent to open the specific file in FileDisplayActivity.
     * Falls back to opening the folder if file info is not available.
     */
    private fun createOpenFileIntent(config: PhotoWidgetConfig?, imageResult: PhotoWidgetImageResult?): Intent {
        val intent = Intent(context, FileDisplayActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (config != null) {
            intent.putExtra("folderPath", config.folderPath)
            intent.putExtra("accountName", config.accountName)
        }
        // If we have the file ID, pass it so the activity opens the specific file
        if (imageResult?.fileId != null) {
            intent.putExtra("fileId", imageResult.fileId)
        }
        if (imageResult?.fileRemotePath != null) {
            intent.putExtra("filePath", imageResult.fileRemotePath)
        }
        return intent
    }
}
