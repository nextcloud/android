/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.util.logger

import com.ionos.scanbot.initializer.ScanbotInitializerImpl

object LoggerUtil {

	@JvmStatic
	fun logE(message: String, t: Throwable?){
		ScanbotInitializerImpl.logger?.logE(message, t)
	}

}