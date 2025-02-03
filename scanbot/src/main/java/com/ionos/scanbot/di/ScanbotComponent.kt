/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.di

import com.ionos.scanbot.controller.ScanbotController
import com.ionos.scanbot.screens.camera.CameraViewModelFactory
import com.ionos.scanbot.screens.common.ExitDialog
import com.ionos.scanbot.screens.crop.CropViewModelFactory
import com.ionos.scanbot.screens.filter.FilterViewModelFactory
import com.ionos.scanbot.screens.gallery.GalleryViewModelFactory
import com.ionos.scanbot.screens.gallery.pager.GalleryPagerAdapter
import com.ionos.scanbot.screens.rearrange.RearrangeViewModelFactory
import com.ionos.scanbot.screens.rearrange.recycler.RearrangeAdapter
import com.ionos.scanbot.screens.save.SaveViewModelFactory
import com.ionos.scanbot.screens.save.SelectDirectoryContract
import dagger.Subcomponent

@Subcomponent
abstract class ScanbotComponent {

	internal abstract fun cameraViewModelFactory(): CameraViewModelFactory

	internal abstract fun cropViewModelFactoryAssistant(): CropViewModelFactory.Assistant

	internal abstract fun filterViewModelFactoryAssistant(): FilterViewModelFactory.Assistant

	internal abstract fun galleryViewModelFactoryAssistant(): GalleryViewModelFactory.Assistant

	internal abstract fun galleryPagerAdapter(): GalleryPagerAdapter

	internal abstract fun rearrangeViewModelFactory(): RearrangeViewModelFactory

	internal abstract fun rearrangeAdapter(): RearrangeAdapter

	internal abstract fun saveViewModelFactory(): SaveViewModelFactory

	internal abstract fun exitDialog(): ExitDialog

	internal abstract fun scanbotController(): ScanbotController

    internal abstract fun selectDirectoryContract(): SelectDirectoryContract
}
