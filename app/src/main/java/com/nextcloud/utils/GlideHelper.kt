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
import com.bumptech.glide.request.FutureTarget
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.BitmapImageViewTarget
import com.bumptech.glide.request.target.Target
import com.nextcloud.android.lib.resources.dashboard.DashboardWidgetItem
import com.nextcloud.common.NextcloudClient
import com.nextcloud.utils.LinkHelper.validateAndGetURI
import com.nextcloud.utils.LinkHelper.validateAndGetURL
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.utils.DisplayUtils.SVG_SIZE
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

    @Suppress("UNCHECKED_CAST", "TooGenericExceptionCaught")
    fun <T> loadIntoTarget(context: Context, iconUrl: String, target: Target<T>, placeholder: Int) {
        try {
            if (isSVG(iconUrl)) {
                loadViaURISVGIntoPictureDrawableTarget(
                    context,
                    iconUrl,
                    target as Target<PictureDrawable?>,
                    placeholder
                )
            } else {
                loadViaURLIntoDrawableTarget(context, iconUrl, target as Target<Drawable>, placeholder)
            }
        } catch (e: Exception) {
            Log_OC.e(TAG, "not setting image as activity is destroyed $e")
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun createSvgRequestBuilder(
        context: Context,
        uri: Uri,
        @DrawableRes placeholder: Int
    ): RequestBuilder<PictureDrawable>? {
        Log_OC.d(TAG, "createSvgRequestBuilder: Starting with URI: $uri, placeholder: $placeholder")

        return try {
            Glide
                .with(context)
                .`as`(PictureDrawable::class.java)
                .load(uri)
                .placeholder(placeholder)
                .error(placeholder)
                .listener(SvgSoftwareLayerSetter())
                .listener(GlideLogger(methodName = "createSvgRequestBuilder", identifier = uri.toString()))
        } catch (e: Exception) {
            Log_OC.e(TAG, "Failed to create SVG request builder for URI: $uri -- $e")
            null
        }
    }

    fun loadViaURISVGIntoImageView(
        context: Context,
        uriString: String?,
        imageView: ImageView,
        @DrawableRes placeholder: Int
    ) {
        val uri = validateAndGetURI(uriString) ?: return
        val svgRequestBuilder = createSvgRequestBuilder(context, uri, placeholder)
        svgRequestBuilder?.into(imageView)
    }

    fun loadViaURISVGIntoPictureDrawableTarget(
        context: Context,
        uriString: String?,
        target: Target<PictureDrawable?>,
        placeholder: Int,
        size: Int? = null
    ) {
        val uri = validateAndGetURI(uriString) ?: return
        val svgRequestBuilder = createSvgRequestBuilder(context, uri, placeholder)

        if (size != null) {
            svgRequestBuilder?.override(size)?.into(target)
        } else {
            svgRequestBuilder?.into(target)
        }
    }

    fun createPictureDrawable(context: Context, uriString: String?): PictureDrawable? {
        val uri = validateAndGetURI(uriString) ?: return null

        return Glide
            .with(context)
            .`as`(PictureDrawable::class.java)
            .load(uri)
            .override(SVG_SIZE, SVG_SIZE)
            .listener(GlideLogger(methodName = "createPictureDrawable", identifier = uri.toString()))
            .submit()
            .get()
    }

    fun loadViaURLIntoDrawableTarget(
        context: Context,
        url: String,
        target: Target<Drawable>,
        @DrawableRes placeholder: Int
    ) {
        val validatedURL = validateAndGetURL(url) ?: return

        Glide
            .with(context)
            .load(validatedURL)
            .centerCrop()
            .placeholder(placeholder)
            .listener(GlideLogger(methodName = "loadViaURLIntoDrawableTarget", identifier = validatedURL))
            .error(placeholder)
            .into(target)
    }

    fun loadViaURLIntoImageView(context: Context, url: String, imageView: ImageView, placeholder: Drawable) {
        val validatedURL = validateAndGetURL(url) ?: return

        Glide
            .with(context)
            .load(validatedURL)
            .placeholder(placeholder)
            .error(placeholder)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .listener(GlideLogger(methodName = "loadViaURLIntoImageView", identifier = validatedURL))
            .into(imageView)
    }

    fun loadViaURLIntoImageView(context: Context, url: String, imageView: ImageView, @DrawableRes placeholder: Int) {
        val validatedURL = validateAndGetURL(url) ?: return

        Glide
            .with(context)
            .load(validatedURL)
            .placeholder(placeholder)
            .error(placeholder)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .listener(GlideLogger(methodName = "loadViaURLIntoImageView", identifier = validatedURL))
            .into(imageView)
    }

    @Suppress("TooGenericExceptionCaught", "ReturnCount")
    fun downloadImageSynchronous(context: Context, url: String?): Bitmap? {
        val validatedURL = validateAndGetURL(url) ?: return null

        try {
            return Glide
                .with(context)
                .asBitmap()
                .load(validatedURL)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .listener(GlideLogger(methodName = "downloadImageSynchronous", identifier = validatedURL))
                .submit(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                .get()
        } catch (e: Exception) {
            Log_OC.e(TAG, "Could not download image $e")
            return null
        }
    }

    fun loadViaURLIntoBitmapImageViewTarget(
        context: Context,
        url: String,
        imageView: ImageView,
        placeholder: Drawable
    ) {
        val validatedURL = validateAndGetURL(url) ?: return

        Glide.with(context)
            .asBitmap()
            .load(validatedURL)
            .placeholder(placeholder)
            .error(placeholder)
            .listener(GlideLogger(methodName = "loadViaURLIntoBitmapImageViewTarget", identifier = validatedURL))
            .into(object : BitmapImageViewTarget(imageView) {
                override fun setResource(resource: Bitmap?) {
                    val circularBitmapDrawable = RoundedBitmapDrawableFactory.create(
                        context.resources,
                        resource
                    )
                    circularBitmapDrawable.isCircular = true
                    imageView.setImageDrawable(circularBitmapDrawable)
                }
            })
    }

    fun getDrawable(context: Context, client: NextcloudClient, uriString: String): Drawable? {
        val requestBuilder = createRequestBuilder(context, client, uriString) ?: return null
        return requestBuilder.submit().get()
    }

    fun loadIntoImageView(
        context: Context,
        client: NextcloudClient,
        url: String?,
        imageView: ImageView,
        placeholder: Drawable,
        circleCrop: Boolean = false
    ) {
        val requestBuilder = createRequestBuilder(context, client, url) ?: return

        val finalRequest = requestBuilder
            .placeholder(placeholder)
            .error(placeholder)

        if (circleCrop) {
            finalRequest.circleCrop().into(imageView)
        } else {
            finalRequest.into(imageView)
        }
    }

    private fun createRequestBuilder(
        context: Context,
        client: NextcloudClient,
        urlString: String?
    ): RequestBuilder<out Drawable>? {
        val validatedURL = validateAndGetURL(urlString) ?: return null

        return if (isSVG(validatedURL)) {
            val uri = validateAndGetURI(validatedURL) ?: return null

            Glide.with(context)
                .`as`(PictureDrawable::class.java)
                .load(uri)
                .listener(SvgSoftwareLayerSetter())
        } else {
            val glideUrl = createGlideUrl(validatedURL, client)

            Glide.with(context)
                .load(glideUrl)
                .listener(GlideLogger(methodName = "loadViaURLIntoImageView", identifier = validatedURL))
        }
    }

    private fun createGlideUrl(url: String, client: NextcloudClient): GlideUrl {
        return GlideUrl(
            url,
            LazyHeaders.Builder()
                .addHeader("Authorization", client.credentials)
                .addHeader("User-Agent", "Mozilla/5.0 (Android) Nextcloud-android")
                .build()
        )
    }

    fun createBitmapFromDashboardWidgetItem(context: Context, widgetItem: DashboardWidgetItem): FutureTarget<Bitmap> {
        return Glide
            .with(context)
            .asBitmap()
            .load(widgetItem.iconUrl)
            .override(SVG_SIZE, SVG_SIZE)
            .listener(GlideLogger(methodName = "createBitmapFromDashboardWidgetItem", identifier = widgetItem.iconUrl))
            .submit()
    }
}
