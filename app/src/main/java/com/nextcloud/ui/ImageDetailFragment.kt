/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 ZetaTom
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.client.NominatimClient
import com.nextcloud.client.account.User
import com.nextcloud.client.di.Injectable
import com.nextcloud.utils.extensions.getParcelableArgument
import com.nextcloud.utils.extensions.logFileSize
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.databinding.PreviewImageDetailsFragmentBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.utils.BitmapUtils
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.ItemizedIconOverlay
import org.osmdroid.views.overlay.ItemizedIconOverlay.OnItemGestureListener
import org.osmdroid.views.overlay.OverlayItem
import java.lang.Long.max
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import kotlin.math.pow
import kotlin.math.roundToInt

class ImageDetailFragment : Fragment(), Injectable {
    private lateinit var binding: PreviewImageDetailsFragmentBinding
    private lateinit var file: OCFile
    private lateinit var user: User
    private lateinit var metadata: ImageMetadata
    private lateinit var nominatimClient: NominatimClient

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    private val tag = "ImageDetailFragment"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = PreviewImageDetailsFragmentBinding.inflate(layoutInflater, container, false)

        binding.fileDetailsIcon.setImageDrawable(
            viewThemeUtils.platform.tintDrawable(
                requireContext(),
                R.drawable.outline_image_24,
                ColorRole.ON_BACKGROUND
            )
        )

        binding.cameraInformationIcon.setImageDrawable(
            viewThemeUtils.platform.tintDrawable(
                requireContext(),
                R.drawable.outline_camera_24,
                ColorRole.ON_BACKGROUND
            )
        )

        val arguments = arguments ?: throw IllegalStateException("arguments are mandatory")
        file = arguments.getParcelableArgument(ARG_FILE, OCFile::class.java)!!
        user = arguments.getParcelableArgument(ARG_USER, User::class.java)!!

        if (savedInstanceState != null) {
            file = savedInstanceState.getParcelableArgument(ARG_FILE, OCFile::class.java)!!
            user = savedInstanceState.getParcelableArgument(ARG_USER, User::class.java)!!
            metadata = savedInstanceState.getParcelableArgument(ARG_METADATA, ImageMetadata::class.java)!!
        }

        nominatimClient = NominatimClient(
            getString(R.string.osm_geocoder_url),
            getString(R.string.osm_geocoder_contact)
        )

        return binding.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        file.logFileSize(tag)
        outState.putParcelable(ARG_FILE, file)
        outState.putParcelable(ARG_USER, user)
        outState.putParcelable(ARG_METADATA, metadata)
    }

    override fun onStart() {
        super.onStart()
        gatherMetadata()
        setupFragment()
    }

    @SuppressLint("LongMethod")
    private fun setupFragment() {
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

                fileInformation.add(String.format(getString(R.string.image_preview_unit_megapixel), pxlCount))
                fileInformation.add("${metadata.width!!} × ${metadata.length!!}")
            } catch (_: NumberFormatException) {
            }
        }
        metadata.fileSize?.let { fileInformation.add(it) }

        if (fileInformation.isNotEmpty()) {
            binding.fileInformationDetails.text = fileInformation.joinToString(separator = TEXT_SEP)
            binding.fileInformation.visibility = View.VISIBLE
        }

        setImageTakenConditions()

        // initialise map and address views
        metadata.location?.let { location ->
            initMap(location.first, location.second)
            binding.imageLocation.visibility = View.VISIBLE

            // launch reverse geocoding request
            CoroutineScope(Dispatchers.IO).launch {
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

    private fun setImageTakenConditions() {
        // camera make and model
        val makeModel = if (metadata.make?.let { metadata.model?.contains(it) } == false) {
            "${metadata.make} ${metadata.model}"
        } else {
            metadata.model ?: metadata.make
        }

        if (metadata.make == null || metadata.model?.contains(metadata.make!!) == true) {
            binding.imgTCMakeModel.text = metadata.model
        } else {
            binding.imgTCMakeModel.text = String.format(
                getString(R.string.make_model),
                metadata.make,
                metadata.model
            )
        }

        // image taking conditions
        val imageTakingConditions = mutableListOf<String>()
        metadata.aperture?.let {
            imageTakingConditions.add(String.format(getString(R.string.image_preview_unit_fnumber), it))
        }
        metadata.exposure?.let {
            imageTakingConditions.add(String.format(getString(R.string.image_preview_unit_seconds), it))
        }
        metadata.focalLen?.let {
            imageTakingConditions.add(String.format(getString(R.string.image_preview_unit_millimetres), it))
        }
        metadata.iso?.let {
            imageTakingConditions.add(String.format(getString(R.string.image_preview_unit_iso), it))
        }

        if (imageTakingConditions.isNotEmpty() && makeModel != null) {
            binding.imgTCMakeModel.text = makeModel
            binding.imgTCConditions.text = imageTakingConditions.joinToString(separator = TEXT_SEP)
            binding.imgTC.visibility = View.VISIBLE
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initMap(latitude: Double, longitude: Double, zoom: Double = 13.0) {
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
                imagePinDrawable(context),
                markerOnGestureListener(latitude, longitude),
                context
            )

            overlays.add(markerOverlay)

            onResume()
        }

        // add copyright notice
        binding.imageLocationMapCopyright.text = binding.imageLocationMap.tileProvider.tileSource.copyrightNotice
    }

    @VisibleForTesting
    fun hideMap() {
        binding.imageLocationMap.visibility = View.GONE
    }

    @SuppressLint("SimpleDateFormat")
    private fun gatherMetadata() {
        val fileSize = DisplayUtils.bytesToHumanReadable(file.fileLength)
        var timestamp = max(file.modificationTimestamp, file.creationTimestamp)
        if (file.isDown) {
            val exif = androidx.exifinterface.media.ExifInterface(file.storagePath)
            var length = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_IMAGE_LENGTH)?.toInt()
            var width = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_IMAGE_WIDTH)?.toInt()
            var exposure = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_SHUTTER_SPEED_VALUE)

            // get timestamp from date string
            exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME)?.let {
                timestamp = SimpleDateFormat("y:M:d H:m:s", Locale.ROOT).parse(it)?.time ?: timestamp
            }

            // format exposure string
            if (exposure == null) {
                exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_EXPOSURE_TIME)?.let {
                    exposure = "1/" + (1 / it.toDouble()).toInt()
                }
            } else if ("/" in exposure!!) {
                try {
                    exposure!!.split("/").also {
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

            metadata = ImageMetadata(
                fileSize = fileSize,
                length = length,
                width = width,
                exposure = exposure,
                date = formatDate(timestamp),
                location = exif.latLong?.let { Pair(it[0], it[1]) },
                aperture = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_F_NUMBER),
                focalLen = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM),
                make = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_MAKE),
                model = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_MODEL),
                iso = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_ISO_SPEED) ?: exif.getAttribute(
                    androidx.exifinterface.media.ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY
                )
            )
        } else {
            // get metadata from server
            val location = if (file.geoLocation == null) {
                null
            } else {
                Pair(file.geoLocation!!.latitude, file.geoLocation!!.longitude)
            }
            metadata = ImageMetadata(
                fileSize = fileSize,
                date = formatDate(timestamp),
                location = location,
                width = file.imageDimension?.width?.toInt(),
                length = file.imageDimension?.height?.toInt()
            )
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun formatDate(timestamp: Long): String {
        return buildString {
            append(SimpleDateFormat("EEEE").format(timestamp))
            append(TEXT_SEP)
            append(DateFormat.getDateInstance(DateFormat.MEDIUM).format(timestamp))
            append(TEXT_SEP)
            append(DateFormat.getTimeInstance(DateFormat.SHORT).format(timestamp))
        }
    }

    private fun imagePinDrawable(context: Context): LayerDrawable {
        val drawable = ContextCompat.getDrawable(context, R.drawable.photo_pin) as LayerDrawable

        val bitmap =
            ThumbnailsCacheManager.getBitmapFromDiskCache(ThumbnailsCacheManager.PREFIX_THUMBNAIL + file.remoteId)
        BitmapUtils.bitmapToCircularBitmapDrawable(resources, bitmap)?.let {
            drawable.setDrawable(1, it)
        }

        return drawable
    }

    /**
     * OnItemGestureListener for marker in MapView.
     */
    private fun markerOnGestureListener(latitude: Double, longitude: Double) =
        object : OnItemGestureListener<OverlayItem> {
            override fun onItemSingleTapUp(index: Int, item: OverlayItem): Boolean {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$latitude,$longitude"))
                DisplayUtils.startIntentIfAppAvailable(intent, activity, R.string.no_map_app_availble)
                return true
            }

            override fun onItemLongPress(index: Int, item: OverlayItem): Boolean {
                return false
            }
        }

    @Parcelize
    private data class ImageMetadata(
        val fileSize: String? = null,
        val date: String? = null,
        val length: Int? = null,
        val width: Int? = null,
        val exposure: String? = null,
        val aperture: String? = null,
        val focalLen: String? = null,
        val iso: String? = null,
        val make: String? = null,
        val model: String? = null,
        val location: Pair<Double, Double>? = null
    ) : Parcelable

    companion object {
        private const val ARG_FILE = "FILE"
        private const val ARG_USER = "USER"
        private const val ARG_METADATA = "METADATA"
        private const val TEXT_SEP = " • "
        private const val SCROLL_LIMIT = 80.0

        @JvmStatic
        fun newInstance(file: OCFile, user: User): ImageDetailFragment {
            return ImageDetailFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_FILE, file)
                    putParcelable(ARG_USER, user)
                }
            }
        }
    }
}
