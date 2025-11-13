/*
 * Nextcloud Android client application
 *
 * @author TSI-mc
 * Copyright (C) 2022 TSI-mc
 * Copyright (C) 2022 Nextcloud GmbH
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.client.di.Injectable
import com.owncloud.android.R
import com.owncloud.android.databinding.FragmentGalleryBottomSheetBinding
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

class GalleryFragmentBottomSheetDialog(private val actions: GalleryFragmentBottomSheetActions) :
    BottomSheetDialogFragment(R.layout.fragment_gallery_bottom_sheet),
    Injectable {
    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    private lateinit var binding: FragmentGalleryBottomSheetBinding
    private var currentMediaState: MediaState = MediaState.MEDIA_STATE_DEFAULT

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentGalleryBottomSheetBinding.inflate(layoutInflater, container, false)

        val bottomSheetDialog = dialog as BottomSheetDialog
        bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        bottomSheetDialog.behavior.skipCollapsed = true

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupLayout()
        setupClickListener()
    }

    private fun setupLayout() {
        viewThemeUtils.platform.colorViewBackground(binding.bottomSheet, ColorRole.SURFACE)

        listOf(
            binding.tickMarkShowImages,
            binding.tickMarkShowVideos
        ).forEach {
            viewThemeUtils.platform.colorImageView(it, ColorRole.PRIMARY)
        }

        listOf(
            binding.btnSelectMediaFolder,
            binding.btnHideVideos,
            binding.btnHideImages
        ).forEach {
            viewThemeUtils.material.colorMaterialButtonText(it)
        }

        when (currentMediaState) {
            MediaState.MEDIA_STATE_PHOTOS_ONLY -> {
                binding.tickMarkShowImages.visibility = View.VISIBLE
                binding.tickMarkShowVideos.visibility = View.GONE
            }
            MediaState.MEDIA_STATE_VIDEOS_ONLY -> {
                binding.tickMarkShowImages.visibility = View.GONE
                binding.tickMarkShowVideos.visibility = View.VISIBLE
            }
            else -> {
                binding.tickMarkShowImages.visibility = View.VISIBLE
                binding.tickMarkShowVideos.visibility = View.VISIBLE
            }
        }
    }

    private fun setupClickListener() {
        binding.btnHideImages.setOnClickListener {
            currentMediaState = if (currentMediaState == MediaState.MEDIA_STATE_VIDEOS_ONLY) {
                MediaState.MEDIA_STATE_DEFAULT
            } else {
                MediaState.MEDIA_STATE_VIDEOS_ONLY
            }
            notifyStateChange()
            dismiss()
        }
        binding.btnHideVideos.setOnClickListener {
            currentMediaState = if (currentMediaState == MediaState.MEDIA_STATE_PHOTOS_ONLY) {
                MediaState.MEDIA_STATE_DEFAULT
            } else {
                MediaState.MEDIA_STATE_PHOTOS_ONLY
            }
            notifyStateChange()
            dismiss()
        }
        binding.btnSelectMediaFolder.setOnClickListener {
            actions.selectMediaFolder()
            dismiss()
        }
    }

    private fun notifyStateChange() {
        setupLayout()
        actions.updateMediaContent(currentMediaState)
    }

    val currMediaState: MediaState
        get() = currentMediaState

    enum class MediaState {
        MEDIA_STATE_DEFAULT,
        MEDIA_STATE_PHOTOS_ONLY,
        MEDIA_STATE_VIDEOS_ONLY
    }
}
