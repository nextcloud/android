/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.avatar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.media.ThumbnailUtils
import android.net.Uri
import androidx.core.content.res.ResourcesCompat
import com.nextcloud.client.core.Clock
import com.owncloud.android.R
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.TextDrawable
import com.owncloud.android.utils.BitmapUtils
import org.apache.commons.httpclient.HttpStatus
import org.apache.commons.httpclient.methods.GetMethod
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours

@Suppress("TooGenericExceptionCaught")
class AvatarLoader @Inject constructor(
    private val context: Context,
    private val arbitraryDataProvider: ArbitraryDataProvider,
    private val clock: Clock
) {

    companion object {
        private const val TAG = "AvatarLoader"
        private const val AVATAR_TIMESTAMP = "avatarTimestamp"
        private const val ETAG_HEADER = "ETag"
        private const val IF_NONE_MATCH_HEADER = "If-None-Match"
        private val REFRESH_INTERVAL_MILLIS = 1.hours.inWholeMilliseconds
    }

    private val resources get() = context.resources

    fun loadCached(request: AvatarRequest): Drawable? {
        val eTag = arbitraryDataProvider.getValue(request.accountName, ThumbnailsCacheManager.AVATAR)
        val bitmap = ThumbnailsCacheManager.getBitmapFromDiskCache(request.cacheKey(eTag)) ?: return null
        return BitmapUtils.bitmapToCircularBitmapDrawable(resources, bitmap)
    }

    fun load(request: AvatarRequest): Drawable? = try {
        loadOrDownload(request)
    } catch (_: OutOfMemoryError) {
        Log_OC.e(TAG, "Out of memory")
        null
    } catch (t: Throwable) {
        Log_OC.e(TAG, "Generation of avatar for ${request.userId} failed", t)
        ResourcesCompat.getDrawable(resources, R.drawable.account_circle_white, null)
    }

    private fun loadOrDownload(request: AvatarRequest): Drawable? {
        val eTag = arbitraryDataProvider.getValue(request.accountName, ThumbnailsCacheManager.AVATAR)
        val cached = ThumbnailsCacheManager.getBitmapFromDiskCache(request.cacheKey(eTag))

        if (cached != null && !isRefreshDue(request)) {
            return toDrawable(request, cached)
        }

        return try {
            download(request, eTag, cached)
        } catch (e: Exception) {
            Log_OC.e(TAG, "Downloading avatar for ${request.userId} failed", e)
            createAccountAvatar(request, cached)
        }
    }

    private fun isRefreshDue(request: AvatarRequest): Boolean {
        val timestamp = arbitraryDataProvider.getLongValue(request.accountName, AVATAR_TIMESTAMP)
        return clock.currentTime - timestamp >= REFRESH_INTERVAL_MILLIS
    }

    private fun download(request: AvatarRequest, eTag: String, cached: Bitmap?): Drawable? {
        val client = OwnCloudClientManagerFactory.getDefaultSingleton()
            .getClientFor(request.user.toOwnCloudAccount(), context)
        ThumbnailsCacheManager.setClient(client)

        val px = resources.getInteger(R.integer.file_avatar_px)
        val get = GetMethod("${client.baseUri}/index.php/avatar/${Uri.encode(request.userId)}/$px")

        try {
            if (eTag.isNotEmpty() && cached != null) {
                get.setRequestHeader(IF_NONE_MATCH_HEADER, eTag)
            }

            return when (client.executeMethod(get)) {
                HttpStatus.SC_OK, HttpStatus.SC_CREATED -> readDownloadedAvatar(request, get, px)

                HttpStatus.SC_NOT_MODIFIED -> {
                    client.exhaustResponse(get.responseBodyAsStream)
                    storeRefreshTimestamp(request)
                    toDrawable(request, cached)
                }

                else -> {
                    client.exhaustResponse(get.responseBodyAsStream)
                    toDrawable(request, cached)
                }
            }
        } finally {
            get.releaseConnection()
        }
    }

    private fun readDownloadedAvatar(request: AvatarRequest, get: GetMethod, px: Int): Drawable? {
        val newETag = get.getResponseHeader(ETAG_HEADER)?.value?.replace("\"", "")
        newETag?.let {
            arbitraryDataProvider.storeOrUpdateKeyValue(request.accountName, ThumbnailsCacheManager.AVATAR, it)
        }

        val bitmap = BitmapFactory.decodeStream(get.responseBodyAsStream)
        val thumbnail = ThumbnailUtils.extractThumbnail(bitmap, px, px)

        if (thumbnail == null || newETag.isNullOrEmpty()) {
            return TextDrawable.createAvatar(request.user, request.avatarRadius)
        }

        val avatar = ThumbnailsCacheManager.handlePNG(thumbnail, px, px)
        ThumbnailsCacheManager.addBitmapToCache(request.cacheKey(newETag), avatar)
        storeRefreshTimestamp(request)
        return toDrawable(request, avatar)
    }

    private fun storeRefreshTimestamp(request: AvatarRequest) {
        arbitraryDataProvider.storeOrUpdateKeyValue(request.accountName, AVATAR_TIMESTAMP, clock.currentTime)
    }

    private fun createAccountAvatar(request: AvatarRequest, cached: Bitmap?): Drawable? = try {
        TextDrawable.createAvatar(request.user, request.avatarRadius)
    } catch (e: Exception) {
        Log_OC.e(TAG, "Error generating fallback avatar", e)
        toDrawable(request, cached)
    }

    private fun toDrawable(request: AvatarRequest, bitmap: Bitmap?): Drawable? =
        BitmapUtils.bitmapToCircularBitmapDrawable(resources, bitmap) ?: createInitialAvatar(request)

    private fun createInitialAvatar(request: AvatarRequest): Drawable? = try {
        TextDrawable.createAvatarByUserId(request.displayName, request.avatarRadius)
    } catch (_: Exception) {
        ResourcesCompat.getDrawable(resources, R.drawable.ic_user_outline, null)
    }
}
