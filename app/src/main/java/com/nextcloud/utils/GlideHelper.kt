/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.graphics.drawable.PictureDrawable
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

    @SuppressLint("CheckResult")
    private fun createSvgRequestBuilder(
        context: Context,
        uri: String,
        client: NextcloudClient,
        placeholder: Int? = null
    ): RequestBuilder<PictureDrawable> {
        val glideUrl = createGlideUrl(uri, client)

        return Glide.with(context)
            .`as`(PictureDrawable::class.java)
            .load(glideUrl)
            .apply {
                placeholder?.let { placeholder(it) }
                placeholder?.let { error(it) }
            }
            .listener(SvgSoftwareLayerSetter())
    }

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

    @Suppress("TooGenericExceptionCaught")
    fun getBitmap(context: Context, url: String?): Bitmap? {
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

    @Suppress("UNCHECKED_CAST", "TooGenericExceptionCaught", "ReturnCount")
    private fun <T> createRequestBuilder(context: Context, client: NextcloudClient, url: String?): RequestBuilder<T>? {
        val validatedUrl = validateAndGetURL(url) ?: return null

        return try {
            val isSVG = isSVG(validatedUrl)

            return if (isSVG) {
                createSvgRequestBuilder(context, validatedUrl, client)
            } else {
                createUrlRequestBuilder(context, client, validatedUrl)
            }
                .withLogging("createRequestBuilder", validatedUrl) as RequestBuilder<T>?
        } catch (e: Exception) {
            Log_OC.e(TAG, "Error createRequestBuilder: $e")
            null
        }
    }

    @SuppressLint("CheckResult")
    fun loadIntoImageView(
        context: Context,
        client: NextcloudClient,
        url: String?,
        imageView: ImageView,
        @DrawableRes placeholder: Int,
        circleCrop: Boolean = false
    ) {
        createRequestBuilder<Drawable>(context, client, url)
            ?.placeholder(placeholder)
            ?.error(placeholder)
            ?.apply { if (circleCrop) circleCrop() }
            ?.into(imageView)
    }

    fun getDrawable(context: Context, client: NextcloudClient, urlString: String?): Drawable? =
        createRequestBuilder<Drawable>(context, client, urlString)?.submit()?.get()

    fun <T> loadIntoTarget(
        context: Context,
        client: NextcloudClient,
        url: String,
        target: Target<T>,
        @DrawableRes placeholder: Int
    ) {
        createRequestBuilder<T>(context, client, url)
            ?.placeholder(placeholder)
            ?.error(placeholder)
            ?.into(target)
    }
}
