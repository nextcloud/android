/*
 *  Nextcloud Android Library is available under MIT license
 *
 *  @author Álvaro Brey Vilas
 *  Copyright (C) 2022 Álvaro Brey Vilas
 *  Copyright (C) 2022 Nextcloud GmbH
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 *  BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 *  ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *  CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
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
import com.owncloud.android.R
import com.owncloud.android.databinding.PreviewPdfFragmentBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.files.FileMenuFilter
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
        viewModel.process(file)
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
        FileMenuFilter.hideAll(menu)
    }

    override fun onResume() {
        super.onResume()
        val toolbarActivity: FileDisplayActivity = requireActivity() as FileDisplayActivity
        toolbarActivity.showSortListGroup(false)
        toolbarActivity.updateActionBarTitleAndHomeButton(file)
    }

    @VisibleForTesting
    fun dismissSnack() {
        snack?.dismiss()
    }
}
