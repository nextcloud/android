/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.tracking

interface ScanbotSaveScreenEventTracker : ScanbotScreenEventTracker {

	fun trackBackPressed()

	fun trackSaveClicked()

	fun trackFileNameChanged()

	fun trackSaveLocationPathClicked()

	fun trackSaveLocationPathChanged()

	fun trackSaveLocationPathChangeCanceled()

	fun trackPdfOcrFileTypeSelected()

	fun trackPdfFileTypeSelected()

	fun trackJpgFileTypeSelected()

	fun trackPngFileTypeSelected()
}
