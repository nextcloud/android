/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.ui.fileInfo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import com.nextcloud.client.account.User
import com.nextcloud.client.di.Injectable
import com.owncloud.android.databinding.FileInfoFragmentBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

class FileInfoFragment :
    Fragment(),
    Injectable {
    private lateinit var binding: FileInfoFragmentBinding

    private val file: OCFile? by lazy {
        arguments?.let { BundleCompat.getParcelable(it, ARG_FILE, OCFile::class.java) }
    }

    private val user: User? by lazy {
        arguments?.let { BundleCompat.getParcelable(it, ARG_USER, User::class.java) }
    }

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FileInfoFragmentBinding.inflate(layoutInflater, container, false)

        val imageDetailInfo = ImageDetailInfo(this, viewThemeUtils)
        if (MimeTypeUtil.isImage(file)) {
            file?.let { imageDetailInfo.init(it, binding) }
        }

        val governanceDetailInfo = GovernanceDetailInfo(binding, viewThemeUtils, this)
        governanceDetailInfo.init()

        return binding.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.run {
            putParcelable(ARG_FILE, file)
            putParcelable(ARG_USER, user)
        }
    }

    @VisibleForTesting
    fun hideMap() {
        binding.imageLocationMap.visibility = View.GONE
    }

    companion object {
        private const val ARG_FILE = "FILE"
        private const val ARG_USER = "USER"

        fun newInstance(file: OCFile, user: User): FileInfoFragment = FileInfoFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_FILE, file)
                putParcelable(ARG_USER, user)
            }
        }
    }
}
