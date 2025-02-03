/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.tracking

interface ScanbotGalleryScreenEventTracker : ScanbotScreenEventTracker {

	fun trackBackPressed()

	fun trackExitConfirmed()

	fun trackExitDenied()

	fun trackSwipeNext()

	fun trackSwipeBack()

	fun trackSaveClicked()

	fun trackAddPictureClicked()

	fun trackCropClicked()

	fun trackFilterClicked()

	fun trackRotateClicked()

	fun trackRearrangeClicked()

	fun trackDeleteClicked()
}
