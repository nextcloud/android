/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.tracking.stub

import com.ionos.scanbot.tracking.ScanbotFilterScreenEventTracker
import javax.inject.Inject

class StubScanbotFilterScreenEventTracker @Inject constructor(
): ScanbotFilterScreenEventTracker {
    override fun trackBackPressed() = Unit

    override fun trackSaveClicked() = Unit

    override fun trackMoreClicked() = Unit

    override fun trackApplyForAllClicked() = Unit

    override fun trackMagicColorFilterApplied() = Unit

    override fun trackMagicTextFilterApplied() = Unit

    override fun trackColorFilterApplied() = Unit

    override fun trackGrayscaleFilterApplied() = Unit

    override fun trackBlackAndWhiteFilterApplied() = Unit

    override fun trackFilterReset() = Unit

    override fun trackBrightnessChanged() = Unit

    override fun trackBrightnessReset() = Unit

    override fun trackSharpnessChanged() = Unit

    override fun trackSharpnessReset() = Unit

    override fun trackContrastChanged() = Unit

    override fun trackContrastReset() = Unit

    override fun trackPage() = Unit
}