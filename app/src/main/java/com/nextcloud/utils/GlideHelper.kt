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
import com.bumptech.glide.request.target.AppWidgetTarget
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

    // region SVG
    /**
     * Creates a Glide request builder specifically for loading SVG images from a [Uri].
     *
     * @param context The context to use with Glide.
     * @param uri The [Uri] of the SVG image.
     * @param placeholder Resource ID of the drawable to be used as a placeholder and error fallback.
     * @return A configured [RequestBuilder] for [PictureDrawable], or null if parameters are invalid.
     */
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

    /**
     * Loads an SVG image from a URI string into an [ImageView].
     *
     * @param context The context to use with Glide.
     * @param uriString String representation of the image URI.
     * @param imageView The target [ImageView].
     * @param placeholder Resource ID of the drawable used for placeholder and error.
     */
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

    /**
     * Loads an SVG image from a URI string into a custom [Target].
     *
     * @param context The context to use with Glide.
     * @param uriString String representation of the image URI.
     * @param target The target [Target] where the SVG drawable will be loaded.
     * @param placeholder Resource ID of the drawable used for placeholder and error.
     */
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

    /**
     * Loads an SVG image synchronously and returns it as a [PictureDrawable].
     *
     * Note: This method should not be called on the main thread as it performs a blocking operation.
     *
     * @param context The context to use with Glide.
     * @param uriString String representation of the image URI.
     * @return A [PictureDrawable] if loading is successful, or null otherwise.
     */
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
    // endregion

    /**
     * Loads an image from a URL into a custom [Target].
     *
     * @param context The context to use with Glide.
     * @param url The URL of the image to load.
     * @param target The Glide [Target] where the image will be loaded.
     * @param placeholder Resource ID of the placeholder and error drawable.
     */
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

    /**
     * Loads an image from a URL into an [ImageView] with a [Drawable] placeholder.
     *
     * Disables disk and memory cache.
     *
     * @param context The context to use with Glide.
     * @param url The URL of the image.
     * @param imageView The target [ImageView].
     * @param placeholder A [Drawable] used for placeholder and error.
     */
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

    /**
     * Loads an image from a URL into an [ImageView] using a drawable resource ID as a placeholder.
     *
     * Disables disk and memory cache.
     *
     * @param context The context to use with Glide.
     * @param url The URL of the image.
     * @param imageView The target [ImageView].
     * @param placeholder Resource ID of the drawable used for placeholder and error.
     */
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

    /**
     * Downloads an image from a URL synchronously and returns a [Bitmap].
     *
     * ⚠ Do not call this method on the main thread. It blocks until the image is fully loaded.
     *
     * @param context The context to use with Glide.
     * @param url The image URL as a string. If null, returns null immediately.
     * @return The downloaded [Bitmap], or null if an error occurs.
     */
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

    // region Bitmap
    /**
     * Loads a circular bitmap from a URL into an [ImageView].
     *
     * @param context The context to use with Glide.
     * @param url The URL of the image.
     * @param imageView The [ImageView] to load into.
     * @param placeholder Drawable shown as placeholder and error fallback.
     */
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

    fun loadViaURLIntoImageView(
        context: Context,
        client: NextcloudClient,
        url: String?,
        imageView: ImageView,
        placeholder: Int
    ) {
        val validatedURL = validateAndGetURL(url) ?: return
        if (isSVG(validatedURL)) {
            val uri = validateAndGetURI(url) ?: return

            Glide
                .with(context)
                .`as`(PictureDrawable::class.java)
                .load(uri)
                .placeholder(placeholder)
                .error(placeholder)
                .listener(SvgSoftwareLayerSetter())
                .listener(GlideLogger(methodName = "loadViaURLIntoImageView", identifier = validatedURL))
                .into(imageView)
        } else {
            val glideUrl = GlideUrl(
                validatedURL,
                LazyHeaders.Builder()
                    .addHeader("Authorization", client.credentials)
                    .addHeader("User-Agent", "Mozilla/5.0 (Android) Nextcloud-android")
                    .build()
            )

            Glide
                .with(context)
                .load(glideUrl)
                .placeholder(placeholder)
                .error(placeholder)
                .listener(GlideLogger(methodName = "loadViaURLIntoImageView", identifier = validatedURL))
                .into(imageView)
        }
    }

    // endregion

    // region Widget
    /**
     * Asynchronously creates a [Bitmap] from a dashboard widget item icon URL.
     *
     * Use [FutureTarget.get] to retrieve the bitmap off the UI thread.
     *
     * @param context The context to use with Glide.
     * @param widgetItem The widget item containing the image URL.
     * @return A [FutureTarget] that will asynchronously load the image as a [Bitmap].
     */
    fun createBitmapFromDashboardWidgetItem(context: Context, widgetItem: DashboardWidgetItem): FutureTarget<Bitmap> {
        return Glide
            .with(context)
            .asBitmap()
            .load(widgetItem.iconUrl)
            .override(SVG_SIZE, SVG_SIZE)
            .listener(GlideLogger(methodName = "createBitmapFromDashboardWidgetItem", identifier = widgetItem.iconUrl))
            .submit()
    }

    /**
     * Loads a bitmap from a URI into an [AppWidgetTarget].
     *
     * Uses [DiskCacheStrategy.RESOURCE] to ensure image is cached for re-use.
     *
     * @param context The context to use with Glide.
     * @param uriString The URI string of the image.
     * @param target The [AppWidgetTarget] to load into.
     */
    fun loadViaURIIntoAppWidgetTarget(context: Context, uriString: String, target: AppWidgetTarget) {
        val uri = validateAndGetURI(uriString) ?: return

        Glide
            .with(context)
            .asBitmap()
            .load(uri)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .listener(GlideLogger(methodName = "loadViaURIIntoAppWidgetTarget", identifier = uri.toString()))
            .into(target)
    }
    // endregion
}
