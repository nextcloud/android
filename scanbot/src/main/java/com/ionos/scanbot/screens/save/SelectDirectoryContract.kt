/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.save

import androidx.activity.result.contract.ActivityResultContract
import com.ionos.scanbot.upload.target_provider.UploadTarget

abstract class SelectDirectoryContract : ActivityResultContract<Unit, SelectDirectoryContract.SelectDirectoryResult>(){

    sealed interface SelectDirectoryResult{
        data class Success(val selectedTarget: UploadTarget): SelectDirectoryResult
        data object Canceled : SelectDirectoryResult
    }

}