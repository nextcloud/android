/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.util.lifedata

import androidx.lifecycle.LiveData

internal abstract class StateLiveData<S : Any>(initialValue: S) : LiveData<S>(initialValue) {

	override fun getValue(): S {
		return super.getValue() ?: throw IllegalStateException()
	}

	operator fun invoke(): S = getValue()
}
