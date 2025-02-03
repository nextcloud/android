/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.tracking.stub

import com.ionos.scanbot.tracking.ScanbotGalleryScreenEventTracker
import javax.inject.Inject

class StubScanbotGalleryScreenEventTracker @Inject constructor(
): ScanbotGalleryScreenEventTracker {
    override fun trackBackPressed() = Unit

    override fun trackExitConfirmed() = Unit

    override fun trackExitDenied() = Unit

    override fun trackSwipeNext() = Unit

    override fun trackSwipeBack() = Unit

    override fun trackSaveClicked() = Unit

    override fun trackAddPictureClicked() = Unit

    override fun trackCropClicked() = Unit

    override fun trackFilterClicked() = Unit

    override fun trackRotateClicked() = Unit

    override fun trackRearrangeClicked() = Unit

    override fun trackDeleteClicked() = Unit

    override fun trackPage() = Unit
}