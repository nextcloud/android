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
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import com.bumptech.glide.request.FutureTarget
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.AppWidgetTarget
import com.bumptech.glide.request.target.BitmapImageViewTarget
import com.bumptech.glide.request.target.Target
import com.nextcloud.android.lib.resources.dashboard.DashboardWidgetItem
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
        return try {
            Glide
                .with(context)
                .`as`(PictureDrawable::class.java)
                .load(uri)
                .placeholder(placeholder)
                .error(placeholder)
                .listener(SvgSoftwareLayerSetter())
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
    fun loadViaURISVGIntoImageView(
        context: Context,
        uriString: String?,
        target: Target<PictureDrawable?>,
        placeholder: Int
    ) {
        val uri = validateAndGetURI(uriString) ?: return
        val svgRequestBuilder = createSvgRequestBuilder(context, uri, placeholder)
        svgRequestBuilder?.into(target)
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

    /**
     * Loads a bitmap from a URI into a bitmap [Target].
     *
     * @param context The context to use with Glide.
     * @param uriString The image URI as a string.
     * @param target The [Target] load into.
     */
    fun loadViaURIIntoBitmapTarget(context: Context, uriString: String, target: Target<Bitmap>) {
        val uri = validateAndGetURI(uriString) ?: return

        Glide
            .with(context)
            .`as`(Bitmap::class.java)
            .load(uri)
            .into(target)
    }

    /**
     * Loads a bitmap image from a URL into an [ImageView] with a cross-fade transition with listener.
     *
     * @param context The context to use with Glide.
     * @param url The URL of the image.
     * @param imageView The [ImageView] to load into.
     * @param placeholder A [Drawable] for placeholder and error.
     * @param listener [RequestListener] to handle success/error events.
     */
    fun loadViaURLIntoImageView(
        context: Context,
        url: String,
        imageView: ImageView,
        placeholder: Drawable,
        listener: RequestListener<Bitmap>
    ) {
        val validatedURL = validateAndGetURL(url) ?: return

        Glide
            .with(context)
            .asBitmap()
            .load(validatedURL)
            .placeholder(placeholder)
            .error(placeholder)
            .transition(BitmapTransitionOptions.withCrossFade(android.R.anim.fade_in))
            .listener(listener)
            .into(imageView)
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
            .into(target)
    }
    // endregion
}
