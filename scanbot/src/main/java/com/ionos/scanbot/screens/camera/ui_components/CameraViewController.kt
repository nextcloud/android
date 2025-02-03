/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.camera.ui_components

import android.app.Activity
import com.ionos.scanbot.provider.ContourDetectorParamsProvider
import com.ionos.scanbot.screens.camera.use_case.GetUserGuidanceStatus
import com.ionos.scanbot.screens.camera.use_case.UserGuidanceStatus
import com.ionos.scanbot.util.rx.plusAssign
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import io.scanbot.sdk.ScanbotSDK
import io.scanbot.sdk.camera.*
import io.scanbot.sdk.contourdetector.ContourDetectorFrameHandler
import io.scanbot.sdk.contourdetector.DocumentAutoSnappingController
import io.scanbot.sdk.ui.PolygonView
import java.util.concurrent.TimeUnit

internal class CameraViewController(
	private val activity: Activity,
	private val cameraView: ScanbotCameraView,
	private val polygonView: PolygonView,
	private val onPictureReceived: ((ByteArray, Int) -> Unit),
    private val userGuidanceView: UserGuidanceView,
) {
	companion object {
		private const val SHOW_GUIDANCE_PERIOD = 3L
	}

	private val contourDetector = ScanbotSDK(activity).createContourDetector()
	private val contourDetectorFrameHandler = ContourDetectorFrameHandler.attach(cameraView, contourDetector)
	private val contourDetectorParamsProvider = ContourDetectorParamsProvider()

	private val autoSnappingController = DocumentAutoSnappingController(cameraView, contourDetectorFrameHandler)

	private val getUserGuidanceStatus = GetUserGuidanceStatus(activity)
	private val userGuidanceStatuses: PublishSubject<UserGuidanceStatus> = PublishSubject.create()
	private val userGuidanceDisposable = CompositeDisposable()

	private var isOnPause = true

	init {
		cameraView.init()
		contourDetectorFrameHandler.init()
	}

	fun onResume() {
		if (isOnPause) {
			cameraView.resume()
			contourDetectorFrameHandler.resume()
			autoSnappingController.resume()
			isOnPause = false
		}
	}

	fun onPause() {
		if (!isOnPause) {
			isOnPause = true
			autoSnappingController.pause()
			contourDetectorFrameHandler.pause()
			cameraView.pause()
		}
	}

	private fun ScanbotCameraView.init() {
		setPreviewMode(CameraPreviewMode.FILL_IN)
		setCameraOpenCallback {
			setAutoFocusSound(false)
			setShutterSound(false)
			continuousFocus()
		}
	}

	private fun ScanbotCameraView.resume() {
		addPictureCallback(pictureReceivedCallback)
		onResume()
	}

	private fun ScanbotCameraView.pause() {
		onPause()
		removePictureCallback(pictureReceivedCallback)
	}

	private fun ContourDetectorFrameHandler.init() {
		setAcceptedAngleScore(contourDetectorParamsProvider.acceptedAngleScore)
		setAcceptedSizeScore(contourDetectorParamsProvider.acceptedSizeScore)
	}

	private fun ContourDetectorFrameHandler.resume() {
		addResultHandler(polygonView.contourDetectorResultHandler)
		isEnabled = true
	}

	private fun ContourDetectorFrameHandler.pause() {
		isEnabled = false
		removeResultHandler(polygonView.contourDetectorResultHandler)
	}

	private fun DocumentAutoSnappingController.resume() {
		if (isEnabled) {
			subscribeToUserGuidanceMessages()
		}
	}

	private fun DocumentAutoSnappingController.pause() {
		if (isEnabled) {
			unsubscribeFromUserGuidanceMessages()
		}
	}

	fun setAutoSnappingEnabled(enabled: Boolean) {
		if (autoSnappingController.isEnabled != enabled) {
			autoSnappingController.isEnabled = enabled
			if (enabled) {
				subscribeToUserGuidanceMessages()
			} else {
				unsubscribeFromUserGuidanceMessages()
			}
		}
	}

	fun setFlashEnabled(enabled: Boolean) {
		// run catching due to bug in scanbot sdk
		val isFlashEnabled = runCatching(cameraView::isFlashEnabled).getOrDefault(!enabled)
		if (isFlashEnabled != enabled) {
			cameraView.useFlash(enabled)
		}
	}

	fun takePicture() {
		cameraView.takePicture(false)
	}

	private fun subscribeToUserGuidanceMessages() {
		contourDetectorFrameHandler.addResultHandler(userGuidanceListener)
		userGuidanceDisposable += userGuidanceStatuses
			.sample(SHOW_GUIDANCE_PERIOD, TimeUnit.SECONDS)
			.observeOn(AndroidSchedulers.mainThread())
			.doOnNext { userGuidanceView.hide() }
			.doOnDispose { userGuidanceView.hide() }
			.subscribe(::showUserGuidanceToast)
	}

	private fun unsubscribeFromUserGuidanceMessages() {
		contourDetectorFrameHandler.removeResultHandler(userGuidanceListener)
		userGuidanceDisposable.clear()
	}

	private fun showUserGuidanceToast(status: UserGuidanceStatus) {
        userGuidanceView.setText(status.text)
        status.icon?.let {
            userGuidanceView.setIcon(it)
        }
        userGuidanceView.show()
	}

	private val userGuidanceListener = ContourDetectorFrameHandler.ResultHandler { result ->
		if (result is FrameHandlerResult.Success) {
			val status = result.value.detectionStatus
			getUserGuidanceStatus(status).let{
                userGuidanceStatuses.onNext(it)
            }
		}
		false
	}

	private val pictureReceivedCallback: PictureCallback = object : PictureCallback() {
		override fun onPictureTaken(image: ByteArray, captureInfo: CaptureInfo) {
			cameraView.post {
				onPictureReceived(image, captureInfo.imageOrientation)
				setAutoSnappingEnabled(false)
				setFlashEnabled(false)
			}
		}
	}
}
