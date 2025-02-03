/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.di

import android.content.Context

internal inline fun <reified T> Context.inject(
	crossinline provide: ScanbotComponent.() -> T,
): Lazy<T> {
	return lazy { scanbotComponent.provide() }
}

internal val Context.scanbotComponent: ScanbotComponent
	get() {
		val provider = applicationContext as? ScanbotComponentProvider
		return provider?.getScanbotComponent() ?: throw IllegalStateException("No available context")
	}
