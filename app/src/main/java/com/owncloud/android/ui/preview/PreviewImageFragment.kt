/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020-2024 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2017-2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2013-2015 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.ui.preview

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.PictureDrawable
import android.os.AsyncTask
import android.os.Bundle
import android.os.Process
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGParseException
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.material.snackbar.Snackbar
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.ui.fileactions.FileActionsBottomSheet.Companion.newInstance
import com.nextcloud.utils.GlideHelper
import com.nextcloud.utils.extensions.clickWithDebounce
import com.nextcloud.utils.extensions.getParcelableArgument
import com.nextcloud.utils.extensions.getTypedActivity
import com.nextcloud.utils.extensions.setVisibleIf
import com.owncloud.android.R
import com.owncloud.android.databinding.PreviewImageFragmentBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment
import com.owncloud.android.ui.dialog.RemoveFilesDialogFragment
import com.owncloud.android.ui.fragment.FileFragment
import com.owncloud.android.ui.preview.PreviewMediaFragment.Companion.newInstance
import com.owncloud.android.utils.BitmapUtils
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.theme.ViewThemeUtils
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.droidsonroids.gif.GifDrawable
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.ref.WeakReference
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

/**
 * This fragment shows a preview of a downloaded image.
 * Trying to get an instance with a NULL [OCFile] will produce an [IllegalStateException].
 * If the [OCFile] passed is not downloaded, an [IllegalStateException] is generated on instantiation too.
 */

/**
 * Creates an empty fragment for image previews.
 *
 *
 * MUST BE KEPT: the system uses it when tries to re-instantiate a fragment automatically (for instance, when the
 * device is turned a aside).
 *
 *
 * DO NOT CALL IT: an [OCFile] and [User] must be provided for a successful construction
 */

@Suppress("TooManyFunctions")
class PreviewImageFragment :
    FileFragment(),
    Injectable {
    private var loadRemotely: Boolean? = null
    private var bitmap: Bitmap? = null

    private var ignoreFirstSavedState = false
    private var loadBitmapTask: LoadBitmapTask? = null

    @Inject
    lateinit var connectivityService: ConnectivityService

    @Inject
    lateinit var accountManager: UserAccountManager

    @Inject
    lateinit var backgroundJobManager: BackgroundJobManager

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    private lateinit var binding: PreviewImageFragmentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = arguments ?: throw IllegalArgumentException("Arguments may not be null!")

        file = args.getParcelableArgument(ARG_FILE, OCFile::class.java)

        // TODO better in super, but needs to check ALL the class extending FileFragment;
        // not right now
        ignoreFirstSavedState = args.getBoolean(ARG_IGNORE_FIRST)
        loadRemotely = args.getBoolean(ARG_LOAD_REMOTELY)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        binding = PreviewImageFragmentBinding.inflate(inflater, container, false)

        binding.image.visibility = View.GONE
        binding.root.setOnClickListener { togglePreviewImageFullScreen() }
        binding.image.setOnClickListener { togglePreviewImageFullScreen() }

        checkLivePhotoAvailability()
        toggleImageLayout(false)

        return binding.root
    }

    @Suppress("MagicNumber")
    private fun checkLivePhotoAvailability() {
        val livePhotoVideo = file.livePhotoVideo ?: return

        binding.livePhotoIndicator.visibility = View.VISIBLE
        clickWithDebounce(binding.livePhotoIndicator, 4000L) {
            playLivePhoto(livePhotoVideo)
        }
    }

    private fun hideActionBar() {
        (requireActivity() as PreviewImageActivity).run {
            toggleActionBarVisibility(true)
        }
    }

    private fun playLivePhoto(file: OCFile?) {
        if (file == null) {
            return
        }

        hideActionBar()

        val mediaFragment: Fragment = newInstance(file, accountManager.user, 0, true, true)
        val fragmentManager = requireActivity().supportFragmentManager
        fragmentManager.beginTransaction().run {
            replace(R.id.top, mediaFragment)
            addToBackStack(null)
            commit()
        }
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated("Deprecated in Java")
    @Suppress("ReturnCount")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (savedInstanceState == null) {
            Log_OC.d(TAG, "savedInstanceState is null")
            return
        }

        if (ignoreFirstSavedState) {
            Log_OC.d(TAG, "Saved state ignored")
            ignoreFirstSavedState = false
            return
        }

        val file = savedInstanceState.getParcelableArgument(EXTRA_FILE, OCFile::class.java)
        if (file == null) {
            Log_OC.d(TAG, "file cannot be found inside the savedInstanceState")
            return
        }

        setFile(file)

        val maxScale = binding.image.maximumScale
        val minScale = binding.image.minimumScale
        var savedScale = savedInstanceState.getFloat(EXTRA_ZOOM)

        if (savedScale < minScale || savedScale > maxScale) {
            Log_OC.d(TAG, "Saved scale $savedScale is out of bounds, setting to default scale.")
            savedScale = min(maxScale.toDouble(), max(minScale.toDouble(), savedScale.toDouble()))
                .toFloat()
        }

        try {
            binding.image.scale = savedScale
        } catch (e: IllegalArgumentException) {
            Log_OC.d(TAG, "Error caught at setScale: $e")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putFloat(EXTRA_ZOOM, binding.image.scale)
        outState.putParcelable(EXTRA_FILE, file)
    }

    override fun onStart() {
        super.onStart()

        if (file == null) {
            showErrorMessage(R.string.preview_image_error_no_local_file)
            return
        }

        binding.image.tag = file.fileId
        shimmerThumbnail()

        if (loadRemotely == true) {
            loadImageRemotely()
        } else {
            loadImageLocally()
        }
    }

    private fun shimmerThumbnail() {
        binding.image.visibility = View.GONE
        binding.emptyListProgress.visibility = View.VISIBLE
        var thumbnail = getThumbnailBitmap(file)
        if (thumbnail != null) {
            binding.shimmer.visibility = View.VISIBLE
            binding.image.visibility = View.GONE
            bitmap = thumbnail
        } else {
            thumbnail = ThumbnailsCacheManager.mDefaultImg
        }

        binding.shimmerThumbnail.setImageBitmap(thumbnail)
    }

    private fun loadImageRemotely() {
        lifecycleScope.launch(Dispatchers.IO) {
            val previewImageActivity = getTypedActivity(PreviewImageActivity::class.java)
            val client = previewImageActivity?.clientRepository?.getNextcloudClient() ?: return@launch
            withContext(Dispatchers.Main) {
                val target = object : CustomTarget<Drawable>() {
                    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                        binding.image.setImageDrawable(resource)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        binding.image.setImageDrawable(placeholder)
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        super.onLoadFailed(errorDrawable)
                        binding.image.setImageDrawable(errorDrawable)
                    }
                }

                GlideHelper.loadIntoTarget(
                    requireContext(),
                    client,
                    client.getFilesDavUri(file.remotePath),
                    target,
                    R.drawable.file_image
                )

                toggleImageLayout(true)
                binding.image.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.background_color_inverse
                    )
                )
            }
        }
    }

    private fun loadImageLocally() {
        loadBitmapTask = LoadBitmapTask(binding.image, binding.emptyListView, binding.emptyListProgress)
        toggleImageLayout(false)
        loadBitmapTask?.execute(file)
    }

    private fun toggleImageLayout(showImage: Boolean) {
        binding.image.setVisibleIf(showImage)
        binding.emptyListView.visibility = View.GONE
        binding.emptyListProgress.setVisibleIf(!showImage)
    }

    private fun getThumbnailBitmap(file: OCFile): Bitmap? =
        ThumbnailsCacheManager.getBitmapFromDiskCache(ThumbnailsCacheManager.PREFIX_THUMBNAIL + file.remoteId)

    override fun onStop() {
        Log_OC.d(TAG, "onStop starts")
        loadBitmapTask?.cancel(true)
        loadBitmapTask = null
        super.onStop()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val menuHost: MenuHost = requireActivity()
        addMenuProvider(menuHost)
    }

    private fun addMenuProvider(menuHost: MenuHost) {
        menuHost.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.custom_menu_placeholder, menu)
                    val item = menu.findItem(R.id.custom_menu_placeholder_item)

                    item.icon?.let {
                        item.setIcon(
                            viewThemeUtils.platform.colorDrawable(
                                it,
                                ContextCompat.getColor(requireContext(), R.color.white)
                            )
                        )
                    }
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
                    R.id.custom_menu_placeholder_item -> {
                        val file = file
                        if (containerActivity.storageManager != null && file != null) {
                            // Update the file
                            val updatedFile = containerActivity.storageManager.getFileById(file.fileId)
                            setFile(updatedFile)

                            val fileNew = getFile()
                            if (fileNew != null) {
                                showFileActions(file)
                            }
                        }
                        true
                    }

                    else -> false
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    private fun showFileActions(file: OCFile) {
        val additionalFilter: MutableList<Int> = ArrayList(
            listOf(
                R.id.action_rename_file,
                R.id.action_sync_file,
                R.id.action_move_or_copy,
                R.id.action_favorite,
                R.id.action_unset_favorite,
                R.id.action_pin_to_homescreen
            )
        )

        if (getFile() != null && getFile().isSharedWithMe && !getFile().canReshare()) {
            additionalFilter.add(R.id.action_send_share_file)
        }

        val fragmentManager = childFragmentManager
        newInstance(file, false, additionalFilter)
            .setResultListener(fragmentManager, this) { itemId: Int -> this.onFileActionChosen(itemId) }
            .show(fragmentManager, "actions")
    }

    /**
     * {@inheritDoc}
     */
    private fun onFileActionChosen(itemId: Int) {
        if (itemId == R.id.action_send_share_file) {
            if (file.isSharedWithMe && !file.canReshare()) {
                Snackbar.make(requireView(), R.string.resharing_is_not_allowed, Snackbar.LENGTH_LONG).show()
            } else {
                containerActivity.fileOperationsHelper.sendShareFile(file)
            }
        } else if (itemId == R.id.action_open_file_with) {
            openFile()
        } else if (itemId == R.id.action_remove_file) {
            val dialog = RemoveFilesDialogFragment.newInstance(file)
            dialog.show(parentFragmentManager, ConfirmationDialogFragment.FTAG_CONFIRMATION)
        } else if (itemId == R.id.action_see_details) {
            seeDetails()
        } else if (itemId == R.id.action_download_file || itemId == R.id.action_sync_file) {
            containerActivity.fileOperationsHelper.syncFile(file)
        } else if (itemId == R.id.action_cancel_sync) {
            containerActivity.fileOperationsHelper.cancelTransference(file)
        } else if (itemId == R.id.action_set_as_wallpaper) {
            containerActivity.fileOperationsHelper.setPictureAs(file, imageView)
        } else if (itemId == R.id.action_export_file) {
            val list = ArrayList<OCFile>()
            list.add(file)
            containerActivity.fileOperationsHelper.exportFiles(
                list,
                context,
                view,
                backgroundJobManager
            )
        } else if (itemId == R.id.action_edit) {
            (requireActivity() as PreviewImageActivity).startImageEditor(file)
        }
    }

    private fun seeDetails() {
        containerActivity.showDetails(file)
    }

    @SuppressFBWarnings("Dm")
    override fun onDestroy() {
        bitmap?.recycle()
        super.onDestroy()
    }

    /**
     * Opens the previewed image with an external application.
     */
    private fun openFile() {
        containerActivity.fileOperationsHelper.openFile(file)
        finish()
    }

    @SuppressLint("StaticFieldLeak")
    private inner class LoadBitmapTask(imageView: PhotoView, infoView: LinearLayout, progressView: FrameLayout) :
        AsyncTask<OCFile?, Void?, LoadImage?>() {
        /**
         * Weak reference to the target [ImageView] where the bitmap will be loaded into.
         *
         *
         * Using a weak reference will avoid memory leaks if the target ImageView is retired from memory before the load
         * finishes.
         */
        private val imageViewRef = WeakReference(imageView)
        private val infoViewRef = WeakReference(infoView)
        private val progressViewRef = WeakReference(progressView)

        /**
         * Error message to show when a load fails.
         */
        private var mErrorMessageId = 0
        private val paramsLength = 1

        @Suppress("TooGenericExceptionCaught", "MagicNumber", "ReturnCount", "LongMethod", "NestedBlockDepth")
        @Deprecated("Deprecated in Java")
        override fun doInBackground(vararg params: OCFile?): LoadImage? {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND + Process.THREAD_PRIORITY_MORE_FAVORABLE)

            if (params.size != paramsLength) {
                return null
            }

            var bitmapResult: Bitmap? = null
            var drawableResult: Drawable? = null
            val ocFile = params[0] ?: return null
            val storagePath = ocFile.storagePath
            try {
                val maxDownScale = 3 // could be a parameter passed to doInBackground(...)
                val screenSize = DisplayUtils.getScreenSize(activity)
                var minWidth = screenSize.x
                var minHeight = screenSize.y
                var i = 0
                while (i < maxDownScale && bitmapResult == null && drawableResult == null) {
                    if (MIME_TYPE_SVG.equals(ocFile.mimeType, ignoreCase = true)) {
                        if (isCancelled) {
                            return null
                        }

                        try {
                            val svg = SVG.getFromInputStream(FileInputStream(storagePath))
                            drawableResult = PictureDrawable(svg.renderToPicture())

                            if (isCancelled) {
                                return LoadImage(null, drawableResult, ocFile)
                            }
                        } catch (e: FileNotFoundException) {
                            mErrorMessageId = R.string.common_error_unknown
                            Log_OC.e(TAG, "File not found trying to load " + file.storagePath, e)
                        } catch (e: SVGParseException) {
                            mErrorMessageId = R.string.common_error_unknown
                            Log_OC.e(TAG, "Couldn't parse SVG " + file.storagePath, e)
                        }
                    } else {
                        if (isCancelled) {
                            return null
                        }

                        try {
                            bitmapResult = BitmapUtils.retrieveBitmapFromFile(storagePath, minWidth, minHeight)

                            if (isCancelled) {
                                return LoadImage(bitmapResult, null, ocFile)
                            }

                            if (bitmapResult == null) {
                                mErrorMessageId = R.string.preview_image_error_unknown_format
                                Log_OC.e(TAG, "File could not be loaded as a bitmap: $storagePath")
                                break
                            }
                        } catch (e: OutOfMemoryError) {
                            mErrorMessageId = R.string.common_error_out_memory
                            if (i < maxDownScale - 1) {
                                Log_OC.w(TAG, "Out of memory rendering file $storagePath ; scaling down")
                                minWidth /= 2
                                minHeight /= 2
                            } else {
                                Log_OC.w(TAG, "Out of memory rendering file $storagePath ; failing")
                            }
                            bitmapResult?.recycle()
                            bitmapResult = null
                        }
                    }
                    i++
                }
            } catch (e: NoSuchFieldError) {
                mErrorMessageId = R.string.common_error_unknown
                Log_OC.e(
                    TAG,
                    "Error from access to non-existing field despite protection; file " +
                        storagePath,
                    e
                )
            } catch (t: Throwable) {
                mErrorMessageId = R.string.common_error_unknown
                Log_OC.e(TAG, "Unexpected error loading " + file.storagePath, t)
            }

            return LoadImage(bitmapResult, drawableResult, ocFile)
        }

        @Deprecated("Deprecated in Java")
        override fun onCancelled(result: LoadImage?) {
            result?.bitmap?.recycle()
        }

        @Deprecated("Deprecated in Java")
        override fun onPostExecute(result: LoadImage?) {
            if (result?.bitmap != null || result?.drawable != null) {
                showLoadedImage(result)
            } else {
                showErrorMessage(mErrorMessageId)
            }

            if (result?.bitmap != null && bitmap != result.bitmap) {
                // unused bitmap, release it! (just in case)
                result.bitmap.recycle()
            }
        }

        private fun showLoadedImage(result: LoadImage?) {
            val imageView = imageViewRef.get()
            val bitmap = result?.bitmap
            val drawable = result?.drawable

            if (imageView == null) {
                return
            }

            if (bitmap != null) {
                Log_OC.d(
                    TAG,
                    "Showing image with resolution " + bitmap.width + "x" +
                        bitmap.height
                )

                if (MIME_TYPE_PNG.equals(result.ocFile.mimeType, ignoreCase = true) ||
                    MIME_TYPE_GIF.equals(result.ocFile.mimeType, ignoreCase = true)
                ) {
                    resources
                    imageView.setImageDrawable(generateCheckerboardLayeredDrawable(result, bitmap))
                } else {
                    imageView.setImageBitmap(bitmap)
                }

                this@PreviewImageFragment.bitmap = bitmap // needs to be kept for recycling when not useful
            } else {
                if (drawable != null &&
                    MIME_TYPE_SVG.equals(result.ocFile.mimeType, ignoreCase = true)
                ) {
                    resources
                    imageView.setImageDrawable(generateCheckerboardLayeredDrawable(result, null))
                }
            }

            val infoView = infoViewRef.get()
            infoView?.visibility = View.GONE

            val progressView = progressViewRef.get()
            progressView?.visibility = View.GONE

            imageView.setBackgroundColor(resources.getColor(R.color.background_color_inverse))
            imageView.visibility = View.VISIBLE
        }
    }

    @Suppress("ReturnCount")
    private fun generateCheckerboardLayeredDrawable(result: LoadImage, bitmap: Bitmap?): LayerDrawable {
        val resources = resources
        val layers = arrayOfNulls<Drawable>(2)
        layers[0] = ResourcesCompat.getDrawable(resources, R.color.bg_default, null)

        val bitmapDrawable =
            if (MIME_TYPE_PNG.equals(result.ocFile.mimeType, ignoreCase = true)) {
                bitmap?.toDrawable(resources)
            } else if (MIME_TYPE_SVG.equals(result.ocFile.mimeType, ignoreCase = true)) {
                result.drawable
            } else if (MIME_TYPE_GIF.equals(result.ocFile.mimeType, ignoreCase = true)) {
                try {
                    GifDrawable(result.ocFile.storagePath)
                } catch (exception: IOException) {
                    result.drawable
                }
            } else {
                bitmap?.toDrawable(resources)
            }

        layers[1] = bitmapDrawable
        val layerDrawable = LayerDrawable(layers)

        val activity: Activity? = activity
        if (activity != null) {
            val bitmapWidth: Int
            val bitmapHeight: Int

            if (MIME_TYPE_PNG.equals(result.ocFile.mimeType, ignoreCase = true)) {
                if (bitmap == null) {
                    return layerDrawable
                }

                bitmapWidth = convertDpToPixel(bitmap.width.toFloat(), getActivity())
                bitmapHeight = convertDpToPixel(bitmap.height.toFloat(), getActivity())
            } else {
                if (bitmapDrawable == null) {
                    return layerDrawable
                }

                bitmapWidth = convertDpToPixel(bitmapDrawable.intrinsicWidth.toFloat(), getActivity())
                bitmapHeight = convertDpToPixel(bitmapDrawable.intrinsicHeight.toFloat(), getActivity())
            }

            layerDrawable.setLayerSize(0, bitmapWidth, bitmapHeight)
            layerDrawable.setLayerSize(1, bitmapWidth, bitmapHeight)
        }

        return layerDrawable
    }

    private fun showErrorMessage(@StringRes errorMessageId: Int) {
        setSorryMessageForMultiList(errorMessageId)
    }

    private fun setSorryMessageForMultiList(@StringRes message: Int) {
        binding.run {
            emptyListViewHeadline.run {
                setText(R.string.preview_sorry)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.standard_grey))
            }

            emptyListViewText.run {
                setText(message)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.standard_grey))
            }

            emptyListView.run {
                setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.bg_default))
                visibility = View.VISIBLE
            }

            emptyListIcon.setImageResource(R.drawable.file_image)
            image.visibility = View.GONE
            emptyListProgress.visibility = View.GONE
        }
    }

    fun handleUnsupportedImage() {
        try {
            (activity as? PreviewImageActivity)?.requestForDownload(file) ?: context?.let {
                Snackbar.make(
                    binding.emptyListView,
                    resources.getString(R.string.could_not_download_image),
                    Snackbar.LENGTH_INDEFINITE
                ).show()
            }
        } catch (e: IllegalArgumentException) {
            Log_OC.d(TAG, e.message)
        }
    }

    fun setNoConnectionErrorMessage() {
        try {
            Snackbar.make(binding.emptyListView, R.string.auth_no_net_conn_title, Snackbar.LENGTH_LONG).show()
        } catch (e: IllegalArgumentException) {
            Log_OC.d(TAG, e.message)
        }
    }

    private fun finish() {
        activity?.finish()
    }

    private fun togglePreviewImageFullScreen() {
        val previewImageActivity = getTypedActivity(PreviewImageActivity::class.java)
        previewImageActivity?.toggleFullScreen()
        toggleImageBackground()
    }

    @Suppress("ComplexCondition")
    private fun toggleImageBackground() {
        if (file != null &&
            (
                MIME_TYPE_PNG.equals(
                    file.mimeType,
                    ignoreCase = true
                ) ||
                    MIME_TYPE_SVG.equals(file.mimeType, ignoreCase = true)
                ) &&
            activity != null &&
            activity is PreviewImageActivity
        ) {
            val previewImageActivity = activity as PreviewImageActivity?

            if (binding.image.drawable is LayerDrawable) {
                val layerDrawable = binding.image.drawable as LayerDrawable

                val layerOne = if (previewImageActivity?.isSystemUIVisible == true) {
                    ResourcesCompat.getDrawable(resources, R.color.bg_default, null)
                } else {
                    ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.backrepeat,
                        null
                    )
                }

                layerDrawable.setDrawableByLayerId(layerDrawable.getId(0), layerOne)

                binding.image.setImageDrawable(layerDrawable)
                binding.image.invalidate()
            }
        }
    }

    val imageView: PhotoView
        get() = binding.image

    private inner class LoadImage(val bitmap: Bitmap?, val drawable: Drawable?, val ocFile: OCFile)
    companion object {
        private const val EXTRA_FILE = "FILE"
        private const val EXTRA_ZOOM = "ZOOM"

        private const val ARG_FILE = "FILE"
        private const val ARG_IGNORE_FIRST = "IGNORE_FIRST"
        private const val ARG_LOAD_REMOTELY = "ARG_LOAD_REMOTELY"
        private const val MIME_TYPE_PNG = "image/png"
        private const val MIME_TYPE_GIF = "image/gif"
        private const val MIME_TYPE_SVG = "image/svg+xml"

        private val TAG: String = PreviewImageFragment::class.java.simpleName

        /**
         * Public factory method to create a new fragment that previews an image.
         *
         *
         * Android strongly recommends keep the empty constructor of fragments as the only public constructor, and use
         * [.setArguments] to set the needed arguments.
         *
         *
         * This method hides to client objects the need of doing the construction in two steps.
         *
         * @param imageFile             An [OCFile] to preview as an image in the fragment
         * @param ignoreFirstSavedState Flag to work around an unexpected behaviour of { FragmentStateAdapter } ;
         * TODO better solution
         */
        fun newInstance(
            imageFile: OCFile,
            ignoreFirstSavedState: Boolean,
            loadRemotely: Boolean
        ): PreviewImageFragment {
            val args = Bundle().apply {
                putParcelable(ARG_FILE, imageFile)
                putBoolean(ARG_IGNORE_FIRST, ignoreFirstSavedState)
                putBoolean(ARG_LOAD_REMOTELY, loadRemotely)
            }

            return PreviewImageFragment().apply {
                this.loadRemotely = loadRemotely
                arguments = args
            }
        }

        /**
         * Helper method to test if an [OCFile] can be passed to a [PreviewImageFragment] to be previewed.
         *
         * @param file File to test if can be previewed.
         * @return 'True' if the file can be handled by the fragment.
         */
        @JvmStatic
        fun canBePreviewed(file: OCFile?): Boolean = file != null && MimeTypeUtil.isImage(file)

        private fun convertDpToPixel(dp: Float, context: Context?): Int {
            val resources = context?.resources ?: return 0
            val metrics = resources.displayMetrics
            return (dp * (metrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)).toInt()
        }
    }
}
