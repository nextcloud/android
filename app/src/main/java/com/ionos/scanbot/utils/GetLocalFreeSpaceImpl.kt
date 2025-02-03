/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.utils

import com.ionos.scanbot.util.GetLocalFreeSpace
import com.owncloud.android.utils.FileStorageUtils
import javax.inject.Inject

class GetLocalFreeSpaceImpl @Inject constructor() : GetLocalFreeSpace {
    override fun invoke(): Long = FileStorageUtils.getUsableSpace()
}