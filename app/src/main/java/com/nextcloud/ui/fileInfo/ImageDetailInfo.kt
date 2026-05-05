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
import com.nextcloud.client.NominatimClient
import com.nextcloud.ui.fileInfo.model.ImageMetadata
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.databinding.FileInfoFragmentBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.utils.BitmapUtils
import com.owncloud.android.utils.DisplayUtils
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
import kotlin.ranges.contains
import kotlin.text.contains
import kotlin.text.split

class ImageDetailInfo(private val fragment: FileInfoFragment) {
    companion object {
        private const val TEXT_SEP = " • "
        private const val SCROLL_LIMIT = 80.0
    }

    fun init(file: OCFile, metadata: ImageMetadata, binding: FileInfoFragmentBinding) {
        binding.fileInformationTime.text = metadata.date

        // detailed file information
        val fileInformation = mutableListOf<String>()
        if ((metadata.length ?: 0) > 0 && (metadata.width ?: 0) > 0) {
            try {
                @Suppress("MagicNumber")
                val pxlCount = when (val res = metadata.length!! * metadata.width!!.toLong()) {
                    in 0..999999 -> "%.2f".format(res / 1000000f)
                    in 1000000..9999999 -> "%.1f".format(res / 1000000f)
                    else -> (res / 1000000).toString()
                }

                fileInformation.add(String.format(fragment.getString(R.string.image_preview_unit_megapixel), pxlCount))
                fileInformation.add("${metadata.width} × ${metadata.length}")
            } catch (_: NumberFormatException) {
            }
        }
        metadata.fileSize?.let { fileInformation.add(it) }

        if (fileInformation.isNotEmpty()) {
            binding.fileInformationDetails.text = fileInformation.joinToString(separator = TEXT_SEP)
            binding.fileInformation.visibility = View.VISIBLE
        }

        setImageTakenConditions(metadata, binding)

        // initialise map and address views
        metadata.location?.let { location ->
            initMap(binding, file,location.first,location.second)
            binding.imageLocation.visibility = View.VISIBLE

            // launch reverse geocoding request
            fragment.lifecycleScope.launch(Dispatchers.IO) {
                val nominatimClient = NominatimClient(
                    fragment.getString(R.string.osm_geocoder_url),
                    fragment.getString(R.string.osm_geocoder_contact)
                )
                val geocodingResult = nominatimClient.reverseGeocode(location.first, location.second)
                if (geocodingResult != null) {
                    withContext(Dispatchers.Main) {
                        binding.imageLocationText.visibility = View.VISIBLE
                        binding.imageLocationText.text = geocodingResult.displayName
                    }
                }
            }
        }
    }

    private fun setImageTakenConditions(metadata: ImageMetadata, binding: FileInfoFragmentBinding) {
        // camera make and model
        val makeModel = if (metadata.make?.let { metadata.model?.contains(it) } == false) {
            "${metadata.make} ${metadata.model}"
        } else {
            metadata.model ?: metadata.make
        }

        if (metadata.make == null || metadata.model?.contains(metadata.make) == true) {
            binding.imgTCMakeModel.text = metadata.model
        } else {
            binding.imgTCMakeModel.text = String.format(
                fragment.getString(R.string.make_model),
                metadata.make,
                metadata.model
            )
        }

        // image taking conditions
        val imageTakingConditions = mutableListOf<String>()
        metadata.aperture?.let {
            imageTakingConditions.add(String.format(fragment.getString(R.string.image_preview_unit_fnumber), it))
        }
        metadata.exposure?.let {
            imageTakingConditions.add(String.format(fragment.getString(R.string.image_preview_unit_seconds), it))
        }
        metadata.focalLen?.let {
            imageTakingConditions.add(String.format(fragment.getString(R.string.image_preview_unit_millimetres), it))
        }
        metadata.iso?.let {
            imageTakingConditions.add(String.format(fragment.getString(R.string.image_preview_unit_iso), it))
        }

        if (imageTakingConditions.isNotEmpty() && makeModel != null) {
            binding.imgTCMakeModel.text = makeModel
            binding.imgTCConditions.text = imageTakingConditions.joinToString(separator = TEXT_SEP)
            binding.imgTC.visibility = View.VISIBLE
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initMap(binding: FileInfoFragmentBinding, file: OCFile, latitude: Double, longitude: Double, zoom: Double = 13.0) {
        // required for OpenStreetMap
        Configuration.getInstance().userAgentValue = MainApp.getUserAgent()

        val location = GeoPoint(latitude, longitude)

        binding.imageLocationMap.apply {
            setTileSource(TileSourceFactory.MAPNIK)

            // set expected boundaries
            setScrollableAreaLimitLatitude(SCROLL_LIMIT, -SCROLL_LIMIT, 0)
            isVerticalMapRepetitionEnabled = false
            minZoomLevel = 2.0
            maxZoomLevel = NominatimClient.Companion.ZoomLevel.MAX.int.toDouble()

            // initial location
            controller.setCenter(location)
            controller.setZoom(zoom)

            // scale labels to be legible
            isTilesScaledToDpi = true
            setZoomRounding(true)

            // hide zoom buttons
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

            // enable multi-touch zoom
            setMultiTouchControls(true)
            setOnTouchListener { v, _ ->
                v.parent.requestDisallowInterceptTouchEvent(true)
                false
            }

            val markerOverlay = ItemizedIconOverlay(
                mutableListOf(OverlayItem(null, null, location)),
                imagePinDrawable(context, file),
                markerOnGestureListener(latitude, longitude),
                context
            )

            overlays.add(markerOverlay)

            onResume()
        }

        // add copyright notice
        binding.imageLocationMapCopyright.text = binding.imageLocationMap.tileProvider.tileSource.copyrightNotice
    }

    @SuppressLint("SimpleDateFormat")
    fun gatherMetadata(file: OCFile): ImageMetadata {
        val fileSize = DisplayUtils.bytesToHumanReadable(file.fileLength)
        var timestamp = java.lang.Long.max(file.modificationTimestamp, file.creationTimestamp)
        return if (file.isDown) {
            val exif = ExifInterface(file.storagePath)
            var length = exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH)?.toInt()
            var width = exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH)?.toInt()
            var exposure = exif.getAttribute(ExifInterface.TAG_SHUTTER_SPEED_VALUE)

            // get timestamp from date string
            exif.getAttribute(ExifInterface.TAG_DATETIME)?.let {
                timestamp = SimpleDateFormat("y:M:d H:m:s", Locale.ROOT).parse(it)?.time ?: timestamp
            }

            // format exposure string
            if (exposure == null) {
                exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.let {
                    exposure = "1/" + (1 / it.toDouble()).toInt()
                }
            } else if ("/" in exposure) {
                try {
                    exposure.split("/").also {
                        exposure = "1/" + 2f.pow(it[0].toFloat() / it[1].toFloat()).roundToInt()
                    }
                } catch (_: NumberFormatException) {
                }
            }

            // determine size if not contained in exif data
            if ((width ?: 0) <= 0 || (length ?: 0) <= 0) {
                val res = BitmapUtils.getImageResolution(file.storagePath)
                width = res[0]
                length = res[1]
            }

            ImageMetadata(
                fileSize = fileSize,
                length = length,
                width = width,
                exposure = exposure,
                date = formatDate(timestamp),
                location = exif.latLong?.let { Pair(it[0], it[1]) },
                aperture = exif.getAttribute(ExifInterface.TAG_F_NUMBER),
                focalLen = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM),
                make = exif.getAttribute(ExifInterface.TAG_MAKE),
                model = exif.getAttribute(ExifInterface.TAG_MODEL),
                iso = exif.getAttribute(ExifInterface.TAG_ISO_SPEED) ?: exif.getAttribute(
                    ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY
                )
            )
        } else {
            // get metadata from server
            val location = if (file.geoLocation == null) {
                null
            } else {
                Pair(file.geoLocation!!.latitude, file.geoLocation!!.longitude)
            }
            ImageMetadata(
                fileSize = fileSize,
                date = formatDate(timestamp),
                location = location,
                width = file.imageDimension?.width?.toInt(),
                length = file.imageDimension?.height?.toInt()
            )
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun formatDate(timestamp: Long): String = buildString {
        append(SimpleDateFormat("EEEE").format(timestamp))
        append(TEXT_SEP)
        append(DateFormat.getDateInstance(DateFormat.MEDIUM).format(timestamp))
        append(TEXT_SEP)
        append(DateFormat.getTimeInstance(DateFormat.SHORT).format(timestamp))
    }

    private fun imagePinDrawable(context: Context, file: OCFile): LayerDrawable {
        val drawable = ContextCompat.getDrawable(context, R.drawable.photo_pin) as LayerDrawable

        val bitmap =
            ThumbnailsCacheManager.getBitmapFromDiskCache(ThumbnailsCacheManager.PREFIX_THUMBNAIL + file.remoteId)
        BitmapUtils.bitmapToCircularBitmapDrawable(fragment.resources, bitmap)?.let {
            drawable.setDrawable(1, it)
        }

        return drawable
    }

    /**
     * OnItemGestureListener for marker in MapView.
     */
    private fun markerOnGestureListener(latitude: Double, longitude: Double) =
        object : ItemizedIconOverlay.OnItemGestureListener<OverlayItem> {
            override fun onItemSingleTapUp(index: Int, item: OverlayItem): Boolean {
                val intent = Intent(Intent.ACTION_VIEW, "geo:0,0?q=$latitude,$longitude".toUri())
                DisplayUtils.startIntentIfAppAvailable(intent, fragment.activity, R.string.no_map_app_availble)
                return true
            }

            override fun onItemLongPress(index: Int, item: OverlayItem): Boolean = false
        }
}
