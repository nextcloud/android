/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 ZetaTom
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */

package com.nextcloud.ui.fileInfo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.client.account.User
import com.nextcloud.client.di.Injectable
import com.nextcloud.ui.fileInfo.model.ImageMetadata
import com.nextcloud.utils.extensions.getParcelableArgument
import com.owncloud.android.R
import com.owncloud.android.databinding.FileInfoFragmentBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

class FileInfoFragment :
    Fragment(),
    Injectable {
    private lateinit var binding: FileInfoFragmentBinding
    private lateinit var file: OCFile
    private lateinit var user: User
    private var metadata: ImageMetadata? = null
    private var imageDetailInfo: ImageDetailInfo? = null

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FileInfoFragmentBinding.inflate(layoutInflater, container, false)

        val arguments = arguments ?: throw IllegalStateException("arguments are mandatory")
        file = arguments.getParcelableArgument(ARG_FILE, OCFile::class.java)!!
        user = arguments.getParcelableArgument(ARG_USER, User::class.java)!!

        if (savedInstanceState != null) {
            file = savedInstanceState.getParcelableArgument(ARG_FILE, OCFile::class.java)!!
            user = savedInstanceState.getParcelableArgument(ARG_USER, User::class.java)!!
            metadata = savedInstanceState.getParcelableArgument(ARG_METADATA, ImageMetadata::class.java)!!
        }

        imageDetailInfo = ImageDetailInfo(this)


        // TODO Bottom sheet or dropdown?
        data class DropdownItem(val text: String, val iconRes: Int)
        val items = listOf(
            DropdownItem("Option 1", R.drawable.outline_camera_24),
            DropdownItem("Option 2", R.drawable.outline_image_24),
            DropdownItem("Option 3", R.drawable.ic_information_outline)
        )

        val adapter = object : ArrayAdapter<DropdownItem>(requireContext(), R.layout.item_dropdown_with_icon, items) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                val item = getItem(position)
                if (item != null) {
                    view.text = item.text
                    val drawable = ContextCompat.getDrawable(context, item.iconRes)?.mutate()
                    drawable?.let {
                        viewThemeUtils.platform.tintDrawable(requireContext(),it, ColorRole.ON_SURFACE)
                    }
                    view.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
                }
                return view
            }
        }
        binding.dropdownMenuAutoComplete.setAdapter(adapter)

        val defaultSelectedItem = items.firstOrNull()
        if (defaultSelectedItem != null) {
            binding.dropdownMenuAutoComplete.setText(defaultSelectedItem.text, false)
            val drawable = ContextCompat.getDrawable(requireContext(), defaultSelectedItem.iconRes)?.mutate()
            drawable?.let {
                viewThemeUtils.platform.tintDrawable(requireContext(),it, ColorRole.ON_SURFACE)
            }
            binding.dropdownMenuAutoComplete.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null)
            binding.dropdownMenuAutoComplete.compoundDrawablePadding = resources.getDimensionPixelSize(R.dimen.standard_padding)
        }

        binding.dropdownMenuAutoComplete.setOnItemClickListener { _, _, position, _ ->
            val selected = items[position]
            binding.dropdownMenuAutoComplete.setText(selected.text, false)
            val drawable = ContextCompat.getDrawable(requireContext(), selected.iconRes)?.mutate()
            binding.dropdownMenuAutoComplete.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null)
            binding.dropdownMenuAutoComplete.compoundDrawablePadding = resources.getDimensionPixelSize(R.dimen.standard_padding)
        }

        //

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

        return binding.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.run {
            putParcelable(ARG_FILE, file)
            putParcelable(ARG_USER, user)
            putParcelable(ARG_METADATA, metadata)
        }
    }

    override fun onStart() {
        super.onStart()

        if (MimeTypeUtil.isImage(file)) {
            metadata = imageDetailInfo?.gatherMetadata(file)
        }

        setupFragment()
    }

    @VisibleForTesting
    fun hideMap() {
        binding.imageLocationMap.visibility = View.GONE
    }

    private fun setupFragment() {
        if (MimeTypeUtil.isImage(file)) {
            metadata?.let { imageDetailInfo?.init(file, it, binding) }
        }
    }

    companion object {
        private const val ARG_FILE = "FILE"
        private const val ARG_USER = "USER"
        private const val ARG_METADATA = "METADATA"

        @JvmStatic
        fun newInstance(file: OCFile, user: User): FileInfoFragment = FileInfoFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_FILE, file)
                putParcelable(ARG_USER, user)
            }
        }
    }
}
