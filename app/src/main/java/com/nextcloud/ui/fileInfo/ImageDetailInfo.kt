/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.ui.fileInfo

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.drawable.LayerDrawable
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.lifecycleScope
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.client.NominatimClient
import com.nextcloud.ui.fileInfo.model.ImageMetadata
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.databinding.FileInfoFragmentBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.utils.BitmapUtils
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.ItemizedIconOverlay
import org.osmdroid.views.overlay.OverlayItem
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToInt

class ImageDetailInfo(private val fragment: FileInfoFragment, private val viewThemeUtils: ViewThemeUtils) {

    companion object {
        private const val TEXT_SEP = " • "
        private const val SCROLL_LIMIT = 80.0
    }

    fun init(file: OCFile, binding: FileInfoFragmentBinding) {
        viewThemeUtils.material.themeCardView(binding.imageDetailLayout)
        binding.imageDetailLayout.visibility = View.VISIBLE

        val metadata = gatherMetadata(file)
        binding.fileInformationTime.text = metadata.date
        binding.fileDetailsIcon.setImageDrawable(
            viewThemeUtils.platform.tintDrawable(
                fragment.requireContext(),
                R.drawable.outline_image_24,
                ColorRole.ON_BACKGROUND
            )
        )

        binding.cameraInformationIcon.setImageDrawable(
            viewThemeUtils.platform.tintDrawable(
                fragment.requireContext(),
                R.drawable.outline_camera_24,
                ColorRole.ON_BACKGROUND
            )
        )

        val fileInformation = buildList {
            val length = metadata.length ?: 0
            val width = metadata.width ?: 0
            if (length > 0 && width > 0) {
                runCatching {
                    @Suppress("MagicNumber")
                    val pxlCount = when (val res = length * width.toLong()) {
                        in 0..999_999 -> "%.2f".format(res / 1_000_000f)
                        in 1_000_000..9_999_999 -> "%.1f".format(res / 1_000_000f)
                        else -> (res / 1_000_000).toString()
                    }
                    add(fragment.getString(R.string.image_preview_unit_megapixel).format(pxlCount))
                    add("$width × $length")
                }
            }
            metadata.fileSize?.let { add(it) }
        }

        if (fileInformation.isNotEmpty()) {
            binding.fileInformationDetails.text = fileInformation.joinToString(TEXT_SEP)
            binding.fileInformation.visibility = View.VISIBLE
        }

        setImageTakenConditions(metadata, binding)

        metadata.location?.let { (lat, lon) ->
            initMap(binding, file, lat, lon)
            binding.imageLocation.visibility = View.VISIBLE

            fragment.lifecycleScope.launch(Dispatchers.IO) {
                val nominatimClient = NominatimClient(
                    fragment.getString(R.string.osm_geocoder_url),
                    fragment.getString(R.string.osm_geocoder_contact)
                )
                nominatimClient.reverseGeocode(lat, lon)?.let { result ->
                    withContext(Dispatchers.Main) {
                        binding.imageLocationText.text = result.displayName
                        binding.imageLocationText.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun setImageTakenConditions(metadata: ImageMetadata, binding: FileInfoFragmentBinding) {
        val makeModel = when {
            metadata.make == null -> metadata.model
            metadata.model?.contains(metadata.make) == true -> metadata.model
            else -> fragment.getString(R.string.make_model).format(metadata.make, metadata.model)
        }

        val imageTakingConditions = buildList {
            metadata.aperture?.let { add(fragment.getString(R.string.image_preview_unit_fnumber).format(it)) }
            metadata.exposure?.let { add(fragment.getString(R.string.image_preview_unit_seconds).format(it)) }
            metadata.focalLen?.let { add(fragment.getString(R.string.image_preview_unit_millimetres).format(it)) }
            metadata.iso?.let { add(fragment.getString(R.string.image_preview_unit_iso).format(it)) }
        }

        if (imageTakingConditions.isNotEmpty() && makeModel != null) {
            binding.imgTCMakeModel.text = makeModel
            binding.imgTCConditions.text = imageTakingConditions.joinToString(TEXT_SEP)
            binding.imgTC.visibility = View.VISIBLE
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initMap(
        binding: FileInfoFragmentBinding,
        file: OCFile,
        latitude: Double,
        longitude: Double,
        zoom: Double = 13.0
    ) {
        Configuration.getInstance().userAgentValue = MainApp.getUserAgent()

        val location = GeoPoint(latitude, longitude)

        binding.imageLocationMap.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setScrollableAreaLimitLatitude(SCROLL_LIMIT, -SCROLL_LIMIT, 0)
            isVerticalMapRepetitionEnabled = false
            minZoomLevel = 2.0
            maxZoomLevel = NominatimClient.Companion.ZoomLevel.MAX.int.toDouble()
            controller.setCenter(location)
            controller.setZoom(zoom)
            isTilesScaledToDpi = true
            setZoomRounding(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            setMultiTouchControls(true)
            setOnTouchListener { v, _ ->
                v.parent.requestDisallowInterceptTouchEvent(true)
                false
            }
            overlays.add(
                ItemizedIconOverlay(
                    mutableListOf(OverlayItem(null, null, location)),
                    imagePinDrawable(context, file),
                    markerOnGestureListener(latitude, longitude),
                    context
                )
            )
            onResume()
        }

        binding.imageLocationMapCopyright.text =
            binding.imageLocationMap.tileProvider.tileSource.copyrightNotice
    }

    fun gatherMetadata(file: OCFile): ImageMetadata {
        val fileSize = DisplayUtils.bytesToHumanReadable(file.fileLength)
        val timestamp = maxOf(file.modificationTimestamp, file.creationTimestamp)
        return if (file.isDown) {
            gatherLocalMetadata(file, fileSize, timestamp)
        } else {
            gatherRemoteMetadata(file, fileSize, timestamp)
        }
    }

    private fun gatherLocalMetadata(file: OCFile, fileSize: String, fallbackTimestamp: Long): ImageMetadata {
        val exif = ExifInterface(file.storagePath)
        val timestamp = parseTimestamp(exif, fallbackTimestamp)
        val (width, length) = parseImageDimensions(exif, file.storagePath)

        return ImageMetadata(
            fileSize = fileSize,
            date = formatDate(timestamp),
            length = length,
            width = width,
            exposure = parseExposure(exif),
            location = exif.latLong?.let { Pair(it[0], it[1]) },
            aperture = exif.getAttribute(ExifInterface.TAG_F_NUMBER),
            focalLen = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM),
            make = exif.getAttribute(ExifInterface.TAG_MAKE),
            model = exif.getAttribute(ExifInterface.TAG_MODEL),
            iso = exif.getAttribute(ExifInterface.TAG_ISO_SPEED)
                ?: exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)
        )
    }

    private fun gatherRemoteMetadata(file: OCFile, fileSize: String, timestamp: Long) = ImageMetadata(
        fileSize = fileSize,
        date = formatDate(timestamp),
        location = file.geoLocation?.let { Pair(it.latitude, it.longitude) },
        width = file.imageDimension?.width?.toInt(),
        length = file.imageDimension?.height?.toInt()
    )

    private fun parseTimestamp(exif: ExifInterface, fallback: Long): Long =
        exif.getAttribute(ExifInterface.TAG_DATETIME)?.let {
            SimpleDateFormat("y:M:d H:m:s", Locale.ROOT).parse(it)?.time
        } ?: fallback

    @Suppress("ReturnCount")
    private fun parseExposure(exif: ExifInterface): String? {
        val shutterSpeed = exif.getAttribute(ExifInterface.TAG_SHUTTER_SPEED_VALUE)
        val exposureTime = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)
            ?.let { "1/${(1 / it.toDouble()).toInt()}" }

        val raw = shutterSpeed ?: exposureTime ?: return null

        if ('/' !in raw) return raw

        return runCatching {
            raw.split("/").let { parts ->
                "1/${2f.pow(parts[0].toFloat() / parts[1].toFloat()).roundToInt()}"
            }
        }.getOrDefault(raw)
    }

    private fun parseImageDimensions(exif: ExifInterface, storagePath: String): Pair<Int?, Int?> {
        val width = exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH)?.toInt()
        val length = exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH)?.toInt()

        if ((width ?: 0) > 0 && (length ?: 0) > 0) return Pair(width, length)

        return BitmapUtils.getImageResolution(storagePath).let { Pair(it[0], it[1]) }
    }

    private fun formatDate(timestamp: Long): String = buildString {
        append(SimpleDateFormat("EEEE", Locale.getDefault()).format(timestamp))
        append(TEXT_SEP)
        append(DateFormat.getDateInstance(DateFormat.MEDIUM).format(timestamp))
        append(TEXT_SEP)
        append(DateFormat.getTimeInstance(DateFormat.SHORT).format(timestamp))
    }

    private fun imagePinDrawable(context: Context, file: OCFile): LayerDrawable =
        (ContextCompat.getDrawable(context, R.drawable.photo_pin) as LayerDrawable).apply {
            val bitmap = ThumbnailsCacheManager.getBitmapFromDiskCache(
                ThumbnailsCacheManager.PREFIX_THUMBNAIL + file.remoteId
            )
            BitmapUtils.bitmapToCircularBitmapDrawable(fragment.resources, bitmap)?.let {
                setDrawable(1, it)
            }
        }

    private fun markerOnGestureListener(latitude: Double, longitude: Double) =
        object : ItemizedIconOverlay.OnItemGestureListener<OverlayItem> {
            override fun onItemSingleTapUp(index: Int, item: OverlayItem): Boolean {
                val intent = Intent(Intent.ACTION_VIEW, "geo:0,0?q=$latitude,$longitude".toUri())
                DisplayUtils.startIntentIfAppAvailable(intent, fragment.activity, R.string.no_map_app_availble)
                return true
            }

            override fun onItemLongPress(index: Int, item: OverlayItem) = false
        }
}
