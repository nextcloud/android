/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2020-2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2017-2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2014 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.utils

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.widget.ImageView
import androidx.annotation.DimenRes
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.blue
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.RoundedBitmapDrawable
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import com.nextcloud.utils.rotateBitmapViaExif
import com.nextcloud.utils.view.ScreenMetrics.dpToPx
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.users.Status
import com.owncloud.android.lib.resources.users.StatusType
import com.owncloud.android.ui.StatusDrawable
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import com.nextcloud.utils.decodeSampledBitmapFromFile as decodeSampledBitmap

@Suppress("TooManyFunctions")
object BitmapUtils {
    private const val TAG = "BitmapUtil"
    private const val MD5_ALGORITHM = "MD5"
    private const val HEX_BYTE_FORMAT = "%02x"
    private const val HEX_RADIX = 16
    private const val PALETTE_STEPS = 6
    private const val OPAQUE_ALPHA = 255
    private const val NO_CORNER_RADIUS = -1f
    private const val UNSPECIFIED_SIZE = -1
    private const val STATUS_SIZE_DIVISOR = 4
    private const val STATUS_NO_CLEAR_AT = -1L
    private const val CROP_MASK_COLOR = 0xFF424242.toInt()

    private val MD5_HASH_REGEX = Regex("[0-9a-f]{32}")

    @Suppress("MagicNumber")
    private val usernamePalette: List<Color> by lazy {
        val red = Color(182, 70, 157)
        val yellow = Color(221, 203, 85)
        val nextcloudBlue = Color(0, 130, 201)
        mixPalette(red, yellow) + mixPalette(yellow, nextcloudBlue) + mixPalette(nextcloudBlue, red)
    }

    private val resources: Resources
        get() = MainApp.getAppContext().resources

    fun addColorFilter(originalBitmap: Bitmap, filterColor: Int, opacity: Int): Bitmap {
        val result = originalBitmap.copy(Bitmap.Config.ARGB_8888, true) ?: return originalBitmap
        val paint = Paint().apply {
            color = filterColor
            alpha = opacity
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
        }
        return result.applyCanvas {
            drawBitmap(result, 0f, 0f, null)
            drawRect(0f, 0f, result.width.toFloat(), result.height.toFloat(), paint)
        }
    }

    @JvmStatic
    fun decodeSampledBitmapFromFile(srcPath: String?, reqWidth: Int, reqHeight: Int): Bitmap? =
        decodeSampledBitmap(srcPath, reqWidth, reqHeight)

    fun retrieveBitmapFromFile(storagePath: String, minWidth: Int, minHeight: Int): Bitmap? {
        val (originalWidth, originalHeight) = getImageResolution(storagePath)
        if (originalWidth <= 0 || originalHeight <= 0) {
            return null
        }

        val scaleFactor = min(minWidth.toFloat() / originalWidth, minHeight.toFloat() / originalHeight)
        val scaledWidth = (originalWidth * scaleFactor).toInt()
        val scaledHeight = (originalHeight * scaleFactor).toInt()
        return decodeSampledBitmap(storagePath, scaledWidth, scaledHeight)
            .rotateBitmapViaExif(readExifOrientation(storagePath))
    }

    @JvmStatic
    fun calculateSampleFactor(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val halfHeight = options.outHeight / 2
        val halfWidth = options.outWidth / 2
        val targetHeight = reqHeight.coerceAtLeast(0)
        val targetWidth = reqWidth.coerceAtLeast(0)
        var inSampleSize = 1
        while (halfHeight / inSampleSize > targetHeight || halfWidth / inSampleSize > targetWidth) {
            inSampleSize *= 2
        }

        return inSampleSize
    }

    @JvmStatic
    fun scaleBitmap(bitmap: Bitmap, px: Float, width: Int, height: Int, max: Int): Bitmap {
        val scale = px / max
        return bitmap.scale((scale * width).roundToInt(), (scale * height).roundToInt())
    }

    @JvmStatic
    fun fitsInto(bitmap: Bitmap, px: Int): Boolean = max(bitmap.width, bitmap.height) <= px

    @JvmStatic
    fun scaleToFit(bitmap: Bitmap, px: Int): Bitmap =
        scaleBitmap(bitmap, px.toFloat(), bitmap.width, bitmap.height, max(bitmap.width, bitmap.height))

    @JvmStatic
    fun getLargestVideoDimension(retriever: MediaMetadataRetriever): Int {
        val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
        val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
        return max(width, height)
    }

    @JvmStatic
    fun centerCropOnPngBackground(source: Bitmap, width: Int, height: Int): Bitmap {
        // a software Canvas cannot draw HARDWARE bitmaps, every other config can be drawn directly
        val drawableSource = if (source.config == Bitmap.Config.HARDWARE) {
            source.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            source
        }

        val scale = max(width.toFloat() / source.width, height.toFloat() / source.height)
        val scaledWidth = scale * source.width
        val scaledHeight = scale * source.height
        val left = (width - scaledWidth) / 2
        val top = (height - scaledHeight) / 2
        val targetRect = RectF(left, top, left + scaledWidth, top + scaledHeight)

        return createBitmap(width, height).applyCanvas {
            drawColor(resources.getColor(R.color.background_color_png, null))
            drawBitmap(drawableSource, null, targetRect, null)
        }.also {
            if (drawableSource !== source) {
                drawableSource.recycle()
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun readExifOrientation(storagePath: String): Int = try {
        ExifInterface(storagePath).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    } catch (e: Exception) {
        Log_OC.e(TAG, "Could not read orientation at: $storagePath, exception: $e")
        ExifInterface.ORIENTATION_NORMAL
    }

    fun getImageResolution(srcPath: String?): IntArray {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(srcPath, options)
        return intArrayOf(options.outWidth, options.outHeight)
    }

    @JvmStatic
    fun usernameToColor(name: String): Color {
        val lowercaseName = name.lowercase()
        val hash = if (MD5_HASH_REGEX.matches(lowercaseName)) {
            lowercaseName
        } else {
            try {
                md5(lowercaseName)
            } catch (_: NoSuchAlgorithmException) {
                return primaryDarkColor()
            }
        }

        val hexDigitSum = hash.sumOf { it.digitToInt(HEX_RADIX) }
        return usernamePalette[hexDigitSum % usernamePalette.size]
    }

    private fun primaryDarkColor(): Color {
        val color = resources.getColor(R.color.primary_dark, null)
        return Color(color.red, color.green, color.blue)
    }

    private fun mixPalette(from: Color, to: Color): List<Color> = List(PALETTE_STEPS) { step ->
        Color(mixChannel(from.r, to.r, step), mixChannel(from.g, to.g, step), mixChannel(from.b, to.b, step))
    }

    private fun mixChannel(from: Int, to: Int, step: Int): Int =
        (from + (to - from) / PALETTE_STEPS.toFloat() * step).toInt()

    private fun md5(value: String): String = MessageDigest.getInstance(MD5_ALGORITHM)
        .digest(value.toByteArray())
        .joinToString("") { HEX_BYTE_FORMAT.format(it) }

    fun bitmapToCircularBitmapDrawable(
        resources: Resources,
        bitmap: Bitmap?,
        radius: Float = NO_CORNER_RADIUS
    ): RoundedBitmapDrawable? {
        bitmap ?: return null

        return RoundedBitmapDrawableFactory.create(resources, bitmap).apply {
            isCircular = true
            if (radius != NO_CORNER_RADIUS) {
                cornerRadius = radius
            }
        }
    }

    @JvmStatic
    @JvmOverloads
    fun drawableToBitmap(
        drawable: Drawable,
        desiredWidth: Int = UNSPECIFIED_SIZE,
        desiredHeight: Int = UNSPECIFIED_SIZE
    ): Bitmap {
        (drawable as? BitmapDrawable)?.bitmap?.let { return it }

        val (width, height) = resolveBitmapSize(drawable, desiredWidth, desiredHeight)
        return createBitmap(width, height).applyCanvas {
            drawable.setBounds(0, 0, width, height)
            drawable.draw(this)
        }
    }

    private fun resolveBitmapSize(drawable: Drawable, desiredWidth: Int, desiredHeight: Int): Pair<Int, Int> {
        val bounds = drawable.bounds
        return when {
            desiredWidth > 0 && desiredHeight > 0 -> desiredWidth to desiredHeight

            drawable.intrinsicWidth > 0 && drawable.intrinsicHeight > 0 ->
                drawable.intrinsicWidth to drawable.intrinsicHeight

            bounds.width() > 0 && bounds.height() > 0 -> bounds.width() to bounds.height()

            else -> 1 to 1
        }
    }

    fun setRoundedBitmapAccordingToListType(gridView: Boolean, thumbnail: Bitmap?, thumbnailView: ImageView) {
        if (gridView) {
            setRoundedBitmapForGridMode(thumbnail, thumbnailView)
        } else {
            setRoundedBitmap(thumbnail, thumbnailView)
        }
    }

    @JvmStatic
    fun setRoundedBitmap(thumbnail: Bitmap?, imageView: ImageView) {
        setRoundedBitmapWithRadius(thumbnail, imageView, R.dimen.file_icon_rounded_corner_radius)
    }

    @JvmStatic
    fun setRoundedBitmapForGridMode(thumbnail: Bitmap?, imageView: ImageView) {
        setRoundedBitmapWithRadius(thumbnail, imageView, R.dimen.file_icon_rounded_corner_radius_for_grid_mode)
    }

    private fun setRoundedBitmapWithRadius(bitmap: Bitmap?, imageView: ImageView, @DimenRes radiusRes: Int) {
        val radius = resources.getDimension(radiusRes)
        imageView.setImageDrawable(bitmapToCircularBitmapDrawable(resources, bitmap, radius))
    }

    @JvmStatic
    fun createAvatarWithStatus(avatar: Bitmap, statusType: StatusType, icon: String, context: Context): Bitmap {
        val avatarRadius = resources.getDimension(R.dimen.list_item_avatar_icon_radius)
        val width = dpToPx(2 * avatarRadius, context)
        val center = width / 2f

        return createBitmap(width, width).applyCanvas {
            drawBitmap(getCroppedBitmap(avatar, width), 0f, 0f, null)

            val status = Status(statusType, "", icon, STATUS_NO_CLEAR_AT)
            val statusDrawable = StatusDrawable(status, (width / STATUS_SIZE_DIVISOR).toFloat(), context)
            translate(center, center)
            statusDrawable.draw(this)
        }
    }

    fun roundBitmap(bitmap: Bitmap): Bitmap {
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        val paint = Paint().apply {
            isAntiAlias = true
            color = resources.getColor(R.color.white, null)
        }

        return createBitmap(bitmap.width, bitmap.height).applyCanvas {
            drawOval(RectF(rect), paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            drawBitmap(bitmap, rect, rect, paint)
        }
    }

    fun tintImage(bitmap: Bitmap, color: Int): Bitmap {
        val paint = Paint().apply { colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN) }
        return createBitmap(bitmap.width, bitmap.height).applyCanvas { drawBitmap(bitmap, 0f, 0f, paint) }
    }

    private fun getCroppedBitmap(bitmap: Bitmap, width: Int): Bitmap {
        val rect = Rect(0, 0, width, width)
        val radius = width / 2f
        val paint = Paint().apply {
            isAntiAlias = true
            color = CROP_MASK_COLOR
        }

        return createBitmap(width, width).applyCanvas {
            drawCircle(radius, radius, radius, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            drawBitmap(bitmap.scale(width, width, filter = false), rect, rect, paint)
        }
    }

    class Color(@JvmField val a: Int, @JvmField val r: Int, @JvmField val g: Int, @JvmField val b: Int) {
        constructor(r: Int, g: Int, b: Int) : this(OPAQUE_ALPHA, r, g, b)
        override fun equals(other: Any?): Boolean = other is Color && r == other.r && g == other.g && b == other.b
        override fun hashCode(): Int = (r shl 16) + (g shl 8) + b
    }
}
