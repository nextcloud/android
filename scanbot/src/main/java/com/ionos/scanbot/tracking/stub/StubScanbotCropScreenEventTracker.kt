/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.tracking.stub

import com.ionos.scanbot.tracking.ScanbotCropScreenEventTracker
import javax.inject.Inject

class StubScanbotCropScreenEventTracker @Inject constructor(
): ScanbotCropScreenEventTracker {
    override fun trackBackPressed() = Unit

    override fun trackSaveClicked() = Unit

    override fun trackDetectDocumentClicked() = Unit

    override fun trackResetBordersClicked() = Unit

    override fun trackPage() = Unit
}