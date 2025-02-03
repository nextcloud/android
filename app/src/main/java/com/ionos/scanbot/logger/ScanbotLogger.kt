/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.logger

import com.ionos.scanbot.util.logger.Logger
import javax.inject.Inject
import com.nextcloud.client.logger.Logger as NextCloudLogger

class ScanbotLogger @Inject constructor(
    private val nextCloudLogger: NextCloudLogger
) : Logger {

    companion object {
        private const val GLOBAL_TAG = "ScanbotModule"
    }

    override fun logE(message: String, t: Throwable?) {
        t?.let {
            nextCloudLogger.e(GLOBAL_TAG, message, it)
        } ?: nextCloudLogger.e(GLOBAL_TAG, message)
    }

}