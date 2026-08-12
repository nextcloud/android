/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.fragment.albums.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.client.di.Injectable
import com.nextcloud.utils.date.DateFormatPattern
import com.nextcloud.utils.extensions.setVisibleIf
import com.nextcloud.utils.thumbnail.ThumbnailArguments
import com.nextcloud.utils.thumbnail.ThumbnailGenerator
import com.owncloud.android.R
import com.owncloud.android.databinding.AlbumImageThumbnailBinding
import com.owncloud.android.databinding.AlbumShareActionBinding
import com.owncloud.android.databinding.AlbumSharingBottomSheetBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.albums.PhotoAlbumEntry
import com.owncloud.android.ui.fragment.albums.util.AlbumCollageLayout
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

class AlbumSharingBottomSheet(
    private val photoAlbumEntry: PhotoAlbumEntry?,
    private val fileList: List<OCFile>?,
    private val actions: AlbumSharingBottomSheetActions
) : BottomSheetDialogFragment(),
    Injectable {

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    @Inject
    lateinit var storageManager: FileDataStorageManager

    @Inject
    lateinit var thumbnailGenerator: ThumbnailGenerator

    private var _binding: AlbumSharingBottomSheetBinding? = null
    val binding
        get() = _binding!!

    private var collage: AlbumCollageLayout? = null

    private var shareId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // read only the 1st item of collaborators as there will be no more data apart from current Album
        shareId = photoAlbumEntry?.collaborators?.firstOrNull()?.id
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = AlbumSharingBottomSheetBinding.inflate(inflater, container, false)
        collage = AlbumCollageLayout(binding)

        (dialog as BottomSheetDialog).behavior.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
        viewThemeUtils.platform.colorViewBackground(binding.bottomSheet, ColorRole.SURFACE)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.bottomSheetLoading.visibility = View.GONE
        setUpContent()
        setUpShareComponentsVisibility()
        bindShareActions()
    }

    private fun setUpContent() {
        val album = photoAlbumEntry ?: return

        bindAlbumText(album)
        bindAlbumThumbnail(album)
        initializeImageCollage()
    }

    private fun bindAlbumText(album: PhotoAlbumEntry) = with(binding) {
        albumTitle.text = album.albumName
        albumElements.text = resources.getQuantityString(
            R.plurals.album_elements_text,
            album.nbItems,
            album.nbItems
        )
        albumDate.text = DisplayUtils.getDateByPattern(album.createdDate, DateFormatPattern.MonthWithYear.pattern)
    }

    private fun bindAlbumThumbnail(album: PhotoAlbumEntry) {
        binding.albumImageLayout.thumbnail.tag = album.lastPhoto

        if (album.lastPhoto <= 0) {
            showPlaceholder()
            return
        }

        loadThumbnail(getOrCreateFile(album), binding.albumImageLayout)
    }

    private fun showPlaceholder() = with(binding.albumImageLayout) {
        thumbnail.setImageResource(R.drawable.file_image)
        thumbnail.visibility = View.VISIBLE
        thumbnailShimmer.visibility = View.GONE
    }

    private fun getOrCreateFile(album: PhotoAlbumEntry): OCFile = storageManager.getFileByLocalId(album.lastPhoto)
        ?: OCFile("/${album.albumName}").apply {
            localId = album.lastPhoto
            remoteId = album.lastPhoto.toString()
        }

    private fun loadThumbnail(file: OCFile, target: AlbumImageThumbnailBinding) {
        thumbnailGenerator.setThumbnail(
            file,
            target.thumbnail,
            ThumbnailArguments(isGrid = false, hideVideoOverlay = true, target.thumbnailShimmer)
        )
    }

    private fun initializeImageCollage() {
        val files = fileList?.takeIf { it.isNotEmpty() } ?: return
        val collage = this.collage ?: return

        binding.imageCollage.visibility = View.VISIBLE
        collage.images.forEach { it.root.visibility = View.GONE }
        collage.arrange(files.size)

        files.forEachIndexed { index, file ->
            collage.images[index].let {
                it.root.visibility = View.VISIBLE
                loadThumbnail(file, it)
            }
        }
    }

    private fun setUpShareComponentsVisibility() {
        binding.createShareGroup.setVisibleIf(shareId.isNullOrEmpty())
        binding.shareGroup.setVisibleIf(!shareId.isNullOrEmpty())
    }

    private fun bindShareActions() = with(binding) {
        actionCreateLink.bind(R.drawable.ic_share, R.string.album_create_new_link) { actions.createShare() }
        actionStopSharing.bind(R.drawable.ic_delete, R.string.album_stop_sharing) { actions.removeShare() }
        actionCopy.bind(R.drawable.ic_content_copy, R.string.common_copy) {
            actions.copyShareLink()
            dismiss()
        }
        actionShareLink.bind(R.drawable.shared_via_link, R.string.album_share_link) {
            actions.shareAlbumLink()
            dismiss()
        }
        btnClose.setOnClickListener { dismiss() }
    }

    private fun AlbumShareActionBinding.bind(@DrawableRes icon: Int, @StringRes text: Int, onClick: () -> Unit) {
        button.setImageResource(icon)
        label.setText(text)
        button.contentDescription = label.text

        val listener = View.OnClickListener { onClick() }
        button.setOnClickListener(listener)
        label.setOnClickListener(listener)
    }

    // has to be called when the new share is created or removed
    fun updateShareId(updatedShareId: String?) {
        shareId = updatedShareId
        setUpShareComponentsVisibility()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        collage = null
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance(
            photoAlbumEntry: PhotoAlbumEntry?,
            fileList: List<OCFile>?,
            actions: AlbumSharingBottomSheetActions
        ): AlbumSharingBottomSheet = AlbumSharingBottomSheet(photoAlbumEntry, fileList, actions)
    }
}
