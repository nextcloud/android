/*
 * Nextcloud Android client application
 *
 * @author Álvaro Brey Vilas
 * Copyright (C) 2022 Álvaro Brey Vilas
 * Copyright (C) 2022 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
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

        file = requireArguments().getParcelable(ARG_FILE)!!
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
        viewModel.shouldShowZoomTip.observe(viewLifecycleOwner) { shouldShow ->
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

    private fun getScreenWidth(): Int =
        requireContext().resources.displayMetrics.widthPixels

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
