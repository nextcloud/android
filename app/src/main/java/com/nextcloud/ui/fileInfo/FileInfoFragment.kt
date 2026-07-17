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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.nextcloud.client.account.User
import com.nextcloud.client.di.Injectable
import com.owncloud.android.databinding.FileInfoFragmentBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.theme.CapabilityUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

class FileInfoFragment :
    Fragment(),
    Injectable {
    private lateinit var binding: FileInfoFragmentBinding

    val file: OCFile? by lazy {
        arguments?.let { BundleCompat.getParcelable(it, ARG_FILE, OCFile::class.java) }
    }

    val user: User? by lazy {
        arguments?.let { BundleCompat.getParcelable(it, ARG_USER, User::class.java) }
    }

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    @Inject
    lateinit var viewModelFactory: FileInfoViewModel.Factory

    private lateinit var viewModel: FileInfoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val file = file ?: return
        val user = user ?: return
        viewModel = ViewModelProvider(this, provideViewModelFactory(file, user))[FileInfoViewModel::class.java]
    }

    private fun provideViewModelFactory(file: OCFile, user: User) = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = viewModelFactory.create(file, user) as T
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FileInfoFragmentBinding.inflate(layoutInflater, container, false)

        if (MimeTypeUtil.isImage(file)) {
            val imageDetailInfo = ImageDetailInfo(this, viewThemeUtils)
            file?.let { imageDetailInfo.init(it, binding) }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!::viewModel.isInitialized) {
            binding.governanceLayout.visibility = View.GONE
            return
        }
        if (CapabilityUtils.getCapability(context).governance.isTrue) {
            val governanceDetailInfo = GovernanceDetailInfo(binding, viewThemeUtils, this, viewModel)
            governanceDetailInfo.init()
        } else {
            binding.governanceLayout.visibility = View.GONE
        }
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
