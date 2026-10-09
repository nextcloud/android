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
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.annotation.DrawableRes
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.scale
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.BitmapImageViewTarget
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.target.DrawableImageViewTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.nextcloud.common.NextcloudClient
import com.nextcloud.utils.LinkHelper.validateAndGetURL
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.utils.Log_OC
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Utility object for loading images (including SVGs) using Glide.
 *
 * Provides methods for loading images into `ImageView`, `Target<Drawable>`, `Target<Bitmap>` ...
 * from both URLs and URIs.
 */
@Suppress("TooManyFunctions", "TooGenericExceptionCaught")
object GlideHelper {
    private const val TAG = "GlideHelper"

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
            Log_OC.e(TAG, "exception getBitmap: $e")
            null
        }
    }

    fun loadCircularBitmapIntoImageView(context: Context, url: String?, imageView: ImageView, placeholder: Drawable?) {
        val validatedUrl = validateAndGetURL(url) ?: return

        try {
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
        } catch (e: Exception) {
            Log_OC.e(TAG, "exception loadCircularBitmapIntoImageView: $e")
            imageView.setImageDrawable(placeholder)
        }
    }

    @SuppressLint("CheckResult")
    fun loadIntoImageView(
        context: Context,
        client: NextcloudClient?,
        url: String?,
        imageView: ImageView,
        @DrawableRes placeholder: Int,
        circleCrop: Boolean = false
    ) {
        try {
            createRequestBuilder<Drawable>(context, client, url)
                ?.placeholder(placeholder)
                ?.error(placeholder)
                ?.apply { if (circleCrop) circleCrop() }
                ?.withLogging("loadIntoImageView", url ?: "null")
                ?.into(UntintedDrawableImageViewTarget(imageView)) ?: imageView.setImageResource(placeholder)
        } catch (e: Exception) {
            Log_OC.e(TAG, "exception loadIntoImageView: $e")
            imageView.setImageResource(placeholder)
        }
    }

    /**
     * Loads an image into an [ImageView], rasterizing the result into a tintable drawable so a tint set on the view
     * takes effect.
     *
     * @param context context used to build the Glide request and the rasterized drawable.
     * @param client authenticated client whose credentials are attached to the request; the load is skipped when null.
     * @param url image URL to load; an invalid or null URL leaves the [placeholder] in place.
     * @param imageView target view that receives the loaded drawable.
     * @param placeholder drawable resource shown while loading and on failure.
     * @param sizePx width and height, in pixels, of the rasterized square drawable.
     */
    fun loadTintableIconIntoImageView(
        context: Context,
        client: NextcloudClient?,
        url: String?,
        imageView: ImageView,
        @DrawableRes placeholder: Int,
        sizePx: Int
    ) {
        imageView.setImageResource(placeholder)
        try {
            createRequestBuilder<Drawable>(context, client, url)
                ?.error(placeholder)
                ?.withLogging("loadTintableIconIntoImageView", url ?: "null")
                ?.into(object : CustomViewTarget<ImageView, Drawable>(imageView) {
                    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                        imageView.setImageDrawable(rasterizeToTintableDrawable(context, resource, sizePx))
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        errorDrawable?.let { imageView.setImageDrawable(it) }
                    }

                    override fun onResourceCleared(placeholderDrawable: Drawable?) = Unit
                })
        } catch (e: Exception) {
            Log_OC.e(TAG, "exception loadTintableIconIntoImageView: $e")
            imageView.setImageResource(placeholder)
        }
    }

    private fun rasterizeToTintableDrawable(context: Context, drawable: Drawable, sizePx: Int): Drawable {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap.scale(sizePx, sizePx).toDrawable(context.resources)
        }

        val width = drawable.intrinsicWidth
        val height = drawable.intrinsicHeight
        val bitmap = createBitmap(sizePx, sizePx)
        val canvas = Canvas(bitmap)
        if (width > 0 && height > 0) {
            canvas.scale(sizePx / width.toFloat(), sizePx / height.toFloat())
            drawable.setBounds(0, 0, width, height)
        } else {
            drawable.setBounds(0, 0, sizePx, sizePx)
        }
        drawable.draw(canvas)
        return bitmap.toDrawable(context.resources)
    }

    fun getDrawable(
        context: Context,
        client: NextcloudClient?,
        urlString: String?,
        sizePx: Int = Target.SIZE_ORIGINAL
    ): Drawable? = try {
        createRequestBuilder<Drawable>(context, client, urlString)
            ?.withLogging("getDrawable", urlString ?: "null")
            ?.submit(sizePx, sizePx)
            ?.get()
    } catch (e: Exception) {
        Log_OC.e(TAG, "exception getDrawable: $e")
        null
    }

    fun <T> loadIntoTarget(
        activity: ComponentActivity,
        account: OwnCloudAccount?,
        url: String,
        target: Target<T>,
        @DrawableRes placeholder: Int
    ) {
        if (account == null) {
            Log_OC.e(TAG, "loadIntoTargetWithActivity: account cannot be null")
            return
        }

        activity.lifecycleScope.launch(Dispatchers.IO) {
            val clientFactory = OwnCloudClientManagerFactory.getDefaultSingleton()
            val client = clientFactory.getNextcloudClientFor(account, activity)
            withContext(Dispatchers.Main) {
                try {
                    createRequestBuilder<T>(activity, client, url)
                        ?.placeholder(placeholder)
                        ?.error(placeholder)
                        ?.withLogging("loadIntoTarget", url)
                        ?.into(target)
                } catch (e: Exception) {
                    Log_OC.e(TAG, "exception loadIntoTarget: $e")
                }
            }
        }
    }

    fun createGlideUrl(url: String, client: NextcloudClient) = GlideUrl(
        url,
        LazyHeaders.Builder()
            .addHeader("Authorization", client.credentials)
            .addHeader("User-Agent", "Mozilla/5.0 (Android) Nextcloud-android")
            .build()
    )

    // region private methods
    private class GlideLogger<T>(private val methodName: String, private val identifier: String) : RequestListener<T> {

        override fun onLoadFailed(
            e: GlideException?,
            model: Any?,
            target: Target<T>,
            isFirstResource: Boolean
        ): Boolean {
            Log_OC.e(TAG, "$methodName: Load failed for $identifier")
            Log_OC.e(TAG, "$methodName: Error: ${e?.message}")
            e?.logRootCauses(TAG)
            return false
        }

        override fun onResourceReady(
            resource: T & Any,
            model: Any?,
            target: Target<T?>?,
            dataSource: DataSource,
            isFirstResource: Boolean
        ): Boolean {
            Log_OC.i(TAG, "$methodName: Successfully loaded $identifier from $dataSource")
            return false
        }
    }

    private fun <T> RequestBuilder<T>.withLogging(methodName: String, identifier: String): RequestBuilder<T> =
        listener(GlideLogger(methodName, identifier))

    private class UntintedDrawableImageViewTarget(imageView: ImageView) : DrawableImageViewTarget(imageView) {
        override fun setResource(resource: Drawable?) {
            if (resource != null) {
                view.imageTintList = null
            }
            super.setResource(resource)
        }
    }

    private fun createUrlRequestBuilder(
        context: Context,
        client: NextcloudClient,
        url: String
    ): RequestBuilder<Drawable> {
        val glideUrl = createGlideUrl(url, client)
        return Glide.with(context)
            .asDrawable()
            .load(glideUrl)
            .centerCrop()
    }

    @Suppress("UNCHECKED_CAST", "TooGenericExceptionCaught", "ReturnCount")
    private fun <T> createRequestBuilder(context: Context, client: NextcloudClient?, url: String?): RequestBuilder<T>? {
        if (client == null) {
            Log_OC.e(TAG, "Client is null")
            return null
        }

        val validatedUrl = validateAndGetURL(url) ?: return null

        return try {
            createUrlRequestBuilder(context, client, validatedUrl) as RequestBuilder<T>?
        } catch (e: Exception) {
            Log_OC.e(TAG, "exception createRequestBuilder: $e")
            null
        }
    }
    // endregion
}
