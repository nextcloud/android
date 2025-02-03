/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.camera

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.addCallback
import com.ionos.scanbot.screens.camera.ui_components.NoFreeSpaceMessageDialogWrapper
import com.ionos.scanbot.R
import com.ionos.scanbot.databinding.ScanbotActivityCameraBinding
import com.ionos.scanbot.di.inject
import com.ionos.scanbot.screens.base.BaseActivity
import com.ionos.scanbot.screens.camera.CameraScreen.*
import com.ionos.scanbot.screens.camera.CameraScreen.Event.*
import com.ionos.scanbot.screens.camera.image_picker.ImagePickerLauncher
import com.ionos.scanbot.screens.camera.ui_components.CameraViewController
import com.ionos.scanbot.screens.camera.ui_components.ImportProgressDialog
import com.ionos.scanbot.screens.common.LockProgressDialog
import com.ionos.scanbot.screens.common.use_case.open_screen.OpenScreen
import com.ionos.scanbot.util.permission.hasPermissionTo
import com.ionos.scanbot.util.permission.requestPermissionTo

private const val CHECK_PERMISSION_REQUEST_CODE = 99

internal class CameraActivity : BaseActivity<Event, State, ViewModel>() {
	override val viewModelFactory by inject { cameraViewModelFactory() }
	override val viewBinding by lazy { ScanbotActivityCameraBinding.inflate(layoutInflater) }

	private val imagePickerLauncher = ImagePickerLauncher(this) { viewModel.onPicturesUrisReceived(it) }
	private val importProgressDialog by lazy { ImportProgressDialog(this, viewModel::onCancelImportClicked) }
	private val decodingProgressDialog by lazy { LockProgressDialog() }
	private val noFreeSpaceDialog by lazy { NoFreeSpaceMessageDialogWrapper(this) }
	private val openScreen = OpenScreen(this)
	private val exitDialog by inject { exitDialog() }

	private val cameraViewController: CameraViewController by lazy {
		CameraViewController(
			this,
			viewBinding.scvCameraa,
			viewBinding.pvPolygon,
			viewModel::onPictureReceived,
            viewBinding.userGuidance,
		)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		initOnBackPressedCallback()
		initClickListeners()
		viewModel.onCreate()
        checkCameraPermissions()
	}

    private fun checkCameraPermissions() {
        if (!hasPermissionTo(Manifest.permission.CAMERA)) {
            requestPermissionTo(arrayOf(Manifest.permission.CAMERA), CHECK_PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (permissions.contains(Manifest.permission.CAMERA) &&
            (grantResults.isEmpty() || grantResults[0] == PackageManager.PERMISSION_DENIED)
        ) {
            close()
        }
    }

    fun close() {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

	private fun initOnBackPressedCallback() {
		onBackPressedDispatcher.addCallback(this, true) { viewModel.onBackPressed() }
	}

	private fun initClickListeners() = with(viewBinding) {
		tvCancel.setOnClickListener { viewModel.onCancelClicked() }
		tvFlashStatus.setOnClickListener { viewModel.onFlashToggled() }
		tvAutomaticCaptureStatus.setOnClickListener { viewModel.onAutomaticCaptureToggled() }
		ivTakePhoto.setOnClickListener { viewModel.onTakePictureClicked() }
		tvImport.setOnClickListener { viewModel.onImportClicked() }
	}

	override fun State.render() {
		setAutomaticCaptureEnabled(automaticCaptureEnabled)
		setFlashEnabled(flashEnabled)
		renderProcessing(processing)
	}

	override fun Event.handle() = when (this) {
		is ShowExitDialogEvent -> showExitDialog()
		is CloseScreenEvent -> finish()
		is OpenScreenEvent -> openScreen(intent)
		is ShowMessageEvent -> showMessage(message)
		is ShowNoFreeSpaceAlertEvent -> noFreeSpaceDialog.show(context)
		is TakePictureEvent -> cameraViewController.takePicture()
		is LaunchImagePickerEvent -> imagePickerLauncher.launch()
	}

	private fun setAutomaticCaptureEnabled(automaticCaptureEnabled: Boolean) {
		viewBinding.tvAutomaticCaptureStatus.isActivated = automaticCaptureEnabled
		cameraViewController.setAutoSnappingEnabled(automaticCaptureEnabled)
	}

	private fun setFlashEnabled(flashEnabled: Boolean) {
		viewBinding.tvFlashStatus.isActivated = flashEnabled
		cameraViewController.setFlashEnabled(flashEnabled)
	}

	private fun renderProcessing(processing: Processing) = when (processing) {
		is Processing.Import -> {
			decodingProgressDialog.stop()
			importProgressDialog.show(processing.progress)
			cameraViewController.onPause()
		}

		is Processing.Decoding -> {
			importProgressDialog.dismiss()
			decodingProgressDialog.start(this, getString(R.string.scanbot_processing_title))
			cameraViewController.onPause()
		}

		is Processing.None -> {
			importProgressDialog.dismiss()
			decodingProgressDialog.stop()
			cameraViewController.onResume()
		}
	}

	private fun showExitDialog() {
		exitDialog.show(context, viewModel::onExitConfirmed, viewModel::onExitDenied)
	}

	override fun onResume() {
		super.onResume()
		cameraViewController.onResume()
	}

	override fun onPause() {
		cameraViewController.onPause()
		super.onPause()
	}

	override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
		return if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
			viewModel.onTakePictureClicked()
			true
		} else {
			super.onKeyDown(keyCode, event)
		}
	}

    override fun showMessage(message: String) {
        viewBinding.userGuidance.setText(message)
        viewBinding.userGuidance.setIcon(R.drawable.scanbot_ic_alert)
        viewBinding.userGuidance.show()
    }
}
