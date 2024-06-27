/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey Vilas <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.owncloud.android.ui.preview.pdf

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.di.ViewModelFactory
import com.nextcloud.utils.MenuUtils
import com.nextcloud.utils.extensions.getParcelableArgument
import com.owncloud.android.R
import com.owncloud.android.databinding.PreviewPdfFragmentBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.preview.PreviewBitmapActivity
import com.owncloud.android.utils.DisplayUtils
import javax.inject.Inject

class PreviewPdfFragment : Fragment(), Injectable {

    @Inject
    lateinit var vmFactory: ViewModelFactory

    private lateinit var binding: PreviewPdfFragmentBinding
    private lateinit var viewModel: PreviewPdfViewModel
    private lateinit var file: OCFile

    private var snack: Snackbar? = null

    companion object {
        private const val ARG_FILE = "FILE"

        @JvmStatic
        fun newInstance(file: OCFile): PreviewPdfFragment = PreviewPdfFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_FILE, file)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = PreviewPdfFragmentBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()

        file = requireArguments().getParcelableArgument(ARG_FILE, OCFile::class.java)!!
        try {
            viewModel.process(file)
        } catch (e: SecurityException) {
            Log_OC.e(this, "onViewCreated: trying to open password protected PDF", e)
            parentFragmentManager.popBackStack()
            DisplayUtils.showSnackMessage(binding.root, R.string.pdf_password_protected)
        }
    }

    private fun setupObservers() {
        viewModel.pdfRenderer.observe(viewLifecycleOwner) { renderer ->
            binding.pdfRecycler.adapter = PreviewPdfAdapter(renderer, getScreenWidth()) { page ->
                viewModel.onClickPage(page)
            }
        }
        viewModel.previewImagePath.observe(viewLifecycleOwner) {
            it?.let { path ->
                val intent = Intent(context, PreviewBitmapActivity::class.java).apply {
                    putExtra(PreviewBitmapActivity.EXTRA_BITMAP_PATH, path)
                }
                requireContext().startActivity(intent)
            }
        }
        viewModel.showZoomTip.observe(viewLifecycleOwner) { shouldShow ->
            if (shouldShow) {
                snack = DisplayUtils.showSnackMessage(binding.root, R.string.pdf_zoom_tip)
                viewModel.onZoomTipShown()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this, vmFactory)[PreviewPdfViewModel::class.java]
        setHasOptionsMenu(true)
    }

    private fun getScreenWidth(): Int = requireContext().resources.displayMetrics.widthPixels

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        MenuUtils.hideAll(menu)
    }

    override fun onResume() {
        super.onResume()
        val parent = activity
        if (parent is FileDisplayActivity) {
            parent.showSortListGroup(false)
            parent.updateActionBarTitleAndHomeButton(file)
        }
    }

    @VisibleForTesting
    fun dismissSnack() {
        snack?.dismiss()
    }
}
