/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.di

import com.ionos.scanbot.tracking.ScanbotCameraScreenEventTracker
import com.ionos.scanbot.tracking.ScanbotCropScreenEventTracker
import com.ionos.scanbot.tracking.ScanbotFilterScreenEventTracker
import com.ionos.scanbot.tracking.ScanbotGalleryScreenEventTracker
import com.ionos.scanbot.tracking.ScanbotRearrangeScreenEventTracker
import com.ionos.scanbot.tracking.ScanbotSaveScreenEventTracker
import com.ionos.scanbot.tracking.stub.StubScanbotCameraScreenEventTracker
import com.ionos.scanbot.tracking.stub.StubScanbotCropScreenEventTracker
import com.ionos.scanbot.tracking.stub.StubScanbotFilterScreenEventTracker
import com.ionos.scanbot.tracking.stub.StubScanbotGalleryScreenEventTracker
import com.ionos.scanbot.tracking.stub.StubScanbotRearrangeScreenEventTracker
import com.ionos.scanbot.tracking.stub.StubScanbotSaveScreenEventTracker
import dagger.Binds
import dagger.Module

@Module
abstract class ScanbotTracking {

    @Binds
    abstract fun bindScanbotCameraScreenEventTracker(
        tracker: StubScanbotCameraScreenEventTracker
    ): ScanbotCameraScreenEventTracker

    @Binds
    abstract fun bindScanbotCropScreenEventTracker(
        tracker: StubScanbotCropScreenEventTracker
    ): ScanbotCropScreenEventTracker

    @Binds
    abstract fun bindScanbotFilterScreenEventTracker(
        tracker: StubScanbotFilterScreenEventTracker
    ): ScanbotFilterScreenEventTracker

    @Binds
    abstract fun bindScanbotGalleryScreenEventTracker(
        tracker: StubScanbotGalleryScreenEventTracker
    ): ScanbotGalleryScreenEventTracker

    @Binds
    abstract fun bindScanbotRearrangeScreenEventTracker(
        tracker: StubScanbotRearrangeScreenEventTracker
    ): ScanbotRearrangeScreenEventTracker

    @Binds
    abstract fun bindScanbotSaveScreenEventTracker(
        tracker: StubScanbotSaveScreenEventTracker
    ): ScanbotSaveScreenEventTracker
}