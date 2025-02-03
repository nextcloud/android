/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.util.lifedata

internal class MutableStateLiveData<S : Any>(initialValue: S) : StateLiveData<S>(initialValue) {

	public override fun setValue(value: S) {
		if (value != getValue()) {
			super.setValue(value)
		}
	}

	public override fun postValue(value: S) {
		super.postValue(value)
	}

	inline fun update(createUpdatedState: S.() -> S) {
		setValue(createUpdatedState(value))
	}

	inline fun postUpdate(createUpdatedState: S.() -> S) {
		postValue(createUpdatedState(value))
	}
}
