/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.graphics.drawable.PictureDrawable
import android.net.Uri
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.BitmapImageViewTarget
import com.bumptech.glide.request.target.Target
import com.nextcloud.common.NextcloudClient
import com.nextcloud.utils.LinkHelper.validateAndGetURI
import com.nextcloud.utils.LinkHelper.validateAndGetURL
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.utils.svg.SvgSoftwareLayerSetter

/**
 * Utility object for loading images (including SVGs) using Glide.
 *
 * Provides methods for loading images into `ImageView`, `Target<Drawable>`, `Target<Bitmap>` ...
 * from both URLs and URIs.
 */
@Suppress("TooManyFunctions")
object GlideHelper {
    private const val TAG = "GlideHelper"

    private class GlideLogger<T>(
        private val methodName: String,
        private val identifier: String
    ) : RequestListener<T> {
        override fun onLoadFailed(p0: GlideException?, p1: Any?, p2: Target<T>, p3: Boolean): Boolean {
            Log_OC.e(TAG, "$methodName: Load failed for $identifier")
            Log_OC.e(TAG, "$methodName: Error: ${p0?.message}")
            p0?.logRootCauses(TAG)
            return false
        }

        override fun onResourceReady(p0: T & Any, p1: Any, p2: Target<T?>?, p3: DataSource, p4: Boolean): Boolean {
            Log_OC.i(TAG, "Glide load completed: $p0")
            return false
        }
    }

    private fun isSVG(url: String): Boolean = (url.toUri().encodedPath?.endsWith(".svg") == true)

    private fun createGlideUrl(url: String, client: NextcloudClient) = GlideUrl(
        url,
        LazyHeaders.Builder()
            .addHeader("Authorization", client.credentials)
            .addHeader("User-Agent", "Mozilla/5.0 (Android) Nextcloud-android")
            .build()
    )

    private fun <T> RequestBuilder<T>.withLogging(methodName: String, identifier: String): RequestBuilder<T> =
        listener(GlideLogger(methodName, identifier))

    private fun createSvgRequestBuilder(
        context: Context,
        uri: Uri,
        placeholder: Int? = null
    ): RequestBuilder<PictureDrawable> = Glide.with(context)
        .`as`(PictureDrawable::class.java)
        .load(uri)
        .apply {
            placeholder?.let { placeholder(it) }
            placeholder?.let { error(it) }
        }
        .listener(SvgSoftwareLayerSetter())

    private fun createUrlRequestBuilder(
        context: Context,
        client: NextcloudClient,
        url: String
    ): RequestBuilder<Drawable> {
        val glideUrl = createGlideUrl(url, client)
        return Glide.with(context)
            .load(glideUrl)
            .centerCrop()
    }

    fun loadIntoImageView(
        context: Context,
        client: NextcloudClient,
        url: String?,
        imageView: ImageView,
        @DrawableRes placeholderRes: Int,
        circleCrop: Boolean = false
    ) {
        val validatedUrl = validateAndGetURL(url) ?: return

        if (isSVG(validatedUrl)) {
            val uri = validateAndGetURI(validatedUrl) ?: return
            val requestBuilder = createSvgRequestBuilder(context, uri, placeholderRes)
                .withLogging("loadIntoImageView", validatedUrl)
            if (circleCrop) {
                requestBuilder.circleCrop().into(imageView)
            } else {
                requestBuilder.into(imageView)
            }
        } else {
            val requestBuilder = createUrlRequestBuilder(context, client, validatedUrl)
                .placeholder(placeholderRes)
                .error(placeholderRes)
                .withLogging("loadIntoImageView", validatedUrl)
            if (circleCrop) {
                requestBuilder.circleCrop().into(imageView)
            } else {
                requestBuilder.into(imageView)
            }
        }
    }

    @Suppress("TooGenericExceptionCaught", "ReturnCount")
    fun getDrawable(context: Context, client: NextcloudClient, urlString: String?): Drawable? {
        val validatedUrl = validateAndGetURL(urlString) ?: return null

        return try {
            if (isSVG(validatedUrl)) {
                val uri = validateAndGetURI(validatedUrl) ?: return null
                createSvgRequestBuilder(context, uri)
                    .withLogging("getDrawable", validatedUrl)
                    .submit()
                    .get()
            } else {
                createUrlRequestBuilder(context, client, validatedUrl)
                    .withLogging("getDrawable", validatedUrl)
                    .submit()
                    .get()
            }
        } catch (e: Exception) {
            Log_OC.e(TAG, "Error getting drawable: $e")
            null
        }
    }

    @Suppress("TooGenericExceptionCaught")
    fun downloadImageSynchronous(context: Context, url: String?): Bitmap? {
        val validatedUrl = validateAndGetURL(url) ?: return null

        return try {
            Glide.with(context)
                .asBitmap()
                .load(validatedUrl)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .withLogging("downloadImageSynchronous", validatedUrl)
                .submit(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                .get()
        } catch (e: Exception) {
            Log_OC.e(TAG, "Could not download image $e")
            null
        }
    }

    fun loadCircularBitmapIntoImageView(context: Context, url: String?, imageView: ImageView, placeholder: Drawable) {
        val validatedUrl = validateAndGetURL(url) ?: return

        Glide.with(context)
            .asBitmap()
            .load(validatedUrl)
            .placeholder(placeholder)
            .error(placeholder)
            .withLogging("loadCircularBitmapIntoImageView", validatedUrl)
            .into(object : BitmapImageViewTarget(imageView) {
                override fun setResource(resource: Bitmap?) {
                    val circularBitmapDrawable = RoundedBitmapDrawableFactory.create(context.resources, resource)
                    circularBitmapDrawable.isCircular = true
                    imageView.setImageDrawable(circularBitmapDrawable)
                }
            })
    }

    @Suppress("UNCHECKED_CAST", "TooGenericExceptionCaught")
    fun <T> loadIntoTarget(
        context: Context,
        client: NextcloudClient,
        url: String,
        target: Target<T>,
        @DrawableRes placeholder: Int
    ) {
        try {
            val validatedUrl = validateAndGetURL(url) ?: return

            if (isSVG(validatedUrl)) {
                val uri = validateAndGetURI(validatedUrl) ?: return
                val requestBuilder = createSvgRequestBuilder(context, uri, placeholder)
                    .withLogging("loadIntoTarget", validatedUrl) as RequestBuilder<T>
                requestBuilder.into(target)
            } else {
                val requestBuilder = createUrlRequestBuilder(context, client, validatedUrl)
                    .placeholder(placeholder)
                    .error(placeholder)
                    .withLogging("loadIntoTarget", validatedUrl) as RequestBuilder<T>
                requestBuilder.into(target)
            }
        } catch (e: Exception) {
            Log_OC.e(TAG, "Failed to load into target: $e")
        }
    }
}
