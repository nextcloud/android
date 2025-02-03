/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.provider

import android.content.Context
import io.scanbot.sdk.ScanbotSDK
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class SdkProvider @Inject constructor(
	private val context: Context,
) {
	private val sdk by lazy { ScanbotSDK(context) }

	fun get() = sdk
}
