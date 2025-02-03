/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.crop

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import com.ionos.scanbot.R
import com.ionos.scanbot.databinding.ScanbotActivityCropBinding
import com.ionos.scanbot.di.inject
import com.ionos.scanbot.screens.base.BaseActivity
import com.ionos.scanbot.exception.CreateIntentException
import com.ionos.scanbot.screens.common.LockProgressDialog
import com.ionos.scanbot.screens.crop.CropScreen.*
import com.ionos.scanbot.screens.crop.CropScreen.Event.*

internal class CropActivity : BaseActivity<Event, State, ViewModel>() {
	override val viewModelFactory by lazy { viewModelFactoryAssistant.create(getImageId()) }
	override val viewBinding by lazy { ScanbotActivityCropBinding.inflate(layoutInflater) }

	private val viewModelFactoryAssistant by inject { cropViewModelFactoryAssistant() }
    private val progressDialog by lazy { LockProgressDialog() }

	companion object {
		private const val IMAGE_ID = "IMAGE_ID"

		fun createIntent(context: Context, imageId: String): Intent {
			return Intent(context, CropActivity::class.java).putExtra(IMAGE_ID, imageId)
		}
	}

	private fun getImageId(): String = intent
		?.getStringExtra(IMAGE_ID)
		?: throw CreateIntentException(this::class)

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		initOnBackPressedCallback()
		initListeners()
	}

	override fun onStart() {
		super.onStart()
		viewModel.onStart()
	}

	private fun initOnBackPressedCallback() {
		onBackPressedDispatcher.addCallback(this, true) { viewModel.onBackPressed() }
	}

	private fun initListeners() = with(viewBinding) {
		btnAutoDetectContour.setOnClickListener { viewModel.onAutoDetectContourClicked() }
		btnResetContour.setOnClickListener { viewModel.onResetContourClicked() }
		toolbar.tvSave.setOnClickListener { viewModel.onSaveClicked(cropView.getPolygon()) }
		toolbar.ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
	}

	override fun State.render() = with(viewBinding) {
		renderProcessing(processing)
		renderButton(buttonType)
	}

	private fun renderProcessing(processing: Boolean) = if (processing) {
		progressDialog.start(this, getString(R.string.scanbot_processing_title))
	} else {
		progressDialog.stop()
	}

	private fun renderButton(buttonType: ButtonType) = with(viewBinding) {
		val isAutoDetect = buttonType == ButtonType.AUTODETECT
		btnAutoDetectContour.visibility = if (isAutoDetect) View.VISIBLE else View.GONE
		btnResetContour.visibility = if (isAutoDetect) View.GONE else View.VISIBLE
	}

	override fun Event.handle() = when (this) {
		is DisplayPictureEvent -> viewBinding.cropView.render(bitmap)
		is DisplayPolygonEvent -> viewBinding.cropView.setPolygon(polygon)
		is ShowErrorMessageEvent -> showMessage(R.string.scanbot_fail)
		is CloseScreenEvent -> finish()
	}
}