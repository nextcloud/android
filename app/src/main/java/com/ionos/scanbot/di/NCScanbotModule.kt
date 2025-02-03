/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.di

import com.ionos.scanbot.image_loader.ImageLoader
import com.ionos.scanbot.image_loading.ImageLoaderImpl
import com.ionos.scanbot.license.LoadScanbotLicense
import com.ionos.scanbot.license.LoadScanbotLicenseImpl
import com.ionos.scanbot.logger.ScanbotLogger
import com.ionos.scanbot.screens.save.SelectDirectoryContract
import com.ionos.scanbot.upload.ScanbotUploader
import com.ionos.scanbot.upload.SelectDirectoryContractImpl
import com.ionos.scanbot.upload.use_case.Uploader
import com.ionos.scanbot.util.GetLocalFreeSpace
import com.ionos.scanbot.util.logger.Logger
import com.ionos.scanbot.utils.GetLocalFreeSpaceImpl
import dagger.Binds
import dagger.Module

@Module(includes = [ScanbotModule::class])
abstract class NCScanbotModule {

    @Binds
    abstract fun bindLoadLicense(loadScanbotLicense: LoadScanbotLicenseImpl): LoadScanbotLicense

    @Binds
    abstract fun bindImageLoader(imageLoader: ImageLoaderImpl): ImageLoader

    @Binds
    abstract fun bindUploader(uploader: ScanbotUploader): Uploader

    @Binds
    abstract fun bindLogger(logger: ScanbotLogger): Logger

    @Binds
    abstract fun bindSelectDirectoryContract(contract: SelectDirectoryContractImpl): SelectDirectoryContract

    @Binds
    abstract fun bindGetLocalFreeSpaceImpl(getLocalFreeSpaceImpl: GetLocalFreeSpaceImpl): GetLocalFreeSpace

}