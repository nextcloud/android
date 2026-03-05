/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2013-2015 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */

package com.owncloud.android.ui.preview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.nextcloud.client.account.User
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.jobs.download.FileDownloadHelper.Companion.instance
import com.nextcloud.utils.extensions.getParcelableArgument
import com.nextcloud.utils.extensions.getTypedActivity
import com.owncloud.android.R
import com.owncloud.android.databinding.FileDownloadFragmentBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.fragment.FileFragment
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

class FileDownloadFragment :
    FileFragment(),
    View.OnClickListener,
    Injectable {
    private var user: User? = null
    private var _binding: FileDownloadFragmentBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    private var ignoreFirstSavedState = false
    private var downloadError = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            file = it.getParcelableArgument(ARG_FILE, OCFile::class.java)
            ignoreFirstSavedState = it.getBoolean(ARG_IGNORE_FIRST)
            user = it.getParcelableArgument(ARG_USER, User::class.java)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)

        if (!ignoreFirstSavedState) {
            downloadError = requireArguments().getBoolean(EXTRA_ERROR)
            instance().downloadFile(user!!, file)
        } else {
            ignoreFirstSavedState = false
        }

        _binding = FileDownloadFragmentBinding.inflate(inflater, container, false)

        viewThemeUtils.material.run {
            colorProgressBar(binding.progressBar)
            colorMaterialTextButton(binding.cancelBtn)
            themeCardView(binding.progressCard)
            themeCardView(binding.errorCard)
        }

        binding.cancelBtn.setOnClickListener(this)
        binding.fileDownloadLL.setOnClickListener {
            getTypedActivity(PreviewImageActivity::class.java)?.toggleFullScreen()
        }

        setButtonsForTransferring(isTransferring = !downloadError)

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.run {
            putParcelable(EXTRA_FILE, file)
            putParcelable(EXTRA_USER, user)
            putBoolean(EXTRA_ERROR, downloadError)
        }
    }

    override fun onClick(v: View) {
        if (v.id == R.id.cancelBtn) {
            containerActivity.getFileOperationsHelper().cancelTransference(file)
            requireActivity().finish()
        } else {
            Log_OC.e(TAG, "Incorrect view clicked!")
        }
    }

    private fun setButtonsForTransferring(isTransferring: Boolean) {
        val binding = _binding ?: return

        val transferringVisibility = if (isTransferring) View.VISIBLE else View.GONE
        val remoteVisibility = if (isTransferring) View.GONE else View.VISIBLE

        binding.run {
            downloadingContainer.visibility = transferringVisibility
            errorContainer.visibility = remoteVisibility

            progressText.apply {
                setText(R.string.downloader_download_in_progress_ticker)
                visibility = transferringVisibility
            }
        }
    }

    fun setError(error: Boolean) {
        downloadError = error
    }

    companion object {
        const val EXTRA_FILE: String = "FILE"
        const val EXTRA_USER: String = "USER"
        private const val EXTRA_ERROR = "ERROR"
        private const val ARG_FILE = "FILE"
        private const val ARG_IGNORE_FIRST = "IGNORE_FIRST"
        private const val ARG_USER = "USER"
        private val TAG: String = FileDownloadFragment::class.java.simpleName

        fun newInstance(file: OCFile?, user: User?, ignoreFirstSavedState: Boolean): Fragment =
            FileDownloadFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_FILE, file)
                    putParcelable(ARG_USER, user)
                    putBoolean(ARG_IGNORE_FIRST, ignoreFirstSavedState)
                }
            }
    }
}
